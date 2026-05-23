package com.upi.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Database & ViewModel
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val viewModelFactory = MainViewModelFactory(
            database.transactionDao(),
            database.categoryDao(),
            applicationContext
        )

        setContent {
            UPIExpenseTrackerTheme {
                val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = viewModelFactory)
                MainAppLayout(viewModel)
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

    // SMS permission states
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "SMS Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS Permission Denied. You will need to manually grant permission or use mock data.", Toast.LENGTH_LONG).show()
        }
    }

    // Trigger permission request on start if not already granted
    LaunchedEffect(Unit) {
        if (!hasSmsPermission) {
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    // Active transaction editing bottom sheet state
    var transactionEditing by remember { mutableStateOf<TransactionEntity?>(null) }

    // Add new transaction bottom sheet state
    var showAddTransaction by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        floatingActionButton = {
            // Show FAB on Home and Transactions screens
            if (currentRoute in listOf("home", "transactions")) {
                FloatingActionButton(
                    onClick = { showAddTransaction = true },
                    containerColor = PrimaryPurple,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Transaction"
                    )
                }
            }
        },
        bottomBar = {
            // Hide bottom navigation if we are inside detailed screen views (budgets, categories, insights)
            val showBottomBar = currentRoute in listOf("home", "transactions", "analytics", "settings")
            if (showBottomBar) {
                Column {
                    // Top border line instead of shadow
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Divider
                    )
                    NavigationBar(
                        containerColor = Background,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                    // Home
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceElevated
                        )
                    )
                    
                    // Transactions
                    NavigationBarItem(
                        selected = currentRoute == "transactions",
                        onClick = {
                            navController.navigate("transactions") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = "Transactions") },
                        label = { Text("Transactions", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceElevated
                        )
                    )

                    // Analytics / Monthly Summary
                    NavigationBarItem(
                        selected = currentRoute == "analytics",
                        onClick = {
                            navController.navigate("analytics") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Analytics") },
                        label = { Text("Analytics", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceElevated
                        )
                    )

                    // Settings
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceElevated
                        )
                    )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onEditTransaction = { transactionEditing = it }
                )
            }
            composable("transactions") {
                TransactionsScreen(
                    viewModel = viewModel,
                    onEditTransaction = { transactionEditing = it }
                )
            }
            composable("analytics") {
                // Include navigation triggers to detailed budget, categories, and insights views
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { navController.navigate("insights") }) {
                            Text("💡 Insights", color = BottomNavSelected, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { navController.navigate("budgets") }) {
                            Text("🎯 Budgets", color = BottomNavSelected, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { navController.navigate("categories") }) {
                            Text("🏷️ Categories", color = BottomNavSelected, fontWeight = FontWeight.Bold)
                        }
                    }
                    AnalyticsScreen(viewModel = viewModel)
                }
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
            composable("budgets") {
                BudgetsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("categories") {
                CategoriesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("insights") {
                InsightsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    // Modal Edit Sheet Dialog
    if (transactionEditing != null) {
        EditTransactionSheet(
            transaction = transactionEditing!!,
            categories = allCategories,
            onDismiss = { transactionEditing = null },
            onSave = { updatedTxn ->
                viewModel.updateTransaction(updatedTxn)
                transactionEditing = null
                Toast.makeText(context, "Transaction saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Add Transaction Sheet
    if (showAddTransaction) {
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        AddTransactionSheet(
            categories = allCategories,
            preselectedDate = todayDate,
            onDismiss = { showAddTransaction = false },
            onAdd = { amount, merchant, category, date, time, description, notes ->
                viewModel.addTransaction(amount, merchant, category, date, time, description, notes)
                showAddTransaction = false
                Toast.makeText(context, "✅ Transaction added!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
