package com.kxxr.sharmin.screen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.Admin.AdminGroup
import com.kxxr.logiclibrary.Admin.checkIfUserIsAdmin
import com.kxxr.logiclibrary.Admin.enrollAdminUser
import com.kxxr.logiclibrary.Admin.loadAdminGroupDetails
import com.kxxr.logiclibrary.Admin.loadAllAdminGroup
import com.kxxr.logiclibrary.Admin.saveAdminGroupToFirestore
import com.kxxr.logiclibrary.Admin.searchAdminGroup
import com.kxxr.logiclibrary.Admin.updateAdminGroup
import com.kxxr.logiclibrary.User.User
import com.kxxr.logiclibrary.User.loadUserDetails
import com.kxxr.logiclibrary.User.loadWalletBalance
import com.kxxr.logiclibrary.eWallet.Transaction
import com.kxxr.logiclibrary.eWallet.loadTransactionHistory
import com.kxxr.logiclibrary.eWallet.recordTransaction
import com.kxxr.logiclibrary.eWallet.updateUserBalance

@Composable
fun LoadAdminGroupScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var adminGroup by remember { mutableStateOf<List<AdminGroup>>(emptyList()) }
    var isBanOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showDialog = true
        loadAllAdminGroup(firestore, onResult = {
            adminGroup = it
            showDialog = false
        })
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ){
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
                text = "Admin Group",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Group Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                showDialog = true
                if(searchQuery.isEmpty()){
                    loadAllAdminGroup(firestore, onResult = {
                        adminGroup = it
                        showDialog = false
                    })
                }else{
                    searchAdminGroup(firestore, searchQuery, context) {
                        adminGroup = it
                        if(adminGroup.isEmpty()){
                            Toast.makeText(context, "No Group found!", Toast.LENGTH_SHORT).show()
                        }
                        showDialog = false
                    }
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
        ) {
            Text("Search")
        }

        TextButton(
            onClick = { navController.navigate("createAdminGroup") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Create new admin group", color = Color.Blue, fontSize = 16.sp, textDecoration = TextDecoration.Underline)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(modifier = Modifier.height(24.dp))

        if(adminGroup.isNotEmpty()){
            Text("Search Results:")
        }

        Column {
            adminGroup.forEach { group ->
                AdminGroupCard(group) {
                    navController.navigate("modify_group/${group.groupId}")
                }
            }
        }

    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

// User Card Component
@Composable
fun AdminGroupCard(group: AdminGroup, onSelect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardColors(containerColor = Color.White, contentColor = Color.Black, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
        //elevation = 4,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                Text(
                    text = group.groupName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.groupName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "ID : "+group.groupId, fontSize = 12.sp)
                Button(
                    onClick = onSelect,
                    colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
                ) {
                    Text("Select")
                }
            }
        }
    }
}

