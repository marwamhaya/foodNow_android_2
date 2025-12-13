package com.example.foodnow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LivreurActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_livreur)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_livreur) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav_livreur)
        
        androidx.navigation.ui.NavigationUI.setupWithNavController(bottomNav, navController)
    }
}
