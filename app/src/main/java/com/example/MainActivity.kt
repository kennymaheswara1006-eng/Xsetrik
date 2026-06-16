package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.GamerGold
import com.example.ui.theme.MatrixDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

// -------------------------------------------------------------
// DATA MODELS
// -------------------------------------------------------------

enum class SpeedMode {
    SILENT, BALANCED, X_MODE
}

data class SystemTelemetry(
    val cpuUsage: Int = 30,
    val gpuUsage: Int = 20,
    val cpuFrequencyGhz: Float = 2.4f,
    val gpuFrequencyMhz: Int = 450,
    val ramPercentage: Float = 60.0f,
    val ramUsedMb: Float = 3600f,
    val ramTotalMb: Float = 6000f,
    val batteryTempCelsius: Float = 34.5f,
    val networkPingMs: Int = 28,
    val fpsEstimate: Int = 60,
    val historyCpu: List<Float> = List(15) { 20f + (it * 3) % 40f },
    val historyRam: List<Float> = List(15) { 55f + (it * 2) % 15f }
)

data class GameProfile(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val developer: String = "Global Launch",
    val packageId: String,
    val customAccent: Color = NeonRed,
    val launchCount: Int = 0,
    val fpsPerformanceTarget: Int = 90
)

data class BoostTerminalLog(
    val timestamp: String,
    val message: String,
    val isAlert: Boolean = false
)

// -------------------------------------------------------------
// VIEW MODEL (THE CONSOLE GAME BOOSTER STATE CORE)
// -------------------------------------------------------------
@Keep
class XStrikeViewModel : ViewModel() {

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _currentMode = MutableStateFlow(SpeedMode.BALANCED)
    val currentMode: StateFlow<SpeedMode> = _currentMode.asStateFlow()

    private val _gameLibrary = MutableStateFlow<List<GameProfile>>(emptyList())
    val gameLibrary: StateFlow<List<GameProfile>> = _gameLibrary.asStateFlow()

    // Boosting System Overlay state
    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting.asStateFlow()

    private val _boostProgress = MutableStateFlow(0f)
    val boostProgress: StateFlow<Float> = _boostProgress.asStateFlow()

    val boostTerminalLogs = mutableStateListOf<BoostTerminalLog>()

    // Modal success readout
    private val _showBoostSuccessReport = MutableStateFlow(false)
    val showBoostSuccessReport: StateFlow<Boolean> = _showBoostSuccessReport.asStateFlow()

    private val _lastSavedRamMb = MutableStateFlow(0)
    val lastSavedRamMb: StateFlow<Int> = _lastSavedRamMb.asStateFlow()

    init {
        // Pre-populate with typical top high-intensity gaming titles
        _gameLibrary.value = listOf(
            GameProfile(title = "Genshin Impact", developer = "COGNOSPHERE", packageId = "com.miHoYo.GenshinImpact", customAccent = NeonRed, launchCount = 14, fpsPerformanceTarget = 60),
            GameProfile(title = "PUBG MOBILE", developer = "Tencent Games", packageId = "com.tencent.ig", customAccent = NeonCyan, launchCount = 28, fpsPerformanceTarget = 90),
            GameProfile(title = "Mobile Legends", developer = "Moonton", packageId = "com.mobile.legends", customAccent = NeonPurple, launchCount = 42, fpsPerformanceTarget = 120),
            GameProfile(title = "Arena of Valor", developer = "Level Infinite", packageId = "com.garena.game.kgid", customAccent = GamerGold, launchCount = 8, fpsPerformanceTarget = 90)
        )

        // Continuous Background Telemetry Monitor Ticker
        viewModelScope.launch {
            var counter = 0
            while (isActive) {
                counter++
                delay(1200)
                tickTelemetrySimulation(counter)
            }
        }
    }

    fun setSpeedMode(mode: SpeedMode) {
        _currentMode.value = mode
        // Forcing telemetry jump to represent visual mode difference immediately
        _telemetry.update { current ->
            when (mode) {
                SpeedMode.SILENT -> current.copy(
                    cpuUsage = 15,
                    gpuUsage = 12,
                    cpuFrequencyGhz = 1.32f,
                    gpuFrequencyMhz = 280,
                    fpsEstimate = 60,
                    networkPingMs = 38
                )
                SpeedMode.BALANCED -> current.copy(
                    cpuUsage = 34,
                    gpuUsage = 28,
                    cpuFrequencyGhz = 2.42f,
                    gpuFrequencyMhz = 512,
                    fpsEstimate = 90,
                    networkPingMs = 24
                )
                SpeedMode.X_MODE -> current.copy(
                    cpuUsage = 78,
                    gpuUsage = 64,
                    cpuFrequencyGhz = 3.20f,
                    gpuFrequencyMhz = 850,
                    fpsEstimate = 120,
                    networkPingMs = 14
                )
            }
        }
    }

    // Connect physical Android hardware readings with our internal state
    fun updateHardwareData(totalMb: Float, availMb: Float, batteryTemp: Float?) {
        val usedRamMb = totalMb - availMb
        val ramUsagePercentage = (usedRamMb / totalMb) * 100f
        
        _telemetry.update { current ->
            current.copy(
                ramTotalMb = totalMb,
                ramUsedMb = usedRamMb,
                ramPercentage = ramUsagePercentage,
                batteryTempCelsius = batteryTemp ?: current.batteryTempCelsius
            )
        }
    }

