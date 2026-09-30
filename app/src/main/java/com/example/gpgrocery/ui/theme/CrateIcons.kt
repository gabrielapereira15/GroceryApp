package com.example.gpgrocery.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.example.gpgrocery.data.model.Category

/**
 * The app's icons: rounded 2px strokes on a 24-unit grid, drawn from SVG path
 * data so they match the design exactly. `Icon(CrateIcons.Home, …)` tints
 * them like any Material icon.
 */
object CrateIcons {
    val Home by icon("M3 10.5 12 3l9 7.5", "M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5")
    val Inventory by icon("M21 8 12 3 3 8v8l9 5 9-5z", "m3 8 9 5 9-5", "M12 13v8")
    val Plus by icon("M12 5v14M5 12h14")
    val Minus by icon("M5 12h14")
    val Receipt by icon("M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2z", "M9 8h6M9 12h6")
    val Chart by icon(
        "M3 20h18",
        "M6 11h1a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
        "M11.5 6h1a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z",
        "M17 9h1a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-1a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1z",
    )
    val Search by icon(circle(11f, 11f, 7f), "m20 20-3.5-3.5")
    val Scan by icon(
        "M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2",
        "M7 8v8M10 8v8M13 8v8M17 8v8",
    )
    val Bell by icon("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val ChevronRight by icon("m9 18 6-6-6-6")
    val ChevronLeft by icon("m15 18-6-6 6-6")
    val ChevronDown by icon("m6 9 6 6 6-6")
    val Truck by icon("M3 6h11v10H3z", "M14 9h4l3 3v4h-7", circle(7f, 18f, 2f), circle(17f, 18f, 2f))
    val Alert by icon("M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z", "M12 9v4M12 17h.01")
    val Check by icon("M20 6 9 17l-5-5")
    val Edit by icon("M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    val More by icon(circle(12f, 5f, 1f), circle(12f, 12f, 1f), circle(12f, 19f, 1f))
    val Camera by icon("M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z", circle(12f, 13.5f, 3.5f))
    val Image by icon(
        "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        circle(9f, 9f, 2f),
        "m21 15-3.1-3.1a2 2 0 0 0-2.8 0L6 21",
    )
    val Close by icon("M18 6 6 18M6 6l12 12")
    val Fingerprint by icon(
        "M12 11v3a8 8 0 0 1-1.2 4.2",
        "M8.5 20.2A12 12 0 0 0 9 14v-3a3 3 0 0 1 6 0v1",
        "M15 16a17 17 0 0 1-.9 4.6",
        "M5.3 17.5A12 12 0 0 0 6 14v-3a6 6 0 0 1 10.3-4.2",
        "M18 10.5c.5 3 .3 6-.4 8.5",
        "M3.5 12.5V11a8.5 8.5 0 0 1 3-6.5",
    )
    val Backspace by icon("M21 5H9l-6 7 6 7h12a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1z", "m17 9-6 6M11 9l6 6")
    val Share by icon(circle(18f, 5f, 3f), circle(6f, 12f, 3f), circle(18f, 19f, 3f), "m8.6 13.5 6.8 4M15.4 6.5l-6.8 4")
    val TrendUp by icon("m22 7-8.5 8.5-5-5L2 17", "M16 7h6v6")
    val TrendDown by icon("m22 17-8.5-8.5-5 5L2 7", "M16 17h6v-6")
    val Sliders by icon(
        "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12",
        circle(16f, 6f, 2f), circle(10f, 12f, 2f), circle(18f, 18f, 2f),
    )
    val Calendar by icon("M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M16 3v4M8 3v4M3 10h18")
    val StockCount by icon(
        "M9 4h6M9 4a2 2 0 0 0-2 2H6a1 1 0 0 0-1 1v13a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-1a2 2 0 0 0-2-2",
        "m9 13 2 2 4-4",
    )
    val Settings by icon(
        "M12.2 2h-.4a2 2 0 0 0-2 2v.2a2 2 0 0 1-1 1.7l-.4.3a2 2 0 0 1-2 0l-.2-.1a2 2 0 0 0-2.7.7l-.2.4a2 2 0 0 0 .7 2.7l.2.1a2 2 0 0 1 1 1.7v.5a2 2 0 0 1-1 1.8l-.2.1a2 2 0 0 0-.7 2.7l.2.4a2 2 0 0 0 2.7.7l.2-.1a2 2 0 0 1 2 0l.4.3a2 2 0 0 1 1 1.7v.2a2 2 0 0 0 2 2h.4a2 2 0 0 0 2-2v-.2a2 2 0 0 1 1-1.7l.4-.3a2 2 0 0 1 2 0l.2.1a2 2 0 0 0 2.7-.7l.2-.4a2 2 0 0 0-.7-2.7l-.2-.1a2 2 0 0 1-1-1.7v-.5a2 2 0 0 1 1-1.8l.2-.1a2 2 0 0 0 .7-2.7l-.2-.4a2 2 0 0 0-2.7-.7l-.2.1a2 2 0 0 1-2 0l-.4-.3a2 2 0 0 1-1-1.7V4a2 2 0 0 0-2-2z",
        circle(12f, 12f, 3f),
    )
    val Trash by icon("M3 6h18", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2", "M10 11v6M14 11v6")
    val Lock by icon("M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z", "M7 11V7a5 5 0 0 1 10 0v4")
    val Moon by icon("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9z")
    val Sun by icon(circle(12f, 12f, 4f), "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M6.3 17.7l-1.4 1.4M19.1 4.9l-1.4 1.4")
    val Contrast by icon(circle(12f, 12f, 9f), "M12 3v18a9 9 0 0 0 0-18z")
    val Download by icon("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5", "M12 15V3")
    val Store by icon("M3 9 4.5 4h15L21 9", "M3 9h18v2a3 3 0 0 1-6 0 3 3 0 0 1-6 0 3 3 0 0 1-6 0z", "M5 13v8h14v-8", "M10 21v-5h4v5")
    val Tag by icon("M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8z", circle(7.5f, 7.5f, 1.5f))
    val Cash by icon("M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", circle(12f, 12f, 2.5f), "M6 12h.01M18 12h.01")
    val Card by icon("M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M2 10h20", "M6 15h4")
    val Info by icon(circle(12f, 12f, 9f), "M12 16v-4M12 8h.01")
    val Reset by icon("M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5")
    val Percent by icon("M19 5 5 19", circle(6.5f, 6.5f, 2.5f), circle(17.5f, 17.5f, 2.5f))
    val User by icon(circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0")

    // One per aisle; the colour pair comes from CrateColors.category().
    val Produce by icon("M12 7c-3-2-7 0-7 5 0 4 3 9 5 9 1 0 1.3-.5 2-.5s1 .5 2 .5c2 0 5-5 5-9 0-5-4-7-7-5z", "M12 7c0-2 1-4 3-4")
    val Dairy by icon("M8 2h8v4l2 3v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V9l2-3z", "M6 9h12")
    val Bakery by icon("M5 11a4 4 0 0 1 2-7.5h10A4 4 0 0 1 19 11v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1z", "M9 10v6M15 10v6")
    val Pantry by icon("M8 3h8v3H8z", "M7 6h10a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z", "M6 11h12")
    val Drinks by icon("M10 2h4v4l2 3v12a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V9l2-3z", "M8 14h8")
    val Frozen by icon("M12 2v20M4 7l16 10M20 7 4 17", "m9 3 3 2 3-2M9 21l3-2 3 2")
    val Snacks by icon(circle(12f, 12f, 9f), "M8.5 8.5h.01M15 9h.01M9 15h.01M14.5 14.5h.01M12 12h.01")
    val Household by icon("M9 3h5v4H9zM11.5 7v3", "M8 10h7l1 3v8a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-8z", "M17 4h2M17 7h3")

    fun forCategory(category: Category): ImageVector = when (category) {
        Category.PRODUCE -> Produce
        Category.DAIRY -> Dairy
        Category.BAKERY -> Bakery
        Category.PANTRY -> Pantry
        Category.DRINKS -> Drinks
        Category.FROZEN -> Frozen
        Category.SNACKS -> Snacks
        Category.HOUSEHOLD -> Household
    }
}

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

/** Builds the icon the first time it is used, then keeps it. */
private fun icon(vararg paths: String) = lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}
