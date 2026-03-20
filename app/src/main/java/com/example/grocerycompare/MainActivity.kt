package com.example.grocerycompare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            // Render a plain green Box for the first frame so the window
            // is visible instantly. After that frame completes, swap in the
            // full NavHost so the JIT compiler has already started warming up.
            var appReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withFrameNanos { } // wait for first frame to finish
                appReady = true
            }

            if (appReady) {
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
                                    HomeScreen(repository = appContainer.repository)
                                }
                                composable("profile") {
                                    ProfileScreen(repository = appContainer.repository)
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF00843D))
                )
            }
        }
    }
}
