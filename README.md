# AMOS IntelliJ Plugin

Language support for AMOS Professional Basic and its sub-languages on the IntelliJ Platform.

## Supported languages

| Language | Extension(s) | Status |
|---|---|---|
| AMOS Professional Basic | `.asc`, `.amosasc`, `.amosbas` | Full support |
| AMAL (AMOS Animation Language) | `.amal` | Lexer, highlighting, quickdoc |
| AMOS Interface Language | `.amui` | File type only (scaffolding) |

Tokenized `.amos` binary program files are intentionally **not** supported.

## Features

### AMOS Professional Basic

#### Editing and language support
- Lexer/parser for AMOS statements and block structure.
- Syntax highlighting for keywords, commands, variables, labels, procedure names, strings, numbers, comments, operators, commas, parentheses, and bad characters.
- Folding for control blocks, procedure blocks, and multi-line string chains.
- Comment support for `Rem` and `'` style comments.

#### Completion and documentation
- Completion with conservative autopopup behavior.
- Type-aware filtering in many value contexts.
- Explicit keyword completion for signatures that require keywords such as `To`.
- Smart insertion:
  - instruction/procedure completions insert formatted casing plus trailing space
  - function completions insert formatted casing and parentheses when required, caret placed inside
- Parameter info (`Ctrl-P`) from JSON signatures.
- Quick documentation (`Ctrl-Q`) with deep links into the AMOS manual.

#### Navigation and refactoring
- Goto declaration for procedures, labels, and variables.
- Find usages for procedures, labels, and variables.
- Rename support for procedures, labels, and variables.

#### Formatting and enter behavior
- Reformat file / reformat code / auto-indent support.
- Enter handling that formats the line being left, indents the next line, de-indents closing lines, and auto-inserts matching block terminators.
- Example: typing `For X=1 To 10` and pressing Enter inserts indented body and `Next X`.

#### Other
- Color settings page under `Settings > Editor > Color Scheme > AMOS`.
- Definitions management UI under `Settings > Languages & Frameworks > AMOS > Definitions`.
- Inspection: `Set Buffer should be first instruction`.

### AMAL (AMOS Animation Language)

AMAL is a compact animation/movement sub-language. This plugin supports standalone `.amal` files and provides:

- **Syntax highlighting** following the AMAL canonical significance model:
  - Significant uppercase letters use keyword color (e.g. `P` in `Pause`, `YM` in `YMouse`).
  - Matching canonical lowercase continuation uses a dimmer keyword color (e.g. `ause`, `ouse`).
  - Non-matching lowercase outside strings uses comment color.
  - Registers (`R0`–`R9`, `RA`–`RZ`, `X`, `Y`, `A`) use variable color.
  - Label definitions use label color.
- **Quick documentation** (`Ctrl-Q`) for all AMAL instructions and functions with links to the manual.
- **Color settings page** under `Settings > Editor > Color Scheme > AMAL`.

#### AMAL compact forms
AMAL programs frequently omit whitespace and use abbreviated spellings:
- `PJLinguini` is tokenized as `P` (Pause), `J` (Jump), `L` (label), `inguini` (ignored suffix).
- `JS` and `JL` tokenize as `J` (Jump keyword) + label identifier, even in expression context.

#### Key AMAL semantic rules
- Statements are separated by **semicolons (`;`)**, never colons.
- Colons are used **exclusively** for label definitions (`L:`, `S:`, `Target:`).
- Only the **first capital letter** of a label is significant — `Loop:` and `L:` are the same label.
- Using `:` as a statement separator instead of `;` causes a "duplicate label" runtime error in AMAL.

## Definition system (AMOS)

AMOS callable knowledge is JSON-driven and loaded at runtime. Definitions cover:

- Core AMOS Professional (chapters 1–13, selected appendices A/B/C/F)
- Music extension (`AMOSPro_Music.Lib`, slot 1) — `music.json`
- Picture compactor extension (`AMOSPro_Compact.Lib`, slot 2) — `compact.json`
- IOPorts extension (`AMOSPro_IOPorts.Lib`, slot 6) — `ioports.json`
- Request extension — `request.json`

AMAL has **no** JSON definition registry — its vocabulary is defined directly in Kotlin code.

### Definitions settings UI

Location: `Settings > Languages & Frameworks > AMOS > Definitions`

- Separate global and project source lists.
- External definition files addable via path or URI.
- Bundled extensions can be disabled but not removed.
- Core definitions are always enabled and cannot be removed.
- Apply blocked when multiple enabled extensions claim the same slot.
- Rows sorted: core first, then by slot, then by name.
- Info column shows extension metadata (id, slot, filename, vendor, version, parse errors).

## Formatting behavior (AMOS)

- Variables uppercased.
- Procedure names uppercased.
- Instructions, functions, and structure keywords in AMOS-style Camel Case from definitions.
- Logical operators `and` / `or` forced to lowercase.
- Block indent width: 3 spaces.
- Statement separator colons spaced; label colons left unspaced.
- Trailing whitespace trimmed; trailing blank lines at EOF removed.

## String-chain support

The plugin recognizes grouped string-building patterns:

```amos
Q$="First"
Q$=Q$+"Second"
Q$=Q$+"Third"
```

- Blank lines and `Rem`/`'` comments between parts are tolerated.
- Chain folding is implemented.
- Tab at chain end expands a new `Q$=Q$+""` snippet.

## Build and development

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon test
./gradlew --no-daemon runIde
./gradlew --no-daemon build
./gradlew --no-daemon buildPlugin
```

## Packaging and installation

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon buildPlugin
```

Generated archive: `build/distributions/amiga-amos-1.1-SNAPSHOT.zip`

Install via `Settings` → `Plugins` → gear icon → `Install Plugin from Disk...`

## Known runIde pitfall

After extension-point or plugin XML changes, stale sandbox / cached metadata can cause:
- `implementation class is not specified [Plugin: dev.rambris.amiga-amos]`
- `IntentionActionWrapper.getImplementationClassName must not return null`

Fix:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --stop
rm -rf build/idea-sandbox
rm -rf .gradle/configuration-cache
./gradlew --no-daemon clean runIde --rerun-tasks --console=plain
```

`build.gradle.kts` also contains `-Dide.experimental.ui=false` as a 2025.2 sandbox workaround.

## Key source files

### AMOS
- `src/main/resources/META-INF/plugin.xml`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosLexer.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosParser.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosSyntaxHighlighter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionContributor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDocumentationProvider.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCodeStyleFormatter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionProjectConfigurable.kt`

### AMAL
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalLexer.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalVocabulary.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalSyntaxHighlighter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalColorSettingsPage.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalDocumentationCatalog.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalDocumentationProvider.kt`

## Tests

### AMOS tests
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosLexerTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosParserStructureTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionContributorTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosDocumentationProviderTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosReferenceContributorTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosRenameSupportTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosFoldingBuilderTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosFormattingTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosEnterHandlerDelegateTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosStringChainSupportTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionRegistryTest.kt`

### AMAL tests
- `src/test/kotlin/dev/rambris/amigaamos/lang/amal/AmalLexerTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amal/AmalDocumentationProviderTest.kt`
- `src/test/kotlin/dev/rambris/amigaamos/lang/amal/AmalColorSettingsPageTest.kt`
