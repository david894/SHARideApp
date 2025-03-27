package com.kxxr.logiclibrary.SignUp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaTypeOrNull

fun performSignUp(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    password: String,
    userid: String,
    userName: String,
    phoneNumber: String,
    gender: String,
    profilePicture: Bitmap,
    context: Context,
    navController: NavController,
    onFinish: () -> Unit
) {
    // Check if email is already registered
    firebaseAuth.fetchSignInMethodsForEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods
                if (signInMethods.isNullOrEmpty()) {
                    // Check for duplicate Student ID
                    checkDuplicateStudentId(firestore, userid,
                        onSuccess = {
                            registerUser(firebaseAuth, firestore, email, password,
                                userid, userName, phoneNumber, gender, profilePicture,
                                context, navController,"user", onFinish
                            )
                        },
                        onDuplicate = {
                            navController.navigate("duplicateID")
                            Toast.makeText(context, "Student ID already exists.", Toast.LENGTH_SHORT).show()
                            onFinish()
                        },
                        onError = { e ->
                            Toast.makeText(context, "Error checking student ID: ${e.message}", Toast.LENGTH_SHORT).show()
                            onFinish()
                        }
                    )
                } else {
                    Toast.makeText(context, "Email already registered.", Toast.LENGTH_SHORT).show()
                    onFinish()
                }
            } else {
                Toast.makeText(context, "Error checking email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                onFinish()
            }
        }
}
fun checkDuplicateStudentId(
    firestore: FirebaseFirestore,
    studentId: String,
    onSuccess: () -> Unit,
    onDuplicate: () -> Unit,
    onError: (Exception) -> Unit
) {
    firestore.collection("users")
        .whereEqualTo("studentId", studentId)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) onSuccess() else onDuplicate()
        }
        .addOnFailureListener { e ->
            onError(e)
        }
}

fun registerUser(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    password: String,
    studentId: String,
    name: String,
    phoneNumber: String,
    gender: String,
    profilePicture: Bitmap,
    context: Context,
    navController: NavController,
    type: String,
    onFinish: () -> Unit
) {
    firebaseAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUserId = task.result?.user?.uid

                // Send email verification
                task.result?.user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        Toast.makeText(context, "Verification email sent!", Toast.LENGTH_SHORT).show()
                    }

                // Upload Profile Picture
                uploadProfilePicture(firebaseUserId!!, profilePicture, { imageUrl ->
                    saveUserData(firestore, firebaseUserId, name,
                        studentId, email, phoneNumber, gender, imageUrl,
                        context, navController, type, onFinish
                    )
                }, { e ->
                    Toast.makeText(context, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    onFinish()
                })
            } else {
                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                onFinish()
            }
        }

}

fun uploadProfilePicture(
    userId: String,
    profilePicture: Bitmap,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val storageReference = FirebaseStorage.getInstance()
        .reference
        .child("ProfilePic/$userId.png")

    val byteArrayOutputStream = ByteArrayOutputStream()
    profilePicture.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    val profilePicData = byteArrayOutputStream.toByteArray()

    storageReference.putBytes(profilePicData)
        .addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }
        .addOnFailureListener { e ->
            onError(e)
        }
}

