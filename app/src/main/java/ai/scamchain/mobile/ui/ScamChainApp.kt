package ai.scamchain.mobile.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration
import ai.scamchain.mobile.data.*
import ai.scamchain.mobile.R
import ai.scamchain.mobile.ui.theme.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
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

    var page by remember { mutableStateOf(Page.SPLASH) }
    var onboardingDone by remember { mutableStateOf(false) }

    if (page == Page.SPLASH) {
        SplashScreen(
            onFinished = {
                page = if (!onboardingDone) Page.ONBOARDING else if (!authenticated) Page.AUTH else Page.HOME
            }
        )
        return
    }

    if (page == Page.ONBOARDING) {
        OnboardingScreen(
            onFinish = {
                onboardingDone = true
                page = if (!authenticated) Page.AUTH else Page.HOME
            }
        )
        return
    }

    if (!authenticated || page == Page.AUTH) {
        LoginScreen(
            firebaseAuth = firebaseAuth,
            onAuthenticated = {
                authenticated = true
                page = Page.HOME
            }
        )
        return
    }

    var maskPii by remember { mutableStateOf(true) }
    var crossCheck by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var chatBusy by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }
    var chatWebSearch by remember { mutableStateOf(false) }
    var analysisGeneration by remember { mutableIntStateOf(0) }
    var incidentMessage by remember { mutableStateOf<String?>(null) }
    var pendingImage by remember { mutableStateOf<ByteArray?>(null) }
    var reportSummary by remember { mutableStateOf("") }
    val incidents = remember { mutableStateListOf<IncidentRecord>() }
    val chatLines = remember { mutableStateListOf<ChatLine>() }
    val api = remember { RemoteScamChainApi(ai.scamchain.mobile.BuildConfig.API_BASE_URL) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(capturePreview) { if (capturePreview != null) page = Page.PREVIEW }

    val isRoot = page in setOf(Page.HOME, Page.SCAN, Page.CHAT, Page.LEARN, Page.PRIVACY)

    Scaffold(
        containerColor = NeuralBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandMark(32)
                        Column {
                            Text("TrustGuard", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Text("TRUST WHAT YOU VERIFY", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = CyanAccent, letterSpacing = 1.2.sp)
                        }
                    }
                },
                navigationIcon = if (!isRoot) ({
                    IconButton(onClick = { page = Page.HOME }) { Icon(Icons.Outlined.ArrowBack, "Back", tint = TextPrimary) }
                }) else ({ Spacer(Modifier.width(8.dp)) }),
                actions = {
                    Box(
                        Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(Corner.large))
                            .background(NeuralCard)
                            .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(SuccessGreen))
                            Text("READY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeuralBg)
            )
        },
        bottomBar = {
            if (isRoot) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NeuralBgSecondary)
                        .border(1.dp, NeuralOutline, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    RootTab("Home", Icons.Outlined.Shield, page == Page.HOME, Modifier.weight(1f)) { page = Page.HOME }
                    RootTab("Analyze", Icons.Outlined.Radar, page == Page.SCAN, Modifier.weight(1f)) { page = Page.SCAN }
                    RootTab("AI Assistant", Icons.Outlined.ChatBubbleOutline, page == Page.CHAT, Modifier.weight(1f)) { page = Page.CHAT }
                    RootTab("Learn", Icons.Outlined.Lightbulb, page == Page.LEARN, Modifier.weight(1f)) { page = Page.LEARN }
                    RootTab("Privacy", Icons.Outlined.Lock, page == Page.PRIVACY, Modifier.weight(1f)) { page = Page.PRIVACY }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                Page.HOME -> HomeScreen(
                    overlayOn = overlayGranted(),
                    onAnalyze = { page = Page.SCAN },
                    onChat = { page = Page.CHAT },
                    onLearn = { page = Page.LEARN },
                    onCapture = onRequestCapture,
                    onSetup = { page = Page.SETUP },
                    latestIncident = incidents.firstOrNull(),
                    userName = firebaseAuth?.currentUser?.displayName ?: "Security User"
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
                    webSearch = chatWebSearch,
                    onWebSearchChange = { chatWebSearch = it },
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
                                    val response = api.chat(ChatRequest(history, chatWebSearch))
                                    chatLines.add(ChatLine("assistant", response.answer, response.sources, response.model, response.webSearchUsed))
                                } catch (e: Exception) {
                                    chatError = e.message ?: "Could not reach TrustGuard AI server. Please check connection and try again."
                                } finally { chatBusy = false }
                            }
                        }
                    },
                    onSuggestionSelected = { chatInput = it },
                    onRetry = {
                        val lastUser = chatLines.lastOrNull { it.role == "user" }?.text
                        if (lastUser != null) { chatInput = lastUser }
                    }
                )
                Page.LEARN -> LearnScreen()
                Page.ACTIVITY -> ActivityScreen(incidents, incidentMessage)
                Page.PRIVACY -> PrivacyScreen(
                    userEmail = firebaseAuth?.currentUser?.email ?: "anonymous@trustguard.ai",
                    maskPii = maskPii,
                    onMaskChange = { maskPii = it },
                    overlayEnabled = overlayGranted(),
                    onOverlayChange = { onToggleOverlay(it) },
                    onSetup = { page = Page.SETUP },
                    onAccessibilitySettings = onOpenAccessibilitySettings,
                    accessibilityEnabled = accessibilityEnabled(),
                    onLogout = {
                        firebaseAuth?.signOut()
                        authenticated = false
                        page = Page.AUTH
                    },
                    darkTheme = darkTheme,
                    onDarkThemeChange = onDarkThemeChange,
                )
                Page.SETUP -> SetupScreen(
                    overlayEnabled = overlayGranted(),
                    onEnableOverlay = { onToggleOverlay(true) },
                    onCapture = onRequestCapture,
                    accessibilityEnabled = accessibilityEnabled(),
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
                    val result by produceState(initialValue = AnalysisResponse("…", "Connecting Neural Shield Engine", emptyList(), emptyList()), analysisText, crossCheck, analysisGeneration) {
                        value = try { api.analyze(AnalyzeRequest(analysisText, image = imageForAnalysis, crossCheck = crossCheck)) }
                        catch (e: Exception) { AnalysisResponse("Unavailable", "Neural Shield Offline", listOf(e.message ?: "Server connection failed"), listOf("Check backend connection and retry."), "unavailable", "error") }
                        pendingImage = null
                    }
                    AnalysisScreen(
                        input = messageText,
                        result = result,
                        onPrepareReport = {
                            reportSummary = """TrustGuard Digital Safety Analysis
Risk Signal: ${result.risk}
Category: ${result.category}
Submitted Content: ${if (maskPii) maskPersonalData(messageText) else messageText}

Prepared locally; not submitted. Preserve original messages, links, and transaction references.""".trimIndent()
                            page = Page.REPORT
                        },
                        onSaveIncident = {
                            scope.launch {
                                val response = api.createIncident(IncidentRequest(result.category, result.risk, "User verified record"))
                                val record = IncidentRecord(response.incidentId, result.category, result.risk, response.status)
                                incidents.add(0, record)
                                incidentMessage = "Saved to local activity log · ${record.id}"
                                page = Page.ACTIVITY
                            }
                        },
                        onBack = { page = Page.SCAN }
                    )
                }
                Page.REPORT -> ReportScreen(reportSummary, { reportSummary = it }, onOpenOfficialPortal)
                else -> {}
            }
        }
    }
}

