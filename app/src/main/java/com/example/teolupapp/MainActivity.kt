package com.example.teolupapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.teolupapp.fragments.CartFragment
import com.example.teolupapp.fragments.MyrefriFragment
import com.example.teolupapp.fragments.RecipeFragment
import com.example.teolupapp.fragments.SettingFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottom_nav)
        if (savedInstanceState == null) {
            replaceFragment(MyrefriFragment())
            bottomNav.selectedItemId = R.id.my_refri
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.my_refri -> {
                    replaceFragment(MyrefriFragment())
                    true
                }
                R.id.recipe -> {
                    replaceFragment(RecipeFragment())
                    true
                }
                R.id.cart -> {
                    replaceFragment(CartFragment())
                    true
                }
                R.id.setting -> {
                    replaceFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
