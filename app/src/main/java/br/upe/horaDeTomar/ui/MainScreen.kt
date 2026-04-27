@file:OptIn(ExperimentalMaterial3Api::class)

package br.upe.horaDeTomar.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.input.nestedscroll.nestedScroll
import br.upe.horaDeTomar.navigation.BottomBarNav
import br.upe.horaDeTomar.navigation.TopLevelsDestinations
import br.upe.horaDeTomar.ui.components.HeaderSection
import br.upe.horaDeTomar.ui.homePage.HomePageScreen
import br.upe.horaDeTomar.ui.medications.MedicationsScreen
import br.upe.horaDeTomar.ui.medications.RegisterMedicineScreen
import br.upe.horaDeTomar.ui.reminders.RemindersScreen
import br.upe.horaDeTomar.ui.settings.SettingsScreen
import br.upe.horaDeTomar.ui.users.UserRegisterScreen
import br.upe.horaDeTomar.ui.users.UsersScreen

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MainScreen(viewModel: AccountViewModel = hiltViewModel()) {
    val startup by viewModel.startup.collectAsStateWithLifecycle()

    when (val s = startup) {
        StartupState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is StartupState.Ready -> {
            val hasAccount = s.hasAccount
            val userName = s.firstAccountName.orEmpty()

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route.orEmpty()

            val hideBarsOn = setOf("registerUser", "registerMedication")
            val topLevelRoutes = TopLevelsDestinations.bottomNavItems.map { it.route }.toSet()

            val showTopBar = currentRoute !in hideBarsOn
            val showBottomBar = currentRoute in topLevelRoutes

            val isHome = currentRoute == TopLevelsDestinations.Home.route
            val topBarState = rememberTopAppBarState()
            val scrollBehavior = if (isHome) {
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
            } else {
                TopAppBarDefaults.pinnedScrollBehavior(topBarState)
            }

            LaunchedEffect(currentRoute) {
                topBarState.heightOffset = 0f
                topBarState.contentOffset = 0f
            }

            Scaffold(
                modifier = if (showTopBar) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                },
                topBar = {
                    if (showTopBar) {
                        HeaderSection(
                            navController = navController,
                            userName = userName,
                            scrollBehavior = scrollBehavior
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        BottomBarNav(
                            navController = navController,
                            topLevelRoutes = TopLevelsDestinations.bottomNavItems
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = if (hasAccount) TopLevelsDestinations.Home.route else "registerUser",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(220)
                        ) + fadeIn(tween(180))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(220)
                        ) + fadeOut(tween(180))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(220)
                        ) + fadeIn(tween(180))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(220)
                        ) + fadeOut(tween(180))
                    }
                ) {
                    composable(TopLevelsDestinations.Home.route) {
                        HomePageScreen(navController = navController)
                    }
                    composable(TopLevelsDestinations.Medications.route) {
                        MedicationsScreen(navController = navController)
                    }
                    composable(TopLevelsDestinations.Reminders.route) {
                        RemindersScreen(navController = navController)
                    }
                    composable(TopLevelsDestinations.Users.route) {
                        UsersScreen(navController = navController)
                    }
                    composable(TopLevelsDestinations.Settings.route) {
                        SettingsScreen()
                    }
                    composable(
                        route = "registerMedication?prescriptionId={prescriptionId}",
                        arguments = listOf(
                            androidx.navigation.navArgument("prescriptionId") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                            }
                        )
                    ) { backStackEntry ->
                        val pIdString = backStackEntry.arguments?.getString("prescriptionId")
                        val prescriptionId = pIdString?.toIntOrNull()

                        RegisterMedicineScreen(
                            navControler = navController,
                            prescriptionId = prescriptionId
                        )
                    }
                    composable("registerUser") {
                        UserRegisterScreen(
                            onUserRegistered = {
                                navController.navigate(TopLevelsDestinations.Home.route) {
                                    popUpTo("registerUser") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navController = navController,
                            isFirstTime = !hasAccount
                        )
                    }
                    composable("editMedication/{medicationId}") { }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true, showSystemUi = true, apiLevel = 35)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
