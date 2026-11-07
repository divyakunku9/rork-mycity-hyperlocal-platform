package com.sheharsaathi.app.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sheharsaathi.app.data.Cities
import com.sheharsaathi.app.data.City
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.data.LocationService
import com.sheharsaathi.app.data.OtpChannel
import com.sheharsaathi.app.data.OtpSendResult
import com.sheharsaathi.app.data.OtpTarget
import com.sheharsaathi.app.ui.components.rememberPressScale
import com.sheharsaathi.app.ui.navigation.AppNavigation
import com.sheharsaathi.app.ui.theme.CityColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Stage { SPLASH, SIGN_IN, OTP, CITY, MAIN }

/** Splash → sign in → OTP → city → the main app. Signed-in users go straight to their city. */
@Composable
fun AppRoot() {
    val vm: CityViewModel = viewModel()
    var splashDone by rememberSaveable { mutableStateOf(false) }
    var target by remember { mutableStateOf<OtpTarget?>(null) }
    var demoCode by remember { mutableStateOf<String?>(null) }
    var pendingName by rememberSaveable { mutableStateOf("") }

    val stage = when {
        !splashDone -> Stage.SPLASH
        !vm.signedIn -> if (target == null) Stage.SIGN_IN else Stage.OTP
        !vm.hasCity || vm.pickingCity -> Stage.CITY
        else -> Stage.MAIN
    }

    LaunchedEffect(Unit) {
        delay(1400)
        splashDone = true
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            if (initialState == Stage.SPLASH) {
                fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.97f) togetherWith fadeOut(tween(300))
            } else {
                (slideInHorizontally(tween(380)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(380))) togetherWith
                    (slideOutHorizontally(tween(320)) { if (forward) -it / 5 else it / 5 } + fadeOut(tween(260)))
            }
        },
        label = "root",
        modifier = Modifier.fillMaxSize().background(CityColors.Canvas),
    ) { s ->
        when (s) {
            Stage.SPLASH -> SplashScreen()
            Stage.SIGN_IN -> SignInScreen(
                vm = vm,
                initialName = pendingName,
                onSent = { t, code, name -> pendingName = name; demoCode = code; target = t },
            )
            Stage.OTP -> OtpScreen(
                vm = vm,
                target = target ?: return@AnimatedContent,
                demoCode = demoCode,
                onResent = { demoCode = it },
                onBack = { target = null },
                onVerified = {
                    val t = target ?: return@OtpScreen
                    vm.completeSignIn(t)
                    if (pendingName.isNotBlank()) vm.updateProfile(vm.profile.copy(name = pendingName.trim()))
                    target = null
                },
            )
            Stage.CITY -> ChooseCityScreen(vm)
            Stage.MAIN -> AppNavigation(vm)
        }
    }
}

// ---------------------------------------------------------------- Splash

@Composable
private fun SplashScreen() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val logoScale by animateFloatAsState(if (shown) 1f else 0.55f, spring(dampingRatio = 0.55f, stiffness = 260f), label = "logo")
    val textAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(700, delayMillis = 250), label = "text")

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF1E2A4A), CityColors.Canvas), radius = 1400f),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(Modifier.scale(logoScale), size = 88)
            Spacer(Modifier.height(22.dp))
            Text(
                "Shehar Saathi", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black,
                color = CityColors.Ink, modifier = Modifier.graphicsLayer { alpha = textAlpha },
            )
            Text(
                "Your city, live in your pocket", style = MaterialTheme.typography.bodyLarge, color = CityColors.Muted,
                modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = textAlpha; translationY = (1 - textAlpha) * 24f },
            )
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier, size: Int = 56) {
    Box(
        modifier.size(size.dp).clip(RoundedCornerShape((size * 0.3f).dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFF9A3D), CityColors.Saffron, Color(0xFFE2571F)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.LocationCity, contentDescription = "Shehar Saathi", tint = Color.White, modifier = Modifier.size((size * 0.55f).dp))
    }
}

// ---------------------------------------------------------------- Sign in

