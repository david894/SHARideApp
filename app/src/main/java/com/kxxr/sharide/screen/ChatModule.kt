@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.kxxr.sharide.screen

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.kxxr.sharide.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID


@Composable
fun ChatListScreen(userId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val chatList = remember { mutableStateListOf<ChatPreview>() }
    val context = LocalContext.current
    val isDriver by remember { mutableStateOf(getDriverPreference(context)) }

    LaunchedEffect(userId, isDriver) {
        val fieldToFilter = if (isDriver) "driverId" else "passengerId"

        db.collection("chats")
            .whereEqualTo(fieldToFilter, userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                chatList.clear()

                for (doc in snapshots) {
                    val otherUserId =
                        if (isDriver) doc.getString("passengerId") else doc.getString("driverId")
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getTimestamp("lastMessageTimestamp")

                    if (otherUserId != null) {
                        db.collection("users")
                            .whereEqualTo("firebaseUserId", otherUserId)
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                val userDoc = userSnapshot.documents.firstOrNull()
                                val name = userDoc?.getString("name") ?: "Unknown"
                                val image = userDoc?.getString("profileImageUrl") ?: ""
                                chatList.add(
                                    ChatPreview(
                                        chatId = doc.id,
                                        passengerName = name,
                                        passengerImage = image,
                                        lastMessage = lastMessage,
                                        lastMessageTimestamp = timestamp
                                    )
                                )
                            }
                    }
                }
            }
    }

    Scaffold(
        topBar = { ChatTopBar(navController) },
        containerColor = Color.White,
        bottomBar = { BottomNavBar("chat_list", navController) },

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(chatList) { chat ->
                    ChatPreviewCard(chat = chat) {
                        navController.navigate("chat_screen/${chat.chatId}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 🔵 Floating Chatbot Button
            IconButton(
                onClick = { navController.navigate("chatbot") },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1976D2))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gemeni_logo),
                    contentDescription = "Chatbot",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}





    @Composable
fun ChatTopBar(navController: NavController) {
    TopAppBar(
        title = {
            Text(
                text = "SHARide Chat",
                color = Color.White // Text color for white background
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White // Icon color for white background
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0075FD)
        )
    )
}



@Composable
fun ChatPreviewCard(chat: ChatPreview, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FD))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = chat.passengerImage.ifEmpty { R.drawable.profile_ico },
                contentDescription = "Passenger Image",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(chat.passengerName, fontWeight = FontWeight.Bold, fontSize = 16.sp,color = Color.White)
                Text(chat.lastMessage, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,color = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = chat.lastMessageTimestamp?.toDate()?.let {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
                } ?: "",
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun ChatScreen(
    chatId: String,
    currentUserId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val db = FirebaseFirestore.getInstance()
    val messages = remember { mutableStateListOf<Message>() }
    var newMessage by remember { mutableStateOf("") }
    val messageRef = db.collection("chats")
        .document(chatId)
        .collection("messages")
        .document() // Generate a new document with a unique ID
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    var receiverName by remember { mutableStateOf("Chat") }
    var receiverImageUrl by remember { mutableStateOf("") }
    var currentUserImageUrl by remember { mutableStateOf("") }
    // For capturing camera image
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri.value != null) {
            // Upload to Firebase or send as chat message
            uploadImageToFirebase(imageUri.value!!, chatId, currentUserId,messageRef.id)
        }
    }
    // Step 1: Get the chat document and determine receiver ID
    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { chatDoc ->
                val driverId = chatDoc.getString("driverId")
                val passengerId = chatDoc.getString("passengerId")

                val receiverId = if (driverId == currentUserId) passengerId else driverId

                // Step 2: Fetch receiver's user profile
                db.collection("users")
                    .whereEqualTo("firebaseUserId", receiverId)
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        val userDoc = userSnapshot.documents.firstOrNull()
                        if (userDoc != null) {
                            receiverName = userDoc.getString("name") ?: "User"
                            receiverImageUrl = userDoc.getString("profileImageUrl") ?: ""
                        }
                    }
            }
    }
    // Fetch current user image
    LaunchedEffect(currentUserId) {
        db.collection("users")
            .whereEqualTo("firebaseUserId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc != null) {
                    currentUserImageUrl = doc.getString("profileImageUrl") ?: ""
                }
            }
    }
    // Step 3: Listen for chat messages
    LaunchedEffect(chatId) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                messages.clear()
                for (doc in snapshot.documents) {
                    val message = doc.toObject(Message::class.java)
                    if (message != null) messages.add(message)
                }
            }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Set screen background to blue
    ) {
        TopAppBar(
            title = { Text(receiverName, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0075FD), // Blue background
                titleContentColor = Color.White      // Title text color
            )
        )


        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            items(messages) { msg ->
                val isCurrentUser = msg.senderId == currentUserId

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (isCurrentUser) {
                                    messageToDelete = msg
                                    showDeleteDialog = true
                                }
                            }
                        )
                ) {
                    ChatBubble(
                        msg = msg,
                        isCurrentUser = isCurrentUser,
                        senderImageUrl = currentUserImageUrl,
                        receiverImageUrl = receiverImageUrl,
                        onDeleteMessage = { selectedMsg ->
                            if (isCurrentUser) {
                                messageToDelete = selectedMsg
                                showDeleteDialog = true
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        if (showDeleteDialog && messageToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    messageToDelete = null
                },
                title = { Text("Delete Message") },
                text = { Text("Are you sure you want to delete this message?") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteMessage(chatId, messageToDelete!!)
                        showDeleteDialog = false
                        messageToDelete = null
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        messageToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
        var requestCameraPermission by remember { mutableStateOf(false) }
        var showPermissionDenied by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = {
                requestCameraPermission = true
            }) {
                Icon(Icons.Default.Photo, contentDescription = "Send Photo")
            }



            val context = LocalContext.current

            IconButton(onClick = {
                fetchCurrentLocation(context) { latLng ->
                    if (latLng != null) {
                        val message = hashMapOf(
                            "messageId" to messageRef.id,
                            "senderId" to currentUserId,
                            "messageText" to "Shared a location",
                            "messageType" to "location",
                            "location" to GeoPoint(latLng.latitude, latLng.longitude),
                            "timestamp" to FieldValue.serverTimestamp(),
                            "isRead" to false
                        )

                        db.collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .add(message)

                        db.collection("chats").document(chatId).update(
                            mapOf(
                                "lastMessage" to "Shared a location",
                                "lastMessageTimestamp" to FieldValue.serverTimestamp()
                            )
                        )
                    } else {

                    }
                }
            }) {
                Icon(Icons.Default.LocationOn, contentDescription = "Send Location")
            }

            TextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )
            IconButton(onClick = {
                if (newMessage.isNotBlank()) {
                    val message = hashMapOf(
                        "messageId" to messageRef.id,
                        "senderId" to currentUserId,
                        "messageText" to newMessage.trim(),
                        "messageType" to "text",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "isRead" to false
                    )
                    db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .add(message)

                    db.collection("chats").document(chatId).update(
                        mapOf(
                            "lastMessage" to newMessage.trim(),
                            "lastMessageTimestamp" to FieldValue.serverTimestamp()
                        )
                    )
                    newMessage = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
            if (requestCameraPermission) {
                CameraPermissionHandler(
                    onPermissionGranted = {
                        requestCameraPermission = false
                        val photoUri = createImageUri(context)
                        imageUri.value = photoUri
                        launcher.launch(photoUri)
                    },
                    onPermissionDenied = {
                        requestCameraPermission = false
                        showPermissionDenied = true
                    }
                )
            }
            if (showPermissionDenied) {
                ShowPermissionDeniedDialog(onDismiss = {
                    showPermissionDenied = false
                })
            }

        }
    }
}

fun createImageUri(context: Context): Uri {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "chat_image_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
}

fun uploadImageToFirebase(uri: Uri, chatId: String, currentUserId: String, messageRef:String) {
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}.jpg")

    imageRef.putFile(uri)
        .continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }
        .addOnSuccessListener { downloadUrl ->
            val db = FirebaseFirestore.getInstance()
            val message = hashMapOf(
                "messageId" to messageRef,
                "senderId" to currentUserId,
                "messageText" to "",
                "messageType" to "image",
                "imageUrl" to downloadUrl.toString(),
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to false
            )
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
        }
}


fun fetchCurrentLocation(context: Context, onLocationResult: (LatLng?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    onLocationResult(latLng)
                } else {
                    onLocationResult(null)
                }
            }
    } else {
        onLocationResult(null)
    }
}

