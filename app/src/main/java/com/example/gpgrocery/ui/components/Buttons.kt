package com.example.gpgrocery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gpgrocery.ui.theme.Crate

private val ButtonShape = RoundedCornerShape(18.dp)
private val ButtonPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?, loading: Boolean, color: Color) {
    if (loading) {
        CircularProgressIndicator(Modifier.size(20.dp), color = color, strokeWidth = 2.5.dp)
        Spacer(Modifier.width(10.dp))
    } else if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}

/** The one thing to do next on a screen: Charge, Save product, Receive stock. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false,
) {
    val colors = Crate.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled && !loading,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = 0.38f),
            disabledContentColor = colors.onPrimary.copy(alpha = 0.85f),
        ),
    ) { ButtonLabel(text, icon, loading, colors.onPrimary) }
}

/** A second choice next to the primary one, in a softer fill. */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false,
) {
    val colors = Crate.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled && !loading,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primarySoft,
            contentColor = colors.onPrimarySoft,
            disabledContainerColor = colors.primarySoft.copy(alpha = 0.5f),
            disabledContentColor = colors.onPrimarySoft.copy(alpha = 0.6f),
        ),
    ) { ButtonLabel(text, icon, loading, colors.onPrimarySoft) }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = Crate.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        border = BorderStroke(1.5.dp, colors.outlineAccent),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface, contentColor = colors.onPrimarySoft),
    ) { ButtonLabel(text, icon, false, colors.onPrimarySoft) }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Crate.colors.primary,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** A round icon button; the dot marks something new behind it. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Crate.colors.surface,
    contentColor: Color = Crate.colors.ink,
    showDot: Boolean = false,
) {
    Box(modifier) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor, contentColor = contentColor),
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
        if (showDot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(10.dp)
                    .border(2.dp, containerColor, CircleShape)
                    .background(Crate.colors.accent, CircleShape),
            )
        }
    }
}
