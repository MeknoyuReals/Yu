package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.*
import kotlin.math.roundToInt

class FloatingMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    // Configurable state accessed globally
    companion object {
        var isServiceRunning = false
        
        // Settings States
        var espEnabled by mutableStateOf(false)
        var espBox by mutableStateOf(false)
        var espLine by mutableStateOf(false)
        var espDistance by mutableStateOf(false)
        
        var autoHeadshotEnabled by mutableStateOf(false)
        var aimLock by mutableStateOf(false)
        var headshotRate by mutableStateOf(85f)
        
        var wallhackEnabled by mutableStateOf(false)
        var chamsColor by mutableStateOf("Neon Pink")
        
        var antibanEnabled by mutableStateOf(true)
        var bypassAntiCheat by mutableStateOf(true)
        var logCleaner by mutableStateOf(true)
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        startNotification()
        showFloatingBubble()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startNotification() {
        val channelId = "nesto_mod_overlay"
        val channelName = "Nesto Mod Menu"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Nesto Mod overlay is running"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nesto Mod Active")
            .setContentText("Tap the floating bubble on screen to access features.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(9921, notification)
    }

    private fun showFloatingBubble() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MyApplicationTheme {
                    FloatingWidgetContent(
                        onDrag = { dx, dy ->
                            params?.let { p ->
                                p.x += dx
                                p.y += dy
                                windowManager.updateViewLayout(this@apply, p)
                            }
                        },
                        onCloseMenu = {
                            stopSelf()
                        }
                    )
                }
            }
        }

        // Setup custom lifecycle owners for the ComposeView inside Service
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(lifecycleOwner)

        floatingView = composeView
        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Custom Lifecycle Owner helper for Jetpack Compose in Service
    private class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        override val viewModelStore: ViewModelStore
            get() = store

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }

        fun performRestore(savedState: Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }
    }
}

