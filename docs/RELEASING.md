# Releasing

How a release is produced. The signing key is never stored in the repository.

## One-time: signing key

Create a keystore and keep it, with its password, in a safe place **outside the repo** (and back it up: if the key is
lost, users can no longer upgrade without uninstalling).

```bash
keytool -genkeypair -keystore automations-release.jks -alias automations -keyalg RSA -keysize 4096 \
  -validity 10000 -storetype PKCS12 -dname "CN=Your Name"
```

Write a properties file next to it (outside the repo; never commit it):

```
storeFile=/absolute/path/to/automations-release.jks
storePassword=...
keyAlias=automations
keyPassword=...
```

and export `AUTOMATIONS_KEYSTORE_PROPERTIES=/absolute/path/to/keystore.properties`. The Gradle build reads it; if it is
missing the release APK is simply left unsigned, so contributors can still build.

## Per release

1. Update `versionName` and `versionCode` in `app/build.gradle.kts`, the version line in `README.md`, and add an entry
   to `CHANGELOG.md`.
2. `./gradlew clean testDebugUnitTest lintRelease assembleRelease` with the signing variable set.
3. Verify and checksum the APK:
   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   cp app/build/outputs/apk/release/app-release.apk automations-<version>.apk
   sha256sum automations-<version>.apk > automations-<version>.apk.sha256
   ```
   The certificate fingerprint must equal the one published in the README.
4. Install that exact APK on a phone and run through [TESTING.md](TESTING.md) (at least: setup card, Run now, a
   scheduled run).
5. Commit, then tag and push: `git tag -a v<version> -m "Automations <version>"` and `git push origin v<version>`.
6. On GitHub: Releases > Draft a new release > choose the tag, paste the changelog entry, attach the APK and the
   `.sha256` file, publish.

## Notes

- Release builds are not minified (`isMinifyEnabled = false`); turning R8 on needs a full on-device re-test.
- The release manifest has no debug-only components (`DebugReceiver` lives in `app/src/debug`).
- `*.apk` and `/release/` are git-ignored, so built artifacts cannot be committed by accident.
