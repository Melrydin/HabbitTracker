# Habit Tracker, Feature-Liste

Stand: 31.08.2026
Plattform: Android (Kotlin, Jetpack Compose, Material 3)
Betrieb: vollständig lokal, keine Netzwerkverbindung, keine `INTERNET`-Permission

Annahme: UI mit Jetpack Compose. Falls XML/Views gewünscht, hier melden, die Feature-Logik bleibt identisch.

Prioritäten: **MVP** = für erste lauffähige Version nötig, **V2** = Komfort und Tiefe, **V3** = Feinschliff.

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
| Widgets | Jetpack Glance (ab F9) |

---

## 2. Design und UI

Leitlinie: modern, schlicht, ruhig. Wenige Farbakzente, viel Weißraum, klare Typografie.

**Farbkonzept**

| Rolle | Verwendung |
|---|---|
| Neutrale Basis | Hintergrund und Flächen in Weiß bzw. sehr hellem Grau (Light), fast Schwarz (Dark) |
| Ein Akzent | Indigo (Material Indigo, ca. `#3F51B5` / `#5C6BC0`) für aktive Elemente, Buttons, Fortschritt |
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
4. Wochen-Habits: Vorlagen und Unter-Habits verwalten
5. Einstellungen: Backup, Erinnerungen, Theme

---

## 3. Datenmodell

Kern-Entitäten. Details siehe Feature-Bereiche.

- `Habit`: Definition eines Habits. Selbstreferenzierende Hierarchie über `kind` und `parentId` (siehe F8). Trägt zusätzlich Felder aus F4, F11 und F12 (`streakRule`, `perWeekTarget`, `polarity`, `category`, `tags`, `sortIndex`, `note`).
- `Day`: ein Kalendertag mit Tagesziel, optionalem Theme-Habit (`themeHabitId`) und Tagesnotiz (`dayNote`).
- `DayHabit`: welcher Habit an welchem Tag gilt, plus Ist-Wert.
- `Goal`: Langzeitziel je Habit (siehe F10).
- `Pause`: Pausen- und Urlaubszeiträume, global oder je Habit (siehe F4).
- `Settings`: App-Einstellungen (einzelne Zeile oder DataStore).

`Day.status` (dreiwertig: `NEUTRAL`, `PASSED`, `FAILED`) wird bei jeder Erfassungsänderung neu berechnet und persistiert, damit Verlauf und Streak schnell abfragbar sind. `NEUTRAL` deckt den leeren Tag ohne zugeordnete Habits ab.

Das Tagesthema ist kein freier Text mehr, sondern immer an einen Habit gebunden (`Day.themeHabitId`). Siehe F2 und F8.

Prinzip: Alle Erweiterungsfelder werden schon im MVP-Schema angelegt, auch wenn die zugehörige UI erst in V2/V3 kommt. Das spart spätere Room-Migrationen.

---

## 4. Features

### F1 Habit-Verwaltung (MVP)

Anlegen, Bearbeiten, Archivieren und Löschen von Habits.

| Feld | Typ | Angabe |
|---|---|---|
| `id` | Long | Primärschlüssel, autogeneriert |
| `name` | String | Pflicht, 1 bis 40 Zeichen |
| `type` | Enum | `CHECK` (mark, ja/nein), `COUNTER` (Anzahl), `AMOUNT` (Menge/Dauer) |
| `target` | Int | Sollwert, bei `CHECK` immer 1 |
| `unit` | String? | frei, z. B. "Gläser", "min", nur bei `COUNTER`/`AMOUNT` |
| `points` | Int | Wert für das Punkte-Tagesziel, Standard 1 |
| `required` | Boolean | Pflicht-Habit für Tagesziel-Regel `ALL_REQUIRED` |
| `icon` | String | Name aus Material-Symbols-Set |
| `colorTag` | Int? | optionaler Akzent, sonst App-Akzent |
| `note` | String? | freie Notiz bzw. Beschreibung zum Habit |
| `archived` | Boolean | ausgeblendet statt gelöscht |