@Composable
fun FloatingWidgetContent(
    onDrag: (Int, Int) -> Unit,
    onCloseMenu: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("main") }

    Box(modifier = Modifier.wrapContentSize()) {
        if (!isExpanded) {
            // DRAGGABLE GLOWING BUBBLE
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    }
                    .shadow(12.dp, CircleShape, spotColor = NeonPink, ambientColor = NeonPink)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyberCard, CyberDark),
                            radius = 120f
                        )
                    )
                    .border(2.dp, NeonPink, CircleShape)
                    .clickable { isExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                // Pulse Animation Background
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                // Glowing circular ring inside
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.15f),
                        radius = size.minDimension / 2 * pulseScale,
                        center = center
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "N",
                        style = TextStyle(
                            color = NeonCyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(
                                color = NeonCyan,
                                offset = Offset(0f, 0f),
                                blurRadius = 15f
                            )
                        )
                    )
                    Text(
                        text = "MOD",
                        style = TextStyle(
                            color = NeonPink,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        } else {
            // EXPANDED MOD BOX (SLEEK NEON CYBERPUNK DESIGN)
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .height(420.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    }
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(NeonPink, NeonCyan)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan, ambientColor = NeonPink),
                colors = CardDefaults.cardColors(
                    containerColor = CyberDark.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NeonGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NESTO MOD v1.0",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = NeonPink,
                                        blurRadius = 6f
                                    )
                                )
                            )
                        }
                        
                        Row {
                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Minimize,
                                    contentDescription = "Minimize Menu",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onCloseMenu,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Menu",
                                    tint = NeonPink,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Divider(color = NeonPink.copy(alpha = 0.3f), thickness = 1.dp)

                    // Tab Selector Navigation (🎯 AIMBOT | 👁️ ESP | 🛡️ SAFETY)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TabHeaderButton(
                            title = "AIM",
                            icon = Icons.Default.MyLocation,
                            isActive = currentTab == "main" || currentTab == "aim",
                            onClick = { currentTab = "aim" }
                        )
                        TabHeaderButton(
                            title = "ESP",
                            icon = Icons.Default.Visibility,
                            isActive = currentTab == "esp",
                            onClick = { currentTab = "esp" }
                        )
                        TabHeaderButton(
                            title = "SAFE",
                            icon = Icons.Default.Shield,
                            isActive = currentTab == "safe",
                            onClick = { currentTab = "safe" }
                        )
                    }

                    // Content Box depending on tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(CyberCard.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (currentTab) {
                                "aim" -> {
                                    Text(
                                        text = "AUTO HEADSHOT SETTINGS",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    MenuToggle(
                                        label = "Auto Headshot",
                                        checked = FloatingMenuService.autoHeadshotEnabled,
                                        onCheckedChange = { FloatingMenuService.autoHeadshotEnabled = it },
                                        color = NeonCyan
                                    )
                                    
                                    MenuToggle(
                                        label = "Aim Lock On Target",
                                        checked = FloatingMenuService.aimLock,
                                        onCheckedChange = { FloatingMenuService.aimLock = it },
                                        color = NeonCyan
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Aim Accuracy Strength: ${FloatingMenuService.headshotRate.toInt()}%",
                                        color = LightText,
                                        fontSize = 11.sp
                                    )
                                    Slider(
                                        value = FloatingMenuService.headshotRate,
                                        onValueChange = { FloatingMenuService.headshotRate = it },
                                        valueRange = 50f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = NeonCyan,
                                            activeTrackColor = NeonCyan,
                                            inactiveTrackColor = CyberDark
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "esp" -> {
                                    Text(
                                        text = "ESP & WALLHACK SETTINGS",
                                        color = NeonPink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    MenuToggle(
                                        label = "Master ESP Overlay",
                                        checked = FloatingMenuService.espEnabled,
                                        onCheckedChange = { FloatingMenuService.espEnabled = it },
                                        color = NeonPink
                                    )

                                    MenuToggle(
                                        label = "ESP Box (Draw Enemy Frame)",
                                        checked = FloatingMenuService.espBox,
                                        onCheckedChange = { FloatingMenuService.espBox = it },
                                        color = NeonPink,
                                        enabled = FloatingMenuService.espEnabled
                                    )

                                    MenuToggle(
                                        label = "ESP Line (Laser Tracer)",
                                        checked = FloatingMenuService.espLine,
                                        onCheckedChange = { FloatingMenuService.espLine = it },
                                        color = NeonPink,
                                        enabled = FloatingMenuService.espEnabled
                                    )

                                    MenuToggle(
                                        label = "ESP Distance Finder",
                                        checked = FloatingMenuService.espDistance,
                                        onCheckedChange = { FloatingMenuService.espDistance = it },
                                        color = NeonPink,
                                        enabled = FloatingMenuService.espEnabled
                                    )

                                    Divider(color = NeonPink.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                                    MenuToggle(
                                        label = "Wallhack (3D Chams)",
                                        checked = FloatingMenuService.wallhackEnabled,
                                        onCheckedChange = { FloatingMenuService.wallhackEnabled = it },
                                        color = NeonPink
                                    )

                                    if (FloatingMenuService.wallhackEnabled) {
                                        Text(
                                            text = "Chams Color Scheme",
                                            color = LightText,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            listOf("Neon Pink", "Neon Cyan", "Neon Green").forEach { colorName ->
                                                val isSelected = FloatingMenuService.chamsColor == colorName
                                                val col = when(colorName) {
                                                    "Neon Pink" -> NeonPink
                                                    "Neon Cyan" -> NeonCyan
                                                    else -> NeonGreen
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) col else col.copy(alpha = 0.4f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .background(
                                                            if (isSelected) col.copy(alpha = 0.15f) else Color.Transparent,
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable { FloatingMenuService.chamsColor = colorName }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = colorName.replace("Neon ", ""),
                                                        color = if (isSelected) Color.White else LightText,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> { // "safe" tab
                                    Text(
                                        text = "ANTIBAN & SECURITY",
                                        color = NeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    MenuToggle(
                                        label = "Anti-Ban Protection (Safe-Guard)",
                                        checked = FloatingMenuService.antibanEnabled,
                                        onCheckedChange = { FloatingMenuService.antibanEnabled = it },
                                        color = NeonGreen
                                    )

                                    MenuToggle(
                                        label = "Bypass Local Anti-Cheat",
                                        checked = FloatingMenuService.bypassAntiCheat,
                                        onCheckedChange = { FloatingMenuService.bypassAntiCheat = it },
                                        color = NeonGreen,
                                        enabled = FloatingMenuService.antibanEnabled
                                    )

                                    MenuToggle(
                                        label = "Log Cleaner (Auto-Wipe Trace)",
                                        checked = FloatingMenuService.logCleaner,
                                        onCheckedChange = { FloatingMenuService.logCleaner = it },
                                        color = NeonGreen,
                                        enabled = FloatingMenuService.antibanEnabled
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CyberDark),
                                        modifier = Modifier.border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = "Shield",
                                                tint = NeonGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Device Status: NON-ROOT",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Safe Virtual bypass is activated for standard devices.",
                                                    color = DarkText,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Footer with Device Status
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: Bypass Active",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = NeonGreen,
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            text = "Non-Root Android Support",
                            color = DarkText,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabHeaderButton(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) NeonCyan else Color.Transparent
    val glowColor = if (isActive) NeonCyan.copy(alpha = 0.2f) else Color.Transparent
    
    Box(
        modifier = Modifier
            .shadow(
                elevation = if (isActive) 4.dp else 0.dp,
                shape = RoundedCornerShape(8.dp),
                clip = true,
                ambientColor = NeonCyan,
                spotColor = NeonCyan
            )
            .border(
                width = 1.dp,
                color = if (isActive) NeonCyan else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isActive) CyberCard else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) NeonCyan else DarkText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isActive) Color.White else DarkText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MenuToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) LightText else DarkText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = color,
                uncheckedThumbColor = DarkText,
                uncheckedTrackColor = CyberDark
            )
        )
    }
}
