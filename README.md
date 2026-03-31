# AMOS IntelliJ Plugin

Language support for AMOS Professional Basic text source files on the IntelliJ Platform.

## Scope

- Primary workflow: AMOS text exports.
- Supported file extensions:
  - `.asc`
  - `.amosasc`
  - `.amosbas`
- Tokenized `.amos` binary program files are intentionally **not** supported yet.

## Current capabilities

### Editing and language support

- Lexer/parser for AMOS statements and block structure.
- Syntax highlighting for:
  - keywords and commands
  - variables, labels, and procedure names
  - strings and numbers
  - comments, operators, commas, parentheses, and bad characters
- Folding for control blocks, procedure blocks, and multi-line string chains.
- Comment support for `Rem` and `'` style comments.

### Completion and documentation

- Completion with conservative autopopup behavior.
- Type-aware filtering in many value contexts.
- Explicit keyword completion for signatures that require keywords such as `To`.
- Smart insertion behavior:
  - instruction/procedure completions insert formatted casing plus trailing space
  - function completions insert formatted casing and parentheses when required
- Parameter info (`Ctrl-P`) from JSON signatures.
- Quick documentation (`Ctrl-Q`) with deep links into the AMOS manual.

### Navigation and refactoring

- Goto declaration for procedures, labels, and variables.
- Find usages for procedures, labels, and variables.
- Rename support for procedures, labels, and variables.

### Formatting and enter behavior

- Reformat file / reformat code / auto-indent support.
- Line indentation support for AMOS block constructs.
- Enter handling that formats the line being left, indents the next line, de-indents closing lines, and auto-inserts matching block terminators for supported blocks.

### Other implemented features

- Color settings page under `Settings > Editor > Color Scheme > AMOS`.
- Definitions management UI under `Settings > Languages & Frameworks > AMOS > Definitions`.
- Inspection: `Set Buffer should be first instruction`.

## Definition system

AMOS callable knowledge is JSON-driven and loaded at runtime.

Key files:

- Registry: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionRegistry.kt`
- Models: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionModels.kt`
- Source reference parsing: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionSourceReferences.kt`
- App-level settings: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionApplicationSettings.kt`
- Project-level settings: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionProjectSettings.kt`
- Settings UI: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionProjectConfigurable.kt`

Important notes:

- The AMOS Professional manual is **not** required to build the plugin.
- Definitions are merged by `(uppercaseName, kind)`.
- Structures such as block statements are still code-owned.
- Extension JSON is intended for functions, instructions, signatures, metadata, and documentation links.

### Bundled definition files

- `src/main/resources/amos/definitions/core.json`
- `src/main/resources/amos/definitions/compact.json`
- `src/main/resources/amos/definitions/music.json`
- `src/main/resources/amos/definitions/ioports.json`
- `src/main/resources/amos/definitions/request.json`

### Bundled extension ownership

- `core.json`: core AMOS Professional commands/functions.
- `compact.json`: Picture compactor extension.
- `music.json`: Music extension definitions.
- `ioports.json`: printer / serial / parallel port extension definitions.
- `request.json`: Request extension definitions.

### Definitions settings UI behavior

- Separate global and project definition source lists.
- External definition files can be added via path or URI.
- External definition files can be removed from the list they were added to.
- Bundled extension definitions can be disabled but not removed.
- Core definitions are always enabled and cannot be removed.
- Apply is blocked when multiple enabled extensions claim the same slot.
- Rows are sorted with core first, then by slot, then by name.
- An info column shows metadata such as extension id, slot, filename, vendor, version, and parse errors.

## Current coverage and caveats

### Covered manual areas

- Core/manual progression through chapter `13-01`.
- Selected appendices:
  - `A`
  - `B`
  - `C`
  - `F`

### Important caveats

- The AMAL **control commands/functions** are defined, but the AMAL sub-language itself is not lexed or parsed as AMAL source.
- Chapter 9 interface/dialog/resource AMOS-facing commands are defined, but the interface sub-language itself is not parsed.
- Tokenized `.amos` binary files remain unsupported.

## Formatting behavior

Current formatting targets include:

- variables uppercased
- procedure names uppercased
- instructions, functions, and structure keywords in AMOS-style Camel Case from definitions
- logical operators `and` / `or` forced to lowercase
- block indent width: 3 spaces
- statement separator colons spaced as separators
- label colons left unspaced
- trailing whitespace trimmed
- trailing blank lines at EOF removed

Primary formatter files:

- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosFormattingModelBuilder.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosPostFormatProcessor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosLineIndentProvider.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosEnterHandlerDelegate.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCodeStyleFormatter.kt`

## Completion behavior notes

- Autopopup is intentionally conservative.
- No autopopup in comments, strings, literals, operators, punctuation, or whitespace.
- At statement start, popup appears only after enough confidence.
- In typed value contexts, popup is narrower and type-aware.
- If the only valid next token is a required keyword such as `To`, broad popup is suppressed.
- Typing space where the required keyword is `To` auto-expands to ` To `.

Key files:

- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionContributor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionConfidence.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosTypedHandler.kt`

## String-chain support

The plugin recognizes grouped string-building patterns such as:

```amos
Q$="First"
Q$=Q$+"Second"
Q$=Q$+"Third"
```

Current behavior:

- blank lines between parts are tolerated
- `Rem` and `'` comment lines between parts are tolerated
- chain folding is implemented
- tab on an empty continuation line can expand a new `Q$=Q$+""` snippet at the end of a detected chain

Implementation file:

- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosStringChainSupport.kt`

## Build and development

Useful commands:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon test
./gradlew --no-daemon runIde
./gradlew --no-daemon build
./gradlew --no-daemon buildPlugin
```

Useful verification commands:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon verifyPlugin
./gradlew --no-daemon verifyPluginStructure
```

## Packaging and installation

Build the installable plugin ZIP with:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon buildPlugin
```

Generated archive:

- `build/distributions/amiga-amos-1.0-SNAPSHOT.zip`

Install it from IntelliJ IDEA via:

- `Settings` → `Plugins` → gear icon → `Install Plugin from Disk...`

## Known runIde pitfall

After extension-point or plugin XML changes, stale sandbox / cached metadata can cause startup issues such as:

- `implementation class is not specified [Plugin: dev.rambris.amiga-amos]`
- `IntentionActionWrapper.getImplementationClassName must not return null`

If that happens, clean the sandbox and configuration cache, then rerun:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --stop
rm -rf build/idea-sandbox
rm -rf .gradle/configuration-cache
./gradlew --no-daemon clean runIde --rerun-tasks --console=plain
```

`build.gradle.kts` also contains an explicit workaround for 2025.2 sandbox startup issues:

- `-Dide.experimental.ui=false`

## Useful source files

- `src/main/resources/META-INF/plugin.xml`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosLexer.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosParser.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosSyntaxHighlighter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionContributor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDocumentationProvider.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCodeStyleFormatter.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionProjectConfigurable.kt`

## Relevant tests

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

