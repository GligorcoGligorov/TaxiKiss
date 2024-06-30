package com.example.myapplication

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {

    private lateinit var e1_email: EditText
    private lateinit var e2_password: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var dialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SignInActivity", "Firebase apps: ${FirebaseApp.getApps(this).size}")
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        e1_email = findViewById(R.id.editText2);
        e2_password = findViewById(R.id.editText3);
        auth = FirebaseAuth.getInstance()
        dialog = ProgressDialog(this);

    }
    fun signInUser(view : View){
        dialog.setMessage("Signing in. Please wait");
        dialog.show();

        if(e1_email.text.toString().equals("") || e2_password.text.toString().equals("")){
            Toast.makeText(applicationContext, "Fields cannot be empty!", Toast.LENGTH_SHORT).show();
        }else{
            auth.signInWithEmailAndPassword(e1_email.text.toString(), e2_password.text.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        dialog.hide();
                        dialog.dismiss()
                        Toast.makeText(applicationContext, "Sign in successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainPageActivity::class.java)
                        startActivity(intent)
                        finish()
                        // Add code to navigate to the next activity or update UI
                    } else {
                        // If sign in fails, display a message to the user
                        dialog.hide();

                        dialog.dismiss()
                        Toast.makeText(applicationContext, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }


        }
    }
}