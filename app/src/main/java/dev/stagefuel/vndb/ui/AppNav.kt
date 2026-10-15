package dev.stagefuel.vndb.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.ui.update.UpdateDialog
import dev.stagefuel.vndb.ui.update.UpdateVm
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.ui.item.ItemScreen
import dev.stagefuel.vndb.ui.list.MyListScreen
import dev.stagefuel.vndb.ui.search.SearchScreen
import dev.stagefuel.vndb.ui.settings.SettingsScreen
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import dev.stagefuel.vndb.ui.vn.VnScreen
import dev.stagefuel.vndb.ui.web.WebScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
  Tab(Routes.SEARCH, "Search", Icons.Default.Search),
  Tab(Routes.LIST, "My list", Icons.AutoMirrored.Filled.List),
  Tab(Routes.FORUMS, "Forums", Icons.Default.Forum),
  Tab(Routes.SETTINGS, "Me", Icons.Default.Person),
)

/** Navigation callbacks handed to every screen. */
class Nav(private val nav: NavHostController, private val external: (String) -> Unit) {
  fun vn(id: String) = nav.navigate(Routes.vn(id))
  fun item(id: String) = nav.navigate(Routes.item(id))
  fun userList(uid: String, name: String) = nav.navigate(Routes.userList(uid, name))
  fun back() = nav.popBackStack()
  fun web(url: String) = nav.navigate(Routes.web(url))
  fun open(url: String) = when (val t = Routes.resolve(url)) {
    is Routes.InApp -> nav.navigate(t.route)
    is Routes.External -> external(t.url)
  }
}

@Composable
fun AppNav(app: VndbApp, prefs: Prefs, startUrl: String?, onStartUrlUsed: () -> Unit, openExternal: (String) -> Unit) {
  val nav = rememberNavController()
  val n = Nav(nav, openExternal)
  val skin = LocalSkin.current
  LaunchedEffect(startUrl) { startUrl?.let { n.open(it); onStartUrlUsed() } }
  val current by nav.currentBackStackEntryAsState()
  val updates = viewModel { UpdateVm(app.updater, app.settings) }
  LaunchedEffect(Unit) { if (VndbApp.checkUpdates) updates.check(manual = false) }
  UpdateDialog(updates)

  Scaffold(
    containerColor = skin.bodyBg,
    bottomBar = {
      NavigationBar(containerColor = skin.blendBg) {
        tabs.forEach { t ->
          val selected = current?.destination?.route == t.route
          NavigationBarItem(
            selected = selected,
            onClick = {
              nav.navigate(t.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = { Icon(t.icon, null) },
            label = { Text(t.label, style = VndbType.small) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = skin.mainText, selectedTextColor = skin.mainText,
              unselectedIconColor = skin.grayedOut, unselectedTextColor = skin.grayedOut,
              indicatorColor = skin.tabBg,
            ),
          )
        }
      }
    },
  ) { pad ->
    Column(Modifier.fillMaxSize().padding(pad)) {
      NavHost(nav, startDestination = Routes.SEARCH) {
        composable(Routes.SEARCH) { SearchScreen(app, prefs, n) }
        composable(Routes.LIST) { MyListScreen(app, prefs, n) }
        composable(Routes.FORUMS) { WebScreen("https://vndb.org/t", n) }
        composable(Routes.SETTINGS) { SettingsScreen(app, prefs, n, updates) }
        composable(Routes.VN, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
          VnScreen(app, prefs, n, e.arguments?.getString("id").orEmpty())
        }
        composable(Routes.ITEM, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
          ItemScreen(app, prefs, n, e.arguments?.getString("id").orEmpty())
        }
        composable(Routes.USER_LIST, arguments = listOf(navArgument("uid") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType; nullable = true })) { e ->
          MyListScreen(app, prefs, n, e.arguments?.getString("uid"), e.arguments?.getString("name"))
        }
        composable(Routes.WEB, arguments = listOf(navArgument("url") { type = NavType.StringType })) { e ->
          WebScreen(e.arguments?.getString("url").orEmpty(), n)
        }
      }
    }
  }
}
