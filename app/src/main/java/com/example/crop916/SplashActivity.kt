package com.example.crop916

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoImage)

        // Появление (плавно)
        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(800).start()

        // Через 3 секунды — переход в MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            logo.animate().alpha(0f).setDuration(400).withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.start()
        }, 2200)
    }
}
