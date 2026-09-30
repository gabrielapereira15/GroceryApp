package com.example.gpgrocery.ui.unlock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.R
import com.example.gpgrocery.data.settings.PinCheck
import com.example.gpgrocery.data.settings.StoreSettings
import com.example.gpgrocery.ui.components.CrateMark
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.BricolageDisplay
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UnlockViewModel(private val container: AppContainer) : ViewModel() {
    var entered by mutableStateOf("")
        private set
    var attemptsLeft by mutableStateOf<Int?>(null)
        private set
    var pausedUntil by mutableLongStateOf(0L)
        private set
    var wrongCount by mutableIntStateOf(0)
        private set
    private var checking = false

    fun press(digit: Char) {
        if (checking || entered.length >= 4 || pausedUntil > System.currentTimeMillis()) return
        entered += digit
        if (entered.length == 4) check()
    }

    fun deleteDigit() {
        if (!checking) entered = entered.dropLast(1)
    }

    private fun check() {
        checking = true
        viewModelScope.launch {
            when (val result = container.settings.checkPin(entered)) {
                PinCheck.Correct -> container.session.unlock()
                is PinCheck.Wrong -> {
                    attemptsLeft = result.attemptsLeft
                    wrongCount++
                }
                is PinCheck.Paused -> {
                    pausedUntil = result.untilMillis
                    attemptsLeft = null
                    wrongCount++
                }
            }
            entered = ""
            checking = false
        }
    }

    fun unlockedByBiometric() = container.session.unlock()

    fun startOver() {
        viewModelScope.launch { container.eraseEverything() }
    }
}

@Composable
fun UnlockScreen(settings: StoreSettings) {
    val viewModel = crateViewModel { UnlockViewModel(it) }
    val colors = Crate.colors
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val biometricReady = remember(context) {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val useBiometric = settings.biometricEnabled && biometricReady && activity != null
    val promptTitle = stringResource(R.string.unlock_prompt_title, settings.storeName)
    val usePin = stringResource(R.string.unlock_prompt_use_pin)
    val showPrompt: () -> Unit = {
        if (activity != null) {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        viewModel.unlockedByBiometric()
                    }
                },
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(promptTitle)
                    .setNegativeButtonText(usePin)
                    .setAllowedAuthenticators(BIOMETRIC_WEAK)
                    .build(),
            )
        }
    }
    LaunchedEffect(useBiometric) { if (useBiometric) showPrompt() }

    // A wrong PIN shakes the dots, as a till's keypad would buzz.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(viewModel.wrongCount) {
        if (viewModel.wrongCount > 0) {
            for (offset in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(offset, spring(stiffness = 4000f))
        }
    }
    var secondsLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(viewModel.pausedUntil) {
        while (true) {
            secondsLeft = ((viewModel.pausedUntil - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
            if (secondsLeft == 0) break
            delay(250)
        }
    }
    var confirmStartOver by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier
                .size(72.dp)
                .background(colors.primary, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) { CrateMark(Modifier.size(46.dp), crateColor = colors.primary) }
        Spacer(Modifier.height(18.dp))
        Text(
            settings.storeName,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.unlock_welcome_back, settings.ownerName), style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
        Spacer(Modifier.height(36.dp))
        Text(stringResource(R.string.unlock_enter_pin), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Spacer(Modifier.height(16.dp))
        val dotsDescription = stringResource(R.string.unlock_dots, viewModel.entered.length)
        Row(
            Modifier
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                .clearAndSetSemantics { contentDescription = dotsDescription },
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            repeat(4) { index -> PinDot(filled = index < viewModel.entered.length) }
        }
        Spacer(Modifier.height(14.dp))
        val status = when {
            secondsLeft > 0 -> stringResource(R.string.unlock_paused, secondsLeft)
            viewModel.attemptsLeft != null -> pluralStringResource(R.plurals.unlock_wrong, viewModel.attemptsLeft!!, viewModel.attemptsLeft!!)
            settings.sampleStore -> stringResource(R.string.unlock_sample_hint)
            else -> ""
        }
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = if (secondsLeft > 0 || viewModel.attemptsLeft != null) colors.onDanger else colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .heightIn(min = 18.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.weight(1f))
        Keypad(
            enabled = secondsLeft == 0,
            onDigit = viewModel::press,
            onDelete = viewModel::deleteDigit,
            onBiometric = if (useBiometric) showPrompt else null,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { confirmStartOver = true }) {
            Text(stringResource(R.string.unlock_forgot), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        }
    }

    if (confirmStartOver) {
        AlertDialog(
            onDismissRequest = { confirmStartOver = false },
            title = { Text(stringResource(R.string.unlock_forgot_title)) },
            text = { Text(stringResource(R.string.unlock_forgot_body)) },
            confirmButton = {
                TextButton(onClick = { confirmStartOver = false; viewModel.startOver() }) {
                    Text(stringResource(R.string.unlock_forgot_confirm), color = colors.onDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStartOver = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    val colors = Crate.colors
    val fill by animateColorAsState(if (filled) colors.primary else colors.background, label = "pin dot")
    Box(
        Modifier
            .size(16.dp)
            .background(fill, CircleShape)
            .border(2.dp, if (filled) colors.primary else colors.fieldLine, CircleShape),
    )
}

@Composable
private fun Keypad(enabled: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit, onBiometric: (() -> Unit)?) {
    val rows = listOf("123", "456", "789")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit -> DigitKey(digit.toString(), enabled, Modifier.weight(1f)) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBiometric != null) {
                IconKey(CrateIcons.Fingerprint, stringResource(R.string.unlock_fingerprint), Modifier.weight(1f), accent = true, onClick = onBiometric)
            } else {
                Spacer(Modifier.weight(1f))
            }
            DigitKey("0", enabled, Modifier.weight(1f)) { onDigit('0') }
            IconKey(CrateIcons.Backspace, stringResource(R.string.unlock_delete_digit), Modifier.weight(1f), onClick = onDelete)
        }
    }
}

@Composable
private fun DigitKey(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = Crate.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        contentColor = colors.ink,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BricolageDisplay), color = if (enabled) colors.ink else colors.inkMuted)
        }
    }
}

@Composable
private fun IconKey(icon: ImageVector, description: String, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
    val colors = Crate.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (accent) colors.primarySoft else colors.surface,
        contentColor = if (accent) colors.onPrimarySoft else colors.ink,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(26.dp))
        }
    }
}

private tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