@Composable
fun CreateAdminGroupScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var groupName by remember { mutableStateOf("") }
    var isEWalletChecked by remember { mutableStateOf(false) }
    var isUserSettingsChecked by remember { mutableStateOf(false) }
    var isAdminSettingsChecked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                text = "Create Admin Group",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(50.dp))
        Text("Create New Admin Group", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Group Name Input
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it.uppercase() },
            label = { Text("Group Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(30.dp))
        Text("Function Access :", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Checkboxes
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isEWalletChecked, onCheckedChange = { isEWalletChecked = it })
            Text("eWallet Settings", fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isUserSettingsChecked, onCheckedChange = { isUserSettingsChecked = it })
            Text("User Settings", fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isAdminSettingsChecked, onCheckedChange = { isAdminSettingsChecked = it })
            Text("Admin Settings", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                if (groupName.isBlank()) {
                    Toast.makeText(context, "Group name cannot be empty!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                saveAdminGroupToFirestore(
                    firestore,
                    groupName.uppercase(),
                    isEWalletChecked,
                    isUserSettingsChecked,
                    isAdminSettingsChecked,
                    onSuccess = {
                        isLoading = false
                        Toast.makeText(context, "Admin Group Created!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() // Navigate back after success
                    },
                    onFailure = { errorMessage ->
                        isLoading = false
                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(if (isLoading) "Saving..." else "Create Group")
        }
    }
}

@Composable
fun ModifyAdminGroupScreen(navController: NavController, groupId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var groupName by remember { mutableStateOf("") }
    var isEWalletChecked by remember { mutableStateOf(false) }
    var isUserSettingsChecked by remember { mutableStateOf(false) }
    var isAdminSettingsChecked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch Group Details from Firestore
    LaunchedEffect(groupId) {
        isLoading = true
        loadAdminGroupDetails(firestore, groupId, onResult = { group ->
            groupName = group.groupName
            isEWalletChecked = group.isEWalletSettings
            isUserSettingsChecked = group.isUserSettings
            isAdminSettingsChecked = group.isAdminSettings
            isLoading = false
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                text = "Modify Admin Group",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(50.dp))
        Text("Modify Admin Group", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Group Name Input
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it.uppercase() },
            label = { Text("Group Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(30.dp))
        Text("Function Access :", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Checkboxes for permissions
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isEWalletChecked, onCheckedChange = { isEWalletChecked = it })
            Text("eWallet Settings", fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isUserSettingsChecked, onCheckedChange = { isUserSettingsChecked = it })
            Text("User Settings", fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isAdminSettingsChecked, onCheckedChange = { isAdminSettingsChecked = it })
            Text("Admin Settings", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                if (groupName.isBlank()) {
                    Toast.makeText(context, "Group name cannot be empty!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                updateAdminGroup(
                    firestore,
                    groupId,
                    groupName.uppercase(),
                    isEWalletChecked,
                    isUserSettingsChecked,
                    isAdminSettingsChecked,
                    onSuccess = {
                        isLoading = false
                        Toast.makeText(context, "Admin Group Updated!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    onFailure = { errorMessage ->
                        isLoading = false
                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(if (isLoading) "Saving..." else "Update Group")
        }
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = isLoading, onDismiss = { isLoading = false })
}

@Composable
fun AddAdminScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var adminGroups by remember { mutableStateOf<List<AdminGroup>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var existingAdminGroupId by remember { mutableStateOf<String?>(null) }
    var isExistingAdmin by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    // Fetch user, admin groups, and check if the user is already an admin
    LaunchedEffect(userId) {
        showDialog = true
        loadUserDetails(firestore, userId) { user = it }
        loadAllAdminGroup(firestore) { groups ->
            adminGroups = groups
        }
        checkIfUserIsAdmin(firestore, userId) { adminGroupId ->
            existingAdminGroupId = adminGroupId
            selectedGroupId = adminGroupId
            isExistingAdmin = adminGroupId != null
            showDialog = false
        }
    }

    user?.let { userData ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // Back Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExistingAdmin) "Update Admin Group" else "Add Admin User",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(15.dp))
                Image(
                    painter = rememberAsyncImagePainter(userData.profileImageUrl),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text("${userData.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${userData.email}")
                    Text("ID: ${userData.studentId}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Group Selection
            Text("Select Admin Group:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            adminGroups.forEach { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedGroupId = group.groupId }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGroupId == group.groupId,
                        onClick = { selectedGroupId = group.groupId },
                        colors = RadioButtonDefaults.colors(selectedColor = Color.Blue)
                    )
                    Text(text = group.groupName, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enroll or Update Button
            Button(
                onClick = {
                    if (selectedGroupId == null) {
                        Toast.makeText(context, "Please select a group", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    showDialog = true
                    enrollAdminUser(firestore, userId, selectedGroupId!!) {
                        showDialog = false
                        Toast.makeText(context, if (isExistingAdmin) "Admin group updated!" else "Admin user added successfully!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text(if (isExistingAdmin) "Update Admin Group" else "Enroll User")
            }
        }
    }

    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

