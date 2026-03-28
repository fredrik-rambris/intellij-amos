# AMOS Professional Language Plugin

IntelliJ plugin for AMOS Professional Basic source files.

## Implemented features

- `.asc` file type registration (AMOS text exports).
- Syntax highlighting for:
  - AMOS keywords/commands from the official manual command index.
  - numbers, strings, comments, operators, punctuation.
- Basic code completion with command/function names and type labels.
- Line commenting support using `'`.

## Command database source

Command metadata is generated from:

- `AmosProManual/14-appendix-g-command-index.html`

During build, Gradle generates `amos/commands.tsv` and bundles it into plugin resources.

## Run and test

```bash
./gradlew runIde
./gradlew test
./gradlew build
```

## Main plugin files

- `src/main/resources/META-INF/plugin.xml`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosLanguage.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosLexer.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosSyntaxHighlighter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosCompletionContributor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosParserDefinition.kt`
