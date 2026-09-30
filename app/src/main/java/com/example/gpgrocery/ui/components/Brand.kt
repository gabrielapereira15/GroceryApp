package com.example.gpgrocery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics

private val Tangerine = Color(0xFFF07F1A)
private val MintFruit = Color(0xFF9FE0B8)

/** The Crate mark: two fruits over a slatted crate, drawn on a 48-unit grid. */
@Composable
fun CrateMark(modifier: Modifier = Modifier, crateColor: Color, lineColor: Color = Color.White) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val u = size.minDimension / 48f
        drawCircle(Tangerine, radius = 6 * u, center = Offset(18 * u, 13 * u))
        drawCircle(MintFruit, radius = 7 * u, center = Offset(30.5f * u, 12 * u))
        val topLeft = Offset(7 * u, 16 * u)
        val crate = Size(34 * u, 25 * u)
        val corner = CornerRadius(6 * u)
        drawRoundRect(crateColor, topLeft, crate, corner)
        drawRoundRect(lineColor, topLeft, crate, corner, style = Stroke(width = 3 * u))
        drawLine(lineColor, Offset(7 * u, 24.5f * u), Offset(41 * u, 24.5f * u), strokeWidth = 3 * u)
        drawLine(lineColor, Offset(7 * u, 32.5f * u), Offset(41 * u, 32.5f * u), strokeWidth = 3 * u)
    }
}
