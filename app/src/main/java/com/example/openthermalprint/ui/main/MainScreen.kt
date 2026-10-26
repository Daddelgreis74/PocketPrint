package com.example.openthermalprint.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.openthermalprint.R
import com.example.openthermalprint.bluetooth.ConnectionState
import com.example.openthermalprint.protocol.BarcodeGenerator
import com.example.openthermalprint.protocol.DitherMode
import com.example.openthermalprint.protocol.EscPosEncoder
import com.example.openthermalprint.ui.PrinterViewModel
import com.example.openthermalprint.ui.ThemeMode
import com.example.openthermalprint.ui.photo.*
import com.example.openthermalprint.ui.templates.*

enum class ScreenSection(val title: String) {
    HOME("Übersicht"),
    SHOPPING("Einkaufsliste"),
    NOTE("Notiz & Memo"),
    PHOTO("Foto-Druck"),
    TEMPLATES("Vorlagen & Etiketten"),
    TEXT("Freitext"),
    CODES("QR- & Barcodes"),
    SETTINGS("Einstellungen")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PrinterViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentSection by remember { mutableStateOf(ScreenSection.HOME) }
    var showDeviceSheet by remember { mutableStateOf(false) }

    // Bluetooth Permission launcher
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    var hasPermissions by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            viewModel.startScan()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.setSelectedImageUri(uri)
    }

    Scaffold(
        topBar = {
            val isConnected = uiState.connectionState is ConnectionState.Connected
            val isConnecting = uiState.connectionState is ConnectionState.Connecting

            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (currentSection == ScreenSection.HOME) "PocketPrint" else currentSection.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    if (currentSection != ScreenSection.HOME) {
                        IconButton(onClick = { currentSection = ScreenSection.HOME }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück zur Übersicht")
                        }
                    }
                },
                actions = {
                    // Kombinierter Verbinden-Button mit integriertem Statuspunkt
                    Surface(
                        onClick = {
                            if (hasPermissions) {
                                viewModel.startScan()
                                showDeviceSheet = true
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isConnected -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                            isConnecting -> Color(0xFFFFA000).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isConnected -> Color(0xFF2E7D32)
                                isConnecting -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Leuchtender Status-Punkt
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when {
                                            isConnected -> Color(0xFF2E7D32)
                                            isConnecting -> Color(0xFFFFA000)
                                            else -> Color(0xFFD32F2F)
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = when {
                                    isConnected -> "Verbunden"
                                    isConnecting -> "Verbinde..."
                                    else -> "Verbinden"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isConnected -> Color(0xFF2E7D32)
                                    isConnecting -> Color(0xFFFFA000)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentSection == ScreenSection.HOME,
                    onClick = { currentSection = ScreenSection.HOME },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.cat_home),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    label = { Text("Start") }
                )
                NavigationBarItem(
                    selected = currentSection == ScreenSection.SHOPPING,
                    onClick = { currentSection = ScreenSection.SHOPPING },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.cat_shopping),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    label = { Text("Einkauf") }
                )
                NavigationBarItem(
                    selected = currentSection == ScreenSection.NOTE,
                    onClick = { currentSection = ScreenSection.NOTE },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.cat_note),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    label = { Text("Notiz") }
                )
                NavigationBarItem(
                    selected = currentSection == ScreenSection.PHOTO,
                    onClick = { currentSection = ScreenSection.PHOTO },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.cat_photo),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    label = { Text("Foto") }
                )
                NavigationBarItem(
                    selected = currentSection == ScreenSection.SETTINGS,
                    onClick = { currentSection = ScreenSection.SETTINGS },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.cat_printer),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    label = { Text("Einstellungen") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status message notification banner
                AnimatedVisibility(visible = uiState.statusMessage != null) {
                    uiState.statusMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                when (currentSection) {
                    // STARTSEITE / DASHBOARD
                    ScreenSection.HOME -> HomeDashboardContent(
                        onNavigate = { currentSection = it },
                        itemCountShopping = uiState.shoppingItems.size
                    )

                    // EINKAUFSLISTE
                    ScreenSection.SHOPPING -> ShoppingListTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    // NOTIZ
                    ScreenSection.NOTE -> NoteTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    // FOTO
                    ScreenSection.PHOTO -> ImageTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSelectImage = { imagePickerLauncher.launch("image/*") }
                    )

                    // VORLAGEN & ETIKETTEN
                    ScreenSection.TEMPLATES -> TemplatesTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    // TEXT & VORLAGEN
                    ScreenSection.TEXT -> TextTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToTemplates = { currentSection = ScreenSection.TEMPLATES }
                    )

                    // QR- & BARCODES
                    ScreenSection.CODES -> BarcodeTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToTemplates = { currentSection = ScreenSection.TEMPLATES }
                    )

                    // GERÄTE- & DRUCKER-EINSTELLUNGEN
                    ScreenSection.SETTINGS -> SettingsTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRequestScan = {
                            if (hasPermissions) {
                                viewModel.startScan()
                                showDeviceSheet = true
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        }
                    )
                }
            }

            // In-App Update Dialog
            uiState.availableUpdate?.let { update ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = { Text("Update verfügbar: v${update.latestVersion}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Eine neue Version von PocketPrint ist auf GitHub bereitgestellt.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (update.releaseNotes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                                ) {
                                    Text(
                                        text = update.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.startUpdateDownload() }) {
                            Text("Jetzt aktualisieren")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text("Später")
                        }
                    }
                )
            }

            // Device picker dialog / sheet
            if (showDeviceSheet) {
                AlertDialog(
                    onDismissRequest = { showDeviceSheet = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Drucker auswählen")
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isScanning) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Suche nach Bluetooth-Druckern...", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            if (uiState.devices.isEmpty()) {
                                Text(
                                    "Keine gekoppelten Geräte gefunden. Bitte stelle sicher, dass der Drucker eingeschaltet ist.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                    items(uiState.devices) { dev ->
                                        ListItem(
                                            headlineContent = { Text(dev.name, fontWeight = FontWeight.SemiBold) },
                                            supportingContent = { Text(dev.address, style = MaterialTheme.typography.bodySmall) },
                                            trailingContent = {
                                                FilledTonalButton(
                                                    onClick = {
                                                        viewModel.connectDevice(dev.address)
                                                        showDeviceSheet = false
                                                    }
                                                ) {
                                                    Text("Verbinden")
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                viewModel.connectDevice(dev.address)
                                                showDeviceSheet = false
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.startScan() }) {
                            Text("Erneut suchen")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeviceSheet = false }) {
                            Text("Schließen")
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STARTSEITE: DASHBOARD / HOME
// -------------------------------------------------------------
@Composable
private fun HomeDashboardContent(
    onNavigate: (ScreenSection) -> Unit,
    itemCountShopping: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 2-Column Grid of Interactive Feature Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardFeatureCard(
                title = "Einkaufsliste",
                subtitle = "$itemCountShopping Artikel",
                imageRes = R.drawable.cat_shopping,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenSection.SHOPPING) }
            )
            DashboardFeatureCard(
                title = "Notiz & Memo",
                subtitle = "Schnellnotiz drucken",
                imageRes = R.drawable.cat_note,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenSection.NOTE) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardFeatureCard(
                title = "Foto-Studio",
                subtitle = "Zuschnitt, Rahmen & Deko",
                imageRes = R.drawable.cat_photo,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenSection.PHOTO) }
            )
            DashboardFeatureCard(
                title = "Vorlagen & Etiketten",
                subtitle = "Gewürze, WLAN, To-Do",
                imageRes = R.drawable.cat_text,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenSection.TEMPLATES) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardFeatureCard(
                title = "QR & Barcodes",
                subtitle = "Links, EAN & Codes",
                imageRes = R.drawable.cat_codes,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenSection.CODES) }
            )
            DashboardFeatureCard(
                title = "Freitext",
                subtitle = "Eigener Text & Layout",
                imageRes = R.drawable.cat_note,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(ScreenSection.TEXT) }
            )
        }

        // Einstellungen Vollbreite-Karte
        ElevatedCard(
            onClick = { onNavigate(ScreenSection.SETTINGS) },
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cat_printer),
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Einstellungen", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Drucker, Design, Vorschub & Updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun DashboardFeatureCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(170.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }
    }
}

