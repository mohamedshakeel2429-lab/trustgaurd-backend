package ai.scamchain.mobile.ui

import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.scamchain.mobile.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

private val Ink = Color(0xFF101C2C)
private val Muted = Color(0xFF718096)
private val Teal = Color(0xFF087E78)
private val Canvas = Color(0xFFF4F7F8)
private val Border = Color(0xFFE5EBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScamChainApp(
    onRequestCapture: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    overlayGranted: () -> Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    accessibilityEnabled: () -> Boolean,
    capturePreview: ByteArray?,
    onCapturePreviewConsumed: () -> Unit,
    onOpenOfficialPortal: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val firebaseAuth = remember(appContext) {
        runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(appContext)
                } catch (e: Exception) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:653448662001:android:2274dbf02c579ac8d29250")
                        .setApiKey("AIzaSyAByXo0fIZoQY_q8jLwcqQ7KqTuwZKe1Cw")
                        .setProjectId("it-expo")
                        .setStorageBucket("it-expo.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(appContext, options)
                }
            }
            FirebaseAuth.getInstance()
        }.getOrNull()
    }
    var authenticated by remember(firebaseAuth) { mutableStateOf(firebaseAuth?.currentUser != null) }
    DisposableEffect(firebaseAuth) {
        if (firebaseAuth == null) return@DisposableEffect onDispose { }
        val listener = FirebaseAuth.AuthStateListener { authenticated = it.currentUser != null }
        firebaseAuth.addAuthStateListener(listener)
        onDispose { firebaseAuth.removeAuthStateListener(listener) }
    }
    if (!authenticated) {
        LoginScreen(firebaseAuth = firebaseAuth, onAuthenticated = { authenticated = true })
        return
    }

    var page by remember { mutableStateOf(Page.HOME) }
    var maskPii by remember { mutableStateOf(true) }
    var crossCheck by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var chatBusy by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }
    var analysisGeneration by remember { mutableIntStateOf(0) }
    var incidentMessage by remember { mutableStateOf<String?>(null) }
    var pendingImage by remember { mutableStateOf<ByteArray?>(null) }
    var reportSummary by remember { mutableStateOf("") }
    val incidents = remember { mutableStateListOf<IncidentRecord>() }
    val chatLines = remember { mutableStateListOf<ChatLine>() }
    val api = remember { RemoteScamChainApi(ai.scamchain.mobile.BuildConfig.API_BASE_URL) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(capturePreview) { if (capturePreview != null) page = Page.PREVIEW }

    val isRoot = page in setOf(Page.HOME, Page.SCAN, Page.CHAT, Page.ACTIVITY, Page.PRIVACY)
    Scaffold(
        containerColor = Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandMark(34)
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text("ScamChain", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink, lineHeight = 20.sp)
                            Text("PERSONAL SAFETY", fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = Muted, letterSpacing = 1.1.sp)
                        }
                    }
                },
                navigationIcon = if (!isRoot) ({
                    IconButton(onClick = { page = Page.HOME }) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Ink) }
                }) else ({ Spacer(Modifier.width(8.dp)) }),
                actions = {
                    IconButton(onClick = { firebaseAuth?.signOut(); authenticated = false }) {
                        Icon(Icons.Outlined.Logout, "Sign out", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
            )
        },
        bottomBar = {
            if (isRoot) Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 5.dp)) {
                RootTab("Home", Icons.Outlined.Home, page == Page.HOME, Modifier.weight(1f)) { page = Page.HOME }
                RootTab("Analyze", Icons.Outlined.Search, page == Page.SCAN, Modifier.weight(1f)) { page = Page.SCAN }
                RootTab("Chat", Icons.Outlined.Chat, page == Page.CHAT, Modifier.weight(1f)) { page = Page.CHAT }
                RootTab("Activity", Icons.Outlined.History, page == Page.ACTIVITY, Modifier.weight(1f)) { page = Page.ACTIVITY }
                RootTab("Privacy", Icons.Outlined.Shield, page == Page.PRIVACY, Modifier.weight(1f)) { page = Page.PRIVACY }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                Page.HOME -> HomeScreen(
                    overlayOn = overlayGranted(),
                    onAnalyze = { page = Page.SCAN },
                    onChat = { page = Page.CHAT },
                    onCapture = onRequestCapture,
                    onSetup = { page = Page.SETUP },
                    latestIncident = incidents.firstOrNull()
                )
                Page.SCAN -> ScanScreen(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onCapture = onRequestCapture,
                    onAnalyze = { if (messageText.isNotBlank()) { pendingImage = null; analysisGeneration++; page = Page.ANALYSIS } },
                    onSetup = { page = Page.SETUP },
                    crossCheck = crossCheck,
                    onCrossCheckChange = { crossCheck = it }
                )
                Page.CHAT -> ChatScreen(
                    lines = chatLines,
                    input = chatInput,
                    onInputChange = { chatInput = it },
                    busy = chatBusy,
                    error = chatError,
                    onSend = {
                        val typed = chatInput.trim()
                        if (typed.isNotEmpty() && !chatBusy) {
                            val history = chatLines.takeLast(22).map { ChatMessage(it.role, if (it.role == "user" && maskPii) maskPersonalData(it.text) else it.text) } + ChatMessage("user", if (maskPii) maskPersonalData(typed) else typed)
                            chatLines.add(ChatLine("user", typed))
                            chatInput = ""
                            chatBusy = true
                            chatError = null
                            scope.launch {
                                try {
                                    val response = api.chat(ChatRequest(history))
                                    chatLines.add(ChatLine("assistant", response.answer, response.sources, response.model, response.webSearchUsed))
                                } catch (e: Exception) {
                                    chatError = e.message ?: "Could not reach Groq. Check the backend and try again."
                                } finally { chatBusy = false }
                            }
                        }
                    }
                )
                Page.ACTIVITY -> ActivityScreen(incidents, incidentMessage)
                Page.PRIVACY -> PrivacyScreen(
                    maskPii = maskPii,
                    onMaskChange = { maskPii = it },
                    overlayEnabled = overlayGranted(),
                    onOverlayChange = { onToggleOverlay(it) },
                    onSetup = { page = Page.SETUP },
                    onAccessibilitySettings = onOpenAccessibilitySettings,
                    accessibilityEnabled = accessibilityEnabled()
                )
                Page.SETUP -> SetupScreen(
                    overlayEnabled = overlayGranted(), onEnableOverlay = { onToggleOverlay(true) },
                    onCapture = onRequestCapture, accessibilityEnabled = accessibilityEnabled(),
                    onAccessibilitySettings = onOpenAccessibilitySettings
                )
                Page.PREVIEW -> CapturePreviewScreen(
                    preview = capturePreview,
                    piiMasking = maskPii,
                    onAnalyze = {
                        messageText = ""
                        pendingImage = capturePreview
                        analysisGeneration++
                        page = Page.ANALYSIS
                        onCapturePreviewConsumed()
                    },
                    onDiscard = { onCapturePreviewConsumed(); page = Page.HOME }
                )
                Page.ANALYSIS -> {
                    val analysisText = if (maskPii) maskPersonalData(messageText) else messageText
                    val imageForAnalysis = pendingImage
                    val result by produceState(initialValue = AnalysisResponse("…", "Connecting to Groq", emptyList(), emptyList()), analysisText, crossCheck, analysisGeneration) {
                        value = try { api.analyze(AnalyzeRequest(analysisText, image = imageForAnalysis, crossCheck = crossCheck)) }
                        catch (e: Exception) { AnalysisResponse("Unavailable", "Groq is unavailable", listOf(e.message ?: "Check the backend and try again."), listOf("No model analysis was returned. Check your Groq backend configuration and try again."), "unavailable", "error") }
                        pendingImage = null
                    }
                    AnalysisScreen(
                        input = messageText,
                        result = result,
                        onPrepareReport = { reportSummary = """ScamChain safety check
Risk signal: ${result.risk}
Category: ${result.category}
Message: ${if (maskPii) maskPersonalData(messageText) else messageText}

This summary is prepared by you. ScamChain has not filed a complaint.""".trimIndent(); page = Page.REPORT },
                        onSaveIncident = {
                            scope.launch {
                                val response = api.createIncident(IncidentRequest(result.category, result.risk, "User-reviewed note"))
                                val record = IncidentRecord(response.incidentId, result.category, result.risk, response.status)
                                incidents.add(0, record)
                                incidentMessage = "Saved privately · ${record.id}"
                                page = Page.ACTIVITY
                            }
                        },
                        onBack = { page = Page.SCAN }
                    )
                }
                Page.REPORT -> ReportScreen(reportSummary, { reportSummary = it }, onOpenOfficialPortal)
            }
        }
    }
}

