package com.example.teolupapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        val nameEdit = findViewById<TextInputEditText>(R.id.name_edit)
        val emailEdit = findViewById<TextInputEditText>(R.id.email_edit)
        val passwordEdit = findViewById<TextInputEditText>(R.id.password_edit)
        val passwordConfirmEdit = findViewById<TextInputEditText>(R.id.password_confirm_edit)
        val registerBtn = findViewById<Button>(R.id.btn_register)
        val goLoginBtn = findViewById<TextView>(R.id.btn_go_login)

        registerBtn.setOnClickListener {
            val name = nameEdit.text.toString().trim()
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()
            val confirm = passwordConfirmEdit.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirm.isNotEmpty()) {
                if (password == confirm) {
                    if (password.length >= 6) {
                        // 회원가입 진행
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // 가입 성공 시 이름(DisplayName) 설정
                                    val user = auth.currentUser
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build()

                                    user?.updateProfile(profileUpdates)
                                        ?.addOnCompleteListener { 
                                            Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                                            finish() // 로그인 화면으로 복귀
                                        }
                                } else {
                                    // 가입 실패
                                    Toast.makeText(this, "가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        goLoginBtn.setOnClickListener {
            finish()
        }
    }
}