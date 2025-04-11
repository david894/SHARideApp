package com.kxxr.sharmin.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.kxxr.logiclibrary.AiDetector.detectFaceFromIdCard
import com.kxxr.logiclibrary.AiDetector.rotateBitmap
import com.kxxr.logiclibrary.Login.resetPassword
import com.kxxr.logiclibrary.SignUp.painterResourceToBitmap
import com.kxxr.logiclibrary.User.User
import com.kxxr.logiclibrary.User.searchUsers
import com.kxxr.sharmin.R
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.kxxr.logiclibrary.Banned.banUser
import com.kxxr.logiclibrary.Banned.isBanned
import com.kxxr.logiclibrary.Banned.unbanUser
import com.kxxr.logiclibrary.SignUp.saveUserData
import com.kxxr.logiclibrary.SignUp.uploadProfilePicture
import com.kxxr.logiclibrary.Driver.Driver
import com.kxxr.logiclibrary.Driver.Vehicle
import com.kxxr.logiclibrary.Driver.searchDriver
import com.kxxr.logiclibrary.Driver.searchVehicle
import com.kxxr.logiclibrary.ManualCase.CompleteDriverCase
import com.kxxr.logiclibrary.ManualCase.CompleteIDCase
import com.kxxr.logiclibrary.ManualCase.DriverCase
import com.kxxr.logiclibrary.ManualCase.SearchPendingCases
import com.kxxr.logiclibrary.ManualCase.UserCase
import com.kxxr.logiclibrary.ManualCase.loadCaseByDriver
import com.kxxr.logiclibrary.ManualCase.loadCaseById
import com.kxxr.logiclibrary.ManualCase.searchPendingDriverCases
import com.kxxr.logiclibrary.ManualCase.sendEmail
import com.kxxr.logiclibrary.ManualCase.updateCaseStatus
import com.kxxr.logiclibrary.ManualCase.updateDupUser
import com.kxxr.logiclibrary.ManualCase.updateVehicle
import com.kxxr.logiclibrary.ManualCase.uploadLicenseAndProfile
import com.kxxr.logiclibrary.ManualCase.uploadVehicleImagesAndSave
import com.kxxr.logiclibrary.Ratings.loadRatingScore
import com.kxxr.logiclibrary.Ratings.ratingAPI
import com.kxxr.logiclibrary.SignUp.saveDriverToFirestore
import com.kxxr.logiclibrary.User.loadUserDetails
import com.kxxr.logiclibrary.eWallet.loadWalletBalance

// Main Screen to Search and Select User
@Composable
fun ReviewUserScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var idCases by remember { mutableStateOf<List<UserCase>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isAvaliableOpen by remember { mutableStateOf(false) }
    var isCompleteOpen by remember { mutableStateOf(false) }
    var completeIDCases by remember { mutableStateOf<List<UserCase>>(emptyList()) }

    LaunchedEffect(Unit) {
        showDialog = true
        SearchPendingCases(firestore,"",context) { cases ->
            idCases = cases
            showDialog = false
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .padding(top = 50.dp)
        .verticalScroll(rememberScrollState())
    ){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { navController.navigate("home") }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Review User Cases",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Name, Email, or Student ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                showDialog = true
                SearchPendingCases(firestore, searchQuery, context) {
                    idCases = it
                    showDialog = false
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(modifier = Modifier.height(24.dp))

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)) // Floating shadow effect
                .clickable(
                    onClick = {
                        isAvaliableOpen = !isAvaliableOpen
                        if (isAvaliableOpen) SearchPendingCases(firestore, "", context) { cases ->
                            idCases = cases
                        }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(if (isAvaliableOpen) "Hide Pending Case ▲ " else "Show Pending Case ▼", modifier = Modifier.padding(start = 10.dp))
        }
        if(isAvaliableOpen){
            if(idCases.isEmpty()){
                Text("No pending cases to review.")
            }

            Column {
                idCases.forEach { idCase ->
                    IdCaseCard(idCase) {
                        navController.navigate("caseDetail/${idCase.caseId}")
                    }
                }
            }
        }

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)) // Floating shadow effect
                .clickable(
                    onClick = {
                        isCompleteOpen = !isCompleteOpen
                        if (isCompleteOpen) CompleteIDCase(firestore, context) { cases ->
                            completeIDCases = cases
                        }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(if (isCompleteOpen) "Hide Completed Case ▲ " else "Show Completed Case ▼", modifier = Modifier.padding(start = 10.dp))
        }
        if(isCompleteOpen){
            if(completeIDCases.isEmpty()){
                Text("No completed cases to review.")
            }

            Column {
                completeIDCases.forEach { idCase ->
                    IdCaseCard(idCase) {
                        navController.navigate("caseDetail/${idCase.caseId}")
                    }
                }
            }
        }
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

// Case Card UI
@Composable
fun IdCaseCard(idCase: UserCase, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardColors(containerColor = Color.White, contentColor = Color.Black, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Name: ${idCase.name}", fontWeight = FontWeight.Bold)
            Text("Email: ${idCase.email}")
            Text("ID: ${idCase.studentId}")
            if(idCase.status != ""){
                Text("Status: ${idCase.status}",color = if(idCase.status == "Approved") Color(0xFF008000) else Color(0xFFCC0000))
            }
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onClick,
                colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
            ) {
                Text("Review")
            }
        }
    }
}