@Composable
private fun LoginScreen(firebaseAuth: FirebaseAuth?, onAuthenticated: () -> Unit) {
    var createAccount by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(ai.scamchain.mobile.R.drawable.scamchain_logo),
            contentDescription = "ScamChain logo",
            modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
        Spacer(Modifier.height(20.dp))
        Text(if (createAccount) "Create your account" else "Welcome back", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Sign in to continue to ScamChain", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { createAccount = false; message = null }) { Text("Sign in") }
            TextButton(onClick = { createAccount = true; message = null }) { Text("Create account") }
        }
        if (createAccount) {
            OutlinedTextField(
                value = displayName, onValueChange = { displayName = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") }, singleLine = true, enabled = !busy,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") }, singleLine = true, enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") }, singleLine = true, enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, capitalization = KeyboardCapitalization.None),
        )
        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(message!!, color = if (firebaseAuth == null) Color(0xFF9A5B00) else Color(0xFFB3261E), fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val auth = firebaseAuth ?: return@Button
                if (email.isBlank() || password.isBlank() || (createAccount && displayName.isBlank())) {
                    message = "Enter your email and password${if (createAccount) " and name" else ""}."
                    return@Button
                }
                busy = true
                message = null
                val task = if (createAccount) auth.createUserWithEmailAndPassword(email.trim(), password)
                    else auth.signInWithEmailAndPassword(email.trim(), password)
                task.addOnCompleteListener { result ->
                    if (!result.isSuccessful) {
                        busy = false
                        message = firebaseAuthError(result.exception)
                    } else if (createAccount) {
                        val profile = UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build()
                        result.result?.user?.updateProfile(profile)?.addOnCompleteListener { profileResult ->
                            busy = false
                            if (profileResult.isSuccessful) onAuthenticated()
                            else {
                                message = "Your account was created, but ${firebaseAuthError(profileResult.exception).replaceFirstChar { it.lowercase() }}"
                                createAccount = false
                            }
                        } ?: run { busy = false; onAuthenticated() }
                    } else {
                        busy = false
                        onAuthenticated()
                    }
                }
            },
            enabled = firebaseAuth != null && !busy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (createAccount) "Create account" else "Sign in")
        }
        if (!createAccount) {
            TextButton(
                enabled = firebaseAuth != null && !busy && email.isNotBlank(),
                onClick = {
                    firebaseAuth?.sendPasswordResetEmail(email.trim())?.addOnCompleteListener { result ->
                        message = if (result.isSuccessful) "Password reset email sent." else firebaseAuthError(result.exception)
                    }
                },
            ) { Text("Forgot password?") }
        }
        if (firebaseAuth == null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Firebase is not configured. Add your Firebase project's google-services.json to the app folder and enable Email/Password sign-in in Firebase Console.",
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }
}