Weitere Felder werden in den jeweiligen Features definiert: Hierarchie und Theme-Kopplung in F8 (`kind`, `parentId`, `weekStart`, `weekSpan`, `recurrence`, `assignedDows`, `givesTheme`, `isThemeGenerated`), Streak-Regel in F4 (`streakRule`, `perWeekTarget`), Polarität in F11 (`polarity`), Ordnung in F12 (`category`, `tags`, `sortIndex`).

Verhalten:
- Löschen fragt nach, Archivieren ist die empfohlene Aktion (Verlauf bleibt erhalten)
- Ein archivierter Habit erscheint nicht mehr in neuen Tagen, alte Einträge bleiben

### F2 Tagesverwaltung: Thema und Tagesziel (MVP)

Jeder Kalendertag ist ein `Day` mit optionalem Theme-Habit und einer Tagesziel-Regel.

| Feld | Typ | Angabe |
|---|---|---|
| `date` | LocalDate | Primärschlüssel |
| `themeHabitId` | Long? | referenziert den Habit, der das Tagesthema ist. Anzeigename = dessen `name`. |
| `dayNote` | String? | freie Tagesnotiz (kurzer Rückblick, Stimmung, Kontext) |
| `goalType` | Enum | `ALL_REQUIRED`, `MIN_COUNT`, `POINTS` |
| `goalThreshold` | Int | Schwelle für `MIN_COUNT` (Anzahl) und `POINTS` (Summe) |
| `status` | Enum | berechneter Tagesstatus: `NEUTRAL`, `PASSED`, `FAILED` |

Standardregel für neue Tage: **`POINTS`** (in Einstellungen änderbar, pro Tag überschreibbar).

Tagesziel-Regeln (genau eine je Tag):

1. `ALL_REQUIRED`: `PASSED`, wenn alle Habits mit `required = true` an diesem Tag erfüllt sind
2. `MIN_COUNT`: `PASSED`, wenn mindestens `goalThreshold` Habits erfüllt sind
3. `POINTS`: `PASSED`, wenn die Summe der `points` erfüllter Habits ≥ `goalThreshold`

Ein Habit gilt als "erfüllt", wenn `progress ≥ target` im zugehörigen `DayHabit`.

Statusberechnung:
- Tag ohne zugeordnete Habits → `NEUTRAL`
- Tag mit Habits, Regel erfüllt → `PASSED`
- Tag mit Habits, Regel nicht erfüllt → `FAILED`

Thema und Habit sind gekoppelt (Details F8):
- Setzt der Nutzer manuell ein Thema, wird automatisch ein Habit angelegt (Typ `CHECK`/mark) und als Theme-Habit des Tages verknüpft.
- Der Theme-Habit ist ein normaler Habit, zählt also ins Tagesziel wie jeder andere.
- Statuswechsel auf `PASSED` wird sofort im UI angezeigt (Marker, kurze Bestätigung).

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
- Jede Änderung triggert Neuberechnung von `Day.status` und Streak

### F4 Auswertung, Streak, Statistik (MVP Basis, V2 Ausbau)

| Kennzahl | Angabe | Prio |
|---|---|---|
| Aktuelle Streak | Anzahl aufeinanderfolgender `PASSED`-Tage bis heute | MVP |
| Längste Streak | Maximum über den gesamten Verlauf | MVP |
| Abschlussquote | Anteil `PASSED` an den gewerteten Tagen (`PASSED` plus `FAILED`) im Zeitraum | MVP |
| Habit-Quote | Erfüllungsrate je Habit über Zeitraum | V2 |
| Heatmap | Kalenderraster, Zelle eingefärbt nach Tagesstatus | MVP |
| Trend | einfacher Verlauf über Wochen | V2 |

Streak-Definition (Tages-Streak): nur `PASSED` zählt hoch. `FAILED` setzt die aktuelle Streak zurück. `NEUTRAL` (leerer Tag, Pausentag) wird übersprungen und bricht die Streak nicht. Streak wird bei jeder Erfassungsänderung neu berechnet.

**Flexibler Habit-Streak (V2)**

Zusätzlich zum Tages-Streak bekommt jeder Habit einen optionalen eigenen Streak mit wählbarer Regel.

