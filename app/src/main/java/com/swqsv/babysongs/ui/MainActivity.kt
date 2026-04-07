package com.swqsv.babysongs.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swqsv.babysongs.ui.components.MiniPlayerBar
import com.swqsv.babysongs.ui.cyclePlayMode
import com.swqsv.babysongs.ui.navigation.AlbumNavKey
import com.swqsv.babysongs.ui.screens.AlbumListScreen
import com.swqsv.babysongs.ui.screens.NowPlayingScreen
import com.swqsv.babysongs.ui.screens.SongListScreen
import com.swqsv.babysongs.ui.screens.storagePermissionList
import com.swqsv.babysongs.ui.theme.BabySongsTheme
import com.swqsv.babysongs.ui.viewmodel.LibraryViewModel
import com.swqsv.babysongs.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabySongsTheme {
                val navController = rememberNavController()
                val libraryViewModel: LibraryViewModel = viewModel()
                val playerViewModel: PlayerViewModel = viewModel()

                val permissions = storagePermissionList()
                var granted by remember { mutableStateOf(false) }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    granted = result.values.all { it }
                }

                LaunchedEffect(Unit) {
                    launcher.launch(permissions)
                }

                val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
                val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showMiniPlayer =
                    playbackState.currentSong != null && currentRoute != "nowPlaying"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showMiniPlayer) {
                            MiniPlayerBar(
                                state = playbackState,
                                onPlayPause = { playerViewModel.playPause() },
                                onSkipNext = { playerViewModel.skipToNext() },
                                onSkipPrevious = { playerViewModel.skipToPrevious() },
                                onSeekToMs = { ms -> playerViewModel.seekTo(ms) },
                                onCyclePlayMode = {
                                    val next = cyclePlayMode(playbackState.playMode)
                                    playerViewModel.setPlayMode(next)
                                },
                            )
                        }
                    },
                ) { innerPadding ->
                val layoutDirection = LocalLayoutDirection.current
                // 仅消费左右与底部：避免与内层 TopAppBar 的状态栏 inset 叠加，消除顶部过大空白
                val navModifier = Modifier.padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding(),
                )
                NavHost(
                    navController = navController,
                    startDestination = "albums",
                    modifier = navModifier,
                ) {
                    composable("albums") {
                        AlbumListScreen(
                            viewModel = libraryViewModel,
                            permissionsGranted = granted,
                            onOpenAlbum = { album ->
                                val segment = AlbumNavKey.encode(album.id)
                                navController.navigate("songs/$segment")
                            },
                        )
                    }
                    composable(
                        route = "songs/{albumId}",
                        arguments = listOf(
                            navArgument("albumId") { type = NavType.StringType },
                        ),
                    ) { entry ->
                        val raw = entry.arguments?.getString("albumId") ?: return@composable
                        val id = AlbumNavKey.decode(raw) ?: raw
                        val album = libraryState.albums.firstOrNull { it.id == id }
                        if (album == null) {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                            return@composable
                        }
                        SongListScreen(
                            album = album,
                            libraryViewModel = libraryViewModel,
                            playerViewModel = playerViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenFullPlayer = {
                                navController.navigate("nowPlaying") {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable("nowPlaying") {
                        NowPlayingScreen(
                            playerViewModel = playerViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                }
            }
        }
    }
}
