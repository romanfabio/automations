# Third-party licenses

This project is released under the [MIT License](LICENSE). It uses the following third-party software.
All of it is available under permissive licenses compatible with MIT. No third-party images, fonts, icons or other
assets are included: the launcher icon is original artwork drawn for this project, and the UI uses the system fonts
and the Material 3 color system from the libraries below.

License information was read from each dependency's published POM metadata (Gradle `releaseRuntimeClasspath`,
Gradle 8.14.3, dependency versions as pinned in `gradle/libs.versions.toml`). Re-check it whenever dependencies
change.

## Shipped inside the app (runtime)

All 112 resolved runtime artifacts are licensed under the **Apache License 2.0**
(<https://www.apache.org/licenses/LICENSE-2.0>). In groups:

| Library | Version | License |
|---|---|---|
| AndroidX Core, Activity, Lifecycle, Savedstate, Startup, Tracing, SQLite, Room, Window and related | see `libs.versions.toml` | Apache-2.0 |
| Jetpack Compose (runtime, UI, foundation, animation, Material 3) | BOM 2026.06.01 | Apache-2.0 |
| Kotlin standard library | 2.2.21 | Apache-2.0 |
| kotlinx.coroutines | 1.10.2 | Apache-2.0 |
| JetBrains annotations | 23.0.0 | Apache-2.0 |
| JSpecify | 1.0.0 | Apache-2.0 |
| Guava `listenablefuture` | 1.0 | Apache-2.0 (inherited from the Guava parent POM) |

If you redistribute a built APK, the Apache-2.0 license asks you to include a copy of the license and keep the
libraries' notices; this file and the link above serve that purpose.

## Build-time only (not shipped)

| Tool | Version | License |
|---|---|---|
| Android Gradle Plugin | 8.13.1 | Apache-2.0 |
| Kotlin Gradle plugin and Compose compiler plugin | 2.2.21 | Apache-2.0 |
| Kotlin Symbol Processing (KSP) | 2.3.12 | Apache-2.0 |
| Room compiler | 2.8.5 | Apache-2.0 |
| Gradle (wrapper) | 8.14.3 | Apache-2.0 |

## Test only (not shipped)

| Library | Version | License |
|---|---|---|
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 |
| Hamcrest Core | 1.3 | BSD 3-Clause |

JUnit and Hamcrest are used only to run the unit tests on a development machine and are not part of the app or of
this repository's source.
