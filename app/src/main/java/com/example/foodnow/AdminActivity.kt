package com.example.foodnow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_admin) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav_admin)
        
        androidx.navigation.ui.NavigationUI.setupWithNavController(bottomNav, navController)
    }
}
