package dev.rambris.amigaamos.lang.amos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.nio.file.Files

class AmosDefinitionRegistryTest {
    @Test
    fun `core registry loads string functions with docs and types`() {
        val mid = AmosDefinitionRegistry.definitionFor("MID$", AmosDefinitionKind.FUNCTION)

        assertNotNull(mid)
        assertEquals(AmosValueType.STRING, mid.returnType)
        assertTrue(mid.documentation?.contains("middle of a string", ignoreCase = true) == true)
        assertEquals(2, mid.signatures.size)
        assertEquals(AmosValueType.STRING, mid.signatures.first().parameters.first().valueType)
        assertEquals(AmosValueType.INTEGER, mid.signatures.first().parameters[1].valueType)
        assertEquals("https://amospromanual.dev/05-02-string-functions.html#fn-mid-dollar", mid.link?.toString())

        val pi = AmosDefinitionRegistry.definitionFor("PI#", AmosDefinitionKind.FUNCTION)
        assertNotNull(pi)
        assertEquals(AmosValueType.REAL, pi.returnType)
        assertEquals("https://amospromanual.dev/05-03-maths.html#fn-pi-pound", pi.link?.toString())

        val randomize = AmosDefinitionRegistry.definitionFor("RANDOMIZE", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(randomize)
        assertEquals("https://amospromanual.dev/05-03-maths.html#i-randomize", randomize.link?.toString())

        val ifDefinition = AmosDefinitionRegistry.definitionFor("IF", AmosDefinitionKind.STRUCTURE)
        assertNotNull(ifDefinition)
        assertEquals("https://amospromanual.dev/05-04-control-structures.html#str-if", ifDefinition.link?.toString())

        val endIfDefinition = AmosDefinitionRegistry.definitionFor("END IF", AmosDefinitionKind.STRUCTURE)
        assertNotNull(endIfDefinition)
        assertEquals("https://amospromanual.dev/05-04-control-structures.html#str-end-if", endIfDefinition.link?.toString())

        val forDefinition = AmosDefinitionRegistry.definitionFor("FOR", AmosDefinitionKind.STRUCTURE)
        assertNotNull(forDefinition)
        assertEquals("https://amospromanual.dev/05-04-control-structures.html#str-for", forDefinition.link?.toString())

        val globalDefinition = AmosDefinitionRegistry.definitionFor("GLOBAL", AmosDefinitionKind.STRUCTURE)
        assertNotNull(globalDefinition)
        assertTrue(globalDefinition.documentation?.contains("outside procedures", ignoreCase = true) == true)
        assertTrue(globalDefinition.documentation?.contains("inside a procedure", ignoreCase = true) == true)
        assertEquals("https://amospromanual.dev/05-05-procedures.html#str-global", globalDefinition.link?.toString())

        val paramString = AmosDefinitionRegistry.definitionFor("PARAM$", AmosDefinitionKind.FUNCTION)
        assertNotNull(paramString)
        assertEquals(AmosValueType.STRING, paramString.returnType)
        assertEquals("https://amospromanual.dev/05-05-procedures.html#fn-param", paramString.link?.toString())

        val locate = AmosDefinitionRegistry.definitionFor("LOCATE", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(locate)
        assertEquals("https://amospromanual.dev/05-06-text.html#i-locate", locate.link?.toString())

        val border = AmosDefinitionRegistry.definitionFor("BORDER$", AmosDefinitionKind.FUNCTION)
        assertNotNull(border)
        assertEquals(AmosValueType.STRING, border.returnType)
        assertEquals("https://amospromanual.dev/05-06-text.html#fn-border-dollar", border.link?.toString())

        val windOpen = AmosDefinitionRegistry.definitionFor("WIND OPEN", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(windOpen)
        assertEquals("https://amospromanual.dev/05-07-windows.html#i-wind-open", windOpen.link?.toString())

        val joy = AmosDefinitionRegistry.definitionFor("JOY", AmosDefinitionKind.FUNCTION)
        assertNotNull(joy)
        assertEquals(AmosValueType.INTEGER, joy.returnType)
        assertEquals("https://amospromanual.dev/05-08-the-joystick-and-mouse.html#fn-joy", joy.link?.toString())

        val blength = AmosDefinitionRegistry.definitionFor("BLENGTH", AmosDefinitionKind.FUNCTION)
        assertNotNull(blength)
        assertEquals("https://amospromanual.dev/05-09-memory-banks.html#fn-blength", blength.link?.toString())

        val screenWidth = AmosDefinitionRegistry.definitionFor("SCREEN WIDTH", AmosDefinitionKind.FUNCTION)
        assertNotNull(screenWidth)
        assertEquals("https://amospromanual.dev/06-01-setting-up-screens.html#fn-screen-width", screenWidth.link?.toString())

        val screenCopy = AmosDefinitionRegistry.definitionFor("SCREEN COPY", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(screenCopy)
        assertEquals("https://amospromanual.dev/06-02-using-screens.html#i-screen-copy", screenCopy.link?.toString())

        val amal = AmosDefinitionRegistry.definitionFor("AMAL", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(amal)
        assertEquals("https://amospromanual.dev/07-06-amal.html#i-amal", amal.link?.toString())

        val synchroOff = AmosDefinitionRegistry.definitionFor("SYNCHRO OFF", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(synchroOff)
        assertEquals("https://amospromanual.dev/07-06-amal.html#i-synchro-off", synchroOff.link?.toString())

        val amreg = AmosDefinitionRegistry.definitionFor("AMREG", AmosDefinitionKind.FUNCTION)
        assertNotNull(amreg)
        assertEquals(AmosValueType.INTEGER, amreg.returnType)
        assertEquals("https://amospromanual.dev/07-06-amal.html#resv-am-reg", amreg.link?.toString())

        val chanan = AmosDefinitionRegistry.definitionFor("CHANAN", AmosDefinitionKind.FUNCTION)
        assertNotNull(chanan)
        assertEquals(AmosValueType.INTEGER, chanan.returnType)
        assertEquals("https://amospromanual.dev/07-06-amal.html#fn-chanan", chanan.link?.toString())

        val animFreeze = AmosDefinitionRegistry.definitionFor("ANIM FREEZE", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(animFreeze)
        assertEquals("https://amospromanual.dev/07-06-amal.html#i-anim-freeze", animFreeze.link?.toString())

        val getIcon = AmosDefinitionRegistry.definitionFor("GET ICON", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(getIcon)
        assertEquals("https://amospromanual.dev/07-07-icons-and-blocks.html#i-get-icon", getIcon.link?.toString())

        val dialogBox = AmosDefinitionRegistry.definitionFor("DIALOG BOX", AmosDefinitionKind.FUNCTION)
        assertNotNull(dialogBox)
        assertEquals(AmosValueType.INTEGER, dialogBox.returnType)
        assertEquals("https://amospromanual.dev/09-01-amos-interface.html#fn-dialog-box", dialogBox.link?.toString())

        val dialogOpen = AmosDefinitionRegistry.definitionFor("DIALOG OPEN", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(dialogOpen)
        assertEquals("https://amospromanual.dev/09-03-advanced-control-panels.html#i-dialog-open", dialogOpen.link?.toString())

        val rdialogString = AmosDefinitionRegistry.definitionFor("RDIALOG$", AmosDefinitionKind.FUNCTION)
        assertNotNull(rdialogString)
        assertEquals(AmosValueType.STRING, rdialogString.returnType)
        assertEquals("https://amospromanual.dev/09-03-advanced-control-panels.html#fn-rdialog-dollar", rdialogString.link?.toString())

        val resourceString = AmosDefinitionRegistry.definitionFor("RESOURCE$", AmosDefinitionKind.FUNCTION)
        assertNotNull(resourceString)
        assertEquals(AmosValueType.STRING, resourceString.returnType)
        assertEquals("https://amospromanual.dev/09-04-interface-resources.html#fn-resource-dollar", resourceString.link?.toString())

        val resourceUnpack = AmosDefinitionRegistry.definitionFor("RESOURCE UNPACK", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(resourceUnpack)
        assertEquals("https://amospromanual.dev/09-04-interface-resources.html#i-resource-unpack", resourceUnpack.link?.toString())

        val inkey = AmosDefinitionRegistry.definitionFor("INKEY$", AmosDefinitionKind.FUNCTION)
        assertNotNull(inkey)
        assertEquals(AmosValueType.STRING, inkey.returnType)
        assertEquals("https://amospromanual.dev/10-01-using-the-keyboard.html#fn-inkey-dollar", inkey.link?.toString())

        val dir = AmosDefinitionRegistry.definitionFor("DIR", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(dir)
        assertEquals("https://amospromanual.dev/10-02-disc-access.html#i-dir", dir.link?.toString())

        val arexx = AmosDefinitionRegistry.definitionFor("AREXX$", AmosDefinitionKind.FUNCTION)
        assertNotNull(arexx)
        assertEquals(AmosValueType.STRING, arexx.returnType)
        assertEquals("https://amospromanual.dev/10-06-arexx.html#fn-arexx-dollar", arexx.link?.toString())

        val say = AmosDefinitionRegistry.definitionFor("SAY", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(say)
        assertEquals("https://amospromanual.dev/11-02-speech.html#i-say", say.link?.toString())

        val prgState = AmosDefinitionRegistry.definitionFor("PRG STATE", AmosDefinitionKind.FUNCTION)
        assertNotNull(prgState)
        assertEquals(AmosValueType.INTEGER, prgState.returnType)
        assertEquals("https://amospromanual.dev/11-04-multitasking.html#fn-prg-state", prgState.link?.toString())

        val devBase = AmosDefinitionRegistry.definitionFor("DEV BASE", AmosDefinitionKind.FUNCTION)
        assertNotNull(devBase)
        assertEquals(AmosValueType.INTEGER, devBase.returnType)
        assertEquals("https://amospromanual.dev/11-05-libraries-and-devices.html#fn-dev-base", devBase.link?.toString())

        val monitor = AmosDefinitionRegistry.definitionFor("MONITOR", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(monitor)
        assertEquals("https://amospromanual.dev/12-01-monitor.html#i-monitor", monitor.link?.toString())

        val onError = AmosDefinitionRegistry.definitionFor("ON ERROR", AmosDefinitionKind.STRUCTURE)
        assertNotNull(onError)
        assertEquals("https://amospromanual.dev/12-02-error-handling.html#str-on-error", onError.link?.toString())

        val errString = AmosDefinitionRegistry.definitionFor("ERR$", AmosDefinitionKind.FUNCTION)
        assertNotNull(errString)
        assertEquals(AmosValueType.STRING, errString.returnType)
        assertEquals("https://amospromanual.dev/12-02-error-handling.html#fn-err-dollar", errString.link?.toString())

        val setAccessory = AmosDefinitionRegistry.definitionFor("SET ACCESSORY", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(setAccessory)
        assertEquals("https://amospromanual.dev/13-01-configuration.html#i-set-accessory", setAccessory.link?.toString())

        val deek = AmosDefinitionRegistry.definitionFor("DEEK", AmosDefinitionKind.FUNCTION)
        assertNotNull(deek)
        assertEquals(AmosValueType.INTEGER, deek.returnType)
        assertEquals("https://amospromanual.dev/14-appendix-a-machine-code.html#fn-deek", deek.link?.toString())

        val areg = AmosDefinitionRegistry.definitionFor("AREG", AmosDefinitionKind.FUNCTION)
        assertNotNull(areg)
        assertEquals(AmosValueType.INTEGER, areg.returnType)
        assertEquals("https://amospromanual.dev/14-appendix-a-machine-code.html#resv-areg", areg.link?.toString())

        val killEditor = AmosDefinitionRegistry.definitionFor("KILL EDITOR", AmosDefinitionKind.INSTRUCTION)
        assertNotNull(killEditor)
        assertEquals("https://amospromanual.dev/14-appendix-b-amos-professional-run-time.html#i-kill-editor", killEditor.link?.toString())

        val displayHeight = AmosDefinitionRegistry.definitionFor("DISPLAY HEIGHT", AmosDefinitionKind.FUNCTION)
        assertNotNull(displayHeight)
        assertEquals(AmosValueType.INTEGER, displayHeight.returnType)
        assertEquals("https://amospromanual.dev/14-appendix-c-pal-and-ntsc.html#fn-display-height", displayHeight.link?.toString())

        val copLogic = AmosDefinitionRegistry.definitionFor("COP LOGIC", AmosDefinitionKind.FUNCTION)
        assertNotNull(copLogic)
        assertEquals(AmosValueType.INTEGER, copLogic.returnType)
        assertEquals("https://amospromanual.dev/14-appendix-f-copper-lists.html#fn-cop-logic", copLogic.link?.toString())

        val withExtensions = AmosDefinitionRegistry.loadFromSources(
            listOf(
                AmosDefinitionSource(
                    origin = "core-test",
                    resourcePath = "amos/definitions/core.json",
                    classLoader = this::class.java.classLoader,
                    allowStructures = true
                ),
                AmosDefinitionSource(
                    origin = "compact-test",
                    resourcePath = "amos/definitions/compact.json",
                    classLoader = this::class.java.classLoader
                ),
                AmosDefinitionSource(
                    origin = "ioports-test",
                    resourcePath = "amos/definitions/ioports.json",
                    classLoader = this::class.java.classLoader
                ),
                AmosDefinitionSource(
                    origin = "music-test",
                    resourcePath = "amos/definitions/music.json",
                    classLoader = this::class.java.classLoader
                ),
                AmosDefinitionSource(
                    origin = "compiler-test",
                    resourcePath = "amos/definitions/compiler.json",
                    classLoader = this::class.java.classLoader
                )
            )
        )

        val spack = withExtensions.firstOrNull {
            it.uppercaseName == "SPACK" && it.kind == AmosDefinitionKind.INSTRUCTION
        }
        assertNotNull(spack)
        assertEquals("compact", spack.extensionId)
        assertEquals("Picture compactor extension", spack.extensionName)
        assertEquals("https://amospromanual.dev/06-02-using-screens.html#picoext-spack", spack.link?.toString())

        val getCblock = withExtensions.firstOrNull {
            it.uppercaseName == "GET CBLOCK" && it.kind == AmosDefinitionKind.INSTRUCTION
        }
        assertNotNull(getCblock)
        assertEquals("compact", getCblock.extensionId)
        assertEquals("https://amospromanual.dev/07-07-icons-and-blocks.html#i-get-cblock", getCblock.link?.toString())

        val musicInstruction = withExtensions.firstOrNull {
            it.uppercaseName == "MUSIC" && it.kind == AmosDefinitionKind.INSTRUCTION
        }
        assertNotNull(musicInstruction)
        assertEquals("music", musicInstruction.extensionId)
        assertEquals("https://amospromanual.dev/08-03-playing-music-modules.html#i-music", musicInstruction.link?.toString())

        val medPlay = withExtensions.firstOrNull {
            it.uppercaseName == "MED PLAY" && it.kind == AmosDefinitionKind.INSTRUCTION
        }
        assertNotNull(medPlay)
        assertEquals("music", medPlay.extensionId)
        assertEquals("https://amospromanual.dev/08-03-playing-music-modules.html#i-med-play", medPlay.link?.toString())

        val boom = withExtensions.firstOrNull {
            it.uppercaseName == "BOOM" && it.kind == AmosDefinitionKind.INSTRUCTION
        }
        assertNotNull(boom)
        assertEquals("music", boom.extensionId)
        assertEquals("https://amospromanual.dev/08-01-music.html#i-boom", boom.link?.toString())

        val vumeter = withExtensions.firstOrNull {
            it.uppercaseName == "VUMETER" && it.kind == AmosDefinitionKind.FUNCTION
        }
        assertNotNull(vumeter)
        assertEquals("music", vumeter.extensionId)
        assertEquals(AmosValueType.INTEGER, vumeter.returnType)
        assertEquals("https://amospromanual.dev/08-01-music.html#fn-vumeter", vumeter.link?.toString())

        val samPlay = withExtensions.firstOrNull {
            it.uppercaseName == "SAM PLAY" && it.kind == AmosDefinitionKind.INSTRUCTION
        }
        assertNotNull(samPlay)
        assertEquals("music", samPlay.extensionId)
        assertEquals("https://amospromanual.dev/08-02-samples.html#i-sam-play", samPlay.link?.toString())

        val samSwapped = withExtensions.firstOrNull {
            it.uppercaseName == "SAM SWAPPED" && it.kind == AmosDefinitionKind.FUNCTION
        }
        assertNotNull(samSwapped)
        assertEquals("music", samSwapped.extensionId)
        assertEquals(AmosValueType.INTEGER, samSwapped.returnType)
        assertEquals("https://amospromanual.dev/08-02-samples.html#fn-sam-swapped", samSwapped.link?.toString())

        val printerOpen = withExtensions.firstOrNull {
            it.uppercaseName == "PRINTER OPEN" && it.kind == AmosDefinitionKind.INSTRUCTION
        }
        assertNotNull(printerOpen)
        assertEquals("ioports", printerOpen.extensionId)
        assertEquals("IOPorts", printerOpen.extensionName)
        assertEquals("https://amospromanual.dev/10-03-accessing-a-printer.html#i-printer-open", printerOpen.link?.toString())

        val serialInput = withExtensions.firstOrNull {
            it.uppercaseName == "SERIAL INPUT$" && it.kind == AmosDefinitionKind.FUNCTION
        }
        assertNotNull(serialInput)
        assertEquals("ioports", serialInput.extensionId)
        assertEquals(AmosValueType.STRING, serialInput.returnType)
        assertEquals("https://amospromanual.dev/10-04-accessing-a-serial-port.html#fn-serial-input-dollar", serialInput.link?.toString())
    }

    @Test
    fun `registry can merge extension definition files`() {
        val merged = AmosDefinitionRegistry.loadFromSources(
            listOf(
                AmosDefinitionSource(
                    origin = "core-test",
                    resourcePath = "amos/definitions/core.json",
                    classLoader = this::class.java.classLoader,
                    allowStructures = true
                ),
                AmosDefinitionSource(
                    origin = "extension-test",
                    resourcePath = "amos/definitions/test-extension.json",
                    classLoader = this::class.java.classLoader
                )
            )
        )

        val fake = merged.firstOrNull { it.uppercaseName == "FAKE EXT$" }
        val draw = merged.firstOrNull { it.uppercaseName == "DRAW" }

        assertNotNull(fake)
        assertEquals(AmosDefinitionKind.FUNCTION, fake.kind)
        assertEquals(AmosValueType.STRING, fake.returnType)
        assertEquals("https://amospromanual.dev/14-appendix-d-extensions.html#test-fake-ext-dollar", fake.link?.toString())
        assertNotNull(draw)
        assertTrue(draw.signatures.any { signature ->
            signature.parameters.any { it.kind == AmosParameterKind.KEYWORD && it.keyword == "To" }
        })
    }

    @Test
    fun `uri source skips structures and keeps extension id`() {
        val tempFile = Files.createTempFile("amos-definition", ".json")
        tempFile.toFile().writeText(
            """
            {
              "extension": { "id": "turbo-plus", "name": "Turbo Plus 2.0" },
              "definitions": [
                {
                  "name": "if",
                  "kind": "structure",
                  "signatures": [
                    { "presentation": "If condition Then" }
                  ]
                },
                {
                  "name": "warp",
                  "kind": "instruction",
                  "signatures": [
                    { "presentation": "Warp value", "parameters": [ { "kind": "value", "name": "value", "valueType": "integer" } ] }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val loaded = AmosDefinitionRegistry.loadFromSources(
            listOf(
                amosUriDefinitionSource(
                    uri = tempFile.toUri().toString(),
                    origin = "temp-test"
                )
            )
        )

        val warp = loaded.firstOrNull { it.uppercaseName == "WARP" }
        val structure = loaded.firstOrNull { it.uppercaseName == "IF" && it.kind == AmosDefinitionKind.STRUCTURE }

        assertNotNull(warp)
        assertEquals("turbo-plus", warp.extensionId)
        assertNull(structure)
    }

    @Test
    fun `invalid definition link is ignored`() {
        val tempFile = Files.createTempFile("amos-invalid-link", ".json")
        tempFile.toFile().writeText(
            """
            {
              "definitions": [
                {
                  "name": "bad link",
                  "kind": "function",
                  "returnType": "integer",
                  "link": "https://amospromanual.dev/not valid",
                  "signatures": [
                    { "presentation": "Bad Link" }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val loaded = AmosDefinitionRegistry.loadFromSources(
            listOf(
                amosUriDefinitionSource(
                    uri = tempFile.toUri().toString(),
                    origin = "invalid-link-test"
                )
            )
        )

        val definition = loaded.single()
        assertNull(definition.link)
    }
}

