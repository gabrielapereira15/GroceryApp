package com.example.gpgrocery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gpgrocery.R
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons

/** A rounded search box with a clear button and room for one more action (usually Scan). */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = Crate.colors
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        // Product names and brands are not dictionary words, so no autocorrect. Search puts the keyboard
        // away to show what was found.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        decorationBox = { field ->
            Row(
                Modifier
                    .heightIn(min = 50.dp)
                    .background(colors.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(CrateIcons.Search, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(22.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                ) {
                    if (query.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted, maxLines = 1)
                    field()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(CrateIcons.Close, contentDescription = stringResource(R.string.action_clear_search), tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                    }
                }
                trailing?.invoke()
            }
        },
    )
}

/** A form field with its label above it rather than inside, so the label never disappears. */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    prefix: String? = null,
    placeholder: String? = null,
    error: String? = null,
    supporting: String? = null,
    readOnly: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    val colors = Crate.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.inkSoft)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.tabularFigures(),
            prefix = prefix?.let { { Text(it, color = colors.inkMuted) } },
            placeholder = placeholder?.let { { Text(it, color = colors.inkMuted) } },
            trailingIcon = trailing,
            isError = error != null,
            supportingText = (error ?: supporting)?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.surface,
                errorContainerColor = colors.surface,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.fieldLine,
                cursorColor = colors.primary,
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                errorBorderColor = colors.onDanger,
                errorSupportingTextColor = colors.onDanger,
                unfocusedSupportingTextColor = colors.primaryStrong,
                focusedSupportingTextColor = colors.primaryStrong,
            ),
        )
    }
}

/** Two to four choices in a sunken track; the chosen one lifts out. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    val colors = Crate.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, RoundedCornerShape(15.dp))
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = height)
                    .then(if (selected) Modifier.shadow(1.dp, RoundedCornerShape(12.dp)) else Modifier)
                    .background(if (selected) colors.surface else colors.surfaceSunken, RoundedCornerShape(12.dp))
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) colors.onPrimarySoft else colors.inkSoft,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A filter chip; the selected one is inked in. */
@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    val colors = Crate.colors
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) colors.chipSelected else colors.surface,
        contentColor = if (selected) colors.onChipSelected else colors.ink,
        border = if (selected) null else BorderStroke(1.dp, colors.fieldLine),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
        }
    }
}

/** Minus, the amount, plus. */
@Composable
fun QuantityStepper(
    amount: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    onAmountClick: (() -> Unit)? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepButton(CrateIcons.Minus, decreaseDescription, onDecrease, size, filled = false)
        Text(
            amount,
            style = MaterialTheme.typography.titleSmall.tabularFigures(),
            color = Crate.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 26.dp)
                .then(if (onAmountClick != null) Modifier.clickable(onClick = onAmountClick) else Modifier),
        )
        StepButton(CrateIcons.Plus, increaseDescription, onIncrease, size, filled = false)
    }
}

@Composable
fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit, size: Dp = 44.dp, filled: Boolean) {
    val colors = Crate.colors
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(if (size < 40.dp) 10.dp else 14.dp),
        color = if (filled) colors.primary else colors.surface,
        contentColor = if (filled) colors.onPrimary else colors.ink,
        border = if (filled) null else BorderStroke(1.5.dp, colors.fieldLine),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (size < 40.dp) 16.dp else 22.dp))
        }
    }
}

/** A switch row: title, a line of explanation, and the switch. The whole row toggles it. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = Crate.colors
    Row(
        modifier
            .fillMaxWidth()
            .selectable(selected = checked, enabled = enabled, role = Role.Switch, onClick = { onCheckedChange(!checked) })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(colors.primarySoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = colors.onPrimarySoft, modifier = Modifier.size(20.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) colors.ink else colors.inkMuted)
            if (body != null) Text(body, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = colors.primary,
                checkedThumbColor = colors.onPrimary,
                uncheckedTrackColor = colors.surfaceSunken,
                uncheckedBorderColor = colors.fieldLine,
                uncheckedThumbColor = colors.inkMuted,
            ),
        )
    }
}
