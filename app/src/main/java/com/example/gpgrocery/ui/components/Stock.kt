package com.example.gpgrocery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gpgrocery.R
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.data.model.StockStatus
import com.example.gpgrocery.domain.Quantity
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import java.io.File

/** The aisle's coloured tile with its icon, or the product's own photo when it has one. */
@Composable
fun CategoryTile(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    corner: Dp = 14.dp,
    iconSize: Dp = 22.dp,
    photoPath: String? = null,
) {
    val colors = Crate.colors.category(category)
    val shape = RoundedCornerShape(corner)
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(colors.container),
        )
    } else {
        Box(
            modifier
                .size(size)
                .background(colors.container, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(CrateIcons.forCategory(category), contentDescription = null, tint = colors.content, modifier = Modifier.size(iconSize))
        }
    }
}

/** "Low · 4 left" on amber, "Out of stock" on red, a plain count on green. */
@Composable
fun StockPill(status: StockStatus, text: String, modifier: Modifier = Modifier) {
    val colors = Crate.colors
    val (container, content) = when (status) {
        StockStatus.IN_STOCK -> colors.primarySoft to colors.primaryStrong
        StockStatus.LOW -> colors.warningSoft to colors.onWarning
        StockStatus.OUT -> colors.dangerSoft to colors.onDanger
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.tabularFigures(),
        color = content,
        maxLines = 1,
        modifier = modifier
            .background(container, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}

/** "3", "2.40 lb", "1.25 kg". */
@Composable
fun quantityWithUnit(milli: Long, soldBy: SoldBy): String {
    val number = Quantity.number(milli, soldBy)
    return soldBy.unit?.let { stringResource(R.string.quantity_with_unit, number, stringResource(it)) } ?: number
}

/** What a stock pill says: "12 lb", "Low · 4 left", "Out of stock". */
@Composable
fun stockLabel(stockMilli: Long, soldBy: SoldBy, status: StockStatus): String {
    val amount = quantityWithUnit(stockMilli, soldBy)
    return when (status) {
        StockStatus.OUT -> stringResource(R.string.stock_out)
        StockStatus.LOW -> if (soldBy.isWeighed) stringResource(R.string.stock_low_weighed, amount)
        else stringResource(R.string.stock_low_counted, amount)
        StockStatus.IN_STOCK -> if (soldBy.isWeighed) amount else stringResource(R.string.stock_in_counted, amount)
    }
}

/** "$5.49", "$0.79/lb". */
@Composable
fun unitPrice(priceCents: Long, soldBy: SoldBy): String {
    val price = com.example.gpgrocery.domain.Money.format(priceCents)
    return soldBy.unit?.let { stringResource(R.string.price_per_unit, price, stringResource(it)) } ?: price
}