private fun firebaseAuthError(error: Exception?): String = when ((error as? FirebaseAuthException)?.errorCode) {
    "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password sign-in is disabled in Firebase Console. Enable it under Authentication > Sign-in method."
    "ERROR_NETWORK_REQUEST_FAILED" -> "Could not reach Firebase Authentication. Check the emulator's internet connection and try again."
    "ERROR_INVALID_API_KEY", "ERROR_API_KEY_INVALID" -> "Firebase rejected this app's API key. Check that google-services.json belongs to this Firebase project."
    "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
    "ERROR_WEAK_PASSWORD" -> "Choose a stronger password and try again."
    "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already uses this email. Switch to Sign in."
    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Email or password is incorrect. New users need to choose Create account first."
    "ERROR_USER_DISABLED" -> "This account is disabled. Contact the Firebase project administrator."
    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a little before trying again."
    else -> error?.localizedMessage ?: "Authentication failed. Check your details and try again."
}

private enum class Page { HOME, SCAN, CHAT, ACTIVITY, PRIVACY, SETUP, PREVIEW, ANALYSIS, REPORT }
private data class IncidentRecord(val id: String, val category: String, val risk: String, val status: String)
private data class ChatLine(val role: String, val text: String, val sources: List<AnalysisSource> = emptyList(), val model: String = "", val webSearchUsed: Boolean = false)

