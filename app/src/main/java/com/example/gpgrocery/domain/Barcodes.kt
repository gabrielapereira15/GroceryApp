package com.example.gpgrocery.domain

object Barcodes {
    /**
     * One form for a barcode however it arrives. Scanners report a UPC-A as a
     * 13-digit EAN with a leading zero, and people type spaces between the
     * groups ("0 62639 30012 8"), so both are folded to the 12 digits.
     */
    fun normalize(raw: String): String {
        val compact = raw.trim().filterNot(Char::isWhitespace)
        return if (compact.length == 13 && compact.startsWith('0') && compact.all(Char::isDigit)) compact.substring(1) else compact
    }

    /** "062639300128" -> "0 62639 30012 8", the way it is printed under the bars. */
    fun pretty(code: String): String =
        if (code.length == 12 && code.all(Char::isDigit)) "${code[0]} ${code.substring(1, 6)} ${code.substring(6, 11)} ${code[11]}"
        else code
}
