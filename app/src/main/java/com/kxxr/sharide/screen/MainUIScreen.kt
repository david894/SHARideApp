package com.kxxr.sharide.screen


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.kxxr.logiclibrary.Login.resetPassword
import com.kxxr.logiclibrary.Ratings.loadRatingScore
import com.kxxr.logiclibrary.User.loadUserDetails
import com.kxxr.sharide.R

@Composable
fun ProfileScreen(firebaseAuth: FirebaseAuth, navController: NavController) {
    val currentUser = firebaseAuth.currentUser
    val context = LocalContext.current
    val firestore = Firebase.firestore

    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0.0) }
    var totalRating by remember { mutableStateOf(0) }

    var showDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var updatedPhoneNumber by remember { mutableStateOf(phoneNumber) }

    // Retrieve isDriver state from SharedPreferences
    val isDriver by remember { mutableStateOf(getDriverPreference(context)) }

    // Fetch user data from Firestore
    LaunchedEffect(Unit) {
        showDialog = true
        currentUser?.uid?.let { userId ->
            loadUserDetails(firestore,userId){
                if (it != null) {
                    userName = it.name
                    studentId = it.studentId
                    email = it.email
                    gender = it.gender
                    phoneNumber = it.phoneNumber
                    profileImageUrl = it.profileImageUrl
                }
            }
            loadRatingScore(firestore, userId) { Rating,TotalRating ->
                rating = Rating
                totalRating = TotalRating
                showDialog = false
            }
        }
    }

    // **Scaffold Layout for BottomAppBar Placement**
    Scaffold(
        bottomBar = { BottomNavBar("profile",navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(Color.Blue)
                .fillMaxWidth()
                .height(280.dp)
                .padding(20.dp)
                .clip(RoundedCornerShape(bottomStart = 60.dp, bottomEnd = 60.dp)) // Rounded bottom corners
        ) {
            Spacer(modifier = Modifier
                .height(36.dp)
                .padding(paddingValues))

            Text(text = "My Profile", fontSize = 26.sp,color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))

            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = rememberAsyncImagePainter(profileImageUrl),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(126.dp))

            Text(text = userName, fontSize = 20.sp, color = Color.White ,fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
            Text(text = "ID: $studentId", color = Color.White,fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                Image(
                    painter = painterResource(id = R.drawable.ratings_ico),
                    contentDescription = "Ratings Icon",
                    modifier = Modifier.size(20.dp)
                )
                Text("$rating/5.0 ($totalRating)", color = Color.White,fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 290.dp)
                .verticalScroll(rememberScrollState()), // Enable scrolling
                horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(text = "Settings", color = Color.Gray, fontSize = 20.sp, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                ProfileCard(title = "Reset Password", img = "reset_password", onClick = {
                    showDialog = true
                    resetPassword(email,context,
                        onSuccess = {
                            showDialog = false
                            showEmailDialog = true},
                        onFailure = {
                            showDialog = false
                        }
                    )
                })
                Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

                ProfileCard(title = "Enable 2FA Login", img = "authentication", onClick = { navController.navigate("check_mfa") })
                Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

                ProfileCard(title = "Edit Personal Info",img = "edit", onClick = { showEditDialog = true })
                Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

                ProfileCard(title = "View Rating Dashboard",img = "ratings_dashboard", onClick = { navController.navigate("ratingsDashboard") })
                Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

                if(isDriver){
                    ProfileCard(title = "Edit Vehicle Info",img = "car_front", onClick = { navController.navigate("addnewcar") })
                    Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom
                }
                else{
                    ProfileCard(title = "Driver Management",img = "manage", onClick = { navController.navigate("manage_driver") })
                    Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom
                }
            }

            Spacer(modifier = Modifier.height(50.dp)) // Pushes Log Out button to bottom

            // **Log Out Button**
            Button(
                onClick = {
                    firebaseAuth.signOut()
                    navController.navigate("intro") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .width(180.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(text = "Log Out", color = Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "© SHARide 2025 \n Ziegler Tan & David Ng\nTARUMT Project ONLY", color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(150.dp))

        }
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Email Sent") },
            text = { Text("An email has been sent to $email. Please check your inbox to reset your password.") },
            confirmButton = {
                Button(
                    onClick = { showEmailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("Done")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showEditDialog) {
        EditPersonalInfoDialog(
            userName = userName,
            studentId = studentId,
            email = email,
            gender = gender,
            phoneNumber = phoneNumber,
            context = context,
            onPhoneNumberChange = { updatedPhoneNumber = it },
            onConfirm = { newPhone ->
                showEditDialog = false
                // Save Phone Number to Firestore
                firestore.collection("users")
                    .whereEqualTo("firebaseUserId", currentUser?.uid)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val document = querySnapshot.documents.firstOrNull()
                        document?.reference?.update("phoneNumber", newPhone)
                            ?.addOnSuccessListener {
                                Toast.makeText(context, "Phone Number Updated!", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to Update Phone Number: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    }
            },
            onDismiss = { showEditDialog = false }
        )
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun ProfileCard(title: String,img: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageResId = remember(img) {
        context.resources.getIdentifier(img, "drawable", context.packageName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.LightGray)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, // Align items in the center
            modifier = Modifier
                .padding(8.dp)
                .padding(start = 10.dp) // Add padding for better spacing
        ) {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = title,
                    modifier = Modifier
                        .size(38.dp)
                        .padding(end = 8.dp) // Add spacing between image and text
                )
            } else {
                Text(
                    "Image Not Found",
                    color = Color.Red,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f) // Allow text to take remaining space
            )
        }

    }
}

@Composable
fun BottomNavBar(screen: String, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 36.dp), // Moves the navbar slightly above the bottom edge
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp) // Adjust width to make it smaller
                .height(78.dp) // Control height
                .padding(8.dp)
                .clip(RoundedCornerShape(30.dp)) // Rounded edges like a pebble
                .shadow(12.dp, RoundedCornerShape(30.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(30.dp)), // Floating shadow effect
            color = Color.White, // Background color
            tonalElevation = 8.dp // More elevation for a lifted effect
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp), // Adds spacing inside
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = R.drawable.home_ico,
                    label = "Home",
                    isSelected = screen == "home",
                    onClick = { navController.navigate("home") }
                )
                BottomNavItem(
                    icon = R.drawable.ewallet_ico,
                    label = "eWallet",
                    isSelected = screen == "eWallet",
                    onClick = { navController.navigate("ewallet") }
                )
                val currentUser = FirebaseAuth.getInstance().currentUser
                BottomNavItem(
                    icon = R.drawable.authentication,
                    label = "Chat",
                    isSelected = screen == "chat_list",
                    onClick = {
                        currentUser?.uid?.let { userId ->
                            navController.navigate("chat_list/$userId")
                        }
                    }
                )
                BottomNavItem(
                    icon = R.drawable.profile_ico,
                    label = "Profile",
                    isSelected = screen == "profile",
                    onClick = { navController.navigate("profile") }
                )
            }
        }
    }
}