- `Habit.streakRule` (Enum): `DAILY` (jeden aktiven Tag erfüllen) oder `WEEKLY_COUNT` (x-mal pro Woche genügt)
- `Habit.perWeekTarget` (Int?): nur bei `WEEKLY_COUNT`, wie viele Erfüllungen pro Woche zählen

Bei `WEEKLY_COUNT` zählt der Habit-Streak erfüllte Wochen statt Tage. Der Tages-Streak (oben) bleibt die Kennzahl auf Tagesebene.

**Streak-Schutz, Kulanztage (V2)**

Ein begrenztes Budget fängt einen `FAILED`-Tag ab, ohne die Serie zu brechen.

- `Settings.freezePerMonth` (Int, Standard 1): Kulanztage pro Kalendermonat
- Würde ein `FAILED`-Tag den Tages-Streak brechen und ist noch Budget übrig, wird automatisch ein Kulanztag verbraucht, die Serie läuft weiter. Verbrauch wird protokolliert und im Verlauf markiert.
- `NEUTRAL`-Tage brauchen keinen Kulanztag, sie brechen ohnehin nicht.

**Pause- und Urlaubsmodus (V2)**

Zeiträume, in denen nichts eingefordert wird und der Streak pausiert statt bricht.

- Tabelle `Pause`: `id`, `from` (LocalDate), `to` (LocalDate), `habitId` (Long?, null = global)
- Tage im Pausenzeitraum zählen `NEUTRAL`, egal ob erfasst. Global pausiert die ganze App, mit `habitId` nur ein Habit.
- Der Streak überspringt Pausentage wie andere `NEUTRAL`-Tage.

**Erweiterte Statistik (V2)**

- bester und schwächster Wochentag (Erfüllungsquote je Wochentag)
- Erfüllungsquote je Habit über wählbaren Zeitraum
- Vergleich aktuelle Woche gegen Vorwoche, aktueller Monat gegen Vormonat
- alles rein lokal aus vorhandenen Daten berechnet

### F5 Erinnerungen (V2)

Lokale Notifications pro Habit oder global.

| Feld | Angabe |
|---|---|
| Zeit | Uhrzeit je Erinnerung |
| Wochentage | Auswahl der Tage |
| Bezug | global ("Tag erfassen") oder je Habit |
| Technik | `WorkManager` bzw. `AlarmManager`, lokal, kein Push |

Berechtigung `POST_NOTIFICATIONS` wird beim ersten Aktivieren abgefragt. Notification-Actions zum direkten Abhaken siehe F9.

### F6 Backup und Restore als ZIP (MVP), CSV-Export (V2)

Logischer Export nach JSON, gebündelt als ZIP. Versionsrobust über Manifest.

**ZIP-Inhalt**

| Datei | Inhalt |
|---|---|
| `manifest.json` | `appVersion`, `schemaVersion`, `exportedAt` |
| `habits.json` | alle Habits (inklusive Wochen- und Unter-Habits) |
| `days.json` | alle Tage |
| `day_habits.json` | alle Erfassungen |
| `goals.json` | Langzeitziele (ab F10) |
| `settings.json` | App-Einstellungen |

Verhalten:
- Export: Nutzer wählt Ziel über Storage Access Framework (`CreateDocument`, MIME `application/zip`), Dateiname `habits_backup_JJJJ-MM-TT.zip`
- Import: Auswahl über `OpenDocument`, Manifest prüfen, bei abweichender `schemaVersion` migrieren, danach in einer Transaktion ersetzen
- Import ist "ersetzen", nicht "zusammenführen" (V1). Merge optional als V2.
- Keine Speicherberechtigung nötig (Scoped Storage über SAF)

**CSV-Export (V2)**

- zusätzlicher Export als CSV (z. B. `days.csv`, `day_habits.csv`) für Auswertung in Tabellenkalkulation
- reiner Export, kein Re-Import über CSV (Restore läuft weiter über ZIP)

### F7 Einstellungen (MVP Basis)