@Composable
private fun HomeScreen(overlayOn: Boolean, onAnalyze: () -> Unit, onChat: () -> Unit, onCapture: () -> Unit, onSetup: () -> Unit, latestIncident: IncidentRecord?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Stay a step ahead.", fontSize = 29.sp, lineHeight = 34.sp, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Check a suspicious message or screen when you choose.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF102D3A))) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.VerifiedUser, null, tint = Color(0xFF7CE3CE), modifier = Modifier.size(19.dp))
                        Text("PROTECTION STATUS", color = Color(0xFFB9DFD7), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.clip(CircleShape).background(Color(0xFF21464C)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(if (overlayOn) "SHORTCUT ON" else "READY", color = Color(0xFF9EF0DC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Your privacy comes first", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Text("Groq reviews content only after you ask. Optional SerpApi search can provide current web results. Your screen is never watched in the background.", color = Color(0xFFC3D2D4), fontSize = 13.sp, lineHeight = 19.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Outlined.Lock, null, tint = Color(0xFF7CE3CE), modifier = Modifier.size(15.dp))
                        Text("Groq AI · Sent only when requested", color = Color(0xFF9EF0DC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Text("QUICK CHECK", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(9.dp))
            Card(onClick = onAnalyze, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Teal)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Search, null, tint = Color.White, modifier = Modifier.size(25.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Check a message", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Review warning signs privately", color = Color(0xFFD0F1EA), fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.ArrowForward, null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCapture, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
                Icon(Icons.Outlined.ScreenshotMonitor, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp)); Text("Capture screen with consent", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onChat, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal)) {
                Icon(Icons.Outlined.Chat, null, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("Ask ScamChain anything", fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Protection tools", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSetup) { Text("Set up", color = Teal) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile("Floating shortcut", if (overlayOn) "Active" else "Optional", Icons.Outlined.BubbleChart, Modifier.weight(1f), onSetup)
                ToolTile("Privacy controls", "Masking & access", Icons.Outlined.Shield, Modifier.weight(1f), onSetup)
            }
        }
        item {
            if (latestIncident != null) {
                Text("RECENT NOTE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(8.dp))
                IncidentCard(latestIncident.id, latestIncident.category, latestIncident.risk, latestIncident.status)
            } else {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFEAF4F2)).padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = Teal, modifier = Modifier.size(19.dp))
                    Text("ScamChain offers helpful signals, not a guarantee. Verify unexpected requests with the organization through a number you trust.", color = Color(0xFF48635F), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun ScanScreen(text: String, onTextChange: (String) -> Unit, onCapture: () -> Unit, onAnalyze: () -> Unit, onSetup: () -> Unit, crossCheck: Boolean, onCrossCheckChange: (Boolean) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeading("Check before you act", "Paste a message or capture a screen you are unsure about.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Teal, modifier = Modifier.size(18.dp))
                        Text("Message or link", color = Ink, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 155.dp),
                        placeholder = { Text("Paste the text you want to check…", color = Muted) },
                        shape = RoundedCornerShape(15.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        supportingText = { Text("Keep passwords, one-time codes and full card numbers to yourself.") }
                    )
                    Button(onClick = onAnalyze, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Analyze with Groq", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Official-source web search", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("Optional SerpApi search. The query goes to SerpApi; result snippets may be incomplete.", color = Muted, fontSize = 11.sp, lineHeight = 15.sp) }
                        Switch(checked = crossCheck, onCheckedChange = onCrossCheckChange)
                    }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f), color = Border); Text("  OR  ", color = Muted, fontSize = 11.sp); HorizontalDivider(Modifier.weight(1f), color = Border) } }
        item {
            Card(onClick = onCapture, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Icon(Icons.Outlined.ScreenshotMonitor, null, tint = Teal, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) { Text("Capture a screen", color = Ink, fontWeight = FontWeight.Bold); Text("Android asks your permission each time", color = Muted, fontSize = 12.sp) }
                    Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
                }
            }
        }
        item { InfoStrip("When you tap Analyze, the selected text or image is sent to your configured backend and Groq. Optional search sends a query to SerpApi. Nothing runs in the background.", Icons.Outlined.Lock) }
        item { TextButton(onClick = onSetup) { Text("Review optional protection settings", color = Teal) } }
    }
}

@Composable
private fun AnalysisScreen(input: String, result: AnalysisResponse, onSaveIncident: () -> Unit, onBack: () -> Unit, onPrepareReport: () -> Unit) {
    val context = LocalContext.current
    val failed = result.mode == "error"
    val high = result.risk.equals("High", true)
    val medium = result.risk.equals("Medium", true)
    val riskColor = when { failed -> Muted; high -> Color(0xFFB6473F); medium -> Color(0xFFAE7119); else -> Teal }
    val riskTint = when { failed -> Color(0xFFF0F2F4); high -> Color(0xFFFFF0ED); medium -> Color(0xFFFFF6E8); else -> Color(0xFFE8F6F2) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { ScreenHeading("Your safety check", if (result.mode == "error") "Groq response unavailable · ${result.model}" else "Groq model · ${result.model}") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = riskTint), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (failed) Icons.Outlined.ErrorOutline else if (high) Icons.Outlined.GppBad else Icons.Outlined.VerifiedUser, null, tint = riskColor, modifier = Modifier.size(21.dp))
                        Text(if (failed) "NO MODEL RESULT" else "${result.risk.uppercase()} RISK SIGNAL", color = riskColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = .8.sp)
                    }
                    Text(result.category, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                    if (!failed) Text("This is a screening signal, not a definitive judgment.", color = Muted, fontSize = 12.sp)
                }
            }
        }
        if (failed) {
            item { InfoStrip("No content was analyzed. Check the backend and Groq settings, then retry.", Icons.Outlined.Info) }
        } else {
            item { InfoStrip("False positives can flag a legitimate message; false negatives can miss a scam. This model result is not proof. Verify through a contact method you already trust.", Icons.Outlined.Info) }
            item { SectionTitle("Signals found") }
            if (result.indicators.isEmpty()) item { InfoStrip("The model found no specific warning signs. That does not guarantee the content is safe.", Icons.Outlined.Info) }
            items(result.indicators) { SignalRow(it) }
            item { SectionTitle("Safer next steps") }
            items(result.advice) { AdviceRow(it) }
            if (result.crossCheckSummary.isNotBlank()) item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Web cross-check", color = Ink, fontWeight = FontWeight.Bold)
                    Text(result.crossCheckSummary, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    result.sources.forEach { source ->
                        Text("${source.title}: ${source.snippet}", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        Text(source.url, color = Teal, fontSize = 11.sp, modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) } })
                    }
                    if (result.sources.isEmpty()) Text("No trusted-source links were returned. Treat this as unverified.", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { SectionTitle("Reviewed content") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Text(input.ifBlank { "No text provided" }, Modifier.fillMaxWidth().padding(14.dp), color = Muted, fontSize = 12.sp, maxLines = 5)
            }
        }
        item {
            if (!failed) {
                OutlinedButton(onClick = onPrepareReport, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Prepare report summary", color = Teal) }
                Button(onClick = onSaveIncident, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
                    Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Save private note", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Edit text", color = Teal) }
        }
        item { InfoStrip("The assessment reflects only the content you chose to submit. ScamChain does not submit an official complaint.", Icons.Outlined.Lock) }
    }
}

