package com.example.openthermalprint.ui.templates

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.openthermalprint.bluetooth.ConnectionState
import com.example.openthermalprint.ui.PrinterViewModel
import com.example.openthermalprint.ui.UiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TemplatesTabContent(
    uiState: UiState,
    viewModel: PrinterViewModel
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("🏷️ Vorrat & Gewürze", "📛 Namensschilder", "📶 WLAN-Gastkarte", "📋 Planer & Listen")

    // State für Gewürze & Vorratsdosen
    var spiceTitle by remember { mutableStateOf("BASILIKUM") }
    var spiceSubtitle by remember { mutableStateOf("Gerebelt & Getrocknet") }
    var spiceMhd by remember { mutableStateOf("MHD: 12/2027") }
    var spiceStyle by remember { mutableStateOf(SpiceLabelStyle.VINTAGE_FLORAL) }

    // State für Namensschilder
    var badgeGreeting by remember { mutableStateOf("HALLO") }
    var badgeName by remember { mutableStateOf("Max Mustermann") }
    var badgeSubtitle by remember { mutableStateOf("PocketPrint Team") }
    var badgeStyle by remember { mutableStateOf(BadgeStyle.HELLO_BADGE) }

    // State für WLAN-Gastkarte
    var wifiSsid by remember { mutableStateOf("MeinHeimnetz_Gast") }
    var wifiPassword by remember { mutableStateOf("Willkommen2026!") }
    var wifiSecurity by remember { mutableStateOf(WifiSecurity.WPA_WPA2) }
    var wifiTitle by remember { mutableStateOf("GÄSTE-WLAN") }
    var wifiNote by remember { mutableStateOf("Kamera scannen zum Verbinden") }

    // State für Planer & To-Do
    val todayDateStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date()) }
    var plannerType by remember { mutableStateOf(PlannerType.DAILY_FOCUS) }
    var plannerDate by remember { mutableStateOf("Datum: $todayDateStr") }
    var customTask1 by remember { mutableStateOf("Projekt abschließen") }
    var customTask2 by remember { mutableStateOf("Einkauf erledigen") }
    var customTask3 by remember { mutableStateOf("Paket bei Post abgeben") }

    // Generierte Live-Vorschau-Bitmap
    val previewBitmap: Bitmap? = remember(
        selectedCategoryIndex,
        spiceTitle, spiceSubtitle, spiceMhd, spiceStyle,
        badgeGreeting, badgeName, badgeSubtitle, badgeStyle,
        wifiSsid, wifiPassword, wifiSecurity, wifiTitle, wifiNote,
        plannerType, plannerDate, customTask1, customTask2, customTask3
    ) {
        when (selectedCategoryIndex) {
            0 -> TemplateGenerator.generateSpiceLabel(spiceTitle, spiceSubtitle, spiceMhd, spiceStyle)
            1 -> TemplateGenerator.generateNameBadge(badgeGreeting, badgeName, badgeSubtitle, badgeStyle)
            2 -> TemplateGenerator.generateWifiCard(wifiSsid, wifiPassword, wifiSecurity, wifiTitle, wifiNote)
            3 -> {
                val tasks = listOf(customTask1, customTask2, customTask3)
                TemplateGenerator.generatePlanner(plannerType, plannerDate, tasks)
            }
            else -> null
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Horizontale Kategorie-Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(14.dp))
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Thermodruck-Vorschau (WYSIWYG-Papier)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Thermodruck-Vorschau (384 px)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "100% WYSIWYG",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bonrollen-Papier-Simulation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Thermodruck-Vorschau",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Eingabefelder je nach gewählter Kategorie
        when (selectedCategoryIndex) {
            // --- 1. GEWÜRZE & VORRAT ---
            0 -> {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Schnell-Auswahl:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TemplatePresets.SPICES.forEach { preset ->
                                AssistChip(
                                    onClick = {
                                        spiceTitle = preset.name
                                        spiceSubtitle = preset.subtitle
                                        spiceMhd = preset.mhd
                                    },
                                    label = { Text(preset.name) }
                                )
                            }
                        }

                        // Stil-Auswahl
                        Text("Rahmen & Design-Stil:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpiceLabelStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = spiceStyle == style,
                                    onClick = { spiceStyle = style },
                                    label = { Text(style.displayName) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = spiceTitle,
                            onValueChange = { spiceTitle = it.uppercase() },
                            label = { Text("Gewürz / Vorrats-Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = spiceSubtitle,
                            onValueChange = { spiceSubtitle = it },
                            label = { Text("Zusatz / Sorte (z. B. Gerebelt, Bio)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = spiceMhd,
                                onValueChange = { spiceMhd = it },
                                label = { Text("MHD / Abfülldatum") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilledTonalButton(
                                onClick = { spiceMhd = "MHD: 12/2027" },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("+ 2 J.")
                            }
                        }
                    }
                }
            }

            // --- 2. NAMENSSCHILDER & TISCHKARTEN ---
            1 -> {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stil-Auswahl
                        Text("Schild-Typ:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BadgeStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = badgeStyle == style,
                                    onClick = { badgeStyle = style },
                                    label = { Text(style.displayName) }
                                )
                            }
                        }

                        // Gruß / Header Vorlagen
                        Text("Begrüßung / Header:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TemplatePresets.BADGE_GREETINGS.forEach { greet ->
                                AssistChip(
                                    onClick = { badgeGreeting = greet },
                                    label = { Text(greet) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = badgeGreeting,
                            onValueChange = { badgeGreeting = it },
                            label = { Text("Header-Text / Gruß") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = badgeName,
                            onValueChange = { badgeName = it },
                            label = { Text("Vor- & Nachname / Tischnummer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = badgeSubtitle,
                            onValueChange = { badgeSubtitle = it },
                            label = { Text("Rolle / Firma / Anlass") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // --- 3. WLAN-GASTKARTE MIT AUTOMATISCHEM CONNECT-QR ---
            2 -> {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("WLAN-Verbindungsdaten:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        // Verschlüsselungs-Auswahl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WifiSecurity.entries.forEach { sec ->
                                FilterChip(
                                    selected = wifiSecurity == sec,
                                    onClick = { wifiSecurity = sec },
                                    label = { Text(sec.displayName) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("WLAN-Netzwerkname (SSID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (wifiSecurity != WifiSecurity.OPEN) {
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("WLAN-Passwort") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = wifiTitle,
                            onValueChange = { wifiTitle = it },
                            label = { Text("Karten-Titel (z. B. Gäste-WLAN)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = wifiNote,
                            onValueChange = { wifiNote = it },
                            label = { Text("Hinweis-Text unten") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // --- 4. PLANER & LISTEN ---
            3 -> {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Planer-Typ Auswahl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlannerType.entries.forEach { pType ->
                                FilterChip(
                                    selected = plannerType == pType,
                                    onClick = { plannerType = pType },
                                    label = { Text(pType.displayName) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = plannerDate,
                                onValueChange = { plannerDate = it },
                                label = { Text("Datum / Kalenderwoche") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilledTonalButton(
                                onClick = { plannerDate = "Datum: $todayDateStr" },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Heute")
                            }
                        }

                        if (plannerType == PlannerType.DAILY_FOCUS) {
                            Text("Eigene Aufgaben (optional, sonst leere Zeilen):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            OutlinedTextField(
                                value = customTask1,
                                onValueChange = { customTask1 = it },
                                label = { Text("Aufgabe 1") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = customTask2,
                                onValueChange = { customTask2 = it },
                                label = { Text("Aufgabe 2") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = customTask3,
                                onValueChange = { customTask3 = it },
                                label = { Text("Aufgabe 3") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Druck-Button
        val isConnected = uiState.connectionState is ConnectionState.Connected
        val labelType = categories[selectedCategoryIndex].replace(Regex("[^a-zA-ZäöüÄÖÜß &]"), "").trim()

        Button(
            onClick = {
                previewBitmap?.let { bmp ->
                    viewModel.printCustomBitmap(bmp, labelType)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = previewBitmap != null && isConnected && !uiState.isPrinting
        ) {
            if (uiState.isPrinting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Wird gedruckt...")
            } else {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("$labelType jetzt drucken")
            }
        }
    }
}
