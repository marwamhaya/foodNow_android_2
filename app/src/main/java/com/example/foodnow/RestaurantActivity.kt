package com.example.foodnow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RestaurantActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_restaurant) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav_restaurant)
        
        androidx.navigation.ui.NavigationUI.setupWithNavController(bottomNav, navController)
    }
}