@Composable
fun ChatBubble(
    msg: Message,
    isCurrentUser: Boolean,
    senderImageUrl: String,
    receiverImageUrl: String,
    onDeleteMessage: (Message) -> Unit
) {
    val bgColor = if (isCurrentUser) Color(0xFF0075FD) else Color.DarkGray
    val horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    val imageUrl = if (isCurrentUser) senderImageUrl else receiverImageUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        if (!isCurrentUser) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Receiver Image",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        onDeleteMessage(msg)
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .widthIn(max = 250.dp)
            ) {
                Column {
                    when (msg.messageType) {
                        "text" -> {
                            Text(text = msg.messageText, color = Color.White)
                        }
                        "image" -> {
                            msg.imageUrl?.let { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Shared Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 150.dp, max = 250.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }                        "location" -> {
                            msg.location?.let { geo ->
                                val location = LatLng(geo.latitude, geo.longitude)
                                val cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(location, 15f)
                                }
                                GoogleMap(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    cameraPositionState = cameraPositionState
                                ) {
                                    Marker(
                                        state = MarkerState(position = location),
                                        title = "Shared location"
                                    )
                                }
                            } ?: Text("Location unavailable", color = Color.Red)
                        }
                    }

                    msg.timestamp?.let {
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()),
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(6.dp))
            AsyncImage(
                model = imageUrl,
                contentDescription = "Sender Image",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }
    }
}


@Composable
fun CameraPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    )

    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        )

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun ShowPermissionDeniedDialog(onDismiss: () -> Unit) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Permission Required") },
        text = { Text("We need camera access to let you take photos.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

fun deleteMessage(chatId: String, message: Message) {
    val db = FirebaseFirestore.getInstance()
    val messagesRef = db.collection("chats")
        .document(chatId)
        .collection("messages")

    if (!message.messageId.isNullOrEmpty()) {
        messagesRef.document(message.messageId!!).delete()
    } else {
        // Fallback: try to find it based on timestamp (less reliable)
        messagesRef
            .whereEqualTo("timestamp", message.timestamp)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.reference?.delete()
            }
    }
}


data class ChatPreview(
    val chatId: String,
    val passengerName: String,
    val passengerImage: String,
    val lastMessage: String,
    val lastMessageTimestamp: Timestamp?
)

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val messageType: String = "text",
    val timestamp: Timestamp? = null,
    val location: GeoPoint? = null,
    val isRead: Boolean = false,
    val imageUrl: String? = null,
)
