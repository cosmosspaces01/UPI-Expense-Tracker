package com.upi.expensetracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
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
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Profile
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Divider)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = { userNameInput = it; viewModel.userName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                        focusedLabelColor = Accent, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Security
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Divider)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("App Lock", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Require PIN/Biometrics on startup", fontSize = 12.sp, color = TextSecondary)
                }
                Switch(
                    checked = isLockEnabled,
                    onCheckedChange = {
                        isLockEnabled = it; viewModel.isAppLockEnabled = it
                        Toast.makeText(context, if (it) "App lock enabled" else "App lock disabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = Accent)
                )
            }
        }

        // Export
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Divider)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup & Export", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                Button(
                    onClick = {
                        if (allTransactions.isEmpty()) Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                        else Exporter.shareCSV(context, allTransactions)
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, "CSV", tint = Background)
                        Text("Export Transactions (CSV)", fontWeight = FontWeight.Bold, color = Background)
                    }
                }

                Button(
                    onClick = {
                        if (allTransactions.isEmpty()) Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                        else Exporter.sharePDF(context, allTransactions)
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDim),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, "PDF", tint = TextPrimary)
                        Text("Export Transactions (PDF)", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }

        // Developer Testing
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Divider)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Developer Tools", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Inject mock transactions for testing analytics, splits, and budgets without actual SMS alerts.", fontSize = 12.sp, color = TextSecondary)
                OutlinedButton(
                    onClick = {
                        viewModel.injectMockSMS()
                        Toast.makeText(context, "Mock data injected", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentDim)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, "Dev", tint = TextSecondary)
                        Text("Inject Mock SMS Data", fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    }
                }
            }
        }

        // Danger Zone
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, DebitRed.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Danger Zone", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DebitRed)
                Text("Deleting transactions is permanent. Offline storage data cannot be recovered.", fontSize = 12.sp, color = TextSecondary)
                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DebitRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.White)
                        Text("Wipe All Data", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // About
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Info, "About", tint = TextMuted, modifier = Modifier.size(14.dp))
                Text("Privacy Note", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            }
            Text(
                "All data is stored completely offline on this device. No external servers are used.",
                fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text("Version 1.0.0", fontSize = 10.sp, color = TextMuted)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Confirm Data Wipe") },
            text = { Text("Are you sure you want to delete all transaction history? This action is irreversible.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData(); showClearDialog = false
                    Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                }) { Text("WIPE ALL", color = DebitRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("CANCEL", color = TextPrimary) } },
            containerColor = Surface, titleContentColor = TextPrimary, textContentColor = TextSecondary
        )
    }
}
