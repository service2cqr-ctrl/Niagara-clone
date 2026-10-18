package com.niagaraclone.launcher.ui.components

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREFS_ONBOARDING = "cascadia_onboarding_prefs"
private const val KEY_ONBOARDING_COMPLETED_V2 = "has_completed_niagara_onboarding_v2"

private enum class OnboardingStage {
    WELCOME_SHOWCASE,
    FAVORITES_PICKER,
    SWITCH_LAUNCHER_PROMPT,
    NOT_SELECTED_YET_PROMPT,
    GIVE_FEW_DAYS_GRAPH,
    HOME_FAVORITES_SPOTLIGHT,
    WAVE_HAND_TUTORIAL
}

@Composable
fun StartupPermissionsDialog(
    viewModel: LauncherViewModel,
    onStageChanged: (Boolean) -> Unit = {},
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var hasCompletedOnboarding by remember {
        mutableStateOf(prefs.getBoolean(KEY_ONBOARDING_COMPLETED_V2, false))
    }

    val finishOnboarding = {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED_V2, true).apply()
        hasCompletedOnboarding = true
        onStageChanged(false)
        onComplete()
    }

    if (hasCompletedOnboarding) {
        LaunchedEffect(Unit) {
            onStageChanged(false)
            onComplete()
        }
        return
    }

    val apps by viewModel.apps.collectAsState()
    val favorites by viewModel.favoritePackages.collectAsState()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    var stage by remember { mutableStateOf(OnboardingStage.WELCOME_SHOWCASE) }

    // Tell HomeScreen whether to hide background elements so they never bleed through!
    LaunchedEffect(stage) {
        val shouldHideHomeContent = stage != OnboardingStage.WAVE_HAND_TUTORIAL
        onStageChanged(shouldHideHomeContent)
    }

    val favoriteAppsList = remember(apps, favorites) {
        apps.filter { favorites.contains(it.packageName) }
    }

    val homeRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkDefaultLauncherStatus()
        val isNowDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            false
        }
        stage = if (isNowDefault) {
            OnboardingStage.GIVE_FEW_DAYS_GRAPH
        } else {
            OnboardingStage.NOT_SELECTED_YET_PROMPT
        }
    }

    val triggerSystemDefaultHomeDialog = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            } else {
                stage = OnboardingStage.GIVE_FEW_DAYS_GRAPH
            }
        } else {
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            stage = OnboardingStage.GIVE_FEW_DAYS_GRAPH
        }
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "niagara_onboarding_flow"
    ) { currentStage ->
        when (currentStage) {
            OnboardingStage.WELCOME_SHOWCASE -> WelcomeShowcaseStep(
                onAgreeContinue = { stage = OnboardingStage.FAVORITES_PICKER }
            )

            OnboardingStage.FAVORITES_PICKER -> FavoritesPickerScreen(
                apps = apps,
                initialSelected = favorites,
                accentColor = accentColor,
                onDone = { selectedSet ->
                    scope.launch {
                        viewModel.repository.setFavoritePackages(selectedSet)
                        stage = if (isDefaultLauncher) {
                            OnboardingStage.GIVE_FEW_DAYS_GRAPH
                        } else {
                            OnboardingStage.SWITCH_LAUNCHER_PROMPT
                        }
                    }
                }
            )

            OnboardingStage.SWITCH_LAUNCHER_PROMPT -> SwitchLauncherPromptSheet(
                accentColor = accentColor,
                onContinue = { triggerSystemDefaultHomeDialog() }
            )

            OnboardingStage.NOT_SELECTED_YET_PROMPT -> NotSelectedYetSheet(
                accentColor = accentColor,
                onTryAgain = { triggerSystemDefaultHomeDialog() },
                onNotNow = { stage = OnboardingStage.GIVE_FEW_DAYS_GRAPH }
            )

            OnboardingStage.GIVE_FEW_DAYS_GRAPH -> GiveFewDaysGraphStep(
                onGetStarted = { stage = OnboardingStage.HOME_FAVORITES_SPOTLIGHT }
            )

            OnboardingStage.HOME_FAVORITES_SPOTLIGHT -> HomeFavoritesSpotlightOverlay(
                favoriteApps = favoriteAppsList,
                accentColor = accentColor,
                onNext = { stage = OnboardingStage.WAVE_HAND_TUTORIAL }
            )

            OnboardingStage.WAVE_HAND_TUTORIAL -> WaveHandTutorialOverlay(
                accentColor = accentColor,
                onFinish = { finishOnboarding() }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// STEP 1: WELCOME SHOWCASE
// ---------------------------------------------------------------------------
@Composable
private fun WelcomeShowcaseStep(
    onAgreeContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A18))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(36.dp),
                color = Color(0xFF0F1E46),
                modifier = Modifier
                    .width(230.dp)
                    .height(440.dp)
                    .border(3.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(36.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1D3587), Color(0xFF091336), Color(0xFF050B24))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("3:50", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
                        Text("Sun, 18 Oct  ☀️ 28°", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        listOf("WhatsApp", "Phone", "Chrome", "Gmail", "Photos", "Notes").forEach { name ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("☆", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "R", "S", "T", "V", "W", "Z").forEach { ch ->
                            Text(ch, color = Color.White.copy(alpha = 0.7f), fontSize = 6.sp)
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "A New Home Screen That Fits\nYour Style",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "From distracting and cluttered to focused and clean.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onAgreeContinue,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7BA7FF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF132247),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Agree & Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// STEP 2: "Your favorites" SELECTION SCREEN (Clean background, no home bleed)
// ---------------------------------------------------------------------------
@Composable
fun FavoritesPickerScreen(
    apps: List<AppModel>,
    initialSelected: Set<String>,
    accentColor: Color,
    onDone: (Set<String>) -> Unit
) {
    val preferredDefaults = remember(apps) {
        val commonNames = listOf("whatsapp", "instagram", "youtube", "phone", "dialer", "chrome", "browser", "spotify", "gmail", "messages", "camera", "photos", "maps")
        apps.sortedBy { app ->
            val idx = commonNames.indexOfFirst { app.label.lowercase().contains(it) }
            if (idx == -1) 999 else idx
        }
    }

    var selectedPackages by remember {
        mutableStateOf(
            if (initialSelected.isNotEmpty()) initialSelected
            else preferredDefaults.take(5).map { it.packageName }.toSet()
        )
    }

    val selectedAppList = remember(apps, selectedPackages) {
        apps.filter { selectedPackages.contains(it.packageName) }
    }
    val unselectedAppList = remember(preferredDefaults, selectedPackages) {
        preferredDefaults.filter { !selectedPackages.contains(it.packageName) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.45f),
                        Color(0xFF0E0B14).copy(alpha = 0.94f),
                        Color(0xFF0A080E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 68.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "fav_title") {
                Text(
                    text = "Your\nfavorites",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 42.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item(key = "fav_info_1") {
                FavoriteInfoBanner(
                    icon = Icons.Outlined.StarBorder,
                    text = "Favorites appear on your\nhome screen for quick access",
                    accentColor = accentColor
                )
            }

            item(key = "fav_info_2") {
                FavoriteInfoBanner(
                    icon = Icons.Outlined.CheckCircle,
                    text = "Most people choose their 4 to\n8 most-used apps",
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (selectedAppList.isNotEmpty()) {
                item(key = "header_selected") {
                    Text(
                        text = "Selected",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(selectedAppList, key = { "sel_${it.packageName}" }) { app ->
                    FavoritePickerRow(
                        app = app,
                        isSelected = true,
                        showDragHandle = true,
                        onToggle = {
                            val next = selectedPackages.toMutableSet()
                            next.remove(app.packageName)
                            selectedPackages = next
                        }
                    )
                }
            }

            item(key = "header_suggestions") {
                Text(
                    text = "Suggestions",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            items(unselectedAppList, key = { "unsel_${it.packageName}" }) { app ->
                FavoritePickerRow(
                    app = app,
                    isSelected = false,
                    showDragHandle = false,
                    onToggle = {
                        val next = selectedPackages.toMutableSet()
                        next.add(app.packageName)
                        selectedPackages = next
                    }
                )
            }
        }

        // Top Sticky Pill ("5 selected · 8 recommended")
        Surface(
            shape = CircleShape,
            color = Color(0xFF1F1624),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            Text(
                text = "${selectedPackages.size} selected · 8 recommended",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }

        // Bottom-Right Floating "✓ Done" Pill Button
        Button(
            onClick = { onDone(selectedPackages) },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Done", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FavoriteInfoBanner(
    icon: ImageVector,
    text: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun FavoritePickerRow(
    app: AppModel,
    isSelected: Boolean,
    showDragHandle: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                checkmarkColor = Color.Black,
                uncheckedColor = Color.White.copy(alpha = 0.7f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        app.icon?.let { drawable ->
            Image(
                bitmap = drawable.toBitmap(width = 48, height = 48).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (showDragHandle) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Reorder",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// STEP 3: "Switch to Cascadia Launcher" & "Isn't Selected Yet"
// ---------------------------------------------------------------------------
@Composable
private fun SwitchLauncherPromptSheet(
    accentColor: Color,
    onContinue: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = palette.sheetBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Switch to Cascadia Launcher",
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Home, contentDescription = null, tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("What Happens When You Switch?", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Cascadia Launcher becomes your new home screen, so you can try it out", color = palette.textSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Replay, contentDescription = null, tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Switch Back Easily", color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("You can switch back to your old home screen anytime in settings", color = palette.textSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onContinue,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Continue", color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NotSelectedYetSheet(
    accentColor: Color,
    onTryAgain: () -> Unit,
    onNotNow: () -> Unit
) {
    val palette = LocalLauncherPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = palette.sheetBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cascadia Launcher Isn't\nSelected Yet",
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "This means Cascadia Launcher isn't set as your new home screen and won't work properly",
                            color = palette.textPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Save, contentDescription = null, tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "No worries, your old home screen is saved. You can switch back anytime in settings.",
                            color = palette.textPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onTryAgain,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Try again", color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onNotNow) {
                    Text("Not now", color = palette.textSecondary, fontSize = 15.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// STEP 4: "Give Cascadia Launcher a Few Days" Graph
// ---------------------------------------------------------------------------
@Composable
private fun GiveFewDaysGraphStep(
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A18))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Give Cascadia\nLauncher a Few Days",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF151C2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFF1D4ED8), modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("JF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Joe Fedewa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("How-To Geek", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "It takes a little getting used to, [...] you'll learn to love it",
                        color = Color.White.copy(alpha = 0.9f),
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("DAY 1–2", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("feels unfamiliar", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DAY 7", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("faster than ever", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.05f, h * 0.72f)
                        cubicTo(
                            w * 0.22f, h * 0.92f,
                            w * 0.45f, h * 0.45f,
                            w * 0.95f, h * 0.15f
                        )
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFF60A5FA))
                        ),
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                    drawCircle(color = Color(0xFFF59E0B), radius = 12f, center = Offset(w * 0.18f, h * 0.78f))
                    drawCircle(color = Color(0xFF60A5FA), radius = 14f, center = Offset(w * 0.95f, h * 0.15f))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..7).forEach { day ->
                        Text(day.toString(), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }

            OutlinedButton(
                onClick = onGetStarted,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7BA7FF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF132247),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Get started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// STEP 5: SPOTLIGHT FAVORITES CARD + TOOLTIP BELOW IT (Exact match to video 01:08)
// ---------------------------------------------------------------------------
@Composable
private fun HomeFavoritesSpotlightOverlay(
    favoriteApps: List<AppModel>,
    accentColor: Color,
    onNext: () -> Unit
) {
    val (currentTime, currentDate) = rememberCurrentTimeState(timeFormatPattern = "h:mm", dateFormatPattern = "EEE, MMM d")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            // Dimmed Clock above the spotlight card (just like video 01:08)
            Column(modifier = Modifier.padding(start = 8.dp, bottom = 18.dp)) {
                Text(
                    text = currentTime.ifEmpty { "9:43" },
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = currentDate.ifEmpty { "Sun, Oct 18" },
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 15.sp
                )
            }

            // Bright Elevated Spotlight Card displaying the user's selected Favorite Apps!
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF18141C).copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                shadowElevation = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    favoriteApps.take(6).forEach { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            app.icon?.let { drawable ->
                                Image(
                                    bitmap = drawable.toBitmap(width = 48, height = 48).asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.width(18.dp))
                            }
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tooltip Card placed cleanly BELOW the favorites card (never covering the apps!)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1F1624),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(end = 28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Favorites for Quick Access",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onNext,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF35283C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveHandTutorialOverlay(
    accentColor: Color,
    onFinish: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_hand_tutorial")
    val handProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hand_y"
    )

    LaunchedEffect(Unit) {
        delay(3400L)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onFinish() }
    ) {
        val offsetYDp = 220.dp + (260.dp * handProgress)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp)
                .offset { IntOffset(0, offsetYDp.roundToPx()) }
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.85f),
                shadowElevation = 8.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Slide alphabet to open apps",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