| Einstellung | Angabe | Prio |
|---|---|---|
| Standard-Tagesziel | Regel und Schwelle für neue Tage, Standard `POINTS` | MVP |
| Theme | System / Hell / Dunkel | MVP |
| Dynamic Color | an/aus, Standard aus | V2 |
| Backup | Export / Import auslösen, CSV-Export | MVP / V2 |
| Erinnerungen | verwalten | V2 |
| Streak-Schutz | Kulanztage pro Monat, Standard 1 | V2 |
| Pause / Urlaub | Zeiträume verwalten | V2 |
| Wochenstart | Montag/Sonntag (für Heatmap und Wochen-Habits) | V2 |

### F8 Wochen-Habits, Unter-Habits und Theme-Kopplung (V2, Datenmodell schon im MVP anlegen)

Zweistufige Habit-Hierarchie plus feste Kopplung von Tagesthema und Habit.

**Erweiterung von `Habit`**

| Feld | Typ | Angabe |
|---|---|---|
| `kind` | Enum | `SIMPLE` (normale Vorlage), `WEEKLY` (Wochen-Habit), `SUB` (Unter-Habit) |
| `parentId` | Long? | bei `SUB` der zugehörige Wochen-Habit |
| `weekStart` | LocalDate? | nur bei `WEEKLY`: Montag der gebundenen Kalenderwoche |
| `weekSpan` | Enum? | nur bei `WEEKLY`: `WORKWEEK` (Mo bis Fr) oder `FULL` (Mo bis So), legt die aktiven Tage der Woche fest |
| `recurrence` | Enum? | nur bei `WEEKLY`: `EVERY_DAY` (an jedem aktiven Tag der Woche) oder `BY_SUBS` (nur über Unter-Habits) |
| `assignedDows` | Set&lt;Int&gt; | nur bei `SUB`: ein oder mehrere Wochentage 1..7 (Mo..So) innerhalb der Wochenspanne, an denen der Habit auftaucht. Persistenz über TypeConverter (z. B. CSV) |
| `givesTheme` | Boolean | ob dieser Habit beim Auftauchen das Tagesthema setzt |
| `isThemeGenerated` | Boolean | true, wenn automatisch aus einem manuell gesetzten Tagesthema entstanden |

`type`, `target`, `unit`, `points` gelten wie bei jedem Habit, also auch Wochen- und Unter-Habits können `CHECK`, `COUNTER` oder `AMOUNT` sein.

**Materialisierung eines Tages** (beim Anlegen/Öffnen eines `Day`)

Ein `WEEKLY` wirkt nur innerhalb seiner gebundenen Woche (`weekStart` bis `weekStart` plus Spanne aus `weekSpan`). Außerhalb dieser Woche erzeugt es nichts.

1. Jeder aktive `WEEKLY` mit `recurrence = EVERY_DAY`, dessen Woche `date` enthält und dessen `weekSpan` den Wochentag abdeckt → `DayHabit` für diesen Tag.
2. Jeder aktive `SUB`, dessen `assignedDows` den Wochentag von `date` enthält und dessen Elternwoche `date` enthält → `DayHabit` für diesen Tag. Ist derselbe `SUB` mehreren Tagen zugeordnet, entsteht pro Tag ein eigener `DayHabit`, der Fortschritt zählt also je Tag getrennt.
3. Theme-Habit bestimmen (siehe unten), `Day.themeHabitId` setzen.

Ein `WEEKLY` mit `recurrence = BY_SUBS` erscheint nicht selbst, sondern nur über seine Unter-Habits.

Hinweis: Ein Wochen-Habit gilt nur für eine konkrete Woche. Für eine Folgewoche wird er neu angelegt oder kopiert (Kopierfunktion optional V2).

**Kopplung Thema und Habit**

- Habit setzt Thema: Ein aufgetauchter Habit mit `givesTheme = true` wird zum Theme-Habit des Tages.
- Thema setzt Habit: Gibt der Nutzer manuell ein Tagesthema ein, wird automatisch ein Habit angelegt (`kind = SIMPLE`, `type = CHECK`/mark, `isThemeGenerated = true`, `name = Thementext`), ein `DayHabit` für den Tag erzeugt und `Day.themeHabitId` gesetzt.
- Der generierte Theme-Habit ist voll editierbar: Typ auf `COUNTER`/`AMOUNT` umstellbar, dann `target`, `unit`, `points` setzbar. Als reines Mark-Habit (`CHECK`) gibt es nichts weiter zu konfigurieren.
- Pro Tag genau ein Theme-Habit.
- Theme-Habits zählen wie jeder andere Habit ins Tagesziel.

