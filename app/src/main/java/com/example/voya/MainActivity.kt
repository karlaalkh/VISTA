package com.example.voya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voya.ui.home.HomeRoute
import com.example.voya.ui.home.HomeViewModel
import com.example.voya.ui.theme.VoyaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as VoyaApplication).container

        setContent {
            VoyaTheme {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(container.counterRepository),
                )
                HomeRoute(viewModel = homeViewModel)
            }
        }
    }
}
