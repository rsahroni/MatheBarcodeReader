package com.example.mathebarcodereader

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mathebarcodereader.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraView: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        cameraView = viewBinding.cameraView
        setContentView(viewBinding.root)

        if (hasCameraPermission()) {
            bindCameraUseCases()
        } else {
            requestPermission()
        }
    }

    private fun hasCameraPermission() = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraView.surfaceProvider)
                }

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_PDF417
                )
                .build()

            val scanner = BarcodeScanning.getClient(options)
            val analysisUseCase = ImageAnalysis.Builder()
                .build()
            analysisUseCase.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                ImageAnalysis.Analyzer { imageProxy ->
                    processImageProxy(scanner, imageProxy)
                }
            )
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase
                )
            } catch (illegalStateException: IllegalStateException) {
                Log.e(TAG, illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                Log.e(TAG, "bindCameraUseCases: ${illegalArgumentException.message.orEmpty()}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError", "StringFormatInvalid")
    private fun processImageProxy(
        scanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        val valueType = barcode.valueType
                        val completeValue: String =
                            "Type: ${valueType.toString()}\nValue: $rawValue"
                        viewBinding.bottomText.text = completeValue
//                        barcode?.rawValue?.let { value ->
//                            viewBinding.bottomText.text = value
//                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "processImageProxy: ${it.message.orEmpty()}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//            val preview:Preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(viewFinder.surfaceProvider)
//                }
//            cameraExecutor = Executors.newSingleThreadExecutor()
//            imageAnalyzer = MyImageAnalyzer()
//            val imageAnalysis = ImageAnalysis.Builder()
//                .setTargetResolution(Size(1280, 720))
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, imageAnalyzer)
//                }
//            val cameraSelector = CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build()
//
//            try {
//                //cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview, imageAnalysis
//                )
//            } catch (exc: Exception) {
//                Log.e(TAG, "Use case binding failed", exc)
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            bindCameraUseCases()
        } else {
            Toast.makeText(
                this,
                "Camera permission required",
                Toast.LENGTH_SHORT
            ).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val TAG = "MatheBarcodeScanner"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }
}

