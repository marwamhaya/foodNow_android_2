package com.example.foodnow

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.foodnow.R

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.foodnow.ui.menu.CartBottomSheet
import com.example.foodnow.utils.CartManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        val layoutGlobalCart = findViewById<LinearLayout>(R.id.layoutGlobalCart)
        val btnGlobalPlaceOrder = findViewById<Button>(R.id.btnGlobalPlaceOrder)

        bottomNav.setupWithNavController(navController)

        // Cart Bar Click: Open Cart Bottom Sheet
        btnGlobalPlaceOrder.setOnClickListener {
            // CartBottomSheet now creates its own ViewModel and gets restaurantId from CartManager
            val bottomSheet = CartBottomSheet()
            bottomSheet.show(supportFragmentManager, "CartBottomSheet")
        }

        // Observe Cart to show/hide Cart Bar
        lifecycleScope.launch {
            CartManager.cartItems.collect { items ->
                if (items.isNotEmpty()) {
                    val total = CartManager.getTotal()
                    val itemCount = items.sumOf { it.quantity }
                    btnGlobalPlaceOrder.text = "View Cart ($itemCount) - ${String.format("%.2f", total)} DH"
                    layoutGlobalCart.visibility = View.VISIBLE
                } else {
                    layoutGlobalCart.visibility = View.GONE
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.ordersFragment, R.id.accountFragment, 
                R.id.menuFragment, R.id.allRestaurantsFragment, R.id.popularItemsFragment,
                R.id.restaurantDashboardFragment, R.id.restaurantOrdersFragment, 
                R.id.restaurantHomeFragment, R.id.restaurantAccountFragment -> {
                    bottomNav.visibility = View.VISIBLE
                }
                else -> {
                    bottomNav.visibility = View.GONE
                }
            }
        }
    }
}