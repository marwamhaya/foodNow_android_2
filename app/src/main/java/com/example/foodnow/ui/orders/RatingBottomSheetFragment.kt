package com.example.foodnow.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RatingBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var ratingBar: RatingBar
    private lateinit var etComment: EditText
    private lateinit var btnSubmit: Button
    private var orderId: Long = -1

    private val viewModel: OrdersViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rating_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orderId = arguments?.getLong("orderId") ?: -1

        ratingBar = view.findViewById(R.id.ratingBar)
        etComment = view.findViewById(R.id.etComment)
        btnSubmit = view.findViewById(R.id.btnSubmitRating)

        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            btnSubmit.isEnabled = rating > 0
        }

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = etComment.text.toString()
            
            if (orderId != -1L) {
                btnSubmit.isEnabled = false
                btnSubmit.text = "Submitting..."
                viewModel.submitRating(orderId, rating, comment)
            }
        }

        viewModel.ratingStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Rating submitted!", Toast.LENGTH_SHORT).show()
                dismiss()
            }.onFailure {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Submit Rating"
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
