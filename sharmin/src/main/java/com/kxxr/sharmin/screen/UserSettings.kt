package com.kxxr.sharmin.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.kxxr.logiclibrary.AiDetector.detectFaceFromIdCard
import com.kxxr.logiclibrary.AiDetector.rotateBitmap
import com.kxxr.logiclibrary.AiDetector.saveBitmapToCache
import com.kxxr.logiclibrary.Login.resetPassword
import com.kxxr.logiclibrary.SignUp.painterResourceToBitmap
import com.kxxr.logiclibrary.SignUp.performSignUp
import com.kxxr.logiclibrary.SignUp.registerUser
import com.kxxr.logiclibrary.User.User
import com.kxxr.logiclibrary.User.searchUsers
import com.kxxr.sharmin.DataClass.UserCase
import com.kxxr.sharmin.R
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.common.internal.StringResourceValueReader
import com.kxxr.logiclibrary.SignUp.saveUserData
import com.kxxr.logiclibrary.SignUp.uploadProfilePicture
import com.kxxr.logiclibrary.eWallet.loadAvailablePins

fun sendEmail(context: Context, recipient: String, subject: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822" // Ensure only email apps respond
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient)) // Recipient(s)
        putExtra(Intent.EXTRA_SUBJECT, subject) // Email subject
        putExtra(Intent.EXTRA_TEXT, content) // Email content
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Choose Email Client"))
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}


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
                        if (isAvaliableOpen) SearchPendingCases(firestore,"",context) { cases ->
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
                        if (isCompleteOpen) CompleteIDCase(firestore,context) { cases ->
                            completeIDCases = cases
                        }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(if (isCompleteOpen) "Hide Completed Case ▲ " else "Show Completed Case ▼", modifier = Modifier.padding(start = 10.dp))
        }
        if(isCompleteOpen){
            if(idCases.isEmpty()){
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


// Load ID cases with empty status
fun SearchPendingCases(firestore: FirebaseFirestore, query: String, context: Context, onResult: (List<UserCase>) -> Unit) {
    val searchField = when {
        query.contains(" ") -> "name"         // Assume "name" if space is present
        query.endsWith("tarc.edu.my") -> "email" // Alphabetic values for "name"
        query.matches(Regex("[0-9]{2}[A-z]{1}")) -> "studentId" // Numeric values for "studentId"
        query.equals("") -> "status"
        else -> "name"        // Otherwise, search by "name"
    }

    firestore.collection("ID Case")
        .whereEqualTo("status", "")
        .whereEqualTo(searchField, if(searchField == "email")query else query.uppercase())
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Toast.makeText(context, "No case found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val cases = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserCase::class.java)
            }
            onResult(cases)
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(emptyList()) // Handle error case by returning null
        }
}

// Load ID cases with empty status
fun CompleteIDCase(firestore: FirebaseFirestore, context: Context, onResult: (List<UserCase>) -> Unit) {

    firestore.collection("ID Case")
        .whereIn("status", listOf("Approved", "Rejected")) // Fetch both approve and reject statuses
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Toast.makeText(context, "No case found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val cases = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserCase::class.java)
            }
            onResult(cases)
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(emptyList()) // Handle error case by returning null
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
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, context)
                                    showDialog = false
                                    showEmailDialog = true

                                }else if (overwrite == "Email"){
                                    showDialog = true
                                    updateDupUser(firestore, dupEmailUsers[userEmailIndex], idCase, context)
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, context)
                                    showDialog = false
                                    showEmailDialog = true

                                }
                            }else if(dupIDUsers.isEmpty() || dupEmailUsers.isEmpty()){
                                if(dupIDUsers.isNotEmpty() ){
                                    showDialog = true
                                    updateDupUser(firestore, dupIDUsers[userIDIndex], idCase, context)
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, context)
                                    showDialog = false
                                    showEmailDialog = true

                                }else if (dupEmailUsers.isNotEmpty()){
                                    showDialog = true
                                    updateDupUser(firestore, dupEmailUsers[userEmailIndex], idCase, context)
                                    updateCaseStatus(firestore, idCase.caseId, decision, remark, context)
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

                                                        updateCaseStatus(firestore, idCase.caseId, decision, remark, context)
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
                            updateCaseStatus(firestore, idCase.caseId, decision, remark, context)
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

// Load specific case by ID
fun loadCaseById(firestore: FirebaseFirestore, caseId: String, onResult: (UserCase) -> Unit) {
    firestore.collection("ID Case")
        .document(caseId)
        .get()
        .addOnSuccessListener { doc ->
            doc.toObject(UserCase::class.java)?.let { onResult(it) }
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
// Update case status and remark
fun updateCaseStatus(
    firestore: FirebaseFirestore,
    caseId: String,
    status: String,
    remark: String,
    context: Context
) {
    firestore.collection("ID Case")
        .document(caseId)
        .update("status", status, "remark", remark)
        .addOnSuccessListener {
            Toast.makeText(context, "Case $status successfully", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to update case", Toast.LENGTH_SHORT).show()
        }
}

fun updateDupUser(
    firestore: FirebaseFirestore,
    user: User,
    case:UserCase,
    context: Context
){
    firestore.collection("users")
        .whereEqualTo("firebaseUserId", user.firebaseUserId)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val document = querySnapshot.documents.firstOrNull()
            document?.reference?.update("email", case.email)
            document?.reference?.update("gender", case.gender)
            document?.reference?.update("name", case.name)
            document?.reference?.update("phoneNumber", case.phone)
            document?.reference?.update("studentId", case.studentId)
            resetPassword(case.email,context, onSuccess = {
                Toast.makeText(context, "Password reset email sent to user successfully!", Toast.LENGTH_LONG).show()
            }, onFailure = {
                Toast.makeText(context, "Error! Unable to sent password reset email", Toast.LENGTH_LONG).show()
            })
        }
        .addOnFailureListener{
            Toast.makeText(context, "Failed to update user", Toast.LENGTH_SHORT).show()
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

