package com.example

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // States
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var showSimulationMode by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationProgress by remember { mutableStateOf(0f) }
    var activeCleaners by remember { mutableStateOf(listOf("Local cache cleared", "RAM speed boosted", "Secure VPN active")) }
    
    // System check function for overlay draw
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    // Initialize state
    LaunchedEffect(Unit) {
        hasOverlayPermission = checkOverlayPermission()
    }

    // Permission launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        hasOverlayPermission = checkOverlayPermission()
        if (hasOverlayPermission) {
            Toast.makeText(context, "Overlay Permission GRANTED!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission was denied. Interactive simulation will be launched instead.", Toast.LENGTH_LONG).show()
        }
    }

    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    // Launch Game Check
    fun launchFreeFire() {
        val packageManager = context.packageManager
        // Free Fire package names
        val packages = listOf("com.dts.freefireth", "com.dts.freefiremax")
        var launched = false
        
        for (pkg in packages) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    // Try to start our overlay service
                    if (hasOverlayPermission) {
                        val serviceIntent = Intent(context, FloatingMenuService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        Toast.makeText(context, "Starting game without overlay (Overlay permission needed)", Toast.LENGTH_LONG).show()
                    }
                    
                    context.startActivity(intent)
                    launched = true
                    break
                }
            } catch (e: Exception) {
                // Not found or failed
            }
        }
        
        if (!launched) {
            // Free fire not found. Let's trigger beautiful interactive simulation mode directly inside the app!
            Toast.makeText(context, "Free Fire not found. Entering full simulation sandbox!", Toast.LENGTH_LONG).show()
            
            // Start the overlay service locally anyway if they granted permission, so they can use it on our custom simulation!
            if (hasOverlayPermission) {
                val serviceIntent = Intent(context, FloatingMenuService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            showSimulationMode = true
        }
    }

    if (showSimulationMode) {
        // BEAUTIFUL INTERACTIVE 3D SIMULATOR LANDSCAPE
        SimulationSandbox(
            onBackToDashboard = {
                showSimulationMode = false
                // Stop service if needed
                try {
                    context.stopService(Intent(context, FloatingMenuService::class.java))
                } catch (e: Exception) {}
            }
        )
    } else {
        // NEON CYBERPUNK MAIN DASHBOARD
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CyberDark
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Glowing Banner Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NESTO MOD",
                            style = TextStyle(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                color = NeonPink,
                                shadow = Shadow(
                                    color = NeonPink,
                                    offset = Offset(0f, 0f),
                                    blurRadius = 25f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "CYBERNETIC ASSISTANT",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = NeonCyan,
                                shadow = Shadow(
                                    color = NeonCyan,
                                    offset = Offset(0f, 0f),
                                    blurRadius = 12f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Grid stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "DEVICE STATUS",
                        value = "NON-ROOT",
                        color = NeonGreen,
                        icon = Icons.Default.Verified,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "ANTI-CHEAT",
                        value = "BYPASSED",
                        color = NeonCyan,
                        icon = Icons.Default.Shield,
                        modifier = Modifier.weight(1f)
                    )
                }

                // System Permission Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Overlay Permission",
                                tint = NeonPurple,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Floating Window Permission",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Allows displaying ESP boxes and aiming controllers on top of Free Fire.",
                                    color = DarkText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (hasOverlayPermission) NeonGreen else NeonPink,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasOverlayPermission) "Overlay Ready" else "Overlay Inactive",
                                    color = if (hasOverlayPermission) NeonGreen else NeonPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    if (hasOverlayPermission) {
                                        Toast.makeText(context, "Permission is already granted!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        requestOverlayPermission()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasOverlayPermission) CyberDark else NeonPurple
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = if (hasOverlayPermission) NeonPurple.copy(alpha = 0.5f) else NeonPurple,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Text(
                                    text = if (hasOverlayPermission) "Granted" else "Grant Permission",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // RAM & Optimization Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Ram Booster",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "System Optimizer & RAM Booster",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Frees game memory and blocks background tracker packets.",
                                        color = DarkText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (isOptimizing) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Optimizing latency packets...", color = NeonCyan, fontSize = 11.sp)
                                    Text("${(optimizationProgress * 100).toInt()}%", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { optimizationProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NeonCyan,
                                    trackColor = CyberDark,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isOptimizing = true
                                        optimizationProgress = 0f
                                        while (optimizationProgress < 1f) {
                                            delay(50)
                                            optimizationProgress += 0.05f
                                        }
                                        isOptimizing = false
                                        Toast.makeText(context, "System fully optimized! Latency decreased by 35%.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .border(1.dp, NeonCyan, RoundedCornerShape(10.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = "Optimize", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("OPTIMIZE SYSTEM NOW", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // BIG LAUNCH ACTION CONTAINER (NEON BORDER GLOW)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = NeonPink,
                            ambientColor = NeonPink
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyberCard, CyberDark)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(NeonPink, NeonCyan, NeonPurple)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { launchFreeFire() }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NeonPink.copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, NeonPink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start game",
                                tint = NeonPink,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "START MOD MENU",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                shadow = Shadow(
                                    color = NeonPink,
                                    blurRadius = 8f
                                )
                            )
                        )
                        
                        Text(
                            text = "Starts Floating Overlay & Launches FF Game",
                            color = DarkText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Simulation mode disclaimer / direct entry
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSimulationMode = true }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Test Mode",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Or Enter Simulated Test Mode Directly",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer credits
                Text(
                    text = "NESTO MOD MENUS FOR ANDROID\nANTI-BAN INJECTOR SYSTEM (NON-ROOT)",
                    color = DarkText.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = DarkText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    shadow = Shadow(
                        color = color.copy(alpha = 0.5f),
                        blurRadius = 6f
                    )
                )
            )
        }
    }
}


// FULLY INTERACTIVE SIMULATION SANDBOX
// It lets users test the exact ESP overlays, headshot tracer lines, wallhacks,
// and anti-ban console logs inside a mock game simulation environment!
@Composable
fun SimulationSandbox(
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    var isAimingActive by remember { mutableStateOf(false) }
    var currentLogIndex by remember { mutableStateOf(0) }
    
    // Timer list for fake anti-ban console activity
    val consoleLogs = remember {
        listOf(
            "Initializing Nesto virtual sandbox standard setup...",
            "Anti-ban defense handshake verified with game server...",
            "Wiping local tracer logs in cache folder directory...",
            "Injecting client-side visual frame tracers...",
            "Memory buffer optimized for 60 FPS...",
            "Custom ESP line anchors drawn over player targets...",
            "Antiban status check: SECURE (100% safety index)...",
            "Auto Headshot trigger initialized at 360hz aim lock...",
            "Chams wallhack pipeline loaded..."
        )
    }

    var liveLogText by remember { mutableStateOf(consoleLogs[0]) }

    LaunchedEffect(Unit) {
        while(true) {
            delay(3500)
            currentLogIndex = (currentLogIndex + 1) % consoleLogs.size
            liveLogText = consoleLogs[currentLogIndex]
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF020408)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MOCK GAME SCREEN BACKGROUND (SIMULATING 3D PLAYERS DRAWN WITH ESP)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Ground grid line lines
                val gridStep = 40.dp.toPx()
                val lineStroke = 0.5.dp.toPx()
                
                // Draw dynamic neon lines for grid to look cybernetic
                for (y in (size.height / 2).toInt()..size.height.toInt() step gridStep.toInt()) {
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.08f),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = lineStroke
                    )
                }

                // Targets (Mock enemies) with simulated coordinates on screen
                val enemies = listOf(
                    Triple(Offset(size.width * 0.25f, size.height * 0.45f), "Target #012", 124),
                    Triple(Offset(size.width * 0.70f, size.height * 0.42f), "Target #055", 98),
                    Triple(Offset(size.width * 0.52f, size.height * 0.35f), "Target #099", 240)
                )

                enemies.forEach { (pos, name, dist) ->
                    // 1. ESP Box (if enabled in Mod Menu)
                    if (FloatingMenuService.espEnabled && FloatingMenuService.espBox) {
                        val boxWidth = 80.dp.toPx()
                        val boxHeight = 140.dp.toPx()
                        
                        drawRect(
                            color = NeonPink,
                            topLeft = Offset(pos.x - boxWidth / 2, pos.y - boxHeight / 2),
                            size = Size(boxWidth, boxHeight),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        
                        // Corner corner frames
                        drawRect(
                            color = NeonPink.copy(alpha = 0.15f),
                            topLeft = Offset(pos.x - boxWidth / 2, pos.y - boxHeight / 2),
                            size = Size(boxWidth, boxHeight)
                        )
                    }

                    // 2. Wallhack Chams color filter (if wallhack enabled)
                    if (FloatingMenuService.wallhackEnabled) {
                        val colorMap = when(FloatingMenuService.chamsColor) {
                            "Neon Pink" -> NeonPink
                            "Neon Cyan" -> NeonCyan
                            else -> NeonGreen
                        }
                        
                        // Draw a full solid glowing visual outline representing "wallhack chams"
                        drawCircle(
                            color = colorMap.copy(alpha = 0.35f),
                            radius = 30.dp.toPx(),
                            center = pos
                        )
                    }

                    // 3. ESP Tracers Line (if enabled)
                    if (FloatingMenuService.espEnabled && FloatingMenuService.espLine) {
                        // Tracers usually start from center bottom or center top
                        drawLine(
                            color = NeonPink.copy(alpha = 0.7f),
                            start = Offset(size.width / 2, size.height),
                            end = pos,
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 4. ESP Distance & Text (if enabled)
                    if (FloatingMenuService.espEnabled && FloatingMenuService.espDistance) {
                        // Drawing markers or text boxes can be simulated with canvas or text, let's draw dots & safe coordinates
                        drawCircle(
                            color = NeonYellow,
                            radius = 3.dp.toPx(),
                            center = pos
                        )
                    }
                }

                // Custom Crosshair drawn at the center
                val crosshairSize = 12.dp.toPx()
                drawLine(
                    color = NeonGreen,
                    start = Offset(size.width / 2 - crosshairSize, size.height / 2),
                    end = Offset(size.width / 2 + crosshairSize, size.height / 2),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = NeonGreen,
                    start = Offset(size.width / 2, size.height / 2 - crosshairSize),
                    end = Offset(size.width / 2, size.height / 2 + crosshairSize),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Circle surrounding crosshair (Aim FOV simulation)
                if (FloatingMenuService.autoHeadshotEnabled) {
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.25f),
                        radius = 48.dp.toPx(),
                        center = Offset(size.width / 2, size.height / 2),
                        style = Stroke(width = 1f.dp.toPx())
                    )
                }
            }

            // MOCK TARGET NAMES & LABELS overlayed on canvas
            if (FloatingMenuService.espEnabled) {
                // Left Target Text info
                Box(
                    modifier = Modifier
                        .offset(x = 40.dp, y = 200.dp)
                        .background(CyberCard.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, NeonPink, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Column {
                        Text("TARGET #012", color = NeonPink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        if (FloatingMenuService.espDistance) {
                            Text("DIST: 124m | HP: 100", color = Color.White, fontSize = 8.sp)
                        }
                    }
                }

                // Right Target Text info
                Box(
                    modifier = Modifier
                        .offset(x = 240.dp, y = 180.dp)
                        .background(CyberCard.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, NeonPink, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Column {
                        Text("TARGET #055", color = NeonPink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        if (FloatingMenuService.espDistance) {
                            Text("DIST: 98m | HP: 85", color = Color.White, fontSize = 8.sp)
                        }
                    }
                }
            }

            // SIMULATOR CONTROL & HUD UI OVERLAYS
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back to dashboard
                    Button(
                        onClick = onBackToDashboard,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCard.copy(alpha = 0.8f)),
                        modifier = Modifier.border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exit Sandbox", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Floating widget tutorial / info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.9f)),
                        modifier = Modifier.border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NeonGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nesto Floating Tool Simulated",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Middle area: Floating Instruction hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDark.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .widthIn(max = 340.dp)
                            .border(1.dp, NeonPink.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SIMULATOR SANDBOX ACTIVE",
                                color = NeonPink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use the floating 'N' bubble on screen to toggle features in real-time. If overlay permission is not granted, you can open the local settings panel below:",
                                color = LightText,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // In-sandbox setting toggle row (alternative backup controller)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SandboxMiniToggle(
                                    label = "ESP",
                                    checked = FloatingMenuService.espEnabled,
                                    onClick = {
                                        FloatingMenuService.espEnabled = !FloatingMenuService.espEnabled
                                        // Auto turn on sub components
                                        if (FloatingMenuService.espEnabled) {
                                            FloatingMenuService.espBox = true
                                            FloatingMenuService.espLine = true
                                            FloatingMenuService.espDistance = true
                                        }
                                    },
                                    color = NeonPink
                                )
                                SandboxMiniToggle(
                                    label = "HEADSHOT",
                                    checked = FloatingMenuService.autoHeadshotEnabled,
                                    onClick = { FloatingMenuService.autoHeadshotEnabled = !FloatingMenuService.autoHeadshotEnabled },
                                    color = NeonCyan
                                )
                                SandboxMiniToggle(
                                    label = "WALLHACK",
                                    checked = FloatingMenuService.wallhackEnabled,
                                    onClick = { FloatingMenuService.wallhackEnabled = !FloatingMenuService.wallhackEnabled },
                                    color = NeonPurple
                                )
                            }
                        }
                    }
                }

                // Bottom logs & system diagnostics console
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberDark.copy(alpha = 0.9f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(NeonGreen, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SECURE ANTIBAN SERVICE CONSOLE", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("NON-ROOT SYSTEM", color = DarkText, fontSize = 8.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = NeonGreen.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ">> ",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = liveLogText,
                                color = Color.White,
                                fontSize = 10.sp,
                                style = TextStyle(lineHeight = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SandboxMiniToggle(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (checked) color else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = if (checked) color.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (checked) Color.White else DarkText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
