package com.example.openthermalprint.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.openthermalprint.bluetooth.BluetoothPrinterManager
import com.example.openthermalprint.bluetooth.ConnectionState
import com.example.openthermalprint.bluetooth.PrinterDevice
import com.example.openthermalprint.protocol.BarcodeGenerator
import com.example.openthermalprint.protocol.DitherMode
import com.example.openthermalprint.protocol.EscPosEncoder
import com.example.openthermalprint.ui.photo.*
import com.example.openthermalprint.update.GitHubUpdater
import com.example.openthermalprint.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ThemeMode {
    NORDIC,
    AURORA,
    SYSTEM,
    LIGHT,
    DARK
}

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)

data class UiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val devices: List<PrinterDevice> = emptyList(),
    val isScanning: Boolean = false,
    val statusMessage: String? = null,
    val isPrinting: Boolean = false,
    // Theme Mode
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Einkaufsliste
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val newShoppingItemText: String = "",
    // Schnell-Notiz
    val noteTitle: String = "",
    val noteContent: String = "",
    val noteCategory: String = "📌 Notiz",
    // Image editing & printing
    val selectedImageUri: Uri? = null,
    val rawImageBitmap: Bitmap? = null,
    val selectedBitmap: Bitmap? = null,
    val imageRotation: Int = 0,
    val isImageFlippedHorizontal: Boolean = false,
    val cropAspect: CropAspect = CropAspect.ORIGINAL,
    val photoFrame: PhotoFrame = PhotoFrame.NONE,
    val photoCaption: String = "",
    val captionPosition: CaptionPosition = CaptionPosition.BOTTOM,
    val stickerEmoji: String = "",
    val stickerPosition: StickerPosition = StickerPosition.NONE,
    val isPrintPreviewEnabled: Boolean = false,
    val isPhotoInverted: Boolean = false,
    val ditherPreviewBitmap: Bitmap? = null,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val ditherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
    // Text printing
    val textToPrint: String = "",
    val textAlignment: EscPosEncoder.TextAlign = EscPosEncoder.TextAlign.LEFT,
    val isBold: Boolean = false,
    val isDoubleSize: Boolean = false,
    // Barcode / QR
    val barcodeInput: String = "",
    val isQrMode: Boolean = true, // true = QR-Code, false = Barcode 128
    // Hardware settings
    val printDensity: Int = 1, // 0 = Light, 1 = Medium, 2 = Dark
    val feedLines: Int = 8, // Default 8 lines to push print past tear-off bar
    // Update Checker
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: UpdateInfo? = null,
    val updateStatus: String? = null
)

class PrinterViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "pocketprint_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PRINT_DENSITY = "print_density"
        private const val KEY_FEED_LINES = "feed_lines"
        private const val KEY_DITHER_MODE = "dither_mode"
        private const val KEY_SHOPPING_ITEMS = "shopping_items"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadSavedThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return try {
            ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun loadSavedDitherMode(): DitherMode {
        val saved = prefs.getString(KEY_DITHER_MODE, null) ?: return DitherMode.FLOYD_STEINBERG
        return try {
            DitherMode.valueOf(saved)
        } catch (_: Exception) {
            DitherMode.FLOYD_STEINBERG
        }
    }

    private fun loadShoppingItems(): List<ShoppingItem> {
        val raw = prefs.getString(KEY_SHOPPING_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ShoppingItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ShoppingItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveShoppingItems(items: List<ShoppingItem>) {
        try {
            val array = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("isChecked", item.isChecked)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_SHOPPING_ITEMS, array.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private val printerManager = BluetoothPrinterManager(application)

    private val _uiState = MutableStateFlow(
        UiState(
            themeMode = loadSavedThemeMode(),
            printDensity = prefs.getInt(KEY_PRINT_DENSITY, 1),
            feedLines = prefs.getInt(KEY_FEED_LINES, 8),
            ditherMode = loadSavedDitherMode(),
            shoppingItems = loadShoppingItems()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            printerManager.connectionState.collect { connState ->
                _uiState.value = _uiState.value.copy(
                    connectionState = connState,
                    // Keine aufdringlichen Verbindungs-/Fehler-Banner auf der Startseite:
                    // Nur echte Druck-Statusmeldungen anzeigen
                    statusMessage = when (connState) {
                        is ConnectionState.Connected -> "Drucker verbunden"
                        else -> null
                    }
                )
            }
        }

        viewModelScope.launch {
            printerManager.discoveredDevices.collect { devs ->
                _uiState.value = _uiState.value.copy(devices = devs)
                // Auto-connect to known Denver / YHK printer if disconnected
                if (_uiState.value.connectionState is ConnectionState.Disconnected) {
                    val target = devs.firstOrNull {
                        it.name.contains("MBP", ignoreCase = true) ||
                        it.name.contains("YHK", ignoreCase = true) ||
                        it.name.contains("Denver", ignoreCase = true) ||
                        it.name.contains("Walk", ignoreCase = true)
                    }
                    target?.let {
                        connectDevice(it.address)
                    }
                }
            }
        }

        viewModelScope.launch {
            printerManager.isScanning.collect { scanning ->
                _uiState.value = _uiState.value.copy(isScanning = scanning)
            }
        }

        // Automatische Update-Prüfung im Hintergrund beim App-Start
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingUpdate = true,
                updateStatus = if (silent) null else "Prüfe GitHub auf Updates..."
            )

            val result = GitHubUpdater.checkForUpdate(getApplication())
            result.onSuccess { info ->
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    availableUpdate = if (info.hasUpdate) info else null,
                    updateStatus = if (info.hasUpdate) {
                        "Neues Update v${info.latestVersion} verfügbar!"
                    } else if (!silent) {
                        "PocketPrint ist auf dem neuesten Stand (v${info.currentVersion})."
                    } else {
                        null
                    }
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    updateStatus = if (!silent) "Update-Prüfung fehlgeschlagen: ${err.message}" else null
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(availableUpdate = null)
    }

    fun startUpdateDownload() {
        val update = _uiState.value.availableUpdate ?: return
        val url = update.downloadUrl ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                availableUpdate = null,
                updateStatus = "Lade Update v${update.latestVersion} herunter..."
            )

            val apkFile = GitHubUpdater.downloadApk(
                context = getApplication(),
                downloadUrl = url
            ) { progress ->
                val percent = (progress * 100).toInt()
                _uiState.value = _uiState.value.copy(updateStatus = "Download: $percent%")
            }

            if (apkFile != null) {
                _uiState.value = _uiState.value.copy(updateStatus = "Starte Installation...")
                GitHubUpdater.startInstallation(getApplication(), apkFile)
            } else {
                _uiState.value = _uiState.value.copy(updateStatus = "Fehler beim Herunterladen des Updates.")
            }
        }
    }

    fun startScan() {
        printerManager.startScan()
    }

    fun stopScan() {
        printerManager.stopScan()
    }

    fun connectDevice(address: String) {
        viewModelScope.launch {
            printerManager.connect(address)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            printerManager.disconnect()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    // --- Einkaufsliste Handlers ---
    fun setNewShoppingItemText(text: String) {
        _uiState.value = _uiState.value.copy(newShoppingItemText = text)
    }

    fun addShoppingItem(text: String = _uiState.value.newShoppingItemText) {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            val updated = _uiState.value.shoppingItems + ShoppingItem(text = trimmed)
            _uiState.value = _uiState.value.copy(shoppingItems = updated, newShoppingItemText = "")
            saveShoppingItems(updated)
        }
    }

    /**
     * Zerlegt gesprochenen Text anhand von Kommas, Zeilenumbrüchen und Bindewörtern ("und", "sowie", "plus")
     * und fügt jeden Artikel einzeln zur Einkaufsliste hinzu.
     */
    fun addSpokenShoppingItems(spokenText: String) {
        if (spokenText.isBlank()) return

        // Trenne an Kommas, Zeilenumbrüchen, Punkten, " und ", " sowie ", " plus "
        val splitRegex = Regex("""(?i),\s*|\n+|\.\s*|\s+(?:und|sowie|plus)\s+""")
        val items = spokenText.split(splitRegex)
            .map { it.trim().trim('.', ',', '-') }
            .filter { it.isNotBlank() }

        if (items.isNotEmpty()) {
            val newItems = items.map { ShoppingItem(text = it.replaceFirstChar { char -> char.uppercase() }) }
            val updated = _uiState.value.shoppingItems + newItems
            _uiState.value = _uiState.value.copy(
                shoppingItems = updated,
                statusMessage = "${items.size} Artikel per Sprache hinzugefügt"
            )
            saveShoppingItems(updated)
        }
    }

    fun clearShoppingList() {
        _uiState.value = _uiState.value.copy(shoppingItems = emptyList())
        saveShoppingItems(emptyList())
    }

    fun toggleShoppingItem(id: String) {
        val updated = _uiState.value.shoppingItems.map {
            if (it.id == id) it.copy(isChecked = !it.isChecked) else it
        }
        _uiState.value = _uiState.value.copy(shoppingItems = updated)
        saveShoppingItems(updated)
    }

    fun removeShoppingItem(id: String) {
        val updated = _uiState.value.shoppingItems.filter { it.id != id }
        _uiState.value = _uiState.value.copy(shoppingItems = updated)
        saveShoppingItems(updated)
    }

    fun clearCheckedShoppingItems() {
        val updated = _uiState.value.shoppingItems.filter { !it.isChecked }
        _uiState.value = _uiState.value.copy(shoppingItems = updated)
        saveShoppingItems(updated)
    }

    fun printShoppingList() {
        val items = _uiState.value.shoppingItems
        if (items.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Einkaufsliste ist leer")
            return
        }

        val dateStr = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.GERMANY).format(Date())
        val sb = StringBuilder()
        sb.append("      🛒 EINKAUFSLISTE\n")
        sb.append("   $dateStr Uhr\n")
        sb.append("──────────────────────────────\n\n")

        items.forEach { item ->
            val box = if (item.isChecked) "[X]" else "[ ]"
            sb.append("$box  ${item.text}\n")
        }

        sb.append("\n──────────────────────────────\n")
        sb.append("Gesamt: ${items.size} Artikel\n")
        sb.append("Viel Erfolg beim Einkauf!\n")

        printFormattedText(sb.toString(), EscPosEncoder.TextAlign.LEFT)
    }

    // --- Notiz Handlers ---
    fun setNoteTitle(title: String) {
        _uiState.value = _uiState.value.copy(noteTitle = title)
    }

    fun setNoteContent(content: String) {
        _uiState.value = _uiState.value.copy(noteContent = content)
    }

    fun setNoteCategory(category: String) {
        _uiState.value = _uiState.value.copy(noteCategory = category)
    }

    fun printNote() {
        val title = _uiState.value.noteTitle.trim()
        val content = _uiState.value.noteContent.trim()
        val category = _uiState.value.noteCategory
        if (content.isBlank() && title.isBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Notiz ist leer")
            return
        }

        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        val sb = StringBuilder()
        sb.append("==============================\n")
        sb.append("   $category $title\n")
        sb.append("==============================\n")
        sb.append("Datum: $dateStr\n")
        sb.append("──────────────────────────────\n\n")
        sb.append(content)
        sb.append("\n\n──────────────────────────────\n")

        printFormattedText(sb.toString(), EscPosEncoder.TextAlign.LEFT)
    }

    // --- Text & Template Handlers ---
    fun setTextToPrint(text: String) {
        _uiState.value = _uiState.value.copy(textToPrint = text)
    }

    fun setTextAlignment(align: EscPosEncoder.TextAlign) {
        _uiState.value = _uiState.value.copy(textAlignment = align)
    }

    fun setBold(bold: Boolean) {
        _uiState.value = _uiState.value.copy(isBold = bold)
    }

    fun setDoubleSize(doubleSize: Boolean) {
        _uiState.value = _uiState.value.copy(isDoubleSize = doubleSize)
    }

    fun insertCheckbox() {
        val current = _uiState.value.textToPrint
        val prefix = if (current.isNotEmpty() && !current.endsWith("\n")) "\n" else ""
        _uiState.value = _uiState.value.copy(textToPrint = "$current$prefix[ ] ")
    }

    fun insertDivider() {
        val current = _uiState.value.textToPrint
        val prefix = if (current.isNotEmpty() && !current.endsWith("\n")) "\n" else ""
        _uiState.value = _uiState.value.copy(textToPrint = "$current$prefix──────────────────────────────\n")
    }

    fun insertTimestamp() {
        val current = _uiState.value.textToPrint
        val prefix = if (current.isNotEmpty() && !current.endsWith("\n")) "\n" else ""
        val now = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date())
        _uiState.value = _uiState.value.copy(textToPrint = "$current$prefix📅 $now\n")
    }

    // --- Image & Processing Handlers ---
    fun setDitherMode(mode: DitherMode) {
        _uiState.value = _uiState.value.copy(ditherMode = mode)
        prefs.edit().putString(KEY_DITHER_MODE, mode.name).apply()
        recomputeEditedBitmap()
    }

    fun setBrightness(brightness: Float) {
        _uiState.value = _uiState.value.copy(brightness = brightness)
        recomputeEditedBitmap()
    }

    fun setContrast(contrast: Float) {
        _uiState.value = _uiState.value.copy(contrast = contrast)
        recomputeEditedBitmap()
    }

    fun rotateImageClockwise() {
        val next = (_uiState.value.imageRotation + 90) % 360
        _uiState.value = _uiState.value.copy(imageRotation = next)
        recomputeEditedBitmap()
    }

    fun toggleImageFlipHorizontal() {
        _uiState.value = _uiState.value.copy(isImageFlippedHorizontal = !_uiState.value.isImageFlippedHorizontal)
        recomputeEditedBitmap()
    }

    fun setCropAspect(aspect: CropAspect) {
        _uiState.value = _uiState.value.copy(cropAspect = aspect)
        recomputeEditedBitmap()
    }

    fun setPhotoFrame(frame: PhotoFrame) {
        _uiState.value = _uiState.value.copy(photoFrame = frame)
        recomputeEditedBitmap()
    }

    fun setPhotoCaption(caption: String) {
        _uiState.value = _uiState.value.copy(photoCaption = caption)
        recomputeEditedBitmap()
    }

    fun setCaptionPosition(position: CaptionPosition) {
        _uiState.value = _uiState.value.copy(captionPosition = position)
        recomputeEditedBitmap()
    }

    fun setSticker(emoji: String, position: StickerPosition = StickerPosition.BOTTOM_RIGHT) {
        _uiState.value = _uiState.value.copy(stickerEmoji = emoji, stickerPosition = position)
        recomputeEditedBitmap()
    }

    fun togglePrintPreview() {
        _uiState.value = _uiState.value.copy(isPrintPreviewEnabled = !_uiState.value.isPrintPreviewEnabled)
    }

    fun togglePhotoInverted() {
        _uiState.value = _uiState.value.copy(isPhotoInverted = !_uiState.value.isPhotoInverted)
        recomputeEditedBitmap()
    }

    private var imageProcessingJob: kotlinx.coroutines.Job? = null

    private fun recomputeEditedBitmap() {
        val raw = _uiState.value.rawImageBitmap ?: return

        imageProcessingJob?.cancel()
        imageProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            val currentState = _uiState.value

            val transformed = PhotoEditorHelper.transformBitmap(
                source = raw,
                rotationDegrees = currentState.imageRotation,
                flipHorizontal = currentState.isImageFlippedHorizontal
            )

            val cropped = PhotoEditorHelper.cropToAspect(
                source = transformed,
                aspect = currentState.cropAspect
            )

            val composed = PhotoEditorHelper.applyFrameAndDecorations(
                source = cropped,
                frame = currentState.photoFrame,
                captionText = currentState.photoCaption,
                captionPos = currentState.captionPosition,
                stickerEmoji = currentState.stickerEmoji,
                stickerPos = currentState.stickerPosition
            )

            val ditherPreview = PhotoEditorHelper.createDitherPreview(
                source = composed,
                targetWidth = 384,
                ditherMode = currentState.ditherMode,
                brightness = currentState.brightness,
                contrast = currentState.contrast,
                invert = currentState.isPhotoInverted
            )

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    selectedBitmap = composed,
                    ditherPreviewBitmap = ditherPreview
                )
            }
        }
    }

    fun setDensity(density: Int) {
        _uiState.value = _uiState.value.copy(printDensity = density)
        prefs.edit().putInt(KEY_PRINT_DENSITY, density).apply()
        viewModelScope.launch {
            if (_uiState.value.connectionState is ConnectionState.Connected) {
                printerManager.sendBytes(EscPosEncoder.setDensity(density))
            }
        }
    }

    fun setFeedLines(lines: Int) {
        val safeLines = lines.coerceIn(2, 25)
        _uiState.value = _uiState.value.copy(feedLines = safeLines)
        prefs.edit().putInt(KEY_FEED_LINES, safeLines).apply()
    }

    // --- Barcode / QR Handlers ---
    fun setBarcodeInput(input: String) {
        _uiState.value = _uiState.value.copy(barcodeInput = input)
    }

    fun setIsQrMode(isQr: Boolean) {
        _uiState.value = _uiState.value.copy(isQrMode = isQr)
    }

    fun setSelectedImageUri(uri: Uri?) {
        if (uri == null) {
            _uiState.value = _uiState.value.copy(
                selectedImageUri = null,
                rawImageBitmap = null,
                selectedBitmap = null,
                ditherPreviewBitmap = null
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        selectedImageUri = uri,
                        rawImageBitmap = bitmap,
                        imageRotation = 0,
                        isImageFlippedHorizontal = false,
                        cropAspect = CropAspect.ORIGINAL,
                        photoFrame = PhotoFrame.NONE,
                        photoCaption = "",
                        stickerEmoji = "",
                        stickerPosition = StickerPosition.NONE
                    )
                    recomputeEditedBitmap()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Bild konnte nicht geladen werden: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun feedPaper(lines: Int = _uiState.value.feedLines) {
        viewModelScope.launch {
            val cmd = EscPosEncoder.feedPaper(lines)
            printerManager.sendBytes(cmd)
        }
    }

    fun initPrinter() {
        viewModelScope.launch {
            val cmd = EscPosEncoder.CMD_INIT
            printerManager.sendBytes(cmd)
            _uiState.value = _uiState.value.copy(statusMessage = "Drucker initialisiert")
        }
    }

    private fun printFormattedText(
        text: String,
        align: EscPosEncoder.TextAlign = EscPosEncoder.TextAlign.LEFT,
        bold: Boolean = false,
        doubleSize: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isPrinting = true, statusMessage = "Bereite Druck vor...")
            }

            val textBitmap = EscPosEncoder.renderTextToBitmap(
                text = text,
                targetWidth = 384,
                textSizeSp = if (doubleSize) 32f else 24f,
                align = align,
                bold = bold,
                doubleSize = doubleSize
            )

            val bytes = EscPosEncoder.bitmapToEscPosRaster(
                source = textBitmap,
                targetWidth = 384,
                ditherMode = DitherMode.THRESHOLD,
                feedLines = _uiState.value.feedLines
            )

            val res = printerManager.sendBytes(bytes)

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    statusMessage = if (res.isSuccess) "Erfolgreich gedruckt!" else "Druckfehler: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun printCurrentText() {
        val text = _uiState.value.textToPrint
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Bitte Text eingeben")
            return
        }
        printFormattedText(
            text = text,
            align = _uiState.value.textAlignment,
            bold = _uiState.value.isBold,
            doubleSize = _uiState.value.isDoubleSize
        )
    }

    fun printCurrentImage() {
        val bmp = _uiState.value.selectedBitmap
        if (bmp == null) {
            _uiState.value = _uiState.value.copy(statusMessage = "Kein Bild ausgewählt")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = true,
                    statusMessage = "Rastere und drucke Bild..."
                )
            }

            val bytes = EscPosEncoder.bitmapToEscPosRaster(
                source = bmp,
                targetWidth = 384,
                ditherMode = _uiState.value.ditherMode,
                brightness = _uiState.value.brightness,
                contrast = _uiState.value.contrast,
                feedLines = _uiState.value.feedLines,
                invert = _uiState.value.isPhotoInverted
            )

            val res = printerManager.sendBytes(bytes)

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    statusMessage = if (res.isSuccess) "Bild erfolgreich gedruckt!" else "Druckfehler: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun printCurrentBarcode() {
        val input = _uiState.value.barcodeInput
        if (input.isBlank()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Bitte Inhalt für Barcode / QR-Code eingeben")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isPrinting = true, statusMessage = "Erzeuge Code und drucke...")
            }

            val codeBmp = if (_uiState.value.isQrMode) {
                BarcodeGenerator.createQrCode(input, 320)
            } else {
                BarcodeGenerator.createBarcode128(input, 360, 90)
            }

            if (codeBmp == null) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        statusMessage = "Fehler: Ungültiges Zeichenformat für Barcode"
                    )
                }
                return@launch
            }

            val bytes = EscPosEncoder.bitmapToEscPosRaster(
                source = codeBmp,
                targetWidth = 384,
                ditherMode = DitherMode.THRESHOLD,
                feedLines = _uiState.value.feedLines
            )

            val res = printerManager.sendBytes(bytes)

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    statusMessage = if (res.isSuccess) "Code erfolgreich gedruckt!" else "Druckfehler: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        printerManager.stopScan()
    }
}
