package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.ui.MainViewModel
import com.upi.expensetracker.ui.theme.*
import com.upi.expensetracker.utils.Exporter

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()

    var userNameInput by remember { mutableStateOf(viewModel.userName) }
    var isLockEnabled by remember { mutableStateOf(viewModel.isAppLockEnabled) }
    
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Profile Configuration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = {
                        userNameInput = it
                        viewModel.userName = it
                    },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color(0xFF2C2C2C),
                        focusedLabelColor = PrimaryPurple
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Preferences Section (App Lock)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Lock Protection",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Require PIN/Biometrics on startup",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isLockEnabled,
                    onCheckedChange = {
                        isLockEnabled = it
                        viewModel.isAppLockEnabled = it
                        val msg = if (it) "App lock enabled" else "App lock disabled"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = PrimaryPurple
                    )
                )
            }
        }

        // Export/Backup data Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Backup & Exporters",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // CSV Export Button
                Button(
                    onClick = {
                        if (allTransactions.isEmpty()) {
                            Toast.makeText(context, "No transactions to export!", Toast.LENGTH_SHORT).show()
                        } else {
                            Exporter.shareCSV(context, allTransactions)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share CSV", tint = TextPrimary)
                        Text(text = "Export Current Transactions (CSV)", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PDF Export Button
                Button(
                    onClick = {
                        if (allTransactions.isEmpty()) {
                            Toast.makeText(context, "No transactions to export!", Toast.LENGTH_SHORT).show()
                        } else {
                            Exporter.sharePDF(context, allTransactions)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = TextPrimary)
                        Text(text = "Export Current Transactions (PDF)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Testing Simulator Actions Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Developer Testing Options",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Inject mock transactions directly into Room DB to test analytics, split expenses, and budgets without actual SIM alerts.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                
                Button(
                    onClick = {
                        viewModel.injectMockSMS()
                        Toast.makeText(context, "Mock data successfully injected!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Developer", tint = PrimaryPurple)
                        Text(text = "Inject Simulated SMS Alerts", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }

        // Danger Zone
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Danger Zone",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningRed
                )
                Text(
                    text = "Deleting transactions is permanent. Offline storage data cannot be recovered.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                
                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = TextPrimary)
                        Text(text = "Wipe Database Records", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // About section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = "About", tint = TextSecondary, modifier = Modifier.size(14.dp))
                Text(text = "Privacy Note", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "All your data is compiled and stored completely offline on this device. No external servers or networking resources are utilized.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "Version 1.0.0 (Native Room DB & Compose)",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }

    // Confirmation dialog overlay
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = "Confirm Data Wipe") },
            text = { Text(text = "Are you absolutely sure you want to delete all transaction history? This action is irreversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                        Toast.makeText(context, "All transaction records deleted.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = "WIPE ALL", color = WarningRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
