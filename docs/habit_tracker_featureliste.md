# Habit Tracker, Feature-Liste

Stand: 31.08.2026
Plattform: Android (Kotlin, Jetpack Compose, Material 3)
Betrieb: vollständig lokal, keine Netzwerkverbindung, keine `INTERNET`-Permission

Annahme: UI mit Jetpack Compose. Falls XML/Views gewünscht, hier melden, die Feature-Logik bleibt identisch.

Prioritäten: **MVP** = für erste lauffähige Version nötig, **V2** = danach.

---

## 1. Rahmenbedingungen

| Punkt | Angabe |
|---|---|
| Sprache | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Datenhaltung | Room (SQLite), rein lokal |
| Min. SDK | API 26 (Android 8), wegen `java.time` |
| Target SDK | aktuell (API 35) |
| Berechtigungen | nur `POST_NOTIFICATIONS` (ab Android 13) für Erinnerungen |
| Architektur | MVVM, Repository, Room-DAO, StateFlow |

---

## 2. Design und UI

Leitlinie: modern, schlicht, ruhig. Wenige Farbakzente, viel Weißraum, klare Typografie.

**Farbkonzept**

| Rolle | Verwendung |
|---|---|
| Neutrale Basis | Hintergrund und Flächen in Weiß bzw. sehr hellem Grau (Light), fast Schwarz (Dark) |
| Ein Akzent | genau eine Akzentfarbe für aktive Elemente, Buttons, Fortschritt (z. B. Indigo oder Teal) |
| Status "bestanden" | dezentes Grün, nur als Marker, nicht flächig |
| Status "offen" | Grauton, kein eigenes Rot für "nicht bestanden", um nicht zu werten |

Regel: maximal zwei Farben plus Neutraltöne im gesamten UI. Material-You Dynamic Color standardmäßig **aus**, damit das Bild konsistent bleibt. Optional in Einstellungen zuschaltbar.

**Typografie und Layout**

| Punkt | Angabe |
|---|---|
| Schrift | System-Schrift (Roboto), 3 Größen: Titel, Fließtext, Label |
| Ecken | einheitlicher Radius (z. B. 16 dp) für Cards und Buttons |
| Abstände | 8-dp-Raster, großzügige Innenabstände (16 bis 24 dp) |
| Icons | ein Set (Material Symbols, outlined), keine Mischung |
| Elevation | flach, Trennung über Farbe und Abstand statt über Schatten |
| Dark Mode | vollständig unterstützt, folgt System |

**Kernscreens (MVP)**

1. Heute: Liste der Habits des Tages, Tagesthema oben, Fortschrittsbalken, Status-Marker
2. Verlauf: Kalender bzw. Heatmap
3. Habit-Detail: Bearbeiten, Statistik, Streak
4. Einstellungen: Backup, Erinnerungen, Theme

---

## 3. Datenmodell

Vier Entitäten. Details siehe Feature-Bereiche.

- `Habit`: Definition eines Habits (Vorlage)
- `Day`: ein Kalendertag mit Thema und Tagesziel
- `DayHabit`: welcher Habit an welchem Tag gilt, plus Ist-Wert
- `Settings`: App-Einstellungen (einzelne Zeile oder DataStore)

`Day.passed` wird bei jeder Erfassungsänderung neu berechnet und persistiert, damit Verlauf und Streak schnell abfragbar sind.

---

## 4. Features

### F1 Habit-Verwaltung (MVP)

Anlegen, Bearbeiten, Archivieren und Löschen von Habits.

| Feld | Typ | Angabe |
|---|---|---|
| `id` | Long | Primärschlüssel, autogeneriert |
| `name` | String | Pflicht, 1 bis 40 Zeichen |
| `type` | Enum | `CHECK` (ja/nein), `COUNTER` (Anzahl), `AMOUNT` (Menge/Dauer) |
| `target` | Int | Sollwert, bei `CHECK` immer 1 |
| `unit` | String? | frei, z. B. "Gläser", "min", nur bei `COUNTER`/`AMOUNT` |
| `points` | Int | Wert für das Punkte-Tagesziel, Standard 1 |
| `required` | Boolean | Pflicht-Habit für Tagesziel-Regel `ALL_REQUIRED` |
| `icon` | String | Name aus Material-Symbols-Set |
| `colorTag` | Int? | optionaler Akzent, sonst App-Akzent |
| `archived` | Boolean | ausgeblendet statt gelöscht |

Verhalten:
- Löschen fragt nach, Archivieren ist die empfohlene Aktion (Verlauf bleibt erhalten)
- Ein archivierter Habit erscheint nicht mehr in neuen Tagen, alte Einträge bleiben

### F2 Tagesverwaltung: Thema und Tagesziel (MVP)

Jeder Kalendertag ist ein `Day` mit optionalem Thema und einer Tagesziel-Regel.

| Feld | Typ | Angabe |
|---|---|---|
| `date` | LocalDate | Primärschlüssel |
| `theme` | String? | optionales Tagesthema, 0 bis 40 Zeichen |
| `goalType` | Enum | `ALL_REQUIRED`, `MIN_COUNT`, `POINTS` |
| `goalThreshold` | Int | Schwelle für `MIN_COUNT` (Anzahl) und `POINTS` (Summe) |
| `passed` | Boolean | berechneter Status, ob Tag bestanden |

Tagesziel-Regeln (genau eine je Tag):

