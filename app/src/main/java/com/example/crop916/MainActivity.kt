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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var previewImage: ImageView
    private lateinit var selectBtn: Button
    private lateinit var saveBtn: Button

    private var selectedBitmap: Bitmap? = null

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

        previewImage = findViewById(R.id.previewImage)
        selectBtn = findViewById(R.id.selectBtn)
        saveBtn = findViewById(R.id.saveBtn)

        selectBtn.setOnClickListener {
            pickImage.launch("image/*")
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

            val cropped = cropTo916(bitmap)
            selectedBitmap = cropped

            previewImage.setImageBitmap(cropped)
            saveBtn.isEnabled = true

            Toast.makeText(this, "Фото готово", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cropTo916(source: Bitmap): Bitmap {
        val srcW = source.width
        val srcH = source.height
        val targetRatio = 9f / 16f
        val srcRatio = srcW.toFloat() / srcH.toFloat()

        val cropW: Int
        val cropH: Int
        val cropX: Int
        val cropY: Int

        if (srcRatio > targetRatio) {
            cropH = srcH
            cropW = (srcH * targetRatio).toInt()
            cropX = (srcW - cropW) / 2
            cropY = 0
        } else {
            cropW = srcW
            cropH = (srcW / targetRatio).toInt()
            cropX = 0
            cropY = (srcH - cropH) / 2
        }

        val cropped = Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)
        return Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
    }

    private fun saveImage() {
        val bitmap = selectedBitmap ?: return

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
