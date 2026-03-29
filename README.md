# AMOS Plugin for IntelliJ

Language support for AMOS Professional Basic text source files.

## File support

- Primary workflow: AMOS text exports (`.asc`).
- Also registered: `.amosasc`, `.amosbas`.

## Implemented capabilities

- Lexer/parser for AMOS statements.
- Syntax highlighting for keywords, instructions/functions, comments, labels, variables, strings, numbers, operators, and punctuation.
- Completion with context-aware filtering and typed parameter hints.
- Parameter info (`Ctrl-P`) based on JSON definition signatures.
- Quick documentation (`Ctrl-Q`) with links back to manual sections.
- Goto declaration, find usages, and rename support for procedures/labels/variables.
- Folding for control/procedure blocks.
- Formatter, line indent provider, and enter behavior for AMOS block structure.
- Inspection: `Set Buffer should be first instruction`.

## Definitions system

Language definitions are JSON-driven and loaded at runtime.

- Registry: `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionRegistry.kt`
- Project settings service: `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionProjectSettings.kt`
- Global settings service: `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionApplicationSettings.kt`
- Project settings UI: `Settings > Languages & Frameworks > AMOS > Definitions`

Bundled definition files:

- `src/main/resources/amos/definitions/core.json`
- `src/main/resources/amos/definitions/compact.json`
- `src/main/resources/amos/definitions/music.json`
- `src/main/resources/amos/definitions/ioports.json`
- `src/main/resources/amos/definitions/request.json`

### Definitions UI behavior

- Separate global and project definition source lists.
- Add/remove external definition files with `+` / `-`.
- Bundled definitions can be disabled but not removed.
- Core definitions are always enabled.
- Apply is blocked when multiple enabled extensions share the same slot.

## Command index source

Keyword command index data is generated from:

- `AmosProManual/14-appendix-g-command-index.html`

During build, Gradle generates `amos/commands.tsv` and bundles it as plugin resources.

## Development commands

```bash
./gradlew --no-daemon test
./gradlew --no-daemon runIde
./gradlew --no-daemon build
```

## Useful source files

- `src/main/resources/META-INF/plugin.xml`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosLexer.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosParser.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosSyntaxHighlighter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosCompletionContributor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDocumentationProvider.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosCodeStyleFormatter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionProjectConfigurable.kt`
