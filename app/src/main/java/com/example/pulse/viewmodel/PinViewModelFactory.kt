package com.example.pulse.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pulse.data.PinStorage

class PinViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(PinViewModel::class.java)) {

            return PinViewModel(
                PinStorage(context)
            ) as T

        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}