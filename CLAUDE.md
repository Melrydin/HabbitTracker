# Habit Tracker

Lokaler Habit-Tracker für Android. Kein Konto, keine Cloud, keine Synchronisation — die App
hat bewusst **keine `INTERNET`-Permission** und soll auch keine bekommen.

Anforderungsquelle ist `~/Downloads/habit_tracker_featureliste.md` (liegt außerhalb des Repos
und ist nicht eingecheckt). Sie definiert die Features F1 bis F7. MVP-Umfang: F1, F2, F3,
F4-Basis, F6, F7-Basis. V2: Erinnerungen, Statistik-Ausbau, Merge-Import, Widgets.

## Stack

| Punkt | Angabe |
|---|---|
| Sprache | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architektur | MVVM, Repository, StateFlow |
| Persistenz | **noch keine** — `InMemoryHabitRepository` ist Platzhalter für Room |
| Navigation | `navigation-compose`, String-Routen |
| minSdk / targetSdk | 26 / 37 |
| Berechtigungen | keine (später nur `POST_NOTIFICATIONS` für F5) |

AGP 9 bringt Kotlin-Unterstützung mit; ein separates `kotlin-android`-Plugin gibt es hier
nicht. Dependencies laufen über den Version-Catalog in `gradle/libs.versions.toml`.

## Befehle

Bauen:

```bash
./gradlew :app:assembleDebug
```

Tests:

```bash
./gradlew :app:testDebugUnitTest
```

Beides zusammen, so wird vor jedem Commit geprüft:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## Aufbau

```
app/src/main/java/com/example/habbittracker/
├── domain/           Fachlogik, reines Kotlin, kein Android-Import
│   ├── model/        Habit, Day, DayHabit, HabitEntry
│   ├── DayEvaluator      Tagesziel-Auswertung (F2)
│   └── StreakCalculator  aktuelle und längste Serie (F4)
├── data/             HabitRepository (Interface) + InMemoryHabitRepository
├── ui/
│   ├── theme/        Farben, Typografie, Formen
│   ├── components/   screenübergreifende Bausteine (ProgressTrack, StatusPill)
│   ├── icons/        HabitIcons: Name aus Habit.icon → ImageVector
│   ├── navigation/   HabitNavHost, Routes
│   ├── today/        Heute-Screen (F2, F3)
│   └── habit/        Habit-Editor und Habits-Liste (F1)
├── HabbitTrackerApp  Application + AppContainer (Service-Locator)
└── MainActivity
```

`domain/` darf nichts aus `android.*` oder Compose importieren — daran hängt, dass die
Fachlogik in schnellen JVM-Tests prüfbar bleibt.

## Fachlogik

Die Regeln für Tagesziel und Streak liegen als **reine Funktionen** in `domain/` und werden
von Repository, ViewModel und Tests gemeinsam benutzt. Neue Regeln gehören dorthin, nicht in
ein ViewModel und nicht in einen Composable.

Drei Verhaltensweisen, die man dem Code nicht ansieht und die durch Tests abgesichert sind:

* Ein Tag ohne erreichbares Ziel ist **neutral**, nicht „nicht bestanden". Das UI zeigt dann
  gar keinen Statusmarker, statt Versagen zu behaupten.
* Ein noch offenes **Heute bricht die Streak nicht** — gezählt wird bis gestern weiter. Sonst
  stünde die Serie jeden Morgen auf 0.
* Ein **archivierter Habit bleibt** an Tagen sichtbar, an denen schon etwas erfasst wurde.
  Damit kann Archivieren einen bestandenen Tag nicht nachträglich kaputtmachen.

`Day.passed` wird nach jeder Änderung neu berechnet und persistiert — nicht nur nach
Erfassungen, sondern auch nach geänderten Punkten oder einem archivierten Habit.

## Designsystem

Leitlinie: modern, schlicht, ruhig. Viel Weißraum, flach, Trennung über Farbe und Abstand
statt über Schatten.

* **Genau eine Farbe plus Neutraltöne.** Der Akzent ist Grün, zentral in `ui/theme/Color.kt`
  an einer Stelle austauschbar.
* Der Statusmarker „bestanden" teilt sich diesen Ton mit dem Akzent. Unterschieden wird über
  **Form** — Pille mit Haken für den Tag, Kreis mit Haken für den Habit —, nicht über Farbe.
