package com.example.gpgrocery.domain

import com.example.gpgrocery.data.sample.SampleStore
import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodesTest {
    @Test
    fun `typed spaces are removed`() {
        assertEquals("062639300128", Barcodes.normalize(" 0 62639 30012 8 "))
    }

    @Test
    fun `a UPC-A read as a 13-digit EAN loses its leading zero`() {
        assertEquals("062639300128", Barcodes.normalize("0062639300128"))
    }

    @Test
    fun `other codes are kept as they are`() {
        assertEquals("4006381333931", Barcodes.normalize("4006381333931"))
        assertEquals("4011", Barcodes.normalize("4011"))
    }

    @Test
    fun `UPC-A is printed in its groups`() {
        assertEquals("0 62639 30012 8", Barcodes.pretty("062639300128"))
        assertEquals("4011", Barcodes.pretty("4011"))
    }

    @Test
    fun `sample barcodes carry a valid check digit`() {
        assertEquals("036000291452", SampleStore.upcA("03600029145"))
        assertEquals("062639300128", SampleStore.upcA("06263930012"))
    }
}