    private fun tickTelemetrySimulation(tickId: Int) {
        val mode = _currentMode.value
        _telemetry.update { current ->
            // CPU load fluctuates around performance profiles
            val cpuTarget = when (mode) {
                SpeedMode.SILENT -> (12..20).random()
                SpeedMode.BALANCED -> (28..45).random()
                SpeedMode.X_MODE -> (75..95).random()
            }
            val gpuTarget = when (mode) {
                SpeedMode.SILENT -> (10..18).random()
                SpeedMode.BALANCED -> (20..38).random()
                SpeedMode.X_MODE -> (60..86).random()
            }

            // CPU Frequency simulator
            val cpuFreq = when (mode) {
                SpeedMode.SILENT -> 1.2f + (tickId % 3) * 0.05f
                SpeedMode.BALANCED -> 2.2f + (tickId % 4) * 0.08f
                SpeedMode.X_MODE -> 3.0f + (tickId % 5) * 0.06f
            }

            // GPU Clock simulator
            val gpuFreq = when (mode) {
                SpeedMode.SILENT -> 250 + (tickId % 2) * 15
                SpeedMode.BALANCED -> 480 + (tickId % 4) * 20
                SpeedMode.X_MODE -> 820 + (tickId % 3) * 25
            }

            // Ping Simulator with mode optimization jitter
            val pingBase = when (mode) {
                SpeedMode.SILENT -> 36
                SpeedMode.BALANCED -> 22
                SpeedMode.X_MODE -> 11
            }
            val finalPing = pingBase + (tickId % 5)

            // FPS frame pacing dynamic output
            val fpsVal = when (mode) {
                SpeedMode.SILENT -> 58 + (tickId % 3 - 1)
                SpeedMode.BALANCED -> 88 + (tickId % 3 - 1)
                SpeedMode.X_MODE -> 118 + (tickId % 3 - 1)
            }

            // Thermal gradual heat simulation for X-Mode
            val thermalBase = current.batteryTempCelsius
            val finalThermal = when (mode) {
                SpeedMode.SILENT -> if (thermalBase > 32f) thermalBase - 0.1f else 32.0f
                SpeedMode.BALANCED -> if (thermalBase > 34.5f) thermalBase - 0.1f else if (thermalBase < 34.5f) thermalBase + 0.1f else 34.5f
                SpeedMode.X_MODE -> if (thermalBase < 40.8f) thermalBase + 0.15f else 40.8f
            }

            // History trackers (Keep last 15 ticks)
            val updatedHistoryCpu = (current.historyCpu.drop(1) + cpuTarget.toFloat())
            val updatedHistoryRam = (current.historyRam.drop(1) + current.ramPercentage)

            current.copy(
                cpuUsage = cpuTarget,
                gpuUsage = gpuTarget,
                cpuFrequencyGhz = String.format("%.2f", cpuFreq).replace(",", ".").toFloat(),
                gpuFrequencyMhz = gpuFreq,
                batteryTempCelsius = String.format("%.1f", finalThermal).replace(",", ".").toFloat(),
                networkPingMs = finalPing,
                fpsEstimate = fpsVal,
                historyCpu = updatedHistoryCpu,
                historyRam = updatedHistoryRam
            )
        }
    }

    // Trigger full system-memory flush and multi-core clock overclock boost sequence
    fun executeBoost(gameToLaunchAfterBoost: GameProfile? = null) {
        if (_isBoosting.value) return
        
        _isBoosting.value = true
        _boostProgress.value = 0f
        boostTerminalLogs.clear()

        // Emulate terminal matrix diagnostic logs during boost process
        viewModelScope.launch {
            val steps = listOf(
                "CONNECTING TO X-STRIKE OPTIMIZER COCKPIT..." to false,
                "INITIALIZING DEVICE HARDWARE INTERFACES..." to false,
                "INTERCEPTING INACTIVE ALLOCATED MEMORY PILLS..." to false,
                "FLUSHING SYSTEM HEAP CACHES & RAM CONVOLUTION..." to true,
                "RE-ROUTING MULTI-CORE WORKLOADS FOR CONCURRENCY..." to false,
                "ADJUSTING REFRESH-RATE TO MAXIMUM POTENTIAL HZ..." to false,
                "OVERCLOCKING EXTREME CORES (X-BOOST TUNED)..." to true,
                "STABILIZING GRAPHIC FREQUENCY ENGINE FRAME PACING..." to false,
                "OPTIMIZING TCP/IP NETWORK CHANNELS (ANTI-JITTER)..." to false,
                "X-STRIKE GAME ENGINE CALIBRATED!" to true
            )

            for (i in steps.indices) {
                val stepData = steps[i]
                delay(220)
                _boostProgress.value = ((i + 1).toFloat() / steps.size)
                boostTerminalLogs.add(
                    BoostTerminalLog(
                        timestamp = String.format("0%d:%02d", (i * 2) / 60, (i * 12) % 60),
                        message = stepData.first,
                        isAlert = stepData.second
                    )
                )
            }

            // Clean RAM simulation reduction
            val ramRegained = (380..760).random()
            _lastSavedRamMb.value = ramRegained

            _telemetry.update { curr ->
                val newPercentage = (curr.ramPercentage - 12f).coerceAtLeast(35f)
                val newUsedMb = (curr.ramUsedMb - ramRegained).coerceAtLeast(2000f)
                curr.copy(
                    ramPercentage = newPercentage,
                    ramUsedMb = newUsedMb,
                    fpsEstimate = if (_currentMode.value == SpeedMode.X_MODE) 120 else 90,
                    networkPingMs = (curr.networkPingMs - 4).coerceAtLeast(10)
                )
            }

            // If we launched memory booster for a game, raise launch statistics!
            if (gameToLaunchAfterBoost != null) {
                _gameLibrary.update { list ->
                    list.map { g ->
                        if (g.id == gameToLaunchAfterBoost.id) {
                            g.copy(launchCount = g.launchCount + 1)
                        } else g
                    }
                }
            }

            delay(400)
            _isBoosting.value = false
            _showBoostSuccessReport.value = true
        }
    }

