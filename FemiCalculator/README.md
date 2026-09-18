# All in One Calculator

A fully offline scientific calculator app for Android, written in Kotlin.

## Features

- **Scientific calculator**: sin/cos/tan + inverses, sinh/cosh/tanh, log, ln,
  powers, roots, factorial, π, e, DEG/RAD toggle, memory (M+/M-/MR/MC),
  fraction display, superscript exponent display, and persistent
  calculation history. Common functions sit in a compact row; a "More"
  toggle expands the rest without needing to scroll.
- **Graph**: plot y = f(x) for one or more functions at once (e.g. `x^2`,
  `sin(x)`, `2*x+3`), with pinch-to-zoom and drag-to-pan.
- **Matrices**: addition, subtraction, multiplication, determinant, inverse,
  transpose, and solving linear systems (Ax = b).
- **Finance**: simple interest, compound interest, future value, present
  value, loan payment, annuity (FV/PV), and solving for rate or time.
- **More**: statistics (mean/median/mode/std dev), percentage tools, ratio
  tools, quadratic equation solver, geometry (2D and 3D shapes), and a unit
  converter (length, weight, volume, temperature).
- **Science**: electrolysis / electrochemical cell calculations (mole, mass,
  volume, and charge in terms of each other, cell E.m.f., Farad-to-Coulomb
  conversion, solving for electron-to-mole ratio) and radioactive half-life
  calculations (solving for half-life, remaining fraction, or elapsed time).
- **History**: every calculator screen's results are reachable from
  clearly visible buttons on the main screen — no hidden menus.

No internet permission is requested — everything runs and is stored
entirely on-device (SharedPreferences for history/memory).

## Opening the project in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Giraffe or newer recommended).
2. Unzip this project.
3. In Android Studio, choose **File → Open**, and select the unzipped
   `FemiCalculator` folder (the one containing `settings.gradle`).
4. Android Studio will detect the project and start a Gradle sync.
   - **Gradle wrapper note**: this project ships with a
     `gradle/wrapper/gradle-wrapper.properties` pointing at a specific
     Gradle version, but the wrapper jar itself isn't included (it's a
     binary file and this project was generated without network access).
     Android Studio will either auto-generate the wrapper for you on first
     sync, or you can go to **File → Settings → Build, Execution,
     Deployment → Gradle** and select "Use Gradle from: 'Specified
     location'" pointing at a Gradle installation Android Studio already
     bundles. Either path works fine.
5. Once the sync finishes, select **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
6. The generated APK will be under `app/build/outputs/apk/debug/`.
7. To install and try it immediately, just click the green **Run ▶** button
   with a device or emulator selected.

## Project structure

```
app/src/main/java/com/femi/calculator/
├── engine/          # Pure Kotlin math: expression evaluator, matrices,
│                    # finance formulas, statistics/geometry/units, fractions,
│                    # electrochemistry/half-life formulas
├── ui/              # Activities + views (MainActivity, GraphActivity,
│                    # MatrixActivity, FinanceActivity, MoreActivity,
│                    # ScienceActivity, HistoryActivity, GraphView custom view)
└── util/            # SharedPreferences-backed HistoryStore & MemoryStore
```

- `minSdk 24` (Android 7.0+), `targetSdk 34`.
- Kotlin + View Binding, no third-party math libraries — the expression
  parser (shunting-yard algorithm) and all formulas are hand-written so the
  app has zero external dependencies beyond standard AndroidX/Material.

## Notes / things you may want to tweak

- The app icon (`res/drawable/ic_launcher.xml`) is a simple placeholder
  vector icon — swap in your own artwork via Android Studio's Image Asset
  Studio (**File → New → Image Asset**) whenever you like.
- The color palette lives in `res/values/colors.xml` if you want a
  different look.

## Repackaged calculator update

The project now includes the requested calculator redesign and study tools:

- Main calculator keypad redesigned to closely match the supplied reference image: DEG/M, SHIFT, MENU, cursor arrows, DEL/AC, STATISTICS/MATRICES/SCIENCE, scientific-function rows, memory controls, CHANGE, five-column keypad and yellow ENTER.
- CHANGE opens an a–z variable picker and rearranges supported equations to make the selected variable the subject.
- Dedicated a→z variable picker.
- Fraction entry dialog accepts numbers or alphabetic variables in numerator and denominator.
- d/dx and ∫dx controls; definite integration accepts lower/upper bounds, while empty bounds use the symbolic integrator for common polynomials, sin, cos and e^x.
- logₓ(y) dialog accepts both base and y.
- Statistics ANOVA, two-way ANOVA, Spearman and regression now use editable grid tables with adjustable rows/columns and the requested default layouts.
- Grouped raw data supports an optional first class interval and outputs a grid containing class interval, midpoint, f and cf.
- Ogive tracing includes Q1, Median and Q3 construction lines.
- Matrix workspace supports multiple side-by-side matrices (A–J) and expressions such as `0.2A^2 - 3B + 4AB`, including scalar-matrix multiplication and matrix powers.
- Science includes a structure engine with common names/formulas such as sulphur(IV) oxide / SO2, H2O, CO2, NH3 and CH4.
