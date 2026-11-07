package com.sheharsaathi.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import com.sheharsaathi.app.ui.components.LocalOpenProfile
import com.sheharsaathi.app.ui.components.LocalOpenWeather
import com.sheharsaathi.app.ui.screens.ProfileScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sheharsaathi.app.data.CityViewModel
import com.sheharsaathi.app.ui.screens.CivicReportsScreen
import com.sheharsaathi.app.ui.screens.CivicScreen
import com.sheharsaathi.app.ui.screens.EventDetailScreen
import com.sheharsaathi.app.ui.screens.EventsScreen
import com.sheharsaathi.app.ui.screens.ExploreScreen
import com.sheharsaathi.app.ui.screens.GoModeScreen
import com.sheharsaathi.app.ui.screens.PlannerScreen
import com.sheharsaathi.app.ui.screens.TransitBoardScreen
import com.sheharsaathi.app.ui.screens.IssueDetailScreen
import com.sheharsaathi.app.ui.screens.NewsScreen
import com.sheharsaathi.app.ui.screens.WeatherScreen
import com.sheharsaathi.app.ui.screens.ReportScreen
import com.sheharsaathi.app.ui.theme.CityColors

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("explore", "Explore", Icons.Outlined.LocationOn, Icons.Filled.LocationOn),
    Tab("news", "News Hub", Icons.Outlined.Newspaper, Icons.Filled.Newspaper),
    Tab("report", "Report", Icons.Outlined.AddCircleOutline, Icons.Filled.AddCircle),
    Tab("events", "Events", Icons.Outlined.Event, Icons.Filled.Event),
    Tab("civic", "Civic Hub", Icons.Outlined.AccountBalance, Icons.Filled.AccountBalance),
)

private val tabRoutes = tabs.map { it.route }.toSet()

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavigation(vm: CityViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // Planner and live board live under Explore and keep the tab bar visible.
    val exploreChildren = setOf("plan", "transit")
    val selectedTab = if (currentRoute in exploreChildren) "explore" else currentRoute
    val onTab = tabs.any { it.route == currentRoute } || currentRoute in exploreChildren

    Scaffold(
        containerColor = CityColors.Canvas,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (onTab) {
                NavigationBar(containerColor = CityColors.Surface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = selectedTab == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (currentRoute != tab.route) navController.switchTab(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CityColors.IndigoDeep,
                                selectedTextColor = CityColors.IndigoDeep,
                                indicatorColor = CityColors.IndigoSoft,
                                unselectedIconColor = CityColors.Muted,
                                unselectedTextColor = CityColors.Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
      CompositionLocalProvider(
          LocalOpenProfile provides { navController.navigate("profile") { launchSingleTop = true } },
          LocalOpenWeather provides { navController.navigate("weather") { launchSingleTop = true } },
      ) {
        NavHost(
            navController = navController,
            startDestination = "explore",
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
            enterTransition = {
                val tabSwitch = initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes
                if (tabSwitch) fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.985f)
                else slideInHorizontally(tween(340)) { it / 3 } + fadeIn(tween(300))
            },
            exitTransition = {
                val tabSwitch = initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes
                if (tabSwitch) fadeOut(tween(160)) else slideOutHorizontally(tween(340)) { -it / 6 } + fadeOut(tween(260))
            },
            popEnterTransition = { slideInHorizontally(tween(320)) { -it / 6 } + fadeIn(tween(280)) },
            popExitTransition = { slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(240)) },
        ) {
            composable("weather") { WeatherScreen(vm = vm, onBack = { navController.popBackStack() }) }
            composable("profile") {
                ProfileScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenEvent = { navController.navigate("event/$it") },
                )
            }
            composable("explore") {
                ExploreScreen(
                    vm = vm,
                    onOpenEvent = { navController.navigate("event/$it") },
                    onOpenIssue = { navController.navigate("issue/$it") },
                    onSeeAllEvents = { navController.switchTab("events") },
                    onSeeAllIssues = { navController.navigate("reports?mine=false") },
                    onPlanTrip = { navController.navigate("plan") { launchSingleTop = true } },
                    onOpenBoard = { navController.navigate("transit") { launchSingleTop = true } },
                )
            }
            composable("plan") {
                PlannerScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onStartGo = { navController.navigate("go") },
                    onOpenBoard = { navController.navigate("transit") { launchSingleTop = true } },
                )
            }
            composable("transit") {
                TransitBoardScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onPlan = {
                        if (!navController.popBackStack("plan", inclusive = false)) {
                            vm.tripTo = null
                            navController.navigate("plan")
                        }
                    },
                )
            }
            composable("go") {
                GoModeScreen(
                    vm = vm,
                    onEnd = {
                        if (!navController.popBackStack("explore", inclusive = false)) navController.popBackStack()
                    },
                    onReroute = { navController.popBackStack() },
                    onReport = { navController.navigate("report") },
                    onOpenBoard = { navController.navigate("transit") { launchSingleTop = true } },
                )
            }
            composable("news") { NewsScreen(vm) }
            composable("report") {
                ReportScreen(vm) { id ->
                    navController.navigate("issue/$id")
                }
            }
            composable("events") {
                EventsScreen(vm) { navController.navigate("event/$it") }
            }
            composable("civic") {
                CivicScreen(vm) { mine -> navController.navigate("reports?mine=$mine") }
            }
            composable("event/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                EventDetailScreen(
                    vm, it.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onPlanTrip = { navController.navigate("plan") },
                )
            }
            composable("issue/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                IssueDetailScreen(vm, it.arguments?.getString("id").orEmpty()) { navController.popBackStack() }
            }
            composable(
                "reports?mine={mine}",
                arguments = listOf(navArgument("mine") { type = NavType.BoolType; defaultValue = false }),
            ) {
                CivicReportsScreen(
                    vm = vm,
                    startOnMine = it.arguments?.getBoolean("mine") ?: false,
                    onBack = { navController.popBackStack() },
                    onOpenIssue = { id -> navController.navigate("issue/$id") },
                )
            }
        }
      }
    }

    if (vm.showLocationPicker) {
        com.sheharsaathi.app.ui.components.LocationPickerSheet(vm)
    }
}
