package com.ufersa.myapplication.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImagemUtils {

    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        val imagesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val image = File(imagesDir, "temp_image.jpg")
        try {
            val outputStream = FileOutputStream(image)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            Log.e("ImagemUtils", "Erro ao converter Bitmap para Uri", e)
            return null
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", image)
    }

    fun resizeImage(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Uri? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("ImagemUtils", "Bitmap é nulo após decodificação.")
                return null
            }

            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            var newWidth = originalWidth
            var newHeight = originalHeight

            if (originalWidth > maxWidth || originalHeight > maxHeight) {
                val widthRatio = originalWidth.toFloat() / maxWidth.toFloat()
                val heightRatio = originalHeight.toFloat() / maxHeight.toFloat()
                val scaleRatio = maxOf(widthRatio, heightRatio)

                newWidth = (originalWidth / scaleRatio).toInt()
                newHeight = (originalHeight / scaleRatio).toInt()
            }

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            val rotatedBitmap = rotateImageIfRequired(context, resizedBitmap, uri)

            return saveImage(context, rotatedBitmap)
        } catch (e: Exception) {
            Log.e("ImagemUtils", "Erro ao redimensionar imagem", e)
            return null
        }
    }

    fun compressImage(context: Context, uri: Uri, quality: Int): Uri? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("ImagemUtils", "Bitmap é nulo após decodificação.")
                return null
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()

            val compressedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            return saveImage(context, compressedBitmap)
        } catch (e: Exception) {
            Log.e("ImagemUtils", "Erro ao comprimir imagem", e)
            return null
        }
    }

    private fun saveImage(context: Context, bitmap: Bitmap): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_$timeStamp.jpg"

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        var imageUri: Uri? = null
        try {
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
        } catch (e: Exception) {
            Log.e("ImagemUtils", "Erro ao salvar imagem", e)
        }
        return imageUri
    }

    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        val ei = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it)
            }
        } catch (e: IOException) {
            Log.e("ImagemUtils", "Erro ao ler metadados da imagem", e)
            null
        }

        val orientation = ei?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}