fun saveUserData(
    firestore: FirebaseFirestore,
    userId: String,
    name: String,
    studentId: String,
    email: String,
    phoneNumber: String,
    gender: String,
    profileImageUrl: String,
    context: Context,
    navController: NavController,
    type : String,
    onFinish: () -> Unit
) {
    val userData = hashMapOf(
        "firebaseUserId" to userId,
        "name" to name,
        "studentId" to studentId,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "gender" to gender,
        "profileImageUrl" to profileImageUrl
    )

    firestore.collection("users").add(userData)
        .addOnSuccessListener {
            Toast.makeText(context, "User Registered Successfully", Toast.LENGTH_SHORT).show()
            if(type == "admin"){
                onFinish()
            }else{
                FirebaseAuth.getInstance().signOut()
                navController.navigate("emailverify")
                onFinish()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
            onFinish()
        }
}

fun uploadImagesAndSaveData(
    context: Context,
    firestore: FirebaseFirestore,
    firebaseStorage: FirebaseStorage,
    caseId: String,
    name: String,
    phone: String,
    email: String,
    gender: String,
    studentId: String,
    studentIdUri: Uri,
    selfieUri: Uri,
    onUploadComplete: () -> Unit,
    onError: () -> Unit
) {
    val studentIdRef = firebaseStorage.reference.child("ID Case/$caseId/student_id.jpg")
    val selfieRef = firebaseStorage.reference.child("ID Case/$caseId/selfie.jpg")

    // Upload Student ID
    studentIdRef.putFile(studentIdUri).addOnSuccessListener {
        studentIdRef.downloadUrl.addOnSuccessListener { studentIdUrl ->
            // Upload Selfie
            selfieRef.putFile(selfieUri).addOnSuccessListener {
                selfieRef.downloadUrl.addOnSuccessListener { selfieUrl ->
                    // Save details to Firestore
                    val userData = hashMapOf(
                        "caseId" to caseId,
                        "name" to name,
                        "phone" to phone,
                        "email" to email,
                        "gender" to gender,
                        "studentId" to studentId,
                        "studentIdLink" to studentIdUrl.toString(),
                        "selfieLink" to selfieUrl.toString(),
                        "status" to "",
                        "remark" to ""
                    )
                    firestore.collection("ID Case").document(caseId)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Submitted Successfully", Toast.LENGTH_SHORT).show()
                            onUploadComplete()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            onError()
                        }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Error uploading selfie: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Error uploading ID: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Helper function to upload images to Firebase Storage
fun uploadToFirebaseStorage(
    userId: String,
    folderName: String,
    fileName: String,
    bitmap: Bitmap?,
    storage: FirebaseStorage,
    onComplete: (String) -> Unit
) {
    if (bitmap == null) {
        onComplete("")
        return
    }

    val storageRef = storage.reference.child("$folderName/$userId/$fileName")
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
    val data = baos.toByteArray()

    storageRef.putBytes(data)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                onComplete(uri.toString())
            }
        }
        .addOnFailureListener {
            onComplete("")
        }
}

// Helper function to save driver data to Firestore
fun saveDriverToFirestore(
    userId: String,
    name: String,
    drivingId: String,
    profileUrl: String,
    lesenUrl: String,
    selfieUrl: String,
    firestore: FirebaseFirestore,
    onComplete: () -> Unit
) {
    val driverData = hashMapOf(
        "userId" to userId,
        "name" to name,
        "drivingId" to drivingId,
        "vehicleId" to "",
        "vehiclePlate" to "",
        "profilePicture" to profileUrl, // Save profile picture URL
        "lesen" to lesenUrl,           // Save lesen URL
        "selfie" to selfieUrl,
        "status" to "Active"
    )

    firestore.collection("driver").document(userId)
        .set(driverData)
        .addOnSuccessListener { onComplete() }
        .addOnFailureListener { onComplete() }
}
fun loadVehicleData(userId: String, onResult: (VehicleData?) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("Vehicle")
        .whereEqualTo("UserID", userId)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                val vehicle = VehicleData(
                    carMake = document.getString("CarMake") ?: "",
                    carModel = document.getString("CarModel") ?: "",
                    carColor = document.getString("CarColour") ?: "",
                    registrationNum = document.getString("CarRegistrationNumber") ?: "",
                    carFrontPhoto = document.getString("CarFrontPhoto") ?: "",
                    carBackPhoto = document.getString("CarBackPhoto") ?: ""
                )
                onResult(vehicle)
            } else {
                onResult(null) // No vehicle found
            }
        }
        .addOnFailureListener {
            onResult(null) // Handle error
        }
}

fun checkDuplicatePlate(
    firestore: FirebaseFirestore,
    registrationNum: String,
    onResult: (Boolean) -> Unit
) {
    firestore.collection("Vehicle")
        .whereEqualTo("CarRegistrationNumber", registrationNum)
        .whereEqualTo("status", "Active")
        .get()
        .addOnSuccessListener { querySnapshot ->
            onResult(!querySnapshot.isEmpty) // Returns `true` if duplicate exists
        }
        .addOnFailureListener {
            onResult(false)
        }
}

fun uploadImageToFirebase(
    storage: FirebaseStorage,
    path: String,
    imageUri: Uri,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val storageRef = storage.reference.child(path)

    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString()) // Return the download URL
            }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

fun saveVehicleToFirestore(
    firestore: FirebaseFirestore,
    caseId: String,
    userId: String,
    carMake: String,
    carModel: String,
    carColor: String,
    registrationNum: String,
    frontPhotoUrl: String,
    backPhotoUrl: String,
    onComplete: (Boolean) -> Unit
) {
    val vehicleData = hashMapOf(
        "caseId" to caseId,
        "CarMake" to carMake,
        "CarModel" to carModel,
        "CarColour" to carColor,
        "CarRegistrationNumber" to registrationNum,
        "CarFrontPhoto" to frontPhotoUrl,
        "CarBackPhoto" to backPhotoUrl,
        "status" to "Active",
        "UserID" to userId
    )

    firestore.collection("Vehicle")
        .document(caseId)
        .set(vehicleData)
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
}

fun updateDriverDetails(
    firestore: FirebaseFirestore,
    userId: String,
    vehicleId: String,
    vehiclePlate: String,
    onComplete: (Boolean) -> Unit
) {
    firestore.collection("driver")
        .document(userId)
        .update(
            mapOf(
                "vehicleId" to vehicleId,
                "vehiclePlate" to vehiclePlate
            )
        )
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
}

