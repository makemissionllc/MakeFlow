package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit = {},
    onOnboardingComplete: (username: String, accounts: List<AccountEntity>) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var accountNameInput by remember { mutableStateOf("") }
    var accountBalanceInput by remember { mutableStateOf("") }
    
    val setupAccounts = remember { mutableStateListOf<AccountEntity>() }

    Scaffold(
        containerColor = Color(0xFF121315)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Let's set up your wallet",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Your Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                Text("Add Your Starting Accounts", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = accountNameInput,
                        onValueChange = { accountNameInput = it },
                        label = { Text("e.g. Bank/Cash") },
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = accountBalanceInput,
                        onValueChange = { accountBalanceInput = it },
                        label = { Text("Init Bal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val bal = accountBalanceInput.toDoubleOrNull() ?: 0.0
                            if (accountNameInput.isNotBlank()) {
                                setupAccounts.add(
                                    AccountEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = accountNameInput.trim(),
                                        balance = bal,
                                        initialBalance = bal,
                                        colorHex = "#1E88E5"
                                    )
                                )
                                accountNameInput = ""
                                accountBalanceInput = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account", tint = Color.Black)
                    }
                }

                // Dynamic list of user-created accounts
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(setupAccounts) { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2124), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                                Text(acc.name, color = Color.White, fontSize = 14.sp)
                            }
                            Text(String.format("KES %,.2f", acc.balance), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (username.isNotBlank() && setupAccounts.isNotEmpty()) {
                        onOnboardingComplete(username.trim(), setupAccounts.toList())
                        onCompleted()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = username.isNotBlank() && setupAccounts.isNotEmpty()
            ) {
                Text("Complete Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
