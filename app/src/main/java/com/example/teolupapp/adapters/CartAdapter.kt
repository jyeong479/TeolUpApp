package com.example.teolupapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.teolupapp.R
import com.example.teolupapp.models.Cart

class CartAdapter(
    private val cartList: List<Cart>,
    private val onOptionClick: (Cart, View) -> Unit,
    private val onCheckChange: (Cart, Boolean) -> Unit
    ): RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val check_item = view.findViewById<CheckBox>(R.id.check_item)
        val editBtn = view.findViewById<ImageButton>(R.id.editBtn)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cart = cartList[position]
        
        holder.check_item.text = cart.name
        
        holder.check_item.setOnCheckedChangeListener(null)
        // 변수명 변경 (isChecked -> checked)
        holder.check_item.isChecked = cart.checked
        
        holder.check_item.setOnCheckedChangeListener { _, isChecked ->
            onCheckChange(cart, isChecked)
        }
        
        holder.editBtn.setOnClickListener { view ->
            onOptionClick(cart, view)
        }
    }

    override fun getItemCount(): Int = cartList.size
}