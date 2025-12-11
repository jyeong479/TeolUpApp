package com.example.teolupapp.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.teolupapp.R
import com.example.teolupapp.models.Refri
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
class MyRefriAdapter(
    private val refriList: List<Refri>,
    private val onOptionClick: (Refri, View) -> Unit
    ): RecyclerView.Adapter<MyRefriAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val item_myrefri = itemView.findViewById<MaterialCardView>(R.id.item_myrefri)
        val item_name = itemView.findViewById<TextView>(R.id.item_name)
        val item_category = itemView.findViewById<TextView>(R.id.item_category)
        val exp_date = itemView.findViewById<TextView>(R.id.exp_date)
        val editBtn = itemView.findViewById<ImageButton>(R.id.editBtn)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_myrefri, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val refri = refriList[position]
        
        // String 날짜를 LocalDate로 변환
        val expiryDate = try {
            LocalDate.parse(refri.expiryDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            LocalDate.now() // 파싱 실패 시 오늘 날짜로 대체 (에러 방지)
        }

        val dDay = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
        
        holder.item_name.text = refri.name
        holder.item_category.text = refri.category
        holder.exp_date.text = when {
            dDay < 0 -> "만료"
            else -> dDay.toString() + "일 남음"
        }
        holder.item_myrefri.strokeColor = item_color(holder.itemView.context, expiryDate)
        holder.editBtn.setOnClickListener { view ->
            onOptionClick(refri, view)
        }
    }

    override fun getItemCount(): Int = refriList.size

    fun item_color (context: Context, expiryDate: LocalDate):Int{
        val today = LocalDate.now()
        val dDay = ChronoUnit.DAYS.between(today, expiryDate)
        val strokeColor = when {
            dDay < 0 -> R.color.grey    // 지남 (회색)
            dDay <= 3 -> R.color.red    // 3일 이하 (빨강)
            dDay <= 7 -> R.color.yellow // 7일 이하 (노랑)
            else -> R.color.green       // 그 외 (초록)
        }
        return ContextCompat.getColor(context, strokeColor)
    }
}