1. `ALL_REQUIRED`: bestanden, wenn alle Habits mit `required = true` an diesem Tag erfüllt sind
2. `MIN_COUNT`: bestanden, wenn mindestens `goalThreshold` Habits erfüllt sind
3. `POINTS`: bestanden, wenn die Summe der `points` erfüllter Habits ≥ `goalThreshold`

Ein Habit gilt als "erfüllt", wenn `progress ≥ target` im zugehörigen `DayHabit`.

Verhalten:
- Standardregel in Einstellungen wählbar, pro Tag überschreibbar
- Thema ist reine Kennzeichnung, es beeinflusst das Tagesziel nicht
- Statuswechsel auf `passed = true` wird sofort im UI angezeigt (Marker, kurze Bestätigung)

### F3 Tageserfassung (MVP)

Fortschritt pro Habit und Tag eintragen.

| Feld (`DayHabit`) | Typ | Angabe |
|---|---|---|
| `date` | LocalDate | Teil des zusammengesetzten Schlüssels |
| `habitId` | Long | Teil des zusammengesetzten Schlüssels |
| `progress` | Int | Ist-Wert, 0 bis `target` (bzw. offen nach oben bei Zählern) |

Verhalten:
- `CHECK`: Tippen togglet zwischen 0 und 1
- `COUNTER`/`AMOUNT`: Plus/Minus oder direkte Eingabe
- Nachtragen für vergangene Tage möglich (über Verlauf)
- Jede Änderung triggert Neuberechnung von `Day.passed` und Streak

### F4 Auswertung, Streak, Statistik (MVP Basis, V2 Ausbau)

| Kennzahl | Angabe | Prio |
|---|---|---|
| Aktuelle Streak | Anzahl aufeinanderfolgender bestandener Tage bis heute | MVP |
| Längste Streak | Maximum über den gesamten Verlauf | MVP |
| Abschlussquote | Anteil bestandener Tage im Zeitraum (Woche/Monat) | MVP |
| Habit-Quote | Erfüllungsrate je Habit über Zeitraum | V2 |
| Heatmap | Kalenderraster, Zelle eingefärbt nach Tagesstatus | MVP |
| Trend | einfacher Verlauf über Wochen | V2 |

Streak-Definition: nur `Day.passed = true` zählt. Ein nicht bestandener oder leerer Tag setzt die aktuelle Streak zurück. Streak wird bei jeder Erfassungsänderung neu berechnet.

### F5 Erinnerungen (V2)

Lokale Notifications pro Habit oder global.

| Feld | Angabe |
|---|---|
| Zeit | Uhrzeit je Erinnerung |
| Wochentage | Auswahl der Tage |
| Bezug | global ("Tag erfassen") oder je Habit |
| Technik | `WorkManager` bzw. `AlarmManager`, lokal, kein Push |

Berechtigung `POST_NOTIFICATIONS` wird beim ersten Aktivieren abgefragt.

### F6 Backup und Restore als ZIP (MVP)

Logischer Export nach JSON, gebündelt als ZIP. Versionsrobust über Manifest.

**ZIP-Inhalt**

| Datei | Inhalt |
|---|---|
| `manifest.json` | `appVersion`, `schemaVersion`, `exportedAt` |
| `habits.json` | alle Habits |
| `days.json` | alle Tage |
| `day_habits.json` | alle Erfassungen |
| `settings.json` | App-Einstellungen |

Verhalten:
- Export: Nutzer wählt Ziel über Storage Access Framework (`CreateDocument`, MIME `application/zip`), Dateiname `habits_backup_JJJJ-MM-TT.zip`
- Import: Auswahl über `OpenDocument`, Manifest prüfen, bei abweichender `schemaVersion` migrieren, danach in einer Transaktion ersetzen
- Import ist "ersetzen", nicht "zusammenführen" (V1). Merge optional als V2.
- Keine Speicherberechtigung nötig (Scoped Storage über SAF)

### F7 Einstellungen (MVP Basis)

| Einstellung | Angabe | Prio |
|---|---|---|
| Standard-Tagesziel | Regel und Schwelle für neue Tage | MVP |
| Theme | System / Hell / Dunkel | MVP |
| Dynamic Color | an/aus, Standard aus | V2 |
| Backup | Export / Import auslösen | MVP |
| Erinnerungen | verwalten | V2 |
| Wochenstart | Montag/Sonntag (für Heatmap) | V2 |

---

## 5. Nicht-Ziele (V1)

- keine Cloud, kein Konto, keine Synchronisation
- kein Teilen/Export einzelner Habits als Bild
- keine Widgets (mögliche V2)
- kein Multi-User

---

## 6. Offene Design-Entscheidungen

1. Akzentfarbe festlegen (ein Ton), Vorschlag: Indigo oder Teal
2. Standard-Tagesziel-Regel für neue Nutzer: Vorschlag `POINTS`, weil flexibel
3. Verhalten bei leerem Tag ohne Habits: als "nicht bestanden" oder "neutral" werten

---

## Fazit

MVP-Umfang: F1, F2, F3, F4 (Basis), F6, F7 (Basis). V2: F5 Erinnerungen, Statistik-Ausbau, Merge-Import, Widgets. Das Design steht auf einer neutralen Basis mit einer Akzentfarbe und einem dezenten Grün als Statusmarker, alles flach und großzügig im Abstand.
