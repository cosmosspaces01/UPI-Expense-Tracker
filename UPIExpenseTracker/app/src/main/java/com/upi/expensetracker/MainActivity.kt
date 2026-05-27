package com.upi.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
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
        if (!hasSmsPermission) permissionLauncher.launch(Manifest.permission.READ_SMS)
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
