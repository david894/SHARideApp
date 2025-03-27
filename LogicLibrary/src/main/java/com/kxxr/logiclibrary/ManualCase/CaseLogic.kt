package com.kxxr.logiclibrary.ManualCase

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.kxxr.logiclibrary.Driver.Vehicle
import com.kxxr.logiclibrary.Login.resetPassword
import com.kxxr.logiclibrary.SignUp.saveVehicleToFirestore
import com.kxxr.logiclibrary.SignUp.updateDriverDetails
import com.kxxr.logiclibrary.SignUp.uploadToFirebaseStorage
import com.kxxr.logiclibrary.User.User
import java.io.ByteArrayOutputStream

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

// Load specific case by ID
fun loadCaseById(firestore: FirebaseFirestore, caseId: String, onResult: (UserCase) -> Unit) {
    firestore.collection("ID Case")
        .document(caseId)
        .get()
        .addOnSuccessListener { doc ->
            doc.toObject(UserCase::class.java)?.let { onResult(it) }
        }
}
// Update case status and remark
fun updateCaseStatus(
    firestore: FirebaseFirestore,
    caseId: String,
    status: String,
    remark: String,
    type : String,
    context: Context
) {
    val collection = if(type == "ID") "ID Case" else "Driver Case"

    firestore.collection(collection)
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


fun loadCaseByDriver(firestore: FirebaseFirestore, caseId: String, onResult: (DriverCase) -> Unit) {
    firestore.collection("Driver Case")
        .document(caseId)
        .get()
        .addOnSuccessListener { doc ->
            doc.toObject(DriverCase::class.java)?.let { onResult(it) }
        }
}

fun CompleteDriverCase(firestore: FirebaseFirestore, context: Context, onResult: (List<DriverCase>) -> Unit) {
    firestore.collection("Driver Case")
        .whereIn("status", listOf("Approved", "Rejected")) // Fetch both approve and reject statuses
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Toast.makeText(context, "No completed driver case found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val cases = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DriverCase::class.java)
            }
            onResult(cases)
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(emptyList()) // Handle error case by returning null
        }
}

fun uploadLicenseAndProfile(
    userId: String,
    rotatedBitmap: Bitmap?,
    userProfile: Bitmap?,
    selfie: Bitmap?,
    firebaseStorage: FirebaseStorage,
    onComplete: (String?, String?, String?) -> Unit
) {
    uploadToFirebaseStorage(userId, "Driving Lesen", "lesen.png", rotatedBitmap, firebaseStorage) { lesenUrl ->
        val profilePicture = userProfile ?: rotatedBitmap

        uploadToFirebaseStorage(userId, "Driving Lesen", "profile_picture.png", profilePicture, firebaseStorage) { profileUrl ->
            uploadToFirebaseStorage(userId, "Driving Lesen", "selfie.png", selfie, firebaseStorage) { selfieUrl ->
                onComplete(lesenUrl, profileUrl, selfieUrl)
            }
        }
    }
}


