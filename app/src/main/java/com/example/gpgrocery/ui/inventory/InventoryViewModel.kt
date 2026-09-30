package com.example.gpgrocery.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.StockStatus
import com.example.gpgrocery.domain.Barcodes
import com.example.gpgrocery.domain.Pricing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

sealed interface StockFilter {
    data object All : StockFilter
    data object Low : StockFilter
    data object Out : StockFilter
    data class Aisle(val category: Category) : StockFilter
}

data class InventoryState(
    val loading: Boolean = true,
    val products: List<ProductEntity> = emptyList(),
    val total: Int = 0,
    val lowCount: Int = 0,
    val outCount: Int = 0,
    val stockValueCents: Long = 0,
    val aisles: List<Category> = emptyList(),
    val query: String = "",
    val filter: StockFilter = StockFilter.All,
)

class InventoryViewModel(private val container: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<StockFilter>(StockFilter.All)

    val state: StateFlow<InventoryState> = combine(container.products.products, query, filter) { products, text, chosen ->
        val needle = fold(text)
        val shown = products
            .filter { product ->
                when (chosen) {
                    StockFilter.All -> true
                    StockFilter.Low -> product.status == StockStatus.LOW
                    StockFilter.Out -> product.status == StockStatus.OUT
                    is StockFilter.Aisle -> product.category == chosen.category
                }
            }
            .filter { product ->
                needle.isEmpty() ||
                    fold(product.name).contains(needle) ||
                    (product.barcode?.contains(Barcodes.normalize(text)) == true)
            }
        InventoryState(
            loading = false,
            products = shown,
            total = products.size,
            lowCount = products.count { it.status == StockStatus.LOW },
            outCount = products.count { it.status == StockStatus.OUT },
            stockValueCents = products.sumOf { Pricing.lineTotal(it.costCents, it.stockMilli) },
            aisles = Category.entries.filter { aisle -> products.any { it.category == aisle } },
            query = text,
            filter = chosen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryState())

    fun search(text: String) {
        query.value = text
    }

    fun show(chosen: StockFilter) {
        filter.value = chosen
    }

    /** Replaces the stock with a fresh count from the shelf. */
    fun saveCount(product: ProductEntity, countedMilli: Long, onSaved: () -> Unit) {
        viewModelScope.launch {
            container.products.setStock(product.id, countedMilli)
            onSaved()
        }
    }

    fun lookup(barcode: String, onResult: (Long?) -> Unit) {
        viewModelScope.launch { onResult(container.products.findByBarcode(barcode)?.id) }
    }

    /** Case and accents do not matter: "creme" finds "Crème fraîche". */
    private fun fold(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
}
