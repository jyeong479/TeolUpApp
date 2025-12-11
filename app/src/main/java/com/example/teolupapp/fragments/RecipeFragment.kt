package com.example.teolupapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.teolupapp.R

class RecipeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnRecommend = view.findViewById<Button>(R.id.btn_recommend)

        btnRecommend.setOnClickListener {
            // ChatFragment로 이동 (Compose 기반 채팅 화면)
            // 주의: fragment_container ID가 다를 경우 앱이 종료될 수 있으므로, 
            // main_activity.xml의 실제 컨테이너 ID로 변경해야 할 수도 있습니다.
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChatFragment()) 
                .addToBackStack(null)
                .commit()
        }
    }
}