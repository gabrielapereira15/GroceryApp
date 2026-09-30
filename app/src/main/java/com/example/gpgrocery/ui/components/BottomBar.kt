package com.example.gpgrocery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gpgrocery.R
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons

enum class Tab(val icon: ImageVector, val label: Int) {
    HOME(CrateIcons.Home, R.string.tab_home),
    INVENTORY(CrateIcons.Inventory, R.string.tab_inventory),
    ACTIVITY(CrateIcons.Receipt, R.string.tab_activity),
    INSIGHTS(CrateIcons.Chart, R.string.tab_insights),
}

/**
 * Four places and, in the middle, the thing a till does most: sell. The Sell
 * button starts a sale rather than switching tab, so it is a button, not a tab.
 */
@Composable
fun CrateBottomBar(current: Tab?, onSelect: (Tab) -> Unit, onSell: () -> Unit) {
    val colors = Crate.colors
    Column(Modifier.background(colors.surface)) {
        HorizontalDivider(thickness = 1.dp, color = colors.line)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 6.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(Tab.HOME, current == Tab.HOME, onSelect)
            TabItem(Tab.INVENTORY, current == Tab.INVENTORY, onSelect)
            SellButton(onSell)
            TabItem(Tab.ACTIVITY, current == Tab.ACTIVITY, onSelect)
            TabItem(Tab.INSIGHTS, current == Tab.INSIGHTS, onSelect)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(tab: Tab, selected: Boolean, onSelect: (Tab) -> Unit) {
    val colors = Crate.colors
    val tint = if (selected) colors.onPrimarySoft else colors.inkMuted
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .width(56.dp)
                .height(30.dp)
                .background(if (selected) colors.primarySoft else colors.surface, RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            stringResource(tab.label),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SellButton(onSell: () -> Unit) {
    val colors = Crate.colors
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            // Taller than the bar: the button rises out of it while the label lines up with the tabs' labels.
            .wrapContentHeight(Alignment.Bottom, unbounded = true)
            .padding(bottom = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            onClick = onSell,
            modifier = Modifier
                .size(58.dp)
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = colors.primary, spotColor = colors.primary),
            shape = RoundedCornerShape(20.dp),
            color = colors.primary,
            contentColor = colors.onPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(CrateIcons.Plus, contentDescription = stringResource(R.string.action_new_sale), modifier = Modifier.size(26.dp))
            }
        }
        Text(
            stringResource(R.string.tab_sell),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = colors.onPrimarySoft,
            maxLines = 1,
        )
    }
}