@Composable
private fun SignInScreen(vm: CityViewModel, initialName: String, onSent: (OtpTarget, String?, String) -> Unit) {
    var channel by rememberSaveable { mutableStateOf(OtpChannel.PHONE) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val phoneOk = phone.length == 10 && phone.first() in '6'..'9'
    val emailOk = Regex("^[^@\\s]+@[^@\\s]+\\.[a-zA-Z]{2,}$").matches(email.trim())
    val valid = name.isNotBlank() && if (channel == OtpChannel.PHONE) phoneOk else emailOk

    fun send() {
        if (!valid || sending) return
        val t = OtpTarget(channel, if (channel == OtpChannel.PHONE) phone else email.trim().lowercase())
        sending = true; error = null
        scope.launch {
            when (val r = vm.otp.send(t)) {
                is OtpSendResult.Sent -> onSent(t, r.demoCode, name)
                is OtpSendResult.Failed -> error = r.message
            }
            sending = false
        }
    }

    Box(Modifier.fillMaxSize().background(CityColors.Canvas)) {
        CityMosaic(Modifier.fillMaxWidth().height(330.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().imePadding()
                .navigationBarsPadding().padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(150.dp))
            BrandMark(size = 56)
            Spacer(Modifier.height(18.dp))
            Text("Sign in to Shehar Saathi", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = CityColors.Ink)
            Text(
                "News, transit, events and civic services for 9 Indian cities.",
                style = MaterialTheme.typography.bodyLarge, color = CityColors.Muted, modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(24.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(OtpChannel.PHONE to "Phone", OtpChannel.EMAIL to "Email").forEachIndexed { i, (c, label) ->
                    SegmentedButton(
                        selected = channel == c,
                        onClick = { channel = c; error = null },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = {
                            Icon(if (c == OtpChannel.PHONE) Icons.Filled.PhoneAndroid else Icons.Filled.Email, null, Modifier.size(18.dp))
                        },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = CityColors.IndigoSoft, activeContentColor = CityColors.IndigoDeep,
                            inactiveContainerColor = CityColors.Surface, inactiveContentColor = CityColors.Muted,
                            activeBorderColor = CityColors.Outline, inactiveBorderColor = CityColors.Outline,
                        ),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it.take(40) },
                label = { Text("Your name") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            AnimatedContent(channel, label = "channel") { c ->
                if (c == OtpChannel.PHONE) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { v -> phone = v.filter { it.isDigit() }.takeLast(12).let { if (it.length > 10 && it.startsWith("91")) it.drop(2) else it }.take(10) },
                        label = { Text("Mobile number") },
                        prefix = { Text("+91  ", color = CityColors.Ink) },
                        singleLine = true,
                        isError = phone.length == 10 && !phoneOk,
                        supportingText = { Text(if (phone.length == 10 && !phoneOk) "Indian mobile numbers start with 6–9" else "We'll text you a 6-digit code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { send() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.trim().take(80) },
                        label = { Text("Email address") },
                        singleLine = true,
                        supportingText = { Text("We'll email you a 6-digit code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { send() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AnimatedVisibility(error != null) {
                Text(error.orEmpty(), color = CityColors.Red, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = if (sending) "Sending code…" else "Send OTP",
                enabled = valid && !sending,
                loading = sending,
                onClick = ::send,
            )
            Text(
                "By continuing you agree to receive a one-time code. Standard SMS rates may apply.",
                style = MaterialTheme.typography.bodySmall, color = CityColors.Muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 24.dp),
            )
        }
    }
}

/** Three city photos fading into the navy canvas behind the sign-in form. */
@Composable
private fun CityMosaic(modifier: Modifier) {
    Box(modifier) {
        Row(Modifier.fillMaxSize()) {
            listOf(Cities.all[0], Cities.all[2], Cities.all[5]).forEach { c ->
                Image(
                    painterResource(c.imageRes), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxSize().graphicsLayer { alpha = 0.55f },
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(CityColors.Canvas.copy(alpha = 0.35f), CityColors.Canvas.copy(alpha = 0.75f), CityColors.Canvas)),
            ),
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = CityColors.Surface,
    focusedContainerColor = CityColors.SurfaceHigh,
    unfocusedBorderColor = CityColors.Outline,
    focusedBorderColor = CityColors.Indigo,
    focusedLabelColor = CityColors.IndigoDeep,
    cursorColor = CityColors.Saffron,
)

@Composable
fun PrimaryButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, loading: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Button(
        onClick = onClick, enabled = enabled, interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CityColors.Saffron, contentColor = Color.White,
            disabledContainerColor = CityColors.SurfaceHigh, disabledContentColor = CityColors.Muted,
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier.fillMaxWidth().scale(scale),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- OTP

@Composable
private fun OtpScreen(
    vm: CityViewModel,
    target: OtpTarget,
    demoCode: String?,
    onResent: (String?) -> Unit,
    onBack: () -> Unit,
    onVerified: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(TextFieldValue("")) }
    var verifying by remember { mutableStateOf(false) }
    var wrong by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(30) }
    var resendKey by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(resendKey) {
        secondsLeft = 30
        while (secondsLeft > 0) { delay(1000); secondsLeft-- }
    }
    LaunchedEffect(Unit) { delay(350); runCatching { focus.requestFocus() } }

    fun verify(c: String) {
        if (verifying || c.length < 6) return
        verifying = true; wrong = false
        scope.launch {
            val ok = vm.otp.verify(target, c)
            if (ok) {
                success = true
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                delay(550)
                onVerified()
            } else {
                wrong = true
                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) shake.animateTo(x, tween(55))
                code = TextFieldValue("")
            }
            verifying = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(CityColors.Canvas).statusBarsPadding().imePadding().navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp).padding(top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CityColors.Ink)
        }
        Spacer(Modifier.height(12.dp))
        Text("Enter the 6-digit code", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CityColors.Ink)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text("Sent to ", style = MaterialTheme.typography.bodyLarge, color = CityColors.Muted)
            Text(target.display, style = MaterialTheme.typography.bodyLarge, color = CityColors.Ink, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onBack) { Text("Edit", color = CityColors.Indigo) }
        }

        if (vm.otp.isDemo && demoCode != null) {
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(CityColors.SaffronSoft).border(1.dp, CityColors.Saffron.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable(onClickLabel = "Fill code") { code = TextFieldValue(demoCode, TextRange(6)); verify(demoCode) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Science, null, tint = CityColors.SaffronText)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Demo mode: no SMS or email is sent yet", style = MaterialTheme.typography.labelLarge, color = CityColors.SaffronText)
                    Text("Your code is $demoCode. Tap to fill.", style = MaterialTheme.typography.bodyMedium, color = CityColors.Ink)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        BasicTextField(
            value = code,
            onValueChange = { v ->
                val digits = v.text.filter { it.isDigit() }.take(6)
                code = TextFieldValue(digits, TextRange(digits.length))
                wrong = false
                if (digits.length == 6) verify(digits)
            },
            enabled = !verifying && !success,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                .semantics { contentDescription = "One-time code, 6 digits" }
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) },
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(6) { i ->
                        val ch = code.text.getOrNull(i)
                        val active = i == code.text.length && !verifying
                        val border = when {
                            success -> CityColors.Green
                            wrong -> CityColors.Red
                            active -> CityColors.Saffron
                            ch != null -> CityColors.Indigo
                            else -> CityColors.Outline
                        }
                        val lift by animateDpAsState(if (ch != null) (-2).dp else 0.dp, spring(Spring.DampingRatioMediumBouncy), label = "lift")
                        Box(
                            Modifier.weight(1f).aspectRatio(0.82f).offset(y = lift).clip(RoundedCornerShape(14.dp))
                                .background(if (success) CityColors.GreenSoft else CityColors.Surface)
                                .border(if (active || wrong || success) 2.dp else 1.dp, border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(ch, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() }, label = "digit") { d ->
                                Text(d?.toString() ?: "", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CityColors.Ink)
                            }
                        }
                    }
                }
            },
        )

        Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(28.dp)) {
            when {
                success -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = CityColors.GreenText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Verified", color = CityColors.GreenText, style = MaterialTheme.typography.labelLarge)
                }
                verifying -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = CityColors.Indigo)
                    Spacer(Modifier.width(8.dp))
                    Text("Checking…", color = CityColors.Muted, style = MaterialTheme.typography.labelLarge)
                }
                wrong -> Text(vm.lastSignInError ?: "That code didn't match. Try again.", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.weight(1f))
        if (secondsLeft > 0) {
            Text(
                "Resend code in 0:${secondsLeft.toString().padStart(2, '0')}",
                color = CityColors.Muted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), textAlign = TextAlign.Center,
            )
        } else {
            TextButton(
                onClick = {
                    scope.launch {
                        val r = vm.otp.send(target)
                        if (r is OtpSendResult.Sent) onResent(r.demoCode)
                        resendKey++
                        code = TextFieldValue("")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) { Text("Resend code", color = CityColors.Indigo, style = MaterialTheme.typography.titleSmall) }
        }
    }
}

// ---------------------------------------------------------------- Choose city

@Composable
fun ChooseCityScreen(vm: CityViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(if (vm.hasCity) vm.city else null) }
    var detecting by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val changing = vm.pickingCity
    if (changing) BackHandler { vm.pickingCity = false }

    fun detect() {
        if (!LocationService.isLocationOn(context)) { note = "Location is off. Pick your city below."; return }
        detecting = true; note = null
        scope.launch {
            val loc = LocationService.currentLocation(context)
            detecting = false
            if (loc == null) { note = "Couldn't get a GPS fix. Pick your city below."; return@launch }
            val c = Cities.nearest(loc.latitude, loc.longitude)
            selected = c
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            note = if (c.contains(loc.latitude, loc.longitude)) "You're in ${c.name}." else "Nearest supported city: ${c.name}."
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) detect() else note = "Location permission denied. Pick your city below."
    }

    Box(Modifier.fillMaxSize().background(CityColors.Canvas)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column(Modifier.padding(top = 8.dp, bottom = 6.dp)) {
                    if (changing) {
                        IconButton(onClick = { vm.pickingCity = false }, modifier = Modifier.offset(x = (-12).dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CityColors.Ink)
                        }
                    } else Spacer(Modifier.height(20.dp))
                    Text(
                        if (changing) "Switch city" else "Hi ${vm.profile.firstName}, where do you live?",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CityColors.Ink,
                    )
                    Text(
                        "Your map, transit, civic services, events and weather follow the city you pick.",
                        style = MaterialTheme.typography.bodyLarge, color = CityColors.Muted, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CityColors.IndigoSoft)
                            .border(1.dp, CityColors.Indigo.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable(enabled = !detecting) {
                                if (LocationService.hasPermission(context)) detect()
                                else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (detecting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = CityColors.IndigoDeep)
                        else Icon(Icons.Filled.MyLocation, null, tint = CityColors.IndigoDeep)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (detecting) "Detecting…" else "Detect my city", style = MaterialTheme.typography.titleSmall, color = CityColors.IndigoDeep)
                            Text(note ?: "Uses GPS to pick the nearest supported city", style = MaterialTheme.typography.bodySmall, color = CityColors.Muted)
                        }
                    }
                }
            }
            itemsIndexed(Cities.all, key = { _, c -> c.id }) { i, c ->
                CityPhotoCard(c, index = i, selected = selected?.id == c.id) {
                    selected = c
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            }
        }

        // Bottom action
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, CityColors.Canvas.copy(alpha = 0.96f), CityColors.Canvas)))
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
        ) {
            PrimaryButton(
                text = selected?.let { "Continue to ${it.name}" } ?: "Pick a city",
                enabled = selected != null,
            ) {
                val c = selected ?: return@PrimaryButton
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                if (c.id == vm.city.id && vm.hasCity) vm.pickingCity = false else vm.selectCity(c)
            }
        }
    }
}

@Composable
private fun CityPhotoCard(c: City, index: Int, selected: Boolean, onClick: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60L * index); appeared = true }
    val alpha by animateFloatAsState(if (appeared) 1f else 0f, tween(420), label = "a")
    val rise by animateFloatAsState(if (appeared) 0f else 40f, tween(420), label = "y")
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressScale(interaction)
    val sel by animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "sel")

    Box(
        Modifier.aspectRatio(0.78f)
            .graphicsLayer { this.alpha = alpha; translationY = rise; scaleX = press; scaleY = press }
            .clip(RoundedCornerShape(22.dp))
            .border(if (selected) 2.5.dp else 1.dp, if (selected) CityColors.Saffron else CityColors.GlassBorder, RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .semantics { contentDescription = "${c.name}, ${c.state}" + if (selected) ", selected" else "" },
    ) {
        Image(
            painterResource(c.imageRes), null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1f + sel * 0.06f; scaleY = 1f + sel * 0.06f },
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color(0xE60F172A))))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(c.tagline, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), maxLines = 2)
        }
        AnimatedVisibility(
            selected, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(CityColors.Saffron), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