@Composable
private fun ChatScreen(
    lines: List<ChatLine>,
    input: String,
    onInputChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(lines.size, busy) {
        val last = (lines.size - 1 + if (busy) 1 else 0).coerceAtLeast(0)
        if (lines.isNotEmpty() || busy) listState.animateScrollToItem(last)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ask ScamChain", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Answers your question directly and searches the web when current sources are needed.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (lines.isEmpty()) {
            InfoStrip("Web search runs automatically for current information, website requests, and company scam or incident questions. Search results may be incomplete; avoid entering passwords, one-time codes, or API keys.", Icons.Outlined.Info)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(lines.size) { index ->
                val line = lines[index]
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (line.role == "user") Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Card(
                        modifier = Modifier.widthIn(max = 330.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = if (line.role == "user") Color(0xFFE4F2EF) else Color.White),
                    ) {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (line.role == "assistant" && line.model.isNotBlank()) Text("GROQ · ${line.model}${if (line.webSearchUsed) " · WEB SOURCES" else ""}", color = Teal, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                            Text(line.text, color = Ink, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                    line.sources.forEach { source ->
                        Card(modifier = Modifier.widthIn(max = 330.dp).clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) } }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(source.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (source.snippet.isNotBlank()) Text(source.snippet, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                                Text(source.url, color = Teal, fontSize = 10.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }
            if (busy) item { Text("Groq is responding…", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }
        if (error != null) InfoStrip("No model response: $error", Icons.Outlined.ErrorOutline)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask anything…") },
                shape = RoundedCornerShape(16.dp),
                maxLines = 4,
                supportingText = { Text("Groq receives your message; relevant queries may also go to SerpApi.", fontSize = 9.sp) },
            )
            FilledIconButton(onClick = onSend, enabled = input.isNotBlank() && !busy, modifier = Modifier.size(50.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Teal)) {
                Icon(Icons.Outlined.Send, contentDescription = "Send to Groq", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ActivityScreen(incidents: List<IncidentRecord>, lastMessage: String?) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Your activity", "Private notes saved on this device.") }
        item {
            if (incidents.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Inbox, null, tint = Teal, modifier = Modifier.size(38.dp))
                        Text("No saved notes yet", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("When you save a safety check, it will appear here.", color = Muted, fontSize = 13.sp)
                    }
                }
            }
        }
        items(incidents, key = { it.id }) { IncidentCard(it.id, it.category, it.risk, it.status) }
        if (lastMessage != null) item { Text(lastMessage, color = Muted, fontSize = 11.sp) }
        item { InfoStrip("Notes last only for this app session. The incident endpoint is a placeholder and does not store case data.", Icons.Outlined.Info) }
    }
}

@Composable
private fun PrivacyScreen(maskPii: Boolean, onMaskChange: (Boolean) -> Unit, overlayEnabled: Boolean, onOverlayChange: (Boolean) -> Unit, onSetup: () -> Unit, onAccessibilitySettings: () -> Unit, accessibilityEnabled: Boolean) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeading("Privacy & controls", "You decide what ScamChain can access and when.") }
        item {
            PrivacySetting("Mask personal details", "Hide email addresses and long number strings before sending typed messages to Groq. Screenshot pixels are not masked.", maskPii, onMaskChange)
            Spacer(Modifier.height(10.dp))
            PrivacySetting("Floating shortcut", "Show a tap-to-open button over other apps. It never reads the screen.", overlayEnabled, onOverlayChange)
        }
        item { InfoStrip("Firebase Authentication stores your account name and email and manages your password. Chat and analysis requests go to your configured backend only after you ask; relevant chat questions may also be sent to SerpApi.", Icons.Outlined.Lock) }
        item { SectionTitle("Optional access") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Accessibility text assist", color = Ink, fontWeight = FontWeight.Bold)
                    Text("Android treats this as sensitive access. The service scaffold does not collect events or read text automatically. Enable it only if you understand and need this feature.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    OutlinedButton(onClick = onAccessibilitySettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text(if (accessibilityEnabled) "Review Android accessibility settings" else "Open Android accessibility settings") }
                }
            }
        }
        item { SectionTitle("Permissions overview") }
        item { PermissionLine(Icons.Outlined.ScreenshotMonitor, "Screen capture", "Android consent is required for every capture.") }
        item { PermissionLine(Icons.Outlined.BubbleChart, "Floating shortcut", "Optional. Requires Android's display-over-other-apps setting.") }
        item { PermissionLine(Icons.Outlined.AccessibilityNew, "Accessibility", "Optional. Must be enabled manually in Android settings.") }
        item { Button(onClick = onSetup, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("View setup guide") } }
    }
}

@Composable
private fun SetupScreen(overlayEnabled: Boolean, onEnableOverlay: () -> Unit, onCapture: () -> Unit, accessibilityEnabled: Boolean, onAccessibilitySettings: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Set up protection", "These options are optional and remain under your control.") }
        item { SetupCard("01", "Floating shortcut", "Adds a small button over other apps. Tapping it only opens ScamChain; it does not inspect your screen.", if (overlayEnabled) "Enabled" else "Allow overlay", overlayEnabled, onEnableOverlay) }
        item { SetupCard("02", "Screen capture", "Each capture starts with Android's system consent screen. The captured image is shown locally before you choose what to do.", "Capture with consent", false, onCapture) }
        item { SetupCard("03", "Accessibility text assist", "This sensitive Android permission is optional. The included service does not harvest text or monitor activity.", if (accessibilityEnabled) "Enabled in settings" else "Review Android settings", accessibilityEnabled, onAccessibilitySettings) }
        item { InfoStrip("You can use message analysis without granting overlay, capture or accessibility permissions.", Icons.Outlined.Lock) }
    }
}