    fun dismissSuccessReport() {
        _showBoostSuccessReport.value = false
    }

    fun addCustomGame(title: String, targetFps: Int, rawColorIndex: Int) {
        val colors = listOf(NeonRed, NeonCyan, NeonPurple, GamerGold, SystemGreen)
        val selectedAccent = colors[rawColorIndex.coerceIn(0, colors.size - 1)]
        val formattedPackage = "com.strike.custom." + title.lowercase().replace(" ", "")

        val newGame = GameProfile(
            title = title,
            packageId = formattedPackage,
            customAccent = selectedAccent,
            launchCount = 0,
            fpsPerformanceTarget = targetFps
        )

        _gameLibrary.update { currentList ->
            currentList + newGame
        }
    }

    fun removeGame(id: String) {
        _gameLibrary.update { list ->
            list.filter { it.id != id }
        }
    }
}


// -------------------------------------------------------------
// MAIN ENTRY POINT ACTIVITY
// -------------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    XStrikeScreenFrame(
                        modifier = Modifier.padding(innerPadding),
                        onTriggerHaptic = { strength ->
                            triggerTactileVibration(strength)
                        }
                    )
                }
            }
        }
    }

    private fun triggerTactileVibration(strengthMs: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (strengthMs > 200) {
                        VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 200), -1)
                    } else {
                        VibrationEffect.createOneShot(strengthMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(strengthMs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


// -------------------------------------------------------------
// MAIN INTERACTIVE COMPOSE VIEW LAYOUT
// -------------------------------------------------------------
@Composable
fun XStrikeScreenFrame(
    modifier: Modifier = Modifier,
    onTriggerHaptic: (Long) -> Unit = {},
    viewModel: XStrikeViewModel = viewModel()
) {
    val telemetryState by viewModel.telemetry.collectAsState()
    val currentModeState by viewModel.currentMode.collectAsState()
    val gameLibraryList by viewModel.gameLibrary.collectAsState()
    val isBoostingActive by viewModel.isBoosting.collectAsState()
    val boostProgressValue by viewModel.boostProgress.collectAsState()
    val showSuccessReportState by viewModel.showBoostSuccessReport.collectAsState()
    val savedRamMbVal by viewModel.lastSavedRamMb.collectAsState()

    var activeTab by remember { mutableStateOf("dashboard") } // "dashboard", "library", "charts"
    var showAddGameSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Periodical physical system readings binding hook
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                // 1. Read genuine hardware RAM parameters
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val availMb = memInfo.availMem / (1024 * 1024).toFloat()
                val totalMb = memInfo.totalMem / (1024 * 1024).toFloat()

                // 2. Read genuine hardware thermal battery sensor
                val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val statusIntent = context.registerReceiver(null, batteryFilter)
                val tempRaw = statusIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                val batteryTemp = if (tempRaw > 0) tempRaw / 10.0f else null

                viewModel.updateHardwareData(totalMb, availMb, batteryTemp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(1800)
        }
    }

    // Outer gamer aesthetic wallpaper matrix container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .drawBehind {
                // Immersive subtle background gradient (from red-500/5 to transparent)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x0CFF1A40), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.4f
                    )
                )

                // Immersive UI Hexagon-style radial dot grid overlay
                val dotSpacing = 20.dp.toPx()
                val dotRadius = 1.dp.toPx()
                val rows = (size.height / dotSpacing).toInt()
                val cols = (size.width / dotSpacing).toInt()
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        drawCircle(
                            color = com.example.ui.theme.DarkDotColor,
                            radius = dotRadius,
                            center = Offset(c * dotSpacing, r * dotSpacing)
                        )
                    }
                }

                // Tech cyber accent lines on corners
                val lineThickness = 2.dp.toPx()
                drawLine(
                    color = Color(0x33FF1A40),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.15f, 0f),
                    strokeWidth = lineThickness
                )
                drawLine(
                    color = Color(0x33FF1A40),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height * 0.08f),
                    strokeWidth = lineThickness
                )
            }
    ) {
        // Cockpit Main Body Scrollable Frame
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // A. TOP GLOWING STATUS BAR PANEL
            GamingDashboardHeader(
                currentMode = currentModeState,
                telemetry = telemetryState,
                onQuickModeChange = { mode ->
                    onTriggerHaptic(40)
                    viewModel.setSpeedMode(mode)
                }
            )

            // B. DECORATIVE MODE LOG BAR INDICATOR
            ModeIndicatorSliver(currentMode = currentModeState)

            // C. INTERCHANGEABLE DISPLAY CHANNELS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    "dashboard" -> {
                        DashboardCockpitContent(
                            telemetry = telemetryState,
                            mode = currentModeState,
                            onGlobalBoostTriggered = {
                                onTriggerHaptic(220)
                                viewModel.executeBoost()
                            }
                        )
                    }
                    "library" -> {
                        GameLauncherContent(
                            games = gameLibraryList,
                            activeMode = currentModeState,
                            onLaunchGame = { gameItem ->
                                onTriggerHaptic(180)
                                viewModel.executeBoost(gameItem)
                            },
                            onAddGameClicked = {
                                showAddGameSheet = true
                            },
                            onDeleteGame = { gameId ->
                                viewModel.removeGame(gameId)
                            }
                        )
                    }
                    "charts" -> {
                        DetailedDiagnosticsContent(
                            telemetry = telemetryState,
                            mode = currentModeState
                        )
                    }
                }
            }

            // D. FUTURISTIC BOTTOM NAVIGATION RAIL SYSTEM
            ArmouryCrateBottomBar(
                currentActiveTab = activeTab,
                onTabSelected = { tab ->
                    onTriggerHaptic(30)
                    activeTab = tab
                }
            )
        }

        // FULL SCREEN COMPRESSOR BOOSTER SEQUENCE GLASS-OVERLAY
        AnimatedVisibility(
            visible = isBoostingActive,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(400))
        ) {
            BoosterConsoleEngineScreen(
                progress = boostProgressValue,
                logs = viewModel.boostTerminalLogs
            )
        }

        // BOOST SUMMARY CONSOLE REPORT DIALOG (POPUP)
        AnimatedVisibility(
            visible = showSuccessReportState,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            MatrixReportOverlayDialog(
                savedRamMb = savedRamMbVal,
                telemetry = telemetryState,
                mode = currentModeState,
                onDismiss = {
                    onTriggerHaptic(35)
                    viewModel.dismissSuccessReport()
                }
            )
        }

        // ADD NEW GAME CUSTOM CONSOLE POPUP DIALOG
        if (showAddGameSheet) {
            RegisterGameDialog(
                onDismiss = { showAddGameSheet = false },
                onAddGame = { title, targetFps, colIndex ->
                    viewModel.addCustomGame(title, targetFps, colIndex)
                    showAddGameSheet = false
                }
            )
        }
    }
}


