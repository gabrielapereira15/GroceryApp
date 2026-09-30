package com.example.gpgrocery.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.R
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.domain.PinHasher
import com.example.gpgrocery.ui.components.CrateMark
import com.example.gpgrocery.ui.components.LabeledField
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.TonalButton
import com.example.gpgrocery.ui.components.TopBar
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.launch

data class SetupForm(
    val storeName: String = "",
    val ownerName: String = "",
    val pin: String = "",
    val confirm: String = "",
    val showErrors: Boolean = false,
) {
    val storeNameMissing get() = storeName.isBlank()
    val ownerNameMissing get() = ownerName.isBlank()
    val pinInvalid get() = !PinHasher.isValidPin(pin)
    val confirmMismatch get() = confirm != pin
    val isValid get() = !storeNameMissing && !ownerNameMissing && !pinInvalid && !confirmMismatch
}

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    var form by mutableStateOf(SetupForm())
        private set
    var openingSample by mutableStateOf(false)
        private set
    var openingStore by mutableStateOf(false)
        private set

    fun edit(change: SetupForm.() -> SetupForm) {
        form = form.change()
    }

    // This ViewModel outlives the store: erasing it brings these screens back with the same instance,
    // so each attempt clears up after itself, the PIN included.
    fun exploreSample() {
        if (openingSample || openingStore) return
        openingSample = true
        viewModelScope.launch {
            try {
                container.openSampleStore()
            } finally {
                openingSample = false
            }
        }
    }

    fun openStore() {
        if (openingSample || openingStore) return
        if (!form.isValid) {
            form = form.copy(showErrors = true)
            return
        }
        openingStore = true
        viewModelScope.launch {
            try {
                container.openNewStore(form.storeName, form.ownerName, form.pin)
                form = SetupForm()
            } finally {
                openingStore = false
            }
        }
    }
}

private enum class Step { WELCOME, SETUP }

/** First run: the welcome screen, then either the sample store or a store of one's own. */
@Composable
fun OnboardingFlow() {
    val viewModel = crateViewModel { OnboardingViewModel(it) }
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    when (step) {
        Step.WELCOME -> WelcomeScreen(
            openingSample = viewModel.openingSample,
            onSetUp = { step = Step.SETUP },
            onExplore = viewModel::exploreSample,
        )
        Step.SETUP -> {
            BackHandler { step = Step.WELCOME }
            SetupScreen(
                form = viewModel.form,
                opening = viewModel.openingStore,
                onEdit = viewModel::edit,
                onBack = { step = Step.WELCOME },
                onOpen = viewModel::openStore,
            )
        }
    }
}

@Composable
private fun WelcomeScreen(openingSample: Boolean, onSetUp: () -> Unit, onExplore: () -> Unit) {
    val colors = Crate.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        // The story scrolls on a short screen; the two buttons stay put.
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Mosaic()
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = colors.primary)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.welcome_headline),
                style = MaterialTheme.typography.displaySmall,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(stringResource(R.string.welcome_set_up), onSetUp, Modifier.fillMaxWidth(), enabled = !openingSample)
        Spacer(Modifier.height(12.dp))
        TonalButton(stringResource(R.string.welcome_explore), onExplore, Modifier.fillMaxWidth(), loading = openingSample)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.welcome_explore_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** Nine tiles, one per aisle, around the Crate mark. Decorative. */
@Composable
private fun Mosaic() {
    val colors = Crate.colors
    val aisles = listOf(
        Category.PRODUCE, Category.BAKERY, Category.DAIRY,
        Category.DRINKS, null, Category.PANTRY,
        Category.FROZEN, Category.SNACKS, Category.HOUSEHOLD,
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        aisles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { aisle ->
                    val tileColors = aisle?.let { colors.category(it) }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(tileColors?.container ?: colors.primary, RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (aisle == null) {
                            CrateMark(Modifier.size(58.dp), crateColor = colors.primary)
                        } else {
                            Icon(CrateIcons.forCategory(aisle), contentDescription = null, tint = tileColors!!.content, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(
    form: SetupForm,
    opening: Boolean,
    onEdit: (SetupForm.() -> SetupForm) -> Unit,
    onBack: () -> Unit,
    onOpen: () -> Unit,
) {
    val colors = Crate.colors
    val errors = form.showErrors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        TopBar(
            title = stringResource(R.string.setup_title),
            navigationIcon = CrateIcons.ChevronLeft,
            navigationDescription = stringResource(R.string.action_back),
            onNavigate = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
            LabeledField(
                label = stringResource(R.string.setup_store_name),
                value = form.storeName,
                onValueChange = { value -> onEdit { copy(storeName = value) } },
                placeholder = stringResource(R.string.setup_store_name_hint),
                error = if (errors && form.storeNameMissing) stringResource(R.string.setup_error_store) else null,
            )
            LabeledField(
                label = stringResource(R.string.setup_owner_name),
                value = form.ownerName,
                onValueChange = { value -> onEdit { copy(ownerName = value) } },
                error = if (errors && form.ownerNameMissing) stringResource(R.string.setup_error_owner) else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PinField(
                    label = stringResource(R.string.setup_pin),
                    value = form.pin,
                    onValueChange = { value -> onEdit { copy(pin = value) } },
                    error = if (errors && form.pinInvalid) stringResource(R.string.setup_error_pin) else null,
                    modifier = Modifier.weight(1f),
                )
                PinField(
                    label = stringResource(R.string.setup_pin_confirm),
                    value = form.confirm,
                    onValueChange = { value -> onEdit { copy(confirm = value) } },
                    error = if (errors && !form.pinInvalid && form.confirmMismatch) stringResource(R.string.setup_error_confirm) else null,
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Done,
                )
            }
            Text(stringResource(R.string.setup_pin_note), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        PrimaryButton(
            stringResource(R.string.setup_open),
            onOpen,
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            loading = opening,
        )
    }
}

@Composable
fun PinField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Crate.colors.inkSoft)
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = { typed -> onValueChange(typed.filter(Char::isDigit).take(4)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Crate.colors.surface,
                unfocusedContainerColor = Crate.colors.surface,
                errorContainerColor = Crate.colors.surface,
                focusedBorderColor = Crate.colors.primary,
                unfocusedBorderColor = Crate.colors.fieldLine,
                errorBorderColor = Crate.colors.onDanger,
            ),
        )
    }
}
