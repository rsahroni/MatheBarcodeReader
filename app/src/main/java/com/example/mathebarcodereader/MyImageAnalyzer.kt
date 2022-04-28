package com.example.mathebarcodereader

import android.annotation.SuppressLint
import android.util.Log
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.fragment.app.FragmentManager
import com.example.mathebarcodereader.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MyImageAnalyzer(): ImageAnalysis.Analyzer {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var tvScannedData: TextView

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        tvScannedData = viewBinding.tvScannedData
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val result = scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val bound = barcode.boundingBox
                    val corner = barcode.cornerPoints
                    val rawValue = barcode.rawValue

                    when (barcode.valueType) {
                        Barcode.TYPE_URL -> {
                            val title = barcode.url!!.title
                            val url = barcode.url!!.url
                            tvScannedData.text = url
                        }

                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "analyze: ${it.message}")
            }
    }

    companion object {
        private const val TAG = "MatheBarcodeScanner"
    }
}