// -------------------------------------------------------------
// HEAD PANEL (STATUS BAR HUD)
// -------------------------------------------------------------
@Composable
fun GamingDashboardHeader(
    currentMode: SpeedMode,
    telemetry: SystemTelemetry,
    onQuickModeChange: (SpeedMode) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fanRotate")
    val fanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (currentMode) {
                    SpeedMode.SILENT -> 2500
                    SpeedMode.BALANCED -> 1200
                    SpeedMode.X_MODE -> 400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "fanAngle"
    )

    val modeGlowColorState = animateColorAsState(
        targetValue = when (currentMode) {
            SpeedMode.SILENT -> SystemGreen
            SpeedMode.BALANCED -> NeonCyan
            SpeedMode.X_MODE -> NeonRed
        },
        animationSpec = tween(400),
        label = "modeglow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Core Brand Title with immersive gradient and system labels
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(modeGlowColorState.value)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "X STRIKE",
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFFB923C))
                                ),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        )
                        Text(
                            text = "SYSTEM PROTOCOL 4.0",
                            style = TextStyle(
                                color = Color(0xFFEF4444),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = modeGlowColorState.value.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, modeGlowColorState.value.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentMode.name,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = modeGlowColorState.value
                            )
                        )
                    }
                }

                // Dynamic Hardware Thermal Badge and circular Immersive 'X' badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x11FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Cooler Speed",
                            tint = modeGlowColorState.value,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(fanAngle)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${telemetry.batteryTempCelsius}°C",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (telemetry.batteryTempCelsius > 39f) NeonRed else TextPrimary
                        )
                    }

                    // Immersive theme branding X circle badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x0FFF1A40))
                            .border(1.dp, Color(0x4DFF1A40), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "X",
                            style = TextStyle(
                                color = Color(0xFFEF4444),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // QUICK PROFILE SECTOR SELECTORS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val profileColor = when (mode) {
                        SpeedMode.SILENT -> SystemGreen
                        SpeedMode.BALANCED -> NeonCyan
                        SpeedMode.X_MODE -> NeonRed
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) profileColor.copy(alpha = 0.15f) else Color(
                                    0x04FFFFFF
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) profileColor else CyberCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onQuickModeChange(mode)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when(mode) {
                                    SpeedMode.SILENT -> "SILENT"
                                    SpeedMode.BALANCED -> "BALANCED"
                                    SpeedMode.X_MODE -> "X-MODE"
                                },
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isSelected) profileColor else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// METRIC LIGHT SLIVER UNDER THE HUD HEADER
// -------------------------------------------------------------
@Composable
fun ModeIndicatorSliver(currentMode: SpeedMode) {
    val accentColorState = animateColorAsState(
        targetValue = when (currentMode) {
            SpeedMode.SILENT -> SystemGreen
            SpeedMode.BALANCED -> NeonCyan
            SpeedMode.X_MODE -> NeonRed
        },
        animationSpec = tween(400),
        label = "accentLine"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColorState.value,
                        accentColorState.value,
                        Color.Transparent
                    )
                )
            )
    )
}


