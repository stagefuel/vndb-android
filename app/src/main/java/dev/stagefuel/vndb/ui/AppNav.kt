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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.ui.update.UpdateDialog
import dev.stagefuel.vndb.ui.update.UpdateVm
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import dev.stagefuel.vndb.ui.common.ImageViewer
import dev.stagefuel.vndb.ui.common.ViewerImage

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
  Tab(Routes.SEARCH, "Search", Icons.Default.Search),
  Tab(Routes.LIST, "My list", Icons.AutoMirrored.Filled.List),
  Tab(Routes.FORUMS, "Forums", Icons.Default.Forum),
  Tab(Routes.SETTINGS, "Me", Icons.Default.Person),
)

/** Navigation callbacks handed to every screen. */
class Nav(
  private val nav: NavHostController,
  private val external: (String) -> Unit,
  /** Opens the full-screen image viewer. */
  val images: (List<ViewerImage>, Int) -> Unit = { _, _ -> },
) {
  fun vn(id: String) = nav.navigate(Routes.vn(id))
  fun item(id: String) = nav.navigate(Routes.item(id))
  fun userList(uid: String, name: String) = nav.navigate(Routes.userList(uid, name))
  fun back() = nav.popBackStack()
  fun web(url: String) = nav.navigate(Routes.web(url))
  fun image(url: String) = images(listOf(ViewerImage(dev.stagefuel.vndb.api.Image(url = url), "")), 0)
  fun open(url: String) {
    if (Routes.isImage(url)) return image(url)
    when (val t = Routes.resolve(url)) {
      is Routes.InApp -> nav.navigate(t.route)
      is Routes.External -> external(t.url)
    }
  }
}

@Composable
fun AppNav(app: VndbApp, prefs: Prefs, startUrl: String?, onStartUrlUsed: () -> Unit, openExternal: (String) -> Unit) {
  val nav = rememberNavController()
  var viewer by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Pair<List<ViewerImage>, Int>?>(null) }
  val n = androidx.compose.runtime.remember(nav) { Nav(nav, openExternal) { list, i -> viewer = list to i } }
  viewer?.let { (list, i) -> ImageViewer(list, i, prefs) { viewer = null } }
  val skin = LocalSkin.current
  LaunchedEffect(startUrl) { startUrl?.let { n.open(it); onStartUrlUsed() } }
  // The tab whose stack is showing, so it stays highlighted on its sub-pages and a second tap returns to its top.
  var activeTab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(Routes.SEARCH) }
  // System back can land on another tab's page (e.g. Me → back → Search): follow it.
  val route = nav.currentBackStackEntryAsState().value?.destination?.route
  LaunchedEffect(route) { if (route != null && tabs.any { it.route == route }) activeTab = route }
  val updates = viewModel { UpdateVm(app.updater, app.settings) }
  LaunchedEffect(Unit) { if (VndbApp.checkUpdates) updates.check(manual = false) }
  UpdateDialog(updates)

  Scaffold(
    containerColor = skin.bodyBg,
    bottomBar = {
      NavigationBar(containerColor = skin.blendBg) {
        tabs.forEach { t ->
          NavigationBarItem(
            selected = activeTab == t.route,
            onClick = {
              if (activeTab == t.route) {
                // Already in this tab: drop its sub-pages (the tab's own page stays, with its state).
                if (!nav.popBackStack(t.route, inclusive = false)) nav.navigate(t.route) { launchSingleTop = true }
              } else {
                activeTab = t.route
                nav.navigate(t.route) {
                  popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
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
    // Edge-to-edge: the window isn't resized for the keyboard, so make room for it here (minus the
    // nav bar space Scaffold already reserves) — keeps bottom search boxes visible while typing.
    Column(Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding()) {
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
