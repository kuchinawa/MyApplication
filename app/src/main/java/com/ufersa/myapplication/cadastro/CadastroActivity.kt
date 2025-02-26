package com.ufersa.myapplication.cadastro

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.ufersa.myapplication.databinding.ActivityCadastroBinding
import com.ufersa.myapplication.utils.ImagemUtils

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storageRef: StorageReference
    private lateinit var firestore: FirebaseFirestore
    private var originalImageUri: Uri? = null
    private var processedImageUri: Uri? = null

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                originalImageUri = it
                processImage()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        storageRef = Firebase.storage.reference
        firestore = Firebase.firestore

        // Set click listener for image selection button
        binding.buttonAddPhoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.buttonCadastrar.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextSenha.text.toString()
            val name = binding.editTextNome.text.toString() // Get the name

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                baseContext, "Cadastro realizado com sucesso.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // You can now proceed to upload the image (if selected)
                            // and store user data (name, etc.) in a database
                            uploadImageToStorage(name)
                        } else {
                            Toast.makeText(
                                baseContext, "Falha no cadastro.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(
                    baseContext, "Preencha todos os campos.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun processImage() {
        originalImageUri?.let { uri ->
            val resizedImageUri: Uri? = ImagemUtils.resizeImage(this, uri, 800, 600)
            val compressedImageUri: Uri? =
                resizedImageUri?.let { ImagemUtils.compressImage(this, it, 70) }

            compressedImageUri?.let {
                processedImageUri = it
                binding.imageViewProfile.setImageURI(it)
            }
        }
    }

    private fun uploadImageToStorage(name: String) {
        processedImageUri?.let { imageUri ->
            val userId = auth.currentUser?.uid ?: return // Get the user ID or return if null

            val imageRef = storageRef.child("profile_images/$userId.jpg") // Create a reference to the image

            val uploadTask = imageRef.putFile(imageUri)

            uploadTask.addOnSuccessListener {
                // Image uploaded successfully
                Log.d("CadastroActivity", "Image uploaded successfully")

                // Get the download URL
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    Log.d("CadastroActivity", "Image URL: $imageUrl")
                    // Now you can save the user data and the image URL to Firestore
                    saveUserDataToFirestore(name, imageUrl)
                }.addOnFailureListener {
                    // Handle any errors
                    Log.e("CadastroActivity", "Error getting image URL: ${it.message}")
                }
            }.addOnFailureListener {
                // Handle unsuccessful uploads
                Log.e("CadastroActivity", "Error uploading image: ${it.message}")
            }
        } ?: run {
            // No image selected, save user data without image URL
            saveUserDataToFirestore(name, null)
        }
    }

    private fun saveUserDataToFirestore(name: String, imageUrl: String?) {
        val userId = auth.currentUser?.uid ?: return // Get the user ID or return if null

        val userData = hashMapOf(
            "name" to name,
            "email" to auth.currentUser?.email,
            "imageUrl" to imageUrl
        )

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Log.d("CadastroActivity", "User data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("CadastroActivity", "Error saving user data: ${e.message}")
            }
    }
}