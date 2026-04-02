# ![AMOS Logo](src/main/resources/META-INF/pluginIcon.svg) AMOS IntelliJ Plugin

Language support for AMOS Professional Basic and related languages on the IntelliJ Platform.

## Supported languages

| Language | Extension(s) | Status |
|---|---|---|
| AMOS Professional Basic | `.asc`, `.amosasc`, `.amosbas` | Full support |
| AMAL (AMOS Animation Language) | `.amal` | Lexer, highlighting, quickdoc |
| AMOS Interface Language | `.amui` | File type only (scaffolding) |

Tokenized `.amos` binary files are intentionally **not** supported.

## Highlights

- AMOS lexer/parser, highlighting, completion, quickdoc, parameter info, navigation, rename, formatting, folding.
- AMAL lexer/highlighting/quickdoc for standalone `.amal` files.
- JSON-driven AMOS definitions with global + project sources.
- Definitions settings at `Settings > Languages & Frameworks > AMOS > Definitions`.

## Definitions UI (AMOS)

- Two lists: global and project sources.
- Core definitions are always enabled and not removable.
- Bundled definitions can be disabled but not removed/edited.
- External definitions use file chooser add/edit actions.
- Project definitions are stored relative to project root when possible.
- Slot conflicts are validated on Apply.

## Documentation map

- Build, test, run, package: [`BUILDING.md`](BUILDING.md)
- Engineering handoff and architecture notes: [`AGENTS.md`](AGENTS.md)

## Install from ZIP

Either download a ZIP file from [Releases](https://github.com/fredrik-rambris/intellij-amos/releases)

or

Build plugin ZIP (see [`BUILDING.md`](BUILDING.md)), then install in IDE via:
`Settings` -> `Plugins` -> gear icon -> `Install Plugin from Disk...`
