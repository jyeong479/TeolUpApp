package com.example.teolupapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // [자동 로그인 체크] 이미 로그인된 사용자가 있는지 확인
        if (auth.currentUser != null) {
            moveToMain() // 바로 메인으로 이동
            return
        }

        val emailEdit = findViewById<TextInputEditText>(R.id.email_edit)
        val passwordEdit = findViewById<TextInputEditText>(R.id.password_edit)
        val loginBtn = findViewById<Button>(R.id.btn_login)
        val goRegisterBtn = findViewById<TextView>(R.id.btn_go_register)

        // 로그인 버튼 클릭
        loginBtn.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            moveToMain()
                        } else {
                            Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 회원가입 페이지로 이동
        goRegisterBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // 메인 화면으로 이동하는 함수
    private fun moveToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // 뒤로가기 눌렀을 때 로그인 화면으로 다시 오지 않도록 스택 정리
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}