package com.upi.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.upi.expensetracker.data.AppDatabase
import com.upi.expensetracker.data.TransactionEntity
import com.upi.expensetracker.ui.MainViewModel
import com.upi.expensetracker.ui.MainViewModelFactory
import com.upi.expensetracker.ui.components.AddTransactionSheet
import com.upi.expensetracker.ui.components.EditTransactionSheet
import com.upi.expensetracker.ui.screens.*
import com.upi.expensetracker.ui.theme.*
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val viewModelFactory = MainViewModelFactory(
            database.transactionDao(),
            database.categoryDao(),
            applicationContext
        )

        executor = ContextCompat.getMainExecutor(this)

        setContent {
            UPIExpenseTrackerTheme {
                val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = viewModelFactory)

                // Track whether biometric authentication has been satisfied this session
                var isAuthenticated by remember { mutableStateOf(!viewModel.isAppLockEnabled) }

                if (isAuthenticated) {
                    MainAppLayout(viewModel)
                } else {
                    // Show the lock screen and launch biometric prompt
                    LockScreen(
                        onAuthenticate = {
                            showBiometricPrompt(
                                onSuccess = { isAuthenticated = true },
                                onFailed = {
                                    Toast.makeText(
                                        this,
                                        "Authentication failed. Try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    )
                    // Auto-trigger the prompt on first composition
                    LaunchedEffect(Unit) {
                        showBiometricPrompt(
                            onSuccess = { isAuthenticated = true },
                            onFailed = {}
                        )
                    }
                }
            }
        }
    }

    /**
     * Displays the system BiometricPrompt.
     *
     * Supports both strong biometrics (fingerprint, face) and device credential
     * (PIN, pattern, password) as a fallback — so the app lock always works even
     * on devices without a fingerprint sensor.
     *
     * @param onSuccess Called on successful authentication.
     * @param onFailed  Called when the user fails or cancels authentication.
     */
    private fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        // Check if any authenticator is available before showing the prompt
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // No authentication method available — grant access transparently
            onSuccess()
            return
        }

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Individual attempt failed (wrong finger) — callback handles UX
                    onFailed()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User cancelled or no more retries — stay on lock screen
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("UPI Expense Tracker")
            .setSubtitle("Authenticate to access your financial data")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

/**
 * A minimal lock screen shown when the app is locked and awaiting authentication.
 * The user taps "Unlock" to re-trigger the system BiometricPrompt.
 */
@Composable
fun LockScreen(onAuthenticate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = "🔒",
                fontSize = 56.sp
            )
            Text(
                text = "UPI Expense Tracker",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Your financial data is protected.\nAuthenticate to continue.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onAuthenticate,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    "Unlock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Background
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val allCategories by viewModel.allCategories.collectAsState()

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) Toast.makeText(context, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, "SMS Permission Denied. You can use mock data instead.", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        if (!hasSmsPermission) permissionLauncher.launch(android.Manifest.permission.READ_SMS)
    }

    var transactionEditing by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddTransaction by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // FAB scale animation
    val fabScale by animateFloatAsState(
        targetValue = if (currentRoute in listOf("home", "transactions")) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fabScale"
    )

    Scaffold(
        floatingActionButton = {
            if (currentRoute in listOf("home", "transactions")) {
                FloatingActionButton(
                    onClick = { showAddTransaction = true },
                    containerColor = Accent,
                    contentColor = Background,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.scale(fabScale)
                ) {
                    Icon(Icons.Default.Add, "Add Transaction")
                }
            }
        },
        bottomBar = {
            val showBottomBar = currentRoute in listOf("home", "transactions", "analytics", "settings")
            if (showBottomBar) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = Divider)
                    NavigationBar(
                        containerColor = Background,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Home, "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = Accent.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "transactions",
                            onClick = { navController.navigate("transactions") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.List, "Transactions") },
                            label = { Text("Transactions", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = Accent.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "analytics",
                            onClick = { navController.navigate("analytics") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Menu, "Analytics") },
                            label = { Text("Analytics", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = Accent.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = { navController.navigate("settings") { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Settings, "Settings") },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = Accent.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
            composable("home") { HomeScreen(viewModel, onEditTransaction = { transactionEditing = it }) }
            composable("transactions") { TransactionsScreen(viewModel, onEditTransaction = { transactionEditing = it }) }
            composable("analytics") {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sub-navigation chips — clean outlined, uniform accent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Background)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.navigate("insights") },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, AccentDim),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("Insights", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { navController.navigate("budgets") },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, AccentDim),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("Budgets", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { navController.navigate("categories") },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, AccentDim),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("Categories", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    }
                    AnalyticsScreen(viewModel = viewModel)
                }
            }
            composable("settings") { SettingsScreen(viewModel) }
            composable("budgets") { BudgetsScreen(viewModel, onBack = { navController.popBackStack() }) }
            composable("categories") { CategoriesScreen(viewModel, onBack = { navController.popBackStack() }) }
            composable("insights") { InsightsScreen(viewModel, onBack = { navController.popBackStack() }) }
        }
    }

    if (transactionEditing != null) {
        EditTransactionSheet(
            transaction = transactionEditing!!,
            categories = allCategories,
            onDismiss = { transactionEditing = null },
            onSave = { viewModel.updateTransaction(it); transactionEditing = null; Toast.makeText(context, "Transaction saved", Toast.LENGTH_SHORT).show() },
            onDelete = { viewModel.deleteTransaction(it); transactionEditing = null; Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show() }
        )
    }

    if (showAddTransaction) {
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        AddTransactionSheet(
            categories = allCategories,
            preselectedDate = todayDate,
            onDismiss = { showAddTransaction = false },
            onAdd = { amount, merchant, category, date, time, description, notes ->
                viewModel.addTransaction(amount, merchant, category, date, time, description, notes)
                showAddTransaction = false
                Toast.makeText(context, "Transaction added", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