@Composable
private fun CapturePreviewScreen(preview: ByteArray?, piiMasking: Boolean, onAnalyze: () -> Unit, onDiscard: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { ScreenHeading("Review your capture", "Check the image before continuing. It stays on this device.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                val bitmap = remember(preview) { preview?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
                if (bitmap != null) Image(bitmap, contentDescription = "Locally captured screen preview", modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).padding(8.dp))
                else Text("Capture preview is unavailable. You can discard it and try again.", Modifier.padding(18.dp), color = Muted)
            }
        }
        item { InfoStrip(if (piiMasking) "Review the image here first. It is sent to Groq only after you tap Analyze this screen. Text masking does not hide screenshot pixels." else "Review the image here first. It is sent to Groq only after you tap Analyze this screen.", Icons.Outlined.Lock) }
        item {
            Text("Visual analysis is sent to your configured backend only after you choose Analyze this screen.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(6.dp))
            Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Analyze this screen", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) { Text("Discard capture", color = Muted) }
        }
    }
}

@Composable
private fun ReportScreen(summary: String, onSummaryChange: (String) -> Unit, onOpenOfficialPortal: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Prepare a report", "Review and edit this summary before choosing what to do next.") }
        item { OutlinedTextField(summary, onSummaryChange, modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp), label = { Text("Your summary") }, shape = RoundedCornerShape(16.dp)) }
        item { InfoStrip("ScamChain does not file a complaint. You will leave the app to use the official Government of India portal. For financial cyber fraud, call 1930 promptly.", Icons.Outlined.Info) }
        item { OutlinedButton(onClick = { clipboard.setText(AnnotatedString(summary)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Copy summary", color = Teal) } }
        item { Button(onClick = onOpenOfficialPortal, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Continue to official portal", fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun ScreenHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Ink, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun BrandMark(size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(size.dp * .28f)), contentAlignment = Alignment.Center) {
        Image(
            painter = androidx.compose.ui.res.painterResource(ai.scamchain.mobile.R.drawable.scamchain_icon),
            contentDescription = "ScamChain brand logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
    }
}

@Composable
private fun RowScope.RootTab(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier = Modifier, action: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = action).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, contentDescription = label, tint = if (selected) Teal else Muted, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) Teal else Muted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun ToolTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, tint = Teal, modifier = Modifier.size(21.dp))
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PrivacySetting(title: String, subtitle: String, checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (checked) Icons.Outlined.Lock else Icons.Outlined.Tune, null, tint = Teal, modifier = Modifier.size(21.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp) }
            if (onChange != null) Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Teal))
            else Text("ON", color = Teal, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SetupCard(number: String, title: String, body: String, actionLabel: String, enabled: Boolean, action: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(31.dp).clip(CircleShape).background(Color(0xFFE5F3F0)), contentAlignment = Alignment.Center) { Text(number, color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (enabled) { Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.CheckCircle, null, tint = Teal, modifier = Modifier.size(19.dp)) }
            }
            Text(body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text(actionLabel, color = if (enabled) Teal else Ink) }
        }
    }
}

