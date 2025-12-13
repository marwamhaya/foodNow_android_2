package com.example.foodnow.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.data.LivreurRequest
import com.example.foodnow.ui.ViewModelFactory

class CreateLivreurFragment : Fragment(R.layout.fragment_create_livreur) {

    private val viewModel: AdminViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPass = view.findViewById<EditText>(R.id.etPass)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val rbMoto = view.findViewById<RadioButton>(R.id.rbMoto)
        val rbVelo = view.findViewById<RadioButton>(R.id.rbVelo)
        val rbScooter = view.findViewById<RadioButton>(R.id.rbScooter)
        val btnCreate = view.findViewById<Button>(R.id.btnCreate)
        
        btnCreate.setOnClickListener {
            val vehicle = when {
                rbMoto.isChecked -> "MOTO"
                rbVelo.isChecked -> "VELO"
                rbScooter.isChecked -> "SCOOTER"
                else -> "VOITURE"
            }
            
            val req = LivreurRequest(
                userFullName = etName.text.toString(),
                userEmail = etEmail.text.toString(),
                userPassword = etPass.text.toString(),
                userPhoneNumber = etPhone.text.toString(),
                vehicleType = vehicle
            )
            
            viewModel.createLivreur(req,
                onSuccess = {
                    Toast.makeText(context, "Livreur Created", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onError = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
