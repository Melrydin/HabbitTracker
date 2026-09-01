# Habit Tracker

A local habit tracker for Android. No account, no cloud, no sync — the app
deliberately has **no `INTERNET` permission** and must not gain one.

The requirements live in [`docs/habit_tracker_featureliste.md`](docs/habit_tracker_featureliste.md)
and define the features F1 to F7. MVP scope: F1, F2, F3, F4 basics, F6, F7 basics. V2:
reminders, richer statistics, merge import, widgets.

## Language

**The entire codebase is written in English** — comments, KDoc, identifiers, test names,
string resources, commit messages and this document. The one exception is the requirements
document under `docs/`, which is the original German source and stays as written; the spell
checker skips it.

## Stack

| Item | Value |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, repository, StateFlow |
| Persistence | Room (SQLite) for data, DataStore for settings, local only |
| Navigation | `navigation-compose`, string routes |
| minSdk / targetSdk | 26 / 37 |
| Permissions | none (later only `POST_NOTIFICATIONS` for F5) |

AGP 9 brings its own Kotlin support; there is no separate `kotlin-android` plugin here.
Dependencies go through the version catalog in `gradle/libs.versions.toml`.

**Two JDKs are in play.** The Gradle daemon runs on 25, declared in
`gradle/gradle-daemon-jvm.properties`, while Kotlin compiles against the 17 toolchain pinned
in `app/build.gradle.kts`. Kotlin 2.2 cannot use JDK 25 as a compile target and would
silently fall back, so the toolchain makes the target explicit instead of letting it follow
whatever JVM happens to run the build. CI installs both versions for the same reason.

## Commands

Build:

```bash
./gradlew :app:assembleDebug
```

Test:

```bash
./gradlew :app:testDebugUnitTest
```

Check and fix the code style:

```bash
./gradlew ktlintCheck
```

```bash
./gradlew ktlintFormat
```

Spell check:

```bash
npx cspell lint -c .github/cspell.json --no-progress "**/*.{kt,kts,xml,yml,yaml,md,toml}"
```

Everything at once, exactly what CI runs:

```bash
./gradlew ktlintCheck :app:lintDebug :app:assembleDebug :app:testDebugUnitTest
```

## Layout

```
app/src/main/java/com/example/habbittracker/
├── domain/           business rules, plain Kotlin, no Android imports
│   ├── model/        Habit, Day, HabitEntry, Goal, Pause
│   ├── DayEvaluator      daily goal evaluation (F2)
│   ├── DayHabits         which habits belong to a day (F1, F3)
│   ├── StreakCalculator  current and longest streak (F4)
│   └── CompletionRate    share of passed among judged days (F4)
├── data/             HabitRepository, SettingsRepository
│   ├── local/        entities, DAOs, type converters, HabitDatabase, DataStore
│   └── backup/       ZIP export and restore (F6)
├── ui/
│   ├── theme/        colors, typography, shapes
│   ├── components/   building blocks shared across screens (BackTopAppBar, EmptyState,
│   │                 LabeledSection, SettingRow, ValueStepper, SegmentedChoice, …)
│   ├── icons/        HabitIcons: name from Habit.icon → ImageVector
│   ├── navigation/   HabitNavHost, Routes
│   ├── today/        today screen (F2, F3)
│   ├── habit/        habit editor and habit list (F1)
│   ├── history/      heatmap, streaks, completion rate (F4)
│   └── settings/     theme, default goal, backup (F6, F7)
├── HabbitTrackerApp  Application + AppContainer (service locator)
└── MainActivity
```

`domain/` must not import anything from `android.*` or Compose — that is what keeps the
business rules testable in fast JVM tests.

## Persistence

Room stores habits, days and recorded values in three tables. `data/local/` holds the
entities, DAOs and converters; `RoomHabitRepository` maps them onto the domain models and
keeps `Day.passed` in step.