private enum class Page { SPLASH, ONBOARDING, AUTH, HOME, SCAN, CHAT, LEARN, ACTIVITY, PRIVACY, SETUP, PREVIEW, ANALYSIS, REPORT }
private data class IncidentRecord(val id: String, val category: String, val risk: String, val status: String)
private data class ChatLine(val role: String, val text: String, val sources: List<AnalysisSource> = emptyList(), val model: String = "", val webSearchUsed: Boolean = false)

// -------------------------------------------------------------------------
// SPLASH SCREEN
// -------------------------------------------------------------------------
@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onFinished()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(NeuralBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LottieIcon(R.raw.shield_motion, "TrustGuard shield scanning", size = 130.dp, tint = Palette.signal, repeat = true)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TrustGuard", fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.headlineLarge.fontFamily, color = TextPrimary, letterSpacing = 0.5.sp)
                Text("TRUST WHAT YOU VERIFY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

// -------------------------------------------------------------------------
// ONBOARDING SCREEN
// -------------------------------------------------------------------------
@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardPage("AI Scam Detection", "Instant neural protection against phishing, fake links, and payment fraud.", Icons.Outlined.Psychology),
        OnboardPage("Neural Shield Scan", "Inspect text, suspicious links, and screenshots in milliseconds.", Icons.Outlined.Radar),
        OnboardPage("Privacy Safeguards", "Automatic PII data masking. Your screen is never watched in the background.", Icons.Outlined.Shield)
    )
    val page = pages[step]

    Column(
        Modifier
            .fillMaxSize()
            .background(NeuralBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandMark(28)
                Text("TrustGuard", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }
            TextButton(onClick = onFinish) {
                Text("SKIP", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LottieIcon(if (step == 1) R.raw.scanning else R.raw.shield_motion, "TrustGuard shield", size = 140.dp, tint = Palette.signal, repeat = step == 1)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(page.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                Text(page.desc, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == step) 20.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (index == step) CyanAccent else NeuralOutline)
                    )
                }
            }
            Button(
                onClick = { if (step < 2) step++ else onFinish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(Corner.small),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NeuralBg)
            ) {
                Text(if (step == 2) "Get Started" else "Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

private data class OnboardPage(val title: String, val desc: String, val icon: ImageVector)

// -------------------------------------------------------------------------
// LOGIN / AUTH SCREEN
// -------------------------------------------------------------------------
@Composable
private fun LoginScreen(firebaseAuth: FirebaseAuth?, onAuthenticated: () -> Unit) {
    var createAccount by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(NeuralBg)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieIcon(R.raw.shield_motion, "TrustGuard shield", size = 90.dp, tint = Palette.signal)
        Spacer(Modifier.height(16.dp))
        Text("TrustGuard", fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.headlineLarge.fontFamily, color = TextPrimary)
        Text("TRUST WHAT YOU VERIFY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Text("A second look before you act.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = NeuralCard),
            shape = RoundedCornerShape(Corner.large),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Corner.small))
                        .background(NeuralBgSecondary)
                        .padding(4.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Corner.small))
                            .background(if (!createAccount) CyanAccent else Color.Transparent)
                            .clickable { createAccount = false; message = null }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (!createAccount) NeuralBg else TextPrimary)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Corner.small))
                            .background(if (createAccount) CyanAccent else Color.Transparent)
                            .clickable { createAccount = true; message = null }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (createAccount) NeuralBg else TextPrimary)
                    }
                }

                if (createAccount) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full Name") },
                        singleLine = true,
                        enabled = !busy,
                        colors = neuralTextFieldColors(),
                        shape = RoundedCornerShape(Corner.small)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Address") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = neuralTextFieldColors(),
                    shape = RoundedCornerShape(Corner.small)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Toggle Password", tint = TextSecondary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = neuralTextFieldColors(),
                    shape = RoundedCornerShape(Corner.small)
                )

                if (message != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Corner.small))
                            .background(DangerRed.copy(alpha = 0.15f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, "Sign-in error", tint = DangerRed, modifier = Modifier.size(16.dp))
                        Text(message!!, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        val auth = firebaseAuth ?: return@Button
                        if (email.isBlank() || password.isBlank() || (createAccount && displayName.isBlank())) {
                            message = "Please complete all required fields."
                            return@Button
                        }
                        busy = true
                        message = null
                        val task = if (createAccount) auth.createUserWithEmailAndPassword(email.trim(), password)
                        else auth.signInWithEmailAndPassword(email.trim(), password)

                        task.addOnCompleteListener { result ->
                            if (!result.isSuccessful) {
                                busy = false
                                message = result.exception?.localizedMessage ?: "Authentication failed."
                            } else if (createAccount) {
                                val profile = UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build()
                                result.result?.user?.updateProfile(profile)?.addOnCompleteListener {
                                    busy = false
                                    onAuthenticated()
                                } ?: run { busy = false; onAuthenticated() }
                            } else {
                                busy = false
                                onAuthenticated()
                            }
                        }
                    },
                    enabled = firebaseAuth != null && !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(Corner.small),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NeuralBg)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = NeuralBg, strokeWidth = 2.dp)
                    else Text(if (createAccount) "Register Account" else "Authenticate", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (!createAccount) {
                    TextButton(
                        enabled = firebaseAuth != null && !busy && email.isNotBlank(),
                        onClick = {
                            firebaseAuth?.sendPasswordResetEmail(email.trim())?.addOnCompleteListener { result ->
                                message = if (result.isSuccessful) "Password reset instructions sent to email." else result.exception?.localizedMessage ?: "Password reset failed."
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Forgot password?", color = CyanAccent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// HOME SCREEN
// -------------------------------------------------------------------------
@Composable
private fun HomeScreen(
    overlayOn: Boolean,
    onAnalyze: () -> Unit,
    onChat: () -> Unit,
    onLearn: () -> Unit,
    onCapture: () -> Unit,
    onSetup: () -> Unit,
    latestIncident: IncidentRecord?,
    userName: String
) {
    val scanlineColor = Palette.signal.copy(alpha = 0.035f)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Welcome, $userName", fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.headlineMedium.fontFamily, color = TextPrimary)
                Text("Keep a clear head when a message feels urgent.", fontSize = 13.sp, color = TextSecondary)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(Corner.extraLarge),
                colors = CardDefaults.cardColors(containerColor = NeuralCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.extraLarge))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(NeuralCard, NeuralBgSecondary, NeuralBg)))
                        .drawBehind {
                            val step = 14.dp.toPx()
                            var y = 0f
                            while (y < size.height) {
                                drawLine(scanlineColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                                y += step
                            }
                        }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LottieIcon(R.raw.shield_motion, "TrustGuard shield", size = 96.dp, tint = Palette.signal)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("FIELD DESK  /  READY", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
                        Text("Pause. Check. Then act.", color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.headlineMedium.fontFamily, fontSize = 21.sp, textAlign = TextAlign.Center)
                        Text("Review suspicious messages, payment requests, links, or screenshots before you respond.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
                    }
                }
            }
        }

        item {
            Text("YOUR NEXT CHECK", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(8.dp))
            Card(
                onClick = onAnalyze,
                shape = RoundedCornerShape(Corner.large),
                colors = CardDefaults.cardColors(containerColor = CyanAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(Corner.small))
                            .background(NeuralBg.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Radar, "Analyze content", tint = NeuralBg, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Analyze Content", color = NeuralBg, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Inspect text, links, or messages", color = NeuralBg.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.ArrowForward, "Continue to analysis", tint = NeuralBg)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCapture,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(Corner.medium),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeuralOutline, NeuralOutline)))
                ) {
                    Icon(Icons.Outlined.ScreenshotMonitor, "Capture screen", modifier = Modifier.size(18.dp), tint = CyanAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Capture Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onChat,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(Corner.medium),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeuralOutline, NeuralOutline)))
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, "Open AI assistant", modifier = Modifier.size(18.dp), tint = CyanAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("AI Assistant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Card(
                onClick = onLearn,
                colors = CardDefaults.cardColors(containerColor = NeuralBgSecondary),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Lightbulb, "Scam awareness tips", tint = WarningAmber, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Scam Awareness Quiz & Tips", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Learn to identify OTP theft, phishing, and fake job offers.", color = TextSecondary, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, "Open", tint = TextSecondary)
                }
            }
        }

        item {
            if (latestIncident != null) {
                Text("RECENT RECORD", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(8.dp))
                IncidentCard(latestIncident.id, latestIncident.category, latestIncident.risk, latestIncident.status)
            }
        }
    }
}

