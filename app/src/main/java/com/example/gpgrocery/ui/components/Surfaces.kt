package com.example.gpgrocery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gpgrocery.ui.theme.Crate

val CardShape = RoundedCornerShape(22.dp)

/** A white card with a hairline border; tappable when [onClick] is given. */
@Composable
fun CrateCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Crate.colors.surface,
    borderColor: Color = Crate.colors.line,
    shape: RoundedCornerShape = CardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Column(content = content)
        }
    }
}

/** A tab screen's heading: big title, an optional line under it, actions on the right. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = Crate.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Crate.colors.inkMuted)
            }
        }
        actions()
    }
}

/** The bar across the top of a task screen: back or close, a title, actions. */
@Composable
fun TopBar(
    title: String,
    navigationIcon: ImageVector,
    navigationDescription: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircleIconButton(navigationIcon, navigationDescription, onNavigate)
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Crate.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Crate.colors.ink,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (action != null) QuietButton(action, onAction)
    }
}

/** Small capitals above a group of rows: TODAY, YESTERDAY. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = MaterialTheme.typography.labelMedium.fontSize * 0.06f),
        color = Crate.colors.inkMuted,
        modifier = modifier
            .padding(start = 4.dp, top = 4.dp)
            .semantics { heading() },
    )
}

@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = Crate.colors.divider)
}

/** A figure in a small card: "Gross margin 30.8%". */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    noteColor: Color = Crate.colors.inkMuted,
) {
    CrateCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Crate.colors.inkMuted)
            Text(value, style = MaterialTheme.typography.headlineSmall.tabularFigures(), color = Crate.colors.ink)
            if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = noteColor)
        }
    }
}

/** When a list has nothing in it, say why and what to do. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(Crate.colors.primarySoft, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Crate.colors.onPrimarySoft, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Crate.colors.ink, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Crate.colors.inkMuted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(6.dp))
            TonalButton(action, onAction)
        }
    }
}

/** A labelled value row in a details list: "Supplier   Harmony Dairy Co." */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier, divider: Boolean = true) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Crate.colors.inkMuted)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.tabularFigures(),
                color = Crate.colors.ink,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        if (divider) RowDivider()
    }
}

fun androidx.compose.ui.text.TextStyle.tabularFigures() = copy(fontFeatureSettings = "tnum")

val ScreenPadding: Dp = 20.dp
