package com.example.grocerycompare.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grocerycompare.data.repository.MasterCatalogueRepository
import com.example.grocerycompare.ui.screens.home.HomeViewModel
import com.example.grocerycompare.ui.theme.FreshGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: MasterCatalogueRepository
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(application, repository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(FreshGreen)) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                CenterAlignedTopAppBar(
                    title = { Text("Profile & Settings", color = Color.White) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Text(
                text = "Regional Settings", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Adjust these to update prices for your area.")
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = uiState.city,
                onValueChange = { newCity ->
                    scope.launch {
                        repository.updatePreferences(newCity, uiState.postcode, uiState.suburb)
                    }
                },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = uiState.suburb,
                onValueChange = { newSuburb ->
                    scope.launch {
                        repository.updatePreferences(uiState.city, uiState.postcode, newSuburb)
                    }
                },
                label = { Text("Suburb") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = uiState.postcode,
                onValueChange = { newPostcode ->
                    scope.launch {
                        repository.updatePreferences(uiState.city, newPostcode, uiState.suburb)
                    }
                },
                label = { Text("Postcode") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { /* Reset Logic */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear App Data", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Grocery Compare v2.1 (Australian Fresh)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