// -------------------------------------------------------------------------
// SCAN / ANALYZE SCREEN
// -------------------------------------------------------------------------
@Composable
private fun ScanScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onCapture: () -> Unit,
    onAnalyze: () -> Unit,
    onSetup: () -> Unit,
    crossCheck: Boolean,
    onCrossCheckChange: (Boolean) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Analyze Content", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Submit text or URLs to Neural Shield for risk screening.", color = TextSecondary, fontSize = 13.sp)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeuralCard),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Radar, "Suspicious message or link", tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Text("Suspicious Message or Link", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { onTextChange(it) }
                            }
                        ) {
                            Text("PASTE", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        placeholder = { Text("Paste suspicious text, link, or message here…", color = TextSecondary) },
                        colors = neuralTextFieldColors(),
                        shape = RoundedCornerShape(Corner.medium),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = EvidenceMono),
                    )

                    Button(
                        onClick = onAnalyze,
                        enabled = text.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(Corner.small),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NeuralBg)
                    ) {
                        Icon(Icons.Outlined.Radar, "Start content analysis", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze with Neural Shield", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Corner.small))
                            .background(NeuralBgSecondary)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Official Web Cross-Check", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Optional SerpApi search against official domains", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = crossCheck, onCheckedChange = onCrossCheckChange, colors = SwitchDefaults.colors(checkedTrackColor = CyanAccent, checkedThumbColor = NeuralBg, uncheckedTrackColor = NeuralCard, uncheckedThumbColor = TextSecondary, uncheckedBorderColor = NeuralOutline))
                    }
                }
            }
        }

        item {
            Card(
                onClick = onCapture,
                colors = CardDefaults.cardColors(containerColor = NeuralCard),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.ScreenshotMonitor, "Capture and analyze screen", tint = CyanAccent, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Capture & Analyze Screen", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Android system permission is requested each time", color = TextSecondary, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, "Open capture", tint = TextSecondary)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// ANALYSIS RESULTS SCREEN
// -------------------------------------------------------------------------
@Composable
private fun AnalysisScreen(
    input: String,
    result: AnalysisResponse,
    onSaveIncident: () -> Unit,
    onBack: () -> Unit,
    onPrepareReport: () -> Unit
) {
    val context = LocalContext.current
    val normalizedRisk = result.risk.trim().uppercase()
    var verdictAppeared by remember(result.risk) { mutableStateOf(false) }
    LaunchedEffect(result.risk) { verdictAppeared = true }
    val stampScale by animateFloatAsState(if (verdictAppeared) 1f else 0.78f, animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "verdict-stamp-scale")
    val stampAngle by animateFloatAsState(if (verdictAppeared) -2f else 3f, animationSpec = tween(Motion.reveal, easing = FastOutSlowInEasing), label = "verdict-stamp-angle")
    val failed = result.mode == "error" || normalizedRisk == "ANALYSIS FAILED"
    val high = normalizedRisk == "HIGH" || normalizedRisk == "HIGH RISK"
    val medium = normalizedRisk == "MEDIUM" || normalizedRisk == "SUSPICIOUS" || normalizedRisk == "INCONCLUSIVE"
    val noObviousThreat = normalizedRisk == "LOW" || normalizedRisk == "NO OBVIOUS THREAT DETECTED"

    val riskColor = when {
        normalizedRisk == "ANALYSIS FAILED" -> RiskInconclusive
        failed || high -> DangerRed
        normalizedRisk == "SUSPICIOUS" || normalizedRisk == "MEDIUM" -> WarningAmber
        normalizedRisk == "NO OBVIOUS THREAT DETECTED" || normalizedRisk == "LOW" -> SuccessGreen
        normalizedRisk == "INCONCLUSIVE" -> RiskInconclusive
        else -> RiskInconclusive
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val animation = when {
                    normalizedRisk == "…" -> R.raw.scanning
                    failed -> R.raw.empty
                    high -> R.raw.risk_high
                    medium -> R.raw.risk_suspicious
                    noObviousThreat -> R.raw.risk_safe
                    else -> R.raw.empty
                }
                LottieIcon(animation, "${result.risk} screening verdict", size = 64.dp, tint = riskColor, repeat = normalizedRisk == "…")
                Column {
                    Text("Screening Results", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Model: ${result.model}", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeuralCard),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, riskColor.copy(alpha = 0.6f), RoundedCornerShape(Corner.large))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.graphicsLayer { scaleX = stampScale; scaleY = stampScale; rotationZ = stampAngle }
                            .border(1.dp, riskColor, RoundedCornerShape(Corner.stamp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val verdictIcon = when {
                            failed -> Icons.Outlined.HelpOutline
                            high -> Icons.Outlined.GppBad
                            medium -> Icons.Outlined.GppMaybe
                            noObviousThreat -> Icons.Outlined.VerifiedUser
                            else -> Icons.Outlined.HelpOutline
                        }
                        Icon(verdictIcon, "${result.risk} verdict", tint = riskColor, modifier = Modifier.size(18.dp))
                        Text(result.risk.uppercase(), color = riskColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                    Text(result.category, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(result.disclaimer, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }

        if (!failed && result.uncertainty.isNotBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.medium)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("UNCERTAINTY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(result.uncertainty, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }

        if (!failed) {
            item { Text("DETECTED INDICATORS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp) }
            items(result.indicators) { SignalRow(it) }

            item { Text("RECOMMENDED ACTIONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp) }
            items(result.advice) { AdviceRow(it) }

            if (result.crossCheckSummary.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.medium)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Official Sources Cross-Check", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(result.crossCheckSummary, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                            result.sources.forEach { source ->
                                Text(source.title, color = TextPrimary, fontSize = 11.sp)
                                Text(source.url, color = CyanAccent, fontFamily = EvidenceMono, fontSize = 10.sp, modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) } })
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!failed) {
                    Button(
                        onClick = onPrepareReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(Corner.small),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NeuralBg)
                    ) {
                        Text("Prepare Report Summary", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSaveIncident,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(Corner.small),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeuralOutline, NeuralOutline)))
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, "Save analysis", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Private Record", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Analyze Another Message", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// AI CHAT ASSISTANT SCREEN
// -------------------------------------------------------------------------
@Composable
private fun ChatScreen(
    lines: List<ChatLine>,
    input: String,
    onInputChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    webSearch: Boolean,
    onWebSearchChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(lines.size, busy) {
        if (lines.isNotEmpty() || busy) listState.animateScrollToItem((lines.size - 1 + if (busy) 1 else 0).coerceAtLeast(0))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(NeuralBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("TrustGuard AI Assistant", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Ask safety questions, check link safety, or get fraud guidance.", color = TextSecondary, fontSize = 12.sp)
        }

        if (lines.isEmpty()) {
            Text("SUGGESTED QUESTIONS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(onClick = { onSuggestionSelected("Is this SMS link a scam? How can I check safely?") }, label = { Text("Check SMS Link", color = TextPrimary, fontSize = 11.sp) })
                SuggestionChip(onClick = { onSuggestionSelected("What should I do immediately if I accidentally shared my OTP?") }, label = { Text("Shared OTP Advice", color = TextPrimary, fontSize = 11.sp) })
                SuggestionChip(onClick = { onSuggestionSelected("I lost money on UPI. What should I do right now, including calling 1930?") }, label = { Text("Lost money on UPI", color = TextPrimary, fontSize = 11.sp) })
            }
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lines.size) { index ->
                val line = lines[index]
                val isUser = line.role == "user"
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 320.dp),
                        shape = RoundedCornerShape(Corner.large),
                        colors = CardDefaults.cardColors(containerColor = if (isUser) NeuralBgSecondary else NeuralCard),
                        border = if (!isUser) ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeuralOutline, NeuralOutline))) else null
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!isUser) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.Psychology, "TrustGuard assistant", tint = CyanAccent, modifier = Modifier.size(15.dp))
                                    Text("TRUSTGUARD AI", color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                            Text(line.text, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                            if (!isUser && line.sources.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("SOURCES", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    line.sources.forEach { source ->
                                        Text(source.title.ifBlank { source.url }, color = TextPrimary, fontSize = 11.sp)
                                        if (source.title.isNotBlank()) {
                                            Text(source.url, color = CyanAccent, fontFamily = EvidenceMono, fontSize = 10.sp, modifier = Modifier.clickable {
                                                val uri = Uri.parse(source.url)
                                                if (uri.scheme == "https" || uri.scheme == "http") runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyanAccent, strokeWidth = 2.dp)
                        Text("TrustGuard AI is analyzing…", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        if (error != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Corner.small))
                    .background(DangerRed.copy(alpha = 0.15f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.ErrorOutline, "Message could not be sent", tint = DangerRed, modifier = Modifier.size(18.dp))
                Text(error, color = TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("Retry", color = CyanAccent, fontWeight = FontWeight.Bold) }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Search Official Web", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Query SerpApi for current sources", color = TextSecondary, fontSize = 10.sp)
            }
            Switch(checked = webSearch, onCheckedChange = onWebSearchChange, colors = SwitchDefaults.colors(checkedTrackColor = CyanAccent, checkedThumbColor = NeuralBg, uncheckedTrackColor = NeuralCard, uncheckedThumbColor = TextSecondary, uncheckedBorderColor = NeuralOutline))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask anything…", color = TextSecondary) },
                colors = neuralTextFieldColors(),
                shape = RoundedCornerShape(Corner.large),
                maxLines = 3
            )
            FilledIconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !busy,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CyanAccent, disabledContainerColor = NeuralCard)
            ) {
                Icon(Icons.Outlined.Send, contentDescription = "Send", tint = NeuralBg)
            }
        }
    }
}

// -------------------------------------------------------------------------
// SCAM AWARENESS / LEARN SCREEN
// -------------------------------------------------------------------------
@Composable
private fun LearnScreen() {
    val topics = listOf(
        LearnTopic("Phishing SMS & Links", "Scammers send fake bank or package delivery links urging immediate action. Never click unexpected links.", Icons.Outlined.Link, DangerRed),
        LearnTopic("OTP & PIN Theft", "No bank or official authority will ever ask for your OTP or UPI PIN. Never share it with anyone.", Icons.Outlined.VpnKey, WarningAmber),
        LearnTopic("Fake Job Offers", "Part-time rating or online job offers asking for advance fees are 100% scams.", Icons.Outlined.WorkOutline, CyanAccent),
        LearnTopic("Impersonation Fraud", "Scammers impersonate police or government officials demanding payment. Verify independently.", Icons.Outlined.Badge, CyanAccent)
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Scam Awareness", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Learn common fraud tactics and how to protect yourself.", color = TextSecondary, fontSize = 13.sp)
            }
        }

        items(topics) { topic ->
            Card(
                colors = CardDefaults.cardColors(containerColor = NeuralCard),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(topic.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(topic.icon, topic.title, tint = topic.color, modifier = Modifier.size(20.dp))
                        }
                        Text(topic.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(topic.desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

private data class LearnTopic(val title: String, val desc: String, val icon: ImageVector, val color: Color)

// -------------------------------------------------------------------------
// ACTIVITY LOG / HISTORY SCREEN
// -------------------------------------------------------------------------
@Composable
private fun ActivityScreen(incidents: List<IncidentRecord>, lastMessage: String?) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saved Records", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Private analysis history saved locally on this device.", color = TextSecondary, fontSize = 13.sp)
            }
        }

        if (incidents.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeuralCard),
                    shape = RoundedCornerShape(Corner.large),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LottieIcon(R.raw.empty, "No saved records", size = 56.dp, tint = Palette.signal)
                        Text("No Saved Records Yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Saved screening notes will appear here.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        items(incidents, key = { it.id }) { IncidentCard(it.id, it.category, it.risk, it.status) }
    }
}

// -------------------------------------------------------------------------
// PRIVACY & SETTINGS SCREEN
// -------------------------------------------------------------------------
@Composable
private fun PrivacyScreen(
    userEmail: String,
    maskPii: Boolean,
    onMaskChange: (Boolean) -> Unit,
    overlayEnabled: Boolean,
    onOverlayChange: (Boolean) -> Unit,
    onSetup: () -> Unit,
    onAccessibilitySettings: () -> Unit,
    accessibilityEnabled: Boolean,
    onLogout: () -> Unit,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Privacy & Settings", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Account security and accessibility controls.", color = TextSecondary, fontSize = 13.sp)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeuralCard),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(CyanAccent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, "Authenticated user", tint = CyanAccent, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Authenticated User", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(userEmail, color = TextSecondary, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(Corner.small)) {
                        Text("Logout", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            PrivacySetting("Mask Personal Data", "Hide phone numbers and emails before AI analysis.", maskPii, onMaskChange)
            Spacer(Modifier.height(10.dp))
            PrivacySetting("Floating Shortcut", "Display shortcut overlay button over other apps.", overlayEnabled, onOverlayChange)
            Spacer(Modifier.height(10.dp))
            PrivacySetting("Dark Forensic Noir", "Use the default ink moss appearance. Turn off for warm paper.", darkTheme, onDarkThemeChange)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeuralCard),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Accessibility Service", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Optional permission to assist reading onscreen text on explicit command.", color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                    OutlinedButton(onClick = onAccessibilitySettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Corner.small)) {
                        Text(if (accessibilityEnabled) "Review Android Settings" else "Open Android Accessibility Settings", color = CyanAccent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(
    overlayEnabled: Boolean,
    onEnableOverlay: () -> Unit,
    onCapture: () -> Unit,
    accessibilityEnabled: Boolean,
    onAccessibilitySettings: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Setup Protection", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Configure optional Android safety permissions.", color = TextSecondary, fontSize = 13.sp)
            }
        }
        item { SetupCard("01", "Floating Shortcut", "Shows quick-access bubble over other apps.", if (overlayEnabled) "Enabled" else "Allow Overlay", overlayEnabled, onEnableOverlay) }
        item { SetupCard("02", "Screen Capture", "Requires Android consent prompt before every screenshot.", "Capture with Consent", false, onCapture) }
        item { SetupCard("03", "Accessibility Assist", "Optional accessibility text reader.", if (accessibilityEnabled) "Enabled in Settings" else "Review Settings", accessibilityEnabled, onAccessibilitySettings) }
    }
}

@Composable
private fun CapturePreviewScreen(preview: ByteArray?, piiMasking: Boolean, onAnalyze: () -> Unit, onDiscard: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Review Captured Screen", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Image remains strictly local until analyzed.", color = TextSecondary, fontSize = 12.sp)
                if (preview != null) LottieIcon(R.raw.success, "Screen capture ready for review", size = 44.dp, tint = Palette.safe)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.large)) {
                val bitmap = remember(preview) { preview?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
                if (bitmap != null) Image(bitmap, contentDescription = "Captured Screen", modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).padding(8.dp))
                else Text("Preview unavailable.", Modifier.padding(18.dp), color = TextSecondary)
            }
        }
        item {
            Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(Corner.small), colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NeuralBg)) {
                Text("Analyze Screen", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReportScreen(summary: String, onSummaryChange: (String) -> Unit, onOpenOfficialPortal: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Report Summary", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Prepare notes and evidence before contacting the official service.", color = TextSecondary, fontSize = 12.sp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.large), modifier = Modifier.border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NEXT STEPS", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text("1. Call 1930 promptly for financial cyber fraud.", color = TextPrimary, fontSize = 13.sp)
                    Text("2. Open cybercrime.gov.in to file a report.", color = TextPrimary, fontSize = 13.sp)
                    Text("3. Keep original messages, screenshots, UPI IDs, and transaction references.", color = TextPrimary, fontSize = 13.sp)
                    Text("Prepared locally; not submitted.", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            OutlinedTextField(value = summary, onValueChange = onSummaryChange, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), colors = neuralTextFieldColors(), shape = RoundedCornerShape(Corner.medium), textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = EvidenceMono))
        }
        item {
            Button(onClick = { clipboard.setText(AnnotatedString(summary)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(Corner.small), colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NeuralBg)) {
                Text("Copy Summary", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenOfficialPortal, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(Corner.small)) {
                Text("Open Cybercrime Portal", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------------------
// HELPER COMPONENTS
// -------------------------------------------------------------------------
@Composable
private fun BrandMark(size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(size.dp * 0.28f)).background(CyanAccent), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.trustguard_mark), contentDescription = "TrustGuard verified shield logo", tint = NeuralBg, modifier = Modifier.size((size * 0.7f).dp))
    }
}

@Composable
private fun RowScope.RootTab(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, action: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(Corner.small))
            .clickable(onClick = action)
            .heightIn(min = 48.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) CyanAccent else TextSecondary, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) CyanAccent else TextSecondary, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun PrivacySetting(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.large), modifier = Modifier.border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Shield, "Privacy setting", tint = CyanAccent, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = CyanAccent, checkedThumbColor = NeuralBg, uncheckedTrackColor = NeuralCard, uncheckedThumbColor = TextSecondary, uncheckedBorderColor = NeuralOutline))
        }
    }
}

