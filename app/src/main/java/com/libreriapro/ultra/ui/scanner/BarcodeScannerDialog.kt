package com.libreriapro.ultra.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.CardElevated
import com.libreriapro.ultra.ui.theme.Green
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft
import java.util.concurrent.Executors

/**
 * Real barcode scanner: a CameraX preview decoded with ML Kit.
 *
 * Manual typing and hardware (USB / Bluetooth) wedge scanners are accepted too, so
 * the counter keeps working even when the tablet has no usable camera.
 */
@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcode: (String) -> Unit,
    title: String = "Escanear código de barras",
    hint: String = "Apunta la cámara al código del producto",
    status: String? = null,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    var manual by remember { mutableStateOf("") }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val submit: (String) -> Unit = { code ->
        val clean = code.trim()
        if (clean.isNotEmpty()) {
            onBarcode(clean)
            // Clears the field so a hardware scanner or the keyboard can type the next code.
            manual = ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📷", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(hint, color = Soft, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Soft)
                    }
                }
                Spacer(Modifier.height(12.dp))

                status?.let { text ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardElevated),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Green)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Green,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (granted) {
                    CameraScanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        onBarcode = submit,
                        onError = { cameraError = it },
                    )
                    cameraError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text("Cámara: $error", color = Red, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text("Cámara activa · lee EAN, UPC, Code128 y QR", color = Green, fontSize = 12.sp)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardElevated),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Permiso de cámara requerido", fontWeight = FontWeight.Bold)
                            Text(
                                "Concede el permiso para escanear con la cámara, o usa un lector " +
                                    "de códigos externo escribiendo el código abajo.",
                                color = Soft,
                                fontSize = 12.sp,
                            )
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Conceder permiso")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = manual,
                    onValueChange = { manual = it },
                    label = { Text("Código manual / lector externo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit(manual) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        unfocusedBorderColor = Soft.copy(alpha = 0.4f),
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { submit(manual) },
                        modifier = Modifier.weight(1f),
                        enabled = manual.isNotBlank(),
                    ) { Text("Buscar código") }
                    OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Consejo: escanear el mismo código otra vez suma una unidad más.",
                    color = Soft,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/**
 * CameraX preview decoded with ML Kit. Repeated reads of the same code are
 * debounced so one physical scan never registers the product twice.
 */
@Composable
private fun CameraScanner(
    modifier: Modifier = Modifier,
    onBarcode: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val currentOnError by rememberUpdatedState(onError)
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val analyzer = remember {
        object : ImageAnalysis.Analyzer {
            private var lastCode: String? = null
            private var lastTime = 0L

            /**
             * [ImageProxy.getImage] is still an experimental CameraX API, so the
             * reader opts in explicitly instead of silently suppressing the check.
             */
            @AndroidXOptIn(ExperimentalGetImage::class)
            override fun analyze(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return
                }
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val value = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                        if (value != null) {
                            val now = System.currentTimeMillis()
                            val isNew = value != lastCode
                            if (isNew || now - lastTime > REPEAT_DELAY_MS) {
                                lastCode = value
                                lastTime = now
                                currentOnBarcode(value)
                            }
                        }
                    }
                    .addOnFailureListener { error ->
                        currentOnError(error.localizedMessage ?: "No se pudo leer el código")
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, analyzer) }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                    boundProvider = provider
                } catch (error: Exception) {
                    currentOnError(error.localizedMessage ?: "No se pudo iniciar la cámara")
                }
            },
            mainExecutor,
        )
        onDispose {
            boundProvider?.unbindAll()
            analysisExecutor.shutdown()
            barcodeScanner.close()
        }
    }
}

/** Minimum delay before the same barcode is accepted again. */
private const val REPEAT_DELAY_MS = 1500L