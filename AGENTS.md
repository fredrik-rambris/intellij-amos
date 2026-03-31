# AGENTS Handoff - AMOS IntelliJ Plugin

## Purpose
- IntelliJ Platform plugin for AMOS Professional Basic source files.
- Primary workflow is text exports, not tokenized binaries.
- `.amos` tokenized binary bundles are still intentionally unsupported.

## Current File / Language Scope
- Language id: `AMOS`.
- Registered file extensions:
  - `.asc` (primary / canonical workflow)
  - `.amosasc`
  - `.amosbas`
- File type registration lives in `src/main/resources/META-INF/plugin.xml`.
- Extra detection lives in `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosFileTypeOverrider.kt`.

## Current Plugin Surface
The plugin currently wires the following in `plugin.xml`:
- file type + detector
- syntax highlighter
- color settings page
- project settings pages
- parser definition
- annotator for procedure-name highlighting
- formatter + post-format processor
- line indent provider
- folding builder
- completion contributor + completion confidence
- goto declaration / rename / find usages
- parameter info
- documentation provider
- typed handler
- commenter
- enter handler delegate
- `Set Buffer should be first instruction` inspection

## What Is Implemented
- Lexer/parser for AMOS statements and blocks.
- Syntax highlighting for keywords, commands, variables, labels, strings, numbers, operators, comments, commas, parentheses, bad chars.
- Color settings page under `Settings > Editor > Color Scheme > AMOS`.
- Completion with:
  - high-confidence autopopup only
  - typed filtering for many value contexts
  - explicit keyword completion where a signature requires keywords such as `To`
  - smart insertion for instructions/functions
- Parameter info (`Ctrl-P`) from JSON definitions/signatures.
- Quick documentation (`Ctrl-Q`) with manual deep links stored directly in definition JSON.
- Goto declaration, find usages, and rename for procedures, labels, and variables.
- Folding for control blocks, procedure blocks, and multi-line string chains.
- Formatter / reformat / auto-indent / enter behavior.
- String-chain support for patterns like `A$=A$+"..."` including folding and continuation helpers.
- Definitions management UI for bundled, global, and project-local definition sources.

## Definition System (Important)
Definitions are JSON-driven and loaded at runtime by the registry.

### Key Files
- Registry: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionRegistry.kt`
- Models: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionModels.kt`
- Source reference parsing: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionSourceReferences.kt`
- App-level settings: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionApplicationSettings.kt`
- Project-level settings: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionProjectSettings.kt`
- Project settings UI: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionProjectConfigurable.kt`
- Extension point bean: `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosDefinitionResourceBean.kt`

### Important Architectural Notes
- The AMOS manual is **not** required to build the plugin.
- `commands.tsv` is no longer used as the runtime/build-time source of command vocabulary.
- Lexer keyword vocabulary is derived from JSON definitions plus a very small hardcoded short-keyword set in `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCommandIndex.kt`.
- Definition files are merged by `(uppercaseName, kind)`.
- Structures are still code-owned; extension JSON is for functions/instructions/metadata.

### Bundled Definition Files
- `src/main/resources/amos/definitions/core.json`
- `src/main/resources/amos/definitions/compact.json`
- `src/main/resources/amos/definitions/music.json`
- `src/main/resources/amos/definitions/ioports.json`
- `src/main/resources/amos/definitions/request.json`

### Extension Metadata Contract (JSON)
```json
{
  "extension": {
    "id": "Compact",
    "name": "Picture compactor extension",
    "filename": "AMOSPro_Compact.Lib",
    "slot": 2,
    "vendor": "François Lionet"
  },
  "definitions": []
}
```

## Definition Settings UI
Location:
- `Settings > Languages & Frameworks > AMOS > Definitions`

Behavior:
- Two separate lists:
  - global definition sources
  - project definition sources
- Bundled definitions appear in the global section.
- Core is always enabled and cannot be removed.
- Bundled extension definitions can be disabled but not removed.
- External definition files can be added via path/URI and removed from their respective list.
- Global and project sources are persisted separately.
- Rows are sorted by:
  1. core first
  2. slot number
  3. extension/display name
- There is an info column that shows extension metadata (id, slot, filename, vendor, version, parse error).
- Apply is blocked if multiple enabled extensions claim the same slot.

## Coverage State (High Level)
Definitions currently cover:
- Core/manual progression through chapter `13-01`.
- Selected appendices: `A`, `B`, `C`, `F`.
- Chapter 8 music commands are split by ownership:
  - `08-01`, `08-02`, `08-03` music-related extension material lives in `music.json`.
- Chapter 10 / 11 extension ownership split:
  - `ioports.json`: printer / serial / parallel port commands.
  - `request.json`: Request extension commands.
- Compact extension commands live in `compact.json`.