fun uploadVehicleImagesAndSave(
    context: Context,
    caseId: String,
    case: DriverCase?,
    userId: String,
    rotatedFrontBitmap: Bitmap?,
    rotatedBackBitmap: Bitmap?,
    firestore: FirebaseFirestore,
    firebaseStorage: FirebaseStorage,
    onComplete: () -> Unit,
    onFailure: () -> Unit
) {
    val byteArrayOutputStream = ByteArrayOutputStream()
    rotatedFrontBitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    val carFront = byteArrayOutputStream.toByteArray()

    val byteArrayOutputStreamBack = ByteArrayOutputStream()
    rotatedBackBitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStreamBack)
    val carBack = byteArrayOutputStreamBack.toByteArray()

    val carFrontRef = firebaseStorage.reference.child("Vehicle Photo/$caseId/car_front.jpg")
    val carBackRef = firebaseStorage.reference.child("Vehicle Photo/$caseId/car_back.jpg")

    carFrontRef.putBytes(carFront).addOnSuccessListener {
        carFrontRef.downloadUrl.addOnSuccessListener { frontUrl ->
            carBackRef.putBytes(carBack).addOnSuccessListener {
                carBackRef.downloadUrl.addOnSuccessListener { backUrl ->

                    // Save vehicle details to Firestore
                    saveVehicleToFirestore(
                        firestore, caseId, userId,
                        case!!.CarMake, case.CarModel, case.CarColour,
                        case.CarRegistrationNumber, frontUrl.toString(), backUrl.toString()
                    ) { vehicleSaved ->
                        if (vehicleSaved) {
                            // Update driver details
                            updateDriverDetails(firestore, userId, caseId, case.CarRegistrationNumber) { driverUpdated ->
                                if (driverUpdated) {
                                    Toast.makeText(context, "Submitted Successfully", Toast.LENGTH_SHORT).show()
                                    onComplete()
                                } else {
                                    Toast.makeText(context, "Error updating driver details", Toast.LENGTH_SHORT).show()
                                    onFailure()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Error saving vehicle details", Toast.LENGTH_SHORT).show()
                            onFailure()
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Error uploading back photo: ${e.message}", Toast.LENGTH_SHORT).show()
                onFailure()
            }
        }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Error uploading front photo: ${e.message}", Toast.LENGTH_SHORT).show()
        onFailure()
    }
}

fun updateVehicle(
    context: Context,
    firestore: FirebaseFirestore,
    firebaseStorage: FirebaseStorage,
    users: User?,
    case: DriverCase?,
    dupVehicle: Vehicle,
    rotatedFrontBitmap: Bitmap?,
    rotatedBackBitmap: Bitmap?,
    onComplete: () -> Unit,
    onFailure: () -> Unit
) {
    // Check for null case before proceeding
    if (case == null || users == null) {
        Toast.makeText(context, "Invalid vehicle data", Toast.LENGTH_SHORT).show()
        onFailure()
        return
    }

    // Ensure bitmaps are not null
    if (rotatedFrontBitmap == null || rotatedBackBitmap == null) {
        Toast.makeText(context, "Missing vehicle images", Toast.LENGTH_SHORT).show()
        onFailure()
        return
    }

    // Convert Bitmaps to ByteArray
    val byteArrayOutputStream = ByteArrayOutputStream()
    rotatedFrontBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    val carFront = byteArrayOutputStream.toByteArray()

    val byteArrayOutputStreamBack = ByteArrayOutputStream()
    rotatedBackBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStreamBack)
    val carBack = byteArrayOutputStreamBack.toByteArray()

    // Firebase Storage References
    val carFrontRef = firebaseStorage.reference.child("Vehicle Photo/${dupVehicle.caseId}/car_front.jpg")
    val carBackRef = firebaseStorage.reference.child("Vehicle Photo/${dupVehicle.caseId}/car_back.jpg")

    // Upload front image
    carFrontRef.putBytes(carFront).addOnSuccessListener {
        carFrontRef.downloadUrl.addOnSuccessListener { frontUrl ->

            // Upload back image
            carBackRef.putBytes(carBack).addOnSuccessListener {
                carBackRef.downloadUrl.addOnSuccessListener { backUrl ->

                    // Update Firestore vehicle details
                    firestore.collection("Vehicle")
                        .document(dupVehicle.caseId)
                        .update(
                            mapOf(
                                "caseId" to dupVehicle.caseId,
                                "CarMake" to case.CarMake,
                                "CarModel" to case.CarModel,
                                "CarColour" to case.CarColour,
                                "CarRegistrationNumber" to case.CarRegistrationNumber,
                                "CarFrontPhoto" to frontUrl.toString(),
                                "CarBackPhoto" to backUrl.toString(),
                                "status" to "Active",
                                "UserID" to users.firebaseUserId
                            )
                        )
                        .addOnSuccessListener {
                            // Find previous drivers and reset vehicle info
                            firestore.collection("driver")
                                .whereEqualTo("vehicleId", dupVehicle.caseId)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    if (snapshot.isEmpty) {
                                        // No previous driver found, just update the new driver directly
                                        updateDriverDetails(
                                            firestore, users.firebaseUserId, dupVehicle.caseId, case.CarRegistrationNumber
                                        ) { driverUpdated ->
                                            if (driverUpdated) {
                                                Toast.makeText(context, "Submitted Successfully", Toast.LENGTH_SHORT).show()
                                                onComplete()
                                            } else {
                                                Toast.makeText(context, "Error updating driver details", Toast.LENGTH_SHORT).show()
                                                onFailure()
                                            }
                                        }
                                    } else {
                                        // Update all previous drivers
                                        snapshot.documents.forEach { document ->
                                            document.reference.update(
                                                mapOf(
                                                    "vehicleId" to "",
                                                    "vehiclePlate" to ""
                                                )
                                            )
                                        }

                                        // Then update the new driver
                                        updateDriverDetails(
                                            firestore, users.firebaseUserId, dupVehicle.caseId, case.CarRegistrationNumber
                                        ) { driverUpdated ->
                                            if (driverUpdated) {
                                                Toast.makeText(context, "Submitted Successfully", Toast.LENGTH_SHORT).show()
                                                onComplete()
                                            } else {
                                                Toast.makeText(context, "Error updating driver details", Toast.LENGTH_SHORT).show()
                                                onFailure()
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error finding driver", Toast.LENGTH_SHORT).show()
                                    onFailure()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error updating vehicle data", Toast.LENGTH_SHORT).show()
                            onFailure()
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Error uploading car back image", Toast.LENGTH_SHORT).show()
                onFailure()
            }
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Error uploading car front image", Toast.LENGTH_SHORT).show()
        onFailure()
    }
}
