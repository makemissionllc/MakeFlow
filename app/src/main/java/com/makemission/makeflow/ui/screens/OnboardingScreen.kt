package com.makemission.makeflow.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    selectedCurrency: String = "KES",
    onCurrencyChange: (String) -> Unit = {},
    onSaveAccounts: suspend (List<AccountEntity>) -> Unit = {},
    onSaveName: suspend (String) -> Unit = {},
    onOnboardingComplete: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var accountBalance by remember { mutableStateOf("") }
    var showNameError by remember { mutableStateOf(false) }

    val transientAccounts = remember {
        mutableStateListOf<AccountEntity>().apply {
            add(AccountEntity(UUID.randomUUID().toString(), "Cash", 0.0, 0.0, "#4CAF50"))
            add(AccountEntity(UUID.randomUUID().toString(), "Bank account", 0.0, 0.0, "#4CAF50"))
        }
    }

    Scaffold(
        containerColor = Color(0xFF121315),
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = username.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> WelcomeStageView(
                        username = username,
                        onNameChange = {
                            username = it
                            if (it.isNotBlank()) showNameError = false
                        },
                        selectedCurrency = selectedCurrency,
                        onCurrencyChange = onCurrencyChange,
                        showNameError = showNameError
                    )
                    1 -> AccountSetupStageView(
                        transientAccounts = transientAccounts,
                        accountName = accountName,
                        accountBalance = accountBalance,
                        onNameChange = { accountName = it },
                        onBalanceChange = { accountBalance = it },
                        onAddAccount = {
                            val parsed = accountBalance.toDoubleOrNull() ?: 0.0
                            if (accountName.isNotBlank()) {
                                transientAccounts.add(
                                    AccountEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = accountName.trim(),
                                        balance = parsed,
                                        initialBalance = parsed,
                                        colorHex = "#FFFFFF"
                                    )
                                )
                                accountName = ""
                                accountBalance = ""
                            }
                        },
                        onRemoveAccount = { transientAccounts.remove(it) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        val isSelected = pagerState.currentPage == index
                        val widthScale by animateFloatAsState(
                            targetValue = if (isSelected) 24f else 8f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "OnboardingIndicator"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(widthScale.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.2f))
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage == 0) {
                            if (username.isNotBlank()) {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            } else {
                                showNameError = true
                            }
                        } else {
                            if (username.isNotBlank() && transientAccounts.isNotEmpty()) {
                                coroutineScope.launch {
                                    onSaveName(username.trim())
                                    onSaveAccounts(transientAccounts.toList())
                                    onOnboardingComplete()
                                }
                            }
                        }
                    },
                    enabled = if (pagerState.currentPage == 0) true else transientAccounts.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF121315),
                        disabledContainerColor = Color(0xFF1B1C1F),
                        disabledContentColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == 0) "Get started" else "Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStageView(
    username: String,
    onNameChange: (String) -> Unit,
    selectedCurrency: String = "KES",
    onCurrencyChange: (String) -> Unit = {},
    showNameError: Boolean = false
) {
    val currencies = listOf("KES", "USD", "EUR", "GBP", "UGX", "TZS")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((-12).dp)
            ) {
                OnboardingVisualCard("Cash", "KES 3,600.00", Icons.Rounded.Wallet, Modifier.align(Alignment.Start))
                OnboardingVisualCard("Savings", "KES 10,000.00", Icons.Rounded.Savings, Modifier.align(Alignment.End))
                OnboardingVisualCard("Credit card", "KES 1,450.00", Icons.Rounded.AccountBalanceWallet, Modifier.align(Alignment.Start))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "MakeFlow", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Text(text = "Manage all your finances with ease.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = username,
                onValueChange = onNameChange,
                label = { Text("Enter your name to unlock workspace") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF1B1C1F),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF1B1C1F),
                    unfocusedContainerColor = Color(0xFF1B1C1F)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (showNameError) {
                Text(
                    text = "Please enter your name to continue",
                    color = Color(0xFFFF3B30),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currencies.forEach { code ->
                    val isSelected = code == selectedCurrency
                    Text(
                        text = code,
                        color = if (isSelected) Color(0xFF121315) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(
                                if (isSelected) Color.White else Color(0xFF1B1C1F),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onCurrencyChange(code) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Text(text = "All your financial data is kept securely on your local device.", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
        }
    }
}

@Composable
fun AccountSetupStageView(
    transientAccounts: List<AccountEntity>,
    accountName: String,
    accountBalance: String,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (AccountEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Quick setup", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Text(text = "Seed initial trackers below. These form your account balance core.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = accountName,
                onValueChange = onNameChange,
                label = { Text("Account Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF1B1C1F),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF1B1C1F),
                    unfocusedContainerColor = Color(0xFF1B1C1F)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1.3f)
            )

            OutlinedTextField(
                value = accountBalance,
                onValueChange = onBalanceChange,
                label = { Text("Bal (KES)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF1B1C1F),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF1B1C1F),
                    unfocusedContainerColor = Color(0xFF1B1C1F)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onAddAccount,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White, RoundedCornerShape(14.dp))
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF121315))
            }
        }

        Text(text = "Accounts Ledger Topology", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.12f to Color.Black,
                            0.88f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(transientAccounts) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1C1F), RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF121315), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text(text = item.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = String.format("KES %,.2f", item.balance), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Discard",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onRemoveAccount(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingVisualCard(name: String, balance: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .width(230.dp)
            .background(Color(0xFF1B1C1F), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF121315), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(text = name, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Text(text = balance, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