* Grün heißt ausschließlich „erledigt". Offene Elemente bleiben neutral grau. Es gibt **kein
  Rot** für „nicht erfüllt", um nicht zu werten.
* Einheitlicher Radius 16 dp, 8-dp-Raster, Innenabstände 16 bis 24 dp.
* Ein Icon-Set: Material Symbols outlined, keine Mischung.
* Dark Mode wird vollständig unterstützt und folgt dem System.
* Material-You Dynamic Color ist **aus** (`dynamicColor = false`), damit das Bild konsistent
  bleibt. Der Parameter steht für F7/V2 bereit.

Farben nie direkt im Composable literalisieren — immer über `MaterialTheme.colorScheme` oder
`HabitTheme.status`.

## Code-Regeln

* **Funktionen höchstens 20 Zeilen, Zeilen höchstens 120 Zeichen.**
* [DRY][dry] und [SRP][srp] einhalten.
* **Nur eine Abstraktionsebene pro Funktion.**
* **Kommentare** erklären komplexe oder nicht offensichtliche Logik — das *Warum*, nicht das
  *Was*. Kommentare und UI-Texte sind auf Deutsch.
* **Statische Typen** nutzen, wo sie die Lesbarkeit verbessern: explizite Rückgabetypen bei
  öffentlichen Funktionen, kein `Any`, keine Plattformtypen aus Java-Interop ungeprüft
  weiterreichen.

[dry]: https://de.wikipedia.org/wiki/Don%E2%80%99t_repeat_yourself
[srp]: https://en.wikipedia.org/wiki/Single-responsibility_principle

### 20-Zeilen-Regel und Compose

Für Logik — ViewModels, Repository, `domain/` — gilt die Grenze hart. Ein deklarativer
Layout-Baum in einer `@Composable`-Funktion darf länger sein, aber unter zwei Bedingungen:

1. Die Funktion stellt **einen** zusammenhängenden UI-Abschnitt dar. Sobald sie zwei
   darstellt, wird sie geteilt (so entstanden `TodayHeader`, `DangerZone`, `SettingCard`).
2. Logik im Composable bleibt bei den 20 Zeilen. Rechnen, Formatieren und Ableiten gehören
   ins ViewModel oder in eine eigene Funktion, nicht zwischen die Layout-Aufrufe.

## Branching

Ein-Personen-Projekt, deshalb Trunk-Based Development:

* Alle Arbeit wird **direkt auf `main`** committet, in kleinen, fokussierten Commits (siehe
  Commit-Nachrichten unten).
* Die CI-Pipeline (Format, Lint, Rechtschreibung) läuft bei jedem Push und hält `main`
  gesund.
* Kommen weitere Mitwirkende dazu, lässt sich ein Feature-Branch- und Pull-Request-Workflow
  wieder einführen.

## Commit-Nachrichten

```
<type>(<optionaler scope>): <description>

<optionaler body>

<optionaler footer>
```

Merge- und Revert-Commits behalten die Standardnachricht von Git
(`Merge branch '<name>'` bzw. `Revert "<betreff>"`). Der erste Commit eines Repos ist
`chore: init`.

### Types

| Type | Wofür |
|---|---|
| `feat` | fügt der API oder dem UI ein Feature hinzu oder entfernt eines |
| `fix` | behebt einen API- oder UI-Fehler eines vorangegangenen `feat` |
| `refactor` | schreibt Code um, **ohne** API- oder UI-Verhalten zu ändern |
| `perf` | spezielles `refactor`, das die Performance verbessert |
| `style` | Formatierung, Leerzeichen, Umbrüche — nichts, was die Bedeutung ändert |
| `test` | ergänzt fehlende Tests oder korrigiert bestehende |
| `docs` | betrifft ausschließlich Dokumentation |
| `build` | Build-Werkzeuge, Dependencies, Projektversion, CI-Pipeline |
| `ops` | Infrastruktur, Deployment, Backup, Recovery |
| `chore` | Sonstiges, z. B. `.gitignore` |

### Scopes

Optional, gibt zusätzlichen Kontext. Keine Issue-IDs als Scope. In diesem Projekt bewährt:
`today`, `habit`, `navigation`, `theme`, `data`, `domain`.

### Breaking Changes

