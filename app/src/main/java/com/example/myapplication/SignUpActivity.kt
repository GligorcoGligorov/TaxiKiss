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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var e5_name: EditText
    private lateinit var e6_email: EditText
    private lateinit var e7_password: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var dialog: ProgressDialog
    private lateinit var databasereference: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SignUpActivity", "Firebase apps: ${FirebaseApp.getApps(this).size}")
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        e5_name = findViewById(R.id.editText5);
        e6_email = findViewById(R.id.editText6);
        e7_password = findViewById(R.id.editText7);
        auth = FirebaseAuth.getInstance()
        dialog = ProgressDialog(this);
    }

    fun signUpUser(view : View){
        dialog.setMessage("Registering. Please wait");
        dialog.show();

        if(e5_name.text.toString().equals("") || e6_email.text.toString().equals("") || e7_password.text.toString().equals("")){
            Toast.makeText(applicationContext, "Fields cannot be blank!", Toast.LENGTH_SHORT).show();
        }else{
            auth.createUserWithEmailAndPassword(e6_email.text.toString(), e7_password.text.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        dialog.hide();

                        // Sign in success, update UI with the signed-in user's information
                        dialog.dismiss()
                        Toast.makeText(applicationContext, "User registered successfully!", Toast.LENGTH_SHORT).show()

                        databasereference = FirebaseDatabase.getInstance().getReference().child("Users");
                        val user:Users =  Users(e5_name.text.toString(),e6_email.text.toString(),e7_password.text.toString());
                        val firebaseUser: FirebaseUser? = auth.currentUser;

                        if (firebaseUser != null) {
                            databasereference.child(firebaseUser.uid).setValue(user)
                                .addOnCompleteListener { task2 ->
                                    if (task2.isSuccessful) {
                                        Toast.makeText(applicationContext, "User data saved", Toast.LENGTH_LONG).show()
                                        startActivity(Intent(this, MainPageActivity::class.java))

                                    } else {
                                        val error = task2.exception
                                        Log.e("DatabaseWrite", "Error saving user data: ${error?.message}", error)
                                        Toast.makeText(applicationContext, "User data could not be saved: ${error?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }else{
                            Log.e("user","User is null")
                        }

                        val intent = Intent(this, MainPageActivity::class.java)
                        startActivity(intent)
                        finish()
                        // Add code to navigate to the next activity or update UI
                    } else {
                        // If sign in fails, display a message to the user
                        dialog.hide();

                        dialog.dismiss()
                        Toast.makeText(applicationContext, "User registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }


        }
    }
}