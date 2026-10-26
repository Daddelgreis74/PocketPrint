package com.example.openthermalprint.ui.templates

enum class SpiceLabelStyle(val displayName: String) {
    VINTAGE_FLORAL("Vintage Floral"),
    SKANDI_CLEAN("Skandi Minimal"),
    CHALKBOARD_BANNER("Kreidetafel-Banner"),
    RUSTIC_BORDER("Rustikal")
}

enum class BadgeStyle(val displayName: String) {
    HELLO_BADGE("Hallo Name-Badge"),
    ELEGANT_GUEST("Elegante Tischkarte"),
    EVENT_PASS("VIP Event-Pass")
}

enum class WifiSecurity(val displayName: String, val code: String) {
    WPA_WPA2("WPA / WPA2 / WPA3", "WPA"),
    WEP("WEP (Älter)", "WEP"),
    OPEN("Offen (ohne Passwort)", "nopass")
}

enum class PlannerType(val displayName: String) {
    DAILY_FOCUS("Tagesplaner (Mein Tag)"),
    WEEKLY_PLANNER("Wochenplaner (Mo - So)"),
    HABIT_TRACKER("Gewohnheiten-Tracker")
}

data class SpicePreset(
    val name: String,
    val subtitle: String,
    val mhd: String
)

object TemplatePresets {
    val SPICES = listOf(
        SpicePreset("BASILIKUM", "Gerebelt & Getrocknet", "MHD: 12/2027"),
        SpicePreset("OREGANO", "Gerebelt aus Griechenland", "MHD: 10/2027"),
        SpicePreset("PAPRIKA EDELSÜSS", "Fein gemahlen, mild", "MHD: 08/2027"),
        SpicePreset("PAPRIKA ROSENSCHARF", "Feurig & aromatisch", "MHD: 08/2027"),
        SpicePreset("KURKUMA", "Bio Gelbwurzel gemahlen", "MHD: 06/2028"),
        SpicePreset("CEYLON ZIMT", "Aromatisch feines Pulver", "MHD: 12/2028"),
        SpicePreset("PFEFFER SCHWARZ", "Ganze Tellicherry Körner", "MHD: 01/2029"),
        SpicePreset("MEERSALZ", "Grobkörniges Natursalz", "Unbegrenzt"),
        SpicePreset("CHILIFLOCKEN", "Scharf mit Saat", "MHD: 11/2027"),
        SpicePreset("THYMIAN", "Gerebeltes Bergkraut", "MHD: 09/2027"),
        SpicePreset("ROSMARIN", "Ganze getrocknete Nadeln", "MHD: 09/2027"),
        SpicePreset("KREUZKÜMMEL", "Cumin fein gemahlen", "MHD: 07/2027"),
        SpicePreset("CURRYPULVER", "Englische Mischung mild", "MHD: 05/2027"),
        SpicePreset("MUSKATNUSS", "Ganze Nüsse zum Reiben", "MHD: 12/2028"),
        SpicePreset("HAFERFLOCKEN", "Zarte Vollkornflocken", "MHD: 04/2027"),
        SpicePreset("BASMATI REIS", "Duftreis aus Indien", "MHD: 02/2028"),
        SpicePreset("KAFFEEBOHNEN", "100% Arabica Röstung", "MHD: 10/2026"),
        SpicePreset("ROHRZUCKER", "Feiner Vollrohrzucker", "Unbegrenzt"),
        SpicePreset("WEIZENMEHL", "Type 405 Backqualität", "MHD: 03/2027")
    )

    val BADGE_GREETINGS = listOf(
        "HALLO",
        "WILLKOMMEN",
        "RESERVIERT",
        "VIP GAST",
        "TISCHKARTE"
    )
}