// -------------------------------------------------------------
// PRIMARY CONTENT TAB A: COCKPIT CONTROLLER DASHBOARD
// -------------------------------------------------------------
@Composable
fun DashboardCockpitContent(
    telemetry: SystemTelemetry,
    mode: SpeedMode,
    onGlobalBoostTriggered: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Giant Dual System Monitor circular gauges surrounding prime reactor button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Interactive core dial cockpit unit
                GamerTelemetryCoreReactor(
                    telemetry = telemetry,
                    mode = mode,
                    onBoostClick = onGlobalBoostTriggered
                )
            }
        }

        // Quad system sensor stats cards readout
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatHudBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Info,
                        label = "ESTIMATED FPS",
                        value = "${telemetry.fpsEstimate}",
                        unit = "FPS",
                        accentColor = when(mode) {
                            SpeedMode.SILENT -> SystemGreen
                            SpeedMode.BALANCED -> NeonCyan
                            SpeedMode.X_MODE -> NeonRed
                        },
                        statusText = when (mode) {
                            SpeedMode.SILENT -> "BATTERY CONSERVED"
                            SpeedMode.BALANCED -> "HD STABLE"
                            SpeedMode.X_MODE -> "OVERLOCKED HZ"
                        }
                    )

                    StatHudBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        label = "PING NETWORK",
                        value = "${telemetry.networkPingMs}",
                        unit = "ms",
                        accentColor = if (telemetry.networkPingMs < 20) NeonCyan else GamerGold,
                        statusText = if (telemetry.networkPingMs < 20) "EXCELLENT SHIELD" else "STABLE BUFFER"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatHudBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Build,
                        label = "AVAILABLE RAM",
                        value = String.format("%.1f", telemetry.ramTotalMb - telemetry.ramUsedMb).replace(",", "."),
                        unit = "GB",
                        accentColor = NeonPurple,
                        statusText = "TOTAL ${String.format("%.1f", telemetry.ramTotalMb / 1024f).replace(",", ".")} GB"
                    )

                    StatHudBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        label = "CPU CORE COCKPIT",
                        value = "${telemetry.cpuFrequencyGhz}",
                        unit = "GHz",
                        accentColor = GamerGold,
                        statusText = "LOAD AT ${telemetry.cpuUsage}%"
                    )
                }
            }
        }

        // Armoury Crate Style Instruction Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonRed.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, NeonRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🛡️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Akselerator X STRIKE Aktif",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "Tekan lingkaran sensor di atas atau pilih game di tab Library untuk mematikan cache sistem.",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}


// -------------------------------------------------------------
// GIANT SYSTEM INDICATOR CIRCULAR DIALS
// -------------------------------------------------------------
@Composable
fun GamerTelemetryCoreReactor(
    telemetry: SystemTelemetry,
    mode: SpeedMode,
    onBoostClick: () -> Unit
) {
    val cpuAnimateVal by animateFloatAsState(targetValue = telemetry.cpuUsage.toFloat() / 100f, animationSpec = tween(800), label = "cpuAnimate")
    val ramAnimateVal by animateFloatAsState(targetValue = telemetry.ramPercentage / 100f, animationSpec = tween(800), label = "ramAnimate")
    val gpuAnimateVal by animateFloatAsState(targetValue = telemetry.gpuUsage.toFloat() / 100f, animationSpec = tween(800), label = "gpuAnimate")

    val accentColor = when (mode) {
        SpeedMode.SILENT -> SystemGreen
        SpeedMode.BALANCED -> NeonCyan
        SpeedMode.X_MODE -> NeonRed
    }

    // Outer rotating concentric ring design
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val ringPulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringPulse"
    )

    val ringRotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotate"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp),
        contentAlignment = Alignment.Center
    ) {
        val diameter = 240.dp

        // Canvas element drawing nested arcs
        Canvas(
            modifier = Modifier.size(diameter)
        ) {
            val width = size.width
            val height = size.height
            val centerPoint = Offset(width / 2, height / 2)

            // 1. Draw outer tactical dashed boundary circle
            drawCircle(
                color = CyberCardBorder.copy(alpha = 0.5f),
                radius = (width / 2) - 8.dp.toPx(),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 20f))
                )
            )

            // 2. Draw rotating compass crosshair ticks
            rotate(ringRotateAngle, centerPoint) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.25f),
                    radius = (width / 2) - 18.dp.toPx(),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 18f))
                    )
                )

                // Tactical notch indicators
                for (angle in 0 until 360 step 45) {
                    rotate(angle.toFloat(), centerPoint) {
                        drawLine(
                            color = accentColor.copy(alpha = 0.6f),
                            start = Offset(centerPoint.x, 22.dp.toPx()),
                            end = Offset(centerPoint.x, 32.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            // 3. Draw Left Arc (CPU load display) - 120-degree span
            drawArc(
                color = Color(0x1AFFFFFF),
                startAngle = 135f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(24.dp.toPx(), 24.dp.toPx()),
                size = size / 1.25f,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(listOf(GamerGold, NeonRed)),
                startAngle = 135f,
                sweepAngle = 100f * cpuAnimateVal,
                useCenter = false,
                topLeft = Offset(24.dp.toPx(), 24.dp.toPx()),
                size = size / 1.25f,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Draw Right Arc (GPU load display) - 120-degree span
            drawArc(
                color = Color(0x1AFFFFFF),
                startAngle = 305f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(24.dp.toPx(), 24.dp.toPx()),
                size = size / 1.25f,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                startAngle = 305f,
                sweepAngle = 100f * gpuAnimateVal,
                useCenter = false,
                topLeft = Offset(24.dp.toPx(), 24.dp.toPx()),
                size = size / 1.25f,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Centered Reactor Core Ring (Pulsating)
            val coreRadius = (width / 3.4f) * ringPulseGlow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                    center = centerPoint,
                    radius = coreRadius
                ),
                radius = coreRadius
            )

            drawCircle(
                color = accentColor,
                radius = width / 3.5f,
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(50f, 12f))
                )
            )
        }

        // Overlay Interactive Central Button UI
        Box(
            modifier = Modifier
                .size(diameter / 2.0f)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CyberCard, CyberBlack)
                    )
                )
                .border(2.dp, accentColor.copy(alpha = 0.8f), CircleShape)
                .clickable {
                    onBoostClick()
                }
                .testTag("boost_now_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BOOST",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(ramAnimateVal * 100).toInt()}% RAM",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                )
            }
        }

        // Gauge Texts outside Arcs (floating left & right)
        Text(
            text = "CPU FREQ\n${telemetry.cpuFrequencyGhz}GHz",
            style = TextStyle(
                fontSize = 10.sp,
                color = GamerGold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 10.dp, y = (40).dp)
        )

        Text(
            text = "GPU CORE\n${telemetry.gpuFrequencyMhz}MHz",
            style = TextStyle(
                fontSize = 10.sp,
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-10).dp, y = (-40).dp)
        )
    }
}