// **Reusable Bottom Nav Item**
@Composable
fun BottomNavItem(icon: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = label,
            modifier = Modifier.size(30.dp),
            colorFilter = if (isSelected) ColorFilter.tint(Color.Blue) else null
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) Color.Blue else Color.Gray
        )
    }
}


@Composable
fun EditPersonalInfoDialog(
    userName: String,
    studentId: String,
    email: String,
    gender: String,
    phoneNumber: String,
    context: Context,
    onPhoneNumberChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var updatedPhoneNumber by remember { mutableStateOf(phoneNumber) }
    var isPhoneValid by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Personal Info") },
        text = {
            Column {
                OutlinedTextField(
                    value = userName,
                    onValueChange = {},
                    label = { Text("Name") },
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        unfocusedContainerColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studentId,
                    onValueChange = {},
                    label = { Text("TARUMT ID") },
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        unfocusedContainerColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email") },
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        unfocusedContainerColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    label = { Text("Gender") },
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        unfocusedContainerColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = updatedPhoneNumber,
                    onValueChange = {
                        updatedPhoneNumber = it
                        onPhoneNumberChange(it)
                        isPhoneValid = updatedPhoneNumber.startsWith("+601") && updatedPhoneNumber.length <= 13 && updatedPhoneNumber.length >= 12
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Phone)
                )
                if (!isPhoneValid) {
                    Text(text = "Invalid Malaysian phone number", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!isPhoneValid) {
                    Toast.makeText(context, "Invalid Phone Number!", Toast.LENGTH_SHORT).show()
                }else{
                    onConfirm(updatedPhoneNumber)
                }
            },colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss,colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("Cancel")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

