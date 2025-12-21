package com.example.foodnow.ui.menu

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.foodnow.R
import com.example.foodnow.databinding.BottomSheetPaymentBinding
import com.example.foodnow.utils.CartManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.math.BigDecimal
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.location.Location
import android.location.LocationListener
import androidx.lifecycle.lifecycleScope

class PaymentBottomSheet(private val viewModel: MenuViewModel, private val restaurantId: Long) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetPaymentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val totalAmount = BigDecimal.valueOf(CartManager.getTotal())
        binding.tvPaymentAmount.text = "Amount to Pay: ${String.format("%.2f", totalAmount)} DH"
        
        // Load User Profile for Card Name
        viewModel.loadUserProfile()
        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.tvCardName.text = user.fullName.uppercase()
            }.onFailure {
                // Keep default or handle error
            }
        }

        // Toggle card details visibility
        binding.rgPaymentMethods.setOnCheckedChangeListener { _, checkedId ->
            binding.layoutCardDetails.visibility = if (checkedId == R.id.rbCard) View.VISIBLE else View.GONE
        }

        binding.btnConfirmPayment.setOnClickListener {
            val selectedId = binding.rgPaymentMethods.checkedRadioButtonId
            val method = if (selectedId == R.id.rbCard) "CARD_SIMULATION" else "CASH_ON_DELIVERY"
            
            // Validate Card Details if Card method is selected
            if (selectedId == R.id.rbCard) {
                if (!validateCardDetails()) {
                    return@setOnClickListener
                }
            }

            // Disable button
            binding.btnConfirmPayment.isEnabled = false
            binding.btnConfirmPayment.text = "Processing..."
            
            viewModel.processPayment(totalAmount, method)
        }

        viewModel.paymentResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Payment Successful: ${it.message}", Toast.LENGTH_SHORT).show()
                // Get GPS location and place order
                placeOrderWithLocation()
            }.onFailure {
                binding.btnConfirmPayment.isEnabled = true
                binding.btnConfirmPayment.text = "Pay & Order"
                Toast.makeText(context, "Payment Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.orderResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_LONG).show()
                dismiss() // This dismisses Payment Sheet
                // We should also ensure Cart Sheet is dismissed. 
                // However, since CartSheet opened this, usually we use a listener or shared viewmodel.
                // Assuming CartSheet observes cart empty -> dismisses itself.
            }.onFailure {
               // Order failed but payment succeeded? Complex rollback scenario usually. 
               // For demo/sim, we just show error.
               Toast.makeText(context, "Order Creation Failed: ${it.message}", Toast.LENGTH_SHORT).show()
               dismiss()
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            placeOrderWithLocation()
        } else {
             Toast.makeText(context, "Location permission required to place order", Toast.LENGTH_LONG).show()
             binding.btnConfirmPayment.isEnabled = true
             binding.btnConfirmPayment.text = "Pay & Order"
        }
    }

    private fun placeOrderWithLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        // Update UI to show locating status
        binding.btnConfirmPayment.text = "Locating..."
        binding.btnConfirmPayment.isEnabled = false
        
        lifecycleScope.launch {
             // Default to Casablaca if location fails completely, but try hard to get real loc
             var lat = 33.57311
             var lng = -7.58984
             
             try {
                // Try to get fresh location with timeout (increased to 15s)
                val location = withTimeoutOrNull(15000L) {
                    com.example.foodnow.service.LocationService.getCurrentLocation(requireContext())
                }
                
                if (location != null) {
                    lat = location.latitude
                    lng = location.longitude
                } else {
                    Toast.makeText(context, "Could not acquire precise location. Using default.", Toast.LENGTH_SHORT).show()
                }
             } catch (e: Exception) {
                 android.util.Log.e("PaymentBottomSheet", "Error locating", e)
             }
             
             // Place order with final coordinates
             viewModel.placeOrder(restaurantId, lat, lng)
        }
    }


    private fun validateCardDetails(): Boolean {
        var isValid = true

        val cardNumber = binding.etCardNumber.text.toString().trim()
        val expiry = binding.etExpiryDate.text.toString().trim()
        val cvv = binding.etCvv.text.toString().trim()
        val holder = binding.etCardHolder.text.toString().trim()

        // Validate Card Number (Simple check for length, e.g., 13-19 digits)
        if (cardNumber.length < 13) {
            binding.tilCardNumber.error = "Invalid Card Number"
            isValid = false
        } else {
            binding.tilCardNumber.error = null
        }

        // Validate Expiry (Simple MM/YY format check)
        if (!expiry.matches(Regex("^(0[1-9]|1[0-2])/[0-9]{2}$"))) {
            binding.tilExpiryDate.error = "Invalid Expiry (MM/YY)"
            isValid = false
        } else {
            binding.tilExpiryDate.error = null
        }

        // Validate CVV (3 or 4 digits)
        if (cvv.length !in 3..4) {
            binding.tilCvv.error = "Invalid CVV"
            isValid = false
        } else {
            binding.tilCvv.error = null
        }

        // Validate Holder Name
        if (holder.isEmpty()) {
            binding.tilCardHolder.error = "Name Required"
            isValid = false
        } else {
            binding.tilCardHolder.error = null
        }

        return isValid
    }
}
