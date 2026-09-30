package com.example.gpgrocery.data.model

import androidx.annotation.StringRes
import com.example.gpgrocery.R

/** The aisles a product can sit in. Each has its own colour pair and icon. */
enum class Category(@StringRes val label: Int) {
    PRODUCE(R.string.category_produce),
    DAIRY(R.string.category_dairy),
    BAKERY(R.string.category_bakery),
    PANTRY(R.string.category_pantry),
    DRINKS(R.string.category_drinks),
    FROZEN(R.string.category_frozen),
    SNACKS(R.string.category_snacks),
    HOUSEHOLD(R.string.category_household),
}

/**
 * How a product is counted. Weighed products keep their quantities to the
 * thousandth of a kilo or pound; counted ones in whole units. Either way the
 * database stores thousandths ("milli-units"), so 2.4 lb is 2400 and 3 cartons
 * of milk are 3000.
 */
enum class SoldBy(@StringRes val label: Int, @StringRes val unit: Int?) {
    EACH(R.string.sold_by_each, null),
    KG(R.string.sold_by_kg, R.string.unit_kg),
    LB(R.string.sold_by_lb, R.string.unit_lb);

    val isWeighed: Boolean get() = this != EACH
}

enum class Payment(@StringRes val label: Int) {
    CARD(R.string.payment_card),
    CASH(R.string.payment_cash),
}

enum class StockStatus { IN_STOCK, LOW, OUT }

enum class ThemeMode { SYSTEM, LIGHT, DARK }
