package com.example.teolupapp.fragments

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teolupapp.AddRefriActivity
import com.example.teolupapp.R
import com.example.teolupapp.adapters.MyRefriAdapter
import com.example.teolupapp.models.Refri
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.util.Calendar

class MyrefriFragment: Fragment() {

    private val refriList = mutableListOf<Refri>()
    private lateinit var adapter: MyRefriAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Activity Result Launcher 등록
    @RequiresApi(Build.VERSION_CODES.O)
    private val addRefriLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val name = data?.getStringExtra("name")
            val category = data?.getStringExtra("category")
            val expiryString = data?.getStringExtra("expiry")
            val memo = data?.getStringExtra("memo") ?: ""

            if (name != null && category != null && expiryString != null) {
                // Firebase에 데이터 추가
                val key = database.push().key
                val user = auth.currentUser
                val uid = user?.uid ?: ""

                if (key != null) {
                    // userId 포함하여 생성
                    val newRefri = Refri(key, name, category, expiryString, memo, uid)
                    database.child(key).setValue(newRefri)
                        .addOnSuccessListener {
                            Toast.makeText(context, "재료가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_myrefri, container, false)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Auth 및 Firebase DB 초기화
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            // "refri/사용자UID" 경로 사용
            database = FirebaseDatabase.getInstance().getReference("refri").child(user.uid)
        } else {
            // 로그인 안 된 경우 (혹은 예외 처리) -> 일단 빈 경로 사용하지 않고 Toast
            Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            // 필요 시 로그인 화면으로 이동 로직 추가
            database = FirebaseDatabase.getInstance().getReference("refri").child("guest")
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = MyRefriAdapter(refriList) { refri, view ->
            showPopupMenu(refri, view, recyclerView)
        }
        recyclerView.adapter = adapter

        // Firebase 데이터 실시간 읽기
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                refriList.clear()
                for (data in snapshot.children) {
                    val refri = data.getValue(Refri::class.java)
                    if (refri != null) {
                        // Firebase 키를 모델에 설정
                        refri.key = data.key ?: ""
                        refriList.add(refri)
                    }
                }
                // 유통기한 임박순 정렬
                refriList.sortBy { it.expiryDate }
                
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // FAB 버튼 클릭 시 AddRefriActivity 실행
        val fab = view.findViewById<FloatingActionButton>(R.id.refri_add)
        fab.setOnClickListener {
            val intent = Intent(requireContext(), AddRefriActivity::class.java)
            addRefriLauncher.launch(intent)
        }

        // [개발용] FAB 버튼 길게 누르면 더미 데이터 추가
        fab.setOnLongClickListener {
            addDummyData()
            Toast.makeText(context, "더미 데이터가 추가되었습니다.", Toast.LENGTH_SHORT).show()
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showPopupMenu(
        selectedRefri: Refri,
        view: View,
        recyclerView: RecyclerView
    ){
        val popup = android.widget.PopupMenu(context, view)
        popup.menu.add("수정")
        popup.menu.add("삭제")

        popup.setOnMenuItemClickListener { menuItem ->
            when(menuItem.title){
                "수정" ->{
                    showEditDialog(selectedRefri)
                    true
                }
                "삭제" ->{
                    showDeleteDialog(selectedRefri)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showEditDialog(selectedRefri: Refri) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.activity_add_refri, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val title = dialogView.findViewById<android.widget.TextView>(R.id.title_text)
        val name = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_name)
        val category = dialogView.findViewById<AutoCompleteTextView >(R.id.category_spinner)
        val expiry = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_expiry)
        val memo = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_memo)
        val cancelBtn = dialogView.findViewById<Button>(R.id.btn_cancel)
        val saveBtn = dialogView.findViewById<Button>(R.id.btn_save)

        title.text = "재료 수정하기"
        cancelBtn.setOnClickListener { dialog.dismiss()}

        name.setText(selectedRefri.name)
        category.setText(selectedRefri.category)
        expiry.setText(selectedRefri.expiryDate)
        memo.setText(selectedRefri.memo)

        val categories = listOf("채소", "과일", "육류", "유제품", "가공식품", "음료", "반찬", "기타")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        category.setAdapter(adapter)

        expiry.setOnClickListener {
            val calendar = Calendar.getInstance()
            // 날짜 파싱 (yyyy-MM-dd)
            try {
                val dateParts = selectedRefri.expiryDate.split("-")
                if (dateParts.size == 3) {
                    calendar.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())
                }
            } catch (e: Exception) {
                // 파싱 실패 시 오늘 날짜
            }
            
            val datePicker = android.app.DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth)
                expiry.setText(formattedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        saveBtn.setOnClickListener {
            val newName = name.text.toString()
            val newCategory = category.text.toString()
            val newExpiry = expiry.text.toString()
            val newMemo = memo.text.toString()

            if (selectedRefri.key.isNotEmpty()) {
                // 기존 userId 유지하며 수정
                val updatedRefri = Refri(selectedRefri.key, newName, newCategory, newExpiry, newMemo, selectedRefri.userId)
                database.child(selectedRefri.key).setValue(updatedRefri)
                    .addOnSuccessListener {
                        Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                         Toast.makeText(context, "수정 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        dialog.show()
    }

    private fun showDeleteDialog(refri: Refri){
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("재료 삭제")
            .setMessage("정말 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                if (refri.key.isNotEmpty()) {
                    database.child(refri.key).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "삭제 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addDummyData() {
        val today = LocalDate.now()
        val user = auth.currentUser
        val uid = user?.uid ?: "guest"

        val dummyList = listOf(
            Refri("", "우유", "유제품", today.plusDays(2).toString(), "", uid),
            Refri("", "두부", "유제품", today.minusDays(1).toString(), "", uid),
            Refri("", "돼지고기", "육류", today.plusDays(3).toString(), "", uid),
            Refri("", "콩나물", "채소", today.plusDays(3).toString(), "", uid),
            Refri("", "식빵", "베이커리", today.plusDays(3).toString(), "", uid),
            Refri("", "시금치", "채소", today.plusDays(4).toString(), "", uid),
            Refri("", "소고기", "육류", today.plusDays(5).toString(), "", uid),
            Refri("", "요거트", "유제품", today.plusDays(6).toString(), "", uid),
            Refri("", "계란", "유제품", today.plusDays(7).toString(), "", uid),
            Refri("", "어묵", "가공식품", today.plusDays(5).toString(), "", uid),
            Refri("", "양상추", "채소", today.plusDays(10).toString(), "", uid),
            Refri("", "베이컨", "육류", today.plusDays(10).toString(), "", uid),
            Refri("", "오렌지주스", "음료", today.plusDays(12).toString(), "", uid),
            Refri("", "치즈", "유제품", today.plusDays(15).toString(), "", uid),
            Refri("", "햄", "가공식품", today.plusDays(15).toString(), "", uid),
            Refri("", "사과", "과일", today.plusDays(20).toString(), "", uid),
            Refri("", "감자", "채소", today.plusDays(20).toString(), "", uid),
            Refri("", "김치", "반찬", today.plusDays(30).toString(), "", uid),
            Refri("", "버터", "유제품", today.plusDays(45).toString(), "", uid),
            Refri("", "마늘", "채소", today.plusDays(60).toString(), "", uid),
            Refri("", "잼", "가공식품", today.plusDays(90).toString(), "", uid)
        )

        for (refri in dummyList) {
            val key = database.push().key
            if (key != null) {
                refri.key = key
                database.child(key).setValue(refri)
            }
        }
    }
}