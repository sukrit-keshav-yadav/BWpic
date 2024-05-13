package com.example.bwpic

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class LandingPage : AppCompatActivity() {

    lateinit var  button : MaterialButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        button = findViewById(R.id.gotoCamera)
        button.setOnClickListener {
            val intent = Intent(this@LandingPage, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}