@Composable
private fun SetupCard(number: String, title: String, body: String, actionLabel: String, enabled: Boolean, action: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.large), modifier = Modifier.border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$number. $title", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(body, color = TextSecondary, fontSize = 11.sp)
            OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Corner.small)) {
                Text(actionLabel, color = CyanAccent, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SignalRow(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corner.small))
            .background(NeuralCard)
            .border(1.dp, NeuralOutline, RoundedCornerShape(Corner.small))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.WarningAmber, "Potential scam indicator", tint = WarningAmber, modifier = Modifier.size(18.dp))
        Text(text, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun AdviceRow(text: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.CheckCircleOutline, "Recommended action", tint = SuccessGreen, modifier = Modifier.size(18.dp))
        Text(text, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun IncidentCard(id: String, category: String, risk: String, status: String) {
    Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.medium), modifier = Modifier.border(1.dp, NeuralOutline, RoundedCornerShape(Corner.medium))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.BookmarkBorder, "Saved incident", tint = CyanAccent, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("$id · $status", color = TextSecondary, fontSize = 10.sp)
            }
            Text(risk.uppercase(), color = if (risk.equals("High", true)) DangerRed else CyanAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun neuralTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = NeuralOutline,
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = CyanAccent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

private fun maskPersonalData(input: String): String = input
    .replace(Regex("\\b(?:gsk|sk|ghp|gho|github_pat)_[A-Za-z0-9_-]{12,}\\b", RegexOption.IGNORE_CASE), "[API KEY]")
    .replace(Regex("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}"), "[EMAIL]")
    .replace(Regex("(?<!\\d)(?:\\+?\\d[ -]?){9,15}(?!\\d)"), "[PHONE OR NUMBER]")

@Preview(name = "Forensic Noir · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class ForensicDarkPreview

@Preview(name = "Warm Paper · light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
annotation class WarmPaperPreview

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    ScamChainTheme(darkTheme = isSystemInDarkTheme(), content = content)
}

@ForensicDarkPreview @WarmPaperPreview @Composable private fun SplashScreenPreview() = PreviewTheme { SplashScreen {} }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun OnboardingScreenPreview() = PreviewTheme { OnboardingScreen {} }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun LoginScreenPreview() = PreviewTheme { LoginScreen(null) {} }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun HomeScreenPreview() = PreviewTheme { HomeScreen(false, {}, {}, {}, {}, {}, null, "Asha") }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ScanScreenPreview() = PreviewTheme { ScanScreen("", {}, {}, {}, {}, false, {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun AnalysisScreenPreview() = PreviewTheme {
    AnalysisScreen("Example message", AnalysisResponse("SUSPICIOUS", "Payment request", listOf("Urgent payment language"), listOf("Verify with the sender through a known number.")), {}, {}, {})
}
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ChatScreenPreview() = PreviewTheme { ChatScreen(emptyList(), "", {}, false, null, false, {}, {}, {}, {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun LearnScreenPreview() = PreviewTheme { LearnScreen() }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ActivityScreenPreview() = PreviewTheme { ActivityScreen(emptyList(), null) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun PrivacyScreenPreview() = PreviewTheme { PrivacyScreen("asha@example.in", true, {}, false, {}, {}, {}, false, {}, isSystemInDarkTheme(), {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun SetupScreenPreview() = PreviewTheme { SetupScreen(false, {}, {}, false, {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun CapturePreviewScreenPreview() = PreviewTheme { CapturePreviewScreen(null, true, {}, {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ReportScreenPreview() = PreviewTheme { ReportScreen("TrustGuard analysis notes", {}, {}) }