// -------------------------------------------------------------
// INDIVIDUAL GRID STAT BOXES (GLASSMORPHIC COCKPIT TILES)
// -------------------------------------------------------------
@Composable
fun StatHudBox(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    statusText: String
) {
    Card(
        modifier = modifier
            .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    ),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusText,
                style = TextStyle(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = if (accentColor == NeonRed) NeonRed else TextSecondary
                )
            )
        }
    }
}


// -------------------------------------------------------------
// PRIMARY CONTENT TAB B: GAME RUNNER LIBRARY
// -------------------------------------------------------------
@Composable
fun GameLauncherContent(
    games: List<GameProfile>,
    activeMode: SpeedMode,
    onLaunchGame: (GameProfile) -> Unit,
    onAddGameClicked: () -> Unit,
    onDeleteGame: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Sector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONSOLE LIBRARY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
                Text(
                    text = "Daftar pintasan game akselerasi optimal",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Button(
                onClick = onAddGameClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonRed.copy(alpha = 0.15f),
                    contentColor = NeonRed
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .border(1.dp, NeonRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .testTag("add_game_shortcut_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add shortcut", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DAFTAR",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎮", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "LIBRARY KOSONG",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Silakan tambah game pertama kamu agar bisa diawasi.",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(games) { game ->
                    GameLauncherProfileCard(
                        game = game,
                        activeMode = activeMode,
                        onLaunch = { onLaunchGame(game) },
                        onDelete = { onDeleteGame(game.id) }
                    )
                }
            }
        }
    }
}


