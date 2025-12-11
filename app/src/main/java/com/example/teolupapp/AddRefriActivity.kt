package com.example.teolupapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class AddRefriActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_refri)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val categories = listOf("채소", "과일", "육류", "유제품", "가공식품", "음료", "반찬", "기타")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        val categorySpinner = findViewById<AutoCompleteTextView>(R.id.category_spinner)
        categorySpinner.setAdapter(adapter)

        val editExpiry = findViewById<TextInputEditText>(R.id.edit_expiry)
        editExpiry.setOnClickListener {
            val calender = Calendar.getInstance()
            val year = calender.get(Calendar.YEAR)
            val month = calender.get(Calendar.MONTH)
            val day = calender.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, R.style.CustomDatePickerTheme, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate =
                    String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                editExpiry.setText(formattedDate)
            }, year, month, day)
            datePicker.show()
        }

        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.edit_name).text.toString()
            val category = categorySpinner.text.toString()
            val expiry = editExpiry.text.toString()
            val memo = findViewById<TextInputEditText>(R.id.edit_memo).text.toString()

            val resultIntent = Intent().apply {
                putExtra("name", name)
                putExtra("category", category)
                putExtra("expiry", expiry)
                putExtra("memo", memo)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}