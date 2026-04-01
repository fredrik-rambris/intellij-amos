# AGENTS Handoff - AMOS IntelliJ Plugin

## Purpose
- IntelliJ Platform plugin for AMOS Professional Basic source files.
- Primary workflow is text exports, not tokenized binaries.
- `.amos` tokenized binary bundles are still intentionally unsupported.

## Current File / Language Scope

### AMOS (`.asc` / `.amosasc` / `.amosbas`)
- Language id: `AMOS`.
- Registered file extensions: `.asc` (primary), `.amosasc`, `.amosbas`.
- File type registration lives in `src/main/resources/META-INF/plugin.xml`.
- Extra detection lives in `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosFileTypeOverrider.kt`.

### AMAL (`.amal`)
- Language id: `AMAL`.
- File extension: `.amal`.
- AMAL is the AMOS Animation Language — a compact sub-language embedded in AMOS strings or memory banks.
- The AMAL language is **not** dynamically extensible; its ~50 instructions/functions are defined directly in Kotlin code.
- Key package: `src/main/kotlin/dev/rambris/amigaamos/lang/amal/`.

### AMOS Interface (`.amui`)
- Language id: `AMUI`.
- File extension: `.amui`.
- Scaffolding only — no parser or highlighter yet.
- Key package: `src/main/kotlin/dev/rambris/amigaamos/lang/amui/`.

## Current Plugin Surface
The plugin currently wires the following in `plugin.xml`:

**AMOS:** file type + detector, syntax highlighter, color settings page, project settings pages, parser definition, annotator (procedure names), formatter + post-format processor, line indent provider, folding builder, completion contributor + confidence, goto declaration / rename / find usages, parameter info, documentation provider, typed handler, commenter, enter handler delegate, inspection.

**AMAL:** file type, syntax highlighter, color settings page, documentation provider.

**AMUI:** file type only.

## What Is Implemented

### AMOS
- Lexer/parser for AMOS statements and blocks.
- Syntax highlighting for keywords, commands, variables, labels, strings, numbers, operators, comments, commas, parentheses, bad chars.
- Color settings page under `Settings > Editor > Color Scheme > AMOS`.
- Completion with high-confidence autopopup, typed filtering, explicit keyword completion (e.g. `To`), smart insertion.
- Parameter info (`Ctrl-P`) from JSON definitions/signatures.
- Quick documentation (`Ctrl-Q`) with manual deep links stored directly in definition JSON.
- Goto declaration, find usages, and rename for procedures, labels, and variables.
- Folding for control blocks, procedure blocks, and multi-line string chains.
- Formatter / reformat / auto-indent / enter behavior.
- String-chain support for patterns like `A$=A$+"..."` including folding and continuation helpers.
- Definitions management UI for bundled, global, and project-local definition sources.

### AMAL
- Lexer with canonical-case significance model.
- Syntax highlighting with dedicated color settings page under `Settings > Editor > Color Scheme > AMAL`.
- Quick documentation (`Ctrl-Q`) from a built-in Kotlin catalog covering all instructions/functions with manual deep links.

#### Token types
| Token | Meaning |
|---|---|
| `KEYWORD` | Significant uppercase letters (`P` in `Pause`, `YM` in `YMouse`) |
| `KEYWORD_CONTINUATION` | Matching canonical lowercase continuation (`ause`, `ouse`) |
| `IGNORED_TEXT` | Non-matching lowercase outside strings — comment-colored |
| `REGISTER` | AMAL registers (`R0`–`R9`, `RA`–`RZ`, `X`, `Y`, `A`) |
| `LABEL` | Single uppercase letter + colon label definitions |
| `IDENTIFIER` | First letter of label references and jump targets |
| `STRING`, `NUMBER`, `OPERATOR`, `SEPARATOR`, `PAREN`, `BAD_CHARACTER` | As described |

#### Compact form splitting (examples)
- `PJLinguini` → `P` (Pause keyword), `J` (Jump keyword), `L` (label identifier), `inguini` (ignored text).
- `JS` / `JL` → `J` (keyword) + `S`/`L` (label identifier), even in expression context after `If`.

#### Canonical significance rules (manual spellings only)
- `Move` → `M` keyword + `ove` continuation.
- `eXit` → `e` ignored + `X` keyword + `it` continuation.
- `AUtotest` → `AU` keyword + `totest` continuation.
- `PLay` → `PL` keyword + `ay` continuation.
- `YMouse` → `YM` keyword + `ouse` continuation.
- Non-canonical suffixes (e.g. `YMonkey`) → `YM` keyword + `onkey` ignored.

#### Key AMAL semantic rules
- Statements are separated by **semicolons (`;`)**, **not colons**.
- Colons are used **exclusively** for label definitions (`L:`, `S:`, `Target:`).
- Only the **first capital letter** of a label name is significant — `Loop:` and `L:` define the same label.
- Using `:` as a statement separator instead of `;` generates a "duplicate label" runtime error in AMAL.

### AMAL Key Files
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalVocabulary.kt` — instruction/function/register sets, canonical matching helpers.
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalLexer.kt` — lexer with compact form and significance splitting.
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalTokenType.kt` — AMAL-specific token types.
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalSyntaxHighlighter.kt` — token-to-color mapping.
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalColorSettingsPage.kt` — color settings page.
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalDocumentationCatalog.kt` — in-code doc catalog with aliases and links.
- `src/main/kotlin/dev/rambris/amigaamos/lang/amal/AmalDocumentationProvider.kt` — quickdoc, caret-position aware.

