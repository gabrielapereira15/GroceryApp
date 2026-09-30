package com.example.gpgrocery.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.gpgrocery.R
import com.example.gpgrocery.domain.Barcodes
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

/** The store's snackbar, for "Product saved" and the like, whichever screen asks. */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Opens Google's code scanner and hands back what it read. The scanner is a
 * Play services screen of its own, so the app needs no camera permission for it.
 */
@Composable
fun rememberBarcodeScanner(onScanned: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.scanner_unavailable)
    val latest by rememberUpdatedState(onScanned)
    return remember(context) {
        {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                )
                .enableAutoZoom()
                .build()
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode -> barcode.rawValue?.let { latest(Barcodes.normalize(it)) } }
                .addOnFailureListener { scope.launch { snackbar.showSnackbar(unavailable) } }
        }
    }
}
