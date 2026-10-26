package com.example.openthermalprint.ui.templates

import android.graphics.*
import com.example.openthermalprint.protocol.BarcodeGenerator

object TemplateGenerator {

    private const val CANVAS_WIDTH = 384

    // -------------------------------------------------------------
    // 1. VORRATSDOSEN- & GEWÜRZ-ETIKETTEN
    // -------------------------------------------------------------
    fun generateSpiceLabel(
        title: String,
        subtitle: String,
        mhd: String,
        labelStyle: SpiceLabelStyle
    ): Bitmap {
        val height = when (labelStyle) {
            SpiceLabelStyle.CHALKBOARD_BANNER -> 240
            SpiceLabelStyle.VINTAGE_FLORAL -> 230
            SpiceLabelStyle.SKANDI_CLEAN -> 220
            SpiceLabelStyle.RUSTIC_BORDER -> 220
        }

        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        when (labelStyle) {
            SpiceLabelStyle.VINTAGE_FLORAL -> {
                // Äußerer Rahmen (3px)
                val stroke3 = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRect(8f, 8f, (CANVAS_WIDTH - 8).toFloat(), (height - 8).toFloat(), stroke3)

                // Innerer Rahmen (1px)
                val stroke1 = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRect(14f, 14f, (CANVAS_WIDTH - 14).toFloat(), (height - 14).toFloat(), stroke1)

                // Eck-Rauten
                drawCornerDiamonds(canvas, 14f, 14f, (CANVAS_WIDTH - 14).toFloat(), (height - 14).toFloat(), blackPaint)

                // Zierschmuck oben
                val ornPaint = Paint(blackPaint).apply {
                    textSize = 14f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }
                canvas.drawText("✦  KÜCHEN-VORRAT  ✦", CANVAS_WIDTH / 2f, 38f, ornPaint)

                // Titel (groß, Serif fett)
                val titlePaint = Paint(blackPaint).apply {
                    textSize = 30f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, title.ifBlank { "GEWÜRZNAME" }, CANVAS_WIDTH / 2f, 92f, 330f, 30f, 18f, titlePaint)

                // Schnörkel-Trennlinie
                canvas.drawLine(50f, 114f, 160f, 114f, stroke1)
                canvas.drawCircle(CANVAS_WIDTH / 2f, 114f, 3f, blackPaint)
                canvas.drawLine(224f, 114f, 334f, 114f, stroke1)

                // Untertitel
                val subPaint = Paint(blackPaint).apply {
                    textSize = 17f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, subtitle.ifBlank { "Gerebelt & Getrocknet" }, CANVAS_WIDTH / 2f, 150f, 330f, 17f, 13f, subPaint)

                // MHD
                val mhdPaint = Paint(blackPaint).apply {
                    textSize = 15f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(mhd.ifBlank { "MHD: 12/2027" }, CANVAS_WIDTH / 2f, 195f, mhdPaint)
            }

            SpiceLabelStyle.SKANDI_CLEAN -> {
                // Abgerundeter moderner Kasten
                val strokeRound = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val rectF = RectF(10f, 10f, (CANVAS_WIDTH - 10).toFloat(), (height - 10).toFloat())
                canvas.drawRoundRect(rectF, 16f, 16f, strokeRound)

                // Header-Pill
                val tagPaint = Paint(blackPaint).apply {
                    textSize = 13f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("P A N T R Y  •  O R G A N I C", CANVAS_WIDTH / 2f, 40f, tagPaint)

                // Titel (Sans-serif Bold)
                val titlePaint = Paint(blackPaint).apply {
                    textSize = 32f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, title.ifBlank { "OREGANO" }, CANVAS_WIDTH / 2f, 96f, 330f, 32f, 20f, titlePaint)

                // Moderne dicke Akzentlinie
                val barPaint = Paint(blackPaint).apply {
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF((CANVAS_WIDTH / 2f) - 25f, 114f, (CANVAS_WIDTH / 2f) + 25f, 118f), 2f, 2f, barPaint)

                // Untertitel
                val subPaint = Paint(blackPaint).apply {
                    textSize = 16f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, subtitle.ifBlank { "Bio-Qualität" }, CANVAS_WIDTH / 2f, 150f, 330f, 16f, 12f, subPaint)

                // MHD in Kapsel
                val capsuleRect = RectF((CANVAS_WIDTH / 2f) - 75f, 172f, (CANVAS_WIDTH / 2f) + 75f, 198f)
                val capsuleStroke = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                canvas.drawRoundRect(capsuleRect, 13f, 13f, capsuleStroke)

                val mhdPaint = Paint(blackPaint).apply {
                    textSize = 13f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(mhd.ifBlank { "MHD: 10/2027" }, CANVAS_WIDTH / 2f, 190f, mhdPaint)
            }

            SpiceLabelStyle.CHALKBOARD_BANNER -> {
                // Ganzer Rahmen
                val strokeOuter = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRect(8f, 8f, (CANVAS_WIDTH - 8).toFloat(), (height - 8).toFloat(), strokeOuter)

                // Schwarzer Header-Balken oben
                val fillBlack = Paint(blackPaint).apply {
                    style = Paint.Style.FILL
                }
                canvas.drawRect(8f, 8f, (CANVAS_WIDTH - 8).toFloat(), 68f, fillBlack)

                // Weißer Banner-Text
                val whitePaint = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    textSize = 20f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("★  K Ü C H E N V O R R A T  ★", CANVAS_WIDTH / 2f, 44f, whitePaint)

                // Titel in Schwarz
                val titlePaint = Paint(blackPaint).apply {
                    textSize = 32f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, title.ifBlank { "PAPRIKA EDELSÜSS" }, CANVAS_WIDTH / 2f, 124f, 340f, 32f, 18f, titlePaint)

                // Dünne Trennlinie
                val stroke1 = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawLine(40f, 145f, (CANVAS_WIDTH - 40).toFloat(), 145f, stroke1)

                // Untertitel
                val subPaint = Paint(blackPaint).apply {
                    textSize = 17f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, subtitle.ifBlank { "Fein gemahlen, mild" }, CANVAS_WIDTH / 2f, 178f, 340f, 17f, 13f, subPaint)

                // MHD
                val mhdPaint = Paint(blackPaint).apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(mhd.ifBlank { "MHD: 08/2027" }, CANVAS_WIDTH / 2f, 216f, mhdPaint)
            }

            SpiceLabelStyle.RUSTIC_BORDER -> {
                // Gestrichelter Rahmen
                val dashStroke = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
                }
                canvas.drawRect(10f, 10f, (CANVAS_WIDTH - 10).toFloat(), (height - 10).toFloat(), dashStroke)

                // Zarter Innenrahmen
                val stroke1 = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRect(16f, 16f, (CANVAS_WIDTH - 16).toFloat(), (height - 16).toFloat(), stroke1)

                // Header
                val tagPaint = Paint(blackPaint).apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("~ HAUSGEMACHT & NATÜRLICH ~", CANVAS_WIDTH / 2f, 44f, tagPaint)

                // Titel
                val titlePaint = Paint(blackPaint).apply {
                    textSize = 30f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, title.ifBlank { "CEYLON ZIMT" }, CANVAS_WIDTH / 2f, 96f, 330f, 30f, 18f, titlePaint)

                // Trennlinie mit Herzchen/Punkt
                canvas.drawLine(60f, 118f, 165f, 118f, stroke1)
                canvas.drawCircle(CANVAS_WIDTH / 2f, 118f, 3.5f, blackPaint)
                canvas.drawLine(219f, 118f, 324f, 118f, stroke1)

                // Untertitel
                val subPaint = Paint(blackPaint).apply {
                    textSize = 16f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, subtitle.ifBlank { "Aromatisch feines Pulver" }, CANVAS_WIDTH / 2f, 154f, 330f, 16f, 12f, subPaint)

                // MHD
                val mhdPaint = Paint(blackPaint).apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(mhd.ifBlank { "MHD: 12/2028" }, CANVAS_WIDTH / 2f, 192f, mhdPaint)
            }
        }

        return bitmap
    }

    // -------------------------------------------------------------
    // 2. NAMENSSCHILDER, TISCHKARTEN & BADGES
    // -------------------------------------------------------------
    fun generateNameBadge(
        greeting: String,
        name: String,
        subtitle: String,
        badgeStyle: BadgeStyle
    ): Bitmap {
        val height = 240
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        when (badgeStyle) {
            BadgeStyle.HELLO_BADGE -> {
                // Abgerundeter Badge-Kasten
                val strokeOuter = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val rectF = RectF(10f, 10f, (CANVAS_WIDTH - 10).toFloat(), (height - 10).toFloat())
                canvas.drawRoundRect(rectF, 18f, 18f, strokeOuter)

                // Schwarzer Header oben
                val path = Path().apply {
                    addRoundRect(
                        RectF(10f, 10f, (CANVAS_WIDTH - 10).toFloat(), 78f),
                        floatArrayOf(18f, 18f, 18f, 18f, 0f, 0f, 0f, 0f),
                        Path.Direction.CW
                    )
                }
                canvas.drawPath(path, blackPaint)

                // Weißer Header-Text
                val whitePaint = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                whitePaint.textSize = 30f
                whitePaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText(greeting.ifBlank { "HALLO" }, CANVAS_WIDTH / 2f, 45f, whitePaint)

                whitePaint.textSize = 14f
                whitePaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("M E I N  N A M E  I S T", CANVAS_WIDTH / 2f, 68f, whitePaint)

                // Name (Groß, zentriert)
                val namePaint = Paint(blackPaint).apply {
                    textSize = 36f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, name.ifBlank { "Max Mustermann" }, CANVAS_WIDTH / 2f, 142f, 340f, 36f, 20f, namePaint)

                // Trennlinie
                val stroke1 = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                canvas.drawLine(40f, 172f, (CANVAS_WIDTH - 40).toFloat(), 172f, stroke1)

                // Rolle / Untertitel
                val subPaint = Paint(blackPaint).apply {
                    textSize = 17f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, subtitle.ifBlank { "PocketPrint Team" }, CANVAS_WIDTH / 2f, 206f, 330f, 17f, 12f, subPaint)
            }

            BadgeStyle.ELEGANT_GUEST -> {
                // Romantische Tischkarte / Gästeschild
                val stroke3 = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRect(8f, 8f, (CANVAS_WIDTH - 8).toFloat(), (height - 8).toFloat(), stroke3)

                val stroke1 = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRect(14f, 14f, (CANVAS_WIDTH - 14).toFloat(), (height - 14).toFloat(), stroke1)
                drawCornerDiamonds(canvas, 14f, 14f, (CANVAS_WIDTH - 14).toFloat(), (height - 14).toFloat(), blackPaint)

                // Gruß oben
                val greetPaint = Paint(blackPaint).apply {
                    textSize = 15f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("~ ${greeting.ifBlank { "HERZLICH WILLKOMMEN" }} ~", CANVAS_WIDTH / 2f, 44f, greetPaint)

                // Name
                val namePaint = Paint(blackPaint).apply {
                    textSize = 34f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, name.ifBlank { "Familie Schmidt" }, CANVAS_WIDTH / 2f, 114f, 330f, 34f, 18f, namePaint)

                // Schnörkel
                val ornPaint = Paint(blackPaint).apply {
                    textSize = 16f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("── ❦ ──", CANVAS_WIDTH / 2f, 150f, ornPaint)

                // Untertitel / Tisch
                val subPaint = Paint(blackPaint).apply {
                    textSize = 18f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, subtitle.ifBlank { "Tisch 4  •  Ehrengäste" }, CANVAS_WIDTH / 2f, 195f, 330f, 18f, 13f, subPaint)
            }

            BadgeStyle.EVENT_PASS -> {
                // VIP Event-Pass mit Ticket-Notches
                val strokeOuter = Paint(blackPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRect(10f, 10f, (CANVAS_WIDTH - 10).toFloat(), (height - 10).toFloat(), strokeOuter)

                // Schwarze Banner-Ecken
                val fillBlack = Paint(blackPaint).apply { style = Paint.Style.FILL }
                canvas.drawRect(10f, 10f, (CANVAS_WIDTH - 10).toFloat(), 55f, fillBlack)

                val whitePaint = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    textSize = 18f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("★  VIP EVENT-PASS  ★", CANVAS_WIDTH / 2f, 38f, whitePaint)

                // Name
                val namePaint = Paint(blackPaint).apply {
                    textSize = 34f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                autoFitAndDrawText(canvas, name.ifBlank { "ANNA MUSTERMANN" }, CANVAS_WIDTH / 2f, 118f, 340f, 34f, 20f, namePaint)

                // Rolle
                val subPaint = Paint(blackPaint).apply {
                    textSize = 16f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(subtitle.ifBlank { "ALL ACCESS  •  SPEAKER" }, CANVAS_WIDTH / 2f, 160f, subPaint)

                // Dekorativer Barcode-Streifen unten
                drawDecorativeBarcode(canvas, 30f, 182f, (CANVAS_WIDTH - 60).toFloat(), 34f, blackPaint)
            }
        }

        return bitmap
    }

    // -------------------------------------------------------------
    // 3. WLAN-GASTKÄRTCHEN MIT AUTOMATISCHEM CONNECT-QR-CODE
    // -------------------------------------------------------------
    fun generateWifiCard(
        ssid: String,
        password: String,
        security: WifiSecurity,
        title: String = "GÄSTE-WLAN",
        note: String = "Kamera scannen zum Verbinden"
    ): Bitmap {
        val height = 510
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        // Äußerer Kasten mit abgerundeten Ecken
        val strokeOuter = Paint(blackPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val outerRect = RectF(10f, 10f, (CANVAS_WIDTH - 10).toFloat(), (height - 10).toFloat())
        canvas.drawRoundRect(outerRect, 18f, 18f, strokeOuter)

        // Wi-Fi Icon oben zeichnen
        drawWifiWaveIcon(canvas, CANVAS_WIDTH / 2f, 38f, 28f, blackPaint)

        // Titel
        val titlePaint = Paint(blackPaint).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        autoFitAndDrawText(canvas, title.ifBlank { "GÄSTE-WLAN" }, CANVAS_WIDTH / 2f, 96f, 320f, 24f, 16f, titlePaint)

        // Trennlinie mit Raute
        val stroke1 = Paint(blackPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawLine(40f, 114f, 170f, 114f, stroke1)
        canvas.drawCircle(CANVAS_WIDTH / 2f, 114f, 3f, blackPaint)
        canvas.drawLine(214f, 114f, (CANVAS_WIDTH - 40).toFloat(), 114f, stroke1)

        // Offizieller Wi-Fi Connect String nach Standard
        val cleanSsid = ssid.ifBlank { "MeinWLAN_Gast" }
        val cleanPassword = password.ifBlank { "Passwort123" }
        val wifiQrString = buildWifiQrContent(cleanSsid, cleanPassword, security)

        // QR-Code mit ZXing generieren (200x200)
        val qrBitmap = BarcodeGenerator.createQrCode(wifiQrString, 190)
        if (qrBitmap != null) {
            val qrX = ((CANVAS_WIDTH - qrBitmap.width) / 2).toFloat()
            canvas.drawBitmap(qrBitmap, qrX, 126f, null)
        }

        // Info-Kasten 1: Netzwerkname (SSID)
        val boxPaint = Paint(blackPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val ssidBox = RectF(24f, 332f, (CANVAS_WIDTH - 24).toFloat(), 376f)
        canvas.drawRoundRect(ssidBox, 8f, 8f, boxPaint)

        val infoLabelPaint = Paint(blackPaint).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Netzwerk (SSID):", 34f, 350f, infoLabelPaint)

        val infoValuePaint = Paint(blackPaint).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        autoFitAndDrawLeftText(canvas, cleanSsid, 34f, 370f, 310f, 16f, 12f, infoValuePaint)

        // Info-Kasten 2: Passwort
        val passBox = RectF(24f, 386f, (CANVAS_WIDTH - 24).toFloat(), 430f)
        canvas.drawRoundRect(passBox, 8f, 8f, boxPaint)

        canvas.drawText("Passwort:", 34f, 404f, infoLabelPaint)
        val passValuePaint = Paint(blackPaint).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val passDisplay = if (security == WifiSecurity.OPEN) "Kein Passwort (Offen)" else cleanPassword
        autoFitAndDrawLeftText(canvas, passDisplay, 34f, 424f, 310f, 16f, 12f, passValuePaint)

        // Hinweis am Fuß
        val footerPaint = Paint(blackPaint).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        val noteDisplay = "📷  " + note.ifBlank { "Mit Kamera scannen zum Verbinden" }
        canvas.drawText(noteDisplay, CANVAS_WIDTH / 2f, 468f, footerPaint)

        return bitmap
    }

    private fun buildWifiQrContent(ssid: String, pass: String, security: WifiSecurity): String {
        fun escape(s: String) = s.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")

        return if (security == WifiSecurity.OPEN) {
            "WIFI:T:nopass;S:${escape(ssid)};;"
        } else {
            "WIFI:T:${security.code};S:${escape(ssid)};P:${escape(pass)};;"
        }
    }

    // -------------------------------------------------------------
    // 4. TO-DO- & WOCHENPLANER-LISTEN
    // -------------------------------------------------------------
    fun generatePlanner(
        plannerType: PlannerType,
        dateString: String,
        customTasks: List<String> = emptyList()
    ): Bitmap {
        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        val stroke1 = Paint(blackPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val stroke2 = Paint(blackPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        return when (plannerType) {
            PlannerType.DAILY_FOCUS -> {
                val height = 660
                val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                // Header Banner
                val fillBlack = Paint(blackPaint).apply { style = Paint.Style.FILL }
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), 52f, fillBlack)

                val whiteTitle = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    textSize = 22f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("★  M E I N   T A G  ★", CANVAS_WIDTH / 2f, 36f, whiteTitle)

                // Datum
                val datePaint = Paint(blackPaint).apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(dateString.ifBlank { "Datum: ____________________" }, CANVAS_WIDTH / 2f, 78f, datePaint)

                // Sektion 1: TOP 3 PRIORITÄTEN
                val sectionHeaderPaint = Paint(blackPaint).apply {
                    textSize = 15f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                canvas.drawText("⚡ TOP 3 PRIORITÄTEN", 16f, 112f, sectionHeaderPaint)
                canvas.drawLine(16f, 118f, (CANVAS_WIDTH - 16).toFloat(), 118f, stroke2)

                var y = 148f
                for (i in 1..3) {
                    drawCheckbox(canvas, 18f, y - 16f, 18f, stroke2)
                    canvas.drawText("$i.", 46f, y - 2f, sectionHeaderPaint)
                    canvas.drawLine(68f, y, (CANVAS_WIDTH - 18).toFloat(), y, stroke1)
                    y += 36f
                }

                // Sektion 2: TO-DO AUFGABEN
                y += 10f
                canvas.drawText("📝 TO-DO AUFGABEN", 16f, y, sectionHeaderPaint)
                canvas.drawLine(16f, y + 6f, (CANVAS_WIDTH - 16).toFloat(), y + 6f, stroke2)
                y += 34f

                val taskCount = 6
                for (i in 0 until taskCount) {
                    drawCheckbox(canvas, 18f, y - 16f, 18f, stroke2)
                    val taskText = customTasks.getOrNull(i)?.takeIf { it.isNotBlank() }
                    if (taskText != null) {
                        val taskPaint = Paint(blackPaint).apply {
                            textSize = 15f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                        }
                        autoFitAndDrawLeftText(canvas, taskText, 46f, y - 2f, 310f, 15f, 11f, taskPaint)
                    } else {
                        canvas.drawLine(46f, y, (CANVAS_WIDTH - 18).toFloat(), y, stroke1)
                    }
                    y += 34f
                }

                // Sektion 3: NOTIZEN & IDEEN
                y += 10f
                canvas.drawText("💡 NOTIZEN & IDEEN", 16f, y, sectionHeaderPaint)
                canvas.drawLine(16f, y + 6f, (CANVAS_WIDTH - 16).toFloat(), y + 6f, stroke2)
                y += 32f

                for (i in 1..3) {
                    canvas.drawLine(18f, y, (CANVAS_WIDTH - 18).toFloat(), y, stroke1)
                    y += 28f
                }

                // Schlusssatz
                val footerPaint = Paint(blackPaint).apply {
                    textSize = 13f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("☺  Fokus behalten & Erfolg haben!", CANVAS_WIDTH / 2f, height - 16f, footerPaint)

                bitmap
            }

            PlannerType.WEEKLY_PLANNER -> {
                val height = 740
                val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                // Header
                val fillBlack = Paint(blackPaint).apply { style = Paint.Style.FILL }
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), 50f, fillBlack)

                val whiteTitle = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    textSize = 20f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("🗓️   W O C H E N P L A N E R", CANVAS_WIDTH / 2f, 34f, whiteTitle)

                val days = listOf("MONTAG", "DIENSTAG", "MITTWOCH", "DONNERSTAG", "FREITAG", "SAMSTAG", "SONNTAG")
                var y = 80f

                val dayPaint = Paint(blackPaint).apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }

                for (day in days) {
                    // Tag Header
                    canvas.drawText(day, 16f, y, dayPaint)
                    canvas.drawLine(16f, y + 4f, (CANVAS_WIDTH - 16).toFloat(), y + 4f, stroke1)
                    y += 26f

                    // 2 Zeilen pro Tag
                    for (line in 1..2) {
                        drawCheckbox(canvas, 18f, y - 14f, 15f, stroke1)
                        canvas.drawLine(42f, y, (CANVAS_WIDTH - 18).toFloat(), y, stroke1)
                        y += 26f
                    }
                    y += 10f
                }

                bitmap
            }

            PlannerType.HABIT_TRACKER -> {
                val height = 580
                val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                // Header
                val fillBlack = Paint(blackPaint).apply { style = Paint.Style.FILL }
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), 52f, fillBlack)

                val whiteTitle = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    textSize = 19f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("✨  TÄGLICHE GEWOHNHEITEN", CANVAS_WIDTH / 2f, 35f, whiteTitle)

                val datePaint = Paint(blackPaint).apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(dateString.ifBlank { "Datum: ____________________" }, CANVAS_WIDTH / 2f, 80f, datePaint)

                var y = 114f
                val hPaint = Paint(blackPaint).apply {
                    textSize = 15f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }

                // 1. Wasser
                canvas.drawText("💧 Wasser (2 Liter / 8 Gläser):", 16f, y, hPaint)
                y += 26f
                var boxX = 22f
                for (b in 1..8) {
                    drawCheckbox(canvas, boxX, y - 16f, 18f, stroke2)
                    boxX += 42f
                }
                y += 40f

                // 2. Bewegung
                canvas.drawText("🏃 Bewegung & Sport (30 Min.):", 16f, y, hPaint)
                y += 26f
                drawCheckbox(canvas, 22f, y - 16f, 18f, stroke2)
                canvas.drawText("Spaziergang / Workout absolviert", 50f, y - 2f, hPaint)
                y += 44f

                // 3. Ernährung
                canvas.drawText("🥗 Ernährung & Vitalität:", 16f, y, hPaint)
                y += 26f
                drawCheckbox(canvas, 22f, y - 16f, 18f, stroke2)
                canvas.drawText("Frühstück", 48f, y - 2f, hPaint)
                drawCheckbox(canvas, 140f, y - 16f, 18f, stroke2)
                canvas.drawText("Obst/Gemüse", 166f, y - 2f, hPaint)
                drawCheckbox(canvas, 276f, y - 16f, 18f, stroke2)
                canvas.drawText("Vitamine", 302f, y - 2f, hPaint)
                y += 44f

                // 4. Fokus & Geist
                canvas.drawText("📖 Fokus & Geist:", 16f, y, hPaint)
                y += 26f
                drawCheckbox(canvas, 22f, y - 16f, 18f, stroke2)
                canvas.drawText("20 Min. Buch lesen / Lernen", 50f, y - 2f, hPaint)
                y += 44f

                // 5. Abend-Routine
                canvas.drawText("🌙 Schlaf & Abendroutine:", 16f, y, hPaint)
                y += 26f
                drawCheckbox(canvas, 22f, y - 16f, 18f, stroke2)
                canvas.drawText("Kein Bildschirm 30 Min vor Schlaf", 50f, y - 2f, hPaint)
                y += 46f

                // 6. Tageserfolg
                canvas.drawText("💭 Heutiger Erfolg / Dankbarkeit:", 16f, y, hPaint)
                y += 28f
                canvas.drawLine(18f, y, (CANVAS_WIDTH - 18).toFloat(), y, stroke1)
                y += 26f
                canvas.drawLine(18f, y, (CANVAS_WIDTH - 18).toFloat(), y, stroke1)

                bitmap
            }
        }
    }

    // -------------------------------------------------------------
    // HILFSFUNKTIONEN ZUM ZEICHNEN
    // -------------------------------------------------------------
    private fun drawCheckbox(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val rect = RectF(x, y, x + size, y + size)
        canvas.drawRoundRect(rect, 3f, 3f, paint)
    }

    private fun drawWifiWaveIcon(canvas: Canvas, centerX: Float, centerY: Float, size: Float, paint: Paint) {
        val strokePaint = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.12f
            strokeCap = Paint.Cap.ROUND
        }
        val fillPaint = Paint(paint).apply {
            style = Paint.Style.FILL
        }

        // Punkt in der Mitte
        canvas.drawCircle(centerX, centerY + size * 0.35f, size * 0.10f, fillPaint)

        // Welle 1
        val r1 = size * 0.38f
        val oval1 = RectF(centerX - r1, centerY + size * 0.35f - r1, centerX + r1, centerY + size * 0.35f + r1)
        canvas.drawArc(oval1, 225f, 90f, false, strokePaint)

        // Welle 2
        val r2 = size * 0.68f
        val oval2 = RectF(centerX - r2, centerY + size * 0.35f - r2, centerX + r2, centerY + size * 0.35f + r2)
        canvas.drawArc(oval2, 225f, 90f, false, strokePaint)

        // Welle 3
        val r3 = size * 0.98f
        val oval3 = RectF(centerX - r3, centerY + size * 0.35f - r3, centerX + r3, centerY + size * 0.35f + r3)
        canvas.drawArc(oval3, 225f, 90f, false, strokePaint)
    }

    private fun drawCornerDiamonds(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        val dSize = 4f
        val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }

        drawSingleDiamond(canvas, left, top, dSize, fillPaint)
        drawSingleDiamond(canvas, right, top, dSize, fillPaint)
        drawSingleDiamond(canvas, left, bottom, dSize, fillPaint)
        drawSingleDiamond(canvas, right, bottom, dSize, fillPaint)
    }

    private fun drawSingleDiamond(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            lineTo(cx + size, cy)
            lineTo(cx, cy + size)
            lineTo(cx - size, cy)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawDecorativeBarcode(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, paint: Paint) {
        val pattern = listOf(2, 4, 1, 3, 2, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2)
        var curX = x
        var isBar = true
        for (w in pattern) {
            val barW = w * 2.5f
            if (curX + barW > x + width) break
            if (isBar) {
                canvas.drawRect(curX, y, curX + barW, y + height, paint)
            }
            curX += barW
            isBar = !isBar
        }
    }

    private fun autoFitAndDrawText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        maxWidth: Float,
        startSize: Float,
        minSize: Float,
        paint: Paint
    ) {
        var size = startSize
        paint.textSize = size
        while (size > minSize && paint.measureText(text) > maxWidth) {
            size -= 1f
            paint.textSize = size
        }
        canvas.drawText(text, cx, cy, paint)
    }

    private fun autoFitAndDrawLeftText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        startSize: Float,
        minSize: Float,
        paint: Paint
    ) {
        var size = startSize
        paint.textSize = size
        while (size > minSize && paint.measureText(text) > maxWidth) {
            size -= 1f
            paint.textSize = size
        }
        canvas.drawText(text, x, y, paint)
    }
}
