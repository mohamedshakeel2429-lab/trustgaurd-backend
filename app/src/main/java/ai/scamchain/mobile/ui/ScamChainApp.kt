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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
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
                requireNotNull(FirebaseApp.initializeApp(appContext)) {
                    "Firebase is not configured. Check the local google-services.json file."
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
    var incidentError by remember { mutableStateOf(false) }
    var pendingImage by remember { mutableStateOf<ByteArray?>(null) }
    var reportSummary by remember { mutableStateOf("") }
    val incidents = remember { mutableStateListOf<IncidentRecord>() }
    val chatLines = remember { mutableStateListOf<ChatLine>() }
    val api = remember { RemoteScamChainApi(ai.scamchain.mobile.BuildConfig.API_BASE_URL) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(capturePreview) { if (capturePreview != null) page = Page.PREVIEW }

    val isRoot = page in setOf(Page.HOME, Page.SCAN, Page.CHAT, Page.LEARN, Page.PRIVACY)
    val backgroundViolet = VioletAccent
    val backgroundCyan = CyanAccent

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
                    if (page != Page.PRIVACY) {
                        IconButton(onClick = { page = Page.PRIVACY }) {
                            Icon(Icons.Outlined.Tune, contentDescription = "Open TrustGuard settings", tint = TextSecondary)
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
                    RootTab("Assistant", Icons.Outlined.ChatBubbleOutline, page == Page.CHAT, Modifier.weight(1f)) { page = Page.CHAT }
                    RootTab("Learn", Icons.Outlined.Lightbulb, page == Page.LEARN, Modifier.weight(1f)) { page = Page.LEARN }
                    RootTab("Privacy", Icons.Outlined.Lock, page == Page.PRIVACY, Modifier.weight(1f)) { page = Page.PRIVACY }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(NeuralBg)
                .drawBehind {
                    val glowRadius = size.width * 0.72f
                    drawCircle(
                        Brush.radialGradient(listOf(backgroundViolet.copy(alpha = 0.10f), Color.Transparent), radius = glowRadius),
                        radius = glowRadius,
                        center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    )
                    drawCircle(
                        Brush.radialGradient(listOf(backgroundCyan.copy(alpha = 0.045f), Color.Transparent), radius = glowRadius),
                        radius = glowRadius,
                        center = androidx.compose.ui.geometry.Offset(0f, size.height),
                    )
                }
                .padding(padding)
        ) {
            when (page) {
                Page.HOME -> HomeScreen(
                    overlayOn = overlayGranted(),
                    onAnalyze = { page = Page.SCAN },
                    onChat = { page = Page.CHAT },
                    onLearn = { page = Page.LEARN },
                    onCapture = onRequestCapture,
                    onSetup = { page = Page.SETUP },
                    onActivity = { page = Page.ACTIVITY },
                    latestIncident = incidents.firstOrNull(),
                    userName = firebaseAuth?.currentUser?.displayName?.substringBefore(' ')?.takeIf(String::isNotBlank) ?: "there"
                )
                Page.SCAN -> ScanScreen(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onCapture = onRequestCapture,
                    onAnalyze = { if (messageText.isNotBlank()) { pendingImage = null; analysisGeneration++; page = Page.ANALYSIS } },
                    crossCheck = crossCheck,
                    onCrossCheckChange = { crossCheck = it },
                    piiMasking = maskPii,
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
                Page.ACTIVITY -> ActivityScreen(incidents, incidentMessage, incidentError)
                Page.PRIVACY -> PrivacyScreen(
                    userEmail = firebaseAuth?.currentUser?.email ?: "Email unavailable on this account",
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
                    onRetake = {
                        onCapturePreviewConsumed()
                        page = Page.HOME
                        onRequestCapture()
                    },
                    onDiscard = { onCapturePreviewConsumed(); page = Page.HOME }
                )
                Page.ANALYSIS -> {
                    val analysisText = if (maskPii) maskPersonalData(messageText) else messageText
                    val imageForAnalysis = pendingImage
                    val result by produceState(initialValue = AnalysisResponse("…", "Checking submitted content", emptyList(), emptyList(), "unknown", "loading"), analysisText, crossCheck, analysisGeneration) {
                        value = try { api.analyze(AnalyzeRequest(analysisText, image = imageForAnalysis, crossCheck = crossCheck)) }
                        catch (e: Exception) { AnalysisResponse("Unavailable", "Analysis could not be completed", listOf(e.message ?: "The service did not return an analysis."), emptyList(), "unavailable", "error") }
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
                                try {
                                    val response = api.createIncident(IncidentRequest(result.category, result.risk, "User requested a prepared activity reference"))
                                    val record = IncidentRecord(response.incidentId, result.category, result.risk, response.status)
                                    incidents.add(0, record)
                                    incidentError = false
                                    incidentMessage = "Reference ${record.id} prepared for this session. Nothing was filed or stored by the service."
                                } catch (e: Exception) {
                                    incidentError = true
                                    incidentMessage = e.message ?: "Could not prepare this activity record."
                                }
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
            .nightwatchCanvas(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(1.5.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.trustguard_logo),
                    contentDescription = "TrustGuard Logo",
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
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
        OnboardPage("A second look, before you act", "Check suspicious messages and payment requests before you reply or share a code.", Icons.Outlined.Psychology),
        OnboardPage("Review what you choose", "Paste a message or link, or approve a one-time screenshot for analysis.", Icons.Outlined.Radar),
        OnboardPage("Privacy stays in your hands", "Email and long-number masking starts on. Screen content is never collected in the background.", Icons.Outlined.Shield)
    )
    val page = pages[step]

    Column(
        Modifier
            .fillMaxSize()
            .nightwatchCanvas()
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
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Palette.textOnSignal)
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
    var messageIsError by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .nightwatchCanvas()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.trustguard_logo),
                contentDescription = "TrustGuard Logo",
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
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
                            .selectable(selected = !createAccount, onClick = { createAccount = false; message = null }, role = Role.Tab)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (!createAccount) Palette.textOnSignal else TextPrimary)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Corner.small))
                            .background(if (createAccount) CyanAccent else Color.Transparent)
                            .selectable(selected = createAccount, onClick = { createAccount = true; message = null }, role = Role.Tab)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (createAccount) Palette.textOnSignal else TextPrimary)
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
                            .background((if (messageIsError) DangerRed else SuccessGreen).copy(alpha = 0.14f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (messageIsError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                            contentDescription = if (messageIsError) "Authentication error" else "Password reset status",
                            tint = if (messageIsError) DangerRed else SuccessGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(message!!, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        val auth = firebaseAuth ?: return@Button
                        if (email.isBlank() || password.isBlank() || (createAccount && displayName.isBlank())) {
                            message = "Please complete all required fields."
                            messageIsError = true
                            return@Button
                        }
                        busy = true
                        message = null
                        val task = if (createAccount) auth.createUserWithEmailAndPassword(email.trim(), password)
                        else auth.signInWithEmailAndPassword(email.trim(), password)

                        task.addOnCompleteListener { result ->
                            if (!result.isSuccessful) {
                                busy = false
                                messageIsError = true
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
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Palette.textOnSignal)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Palette.textOnSignal, strokeWidth = 2.dp)
                    else Text(if (createAccount) "Create account" else "Sign in", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (firebaseAuth == null) {
                    Text(
                        "Sign-in is unavailable because Firebase is not configured for this build.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }

                if (!createAccount) {
                    TextButton(
                        enabled = firebaseAuth != null && !busy && email.isNotBlank(),
                        onClick = {
                            firebaseAuth?.sendPasswordResetEmail(email.trim())?.addOnCompleteListener { result ->
                                messageIsError = !result.isSuccessful
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
    onActivity: () -> Unit,
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
                Text(if (userName.isBlank()) "Your safety desk" else "Welcome, $userName", fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.headlineMedium.fontFamily, color = TextPrimary)
                Text("Pause for a second look before you respond.", fontSize = 13.sp, color = TextSecondary)
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
                        .background(Brush.linearGradient(listOf(Palette.elevated, NeuralCard, NeuralBgSecondary)))
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
                        Text("YOUR PRIVATE SAFETY DESK", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp)
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
                        Icon(Icons.Outlined.Radar, "Analyze content", tint = Palette.textOnSignal, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Analyze a message", color = Palette.textOnSignal, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Review text, a link, or a screenshot", color = Palette.textOnSignal.copy(alpha = 0.78f), fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.ArrowForward, "Continue to analysis", tint = Palette.textOnSignal)
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
                    Text("Scam Awareness", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Learn to identify OTP theft, phishing, and fake job offers.", color = TextSecondary, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, "Open", tint = TextSecondary)
                }
            }
        }

        item {
            Card(
                onClick = onSetup,
                colors = CardDefaults.cardColors(containerColor = NeuralBgSecondary),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier.fillMaxWidth().border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large)),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.SettingsSuggest, contentDescription = null, tint = VioletAccent)
                    Column(Modifier.weight(1f)) {
                        Text("Optional Android tools", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (overlayOn) "Shortcut permission is available." else "Set up the floating shortcut or screen capture.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                    Text("SETUP", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        item {
            Card(
                onClick = onActivity,
                colors = CardDefaults.cardColors(containerColor = Palette.elevated),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier.fillMaxWidth().border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Activity", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (latestIncident == null) "No prepared records in this session yet." else "Open this session’s prepared records.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Open activity", tint = TextSecondary)
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
    crossCheck: Boolean,
    onCrossCheckChange: (Boolean) -> Unit,
    piiMasking: Boolean,
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
                Text("Review a message, link, or screenshot before acting.", color = TextSecondary, fontSize = 13.sp)
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

                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Corner.small)).background(NeuralBgSecondary).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(16.dp))
                        Text(
                            if (piiMasking) "Email addresses and long numbers are masked on device before analysis." else "Personal-data masking is off. Remove sensitive details before sending.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }

                    Button(
                        onClick = onAnalyze,
                        enabled = text.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(Corner.small),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Palette.textOnSignal)
                    ) {
                        Icon(Icons.Outlined.Radar, "Start content analysis", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze message", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        Switch(checked = crossCheck, onCheckedChange = onCrossCheckChange, colors = SwitchDefaults.colors(checkedTrackColor = CyanAccent, checkedThumbColor = Palette.textOnSignal, uncheckedTrackColor = NeuralCard, uncheckedThumbColor = TextSecondary, uncheckedBorderColor = NeuralOutline))
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
    val medium = normalizedRisk == "MEDIUM" || normalizedRisk == "SUSPICIOUS"
    val noObviousThreat = normalizedRisk == "LOW" || normalizedRisk == "NO OBVIOUS THREAT DETECTED"

    val riskColor = if (failed) RiskInconclusive else riskAccent(result.risk)

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
                    Text(if (normalizedRisk == "…") "Reviewing your submission" else "Analysis report", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (!failed && normalizedRisk != "…" && result.model.isNotBlank() && result.model != "unknown") {
                        Text("Model · ${result.model}", color = TextSecondary, fontSize = 11.sp, fontFamily = EvidenceMono)
                    }
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
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(riskColor.copy(alpha = 0.10f), NeuralCard, NeuralCard)))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                        Text(if (failed) "NOT COMPLETED" else result.risk.uppercase(), color = riskColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                    Text(result.category, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(result.disclaimer, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }

        if (failed) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeuralBgSecondary),
                    shape = RoundedCornerShape(Corner.medium),
                    modifier = Modifier.border(1.dp, NeuralOutline, RoundedCornerShape(Corner.medium)),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ANALYSIS NOT COMPLETED", color = RiskInconclusive, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(result.indicators.firstOrNull() ?: "The service did not return an analysis. You can edit the message and try again.", color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                    }
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
            if (result.indicators.isEmpty()) {
                item { Text("No indicators were returned. That is not proof the content is legitimate.", color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp) }
            } else {
                items(result.indicators) { SignalRow(it) }
            }

            item { Text("RECOMMENDED ACTIONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp) }
            if (result.advice.isEmpty()) {
                item { Text("No recommended actions were returned.", color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp) }
            } else {
                items(result.advice) { AdviceRow(it) }
            }

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
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Palette.textOnSignal)
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
                        Text("Prepare session reference", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(if (failed) "Edit and try again" else "Analyze another message", color = TextSecondary, fontSize = 13.sp)
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
            Switch(checked = webSearch, onCheckedChange = onWebSearchChange, colors = SwitchDefaults.colors(checkedTrackColor = CyanAccent, checkedThumbColor = Palette.textOnSignal, uncheckedTrackColor = NeuralCard, uncheckedThumbColor = TextSecondary, uncheckedBorderColor = NeuralOutline))
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about scams or online safety…", color = TextSecondary) },
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
                Icon(Icons.Outlined.Send, contentDescription = "Send", tint = Palette.textOnSignal)
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
private fun ActivityScreen(incidents: List<IncidentRecord>, lastMessage: String?, lastMessageIsError: Boolean) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Activity", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Session references only; the service does not store reports.", color = TextSecondary, fontSize = 13.sp)
            }
        }

        if (lastMessage != null) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Corner.medium))
                        .background((if (lastMessageIsError) DangerRed else SuccessGreen).copy(alpha = 0.12f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        if (lastMessageIsError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (lastMessageIsError) DangerRed else SuccessGreen,
                    )
                    Text(lastMessage, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                }
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
                        Text("No activity yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("When you prepare a report reference, it will appear here for this session.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
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
            PrivacySetting("Mask personal data", "Mask emails and long numbers on device before analysis.", maskPii, onMaskChange)
            Spacer(Modifier.height(10.dp))
            PrivacySetting("Floating Shortcut", "Display shortcut overlay button over other apps.", overlayEnabled, onOverlayChange)
            Spacer(Modifier.height(10.dp))
            PrivacySetting("Nightwatch dark appearance", "Choose deep navy or the cool daylight palette.", darkTheme, onDarkThemeChange)
        }

        item {
            Card(
                onClick = onSetup,
                colors = CardDefaults.cardColors(containerColor = Palette.elevated),
                shape = RoundedCornerShape(Corner.large),
                modifier = Modifier.fillMaxWidth().border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large)),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.SettingsSuggest, contentDescription = null, tint = VioletAccent)
                    Column(Modifier.weight(1f)) {
                        Text("Android permissions", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Review shortcut and one-time capture setup.", color = TextSecondary, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Open setup", tint = TextSecondary)
                }
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Accessibility Service", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (accessibilityEnabled) "Enabled in Android, but this build does not read or collect onscreen text." else "This build does not read or collect onscreen text. The Android service is a scaffold only.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                    OutlinedButton(onClick = onAccessibilitySettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Corner.small)) {
                        Text("Review Android accessibility settings", color = CyanAccent, fontSize = 12.sp)
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
        item { SetupCard("03", "Accessibility scaffold", "Does not read or monitor screen text in this build.", "Review Android Settings", accessibilityEnabled, onAccessibilitySettings) }
    }
}

@Composable
private fun CapturePreviewScreen(
    preview: ByteArray?,
    piiMasking: Boolean,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit,
    onDiscard: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Is this the screen you want to check?", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Nothing is sent until you choose Use screenshot. Screenshot pixels are sent as-is; text masking does not cover images.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
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
            Button(onClick = onAnalyze, enabled = preview != null, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(Corner.small), colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Palette.textOnSignal)) {
                Text("Use screenshot and analyze", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(Corner.small)) {
                Text("Retake screenshot", color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(Corner.small)) {
                Text("Discard screenshot", color = TextSecondary)
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
            Button(onClick = { clipboard.setText(AnnotatedString(summary)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(Corner.small), colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Palette.textOnSignal)) {
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
private fun Modifier.nightwatchCanvas(): Modifier {
    val base = NeuralBg
    val cyan = CyanAccent
    val violet = VioletAccent
    return background(base).drawBehind {
        val radius = maxOf(size.width, size.height) * 0.72f
        drawCircle(
            Brush.radialGradient(listOf(violet.copy(alpha = 0.12f), Color.Transparent), radius = radius),
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.08f),
        )
        drawCircle(
            Brush.radialGradient(listOf(cyan.copy(alpha = 0.05f), Color.Transparent), radius = radius),
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.04f, size.height * 0.92f),
        )
    }

}

@Composable
private fun BrandMark(size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(size.dp * 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.trustguard_logo),
            contentDescription = "TrustGuard Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
    }
}

@Composable
private fun RowScope.RootTab(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, action: () -> Unit) {
    val indicator by animateFloatAsState(if (selected) 1f else 0f, animationSpec = tween(Motion.standard), label = "tab-indicator")
    Column(
        modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(Corner.medium))
            .background(if (selected) Palette.signalMuted else Color.Transparent)
            .selectable(selected = selected, onClick = action, role = Role.Tab)
            .heightIn(min = 52.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) CyanAccent else TextSecondary, modifier = Modifier.size(19.dp))
        Text(label, color = if (selected) CyanAccent else TextSecondary, fontSize = 9.sp, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        Spacer(Modifier.width((8 * indicator).dp).height(2.dp).clip(CircleShape).background(CyanAccent))
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
            Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = CyanAccent, checkedThumbColor = Palette.textOnSignal, uncheckedTrackColor = NeuralCard, uncheckedThumbColor = TextSecondary, uncheckedBorderColor = NeuralOutline))
        }
    }
}

@Composable
private fun SetupCard(number: String, title: String, body: String, actionLabel: String, enabled: Boolean, action: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NeuralCard), shape = RoundedCornerShape(Corner.large), modifier = Modifier.border(1.dp, NeuralOutline, RoundedCornerShape(Corner.large))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$number. $title", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (enabled) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Enabled", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                }
            }
            Text(body, color = TextSecondary, fontSize = 11.sp)
            OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Corner.small)) {
                Text(actionLabel, color = if (enabled) TextSecondary else CyanAccent, fontSize = 12.sp)
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
            Text(risk.uppercase(), color = riskAccent(risk), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun riskAccent(risk: String): Color = when {
    risk.contains("HIGH", ignoreCase = true) -> DangerRed
    risk.contains("SUSPICIOUS", ignoreCase = true) || risk.contains("MEDIUM", ignoreCase = true) -> WarningAmber
    risk.contains("LOW", ignoreCase = true) || risk.contains("SAFE", ignoreCase = true) -> SuccessGreen
    else -> RiskInconclusive
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

@Preview(name = "Nightwatch Signal · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class ForensicDarkPreview

@Preview(name = "Daylight · light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
annotation class WarmPaperPreview

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    ScamChainTheme(darkTheme = isSystemInDarkTheme(), content = content)
}

@ForensicDarkPreview @WarmPaperPreview @Composable private fun SplashScreenPreview() = PreviewTheme { SplashScreen {} }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun OnboardingScreenPreview() = PreviewTheme { OnboardingScreen {} }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun LoginScreenPreview() = PreviewTheme { LoginScreen(null) {} }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun HomeScreenPreview() = PreviewTheme { HomeScreen(false, {}, {}, {}, {}, {}, {}, null, "") }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ScanScreenPreview() = PreviewTheme { ScanScreen("", {}, {}, {}, false, {}, true) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun AnalysisScreenPreview() = PreviewTheme {
    AnalysisScreen("Example message", AnalysisResponse("SUSPICIOUS", "Payment request", listOf("Urgent payment language"), listOf("Verify with the sender through a known number.")), {}, {}, {})
}
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ChatScreenPreview() = PreviewTheme { ChatScreen(emptyList(), "", {}, false, null, false, {}, {}, {}, {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun LearnScreenPreview() = PreviewTheme { LearnScreen() }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ActivityScreenPreview() = PreviewTheme { ActivityScreen(emptyList(), null, false) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun PrivacyScreenPreview() = PreviewTheme { PrivacyScreen("asha@example.in", true, {}, false, {}, {}, {}, false, {}, isSystemInDarkTheme(), {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun SetupScreenPreview() = PreviewTheme { SetupScreen(false, {}, {}, false, {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun CapturePreviewScreenPreview() = PreviewTheme { CapturePreviewScreen(null, true, {}, {}, {}) }
@ForensicDarkPreview @WarmPaperPreview @Composable private fun ReportScreenPreview() = PreviewTheme { ReportScreen("TrustGuard analysis notes", {}, {}) }
