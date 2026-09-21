package com.example.crop916

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var cropView: CropView
    private lateinit var selectBtn: Button
    private lateinit var applyBtn: Button
    private lateinit var saveBtn: Button

    private var croppedBitmap: Bitmap? = null

    private val targetWidth = 1080
    private val targetHeight = 1920

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            loadImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cropView = findViewById(R.id.cropView)
        selectBtn = findViewById(R.id.selectBtn)
        applyBtn = findViewById(R.id.applyBtn)
        saveBtn = findViewById(R.id.saveBtn)

        selectBtn.setOnClickListener {
            pickImage.launch("image/*")
        }

        applyBtn.setOnClickListener {
            applyCrop()
        }

        saveBtn.setOnClickListener {
            saveImage()
        }
    }

    private fun loadImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
                return
            }

            cropView.setBitmap(bitmap)
            applyBtn.isEnabled = true
            saveBtn.isEnabled = false
            croppedBitmap = null

            Toast.makeText(this, "Двигай фото пальцем, потом нажми Применить", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyCrop() {
        val cropped = cropView.getCroppedBitmap() ?: return
        val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        croppedBitmap = scaled
        saveBtn.isEnabled = true
        Toast.makeText(this, "✅ Обрезано. Нажми Сохранить", Toast.LENGTH_SHORT).show()
    }

    private fun saveImage() {
        val bitmap = croppedBitmap ?: return

        try {
            val filename = "crop916_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Crop916")
                }
            }

            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw Exception("Не удалось создать файл")

            val outputStream = contentResolver.openOutputStream(uri)
                ?: throw Exception("Не удалось открыть поток")

            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.close()

            Toast.makeText(this, "✅ Сохранено в Pictures/Crop916", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