* Dates are stored as ISO-8601 text and enums by name, so the database stays readable when
  inspected by hand and dates still sort correctly. Sets (`assignedDows`, `tags`) are stored
  as comma separated text.
* **Every V2 and V3 field is already in the schema** (F4 `streakRule`/`perWeekTarget`, F8
  hierarchy and theme coupling, F11 `polarity`, F12 `category`/`tags`/`sortIndex`), and the
  `goals` and `pauses` tables exist empty. That is what the roadmap asks for, so those
  features land without another migration.
* `day_habits` has a foreign key onto `habits` with `ON DELETE CASCADE`. Deleting a habit
  takes its recorded values with it; archiving leaves those rows untouched, which is what
  keeps old entries visible.
* Schemas are exported to `app/schemas/` and checked in, so a future migration can diff
  against them. Bump `HabitDatabase.SCHEMA_VERSION` together with a migration — there is
  deliberately **no destructive fallback**, losing a user's history silently is worse than a
  crash that shows up in testing.

`RoomHabitRepository` is covered by JVM tests through Robolectric, so the DAOs, converters and
the cascade run in the normal `testDebugUnitTest` pass rather than only on a device. The
in-memory `InMemoryHabitRepository` lives in the test sources as a fast fake; both share the
rules in `domain/`, so they cannot drift apart on the parts that matter.

KSP registers its generated sources through the `kotlin.sourceSets` DSL, which AGP 9's
built-in Kotlin support rejects. `android.disallowKotlinSourceSets=false` in
`gradle.properties` is the documented escape hatch and is required for Room to build; AGP 9.3
ships no built-in KSP to fall back on. `android.sync.suppressAgpWarnings` silences the
resulting experimental-option warning — and, be aware, every other one of that kind too.

## Backup

`data/backup/` writes a ZIP of six JSON files (F6). Its models are deliberately **separate
from both the domain models and the Room entities**: a backup written today has to stay
readable after either of those is refactored, so the file format can stand still while the
code moves.

* Reading is lenient downwards only. Missing fields take their defaults, so an older backup
  restores without a migration step, and an unknown enum value falls back instead of failing
  the whole restore. A **newer** schema is refused rather than guessed at.
* Restoring replaces rather than merges (V1) and runs in one transaction, so a damaged file
  leaves the existing data untouched.
* Habits are inserted parents first: a sub habit references its weekly parent and the foreign
  key is checked per row.
* `ZipBackupRepository` only reads and writes streams; `BackupManager` does the URI handling,
  which keeps the format testable without Android.

## Business rules

The rules for daily goals and streaks live as **pure functions** in `domain/` and are shared
by repository, view models and tests. New rules belong there, not in a view model and not in
a composable.

Three behaviors that are not obvious from the code and are pinned down by tests:

* `Day.status` is three-valued: `PASSED`, `FAILED`, `NEUTRAL`. A day with no reachable goal is
  **neutral**, not failed, and the UI then shows no status marker at all.
* A **neutral day is skipped** by the streak: it neither extends nor breaks a run. Only
  `FAILED` resets it.
* **Only `POINTS` carries a threshold.** `MIN_COUNT` asks for every habit of the day and
  `ALL_REQUIRED` for the ones marked as such, so neither can be set beyond reach. The points
  threshold is capped at the points the day actually holds — a bar above that would make the
  day impossible to pass.
* A **today that is still open does not break the streak** — the run is counted up to
  yesterday. Otherwise the streak would read 0 every morning.
* An **archived habit stays visible** on days that already have a value recorded. Archiving
  therefore cannot retroactively ruin a day that had passed.

`Day.status` is recomputed and stored after every change — not only after tracking a value,
but also after edited points or an archived habit.

## Design system

Guiding idea: modern, plain, calm. Plenty of whitespace, flat, separation through color and
spacing rather than shadow.