// -------------------------------------------------------------
// REITER: EINKAUFSLISTE
// -------------------------------------------------------------
@Composable
private fun ShoppingListTabContent(
    uiState: com.example.openthermalprint.ui.UiState,
    viewModel: PrinterViewModel
) {
    // Speech-to-Text Intent Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.addSpokenShoppingItems(spokenText)
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🛒 Einkaufszettel",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (uiState.shoppingItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearShoppingList() }) {
                        Text("Alle löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Input Row: Text Input + Microphone Button + Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newShoppingItemText,
                    onValueChange = { viewModel.setNewShoppingItemText(it) },
                    label = { Text("Eintrag tippen...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Voice Input Button (Google Speech Overlay)
                FilledTonalIconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Artikel sprechen (z. B. 'Milch, Brot und Äpfel')...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Fallback wenn kein Google Speech Service installiert ist
                        }
                    },
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Spracheingabe",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Button(
                    onClick = { viewModel.addShoppingItem() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(50.dp),
                    contentPadding = PaddingValues(0.dp),
                    enabled = uiState.newShoppingItemText.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                }
            }

            // List of items
            if (uiState.shoppingItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Deine Einkaufsliste ist noch leer.",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.shoppingItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.toggleShoppingItem(item.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { viewModel.toggleShoppingItem(item.id) }
                            )
                            Text(
                                text = item.text,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { viewModel.removeShoppingItem(item.id) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Löschen",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Print Button
            Button(
                onClick = { viewModel.printShoppingList() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.shoppingItems.isNotEmpty() && uiState.connectionState is ConnectionState.Connected && !uiState.isPrinting
            ) {
                if (uiState.isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Einkaufsliste als Bon drucken")
            }
        }
    }
}

// -------------------------------------------------------------
// REITER: NOTIZ & MEMO
// -------------------------------------------------------------
@Composable
private fun NoteTabContent(
    uiState: com.example.openthermalprint.ui.UiState,
    viewModel: PrinterViewModel
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "📌 Schnelle Notiz",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // Category Selector Chips
            Text("Kategorie:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            val categories = listOf("📌 Notiz", "💡 Idee", "⏰ Termin", "⚠️ Wichtig")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = uiState.noteCategory == cat,
                        onClick = { viewModel.setNoteCategory(cat) },
                        label = { Text(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.noteTitle,
                onValueChange = { viewModel.setNoteTitle(it) },
                label = { Text("Titel der Notiz") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.noteContent,
                onValueChange = { viewModel.setNoteContent(it) },
                label = { Text("Inhalt / Text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Hier den Inhalt der Notiz eintragen...") }
            )

            Button(
                onClick = { viewModel.printNote() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = (uiState.noteTitle.isNotBlank() || uiState.noteContent.isNotBlank()) &&
                        uiState.connectionState is ConnectionState.Connected && !uiState.isPrinting
            ) {
                if (uiState.isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Notiz jetzt drucken")
            }
        }
    }
}

// -------------------------------------------------------------
// REITER: BILDER & FOTOS
// -------------------------------------------------------------
@Composable
private fun ImageTabContent(
    uiState: com.example.openthermalprint.ui.UiState,
    viewModel: PrinterViewModel,
    onSelectImage: () -> Unit
) {
    var selectedEditorTab by remember { mutableIntStateOf(0) }
    val editorTabs = listOf("✂️ Schnitt", "🖼️ Rahmen", "✍️ Text", "🎛️ Raster")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Bild auswählen / entfernen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSelectImage,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.selectedBitmap == null) "Bild auswählen" else "Neues Bild")
                }

                if (uiState.selectedBitmap != null) {
                    TextButton(onClick = { viewModel.setSelectedImageUri(null) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Entfernen")
                    }
                }
            }

            uiState.selectedBitmap?.let { bmp ->
                // Preview Container mit Live Druck-Vorschau Switch
                val displayBitmap = if (uiState.isPrintPreviewEnabled) {
                    uiState.ditherPreviewBitmap ?: bmp
                } else {
                    bmp
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Vorschau",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Overlay Pill für Druck-Vorschau Toggle
                    Surface(
                        onClick = { viewModel.togglePrintPreview() },
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.isPrintPreviewEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (uiState.isPrintPreviewEnabled) Icons.Default.Print else Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (uiState.isPrintPreviewEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isPrintPreviewEnabled) "Druck-Vorschau" else "Farbe",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isPrintPreviewEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Editor Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedEditorTab,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    editorTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedEditorTab == index,
                            onClick = { selectedEditorTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                // Tab Content
                when (selectedEditorTab) {
                    0 -> { // Zuschnitt & Drehung
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.rotateImageClockwise() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("90° Drehen")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.toggleImageFlipHorizontal() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Spiegeln")
                                }
                            }

                            Text("Seitenverhältnis (Zuschnitt):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CropAspect.entries.forEach { aspect ->
                                    FilterChip(
                                        selected = uiState.cropAspect == aspect,
                                        onClick = { viewModel.setCropAspect(aspect) },
                                        label = { Text(aspect.label) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // Rahmen
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Foto-Rahmen wählen:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PhotoFrame.entries.forEach { frame ->
                                    FilterChip(
                                        selected = uiState.photoFrame == frame,
                                        onClick = { viewModel.setPhotoFrame(frame) },
                                        label = { Text(frame.label) }
                                    )
                                }
                            }

                            if (uiState.photoFrame == PhotoFrame.POLAROID) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Tipp: Unter 'Text' kannst du eine Notiz im Polaroid-Fuß platzieren!",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                    2 -> { // Text & Deko
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = uiState.photoCaption,
                                onValueChange = { viewModel.setPhotoCaption(it) },
                                label = { Text("Beschriftung auf Foto") },
                                placeholder = { Text("Z.B. Urlaub 2026, Für Mama...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val now = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.GERMANY).format(java.util.Date())
                                        viewModel.setPhotoCaption(now)
                                    }) {
                                        Icon(Icons.Default.Schedule, contentDescription = "Datum")
                                    }
                                }
                            )

                            // Position der Beschriftung
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CaptionPosition.entries.forEach { pos ->
                                    if (pos != CaptionPosition.FOOTER || uiState.photoFrame == PhotoFrame.POLAROID) {
                                        FilterChip(
                                            selected = uiState.captionPosition == pos,
                                            onClick = { viewModel.setCaptionPosition(pos) },
                                            label = { Text(pos.label) }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Sticker / Emojis
                            Text("Deko-Stempel / Emoji:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            val stickers = listOf("❤️", "⭐", "🐱", "🐶", "☕", "🌸", "🎉", "🎁", "📌", "☀️", "🔥", "👍")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssistChip(
                                    onClick = { viewModel.setSticker("", StickerPosition.NONE) },
                                    label = { Text("Kein") }
                                )
                                stickers.forEach { emoji ->
                                    FilterChip(
                                        selected = uiState.stickerEmoji == emoji,
                                        onClick = {
                                            if (uiState.stickerEmoji == emoji) {
                                                viewModel.setSticker("", StickerPosition.NONE)
                                            } else {
                                                val pos = if (uiState.stickerPosition == StickerPosition.NONE) StickerPosition.BOTTOM_RIGHT else uiState.stickerPosition
                                                viewModel.setSticker(emoji, pos)
                                            }
                                        },
                                        label = { Text(emoji) }
                                    )
                                }
                            }

                            if (uiState.stickerEmoji.isNotBlank()) {
                                Text("Sticker-Position:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StickerPosition.entries.filter { it != StickerPosition.NONE }.forEach { pos ->
                                        FilterChip(
                                            selected = uiState.stickerPosition == pos,
                                            onClick = { viewModel.setSticker(uiState.stickerEmoji, pos) },
                                            label = { Text(pos.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Raster & Helligkeit
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Raster-Verfahren (Dithering):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.ditherMode == DitherMode.FLOYD_STEINBERG,
                                    onClick = { viewModel.setDitherMode(DitherMode.FLOYD_STEINBERG) },
                                    label = { Text("Foto") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = uiState.ditherMode == DitherMode.BAYER_ORDERED,
                                    onClick = { viewModel.setDitherMode(DitherMode.BAYER_ORDERED) },
                                    label = { Text("Retro") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = uiState.ditherMode == DitherMode.THRESHOLD,
                                    onClick = { viewModel.setDitherMode(DitherMode.THRESHOLD) },
                                    label = { Text("Strich") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Invertieren Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.InvertColors, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Farben invertieren (Negativ)", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(
                                    checked = uiState.isPhotoInverted,
                                    onCheckedChange = { viewModel.togglePhotoInverted() }
                                )
                            }

                            // Sliders
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Helligkeit", style = MaterialTheme.typography.bodySmall)
                                    Text("${uiState.brightness.toInt()}", style = MaterialTheme.typography.labelSmall)
                                }
                                Slider(
                                    value = uiState.brightness,
                                    onValueChange = { viewModel.setBrightness(it) },
                                    valueRange = -80f..80f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kontrast", style = MaterialTheme.typography.bodySmall)
                                    Text(String.format(java.util.Locale.US, "%.1fx", uiState.contrast), style = MaterialTheme.typography.labelSmall)
                                }
                                Slider(
                                    value = uiState.contrast,
                                    onValueChange = { viewModel.setContrast(it) },
                                    valueRange = 0.5f..2.0f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Drucken Button
                Button(
                    onClick = { viewModel.printCurrentImage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = uiState.connectionState is ConnectionState.Connected && !uiState.isPrinting
                ) {
                    if (uiState.isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Drucke Foto...")
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Foto jetzt drucken")
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Wähle ein Bild aus deiner Galerie",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REITER: TEXT & FREITEXT
// -------------------------------------------------------------
@Composable
private fun TextTabContent(
    uiState: com.example.openthermalprint.ui.UiState,
    viewModel: PrinterViewModel,
    onNavigateToTemplates: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onNavigateToTemplates,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Neu: Fertige Vorlagen für Gewürze, WLAN & Planer!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = uiState.textToPrint,
                onValueChange = { viewModel.setTextToPrint(it) },
                label = { Text("Text / Freitext zum Drucken") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Einkaufsliste, Notiz, Gedicht...") }
            )

            // Schnellvorlagen / Templates Toolbar
            Text("Schnelleinfügungen:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { viewModel.insertCheckbox() },
                    label = { Text("[ ] Checkbox") },
                    leadingIcon = { Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.insertDivider() },
                    label = { Text("── Trennlinie") },
                    leadingIcon = { Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = { viewModel.insertTimestamp() },
                    label = { Text("Datum/Zeit") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Typography formatting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = uiState.isBold,
                        onClick = { viewModel.setBold(!uiState.isBold) },
                        label = { Text("Fett") },
                        leadingIcon = { Icon(Icons.Default.FormatBold, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = uiState.isDoubleSize,
                        onClick = { viewModel.setDoubleSize(!uiState.isDoubleSize) },
                        label = { Text("Groß") },
                        leadingIcon = { Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { viewModel.setTextAlignment(EscPosEncoder.TextAlign.LEFT) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.FormatAlignLeft,
                            contentDescription = "Links",
                            tint = if (uiState.textAlignment == EscPosEncoder.TextAlign.LEFT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { viewModel.setTextAlignment(EscPosEncoder.TextAlign.CENTER) }) {
                        Icon(
                            Icons.Default.FormatAlignCenter,
                            contentDescription = "Mitte",
                            tint = if (uiState.textAlignment == EscPosEncoder.TextAlign.CENTER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { viewModel.setTextAlignment(EscPosEncoder.TextAlign.RIGHT) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.FormatAlignRight,
                            contentDescription = "Rechts",
                            tint = if (uiState.textAlignment == EscPosEncoder.TextAlign.RIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.printCurrentText() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.textToPrint.isNotBlank() && uiState.connectionState is ConnectionState.Connected && !uiState.isPrinting
            ) {
                if (uiState.isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Text jetzt drucken")
            }
        }
    }
}

// -------------------------------------------------------------
// REITER: QR-CODES & BARCODES
// -------------------------------------------------------------
@Composable
private fun BarcodeTabContent(
    uiState: com.example.openthermalprint.ui.UiState,
    viewModel: PrinterViewModel,
    onNavigateToTemplates: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                onClick = onNavigateToTemplates,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Tipp: Für WLAN-Gastkarten mit Design & Passwort nutze Vorlagen!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.isQrMode,
                    onClick = { viewModel.setIsQrMode(true) },
                    label = { Text("QR-Code") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !uiState.isQrMode,
                    onClick = { viewModel.setIsQrMode(false) },
                    label = { Text("Barcode 128") },
                    leadingIcon = { Icon(Icons.Default.ViewWeek, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = uiState.barcodeInput,
                onValueChange = { viewModel.setBarcodeInput(it) },
                label = { Text(if (uiState.isQrMode) "Web-Adresse / Text für QR-Code" else "Nummer / Text für Barcode") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Live generated preview
            val previewBmp = remember(uiState.barcodeInput, uiState.isQrMode) {
                if (uiState.isQrMode) {
                    BarcodeGenerator.createQrCode(uiState.barcodeInput, 240)
                } else {
                    BarcodeGenerator.createBarcode128(uiState.barcodeInput, 320, 80)
                }
            }

            previewBmp?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Code-Vorschau",
                        modifier = Modifier.padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Button(
                onClick = { viewModel.printCurrentBarcode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.barcodeInput.isNotBlank() && uiState.connectionState is ConnectionState.Connected && !uiState.isPrinting
            ) {
                if (uiState.isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Code jetzt drucken")
            }
        }
    }
}

// -------------------------------------------------------------
// REITER: EINSTELLUNGEN (AUFGERÄUMT IN 3 SEKTIONS-KARTEN)
// -------------------------------------------------------------
@Composable
private fun SettingsTabContent(
    uiState: com.example.openthermalprint.ui.UiState,
    viewModel: PrinterViewModel,
    onRequestScan: () -> Unit
) {
    val context = LocalContext.current
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val isConnecting = uiState.connectionState is ConnectionState.Connecting

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KARTE 1: DRUCKER & HARDWARE
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header mit Status-Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Drucker & Verbindung", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isConnected -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                            isConnecting -> Color(0xFFFFA000).copy(alpha = 0.15f)
                            else -> Color(0xFFD32F2F).copy(alpha = 0.15f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        when {
                                            isConnected -> Color(0xFF2E7D32)
                                            isConnecting -> Color(0xFFFFA000)
                                            else -> Color(0xFFD32F2F)
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isConnected -> "Verbunden"
                                    isConnecting -> "Verbinde..."
                                    else -> "Getrennt"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isConnected -> Color(0xFF2E7D32)
                                    isConnecting -> Color(0xFFFFA000)
                                    else -> Color(0xFFD32F2F)
                                }
                            )
                        }
                    }
                }

                // Verbinden / Trennen Button
                if (isConnected) {
                    OutlinedButton(
                        onClick = { viewModel.disconnect() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Drucker-Verbindung trennen")
                    }
                } else {
                    Button(
                        onClick = onRequestScan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Bluetooth-Drucker suchen")
                    }
                }

                HorizontalDivider()

                // Papiervorschub Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Abreiß-Vorschub", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${uiState.feedLines} Zeilen (~${(uiState.feedLines * 2.5).toInt()} mm)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "Schiebt den Bon nach dem Drucken über die Abreißkante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Slider(
                        value = uiState.feedLines.toFloat(),
                        onValueChange = { viewModel.setFeedLines(it.toInt()) },
                        valueRange = 4f..20f,
                        steps = 7,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                // Druckhitze / Dichte
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Druckdichte / Hitze:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.printDensity == 0,
                            onClick = { viewModel.setDensity(0) },
                            label = { Text("Hell") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.printDensity == 1,
                            onClick = { viewModel.setDensity(1) },
                            label = { Text("Normal") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.printDensity == 2,
                            onClick = { viewModel.setDensity(2) },
                            label = { Text("Dunkel") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Schnell-Aktionen: Vorschub & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.feedPaper(uiState.feedLines) },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Vorschub")
                    }

                    FilledTonalButton(
                        onClick = { viewModel.initPrinter() },
                        modifier = Modifier.weight(1f),
                        enabled = isConnected
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset")
                    }
                }
            }
        }

        // KARTE 2: DARSTELLUNG & DESIGN-THEMES
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Design & Farbschema", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        when (uiState.themeMode) {
                            ThemeMode.NORDIC -> "Nordic Paper"
                            ThemeMode.AURORA -> "Aurora Glass"
                            ThemeMode.LIGHT -> "Hell"
                            ThemeMode.DARK -> "Dunkel"
                            ThemeMode.SYSTEM -> "System"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text("Signature Themes:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.NORDIC,
                        onClick = { viewModel.setThemeMode(ThemeMode.NORDIC) },
                        label = { Text("📜 Nordic Paper") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.AURORA,
                        onClick = { viewModel.setThemeMode(ThemeMode.AURORA) },
                        label = { Text("✨ Aurora Glass") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Standard Modus:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("Hell") },
                        leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text("Dunkel") },
                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("System") },
                        leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mini Farbpalette
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }

        // KARTE 3: APP-INFO & GITHUB-UPDATER
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("PocketPrint", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Daddelgreis74/PocketPrint", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "v${com.example.openthermalprint.update.GitHubUpdater.getCurrentVersion(context)}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (uiState.updateStatus != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.updateStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.checkForUpdates(silent = false) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isCheckingUpdate
                ) {
                    if (uiState.isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (uiState.isCheckingUpdate) "Prüfe GitHub..." else "Nach App-Updates suchen")
                }
            }
        }
    }
}
