# Building AMOS Plugin

This file contains development and packaging commands for the plugin.

## Requirements

- JDK 21
- Gradle wrapper (`./gradlew`)

## Common tasks

```bash
./gradlew --no-daemon test
./gradlew --no-daemon build
./gradlew --no-daemon buildPlugin
```

## Run in sandbox IDE

```bash
./gradlew --no-daemon runIde
```

## Build distributable ZIP

```bash
./gradlew --no-daemon buildPlugin
```

Output ZIP:

- `build/distributions/*.zip`

## Targeted tests

```bash
./gradlew --no-daemon test --tests "dev.rambris.amigaamos.lang.amos.AmosFormattingTest"
./gradlew --no-daemon test --tests "dev.rambris.amigaamos.lang.amos.AmosDefinitionRegistryTest"
./gradlew --no-daemon test --tests "dev.rambris.amigaamos.lang.amal.AmalLexerTest"
```

## If runIde sandbox gets stale

After extension-point changes, sandbox/cache state can cause startup issues.

```bash
./gradlew --stop
rm -rf build/idea-sandbox
rm -rf .gradle/configuration-cache
./gradlew --no-daemon clean runIde --rerun-tasks --console=plain
```

`build.gradle.kts` includes `-Dide.experimental.ui=false` as a 2025.2 sandbox workaround.

