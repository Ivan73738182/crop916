package com.example.crop916

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var cropView: CropView
    private lateinit var selectBtn: Button
    private lateinit var applyBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var shareBtn: Button
    private lateinit var rotateBtn: Button
    private lateinit var undoBtn: Button
    private lateinit var centerBtn: Button
    private lateinit var themeBtn: Button
    private lateinit var rootLayout: LinearLayout

    private lateinit var format916: Button
    private lateinit var format11: Button
    private lateinit var format45: Button
    private lateinit var format169: Button

    private lateinit var quality720: Button
    private lateinit var quality1080: Button
    private lateinit var quality1440: Button

    private var croppedBitmap: Bitmap? = null
    private var currentRatio = 9f / 16f
    private var currentQuality = 1080
    private var isDarkTheme = true

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) loadImage(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cropView = findViewById(R.id.cropView)
        selectBtn = findViewById(R.id.selectBtn)
        applyBtn = findViewById(R.id.applyBtn)
        saveBtn = findViewById(R.id.saveBtn)
        shareBtn = findViewById(R.id.shareBtn)
        rotateBtn = findViewById(R.id.rotateBtn)
        undoBtn = findViewById(R.id.undoBtn)
        centerBtn = findViewById(R.id.centerBtn)
        themeBtn = findViewById(R.id.themeBtn)
        rootLayout = findViewById(R.id.rootLayout)

        format916 = findViewById(R.id.format916)
        format11 = findViewById(R.id.format11)
        format45 = findViewById(R.id.format45)
        format169 = findViewById(R.id.format169)

        quality720 = findViewById(R.id.quality720)
        quality1080 = findViewById(R.id.quality1080)
        quality1440 = findViewById(R.id.quality1440)

        selectBtn.setOnClickListener { pickImage.launch("image/*") }
        applyBtn.setOnClickListener { applyCrop() }
        saveBtn.setOnClickListener { saveImage() }
        shareBtn.setOnClickListener { shareImage() }
        rotateBtn.setOnClickListener { cropView.rotate() }
        undoBtn.setOnClickListener { cropView.undo() }
        centerBtn.setOnClickListener { cropView.centerImage() }
        themeBtn.setOnClickListener { toggleTheme() }

        format916.setOnClickListener { selectFormat(9f / 16f, format916) }
        format11.setOnClickListener { selectFormat(1f, format11) }
        format45.setOnClickListener { selectFormat(4f / 5f, format45) }
        format169.setOnClickListener { selectFormat(16f / 9f, format169) }

        quality720.setOnClickListener { selectQuality(720, quality720) }
        quality1080.setOnClickListener { selectQuality(1080, quality1080) }
        quality1440.setOnClickListener { selectQuality(1440, quality1440) }
    }

    private fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        if (isDarkTheme) {
            rootLayout.setBackgroundColor(Color.parseColor("#121212"))
            themeBtn.text = "☀"
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#F5F5F5"))
            themeBtn.text = "🌙"
        }
    }

    private fun selectQuality(q: Int, btn: Button) {
        currentQuality = q
        val btns = listOf(quality720, quality1080, quality1440)
        btns.forEach { it.setBackgroundColor(0xFF2A2A3A.toInt()) }
        btn.setBackgroundColor(0xFF6A1B9A.toInt())
    }

    private fun selectFormat(ratio: Float, activeBtn: Button) {
        currentRatio = ratio
        cropView.setRatio(ratio)
        saveBtn.isEnabled = false
        shareBtn.isEnabled = false
        croppedBitmap = null

        val btns = listOf(format916, format11, format45, format169)
        btns.forEach { it.setBackgroundColor(0xFF2A2A3A.toInt()) }
        activeBtn.setBackgroundColor(0xFF6A1B9A.toInt())
    }

    private fun loadImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) {
                Toast.makeText(this, "Не удалось загрузить", Toast.LENGTH_SHORT).show()
                return
            }
            cropView.setBitmap(bitmap)
            applyBtn.isEnabled = true
            saveBtn.isEnabled = false
            shareBtn.isEnabled = false
            croppedBitmap = null
            Toast.makeText(this, "Двигай фото, масштабируй пальцами", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyCrop() {
        val cropped = cropView.getCroppedBitmap() ?: return

        val (baseW, baseH) = when {
            currentRatio < 0.7f -> Pair(1080, 1920)    // 9:16
            currentRatio < 0.9f -> Pair(1080, 1350)    // 4:5
            currentRatio < 1.2f -> Pair(1080, 1080)    // 1:1
            else -> Pair(1920, 1080)                   // 16:9
        }

        val factor = currentQuality / 1080f
        val w = (baseW * factor).toInt()
        val h = (baseH * factor).toInt()

        val scaled = Bitmap.createScaledBitmap(cropped, w, h, true)
        croppedBitmap = scaled
        saveBtn.isEnabled = true
        shareBtn.isEnabled = true
        Toast.makeText(this, "✅ ${w}×${h}. Нажми Сохранить", Toast.LENGTH_SHORT).show()
    }

    private fun saveImage() {
        val bitmap = croppedBitmap ?: return
        try {
            val filename = "snapfit_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SnapFit")
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Не удалось создать")
            val outputStream = contentResolver.openOutputStream(uri)
                ?: throw Exception("Не удалось открыть поток")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.close()
            Toast.makeText(this, "✅ Сохранено в Pictures/SnapFit", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareImage() {
        val bitmap = croppedBitmap ?: return
        try {
            val cachePath = File(cacheDir, "shared")
            cachePath.mkdirs()
            val file = File(cachePath, "share_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Поделиться"))
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
