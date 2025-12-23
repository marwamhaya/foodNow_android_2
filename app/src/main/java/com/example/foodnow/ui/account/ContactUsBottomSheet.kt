package com.example.foodnow.ui.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.foodnow.databinding.BottomSheetContactUsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ContactUsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetContactUsBinding

    companion object {
        private const val PHONE_NUMBER = "+212612345678"
        private const val EMAIL_ADDRESS = "support@foodnow.ma"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetContactUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Phone card click
        binding.cardPhone.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$PHONE_NUMBER")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open phone dialer", Toast.LENGTH_SHORT).show()
            }
        }

        // Email card click
        binding.cardEmail.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$EMAIL_ADDRESS")
                    putExtra(Intent.EXTRA_SUBJECT, "FoodNow Support Request")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open email client", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