**Theme-Bestimmung pro Tag** (genau ein Gewinner)

- Ein manuell gesetztes Thema gewinnt immer (expliziter Nutzereingriff).
- Gibt es genau einen Habit mit `givesTheme` an diesem Tag, wird er automatisch Theme-Habit.
- Gibt es mehrere Habits mit `givesTheme`, wählt der Nutzer, welcher das Tagesthema wird. Bis zur Wahl bleibt das Thema leer.

**Verhalten**

- Löschen eines Wochen-Habits: seine Unter-Habits werden mitgelöscht (Cascade, `onDelete = CASCADE` über `parentId`).
- Archivieren wirkt wie bei F1, ab dem nächsten Tag keine neue Materialisierung, alte `DayHabit`-Einträge bleiben.

### F9 Widgets und Schnell-Erfassung (V2)

Check-in ohne die App zu öffnen. Stärkster Hebel für Dranbleiben, bei vergleichbaren Apps das meistgenutzte Feature.

- Homescreen-Widget mit Jetpack Glance: heutige Habits, Status und Tagesthema, plus Ein-Tipp-Check-in direkt im Widget
- konfigurierbar: alle Habits des Tages oder eine Auswahl
- Quick-Settings-Tile: einen definierten Habit oder "Tag erfassen" mit einem Tipp abhaken
- Notification-Action: Erinnerung (F5) bekommt Aktions-Buttons zum direkten Abhaken
- alles lokal: Widget liest aus Room, schreibt `DayHabit`, stößt Statusneuberechnung an

### F10 Langzeitziele pro Habit (V3)

Ziel über einen längeren Zeitraum, unabhängig vom Tagesziel.

Tabelle `Goal`:

| Feld | Typ | Angabe |
|---|---|---|
| `id` | Long | Primärschlüssel |
| `habitId` | Long | Bezug zum Habit |
| `targetCount` | Int | z. B. 30 Erfüllungen |
| `periodStart` | LocalDate | Beginn |
| `periodEnd` | LocalDate | Ende bzw. Deadline |
| `reward` | String? | frei, z. B. "Kinoabend" |
| `achieved` | Boolean | erreicht |

Verhalten: Fortschritt = Anzahl erfüllter Tage des Habits im Zeitraum, live berechnet. Anzeige im Habit-Detail. Bei Erreichen eine dezente, kurze Abschluss-Animation (kein Dauereffekt, passt zum schlichten Design).

### F11 Abstinenz- und Reduktions-Habits (V2)

Für Gewohnheiten, die man lassen oder reduzieren will (nicht rauchen, weniger Zucker).

- `Habit.polarity` (Enum): `GOOD` (Standard, aufbauen) oder `BAD` (vermeiden)
- bei `BAD`: erfüllt = "cleaner Tag" ohne Vorfall. Der Streak ist die Abstinenz-Serie (Tage seit letztem Vorfall)
- Erfassung invertiert: ein gemeldeter Vorfall setzt den Tag auf nicht erfüllt und die Serie zurück
- "Tage seit"-Zähler prominent im Detail
- zählt ins Tagesziel wie ein normaler Habit (cleaner Tag gibt Punkte)

### F12 Kategorien, Tags, Sortierung und Filter (V2)

Ordnung, sobald es viele Habits gibt.

- `Habit.category` (String?): eine Kategorie je Habit (z. B. Gesundheit, Arbeit)
- `Habit.tags` (Set&lt;String&gt;): mehrere Schlagworte, Persistenz über TypeConverter
- `Habit.sortIndex` (Int): manuelle Reihenfolge per Drag-and-Drop
- Listen (Heute, Habit-Verwaltung): Filter nach Kategorie/Tag, optionale Gruppierung nach Kategorie, Sortierung nach `sortIndex`, Name oder Streak
- Umschalter der Darstellung direkt in der Ansicht, nicht in den Einstellungen (typischer Kritikpunkt bei minimalistischen Trackern)