@Composable
fun CaseDetailScreen(navController: NavController, caseId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var case by remember { mutableStateOf<UserCase?>(null) }
    var remark by remember { mutableStateOf("") }
    var decision by remember { mutableStateOf("") }
    var dupIDUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var dupEmailUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var overwrite by remember { mutableStateOf("") }
    var userIDIndex by remember { mutableStateOf(0) }
    var userEmailIndex by remember { mutableStateOf(0) }

    var rotation by remember { mutableStateOf(0f) }
    var rotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var profileBitmap = painterResourceToBitmap(context, R.drawable.pre_profile)
    var showDialog by remember { mutableStateOf(false) }
    var newFirebaseID by remember { mutableStateOf("") }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailContent by remember { mutableStateOf("") }

    LaunchedEffect(caseId) {
        showDialog = true
        loadCaseById(firestore, caseId) { loadedCase ->
            case = loadedCase
            val storageReference =
                FirebaseStorage.getInstance().getReferenceFromUrl(case!!.studentIdLink)
            storageReference.getBytes(1024 * 1024)
                .addOnSuccessListener { bytes ->
                    rotatedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            showDialog = false
        }
    }

    case?.let { idCase ->
        searchUsers(firestore, idCase.email, context, onResult = {
            dupEmailUsers = it
        })

        searchUsers(firestore, idCase.studentId, context, onResult = {
            dupIDUsers = it
        })

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 50.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Review Case",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("1. Case Personal Info:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // User Details
            Text("Name: ${idCase.name}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Email: ${idCase.email}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Phone: ${idCase.phone}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Student ID: ${idCase.studentId}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Gender: ${if(idCase.gender == "M")"Male" else "Female" }")
            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(16.dp))
            Text("2. Case Student ID Photo:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            rotatedBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selfie Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(vertical = 16.dp)
                )
            }
            Text("* Rotate for correct face recognition")
            // Rotate Button
            Button(
                onClick = {
                    rotation += 90f
                    if (rotation >= 360f) {
                        rotation = 0f
                    }
                    // Create rotated bitmap
                    rotatedBitmap = rotatedBitmap?.let { rotateBitmap(it, rotation) }
                },
                colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                Text("Rotate 90°")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("3. Case Selfie with ID:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(idCase.selfieLink),
                contentDescription = "Selfie Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            if(idCase.status == ""){
                Text("Conflict User Details :", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                if(dupIDUsers.isNotEmpty()){
                    dupIDUsers.forEachIndexed{ index,dupIDUsers ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { userIDIndex = index }
                        ) {
                            RadioButton(
                                selected = userIDIndex == index,
                                onClick = { userIDIndex = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Blue,
                                    unselectedColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text("${index + 1}. Conflict ID Personal Info:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // User Details
                                Text("Name: ${dupIDUsers.name}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Email: ${dupIDUsers.email}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Phone: ${dupIDUsers.phoneNumber}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Student ID: ${dupIDUsers.studentId}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Gender: ${if(dupIDUsers.gender == "M")"Male" else "Female" }")
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }else{
                    Text("No ID conflict user found.")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if(dupEmailUsers.isNotEmpty()){
                    dupEmailUsers.forEachIndexed { index, dupEmailUsers ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { userEmailIndex = index }
                        ) {
                            RadioButton(
                                selected = userEmailIndex == index,
                                onClick = { userEmailIndex = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Blue,
                                    unselectedColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "${index + 1}. Conflict Email Personal Info:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // User Details
                                Text("Name: ${dupEmailUsers.name}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Email: ${dupEmailUsers.email}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Phone: ${dupEmailUsers.phoneNumber}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Student ID: ${dupEmailUsers.studentId}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Gender: ${if (dupEmailUsers.gender == "M") "Male" else "Female"}")
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }else{
                    Text("No Email conflict user found.")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Remark for this case:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("* Remark") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if(dupIDUsers.isNotEmpty() && dupEmailUsers.isNotEmpty()){
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.clickable { overwrite = "ID" },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = overwrite == "ID",
                                onClick = { overwrite = "ID" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Blue,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(text = "Overwrite ID User")
                        }
                        Spacer(modifier = Modifier.width(28.dp))
                        Row(
                            modifier = Modifier.clickable { overwrite = "Email" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = overwrite == "Email",
                                onClick = { overwrite = "Email" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Blue,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(text = "Overwrite Email User")
                        }
                    }

                }
                Spacer(modifier = Modifier.height(16.dp))

                // Approve Button
                Button(
                    onClick = {
                        decision = "Approved"
                        if(remark.isNotEmpty()){
                            if(dupIDUsers.isNotEmpty() && dupEmailUsers.isNotEmpty() && overwrite != ""){
                                if(overwrite == "ID"){
                                    showDialog = true
                                    updateDupUser(firestore, dupIDUsers[userIDIndex], idCase, context)
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, "ID",context)
                                    showDialog = false
                                    showEmailDialog = true

                                }else if (overwrite == "Email"){
                                    showDialog = true
                                    updateDupUser(firestore, dupEmailUsers[userEmailIndex], idCase, context)
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, "ID",context)
                                    showDialog = false
                                    showEmailDialog = true

                                }
                            }else if(dupIDUsers.isEmpty() || dupEmailUsers.isEmpty()){
                                if(dupIDUsers.isNotEmpty() ){
                                    showDialog = true
                                    updateDupUser(firestore, dupIDUsers[userIDIndex], idCase, context)
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, "ID",context)
                                    showDialog = false
                                    showEmailDialog = true

                                }else if (dupEmailUsers.isNotEmpty()){
                                    showDialog = true
                                    updateDupUser(firestore, dupEmailUsers[userEmailIndex], idCase, context)
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, "ID",context)
                                    showDialog = false
                                    showEmailDialog = true

                                }else{
                                    showDialog = true

                                    // Replace with your API key and endpoint URL
                                    val apiKey = context.getString(R.string.rest_api)
                                    val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
                                    val userpass = context.getString(R.string.signup_pass)

                                    // Create the JSON payload
                                    val jsonRequest = JSONObject().apply {
                                        put("email", idCase.email)
                                        put("password", userpass)
                                        put("returnSecureToken", true)
                                    }

                                    // Create the Volley request
                                    val request = JsonObjectRequest(Request.Method.POST, url, jsonRequest,
                                        { response ->
                                            // Handle the successful response here
                                            newFirebaseID = response.getString("localId")
                                            val idToken = response.optString("idToken")

                                            sendEmailVerificationUsingVolley(context, apiKey, idToken, onSuccess = {})

                                            rotatedBitmap?.let {
                                                detectFaceFromIdCard(it) { faceBitmap ->
                                                    if (faceBitmap != null) {
                                                        // Save the face image
                                                        profileBitmap = faceBitmap
                                                    }
                                                }
                                            }

                                            uploadProfilePicture(newFirebaseID, profileBitmap, { imageUrl ->
                                                saveUserData(firestore, newFirebaseID, idCase.name,
                                                    idCase.studentId, idCase.email, idCase.phone, idCase.gender, imageUrl,
                                                    context, navController,"admin", onFinish={
                                                        resetPassword(idCase.email,context, onSuccess = {}, onFailure = {})

                                                        updateCaseStatus(firestore, idCase.caseId, decision, remark, "ID",context)
                                                        showEmailDialog = true
                                                        showDialog = false
                                                    }
                                                )
                                            }, { e ->
                                                Toast.makeText(context, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                                                showDialog = false
                                            })
                                        },
                                        { error ->
                                            // Handle error
                                            Toast.makeText(context, "Failed to create user account", Toast.LENGTH_SHORT).show()
                                            showDialog = false
                                        })
                                    // Add the request to the RequestQueue
                                    Volley.newRequestQueue(context).add(request)
                                }
                            }else{
                                Toast.makeText(context, "Please select which user to overwrite", Toast.LENGTH_SHORT).show()
                            }
                        }else{
                            Toast.makeText(context, "Please enter a remark", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonColors(containerColor = Color(0xFF008000), contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
                ) {
                    Text("Approve")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Reject Button
                Button(
                    onClick = {
                        decision = "Rejected"
                        if(remark.isNotEmpty()){
                            updateCaseStatus(firestore, idCase.caseId, decision, remark, "ID",context)
                            showEmailDialog = true
                        }else{
                            Toast.makeText(context, "Please enter a remark", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonColors(containerColor = Color(0xFFCC0000), contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
                ) {
                    Text("Reject")
                }
            }

            if(idCase.status != ""){
                OutlinedTextField(
                    value = idCase.remark,
                    onValueChange = { remark = it },
                    enabled = false,
                    label = { Text("* Remark") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Case Status: ${idCase.status}",color = if(idCase.status == "Approved") Color(0xFF008000) else Color(0xFFCC0000),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Button(
                        onClick = {
                            showEmailDialog = false
                            emailContent(idCase.status, idCase.email, idCase.name, idCase.remark, context, onResult = {
                                emailContent = it
                            })

                            sendEmail(
                                context = context,
                                recipient = idCase.email, // Replace with dynamic user email
                                subject = "Case Update Notification from SHARide Team",
                                content = emailContent
                            )
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("Resend Email")
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))

            }

            if (showEmailDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showEmailDialog = false
                        navController.popBackStack()
                    },
                    title = { Text("Update Case Successfully !") },
                    text = { Text("Would you like to email the user with the case update?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEmailDialog = false
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                        ) {
                            Text("No Thanks")
                        }
                        Button(
                            onClick = {
                                showEmailDialog = false
                                emailContent(decision, idCase.email, idCase.name, remark, context, onResult = {
                                    emailContent = it
                                })

                                sendEmail(
                                    context = context,
                                    recipient = idCase.email, // Replace with dynamic user email
                                    subject = "Case Update Notification from SHARide Team",
                                    content = emailContent
                                )
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                        ) {
                            Text("Send Email")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        // Show Loading Dialog
        LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
    }
}

fun emailContent(decision:String, userEmail:String, userName:String,remark:String, context: Context,onResult: (String) -> Unit){
    if (decision == "Approved") {
        onResult(
            """
            Dear ${userName},
            
            We are pleased to inform you that your case has been $decision.
            
            Here are your login details:
            Email: ${userEmail}
            Password:  ${context.getString(R.string.signup_pass)}
            
            Please change your password after logging in for security purposes.
    
            If you have any questions, feel free to contact support.
    
            Best regards,
            Your SHARide Team
            """.trimIndent()
        )
    } else {
        onResult(
            """
            Dear ${userName},
            
            Unfortunately, your case has been $decision.
    
            Reason for Rejection:
            $remark
            
            If you believe this was an error, please contact our support team.
    
            Best regards,
            Your SHARide Team
        """.trimIndent()
        )

    }
}


fun sendEmailVerificationUsingVolley(
    context: Context,
    apiKey: String,
    idToken: String,
    onSuccess: () -> Unit,
) {
    val url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$apiKey"

    val jsonBody = JSONObject().apply {
        put("requestType", "VERIFY_EMAIL")
        put("idToken", idToken)
    }

    val request = JsonObjectRequest(
        Request.Method.POST, url, jsonBody,
        { response ->
            Toast.makeText(context, "Verification email sent!", Toast.LENGTH_SHORT).show()
            onSuccess()
        },
        { error ->
            Toast.makeText(context, "Failed to send verification email", Toast.LENGTH_SHORT).show()
        }
    )

    Volley.newRequestQueue(context).add(request)
}

// Main Screen to Search and Select User
@Composable
fun ReviewDriverScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var driverCases by remember { mutableStateOf<List<DriverCase>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isAvaliableOpen by remember { mutableStateOf(false) }
    var isCompleteOpen by remember { mutableStateOf(false) }
    var completeDriverCases by remember { mutableStateOf<List<DriverCase>>(emptyList()) }

    LaunchedEffect(Unit) {
        showDialog = true
        searchPendingDriverCases(firestore,"",context) { cases ->
            driverCases = cases
            showDialog = false
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .padding(top = 50.dp)
        .verticalScroll(rememberScrollState())
    ){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { navController.navigate("home") }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Review Driver Cases",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Name, Plate No, or Driver ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                showDialog = true
                searchPendingDriverCases(firestore, searchQuery, context) {
                    driverCases = it
                    showDialog = false
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(modifier = Modifier.height(24.dp))

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)) // Floating shadow effect
                .clickable(
                    onClick = {
                        isAvaliableOpen = !isAvaliableOpen
                        if (isAvaliableOpen) searchPendingDriverCases(
                            firestore,
                            "",
                            context
                        ) { cases ->
                            driverCases = cases
                        }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(if (isAvaliableOpen) "Hide Pending Case ▲ " else "Show Pending Case ▼", modifier = Modifier.padding(start = 10.dp))
        }
        if(isAvaliableOpen){
            if(driverCases.isEmpty()){
                Text("No pending driver cases to review.")
            }

            Column {
                driverCases.forEach { idCase ->
                    DriverCaseCard(idCase) {
                        navController.navigate("driverCaseDetail/${idCase.caseId}")
                    }
                }
            }
        }

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)) // Floating shadow effect
                .clickable(
                    onClick = {
                        isCompleteOpen = !isCompleteOpen
                        if (isCompleteOpen) CompleteDriverCase(firestore, context) { cases ->
                            completeDriverCases = cases
                        }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(if (isCompleteOpen) "Hide Completed Case ▲ " else "Show Completed Case ▼", modifier = Modifier.padding(start = 10.dp))
        }
        if(isCompleteOpen){
            if(completeDriverCases.isEmpty()){
                Text("No completed cases to review.")
            }

            Column {
                completeDriverCases.forEach { idCase ->
                    DriverCaseCard(idCase) {
                        navController.navigate("driverCaseDetail/${idCase.caseId}")
                    }
                }
            }
        }
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}


// Case Card UI
@Composable
fun DriverCaseCard(case: DriverCase, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardColors(containerColor = Color.White, contentColor = Color.Black, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Driver Name: ${case.driverName}", fontWeight = FontWeight.Bold)
            Text("Driver ID: ${case.driverId}")
            Text("Plate No: ${case.CarRegistrationNumber}")
            if(case.status != ""){
                Text("Status: ${case.status}",color = if(case.status == "Approved") Color(0xFF008000) else Color(0xFFCC0000))
            }
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onClick,
                colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
            ) {
                Text("Review")
            }
        }
    }
}

@Composable
fun DriverCaseDetailScreen(navController: NavController, caseId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var case by remember { mutableStateOf<DriverCase?>(null) }
    var remark by remember { mutableStateOf("") }
    var decision by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<User?>(null) }
    var dupDriverID by remember { mutableStateOf<List<Driver>>(emptyList()) }
    var dupVehicle by remember { mutableStateOf<List<Vehicle>>(emptyList()) }

    var overwrite by remember { mutableStateOf("") }
    var driverIDIndex by remember { mutableStateOf(0) }
    var vehicleNoIndex by remember { mutableStateOf(0) }

    var rotation by remember { mutableStateOf(0f) }
    var rotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotatedFrontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotatedBackBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userProfile by remember { mutableStateOf<Bitmap?>(null) }
    var selfie by remember { mutableStateOf<Bitmap?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var samePerson by remember { mutableStateOf(false) }

    var showEmailDialog by remember { mutableStateOf(false) }
    var emailContent by remember { mutableStateOf("") }
    val firebaseStorage = FirebaseStorage.getInstance()
    var profilePicture by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(caseId) {
        showDialog = true
        loadCaseByDriver(firestore, caseId) { loadedCase ->
            case = loadedCase
            val storageReference =
                FirebaseStorage.getInstance().getReferenceFromUrl(case!!.IDPhoto)
            storageReference.getBytes(1024 * 1024)
                .addOnSuccessListener { bytes ->
                    rotatedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            val carFrontReference =
                FirebaseStorage.getInstance().getReferenceFromUrl(case!!.CarFrontPhoto)
            carFrontReference.getBytes(1024 * 1024)
                .addOnSuccessListener { bytes ->
                    rotatedFrontBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            val carBackReference =
                FirebaseStorage.getInstance().getReferenceFromUrl(case!!.CarBackPhoto)
            carBackReference.getBytes(1024 * 1024)
                .addOnSuccessListener { bytes ->
                    rotatedBackBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            val driverselfie =
                FirebaseStorage.getInstance().getReferenceFromUrl(case!!.driverSelfie)
            driverselfie.getBytes(1024 * 1024)
                .addOnSuccessListener { bytes ->
                    selfie = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            showDialog = false
        }
    }

    case?.let { idCase ->
        LaunchedEffect(Unit) {
            loadUserDetails(firestore, idCase.UserID, onResult = {
                users = it
            })
            searchDriver(firestore, idCase.driverId, context, onResult = {
                dupDriverID = it
            })

            searchVehicle(firestore, idCase.CarRegistrationNumber, context, onResult = {
                dupVehicle = it
            })
            if(users?.profileImageUrl != ""){
                val profilepic =
                    FirebaseStorage.getInstance().getReferenceFromUrl(case!!.CarBackPhoto)
                profilepic.getBytes(1024 * 1024)
                    .addOnSuccessListener { bytes ->
                        userProfile = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
        }


        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 50.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Review Driver Case",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("1. Driver Personal Info:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // User Details
            Text("Name: ${idCase.driverName}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Driving ID: ${idCase.driverId}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("ID Photo", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            rotatedBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "ID Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(vertical = 16.dp)
                )
            }
            Text("* Rotate for correct ID recognition")
            // Rotate Button
            Button(
                onClick = {
                    rotation += 90f
                    if (rotation >= 360f) {
                        rotation = 0f
                    }
                    // Create rotated bitmap
                    rotatedBitmap = rotatedBitmap?.let { rotateBitmap(it, rotation) }
                },
                colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                Text("Rotate 90°")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Selfie with Driver ID",fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(idCase.driverSelfie),
                contentDescription = "Selfie Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("2. Vehicle Details:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Plate No: ${idCase.CarRegistrationNumber}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Car Make: ${idCase.CarMake}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Car Model: ${idCase.CarModel}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Car Colour: ${idCase.CarColour}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Car Type: ${idCase.CarType}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Pet Preference: ${idCase.PetPreference}")
            Spacer(modifier = Modifier.height(8.dp))

            Text("Car Front Image:",fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            rotatedFrontBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Car Front Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(vertical = 16.dp)
                )
            }
            Text("* Rotate for correct plate no recognition")
            // Rotate Button
            Button(
                onClick = {
                    rotation += 90f
                    if (rotation >= 360f) {
                        rotation = 0f
                    }
                    // Create rotated bitmap
                    rotatedFrontBitmap = rotatedFrontBitmap?.let { rotateBitmap(it, rotation) }
                },
                colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                Text("Rotate 90°")
            }

            Text("Car Back Image:",fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            rotatedBackBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Car Back Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(vertical = 16.dp)
                )
            }
            Text("* Rotate for correct plate no recognition")
            // Rotate Button
            Button(
                onClick = {
                    rotation += 90f
                    if (rotation >= 360f) {
                        rotation = 0f
                    }
                    // Create rotated bitmap
                    rotatedBackBitmap = rotatedBackBitmap?.let { rotateBitmap(it, rotation) }
                },
                colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                Text("Rotate 90°")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("3. Request User Details:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Name: ${users?.name}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Email: ${users?.email}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Phone: ${users?.phoneNumber}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Gender: ${if(users?.gender == "M")"Male" else "Female" }")
            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            if(idCase.status == ""){
                Text("Conflict Driver Details :", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                if(dupDriverID.isNotEmpty()){
                    dupDriverID.forEachIndexed{ index,dupDriverID ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { driverIDIndex = index }
                        ) {
                            RadioButton(
                                selected = driverIDIndex == index,
                                onClick = { driverIDIndex = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Blue,
                                    unselectedColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))
                            if(dupDriverID.userId == users!!.firebaseUserId) samePerson=true
                            Column {
                                Text("${index + 1}. Conflict Driver Personal Info:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // User Details
                                Text("Name: ${dupDriverID.name}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Driving ID: ${dupDriverID.drivingId}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Vehicle No: ${dupDriverID.vehiclePlate}")
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }else{
                    Text("No conflict driver found.")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if(dupVehicle.isNotEmpty()){
                    dupVehicle.forEachIndexed { index, dupVehicle ->
                        var owner by remember { mutableStateOf<User?>(null) }
                        loadUserDetails(firestore, dupVehicle.UserID) {
                            owner = it
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vehicleNoIndex = index }
                        ) {
                            RadioButton(
                                selected = vehicleNoIndex == index,
                                onClick = { vehicleNoIndex = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Blue,
                                    unselectedColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "${index + 1}. Conflict Vehicle Info:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Vehicle Details
                                Text("Car Make: ${dupVehicle.CarMake}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Car Model: ${dupVehicle.CarModel}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Car Colour: ${dupVehicle.CarColour}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Car Type: ${dupVehicle.CarType}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Pet Preference: ${dupVehicle.PetPreference}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Owner Name: ${owner?.name}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Owner Phone: ${owner?.phoneNumber}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Owner Email: ${owner?.email}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Car Front Image: ")
                                Spacer(modifier = Modifier.height(8.dp))

                                Image(
                                    painter = rememberAsyncImagePainter(dupVehicle.CarFrontPhoto),
                                    contentDescription = "Car Front Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .padding(vertical = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Car Back Image: ")
                                Spacer(modifier = Modifier.height(8.dp))

                                Image(
                                    painter = rememberAsyncImagePainter(dupVehicle.CarBackPhoto),
                                    contentDescription = "Car Back Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .padding(vertical = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                            }
                        }
                    }
                }else{
                    Text("No conflict vehicle found.")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Remark for this case:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("* Remark") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if(dupDriverID.isNotEmpty() && dupVehicle.isNotEmpty()){
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if(samePerson){
                            Row(
                                modifier = Modifier.clickable { overwrite = "Vehicle" },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = overwrite == "Vehicle",
                                    onClick = { overwrite = "Vehicle" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color.Blue,
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Text(text = "Overwrite Vehicle Only")
                            }
                            Spacer(modifier = Modifier.width(28.dp))
                        }

                        Row(
                            modifier = Modifier.clickable { overwrite = "Combine" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = overwrite == "Combine",
                                onClick = { overwrite = "Combine" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Blue,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(text = "Overwrite Driver & Vehicle")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                // Approve Button
                Button(
                    onClick = {
                        decision = "Approved"
                        if(remark.isNotEmpty()){
                            showDialog = true

                            if(overwrite == "" && dupDriverID.isNotEmpty() && dupVehicle.isNotEmpty()){
                                Toast.makeText(context, "Please select which item to overwrite", Toast.LENGTH_SHORT).show()
                                showDialog = false
                            }

                            if(overwrite == "" && dupDriverID.isEmpty() && dupVehicle.isEmpty()){
                                rotatedBitmap?.let {
                                    detectFaceFromIdCard(it) { faceBitmap ->
                                        if (faceBitmap != null) {
                                            // Save the face image
                                            profilePicture = faceBitmap
                                        }else{
                                            Toast.makeText(context, "No Face Found on ID!", Toast.LENGTH_SHORT).show()
                                            showDialog = false
                                        }
                                    }
                                }
                                // Upload lesen and profile picture to Firebase Storage
                                users?.let { user ->
                                    uploadLicenseAndProfile(user.firebaseUserId, rotatedBitmap, userProfile, selfie, firebaseStorage) { lesenUrl, profileUrl, selfieUrl ->
                                        if (lesenUrl != null && profileUrl != null && selfieUrl != null) {
                                            saveDriverToFirestore(user.firebaseUserId, case!!.driverName, case!!.driverId, profileUrl, lesenUrl, selfieUrl, firestore) {
                                                uploadVehicleImagesAndSave(context, caseId, case, user.firebaseUserId, rotatedFrontBitmap, rotatedBackBitmap, firestore, firebaseStorage,
                                                    onComplete = {
                                                        updateCaseStatus(firestore, idCase.caseId, decision, remark, "Driver Case",context)
                                                        showDialog = false
                                                        showEmailDialog = true
                                                    }, onFailure = {
                                                        showDialog = false
                                                    }
                                                )
                                            }
                                        } else {
                                            Toast.makeText(context, "Error uploading driver images", Toast.LENGTH_SHORT).show()
                                            showDialog = false
                                        }
                                    }
                                }
                            }
                            if(overwrite == "Combine" || (dupVehicle.isNotEmpty() && overwrite == "" && dupDriverID.isEmpty()) || (dupDriverID.isNotEmpty() && overwrite == "" && dupVehicle.isEmpty())){
                                rotatedBitmap?.let {
                                    detectFaceFromIdCard(it) { faceBitmap ->
                                        if (faceBitmap != null) {
                                            // Save the face image
                                            profilePicture = faceBitmap
                                        }else{
                                            Toast.makeText(context, "No Face Found on ID!", Toast.LENGTH_SHORT).show()
                                            showDialog = true
                                        }
                                    }
                                }
                                // Upload lesen and profile picture to Firebase Storage
                                users?.let { user ->
                                    firestore.collection("driver")
                                        .whereEqualTo("drivingId", case!!.driverId)
                                        .get()
                                        .addOnSuccessListener{ snapshot ->
                                            if (!snapshot.isEmpty) {
                                                // Delete all found duplicate driver documents
                                                snapshot.documents.forEach { document ->
                                                    document.reference.delete()
                                                }
                                            }

                                            // Upload lesen and profile picture to Firebase Storage
                                            uploadLicenseAndProfile(user.firebaseUserId, rotatedBitmap, userProfile, selfie, firebaseStorage) { lesenUrl, profileUrl, selfieUrl ->
                                                if (lesenUrl != null && profileUrl != null && selfieUrl != null) {
                                                    saveDriverToFirestore(user.firebaseUserId, case!!.driverName, case!!.driverId, profileUrl, lesenUrl, selfieUrl, firestore) {
                                                        if(dupVehicle.isEmpty()){
                                                            uploadVehicleImagesAndSave(context, caseId, case, user.firebaseUserId, rotatedFrontBitmap, rotatedBackBitmap, firestore, firebaseStorage,
                                                                onComplete = {
                                                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, "Driver Case",context)
                                                                    showDialog = false
                                                                    showEmailDialog = true
                                                                }, onFailure = {
                                                                    showDialog = false
                                                                }
                                                            )
                                                        }else{
                                                            dupVehicle.forEach{ dupVehicle ->
                                                                updateVehicle(context, firestore, firebaseStorage, users, case, dupVehicle, rotatedFrontBitmap, rotatedBackBitmap,
                                                                    onComplete = {
                                                                        updateCaseStatus(firestore, idCase.caseId, decision, remark, "Driver Case",context)
                                                                        showDialog = false
                                                                        showEmailDialog = true
                                                                    }, onFailure = {
                                                                        showDialog = false
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Error uploading driver images", Toast.LENGTH_SHORT).show()
                                                    showDialog = false
                                                }
                                            }
                                        }.addOnFailureListener{
                                            Toast.makeText(context, "Error deleting conflict driver", Toast.LENGTH_SHORT).show()
                                            // Upload lesen and profile picture to Firebase Storage
                                            uploadLicenseAndProfile(user.firebaseUserId, rotatedBitmap, userProfile, selfie, firebaseStorage) { lesenUrl, profileUrl, selfieUrl ->
                                                if (lesenUrl != null && profileUrl != null && selfieUrl != null) {
                                                    saveDriverToFirestore(user.firebaseUserId, case!!.driverName, case!!.driverId,
                                                        profileUrl, lesenUrl, selfieUrl, firestore
                                                    ) {
                                                        dupVehicle.forEach { dupVehicle ->
                                                            updateVehicle(context, firestore, firebaseStorage, users,
                                                                case, dupVehicle, rotatedFrontBitmap, rotatedBackBitmap,
                                                                onComplete = {
                                                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, "Driver Case",context)
                                                                    showDialog = false
                                                                    showEmailDialog = true
                                                                },
                                                                onFailure = {
                                                                    showDialog = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Error uploading driver images", Toast.LENGTH_SHORT).show()
                                                    showDialog = false
                                                }
                                            }
                                        }
                                }

                            }
                            if(overwrite == "Vehicle" && dupVehicle.isNotEmpty()&& samePerson){
                                dupVehicle.forEach { dupVehicle ->
                                    updateVehicle(context, firestore, firebaseStorage, users, case, dupVehicle, rotatedFrontBitmap, rotatedBackBitmap,
                                        onComplete = {
                                            updateCaseStatus(firestore, idCase.caseId, decision, remark, "Driver Case",context)
                                            showDialog = false
                                            showEmailDialog = true
                                        }, onFailure = {
                                        showDialog = false
                                        }
                                    )
                                }
                            }
                        }else{
                            Toast.makeText(context, "Please enter a remark", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonColors(containerColor = Color(0xFF008000), contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
                ) {
                    Text("Approve")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Reject Button
                Button(
                    onClick = {
                        decision = "Rejected"
                        if(remark.isNotEmpty()){
                            updateCaseStatus(firestore, idCase.caseId, decision, remark, "Driver Case",context)
                            showEmailDialog = true
                        }else{
                            Toast.makeText(context, "Please enter a remark", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonColors(containerColor = Color(0xFFCC0000), contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
                ) {
                    Text("Reject")
                }
            }

            if(idCase.status != ""){
                OutlinedTextField(
                    value = idCase.remark,
                    onValueChange = { remark = it },
                    enabled = false,
                    label = { Text("* Remark") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Case Status: ${idCase.status}",color = if(idCase.status == "Approved") Color(0xFF008000) else Color(0xFFCC0000),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Button(
                        onClick = {
                            showEmailDialog = false
                            users?.let {
                                showEmailDialog = false
                                driverEmailContent(case!!.status, it.name, case!!.remark, onResult = {
                                    emailContent = it
                                })
                                sendEmail(
                                    context = context,
                                    recipient = it.email, // Replace with dynamic user email
                                    subject = "Driver Case Update Notification from SHARide Team",
                                    content = emailContent
                                )
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("Resend Email")
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))

            }

            if (showEmailDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showEmailDialog = false
                        navController.popBackStack()
                    },
                    title = { Text("Update Case Successfully !") },
                    text = { Text("Would you like to email the user with the case update?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEmailDialog = false
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                        ) {
                            Text("No Thanks")
                        }
                        Button(
                            onClick = {
                                users?.let {
                                    showEmailDialog = false
                                    driverEmailContent(decision, it.name, remark, onResult = {
                                        emailContent = it
                                    })
                                    sendEmail(
                                        context = context,
                                        recipient = it.email, // Replace with dynamic user email
                                        subject = "Driver Case Update Notification from SHARide Team",
                                        content = emailContent
                                    )
                                    navController.popBackStack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                        ) {
                            Text("Send Email")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        // Show Loading Dialog
        LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
    }
}


fun driverEmailContent(decision:String, userName:String, remark:String, onResult: (String) -> Unit){
    if (decision == "Approved") {
        onResult(
            """
            Dear ${userName},
            
            We are pleased to inform you that your driver case has been $decision.
       
            Thanks for joining SHARide Driver! Start Your Journey Today!
    
            If you have any questions, feel free to contact support.
    
            Best regards,
            Your SHARide Team
            """.trimIndent()
        )
    } else {
        onResult(
            """
            Dear ${userName},
            
            Unfortunately, your driver case has been $decision.
    
            Reason for Rejection:
            $remark
            
            If you believe this was an error, please contact our support team.
    
            Best regards,
            Your SHARide Team
        """.trimIndent()
        )

    }
}

@Composable
fun UserDetailScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var balance by remember { mutableStateOf(0.0) }
    var rating by remember { mutableStateOf(0.0) }
    var totalRating by remember { mutableStateOf(0) }

    var isDriver by remember { mutableStateOf<List<Driver>>(emptyList()) }
    var vehicle by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    // Fetch user data when screen is opened
    LaunchedEffect(userId) {
        showDialog = true
        loadUserDetails(firestore, userId) { user = it }
        loadWalletBalance(firestore, userId) { balance = it }
        loadRatingScore(firestore, userId) { Rating,TotalRating ->
            rating = Rating
            totalRating = TotalRating
        }
        showDialog = false
    }

    loadWalletBalance(firestore, userId) { balance = it }
    loadRatingScore(firestore, userId) { Rating,TotalRating ->
        rating = Rating
        totalRating = TotalRating
    }
    user?.let { userData ->

        searchDriver(firestore, userData.firebaseUserId, context, onResult = {
            isDriver = it
        })


        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "User Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ){
                Spacer(modifier = Modifier.width(15.dp))
                Image(
                    painter = rememberAsyncImagePainter(userData.profileImageUrl),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("${userData.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${userData.email}")
                    Spacer(modifier = Modifier.height(4.dp))

                    Text("ID: ${userData.studentId}")
                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Phone: ${userData.phoneNumber}")
                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Gender: ${if(userData.gender == "M")"Male" else "Female"}")
                    Spacer(modifier = Modifier.height(4.dp))

                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ){
                Column (
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Text("eWallet Balance", fontSize = 20.sp)
                    Text("RM $balance", fontSize = 23.sp,fontWeight = FontWeight.Bold)
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Rating", fontSize = 20.sp)
                    Text("$rating/5.0 ($totalRating)", fontSize = 23.sp,fontWeight = FontWeight.Bold)
                }

            }

            Spacer(modifier = Modifier.height(30.dp))
            if(isDriver.isNotEmpty()) {
                isDriver.forEach { driver ->
                    searchVehicle(firestore, driver.vehiclePlate, context, onResult = {
                        vehicle = it
                    })
                    Text("Driver Details :", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Name : ${driver.name}")
                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Driving ID : ${driver.drivingId}")
                        Spacer(modifier = Modifier.height(4.dp))

                        Text("ID Photo : ")
                        Image(
                            painter = rememberAsyncImagePainter(driver.lesen),
                            contentDescription = "Lesen Image",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Selfie Photo : ")
                        Image(
                            painter = rememberAsyncImagePainter(driver.selfie),
                            contentDescription = "Lesen Image",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                    }
                    if(vehicle.isNotEmpty()){
                        vehicle.forEach { vehicle ->
                            Text("Vehicle Details :", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                            ){
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Car Make : ${vehicle.CarMake}")
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Car Model : ${vehicle.CarModel}")
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Car Colour : ${vehicle.CarColour}")
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Car Type : ${vehicle.CarType}")
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Pet Preference : ${vehicle.PetPreference}")
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Car Front Photo :")
                                Image(
                                    painter = rememberAsyncImagePainter(vehicle.CarFrontPhoto),
                                    contentDescription = "Car Front Image",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Car Back Photo : ${driver.drivingId}")
                                Image(
                                    painter = rememberAsyncImagePainter(vehicle.CarBackPhoto),
                                    contentDescription = "Car Back Image",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }else{
                        Text("No Vehicle Found")
                    }
                }
            }else{
                Text("No Driver Found")
            }
        }
        // Show Loading Dialog
        LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
    }
}


@Composable
fun BanUserScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var remark by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var isBanned by remember { mutableStateOf(false) }

    // Fetch user data when screen is opened
    LaunchedEffect(userId) {
        showDialog = true
        loadUserDetails(firestore, userId) { user = it }
        showDialog = false
    }


    user?.let { userData ->
        LaunchedEffect(userId) {
            isBanned(firestore,userData.firebaseUserId, onResult = { remarks,banned ->
                isBanned = banned
                if (remarks != null) {
                    remark = remarks
                }
            })
        }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Ban User",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ){
                Spacer(modifier = Modifier.width(15.dp))
                Image(
                    painter = rememberAsyncImagePainter(userData.profileImageUrl),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("${userData.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${userData.email}")
                    Text("ID: ${userData.studentId}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Remark", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Enter Description") },
                    enabled = if(isBanned)false else true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))
                if(!isBanned){
                    Button(
                        onClick = {

                            if(remark.isEmpty()){
                                Toast.makeText(context, "Please enter a remark", Toast.LENGTH_SHORT).show()
                            }else {
                                showDialog = true
                                banUser(firestore,userData.firebaseUserId,remark, onComplete = { isComplete ->
                                    if(isComplete){
                                        showDialog = false
                                        Toast.makeText(context,"User Banned Successfully",Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }else{
                                        showDialog = false
                                        Toast.makeText(context,"Error Banned User",Toast.LENGTH_SHORT).show()
                                    }
                                })
                            }
                        },
                        colors = ButtonColors(containerColor = Color(0xFFCC0000), contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ban This User")
                    }
                }else{
                    Button(
                        onClick = {
                            showDialog = true
                            unbanUser(firestore,userData.firebaseUserId, onComplete = {
                                showDialog = false
                                Toast.makeText(context,"User Unbanned Successfully",Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            })

                        },
                        colors = ButtonColors(containerColor = Color(0xFF008000), contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unban This User")
                    }
                }


            }
        }
        // Show Loading Dialog
        LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
    }
}