Important caveats:
- The AMAL **control commands/functions** are defined, but the AMAL sub-language itself is **not** lexed/parsed as AMAL source.
- Chapter 9 interface/dialog/resource AMOS-facing commands are defined, but the interface sub-language itself is **not** parsed.

## Formatting / Reformat Behavior
User-expected formatting currently targets:
- Variables uppercased.
- Procedure names uppercased.
- Instructions / functions / structure keywords in AMOS-style Camel Case from definitions.
- Logical operators `and` / `or` forced to lowercase.
- Block indent width: 3 spaces.
- Statement separator colons get spaces around them when used as separators.
- Label colons do **not** get statement-separator spacing.
- Trailing whitespace is trimmed.
- Extra blank lines at EOF are removed.

Primary formatter files:
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosFormattingModelBuilder.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosPostFormatProcessor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosLineIndentProvider.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosEnterHandlerDelegate.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCodeStyleFormatter.kt`

### Enter Handling
Pressing Enter currently:
- formats the line being left
- computes indent for the new line
- de-indents closers like `Next`, `Wend`, `Loop`, `End Proc`, etc.
- auto-inserts matching closing lines for supported blocks

Examples:
- `For X=1 To 100` → inserts indented body line and `Next X`
- `Procedure TEST` → inserts indented body line and `End Proc`
- `Repeat` → inserts indented body line and `Until `

## Completion Behavior Notes
- Autopopup is intentionally conservative.
- No autopopup in comments, strings, literals, operators, punctuation, or whitespace.
- At the start of a statement, popup is allowed only after enough confidence (for example 2+ chars).
- In typed value contexts, popup is narrower and type-aware.
- When only a required keyword is valid, broad popup is suppressed.
- Typing space where the next required keyword is `To` auto-expands to ` To `.
- Selecting instruction/procedure completions inserts formatted casing plus trailing space.
- Selecting function completions inserts the formatted name and, when required, parentheses with the caret placed inside.

Key files:
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionContributor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionConfidence.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosTypedHandler.kt`

## String-Chain Support
Implemented in `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosStringChainSupport.kt`.

Current supported pattern:
```amos
Q$="First"
Q$=Q$+"Second"
Q$=Q$+"Third"
```

Notes:
- Blank lines between chain parts are tolerated.
- `Rem` and `'` comment lines between parts are tolerated.
- Chain folding is implemented.
- Tab on an empty continuation line can expand a new `Q$=Q$+""` snippet when the caret is at the end of a detected chain context.

## Highlighting / Name Resolution Notes
- Procedure names are uppercased and highlighted via `AmosProcedureNameAnnotator.kt`.
- Labels are only recognized when they start a statement/line (after optional leading whitespace).
- Procedure / label / variable references are indexed from source analysis rather than a full PSI grammar for every symbol kind.

## Inspection
- `AmosSetBufferPositionInspection.kt`
- Registered as group `AMOS` in `plugin.xml`.
- Current purpose: `Set Buffer should be first instruction`.

## Build / Packaging
Useful commands:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon test
./gradlew --no-daemon runIde
./gradlew --no-daemon build
./gradlew --no-daemon buildPlugin
```

Distributable plugin ZIP:
- `build/distributions/amiga-amos-1.0-SNAPSHOT.zip`

## Known RunIde Pitfall
A recurring development pitfall has been stale sandbox / cached plugin metadata after extension-point changes.

Typical symptoms:
- `implementation class is not specified [Plugin: dev.rambris.amiga-amos]`
- `IntentionActionWrapper.getImplementationClassName must not return null`

If this reappears, clean sandbox + configuration cache and rerun with tasks forced:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --stop
rm -rf build/idea-sandbox
rm -rf .gradle/configuration-cache
./gradlew --no-daemon clean runIde --rerun-tasks --console=plain
```

There is also an explicit `runIde` JVM workaround in `build.gradle.kts`:
- `-Dide.experimental.ui=false`

## Verification / Regression Tests
Relevant test files:
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

Quick verification:

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon test
./gradlew --no-daemon runIde
```

## Current User Preferences / Constraints
- Ask instead of assuming when AMOS semantics are uncertain.
- Keep extension definitions file-driven and splittable by extension.
- Prefer runtime-loadable JSON definitions over hardcoded command tables.
- Reformat code to the current project style when inserting/editing code.
- Prefer Java 21 style and `var` when Java is touched.
- Prefer records for dataclass-like Java structures.
- Prefer lower case SQL.

## Suggested Next Work
1. Extend typed-parameter filtering coverage across more signatures and edge cases.
2. Add diagnostics for recognized-but-disabled extension commands if not already present / complete.
3. Continue formatter regression coverage for single-line `if ... then`, colon-heavy lines, and mixed label/separator cases.
4. Consider future support for parsing AMAL and interface sub-languages as embedded languages.
5. Continue validating JSON definition links/signatures against the manual chapter by chapter.

