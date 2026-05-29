package com.upi.expensetracker.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.BuildConfig
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
    var showClearDialog by remember { mutableStateOf(false) }

    // Refresh permission states whenever this screen is composed / resumed
    var hasSmsPermission by remember { mutableStateOf(viewModel.isSmsPermissionGranted()) }
    var hasNotificationAccess by remember { mutableStateOf(viewModel.isNotificationListenerGranted()) }
    var isNotificationSyncEnabled by remember { mutableStateOf(viewModel.isNotificationSyncEnabled) }
    var isAppLockEnabled by remember { mutableStateOf(viewModel.isAppLockEnabled) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // ── Profile ──────────────────────────────────────────────────────────
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

        // ── App Lock ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(
                width = 1.dp,
                color = if (isAppLockEnabled) Accent.copy(alpha = 0.4f) else Divider
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isAppLockEnabled) Accent.copy(alpha = 0.12f)
                                else Divider.copy(alpha = 0.3f),
                                androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "App Lock",
                            tint = if (isAppLockEnabled) Accent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "App Lock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            if (isAppLockEnabled) "Fingerprint / PIN required on open"
                            else "Tap to protect your financial data",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = { enabled ->
                        isAppLockEnabled = enabled
                        viewModel.isAppLockEnabled = enabled
                        val msg = if (enabled)
                            "App Lock enabled — biometric required on next open"
                        else
                            "App Lock disabled"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Background,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceElevated
                    )
                )
            }
        }

        // ── Permission Guides ─────────────────────────────────────────────────
        PermissionGuidesCard(
            hasSmsPermission = hasSmsPermission,
            hasNotificationAccess = hasNotificationAccess,
            onRefresh = {
                hasSmsPermission = viewModel.isSmsPermissionGranted()
                hasNotificationAccess = viewModel.isNotificationListenerGranted()
            },
            onOpenNotificationSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )

        // ── Notification Sync ─────────────────────────────────────────────────
        NotificationSyncCard(
            isEnabled = isNotificationSyncEnabled,
            hasAccess = hasNotificationAccess,
            onToggle = { enabled ->
                if (enabled && !hasNotificationAccess) {
                    // Remind user they need to grant access first
                    Toast.makeText(
                        context,
                        "Please grant Notification Access first (see Permission Guides above)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    isNotificationSyncEnabled = enabled
                    viewModel.isNotificationSyncEnabled = enabled
                    val msg = if (enabled) "Notification sync enabled" else "Notification sync disabled"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )

        // ── Export ────────────────────────────────────────────────────────────
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


        // ── Developer Tools ───────────────────────────────────────────────────
        // Only visible in debug builds — never ships in the production APK
        if (BuildConfig.DEBUG) {
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
        }

        // ── Danger Zone ───────────────────────────────────────────────────────
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

        // ── About / Privacy ───────────────────────────────────────────────────
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

    // ── Confirm wipe dialog ───────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Permission Guides Card
// Shows step-by-step visual guides for SMS and Notification Access permissions.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionGuidesCard(
    hasSmsPermission: Boolean,
    hasNotificationAccess: Boolean,
    onRefresh: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Permission Guides", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                // Refresh button to re-check permission state without restarting the app
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Refresh status", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }

            Text(
                "This app needs two permissions to automatically track your UPI payments. " +
                "Tap the guide steps below for instructions.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // ── SMS Permission guide ──────────────────────────────────────────
            PermissionGuideRow(
                icon = Icons.Default.Email,
                title = "SMS Access",
                description = "Reads bank transaction alerts from your inbox.",
                isGranted = hasSmsPermission,
                steps = listOf(
                    "Open your device Settings app",
                    "Go to Apps → UPI Expense Tracker",
                    "Tap Permissions → SMS",
                    "Select Allow"
                ),
                actionLabel = null, // SMS permission is handled via system dialog in MainActivity
                onAction = null
            )

            HorizontalDivider(color = Divider, thickness = 0.5.dp)

            // ── Notification Access guide ─────────────────────────────────────
            PermissionGuideRow(
                icon = Icons.Default.Notifications,
                title = "Notification Access",
                description = "Captures UPI payment notifications from Google Pay, PhonePe, Paytm, etc.",
                isGranted = hasNotificationAccess,
                steps = listOf(
                    "Tap the button below to open Notification Access settings",
                    "Find 'UPI Expense Tracker' in the list",
                    "Enable the toggle next to it",
                    "Tap Allow in the confirmation dialog",
                    "Return here and tap ↺ to refresh status"
                ),
                actionLabel = "Open Notification Access Settings",
                onAction = onOpenNotificationSettings
            )
        }
    }
}

@Composable
private fun PermissionGuideRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    steps: List<String>,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    var expanded by remember { mutableStateOf(!isGranted) }

    val statusColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF2ECC71) else Color(0xFFF39C12),
        animationSpec = tween(400),
        label = "statusColor"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Permission icon in tinted circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(statusColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, tint = statusColor, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                Text(description, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
            }

            // Status chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isGranted) "Granted" else "Required",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Expandable guide steps — shown by default if permission not yet granted
        if (!isGranted || expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("How to enable:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                steps.forEachIndexed { index, step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Step number badge
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Accent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent
                            )
                        }
                        Text(step, fontSize = 12.sp, color = TextPrimary, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                    }
                }

                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onAction,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = Background, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Background)
                    }
                }
            }
        }

        // Toggle to show/hide steps when already granted
        if (isGranted) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(
                    text = if (expanded) "Hide steps" else "Show setup steps",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Notification Sync Card
// Toggle to enable real-time UPI app notification capture.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationSyncCard(
    isEnabled: Boolean,
    hasAccess: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled && hasAccess) Accent.copy(alpha = 0.4f) else Divider
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header row with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Accent.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            "Notification Sync",
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("Notification Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Auto-capture UPI app payments", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Background,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceElevated
                    )
                )
            }

            // Description
            Text(
                "When enabled, payments made via Google Pay, PhonePe, Paytm, CRED, BHIM, Amazon Pay, and WhatsApp Pay will be automatically synced — even if you don't receive a bank SMS.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // Status banner
            val (bannerColor, bannerText, bannerIcon) = when {
                isEnabled && hasAccess  -> Triple(Color(0xFF2ECC71), "Active — listening for UPI notifications", Icons.Default.CheckCircle)
                isEnabled && !hasAccess -> Triple(Color(0xFFF39C12), "Notification Access not granted — sync won't work", Icons.Default.Warning)
                !isEnabled && hasAccess -> Triple(TextMuted, "Access granted but sync is toggled off", Icons.Default.Info)
                else                    -> Triple(TextMuted, "Disabled", Icons.Default.Info)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bannerColor.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(bannerIcon, null, tint = bannerColor, modifier = Modifier.size(16.dp))
                    Text(bannerText, fontSize = 12.sp, color = bannerColor, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
                }
            }

            // Quick shortcut to notification settings if access not granted
            if (!hasAccess) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AccentDim)
                ) {
                    Icon(Icons.Default.Settings, null, tint = Accent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grant Notification Access", fontSize = 12.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
