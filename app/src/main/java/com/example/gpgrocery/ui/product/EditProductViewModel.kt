package com.example.gpgrocery.ui.product

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.data.DuplicateBarcodeException
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SupplierEntity
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.domain.Pricing
import com.example.gpgrocery.domain.Quantity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the form holds, as typed. Parsing happens on the way out. */
data class ProductForm(
    val name: String = "",
    val barcode: String = "",
    val category: Category = Category.PRODUCE,
    val soldBy: SoldBy = SoldBy.EACH,
    val price: String = "",
    val cost: String = "",
    val taxable: Boolean = false,
    // Blank rather than prefilled, so typing a number does not end up after a default ("54" for 4).
    val stock: String = "",
    val alertBelow: String = "",
    val supplierId: Long? = null,
    val photoPath: String? = null,
    val showErrors: Boolean = false,
    /** The product that already has this barcode, when saving found one. */
    val barcodeTakenBy: String? = null,
) {
    val priceCents: Long? get() = Money.parse(price)
    val costCents: Long? get() = if (cost.isBlank()) 0 else Money.parse(cost)
    val stockMilli: Long? get() = Quantity.parse(stock.ifBlank { "0" }, soldBy)
    val alertMilli: Long? get() = Quantity.parse(alertBelow.ifBlank { DEFAULT_ALERT }, soldBy)

    val nameMissing get() = name.isBlank()
    val priceInvalid get() = priceCents == null || priceCents == 0L
    val costInvalid get() = costCents == null
    val stockInvalid get() = stockMilli == null
    val alertInvalid get() = alertMilli == null
    val isValid get() = !nameMissing && !priceInvalid && !costInvalid && !stockInvalid && !alertInvalid && barcodeTakenBy == null

    val marginPercent: Double? get() {
        val price = priceCents ?: return null
        val cost = costCents ?: return null
        return if (price > 0 && cost > 0) Pricing.marginPercent(price, cost) else null
    }

    companion object {
        /** Shown as the placeholder, and used when the field is left blank. */
        const val DEFAULT_ALERT = "5"
    }
}

class EditProductViewModel(
    private val container: AppContainer,
    private val productId: Long,
    scannedBarcode: String?,
) : ViewModel() {
    val isNew = productId == 0L
    var form by mutableStateOf(ProductForm(barcode = scannedBarcode.orEmpty()))
        private set
    var loaded by mutableStateOf(isNew)
        private set
    var saving by mutableStateOf(false)
        private set
    private var original: ProductEntity? = null
    private val photosAddedHere = mutableListOf<String>()

    val suppliers: StateFlow<List<SupplierEntity>> =
        container.suppliers.suppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taxRateBasisPoints: StateFlow<Int> = kotlinx.coroutines.flow.flow {
        emit(container.settings.current().taxRateBasisPoints)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 1300)

    init {
        if (!isNew) {
            viewModelScope.launch {
                container.products.get(productId)?.let { product ->
                    original = product
                    form = ProductForm(
                        name = product.name,
                        barcode = product.barcode.orEmpty(),
                        category = product.category,
                        soldBy = product.soldBy,
                        price = Money.plain(product.priceCents),
                        cost = Money.plain(product.costCents),
                        taxable = product.taxable,
                        stock = Quantity.number(product.stockMilli, product.soldBy),
                        alertBelow = Quantity.number(product.alertBelowMilli, product.soldBy),
                        supplierId = product.supplierId,
                        photoPath = product.photoPath,
                    )
                }
                loaded = true
            }
        }
    }

    fun edit(change: ProductForm.() -> ProductForm) {
        val next = form.change()
        form = if (next.barcode != form.barcode) next.copy(barcodeTakenBy = null) else next
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            container.photos.keep(uri)?.let { path ->
                photosAddedHere += path
                form = form.copy(photoPath = path)
            }
        }
    }

    fun removePhoto() {
        form = form.copy(photoPath = null)
    }

    fun save(onSaved: (id: Long, name: String) -> Unit) {
        val current = form
        if (!current.isValid) {
            form = current.copy(showErrors = true)
            return
        }
        saving = true
        viewModelScope.launch {
            val base = original
            val product = ProductEntity(
                id = base?.id ?: 0,
                name = current.name,
                category = current.category,
                soldBy = current.soldBy,
                priceCents = current.priceCents!!,
                costCents = current.costCents!!,
                taxable = current.taxable,
                stockMilli = current.stockMilli!!,
                alertBelowMilli = current.alertMilli!!,
                barcode = current.barcode,
                supplierId = current.supplierId,
                photoPath = current.photoPath,
                createdAt = base?.createdAt ?: 0,
                updatedAt = 0,
            )
            try {
                val id = container.products.save(product)
                // Photos picked and then replaced, and the one the product had before, are no longer needed.
                (photosAddedHere + listOfNotNull(base?.photoPath))
                    .filter { it != current.photoPath }
                    .forEach { container.photos.remove(it) }
                photosAddedHere.clear()
                onSaved(id, current.name.trim())
            } catch (taken: DuplicateBarcodeException) {
                form = form.copy(barcodeTakenBy = taken.existingName, showErrors = true)
            } finally {
                saving = false
            }
        }
    }

    /** Leaving without saving: photos taken for this form go too. */
    fun discard() {
        val keep = original?.photoPath
        val unsaved = photosAddedHere.filter { it != keep }
        photosAddedHere.clear()
        viewModelScope.launch { unsaved.forEach { container.photos.remove(it) } }
    }
}