Ein `!` vor dem `:` markiert sie, z. B. `feat(data)!: schema neu schneiden`. Ein Breaking
Change **muss** zusätzlich im Footer beschrieben werden, beginnend mit `BREAKING CHANGE:`.

### Description

Pflichtteil. Knapp, Imperativ Präsens („ändere", nicht „geändert"), **klein** beginnend, kein
Punkt am Ende. Gedanklich: *Dieser Commit wird…*

### Body

Optional. Nennt die **Motivation** und stellt sie dem vorherigen Verhalten gegenüber. Ebenfalls
Imperativ Präsens. Hier gehören Issue-Bezüge hin.

### Footer

Optional. Trägt Breaking Changes und Issue-Referenzen.

### Versionierung

Enthält das nächste Release **Breaking Changes**, steigt die Major-Version; enthält es
API-relevante Änderungen (`feat` oder `fix`), die Minor-Version; sonst die Patch-Version.

### Beispiele

```
feat(habit): editor an den heute-screen anbinden
```

```
fix(today): leeren tag nicht als nicht bestanden werten
```

```
refactor(domain): tagesziel-auswertung je regel aufteilen
```

```
build: dependencies aktualisieren
```

```
feat(data)!: erfassung auf materialisierte tageszeilen umstellen

BREAKING CHANGE: bestehende Backups vor Schema 2 lassen sich nicht mehr importieren.
```

Zusätzlich gilt: **ein Commit, ein Thema**, und jeder Commit baut und besteht die Tests
(`./gradlew :app:assembleDebug :app:testDebugUnitTest`).

Die Konvention folgt dem [Gist von Bengt Brodersen][gist], der wiederum auf den von Angular
verbreiteten Conventional Commits aufbaut.

[gist]: https://gist.github.com/qoomon/5dfcdf8eec66a051ecd85625518cfd13

## Organisation und Benennung

* **Nach Domäne gruppieren**, nicht nach technischem Typ: `today/`, `habit/`, `theme/` — es
  gibt bewusst kein `viewmodels/` oder `screens/`.
* **Jeder Ordner und jede Datei folgt denselben Konventionen.** Konsistente Benennung senkt
  die kognitive Last und macht Navigation vorhersagbar.

Die Schreibweise richtet sich danach, was die jeweilige Plattform verlangt:

| Was | Schreibweise | Beispiel |
|---|---|---|
| Package-Ordner | klein, ein Wort | `domain/model`, `ui/today` |
| Kotlin-Dateien | PascalCase, wie die enthaltene Klasse | `HabitRepository.kt` |
| Ressourcen unter `res/` | `snake_case` | `values-night/themes.xml` |
| String-Keys | `snake_case`, Screen-Präfix | `today_goal_label` |
| Icon-Namen in `Habit.icon` | `snake_case`, wie Material Symbols | `water_drop` |

> Die ursprüngliche Regel lautete `snake_case` für alle Verzeichnisse und Strukturdateien.
> Sie stammt aus einem Godot-Projekt. Unter Android gilt sie deshalb dort, wo die Plattform
> sie erzwingt: das Ressourcensystem lässt in `res/` ausschließlich `[a-z0-9_]` zu.
> Kotlin-Quelldateien heißen dagegen wie ihre Klasse, sonst arbeiten Android Studio, Lint
> und jede Navigation im Projekt gegen einen. Die Absicht — eine Konvention, überall gleich —
> bleibt damit erhalten.

## Stand

Fertig: Heute-Screen (F2, F3, F4-Basis) und Habit-Verwaltung (F1) inklusive Anlegen,
Bearbeiten, Archivieren und Löschen.

Offen:

* **Room** — bis dahin überlebt kein Habit einen App-Neustart. Das Repository-Interface ist
  so geschnitten, dass ViewModel und UI dabei unverändert bleiben.
* Verlauf und Einstellungen sind im `HabitNavHost` noch leere Callbacks.
* `Habit.colorTag` steckt im Datenmodell, aber bewusst nicht im Editor — ein Farbwähler pro
  Habit steht gegen die Ein-Farb-Regel oben.
* `material-icons-extended` bläht das Debug-APK auf ~19 MB. Für Release entweder R8
  anlassen (steht im Template auf `enable = false`) oder den Katalog auf handverlesene
  Vektoren eindampfen.