// -------------------------------------------------------------
// ITEM GAME CARD DESIGN WITH NEON ACCENTS
// -------------------------------------------------------------
@Composable
fun GameLauncherProfileCard(
    game: GameProfile,
    activeMode: SpeedMode,
    onLaunch: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Game Poster Initial circle
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        game.customAccent,
                                        game.customAccent.copy(alpha = 0.2f)
                                    )
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.dp, game.customAccent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = game.title.take(2).uppercase(),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = game.title,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = game.packageId,
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove game",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tech specs inside game profile card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x08FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MODE TARGET",
                        style = TextStyle(fontSize = 7.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = if (activeMode == SpeedMode.X_MODE) "ULTRACLOCK 120HZ" else "BALANCED STEADY",
                        style = TextStyle(fontSize = 10.sp, color = game.customAccent, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LAUNCH COUNT",
                        style = TextStyle(fontSize = 7.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = "${game.launchCount} plays",
                        style = TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ALLOCATED FPS",
                        style = TextStyle(fontSize = 7.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = "${game.fpsPerformanceTarget} FPS Max",
                        style = TextStyle(fontSize = 10.sp, color = GamerGold, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LAUNCH ACTION WITH HOVER FEEDBACK
            Button(
                onClick = onLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("launch_shortcut_${game.title.replace(" ", "_")}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = game.customAccent
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Launch play",
                    tint = CyberBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OPTIMALKAN & LUNCH GAME",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = CyberBlack
                    )
                )
            }
        }
    }
}


// -------------------------------------------------------------
// PRIMARY CONTENT TAB C: REAL-TIME ANALYTICAL CHARTS (DIAGNOSTICS)
// -------------------------------------------------------------
@Composable
fun DetailedDiagnosticsContent(
    telemetry: SystemTelemetry,
    mode: SpeedMode
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "REAL-TIME DIAGNOSTIC CHART",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        // CPU Performance over time (Visual Wave Graph)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MONITOR BEBAN CORE CPU (%)",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                            )
                            Text(
                                text = "Memantau performa frekuensi dynamic CPU",
                                style = TextStyle(fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            )
                        }

                        Text(
                            text = "${telemetry.cpuUsage}%",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = NeonRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PerformanceSparkline(
                        history = telemetry.historyCpu,
                        lineColor = NeonRed,
                        glowColor = NeonRed.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Interval update: ~1.2 detik. Status: ${if (mode == SpeedMode.X_MODE) "OVERCLOCK BUFFERING" else "IDLE OPTIMIZATION"}",
                        style = TextStyle(fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }

        // Memory usage timeline (Dynamic progress tracking)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DISTRIBUSI KONSUMSI RAM (%)",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                            )
                            Text(
                                text = "Menghitung byte yang dialokasikan di background",
                                style = TextStyle(fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            )
                        }

                        Text(
                            text = String.format("%.1f%%", telemetry.ramPercentage).replace(",", "."),
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PerformanceSparkline(
                        history = telemetry.historyRam,
                        lineColor = NeonCyan,
                        glowColor = NeonCyan.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Total RAM terdeteksi: ${String.format("%.1f", telemetry.ramTotalMb).replace(",", ".")} MB | Dialokasikan: ${String.format("%.1f", telemetry.ramUsedMb).replace(",", ".")} MB",
                        style = TextStyle(fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }

        // Sub-hardware specs diagnostics panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "X STRIKE BOOSTER KERNEL REPORT",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticSpecificationRow(label = "Hardware Chipset Processor type", value = Build.HARDWARE.uppercase())
                    DiagnosticSpecificationRow(label = "System OS Build Version SDK Release", value = "CORE ENGINE ${Build.VERSION.SDK_INT}")
                    DiagnosticSpecificationRow(label = "X-STRIKE Hypercore Service status", value = "ACTIVE ENCRYPTED")
                    DiagnosticSpecificationRow(label = "Thermal Throttling limit defense", value = "75.0 °C PROTECTION")
                }
            }
        }
    }
}


// -------------------------------------------------------------
// DYNAMIC ADVANCED LINE GRAPH COMPONENT DRAWN ON CANVAS
// -------------------------------------------------------------
@Keep
@Composable
fun PerformanceSparkline(
    history: List<Float>,
    lineColor: Color,
    glowColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0x05FFFFFF))
            .border(0.5.dp, CyberCardBorder.copy(alpha = 0.3f))
    ) {
        if (history.size < 2) return@Canvas

        val samplePoints = history.size
        val blockWidth = size.width / (samplePoints - 1)
        val limitMax = 100f
        val limitMin = 0f
        val span = limitMax - limitMin

        val graphPath = Path()
        val fillPath = Path()

        // Starting dot
        val startY = size.height - ((history[0] - limitMin) / span * size.height)
        graphPath.moveTo(0f, startY)
        fillPath.moveTo(0f, size.height)
        fillPath.lineTo(0f, startY)

        for (i in 1 until samplePoints) {
            val pointX = i * blockWidth
            val rawY = history[i]
            val pointY = size.height - ((rawY - limitMin) / span * size.height)
            graphPath.lineTo(pointX, pointY)
            fillPath.lineTo(pointX, pointY)
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        // 1. Draw glowing background grid lines
        val rawGridY = floatArrayOf(0.25f, 0.5f, 0.75f)
        rawGridY.forEach { fraction ->
            val gridLineY = size.height * fraction
            drawLine(
                color = CyberCardBorder.copy(alpha = 0.15f),
                start = Offset(0f, gridLineY),
                end = Offset(size.width, gridLineY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Draw path fill with neon lighting gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(glowColor, Color.Transparent)
            )
        )

        // 3. Draw outline path line
        drawPath(
            path = graphPath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 4. Draw terminal active pulsing point
        val activeX = size.width
        val activeY = size.height - ((history.last() - limitMin) / span * size.height)
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(activeX, activeY)
        )
        drawCircle(
            color = lineColor,
            radius = 7.dp.toPx(),
            center = Offset(activeX, activeY),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}


// -------------------------------------------------------------
// HELPER SPECIFICATION LIST ROW
// -------------------------------------------------------------
@Composable
fun DiagnosticSpecificationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        )
        Text(
            text = value,
            style = TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )
    }
}