### F13 Vorlagenbibliothek und Onboarding (V3)

Schnellstart beim ersten Start.

- kuratierte, lokale Liste von Standard-Habits (Sport, Wasser, Lesen, Meditation ...), rein statisch, kein Netz
- beim Onboarding mehrere Vorlagen auswählbar, werden als normale Habits angelegt
- später über "Habit aus Vorlage" erreichbar

---

## 5. Nicht-Ziele (V1)

- keine Cloud, kein Konto, keine Synchronisation
- kein Multi-User, keine Familienfreigabe, kein Vergleich mit anderen
- kein Teilen/Export einzelner Habits als Bild
- kein Wear-OS-Begleiter (bewusst zurückgestellt, kein Ziel dieser Roadmap)

---

## 6. Festgelegte Design-Entscheidungen

1. Akzentfarbe: **Indigo**
2. Standard-Tagesziel-Regel: **`POINTS`**
3. Leerer Tag ohne Habits: **neutral** (`NEUTRAL`), zählt nicht als bestanden oder nicht bestanden und bricht die Streak nicht

---

## 7. Festgelegte Entscheidungen (Wochen-Habits)

1. Geltungsbereich: Wochen-Habit ist an eine konkrete Kalenderwoche gebunden (`weekStart` = Montag). Wochenspanne wählbar, `WORKWEEK` (Mo bis Fr) oder `FULL` (Mo bis So).
2. Unter-Habit: einem oder mehreren Wochentagen zuordenbar (`assignedDows`) innerhalb der Wochenspanne. Pro Tag ein eigener `DayHabit`, Fortschritt zählt je Tag getrennt.
3. Theme-Konflikt: bei mehreren `givesTheme`-Kandidaten an einem Tag wählt der Nutzer.
4. Löschen eines Wochen-Habits löscht die Unter-Habits mit (Cascade).
5. Ein aus einem Thema generierter Habit (`isThemeGenerated`) bleibt tages-lokal (`SIMPLE`), keine Beförderung zu Wochen- oder Unter-Habit.

---

## 8. Roadmap

**Phase 1, MVP (Grundgerüst)**

F1 Habit-Verwaltung inkl. Notizfeld, F2 Tagesverwaltung inkl. Tagesnotiz, F3 Tageserfassung, F4 Basis (Tages-Streak, längste Streak, Abschlussquote, Heatmap), F6 ZIP-Backup, F7 Basis-Einstellungen.

Wichtig: alle Erweiterungsfelder schon im Schema anlegen (F8 Hierarchie, F4 `streakRule`/`perWeekTarget`, F11 `polarity`, F12 `category`/`tags`/`sortIndex`), damit V2 ohne Room-Migration auskommt. Tabellen `Goal` und `Pause` können leer mit angelegt werden.

**Phase 2, V2 (Komfort und Tiefe)**

F5 Erinnerungen, F9 Widgets und Schnell-Erfassung, F8 Wochen-Habits, F4-Ausbau (flexibler Streak, Streak-Schutz, Pause/Urlaub, erweiterte Statistik), F11 Abstinenz-Habits, F12 Kategorien/Tags/Filter/Sortierung, CSV-Export (F6). Merge-Import optional.

Reihenfolge-Empfehlung: erst F5 und F9 zusammen (Notification-Action verbindet beide), dann F4-Ausbau, dann F8, danach F11 und F12.

**Phase 3, V3 (Feinschliff)**

F10 Langzeitziele mit Abschluss-Animation, F13 Vorlagenbibliothek und Onboarding, weiterer Statistik-Ausbau.

---

## Fazit

Der MVP bleibt schlank (F1 bis F4 Basis, F6, F7), trägt aber schon das vollständige Schema, damit V2 ohne Migration draufsatteln kann. Die drei stärksten V2-Erweiterungen für den Alltag sind Widgets mit Ein-Tipp-Check-in (F9), flexibler Streak plus Streak-Schutz und Pause (F4) sowie Ordnung über Kategorien und Filter (F12). Tagesthema und Wochen-Habit-System (F2, F8) bleiben die inhaltliche Differenzierung, Cloud und Social bleiben bewusst außen vor.
