package com.kxxr.sharide.screen

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.kxxr.sharide.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EWalletIntro(navController: NavController) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { BottomNavBar("eWallet",navController) }
    ) {paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Blue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Text(
                text = "Welcome to \n\nSHARide eWallet",
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ewallet_intro), // Replace with your error image resource
                contentDescription = "Error Icon",
                modifier = Modifier.size(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ditch the Cash, Glide with Ease \n\nSimplify Carpool Payments with Our New eWallet Payment system ",
                fontSize = 21.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(54.dp))

            // "Try Again" Button
            Button(
                onClick = {
                    showPinDialog = true
                }, // Navigate to the verification screen
                modifier = Modifier
                    .width(300.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(text = "Create PIN", color = Color.Blue)
            }

            Spacer(modifier = Modifier.height(66.dp))
        }
    }
    // Show the PIN input dialog when triggered
    if (showPinDialog) {
        PinInputDialog(
            title = "Create PIN",
            description = "Please create a 6-digit PIN to associate with your eWallet",
            onPinEntered = { enteredPin ->
                // Handle PIN submission
                Toast.makeText( context,"$enteredPin", Toast.LENGTH_SHORT).show()
                println("Entered PIN: $enteredPin")
                showPinDialog = false
                // TODO: Save the PIN or navigate
            },
            onDismiss = {
                showPinDialog = false
            }
        )
    }
}

@Composable
fun PinInputDialog(
    title: String,
    description: String,
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinCode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp)), // Black outline
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), // White background
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Title & Description
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, fontSize = 16.sp, textAlign = TextAlign.Center, color = Color.Black)

                Spacer(modifier = Modifier.height(16.dp))

                // PIN Display
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(2.dp, Color.Black, RoundedCornerShape(8.dp)) // Black border
                                .background(Color.White), // White fill
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < pinCode.length) {
                                Text(text = "•", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numpad
                Column {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                if (key.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            if (key == "⌫") {
                                                pinCode = pinCode.dropLast(1)
                                            } else if (pinCode.length < 6) {
                                                pinCode += key
                                            }
                                            if (pinCode.length == 6) {
                                                onPinEntered(pinCode)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(60.dp)
                                            .border(2.dp, Color.Black, CircleShape), // Black outline
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White) // White fill
                                    ) {
                                        Text(key, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(60.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