@Composable
private fun InfoStrip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFFE9F3F1)).padding(13.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Icon(icon, null, tint = Teal, modifier = Modifier.size(17.dp)); Text(text, color = Color(0xFF48635F), fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun SectionTitle(text: String) { Text(text, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp) }

@Composable
private fun SignalRow(text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.WarningAmber, null, tint = Color(0xFFB87922), modifier = Modifier.size(18.dp)); Text(text, color = Ink, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun AdviceRow(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.CheckCircleOutline, null, tint = Teal, modifier = Modifier.size(18.dp)); Text(text, color = Ink, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun PermissionLine(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Icon(icon, null, tint = Teal, modifier = Modifier.size(20.dp)); Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(description, color = Muted, fontSize = 11.sp, lineHeight = 15.sp) }
    }
}

@Composable
private fun IncidentCard(id: String, category: String, risk: String, status: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.BookmarkBorder, null, tint = Teal, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(category, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("$id · $status", color = Muted, fontSize = 10.sp) }
            Text(risk.uppercase(), color = if (risk.equals("High", true)) Color(0xFFB6473F) else Teal, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

private fun maskPersonalData(input: String): String = input
    .replace(Regex("\\b(?:gsk|sk|ghp|gho|github_pat)_[A-Za-z0-9_-]{12,}\\b", RegexOption.IGNORE_CASE), "[API KEY]")
    .replace(Regex("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}"), "[EMAIL]")
    .replace(Regex("(?<!\\d)(?:\\+?\\d[ -]?){9,15}(?!\\d)"), "[PHONE OR NUMBER]")
