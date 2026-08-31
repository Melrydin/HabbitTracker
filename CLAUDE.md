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
| Persistence | **none yet** — `InMemoryHabitRepository` is a placeholder for Room |
| Navigation | `navigation-compose`, string routes |
| minSdk / targetSdk | 26 / 37 |
| Permissions | none (later only `POST_NOTIFICATIONS` for F5) |

AGP 9 brings its own Kotlin support; there is no separate `kotlin-android` plugin here.
Dependencies go through the version catalog in `gradle/libs.versions.toml`.

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
npx cspell lint --no-progress "**/*.{kt,kts,xml,yml,md,toml}"
```

Everything at once, exactly what CI runs:

```bash
./gradlew ktlintCheck :app:lintDebug :app:assembleDebug :app:testDebugUnitTest
```

## Layout

```
app/src/main/java/com/example/habbittracker/
├── domain/           business rules, plain Kotlin, no Android imports
│   ├── model/        Habit, Day, DayHabit, HabitEntry
│   ├── DayEvaluator      daily goal evaluation (F2)
│   └── StreakCalculator  current and longest streak (F4)
├── data/             HabitRepository (interface) + InMemoryHabitRepository
├── ui/
│   ├── theme/        colors, typography, shapes
│   ├── components/   building blocks shared across screens (ProgressTrack, StatusPill)
│   ├── icons/        HabitIcons: name from Habit.icon → ImageVector
│   ├── navigation/   HabitNavHost, Routes
│   ├── today/        today screen (F2, F3)
│   └── habit/        habit editor and habit list (F1)
├── HabbitTrackerApp  Application + AppContainer (service locator)
└── MainActivity
```

`domain/` must not import anything from `android.*` or Compose — that is what keeps the
business rules testable in fast JVM tests.

## Business rules

The rules for daily goals and streaks live as **pure functions** in `domain/` and are shared
by repository, view models and tests. New rules belong there, not in a view model and not in
a composable.

Three behaviors that are not obvious from the code and are pinned down by tests:

* A day with no reachable goal is **neutral**, not failed. The UI then shows no status marker
  at all rather than claiming failure.
* A **today that is still open does not break the streak** — the run is counted up to
  yesterday. Otherwise the streak would read 0 every morning.
* An **archived habit stays visible** on days that already have a value recorded. Archiving
  therefore cannot retroactively ruin a day that had passed.

`Day.passed` is recomputed and stored after every change — not only after tracking a value,
but also after edited points or an archived habit.

## Design system

Guiding idea: modern, plain, calm. Plenty of whitespace, flat, separation through color and
spacing rather than shadow.

* **Exactly one color plus neutrals.** The accent is green, swappable in one place in
  `ui/theme/Color.kt`.
* The "passed" status marker shares that tone with the accent. The two are told apart by
  **shape** — a pill with a check for the day, a circle with a check for the habit — never by
  color.
* Green means "done" and nothing else. Open elements stay neutral grey. There is **no red**
  for "not fulfilled", so that nothing is being judged.
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
  a **self-hosted runner**. On a red run the reports are attached to the job as an artifact.
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

`cspell.json` configures the checker; `cspell-project.txt` holds the project vocabulary. Add a
word there only when it is a real term — a toolchain identifier, a library name, a proper noun.
Never add a word to silence an actual typo.

## State

Done: today screen (F2, F3, F4 basics) and habit management (F1) including create, edit,
archive and delete.

Open:

* **Room** — until then no habit survives an app restart. The repository interface is cut so
  that view models and UI stay untouched by that change.
* History and settings are still empty callbacks in `HabitNavHost`.
* `Habit.colorTag` exists in the data model but deliberately not in the editor — a per-habit
  color picker contradicts the one-color rule above.
* The project name is spelled **Habbit** with two b's, in the app name, the package
  `com.example.habbittracker` and the Gradle project. It is carried in the spell checker
  vocabulary rather than corrected, because renaming touches the package and the application
  id.
* `material-icons-extended` inflates the debug APK to roughly 19 MB. For release either keep R8
  on (the template sets `enable = false`) or reduce the catalog to hand-picked vectors.