* **One accent plus one status tone, on top of neutrals.** The accent is Indigo, the "passed"
  marker a restrained green. Both live in `ui/theme/Color.kt` and are swappable there.
* Green is a marker only and never fills an area, so "done" reads at a glance without the
  screen turning green.
* Open elements stay neutral grey. There is **no red** for "not fulfilled", so that nothing is
  being judged.
* A uniform 16 dp radius, an 8 dp grid, 16 to 24 dp of inner padding.
* One icon set: Material Symbols outlined, never mixed.
* Dark mode is fully supported and follows the system.
* Material You dynamic color is **off** (`dynamicColor = false`) to keep the look consistent.
  The parameter is ready for F7/V2.

Never write color literals inside a composable — always go through `MaterialTheme.colorScheme`
or `HabitTheme.status`.

## Code rules

* **Functions at most 20 lines, lines at most 120 characters.**
* Follow [DRY][dry] and [SRP][srp].
* **Only one level of abstraction per function.**
* **Comments** explain complex or non-obvious logic — the *why*, not the *what*.
* **Static types** where they help readability: explicit return types on public functions, no
  `Any`, never pass platform types from Java interop through unchecked.

Formatting is enforced by **ktlint** (`ktlint_official`), configured in `.editorconfig`. Two
rules are deliberately disabled there: `function-signature` and `class-signature` break every
signature with two or more parameters onto separate lines, even when it fits comfortably
within 120 characters. That lengthens functions by roughly a third and works directly against
the 20 line rule, while `max_line_length` already covers the line length itself.

[dry]: https://en.wikipedia.org/wiki/Don%27t_repeat_yourself
[srp]: https://en.wikipedia.org/wiki/Single-responsibility_principle

### The 20 line rule and Compose

For logic — view models, repository, `domain/` — the limit is hard. A declarative layout tree
inside a `@Composable` may be longer, under two conditions:

1. The function renders **one** coherent section of UI. As soon as it renders two, it gets
   split (that is how `TodayHeader`, `DangerZone` and `SettingCard` came to be).
2. Logic inside a composable stays within the 20 lines. Computing, formatting and deriving
   belong in the view model or in a function of their own, not between the layout calls.

## Branching

A solo project, so trunk based development:

* All work is committed **directly to `main`**, in small focused commits (see commit messages
  below).
* The CI pipeline runs on every push and keeps `main` healthy: `.github/workflows/ci.yml`
  checks code style (ktlint), spelling (cspell), Android Lint, the build and the unit tests on
  `ubuntu-latest`. On a red run the reports are attached to the job as an artifact.
* Should the project gain further contributors, a feature branch and pull request workflow can
  be reintroduced.

## Commit messages

```
<type>(<optional scope>): <description>

<optional body>

<optional footer>
```

Merge and revert commits keep the default git message (`Merge branch '<name>'` and
`Revert "<subject>"`). The first commit of a repository is `chore: init`.

### Types

| Type | Purpose |
|---|---|
| `feat` | adds or removes a feature of the API or UI |
| `fix` | fixes an API or UI bug of a preceding `feat` |
| `refactor` | rewrites code **without** changing API or UI behavior |
| `perf` | a special `refactor` that improves performance |
| `style` | formatting, whitespace, wrapping — nothing that changes meaning |
| `test` | adds missing tests or corrects existing ones |
| `docs` | documentation only |
| `build` | build tools, dependencies, project version, CI pipeline |
| `ops` | infrastructure, deployment, backup, recovery |
| `chore` | miscellaneous, for example `.gitignore` |

### Scopes

Optional, adds context. Never use issue identifiers as a scope. Established in this project:
`today`, `habit`, `navigation`, `theme`, `data`, `domain`.

### Breaking changes

Marked by a `!` before the `:`, for example `feat(data)!: reshape the schema`. A breaking
change **must** also be described in the footer, starting with `BREAKING CHANGE:`.

### Description

