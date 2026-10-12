package ai.scamchain.mobile.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ScamChainApi {
    suspend fun analyze(request: AnalyzeRequest): AnalysisResponse
    suspend fun chat(request: ChatRequest): ChatResponse
    suspend fun createIncident(request: IncidentRequest): IncidentResponse
}

data class AnalyzeRequest(
    val text: String,
    val source: String = "user_triggered",
    val image: ByteArray? = null,
    val crossCheck: Boolean = false,
)
data class AnalysisResponse(
    val risk: String, val category: String, val indicators: List<String>, val advice: List<String>,
    val model: String = "unknown", val mode: String = "groq",
    val crossCheckSummary: String = "", val sources: List<AnalysisSource> = emptyList(),
)
data class AnalysisSource(val title: String, val url: String, val snippet: String = "")
data class ChatMessage(val role: String, val content: String)
data class ChatRequest(val messages: List<ChatMessage>, val webSearch: Boolean = false)
data class ChatResponse(val answer: String, val model: String, val webSearchUsed: Boolean, val sources: List<AnalysisSource>)
data class IncidentRequest(val category: String, val risk: String, val summary: String)
data class IncidentResponse(val incidentId: String, val status: String)

class RemoteScamChainApi(private val baseUrl: String) : ScamChainApi {
    override suspend fun analyze(request: AnalyzeRequest): AnalysisResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().put("text", request.text).put("source", request.source).put("cross_check", request.crossCheck)
        request.image?.let { body.put("image_base64", Base64.encodeToString(it, Base64.NO_WRAP)).put("image_mime_type", "image/jpeg") }
        val json = post("/analyze", body)
        AnalysisResponse(
            json.optString("risk", "Unknown"), json.optString("category", "Unclear"),
            json.array("indicators"), json.array("advice"), json.optString("model", "unknown"), json.optString("mode", "unknown"),
            json.optString("cross_check_summary"), json.optJSONArray("sources")?.let { arr ->
                (0 until arr.length()).map { i -> arr.getJSONObject(i).let { AnalysisSource(it.optString("title"), it.optString("url"), it.optString("snippet")) } }
            } ?: emptyList(),
        )
    }

    override suspend fun chat(request: ChatRequest): ChatResponse = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        request.messages.forEach { messages.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val json = post("/chat", JSONObject().put("messages", messages).put("web_search", request.webSearch))
        ChatResponse(
            json.optString("answer", ""),
            json.optString("model", "unknown"),
            json.optBoolean("web_search_used", false),
            json.optJSONArray("sources")?.let { arr ->
                (0 until arr.length()).map { i -> arr.getJSONObject(i).let { AnalysisSource(it.optString("title"), it.optString("url"), it.optString("snippet")) } }
            } ?: emptyList(),
        )
    }

    override suspend fun createIncident(request: IncidentRequest): IncidentResponse = withContext(Dispatchers.IO) {
        val result = post("/incidents", JSONObject().put("category", request.category).put("risk", request.risk).put("summary", request.summary))
        IncidentResponse(result.optString("incident_id", "Unavailable"), result.optString("status", "Prepared"))
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 7000; readTimeout = 30000
            doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Backend returned HTTP $code: ${runCatching { JSONObject(response).optString("detail") }.getOrDefault("Request failed")}")
            return JSONObject(response)
        } finally { connection.disconnect() }
    }
}

private fun JSONObject.array(key: String): List<String> = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