## Definition System (Important — AMOS only)
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
- AMAL has **no** JSON-based definition registry; its vocabulary is hardcoded in Kotlin.

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
Location: `Settings > Languages & Frameworks > AMOS > Definitions`

Behavior:
- Two separate lists: global definition sources and project definition sources.
- Bundled definitions appear in the global section.
- Core is always enabled and cannot be removed.
- Bundled extension definitions can be disabled but not removed.
- External definition files can be added via path/URI and removed from their respective list.
- Global and project sources are persisted separately.
- Rows are sorted: core first, then slot, then extension/display name.
- Info column shows extension metadata (id, slot, filename, vendor, version, parse error).
- Apply is blocked if multiple enabled extensions claim the same slot.

## Coverage State (High Level)

### AMOS definitions
- Core/manual progression through chapter `13-01`.
- Selected appendices: `A`, `B`, `C`, `F`.
- `music.json`: chapters 08-01, 08-02, 08-03 (Music extension).
- `ioports.json`: printer / serial / parallel port commands (chapters 10-03–10-05).
- `request.json`: Request extension commands.
- `compact.json`: Picture compactor extension commands.

### Important caveats
- AMAL language is now fully lexed/highlighted/documented in standalone `.amal` files.
- AMAL **embedded in AMOS strings** is not lexed — stored as plain strings in AMOS source.
- Chapter 9 interface/dialog/resource AMOS-facing commands are defined, but the interface sub-language itself is **not** parsed.

## Formatting / Reformat Behavior (AMOS)
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
- Formats the line being left; computes indent for new line.
- De-indents closers (`Next`, `Wend`, `Loop`, `End Proc`, etc.).
- Auto-inserts matching closing lines for supported blocks:
  - `For X=1 To 100` → inserts indented body line and `Next X`
  - `Procedure TEST` → inserts indented body line and `End Proc`
  - `Repeat` → inserts indented body line and `Until `

## Completion Behavior Notes (AMOS)
- Autopopup is intentionally conservative.
- No autopopup in comments, strings, literals, operators, punctuation, or whitespace.
- At the start of a statement, popup is allowed only after enough confidence (2+ chars).
- In typed value contexts, popup is narrower and type-aware.
- When only a required keyword is valid, broad popup is suppressed.
- Typing space where the next required keyword is `To` auto-expands to ` To `.
- Selecting instruction/procedure completions inserts formatted casing plus trailing space.
- Selecting function completions inserts the formatted name and parentheses with caret inside.

Key files:
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionContributor.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosCompletionConfidence.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosTypedHandler.kt`

## String-Chain Support
Implemented in `src/main/kotlin/dev/rambris/amigaamos/lang/amos/AmosStringChainSupport.kt`.

```amos
Q$="First"
Q$=Q$+"Second"
Q$=Q$+"Third"
```

- Blank lines and `Rem`/`'` comments between parts are tolerated.
- Chain folding is implemented.
- Tab at chain end expands a new `Q$=Q$+""` snippet.

## Highlighting / Name Resolution Notes (AMOS)
- Procedure names are uppercased and highlighted via `AmosProcedureNameAnnotator.kt`.
- Labels are only recognized when they start a statement/line.
- Procedure / label / variable references are indexed from source analysis.

## Inspection
- `AmosSetBufferPositionInspection.kt`
- Registered as group `AMOS` in `plugin.xml`.
- Current purpose: `Set Buffer should be first instruction`.

## Build / Packaging
```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon test
./gradlew --no-daemon runIde
./gradlew --no-daemon build
./gradlew --no-daemon buildPlugin
```

Distributable plugin ZIP: `build/distributions/amiga-amos-1.1-SNAPSHOT.zip`

## Known RunIde Pitfall
Stale sandbox / cached plugin metadata after extension-point changes causes:
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

`build.gradle.kts` also contains: `-Dide.experimental.ui=false`

## Verification / Regression Tests

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

## Current User Preferences / Constraints
- Ask instead of assuming when AMOS or AMAL semantics are uncertain.
- Keep AMOS extension definitions file-driven and splittable by extension.
- AMAL has no JSON registry; vocabulary is hardcoded in Kotlin.
- Prefer runtime-loadable JSON definitions over hardcoded command tables (AMOS only).
- Reformat code to the current project style when inserting/editing code.
- Prefer Java 21 style and `var` when Java is touched.
- Prefer records for dataclass-like Java structures.
- Prefer lower case SQL.

## Suggested Next Work
1. AMAL completion: inserting full canonical long form (`Jump`, `Pause`, `YMouse`) from suggestion box.
2. Add diagnostics for recognized-but-disabled AMOS extension commands.
3. Continue AMOS formatter regression coverage (single-line `if ... then`, colon-heavy lines, mixed label/separator cases).
4. AMUI (AMOS Interface Language) lexer/parser/highlighter.
5. Continue validating AMOS JSON definition links/signatures against the manual chapter by chapter.
6. Consider embedded AMAL language support for AMOS string-chain patterns that contain AMAL programs.
