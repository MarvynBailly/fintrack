package com.marvyn.fintrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marvyn.fintrack.ui.DashboardScreen
import com.marvyn.fintrack.ui.FinanceViewModel
import com.marvyn.fintrack.ui.SettingsScreen
import com.marvyn.fintrack.ui.TransactionsScreen
import com.marvyn.fintrack.ui.theme.FinTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinTrackTheme {
                FinTrackApp()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Filled.Dashboard),
    Transactions("Activity", Icons.Filled.ReceiptLong),
    Settings("Settings", Icons.Filled.Settings)
}

@Composable
private fun FinTrackApp(viewModel: FinanceViewModel = viewModel()) {
    var selected by remember { mutableStateOf(Tab.Dashboard) }
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (selected) {
            Tab.Dashboard -> DashboardScreen(transactions, viewModel, contentModifier)
            Tab.Transactions -> TransactionsScreen(transactions, viewModel, contentModifier)
            Tab.Settings -> SettingsScreen(viewModel, contentModifier)
        }
    }
}
