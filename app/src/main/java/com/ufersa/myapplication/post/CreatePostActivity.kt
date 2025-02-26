package com.ufersa.myapplication.post

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.semantics.text
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.ufersa.myapplication.R
import com.ufersa.myapplication.databinding.ActivityCreatePostBinding
import com.ufersa.myapplication.model.Post
import com.ufersa.myapplication.utils.ImagemUtils

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private lateinit var auth: FirebaseAuth
    private var originalImageUri: Uri? = null
    private var processedImageUri: Uri? = null
    private var userProfileImageUrl: String? = null

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val GALLERY_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase components
        firestore = Firebase.firestore
        storageRef = Firebase.storage.reference
        auth = Firebase.auth

        // Setup Toolbar
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.fabAddImage.setOnClickListener {
            showImageSourceDialog()
        }

        binding.buttonCreatePost.setOnClickListener {
            createPost()
        }
        getUserProfileImageUrl()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Tirar Foto", "Selecionar da Galeria")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Adicionar Imagem")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpenCamera()
                    1 -> checkGalleryPermissionAndOpenGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun checkGalleryPermissionAndOpenGallery() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                GALLERY_PERMISSION_REQUEST_CODE
            )
        } else {
            openGallery()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
                imageBitmap?.let {
                    originalImageUri = ImagemUtils.bitmapToUri(this, it)
                    processImage()
                }
            } else {
                Toast.makeText(this, "Nenhuma imagem foi capturada", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                originalImageUri = result.data?.data
                processImage()
            } else {
                Toast.makeText(this, "Nenhuma imagem foi selecionada", Toast.LENGTH_SHORT).show()
            }
        }

    private fun processImage() {
        originalImageUri?.let { uri ->
            val resizedImageUri: Uri? = ImagemUtils.resizeImage(this, uri, 800, 600)
            val compressedImageUri: Uri? =
                resizedImageUri?.let { ImagemUtils.compressImage(this, it, 70) }

            compressedImageUri?.let {
                processedImageUri = it
                binding.imageViewPost.setImageURI(it)
            }
        }
    }

    private fun createPost() {
        val description = binding.editTextDescription.text.toString()

        if (description.isEmpty() || processedImageUri == null) {
            Toast.makeText(
                this,
                "Preencha todos os campos e selecione uma imagem",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val user = auth.currentUser
        val userId = user?.uid ?: return // Get the user ID or return if null
        val userName = user.displayName ?: user.email ?: "Usuário"
        val timestamp = Timestamp.now()
        val imageName = "$userId-${timestamp.seconds}.jpg"
        val imageRef = storageRef.child("posts/$imageName")

        processedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val post = Post(
                            userId = userId,
                            userName = userName,
                            description = description,
                            imageUrl = downloadUri.toString(),
                            timestamp = timestamp,
                            profileImageUrl = userProfileImageUrl
                        )

                        firestore.collection("posts")
                            .add(post)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Post criado com sucesso",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("CreatePostActivity", "Erro ao criar post: ${e.message}")
                                Toast.makeText(
                                    this,
                                    "Erro ao criar post",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CreatePostActivity", "Erro ao fazer upload da imagem: ${e.message}")
                    Toast.makeText(
                        this,
                        "Erro ao fazer upload da imagem",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }


    private fun getUserProfileImageUrl() {
        val user = auth.currentUser
        val userId = user?.uid ?: return

        val profileImageRef = storageRef.child("profileImages/$userId.jpg")

        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
            userProfileImageUrl = uri.toString()
        }.addOnFailureListener {
            userProfileImageUrl = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
                }
            }
            GALLERY_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permissão de galeria negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}