package nl.hva.huecolors

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import kotlinx.coroutines.launch
import nl.hva.huecolors.data.Resource
import nl.hva.huecolors.data.model.NavItem
import nl.hva.huecolors.ui.components.HueInfoCard
import nl.hva.huecolors.ui.screens.Screens
import nl.hva.huecolors.ui.screens.app.CameraScreen
import nl.hva.huecolors.ui.screens.app.LibraryScreen
import nl.hva.huecolors.ui.screens.app.LightsScreen
import nl.hva.huecolors.ui.screens.app.SettingsScreen
import nl.hva.huecolors.ui.screens.bridge.InteractScreen
import nl.hva.huecolors.ui.screens.bridge.IpScreen
import nl.hva.huecolors.ui.screens.bridge.ListScreen
import nl.hva.huecolors.ui.screens.bridge.ScanScreen
import nl.hva.huecolors.ui.theme.HueColorsTheme
import nl.hva.huecolors.utils.Utils
import nl.hva.huecolors.viewmodel.BridgeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Transparent.toArgb()
            )
        )

        super.onCreate(savedInstanceState)

        setContent {
            HueColorsTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val wifiState by Utils.rememberWifiEnabledState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        HueColorsApp()
                        AnimatedVisibility(
                            visible = !wifiState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .clickable { }
                                .background(MaterialTheme.colorScheme.scrim.copy(0.9F))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp, vertical = 48.dp),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    HueInfoCard(headline = getString(R.string.enable_wi_fi), body = getString(R.string.wifi_on_description))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HueColorsApp() {
    val navController = rememberNavController()
    HueNavHost(navController)
}

@Composable
fun HueNavHost(navController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    val viewModel: BridgeViewModel = viewModel()
    val isBridgeAuthorized by viewModel.isBridgeAuthorized.observeAsState()
    val navEntry by navController.currentBackStackEntryAsState()
    val startDestination =
        rememberUpdatedState(if (isBridgeAuthorized?.data == true) Screens.App.route else Screens.Bridge.route)

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            viewModel.isBridgeAuthorized()
        }
    }

    if ((isBridgeAuthorized is Resource.Success)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface, bottomBar = {
                if (navEntry?.destination?.route in Screens.App.getAllRoutes()) {
                    BottomNavBar(navController)
                }
            }, modifier = Modifier.fillMaxSize()
        ) { padding ->
            Column(
                Modifier.fillMaxSize()
            ) {
                NavHost(
                    navController = navController, startDestination = startDestination.value
                ) {
                    BridgeGraph<BridgeViewModel>(navController)
                    AppGraph<BridgeViewModel>(navController, padding)
                }
            }
        }
    }
}

@SuppressLint("ResourceType")
@Composable
fun BottomNavBar(navController: NavHostController) {
    val navEntry by navController.currentBackStackEntryAsState()
    val navigationItems = listOf(
        NavItem(Screens.App.Library, R.drawable.ic_folder, "Library"),
        NavItem(Screens.App.Lights, R.drawable.ic_bulb, "Lights"),
        NavItem(Screens.App.Settings, R.drawable.ic_settings, "Settings"),
        NavItem(Screens.App.Camera, R.drawable.ic_camera, "Camera"),
    )
    val navigateTo = { route: String ->
        navController.navigate(route) {
//            popUpTo(navController.graph.findStartDestination().id) {
//                saveState = true
//            }
            popUpTo(0)
        }
    }

    NavigationBar(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F0F0F), MaterialTheme.colorScheme.scrim
                    )
                )
            )
            .height(92.dp),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            navigationItems.forEach { item ->
                BottomNavBarItem(
                    active = navEntry?.destination?.route == item.route.route,
                    painter = item.painter,
                    label = item.label,
                    modifier = Modifier.weight(1 / 4F)
                ) {
                    navigateTo(item.route.route)
                }
            }
        }
    }
}

/**
 * Displays a bottom navigation bar item with an icon and label
 *
 * @param active Determines whether the item is currently active.
 * @param painter The resource ID of the icon to be displayed.
 * @param label The text label for the item.
 * @param modifier Modifier for customization of the item's appearance.
 * @param onClick An optional lambda function to run when the item is
 *     clicked.
 */
@Composable
fun BottomNavBarItem(
    active: Boolean,
    painter: Int,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(targetValue = if (active) 1F else 0F, label = "")

    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp),
                painter = painterResource(id = painter),
                contentDescription = "Bridge",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                modifier = Modifier.alpha(alpha),
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp
                ),
                letterSpacing = 2.sp
            )
        }
    }
}

/**
 * Gets a shared ViewModel of type T associated with the parent route / nav
 * entry
 *
 * @param navController The NavController used for navigation.
 * @return Shared ViewModel of type T
 */
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavController): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }

    return viewModel(parentEntry)
}

/**
 * Sets up a navigation graph for the Bridge flow screens.
 *
 * @param navController The NavController used for navigation.
 */
inline fun <reified T : ViewModel> NavGraphBuilder.BridgeGraph(navController: NavHostController) {
    navigation(
        startDestination = Screens.Bridge.Scan.route, route = Screens.Bridge.route
    ) {
        composable(Screens.Bridge.Scan.route) {
            ScanScreen(navController = navController, it.sharedViewModel(navController))
        }

        composable(Screens.Bridge.Ip.route) {
            IpScreen(navController = navController, it.sharedViewModel(navController))
        }

        composable(Screens.Bridge.List.route) {
            ListScreen(navController = navController, it.sharedViewModel(navController))
        }
        composable(Screens.Bridge.Interact.route) {
            InteractScreen(navController = navController, it.sharedViewModel(navController))
        }
    }
}

/**
 * Sets up a navigation graph for the App flow screens.
 *
 * @param navController The NavController used for navigation.
 */
@RequiresApi(Build.VERSION_CODES.S)
inline fun <reified T : ViewModel> NavGraphBuilder.AppGraph(
    navController: NavHostController,
    padding: PaddingValues
) {
    navigation(
        startDestination = Screens.App.Lights.route, route = Screens.App.route
    ) {
        composable(Screens.App.Lights.route) {
            LightsScreen(navController = navController, it.sharedViewModel(navController), padding)
        }
        composable(Screens.App.Library.route) {
            LibraryScreen(navController = navController, it.sharedViewModel(navController), padding)
        }
        composable(Screens.App.Settings.route) {
            SettingsScreen(navController = navController, it.sharedViewModel(navController))
        }
        composable(Screens.App.Camera.route) {
            CameraScreen(navController = navController, it.sharedViewModel(navController), padding)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HueColorsPreview() {
    HueColorsTheme(darkTheme = true) {
        HueColorsApp()
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    HueColorsTheme(darkTheme = true) {
        BottomNavBar(rememberNavController())
    }
}