package com.yasin.playora

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yasin.playora.player.PlaybackViewModel
import com.yasin.playora.ui.music.EqualizerScreen
import com.yasin.playora.ui.music.MusicListScreen
import com.yasin.playora.ui.music.PlaylistDetailScreen
import com.yasin.playora.ui.music.QueueScreen
import com.yasin.playora.ui.photos.PhotoEditorScreen
import com.yasin.playora.ui.photos.PhotoGridScreen
import com.yasin.playora.ui.photos.PhotoViewModel
import com.yasin.playora.ui.photos.PhotoViewerScreen
import com.yasin.playora.ui.theme.PlayoraTheme
import com.yasin.playora.ui.video.VideoListScreen
import com.yasin.playora.ui.video.VideoPlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlayoraTheme {
                PlayoraApp()
            }
        }
    }
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab("video", R.string.tab_video, Icons.Filled.Movie),
    Tab("music", R.string.tab_music, Icons.Filled.MusicNote),
    Tab("photos", R.string.tab_photos, Icons.Filled.PhotoLibrary)
)

@Composable
private fun PlayoraApp() {
    val navController = rememberNavController()
    // One playback controller shared by every music screen, so music keeps
    // playing while navigating between lists, playlists and the queue.
    val playbackViewModel: PlaybackViewModel = viewModel()
    // One photo list shared by the grid, the viewer and the editor.
    val photoViewModel: PhotoViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // The bottom bar shows only on the three main tabs.
    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        tabs.any { it.route == dest.route }
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "video",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("video") {
                VideoListScreen(
                    onVideoClick = { video ->
                        navController.navigate(
                            "video_player?uri=${Uri.encode(video.uri)}" +
                                "&title=${Uri.encode(video.displayName)}"
                        )
                    }
                )
            }
            composable(
                route = "video_player?uri={uri}&title={title}",
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("uri") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: ""
                VideoPlayerScreen(uri = uri, title = title)
            }
            composable("music") {
                MusicListScreen(
                    viewModel = playbackViewModel,
                    onPlaylistClick = { id -> navController.navigate("playlist/$id") },
                    onQueueClick = { navController.navigate("queue") },
                    onEqualizerClick = { navController.navigate("equalizer") }
                )
            }
            composable(
                route = "playlist/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                PlaylistDetailScreen(
                    playlistId = id,
                    viewModel = playbackViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("queue") {
                QueueScreen(
                    viewModel = playbackViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("equalizer") {
                EqualizerScreen(onBack = { navController.popBackStack() })
            }
            composable("photos") {
                PhotoGridScreen(
                    viewModel = photoViewModel,
                    onPhotoClick = { index -> navController.navigate("photo_viewer?index=$index") }
                )
            }
            composable(
                route = "photo_viewer?index={index}",
                arguments = listOf(
                    navArgument("index") { type = NavType.IntType; defaultValue = 0 }
                )
            ) { backStackEntry ->
                val index = backStackEntry.arguments?.getInt("index") ?: 0
                PhotoViewerScreen(
                    viewModel = photoViewModel,
                    startIndex = index,
                    onBack = { navController.popBackStack() },
                    onEdit = { uri ->
                        navController.navigate("photo_editor?uri=${Uri.encode(uri)}")
                    }
                )
            }
            composable(
                route = "photo_editor?uri={uri}",
                arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("uri") ?: ""
                PhotoEditorScreen(uri = uri, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