Mandatory. Concise, imperative present tense ("change", not "changed"), starting **lowercase**,
no full stop at the end. Think: *This commit will…*

### Body

Optional. States the **motivation** and contrasts it with the previous behavior, also in
imperative present tense. Issue references belong here.

### Footer

Optional. Carries breaking changes and issue references.

### Versioning

If the next release contains **breaking changes**, the major version goes up; if it contains
API relevant changes (`feat` or `fix`), the minor version; otherwise the patch version.

### Examples

```
feat(habit): wire the editor into the today screen
```

```
fix(today): stop treating an empty day as failed
```

```
refactor(domain): split daily goal evaluation per rule
```

```
build: update dependencies
```

```
feat(data)!: move tracking onto materialized day rows

BREAKING CHANGE: backups written before schema 2 can no longer be imported.
```

On top of that: **one commit, one topic**, and every commit builds and passes the tests
(`./gradlew :app:assembleDebug :app:testDebugUnitTest`).

The convention follows the [gist by Bengt Brodersen][gist], which in turn builds on the
Conventional Commits popularized by Angular.

[gist]: https://gist.github.com/qoomon/5dfcdf8eec66a051ecd85625518cfd13

## Organization and naming

* **Group by domain**, not by technical type: `today/`, `habit/`, `theme/` — there is
  deliberately no `viewmodels/` or `screens/`.
* **Every folder and file follows the same conventions.** Consistent naming lowers cognitive
  overhead and makes navigation predictable.

The casing follows what each platform requires:

| What | Casing | Example |
|---|---|---|
| Package folders | lowercase, single word | `domain/model`, `ui/today` |
| Kotlin files | PascalCase, named after the class inside | `HabitRepository.kt` |
| Resources under `res/` | `snake_case` | `values-night/themes.xml` |
| String keys | `snake_case`, prefixed by screen | `today_goal_label` |
| Icon names in `Habit.icon` | `snake_case`, as in Material Symbols | `water_drop` |

> The original rule asked for `snake_case` across all directories and structural files. It came
> from a Godot project. On Android it therefore applies where the platform enforces it: the
> resource system only accepts `[a-z0-9_]` under `res/`. Kotlin source files keep the name of
> their class, otherwise Android Studio, Lint and every jump-to-file in the project work
> against you. The intent — one convention, applied everywhere — is preserved.

## Spell checking

`.github/cspell.json` configures the checker and `.github/cspell-project.txt` holds the project
vocabulary; both live next to the workflow that runs them. cspell does not look inside
`.github/` on its own, so every invocation passes `-c .github/cspell.json`.

Add a word to the vocabulary only when it is a real term — a toolchain identifier, a library
name, a proper noun. Never add a word to silence an actual typo.

`.editorconfig` stays in the project root on purpose: ktlint and the IDEs only pick it up from
there.

## State

Done: today screen (F2, F3, F4 basics) and habit management (F1) including create, edit,
archive and delete.

Open:

**Phase 1 of the roadmap is complete**: F1, F2, F3, F4 basics, F6 and F7 basics all have data,
rules and UI, and the schema already carries every V2 and V3 field.

Open:

* **Phase 2 (V2)** has not started: reminders (F5), widgets (F9), weekly habits (F8), the
  statistics build-out (F4), abstinence habits (F11), categories and tags (F12), CSV export.
* The `goals` and `pauses` tables exist but stay empty until F10 and F4 need them.
* `Habit.colorTag` exists in the data model but deliberately not in the editor — a per-habit
  color picker contradicts the one-color rule above.
* The project name is spelled **Habbit** with two b's, in the app name, the package
  `com.example.habbittracker` and the Gradle project. It is carried in the spell checker
  vocabulary rather than corrected, because renaming touches the package and the application
  id.
* `material-icons-extended` inflates the debug APK to roughly 19 MB. For release either keep R8
  on (the template sets `enable = false`) or reduce the catalog to hand-picked vectors.
