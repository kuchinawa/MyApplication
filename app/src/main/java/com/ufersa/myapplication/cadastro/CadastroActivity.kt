package com.ufersa.myapplication.cadastro

import android.content.Intent
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
import com.ufersa.myapplication.post.HomePagePost
import com.ufersa.myapplication.utils.ImagemUtils

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storageRef: StorageReference
    private lateinit var firestore: FirebaseFirestore
    private var originalImageUri: Uri? = null
    private var processedImageUri: Uri? = null
    private var isRegistering = false

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

        binding.buttonAddPhoto.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.buttonCadastrar.setOnClickListener {
            if (!isRegistering) {
                isRegistering = true
                binding.buttonCadastrar.isEnabled = false
                binding.buttonCadastrar.text = "Cadastrando..."

                val email = binding.editTextEmail.text.toString()
                val password = binding.editTextSenha.text.toString()
                val name = binding.editTextNome.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    baseContext, "Cadastro realizado com sucesso.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                uploadImageToStorage(name)
                                val intent = Intent(this, HomePagePost::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    baseContext, "Falha no cadastro.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                clearFields()
                            }
                            isRegistering = false
                            binding.buttonCadastrar.isEnabled = true
                            binding.buttonCadastrar.text = "Cadastrar"
                        }
                } else {
                    Toast.makeText(
                        baseContext, "Preencha todos os campos.",
                        Toast.LENGTH_SHORT
                    ).show()
                    isRegistering = false
                    binding.buttonCadastrar.isEnabled = true
                    binding.buttonCadastrar.text = "Cadastrar"
                }
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
            val userId = auth.currentUser?.uid ?: return

            val imageRef = storageRef.child("profile_images/$userId.jpg")
            val uploadTask = imageRef.putFile(imageUri)

            uploadTask.addOnSuccessListener {
                Log.d("CadastroActivity", "Image uploaded successfully")

                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    Log.d("CadastroActivity", "Image URL: $imageUrl")
                    saveUserDataToFirestore(name, imageUrl)
                }.addOnFailureListener {
                    Log.e("CadastroActivity", "Error getting image URL: ${it.message}")
                }
            }.addOnFailureListener {
                Log.e("CadastroActivity", "Error uploading image: ${it.message}")
            }
        } ?: run {
            saveUserDataToFirestore(name, null)
        }
    }

    private fun saveUserDataToFirestore(name: String, imageUrl: String?) {
        val userId = auth.currentUser?.uid ?: return

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

    private fun clearFields() {
        binding.editTextEmail.text.clear()
        binding.editTextSenha.text.clear()
        binding.editTextNome.text.clear()
    }
}