fun handleVehicleSubmission(
    context: Context,
    navController: NavController,
    userId: String,
    caseId: String,
    carMake: String,
    carModel: String,
    carColor: String,
    registrationNum: String,
    carFrontUri: Uri?,
    carBackUri: Uri?,
    verificationStatus: String,
    backVerificationStatus: String,
    onComplete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val firebaseStorage = FirebaseStorage.getInstance()

    // Validate inputs
    if (
        carMake.isEmpty() || carModel.isEmpty() || carColor.isEmpty() || registrationNum.isEmpty()
        || carFrontUri == null || carBackUri == null
        || !verificationStatus.contains("Matched") || !backVerificationStatus.contains("Matched")
    ) {
        Toast.makeText(context, "Please fill up all the details", Toast.LENGTH_SHORT).show()
        onComplete()
        return
    }

    // Check for duplicate plate number
    checkDuplicatePlate(firestore, registrationNum) { isDuplicate ->
        if (isDuplicate) {
            Toast.makeText(context, "Duplicate Car Plate!", Toast.LENGTH_SHORT).show()
            navController.navigate("duplicatecar")
            onComplete()
            return@checkDuplicatePlate
        }

        // Proceed with uploading images
        uploadImageToFirebase(firebaseStorage, "Vehicle Photo/$caseId/car_front.jpg", carFrontUri!!,
            onSuccess = { frontUrl ->

                uploadImageToFirebase(firebaseStorage, "Vehicle Photo/$caseId/car_back.jpg", carBackUri!!,
                    onSuccess = { backUrl ->

                        // Save vehicle details to Firestore
                        saveVehicleToFirestore(firestore, caseId, userId, carMake, carModel, carColor, registrationNum, frontUrl, backUrl) { vehicleSaved ->
                            if (vehicleSaved) {
                                // Update driver details
                                updateDriverDetails(firestore, userId, caseId, registrationNum) { driverUpdated ->
                                    if (driverUpdated) {
                                        Toast.makeText(context, "Submitted Successfully", Toast.LENGTH_SHORT).show()
                                        navController.navigate("driversuccess")
                                    } else {
                                        Toast.makeText(context, "Error updating driver details", Toast.LENGTH_SHORT).show()
                                    }
                                    onComplete()
                                }
                            } else {
                                Toast.makeText(context, "Error saving vehicle details", Toast.LENGTH_SHORT).show()
                                onComplete()
                            }
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Error uploading back photo: ${e.message}", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                )
            },
            onFailure = { e ->
                Toast.makeText(context, "Error uploading front photo: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        )
    }
}

fun uploadDriverData(
    firebaseStorage: FirebaseStorage,
    firestore: FirebaseFirestore,
    caseId: String,
    name: String,
    driverid: String,
    IDUri: Uri,
    selfieUri: Uri,
    carmake: String,
    carmodel: String,
    carcolor: String,
    registrationnum: String,
    carfronturi: Uri,
    carbackuri: Uri,
    userId: String,
    context: Context,
    navController: NavController,
    onUploadFinished: () -> Unit
) {
    val storageRef = firebaseStorage.reference.child("Driver Case/$caseId")

    uploadFile(storageRef.child("driver_id.jpg"), IDUri) { idCardUrl ->
        uploadFile(storageRef.child("selfie.jpg"), selfieUri) { selfieUrl ->
            uploadFile(storageRef.child("car_front.jpg"), carfronturi) { carFrontUrl ->
                uploadFile(storageRef.child("car_back.jpg"), carbackuri) { carBackUrl ->
                    saveDriverData(firestore, caseId, name, driverid,
                        idCardUrl, selfieUrl, carmake, carmodel,
                        carcolor, registrationnum, carFrontUrl, carBackUrl,
                        userId, context, navController, onUploadFinished
                    )
                }
            }
        }
    }
}

fun uploadFile(
    storageReference: StorageReference,
    fileUri: Uri,
    onSuccess: (String) -> Unit,
) {
    storageReference.putFile(fileUri)
        .addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(storageReference.storage.app.applicationContext, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun saveDriverData(
    firestore: FirebaseFirestore,
    caseId: String,
    name: String,
    driverid: String,
    idCardUrl: String,
    selfieUrl: String,
    carmake: String,
    carmodel: String,
    carcolor: String,
    registrationnum: String,
    carFrontUrl: String,
    carBackUrl: String,
    userId: String,
    context: Context,
    navController: NavController,
    onUploadFinished: () -> Unit
) {
    val userData = hashMapOf(
        "caseId" to caseId,
        "driverName" to name,
        "driverId" to driverid,
        "IDPhoto" to idCardUrl,
        "driverSelfie" to selfieUrl,
        "CarMake" to carmake,
        "CarModel" to carmodel,
        "CarColour" to carcolor,
        "CarRegistrationNumber" to registrationnum,
        "CarFrontPhoto" to carFrontUrl,
        "CarBackPhoto" to carBackUrl,
        "status" to "",
        "remark" to "",
        "UserID" to userId
    )

    firestore.collection("Driver Case")
        .document(caseId)
        .set(userData)
        .addOnSuccessListener {
            onUploadFinished()
            Toast.makeText(context, "Submitted Successfully", Toast.LENGTH_SHORT).show()
            navController.navigate("ReportSubmitted/home")
        }
        .addOnFailureListener { e ->
            onUploadFinished()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun painterResourceToBitmap(context: Context, drawableResId: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, drawableResId)!!
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

