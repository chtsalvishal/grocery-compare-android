package com.example.grocerycompare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.grocerycompare.ui.components.BottomNavBar
import com.example.grocerycompare.ui.screens.home.HomeScreen
import com.example.grocerycompare.ui.screens.profile.ProfileScreen
import com.example.grocerycompare.ui.theme.GroceryCompareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as GroceryApplication).container
        
        setContent {
            GroceryCompareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = { BottomNavBar(navController) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        NavHost(
                            navController = navController, 
                            startDestination = "home",
                            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            composable("home") {
                                HomeScreen(
                                    repository = appContainer.repository
                                )
                            }

                            composable("profile") {
                                ProfileScreen(
                                    repository = appContainer.repository
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
