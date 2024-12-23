package com.kxxr.sharide.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kxxr.sharide.R
import kotlinx.coroutines.delay

@Composable
fun IntroScreen() {
    val images = listOf(
        painterResource(id = R.drawable.intro1), // Replace with your image resource IDs
        painterResource(id = R.drawable.intro2),
        painterResource(id = R.drawable.intro3)
    )
    val introMain = listOf(
        "Reduce Cost",
        "Save Environment",
        "Stress Free Commute"
    )

    val introDesc = listOf(
        "Join the carpooling community and save time and money on your daily commute",
        "Reduce your carbon footprint and help to reduce traffic congestion",
        "Enjoy a stress-free commute with real-time carpool tracking and notifications"
    )

    var currentImageIndex by remember { mutableStateOf(0) }
    //val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // 5 seconds delay
            currentImageIndex = (currentImageIndex + 1) % images.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        // Logo
        Text(
            text = "SHARide",
            fontWeight = FontWeight.Bold,
            fontSize = 50.sp,
            color = Color.Blue
        )

        Spacer(modifier = Modifier.height(50.dp))

        // Rotating Image
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = images[currentImageIndex],
                contentDescription = "Rotating image",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Text
        Text(
            text = introMain[currentImageIndex],
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = Color.Blue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = introDesc[currentImageIndex],
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        // Three-dot Indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (currentImageIndex == index) 12.dp else 8.dp)
                        .background(
                            color = if (currentImageIndex == index) Color.Blue else Color.Gray,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(4.dp)
                )
                if (index != images.size - 1) {
                    Spacer(modifier = Modifier.width(5.dp)) // Space between dots
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Buttons
        Button(
            onClick = {
                // Navigate to Login
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp)
                .shadow(10.dp, shape = RoundedCornerShape(25.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(text = "Login", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Navigate to Sign Up
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp)
                .shadow(10.dp, shape = RoundedCornerShape(25.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(text = "Sign Up", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
