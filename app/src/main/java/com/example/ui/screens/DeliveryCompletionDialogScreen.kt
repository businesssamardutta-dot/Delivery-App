package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Order
import com.example.ui.components.SignatureCanvas
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryCompletionDialogScreen(
    order: Order,
    onClose: () -> Unit,
    onSubmitComplete: (orderId: String, collectedAmount: Double, signature: Bitmap?, proofPath: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var collectedAmountText by remember { mutableStateOf(String.format("%.0f", order.total_amount)) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var proofPhotoAttached by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HaribanshoBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Complete Delivery #${order.order_number}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HaribanshoPrimary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HaribanshoDanger.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = errorMessage!!, color = HaribanshoDanger, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            // COD Collection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Payment Verification", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    if (order.payment_mode == "COD") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cash to Collect:", fontSize = 14.sp, color = HaribanshoTextSecondary)
                            Text("₹${String.format("%.2f", order.total_amount)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HaribanshoPrimary)
                        }

                        OutlinedTextField(
                            value = collectedAmountText,
                            onValueChange = { collectedAmountText = it },
                            label = { Text("Collected Amount (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("collected_amount_input")
                        )
                    } else {
                        Surface(shape = RoundedCornerShape(8.dp), color = HaribanshoSuccess.copy(alpha = 0.15f)) {
                            Text("Prepaid Online Payment Already Received", color = HaribanshoSuccess, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            // Customer Signature
            SignatureCanvas(
                onSignatureCaptured = { signatureBitmap = it }
            )

            // Proof Photo Upload
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Delivery Proof Photo (Optional)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { proofPhotoAttached = !proofPhotoAttached },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (proofPhotoAttached) "Photo Attached ✓" else "Take Photo")
                        }

                        if (proofPhotoAttached) {
                            Text("Proof Ready", color = HaribanshoSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    val amt = collectedAmountText.toDoubleOrNull() ?: 0.0
                    if (order.payment_mode == "COD" && amt < order.total_amount) {
                        errorMessage = "Collected amount must be at least ₹${String.format("%.2f", order.total_amount)}"
                    } else {
                        onSubmitComplete(order.id, amt, signatureBitmap, if (proofPhotoAttached) "proof_photo_path" else null)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HaribanshoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_complete_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit & Complete Delivery", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
