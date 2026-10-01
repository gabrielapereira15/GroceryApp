package com.example.gpgrocery.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.R
import com.example.gpgrocery.domain.InsightsMath
import com.example.gpgrocery.domain.InsightsSummary
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.domain.Period
import com.example.gpgrocery.ui.components.Bar
import com.example.gpgrocery.ui.components.BarChart
import com.example.gpgrocery.ui.components.CategoryTile
import com.example.gpgrocery.ui.components.CrateCard
import com.example.gpgrocery.ui.components.EmptyState
import com.example.gpgrocery.ui.components.RowDivider
import com.example.gpgrocery.ui.components.ScreenHeader
import com.example.gpgrocery.ui.components.SectionHeader
import com.example.gpgrocery.ui.components.SegmentedControl
import com.example.gpgrocery.ui.components.Share
import com.example.gpgrocery.ui.components.StackedBar
import com.example.gpgrocery.ui.components.StatCard
import com.example.gpgrocery.ui.components.quantityWithUnit
import com.example.gpgrocery.ui.components.tabularFigures
import com.example.gpgrocery.ui.crateViewModel
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.abs

data class InsightsState(
    val loading: Boolean = true,
    val period: Period = Period.WEEK,
    val today: LocalDate = LocalDate.now(),
    val summary: InsightsSummary? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(container: AppContainer) : ViewModel() {
    private val period = MutableStateFlow(Period.WEEK)
    private val zone = container.clock.zone
    private val today = LocalDate.now(container.clock)

    val state: StateFlow<InsightsState> = period.flatMapLatest { chosen ->
        val window = InsightsMath.window(chosen, today)
        fun millis(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli()
        combine(
            container.sales.linesBetween(millis(window.start), millis(window.endExclusive)),
            container.sales.linesBetween(millis(window.previousStart), millis(window.start)),
        ) { lines, previous ->
            InsightsState(
                loading = false,
                period = chosen,
                today = today,
                summary = InsightsMath.summarize(chosen, today, zone, lines, previous),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())

    fun show(chosen: Period) {
        period.value = chosen
    }
}

@Composable
fun InsightsScreen(onOpenProduct: (Long) -> Unit) {
    val viewModel = crateViewModel { InsightsViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val summary = state.summary

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.insights_title)) }
        item {
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.insights_week),
                    stringResource(R.string.insights_month),
                    stringResource(R.string.insights_year),
                ),
                selectedIndex = state.period.ordinal,
                onSelect = { viewModel.show(Period.entries[it]) },
            )
        }
        if (summary == null) return@LazyColumn
        item { RevenueCard(state.period, state.today, summary) }
        if (summary.revenueCents == 0L) {
            item {
                EmptyState(
                    icon = CrateIcons.Chart,
                    title = stringResource(R.string.insights_empty_title),
                    body = stringResource(R.string.insights_empty_body),
                )
            }
            return@LazyColumn
        }
        item {
            // Same height side by side, though only one of them has a note under its number.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = stringResource(R.string.insights_margin),
                    value = summary.marginPercent?.let { stringResource(R.string.insights_share, oneDecimal(it)) } ?: "—",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StatCard(
                    label = stringResource(R.string.insights_items),
                    value = "%,d".format(summary.itemsSold),
                    note = pluralStringResource(R.plurals.insights_sales, summary.salesCount, summary.salesCount),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        item { CategoryCard(summary) }
        item { SectionHeader(stringResource(R.string.insights_top_products)) }
        item {
            CrateCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    summary.topProducts.forEachIndexed { index, product ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .then(if (product.productId != null) Modifier.clickable { onOpenProduct(product.productId) } else Modifier)
                                .padding(vertical = 10.dp)
                                .semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.titleMedium.tabularFigures(),
                                color = Crate.colors.inkMuted,
                                modifier = Modifier.width(18.dp),
                            )
                            CategoryTile(product.category, size = 40.dp, corner = 12.dp, iconSize = 20.dp)
                            Column(Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Crate.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(stringResource(R.string.home_sold, quantityWithUnit(product.quantityMilli, product.soldBy)), style = MaterialTheme.typography.bodySmall.tabularFigures(), color = Crate.colors.inkMuted)
                            }
                            Text(Money.format(product.revenueCents), style = MaterialTheme.typography.titleSmall.tabularFigures(), color = Crate.colors.ink)
                        }
                        if (index < summary.topProducts.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RevenueCard(period: Period, today: LocalDate, summary: InsightsSummary) {
    val colors = Crate.colors
    val locale = LocalLocale.current.platformLocale
    val bars = summary.buckets.map { bucket ->
        val label = when (period) {
            Period.WEEK -> bucket.start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            Period.MONTH -> if (bucket.start.dayOfMonth % 5 == 0 || bucket.start == today) bucket.start.dayOfMonth.toString() else ""
            Period.YEAR -> bucket.start.month.getDisplayName(TextStyle.NARROW, locale)
        }
        Bar(label = label, value = bucket.revenueCents, caption = if (period == Period.WEEK) Money.compact(bucket.revenueCents) else null)
    }
    val spoken = summary.buckets.joinToString { bucket ->
        val name = when (period) {
            Period.YEAR -> bucket.start.format(DateTimeFormatter.ofPattern("MMMM", locale))
            else -> bucket.start.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
        }
        "$name ${Money.format(bucket.revenueCents)}"
    }
    CrateCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.insights_revenue), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Money.format(summary.revenueCents), style = MaterialTheme.typography.displaySmall.tabularFigures(), color = colors.ink)
                summary.changePercent?.let { change ->
                    val up = change >= 0
                    Text(
                        stringResource(if (up) R.string.insights_change_up else R.string.insights_change_down, oneDecimal(abs(change))),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (up) colors.primaryStrong else colors.onDanger,
                        modifier = Modifier
                            .background(if (up) colors.primarySoft else colors.dangerSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }
            Text(
                stringResource(
                    when (period) {
                        Period.WEEK -> R.string.insights_vs_week
                        Period.MONTH -> R.string.insights_vs_month
                        Period.YEAR -> R.string.insights_vs_year
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
            Box(Modifier.padding(top = 12.dp)) {
                BarChart(
                    bars = bars,
                    highlightIndex = bars.lastIndex,
                    description = stringResource(R.string.insights_chart, spoken),
                    barAreaHeight = if (period == Period.WEEK) 96.dp else 110.dp,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(summary: InsightsSummary) {
    val colors = Crate.colors
    val total = summary.byCategory.sumOf { it.second }.coerceAtLeast(1)
    val shares = summary.byCategory.map { (category, cents) ->
        Triple(category, cents.toFloat() / total, colors.chartCategories.getValue(category))
    }
    val labels = shares.map { (category, fraction, _) -> stringResource(category.label) to percent(fraction) }
    CrateCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.insights_by_category), style = MaterialTheme.typography.titleSmall, color = colors.ink)
            StackedBar(
                shares = shares.mapIndexed { index, (_, fraction, color) -> Share(labels[index].first, fraction, color) },
                description = labels.joinToString { (name, share) -> "$name $share%" },
            )
            labels.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEachIndexed { column, (name, share) ->
                        val color = shares[row * 2 + column].third
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                            Text(name, style = MaterialTheme.typography.bodySmall, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(stringResource(R.string.insights_share, share), style = MaterialTheme.typography.labelMedium.tabularFigures(), color = colors.ink)
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun percent(fraction: Float): String = BigDecimal.valueOf(fraction * 100.0).setScale(0, RoundingMode.HALF_UP).toPlainString()

private fun oneDecimal(value: Double): String = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString()
