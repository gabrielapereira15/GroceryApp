package com.example.gpgrocery.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gpgrocery.R
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.domain.Quantity
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.LabeledField
import com.example.gpgrocery.ui.components.PrimaryButton
import com.example.gpgrocery.ui.components.StepButton
import com.example.gpgrocery.ui.components.StockPill
import com.example.gpgrocery.ui.components.stockLabel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons

/**
 * A count from the shelf: type what is there, or nudge it with minus and
 * plus. Saving replaces the stock rather than adding to it, because a count
 * is the truth and the old number was not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCountSheet(product: ProductEntity, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    val colors = Crate.colors
    var text by rememberSaveable(product.id) { mutableStateOf(Quantity.number(product.stockMilli, product.soldBy)) }
    val counted = Quantity.parse(text, product.soldBy)
    val unit = product.soldBy.unit?.let { stringResource(it) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(product.category, photoPath = product.photoPath)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.count_title, product.name), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    StockPill(product.status, stockLabel(product.stockMilli, product.soldBy, product.status))
                }
            }
            Text(stringResource(R.string.count_body), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepButton(CrateIcons.Minus, stringResource(R.string.count_less), filled = false, onClick = {
                    val now = counted ?: 0
                    text = Quantity.number((now - Quantity.ONE).coerceAtLeast(0), product.soldBy)
                })
                LabeledField(
                    label = stringResource(R.string.count_field),
                    value = text,
                    onValueChange = { text = it },
                    keyboardType = if (product.soldBy.isWeighed) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    trailing = unit?.let { { Text(it, color = colors.inkMuted) } },
                    modifier = Modifier.weight(1f),
                )
                StepButton(CrateIcons.Plus, stringResource(R.string.count_more), filled = true, onClick = {
                    text = Quantity.number((counted ?: 0) + Quantity.ONE, product.soldBy)
                })
            }
            PrimaryButton(
                stringResource(R.string.count_save),
                onClick = { counted?.let(onSave) },
                enabled = counted != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
