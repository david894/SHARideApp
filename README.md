# SHARide: Enhanced Carpooling Mobile Application for TAR UMT

**SHARide** is a carpooling mobile application developed as part of a Final Year Project at Tunku Abdul Rahman University of Management and Technology (TAR UMT). The app is designed to address pressing transportation challenges faced by students and staff on campus, such as long queues for public transport, safety concerns, and inefficient vehicle utilization. This enhanced version of SHARide introduces advanced features and secure modules to improve usability, safety, and operational efficiency.

## 📱 Technologies Used

* **Android Studio Koala 2024.1.1**: Core IDE for Android development.
* **Kotlin (Jetpack Compose)**: For building modern, declarative UIs.
* **Firebase Services**:

  * **Firebase Authentication** – Secure user sign-in and admin access.
  * **Firebase Firestore** – Real-time NoSQL database for structured user and transaction data.
  * **Firebase Storage** – For storing uploaded ID and vehicle images.
  * **Firebase Functions** – For server-side business logic, such as transaction validation.
  * **Firebase Cloud Messaging (FCM)** – For sending real-time notifications.
* **ML Kit (OCR & Face Detection)**: For AI-based TAR UMT ID verification and vehicle plate recognition.
* **NFC API**: For secure, contactless TAR UMT ID verification.

## 🔐 Key Features

### 1. **User Verification Module**

* Supports AI-based Optical Character Recognition (OCR) to extract text from TAR UMT student/staff ID cards.
* Integrates face detection to validate identity using the uploaded ID photo.
* Implements NFC scanning for double-layer verification.
* Provides manual verification flow for cases where AI fails.

### 2. **E-Wallet Payment System**

* Users can set a 6-digit secure PIN (SHA-256 hashed) for wallet access.
* Features balance top-up via generated PIN codes, instant balance updates, and secure ride payments.
* Records complete transaction history for each user.
* Includes security question and lockout mechanisms after multiple incorrect attempts.

### 3. **Rating and Classification Module**

* Enables post-ride feedback with a 1–5 star system.
* Automatically issues warnings for users receiving 1-star reviews.
* Auto-ban mechanism triggers when a user receives 3 warnings within 30 days.
* Ratings and comments are sanitized to prevent inappropriate input.

### 4. **Admin Backend Module**

* Provides role-based access control (Admin Group Permissions).
* Admins can review manual verification submissions, manage e-wallet balances, and enforce user bans.
* Admin dashboard is protected with 2FA for added security.

## 🌐 Purpose

SHARide is intended to improve the transportation experience within the TAR UMT campus by offering a safer, more reliable, and eco-friendly alternative to private vehicle use and public transport. By promoting shared rides and providing secure digital infrastructure, SHARide supports the vision of a smarter and more sustainable campus.

## 📥 How to Run

1. Clone the repository:
   `git clone https://github.com/david894/SHARideApp.git`
2. Open the project in **Android Studio Koala 2024.1.1**
3. Sync Gradle and ensure Firebase JSON configuration is added
4. Build and run on an Android device (with NFC enabled for verification module)
