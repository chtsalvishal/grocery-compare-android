package com.example.grocerycompare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.grocerycompare.ui.theme.FreshGreen
import com.example.grocerycompare.ui.theme.InactiveSlate

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // Removed blur and transparency to fix the translucent overlap issue.
    // Using a solid background with z-index to sit cleanly on top of the list.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(1f),
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = currentRoute == "home",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    }
                )

                // Integrated Scan Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(FreshGreen)
                        .clickable { /* Trigger Scan Action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Scan", 
                        tint = Color.White, 
                        modifier = Modifier.size(28.dp)
                    )
                }

                NavBarItem(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    selected = currentRoute == "profile",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (currentRoute != "profile") {
                            navController.navigate("profile") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) FreshGreen else InactiveSlate
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) FreshGreen else InactiveSlate,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
