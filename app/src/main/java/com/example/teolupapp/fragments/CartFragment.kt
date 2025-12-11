package com.example.teolupapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teolupapp.R
import com.example.teolupapp.adapters.CartAdapter
import com.example.teolupapp.models.Cart
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartFragment : Fragment() {
    private val cartList = mutableListOf<Cart>()
    private lateinit var adapter: CartAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Auth 및 Firebase DB 초기화
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            // "shopping/사용자UID" 경로 사용
            database = FirebaseDatabase.getInstance().getReference("shopping").child(user.uid)
        } else {
            Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            database = FirebaseDatabase.getInstance().getReference("shopping").child("guest")
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 어댑터 설정 (옵션 클릭 콜백, 체크 상태 변경 콜백)
        adapter = CartAdapter(
            cartList,
            onOptionClick = { cart, v ->
                showPopupMenu(cart, v)
            },
            onCheckChange = { cart, isChecked ->
                // 로컬 데이터 즉시 업데이트
                cart.checked = isChecked
                // 리스트 내의 해당 객체도 업데이트
                cartList.find { it.key == cart.key }?.checked = isChecked
                
                // 체크 상태 변경 시 Firebase 업데이트 (필드명 checked 사용)
                if (cart.key.isNotEmpty()) {
                    database.child(cart.key).child("checked").setValue(isChecked)
                }
            }
        )
        recyclerView.adapter = adapter

        // Firebase 실시간 데이터 읽기
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cartList.clear()
                for (data in snapshot.children) {
                    val cart = data.getValue(Cart::class.java)
                    if (cart != null) {
                        cart.key = data.key ?: ""
                        cartList.add(cart)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        val addBtn = view.findViewById<Button>(R.id.add_cart)
        addBtn.setOnClickListener {
            addCart()
        }

        val checkedDelBtn = view.findViewById<Button>(R.id.checkedDel)
        checkedDelBtn.setOnClickListener {
            // 체크된 항목 일괄 삭제 (checked 필드 사용)
            val checkedItems = cartList.filter { it.checked }
            if (checkedItems.isEmpty()) {
                Toast.makeText(context, "선택된 항목이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            for (item in checkedItems) {
                if (item.key.isNotEmpty()) {
                    database.child(item.key).removeValue()
                }
            }
            Toast.makeText(context, "선택한 항목이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addCart() {
        val inputView = view?.findViewById<TextInputEditText>(R.id.cart_name)
        val addItem = inputView?.text.toString()
        val user = auth.currentUser
        val uid = user?.uid ?: ""
        
        if (addItem.isNotEmpty()) {
            val key = database.push().key
            if (key != null) {
                // 생성 시 userId 포함
                val newCart = Cart(key, addItem, false, uid)
                database.child(key).setValue(newCart)
                    .addOnSuccessListener {
                        inputView?.text?.clear()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun showPopupMenu(
        selectedCart: Cart,
        view: View
    ){
        val popup = android.widget.PopupMenu(context, view)
        popup.menu.add("수정")
        popup.menu.add("삭제")

        popup.setOnMenuItemClickListener { menuItem ->
            when(menuItem.title){
                "수정" -> {
                    showEditDialog(selectedCart)
                    true
                }
                "삭제" -> {
                    showDeleteDialog(selectedCart)
                    true
                }
                else -> false
                }
            }
        popup.show()
    }

    private fun showEditDialog(selectedCart: Cart) {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val editText = EditText(context)
        editText.setText(selectedCart.name)

        layout.addView(editText)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("장 볼 목록 수정")
            .setView(layout)
            .setPositiveButton("수정") {_, _ ->
                val newText = editText.text.toString()
                if (newText.isNotEmpty() && selectedCart.key.isNotEmpty()) {
                    
                    // 리스트에서 최신 체크 상태 확인
                    val currentItem = cartList.find { it.key == selectedCart.key }
                    val currentCheckedState = currentItem?.checked ?: selectedCart.checked
                    
                    // 기존 userId 유지
                    val updatedCart = selectedCart.copy(name = newText, checked = currentCheckedState, userId = selectedCart.userId)
                    database.child(selectedCart.key).setValue(updatedCart)
                        .addOnSuccessListener {
                            Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteDialog(cart: Cart){
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("목록 삭제")
            .setMessage("정말 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                if (cart.key.isNotEmpty()) {
                    database.child(cart.key).removeValue()
                        .addOnSuccessListener {
                             Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("취소", null)
            .show()

    }
}