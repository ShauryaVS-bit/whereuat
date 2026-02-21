package com.example.whereuat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whereuat.ui.WhereUAtApp
import com.example.whereuat.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.factory(this))
            val state by vm.uiState.collectAsState()
            WhereUAtApp(state = state, onAction = vm::onAction)
        }
    }
}