// -------------------------------------------------------------
// CORE BOTTOM NAV BAR (ARMOURY CRATE CYBERNETIC RAIL)
// -------------------------------------------------------------
@Composable
fun ArmouryCrateBottomBar(
    currentActiveTab: String,
    onTabSelected: (String) -> Unit
) {
    // Ensuring navigation respects safely the bottom safe zones (navigationBars)
    val bottomInsetsPadding = WindowInsets.navigationBars.asPaddingValues()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottomInsetsPadding)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .border(1.dp, CyberCardBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("dashboard", "🕹️ CONSOLE", "Dashboard sensor utama"),
                Triple("library", "🎮 LIBRARY", "Daftar launcher game"),
                Triple("charts", "📈 ANALYTICS", "Laporan diagnostik real-time")
            )

            tabs.forEach { tabData ->
                val tabKey = tabData.first
                val tabHeader = tabData.second

                val isSelected = currentActiveTab == tabKey
                val highlightColor = if (isSelected) NeonRed else Color.Transparent

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTabSelected(tabKey) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("nav_tab_$tabKey")
                ) {
                    Text(
                        text = tabHeader,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tactical glowing selector capsule
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .background(highlightColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}


// -------------------------------------------------------------
// GLASS OVERLAY: ACTIVE FLUSH OPTIMIZER ENGINE WITH PROGRESS BAR
// -------------------------------------------------------------
@Composable
fun BoosterConsoleEngineScreen(
    progress: Float,
    logs: List<BoostTerminalLog>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack.copy(alpha = 0.96f))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HUD Header Info
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Text(
                    text = "X STRIKE CORE RE-ROUTER STATUS",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonRed
                    )
                )
                Text(
                    text = "FLUSHING CPU CACHE & REDUCING MEMORY JITTER DEVIATIONS",
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Progress state percentage HUD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "OPTIMIZING SYSTEM ENVIRONMENT...",
                        style = TextStyle(fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = NeonRed, fontFamily = FontFamily.Monospace)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Glowing neon red progress tracker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(Color(0x22FF1A40), RoundedCornerShape(5.dp))
                        .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NeonPurple, NeonRed)
                                ),
                                RoundedCornerShape(5.dp)
                            )
                    )
                }
            }

            // Real-time matrix debug log window
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(Color(0xFF07080D), RoundedCornerShape(12.dp))
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(logs.reversed()) { log ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "[${log.timestamp}]",
                                style = TextStyle(fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log.message,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.isAlert) NeonRed else SystemGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }

            // Tech Warnings Indicator HUD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.05f))
            ) {
                Text(
                    text = "WARNING: JANGAN PINDAH HALAMAN ATAU KELUAR DARI APLIKASI SAAT DETEKTOR OPTIMALISASI SEDANG BERJALAN TIDAK TERGANGGU.",
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonRed,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


// -------------------------------------------------------------
// REVAL SUCCESS STATS OVERLAY DIALOG (MATRIC REPORT GLASS)
// -------------------------------------------------------------
@Composable
fun MatrixReportOverlayDialog(
    savedRamMb: Int,
    telemetry: SystemTelemetry,
    mode: SpeedMode,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEA07080D))
            .clickable(enabled = true, onClick = {}) // Block background clicks
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, NeonRed, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚀 SYSTEM BOOST SUCCESS!",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = NeonRed,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(NeonRed.copy(alpha = 0.1f), CircleShape)
                        .border(2.dp, NeonRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+$savedRamMb",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "MB CLEANED",
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats improvement breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0EFFFFFF), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuccessStatRow(label = "X Strike Engine State", value = "MAXIMUM FREQUENCY")
                    SuccessStatRow(label = "Allocated FPS Refresh", value = "${telemetry.fpsEstimate} FPS")
                    SuccessStatRow(label = "Hyper-Ping Latency Status", value = "${telemetry.networkPingMs} ms (EXCELLENT)")
                    SuccessStatRow(label = "Gamer Profile Tuner", value = mode.name)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("dismiss_boost_report_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "KOKPIT UTAMA",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CyberBlack
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SuccessStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        )
        Text(
            text = value,
            style = TextStyle(fontSize = 10.sp, color = SystemGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )
    }
}


// -------------------------------------------------------------
// ADD NEW GAME CUSTOM POPUP DIALOG WINDOW
// -------------------------------------------------------------
@Composable
fun RegisterGameDialog(
    onDismiss: () -> Unit,
    onAddGame: (String, Int, Int) -> Unit
) {
    var gameName by remember { mutableStateOf("") }
    var performanceTarget by remember { mutableStateOf("90") }
    var colorIndex by remember { mutableStateOf(0) }

    val colorOptions = listOf(NeonRed, NeonCyan, NeonPurple, GamerGold, SystemGreen)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "REGISTER GAME SHORTCUT",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = NeonRed
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Daftarkan shortcut game android kamu ke dashboard agar bisa menerima pengawasan RAM dan alokasi daya core.",
                    style = TextStyle(fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                )

                // Input Game Title
                OutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("Game Name (e.g. Arknights)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_game_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonRed,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedLabelColor = NeonRed,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Input target priority frame rate
                OutlinedTextField(
                    value = performanceTarget,
                    onValueChange = { performanceTarget = it },
                    label = { Text("Target Frame Rate (e.g. 60, 90, 120)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_target_fps"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonRed,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedLabelColor = NeonRed,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Choose Accent Gamer Color
                Column {
                    Text(
                        text = "PILIH HEX GAMER COCKPIT ACCENT:",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEachIndexed { idx, col ->
                            val selected = colorIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(col, CircleShape)
                                    .border(
                                        width = if (selected) 2.5.dp else 0.dp,
                                        color = if (selected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorIndex = idx }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (gameName.isNotBlank()) {
                        val fps = performanceTarget.toIntOrNull() ?: 90
                        onAddGame(gameName, fps, colorIndex)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                modifier = Modifier.testTag("dialog_confirm_add_game_button")
            ) {
                Text(
                    text = "REGISTER",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = CyberBlack)
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)
            ) {
                Text(text = "CANCEL", style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace))
            }
        },
        containerColor = CyberCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
    )
}

// -------------------------------------------------------------
// COMPAT ACCENTS FOR COMPOSE SYSTEM STATUSBAR / NAVIGATION Safe-Zone
// -------------------------------------------------------------
@Composable
fun Modifier.statusBarsPadding() = this.padding(WindowInsets.statusBars.asPaddingValues())
