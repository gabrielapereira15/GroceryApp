package com.example.gpgrocery.ui.product

import com.example.gpgrocery.data.model.SoldBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFormTest {
    private val filled = ProductForm(name = "Cold brew coffee 473ml", price = "4.99", cost = "2.10")

    @Test
    fun `a new product starts with empty stock fields`() {
        assertEquals("", ProductForm().stock)
        assertEquals("", ProductForm().alertBelow)
    }

    @Test
    fun `blank stock means none, blank alert means the default`() {
        assertEquals(0L, filled.stockMilli)
        assertEquals(5_000L, filled.alertMilli)
        assertTrue(filled.isValid)
    }

    @Test
    fun `typed amounts are used as they are`() {
        val form = filled.copy(stock = "12", alertBelow = "4")
        assertEquals(12_000L, form.stockMilli)
        assertEquals(4_000L, form.alertMilli)
    }

    @Test
    fun `weighed products take decimals`() {
        val form = filled.copy(soldBy = SoldBy.LB, stock = "12.5", alertBelow = "2.25")
        assertEquals(12_500L, form.stockMilli)
        assertEquals(2_250L, form.alertMilli)
    }

    @Test
    fun `counted products do not`() {
        assertTrue(filled.copy(stock = "1.5").stockInvalid)
    }

    @Test
    fun `a product needs a name and a price`() {
        assertFalse(ProductForm(price = "4.99").isValid)
        assertFalse(ProductForm(name = "Cold brew").isValid)
        assertFalse(filled.copy(price = "0").isValid)
    }

    @Test
    fun `a blank cost is free, a wrong one is an error`() {
        assertEquals(0L, filled.copy(cost = "").costCents)
        assertTrue(filled.copy(cost = "abc").costInvalid)
    }

    @Test
    fun `margin needs both a price and a cost`() {
        assertEquals(57.9, filled.marginPercent!!, 0.05)
        assertNull(filled.copy(cost = "").marginPercent)
    }
}
