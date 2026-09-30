package com.example.gpgrocery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gpgrocery.ui.theme.Crate

/** A line of how the day has gone so far, with a soft fill under it. Decorative: the figures are said elsewhere. */
@Composable
fun Sparkline(values: List<Long>, lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics { }) {
        if (values.size < 2) return@Canvas
        val max = values.max().coerceAtLeast(1)
        val min = values.min()
        val span = (max - min).coerceAtLeast(1).toFloat()
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            Offset(index * stepX, size.height - 4f - (value - min) / span * (size.height - 8f))
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(area, lineColor.copy(alpha = 0.18f))
        drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

data class Bar(val label: String, val value: Long, val caption: String? = null)

/**
 * Vertical bars with a label under each; the highlighted bar (usually today)
 * is in the primary colour, the rest in a paler green. The whole chart is read
 * out as one description.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    highlightIndex: Int,
    description: String,
    modifier: Modifier = Modifier,
    barAreaHeight: Dp = 88.dp,
) {
    val colors = Crate.colors
    val max = bars.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(if (bars.size > 10) 4.dp else 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { index, bar ->
            val highlighted = index == highlightIndex
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (bar.caption != null) {
                    Text(
                        bar.caption,
                        style = MaterialTheme.typography.labelSmall.tabularFigures(),
                        color = if (highlighted) colors.onPrimarySoft else colors.inkMuted,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Box(Modifier.height(barAreaHeight), contentAlignment = Alignment.BottomCenter) {
                    val fraction = (bar.value.toFloat() / max).coerceIn(0.02f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(if (bars.size > 10) 4.dp else 8.dp))
                            .background(if (highlighted) colors.primary else colors.chartMuted),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(bar.label, style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, maxLines = 1)
            }
        }
    }
}

data class Share(val label: String, val fraction: Float, val color: Color)

/** One bar split by share, for a mix like sales by aisle. */
@Composable
fun StackedBar(shares: List<Share>, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(999.dp))
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        shares.filter { it.fraction > 0f }.forEach { share ->
            Box(
                Modifier
                    .weight(share.fraction)
                    .fillMaxHeight()
                    .background(share.color),
            )
        }
    }
}
