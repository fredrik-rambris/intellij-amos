# AGENTS Handoff - AMOS IntelliJ Plugin

## Purpose
- IntelliJ Platform plugin for AMOS Professional Basic text exports (`.asc`).
- `.amos` binary tokenized files are intentionally not supported yet.

## Current File/Language Scope
- File type: `.asc` only.
- Language id: `AMOS`.
- Core wiring is in `src/main/resources/META-INF/plugin.xml`.

## What Is Implemented
- Lexer/parser + syntax highlighting for AMOS tokens, comments, variables, labels, numbers, strings, keywords, instructions/functions.
- Completion with type-aware filtering in many value contexts.
- Parameter info (`Ctrl-P`) for typed call signatures from definition JSON.
- Quick documentation (`Ctrl-Q`) with manual deep links.
- Goto declaration, find usages, rename support for procedures/labels/variables.
- Folding for control blocks and procedure blocks.
- String-chain support (`A$=A$+"..."`) including folding and related editing helpers.
- Formatter + line indent provider + enter handler behavior.

## Definition System (Important)
Definitions are JSON-driven and loaded at runtime by the registry.

### Registry and Model
- Registry: `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionRegistry.kt`
- Models: `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionModels.kt`
- Project settings for toggling extension ids/sources: `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionProjectSettings.kt`
- Extension point bean: `src/main/kotlin/dev/rambris/amigaamos/lang/AmosDefinitionResourceBean.kt`

### Bundled definition files
- `src/main/resources/amos/definitions/core.json`
- `src/main/resources/amos/definitions/compact.json`
- `src/main/resources/amos/definitions/music.json`
- `src/main/resources/amos/definitions/ioports.json`
- `src/main/resources/amos/definitions/request.json`

### Extension metadata contract (JSON)
```json
{
  "extension": {
    "id": "Compact",
    "name": "Picture compactor extension",
    "filename": "AMOSPro_Compact.Lib",
    "slot": 2,
    "vendor": "Francois Lionet"
  },
  "definitions": []
}
```

## Chapter Coverage State (high level)
Definitions are present for:
- Core/manual progression through chapter 13-01 plus selected appendices (A, B, C, F).
- Chapter 10/11 split by extension ownership:
  - `ioports.json`: printer/serial/parallel port related commands.
  - `request.json`: Request extension commands moved out from core.
- Music-related commands are in `music.json`.
- Compact extension commands are in `compact.json`.

## Formatting Rules Implemented
User-expected AMOS formatting currently targets:
- Variables uppercased.
- Procedure names uppercased.
- Instructions/functions/keywords in AMOS-style Camel Case from definitions.
- Block indent width: 3 spaces.
- Space around statement separators `:` when used as separators.
- Reformat/auto-indent should trim trailing spaces and avoid adding extra blank lines at EOF.
- Enter handling:
  - formats the line being left,
  - computes indent for the newly created line,
  - de-indents when closing block statements are entered.

Primary files for formatting/indent:
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosFormattingModelBuilder.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosLineIndentProvider.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosEnterHandlerDelegate.kt`
- `src/main/kotlin/dev/rambris/amigaamos/lang/AmosCodeStyleFormatter.kt`

## Known RunIde Pitfall (Important)
A recurring error seen during development:
- `implementation class is not specified [Plugin: dev.rambris.amiga-amos]`
- `IntentionActionWrapper.getImplementationClassName must not return null`

This was caused by stale plugin sandbox/plugin.xml state during fast iteration.
If it reappears, clean sandbox and rerun with tasks rerun.

```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --stop
rm -rf build/idea-sandbox
rm -rf .gradle/configuration-cache
./gradlew --no-daemon clean runIde --rerun-tasks --console=plain
```

## Verify Quickly
```bash
cd /home/boost/Data/Coding/amiga-amos
./gradlew --no-daemon test
./gradlew --no-daemon runIde
```

## Current User Preferences / Constraints
- Ask instead of assuming when AMOS semantics are uncertain.
- Keep extension definitions file-driven and splittable by extension.
- No UI required yet for extension enable/disable; runtime support is the focus.
- Follow manual command index casing for now (future tokenization/casing pass may change behavior).

## Suggested Next Work
1. Continue completion-confidence tuning (show popup only in high-confidence contexts).
2. Extend typed-parameter filtering coverage for all definitions/signatures.
3. Add more formatter regression tests for edge cases (`if ... then` single-line, colons, EOF newlines).
4. Keep chapter-by-chapter import workflow and validate links against manual anchors.

