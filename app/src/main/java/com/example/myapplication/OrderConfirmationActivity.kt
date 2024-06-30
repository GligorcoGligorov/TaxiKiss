package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        val confirmationText = findViewById<TextView>(R.id.confirmationText)
        confirmationText.text = "Successfully ordered a taxi, please wait at your location."

        val homeButton = findViewById<Button>(R.id.backButton)
        homeButton.setOnClickListener {
            navigateToMainPage()
        }
    }

    private fun navigateToMainPage() {
        val intent = Intent(this, MainPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}