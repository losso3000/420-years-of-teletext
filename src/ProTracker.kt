import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import kotlin.random.Random

val equalizerBars = listOf(
    TeletextSprite(listOf(listOf(GFX_RED, BOX3,BOX3, TXT_BLUE),listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(GFX_RED, BOX3,BOX3, TXT_BLUE),listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(GFX_RED, BOX2,BOX2, TXT_BLUE),listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(GFX_RED, BOX2,BOX2, TXT_BLUE),listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(GFX_RED, BOX1,BOX1, TXT_BLUE),listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(GFX_RED, BOX1,BOX1, TXT_BLUE),listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(GFX_YEL, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(GFX_YEL, BOX2,BOX2, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(GFX_YEL, BOX2,BOX2, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(GFX_YEL, BOX1,BOX1, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(GFX_YEL, BOX1,BOX1, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(GFX_GRN, BOX2,BOX2, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(GFX_GRN, BOX2,BOX2, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(GFX_GRN, BOX1,BOX1, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(GFX_GRN, BOX1,BOX1, TXT_BLUE),listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(),                            listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(),                            listOf(GFX_GRN, BOX3,BOX3, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(),                            listOf(GFX_GRN, BOX2,BOX2, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(),                            listOf(GFX_GRN, BOX2,BOX2, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(),                            listOf(GFX_GRN, BOX1,BOX1, TXT_BLUE))),
    TeletextSprite(listOf(listOf(),                            listOf(),                            listOf(),                            listOf(GFX_GRN, BOX1,BOX1, TXT_BLUE))),
)

// Sample thingie: 8 * 9 each = 4 * 3 chars
//
// ...#....
// ...#....
// ...##...
// ...####.
// ###.....
// .##.....
// ..#.....
// ..#.....
// ........

const val QUADRASCOPE_W = 8
const val QUADRASCOPE_H = 9
const val QUADRASCOPE_CHAR_W = QUADRASCOPE_W/2
const val QUADRASCOPE_CHAR_H = QUADRASCOPE_H/3

data class Note(var note: String, var instr: Int) {
    init {
        note = note.replace('#',Char(0x5f)).replace("...", "---")
        instr = if (note == "---") 0 else instr
    }
    fun formatAsString() = "%3s %02x".format(note, instr).uppercase()
}

data class Row(
    val channel1: Note,
    val channel2: Note,
    val channel3: Note,
    val channel4: Note,
)

class Pattern(val rows: List<Row>)

class Sample(val data: DoubleArray) {
    var pos = 0
    var frequency = 0
    fun trigger(frequency: Int) {
        this.frequency = frequency
        pos = 0
    }
    fun tick() {
        pos += frequency
    }
    fun sample(n: Int): List<Double> {
        val ret = mutableListOf<Double>()
        for (i in 0 until n) {
            ret.add(if (pos+i < data.size) data[pos+i] else 0.0)
        }
        return ret
    }
}

class ProTrackerEffect(val template: ByteArray = File("gfx/protracker.bin").readBytes(), val patterns: List<Pattern> = createDocDemoPatterns(), val samples: List<Sample> = createDummySamples()): Effect("ProTracker") {
    var songPos = 0
    var patternPos = 0
    var speed = 6
    var rowTicks = 0
    var totalTicks = 0
    var eqLevel1 = 0
    var eqLevel2 = 0
    var eqLevel3 = 0
    var eqLevel4 = 0
    val quadrascopeImage = BufferedImage(QUADRASCOPE_W*4, QUADRASCOPE_H, BufferedImage.TYPE_INT_RGB)
    val quadrascopeG = quadrascopeImage.createGraphics()
    var sample1 : Sample? = null
    var sample2 : Sample? = null
    var sample3 : Sample? = null
    var sample4 : Sample? = null
    fun internalTick() {
        totalTicks++
        rowTicks++
        if (rowTicks > speed) {
            rowTicks = 0
            patternPos++
            if (patternPos >= patterns[songPos].rows.size) {
                songPos = (songPos + 1) % patterns.size
                patternPos = 0
            }
        }
        eqLevel1--
        eqLevel2--
        eqLevel3--
        eqLevel4--
        sample1?.let { it.tick() }
        sample2?.let { it.tick() }
        sample3?.let { it.tick() }
        sample4?.let { it.tick() }
        if (rowTicks == 0) {
            with (patterns[songPos].rows[patternPos]) {
                if (channel1.instr != 0) { eqLevel1 = equalizerBars.size - Random.nextInt(4); sample1 = triggerSample(channel1) }
                if (channel2.instr != 0) { eqLevel2 = equalizerBars.size - Random.nextInt(6); sample2 = triggerSample(channel2) }
                if (channel3.instr != 0) { eqLevel3 = equalizerBars.size - Random.nextInt(4); sample3 = triggerSample(channel3) }
                if (channel4.instr != 0) { eqLevel4 = equalizerBars.size - Random.nextInt(7); sample4 = triggerSample(channel4) }
            }
        }
    }

    override fun doReset() {
        songPos = 0
        patternPos = 0
        speed = 6
        rowTicks = 0
        totalTicks = 0
        eqLevel1 = 0
        eqLevel2 = 0
        eqLevel3 = 0
        eqLevel4 = 0
        sample1 = null
        sample2 = null
        sample3 = null
        sample4 = null
    }

    var lastBuf: ByteArray? = null
    var crashPage = TeletextPage()

    override fun doTick(ticks: Int, elapsed: Double) {
        if (elapsed > 11.0) {
            crash()
            return
        }
        val newTicks = (elapsed*50.0).toInt()
        if (ticks == 0) internalTick()
        while (totalTicks < newTicks) internalTick()
        val renderedBuf = render()
        val seconds = elapsed.toInt()
        val elapsed = "%02d:%02d".format(seconds/60, seconds%60)
        renderedBuf[34+9*PAGE_COLS] = elapsed[0].code.toByte()
        renderedBuf[35+9*PAGE_COLS] = elapsed[1].code.toByte()
        renderedBuf[36+9*PAGE_COLS] = elapsed[2].code.toByte()
        renderedBuf[37+9*PAGE_COLS] = elapsed[3].code.toByte()
        renderedBuf[38+9*PAGE_COLS] = elapsed[4].code.toByte()
        formatAsPage(renderedBuf, 0x100, buf)
        lastBuf = renderedBuf
    }

    private fun crash() {
        val crashBuf = lastBuf ?: return
        for (y in 1 until PAGE_ROWS) {
            if (Random.nextDouble() < 0.5) continue
            crashBuf[PAGE_COLS*y] = 0x17
            for (x in 1 until PAGE_COLS) {
                crashBuf[PAGE_COLS*y+x] = Random.nextInt(0x7f).toByte()
            }
        }
        formatAsPage(crashBuf, 0x100, buf)
    }

    private fun triggerSample(note: Note): Sample? {
        val sample = samples[note.instr % samples.size]
        val freq = 4 + Random.nextInt(10); val ret = Sample(sample.data)
        ret.trigger(freq)
        return ret
    }

    fun render(): ByteArray {
        renderRow(template, 13, patterns[songPos], patternPos-4)
        renderRow(template, 14, patterns[songPos], patternPos-3)
        renderRow(template, 15, patterns[songPos], patternPos-2)
        renderRow(template, 16, patterns[songPos], patternPos-1)
        renderRow(template, 17, patterns[songPos], patternPos)
        renderRow(template, 19, patterns[songPos], patternPos+1)
        renderRow(template, 20, patterns[songPos], patternPos+2)
        renderRow(template, 21, patterns[songPos], patternPos+3)
        renderRow(template, 22, patterns[songPos], patternPos+4)
        writeEq(template,  5, 13, eqLevel1)
        writeEq(template, 14, 13, eqLevel2)
        writeEq(template, 23, 13, eqLevel3)
        writeEq(template, 32, 13, eqLevel4)
        renderQuadrascope(template)
        return template
    }

    private fun renderQuadrascope(buf: ByteArray) {
        quadrascopeG.color = Color.WHITE
        quadrascopeG.fillRect(0, 0, quadrascopeImage.width, quadrascopeImage.height)
        quadrascopeG.color = Color.BLACK
        drawQuadrascope(sample1, quadrascopeG, 0*QUADRASCOPE_W, 0)
        drawQuadrascope(sample2, quadrascopeG, 1*QUADRASCOPE_W, 0)
        drawQuadrascope(sample3, quadrascopeG, 2*QUADRASCOPE_W, 0)
        drawQuadrascope(sample4, quadrascopeG, 3*QUADRASCOPE_W, 0)
        val qBuf = convertImage(quadrascopeImage, 0x20, 4*QUADRASCOPE_CHAR_W, QUADRASCOPE_CHAR_H)
        val stride = qBuf.size / QUADRASCOPE_CHAR_H
        for (charX in 0 until 4*QUADRASCOPE_CHAR_W) {
            for (charY in 0 until QUADRASCOPE_CHAR_H) {
                buf[PAGE_COLS*(charY+5) + charX+23] = qBuf[charY * stride + charX + 1]
            }
        }
    }

    private fun drawQuadrascope(sample: Sample?, g: Graphics2D, x: Int, y: Int) {
        if (sample == null) {
            for (dx in 0 until QUADRASCOPE_W-1) {
                drawQuadrascopeSlice(g, x+dx, y, 0.0)
            }
            return
        }
        sample.sample(QUADRASCOPE_W-1).forEachIndexed { index, value ->
            drawQuadrascopeSlice(g, x+index, y, value)
        }
    }

    private fun drawQuadrascopeSlice(g: Graphics2D, x: Int, y: Int, value: Double) {
        val half = QUADRASCOPE_H/2.0
        val yTo   = y + Math.round(0.5 + half + half * value).toInt()
        g.fillRect(x, yTo, 1, 1)
    }

    private fun renderRow(buf: ByteArray, y: Int, pattern: Pattern, row: Int) {
        val string1: String
        val string2: String
        val string3: String
        val string4: String
        if (row < 0 || row >= pattern.rows.size) {
            string1 = "      "
            string2 = "      "
            string3 = "      "
            string4 = "      "
        } else {
            string1 = pattern.rows[row].channel1.formatAsString()
            string2 = pattern.rows[row].channel2.formatAsString()
            string3 = pattern.rows[row].channel3.formatAsString()
            string4 = pattern.rows[row].channel4.formatAsString()
        }
        writeIntoBuffer(buf,  4, y, string1)
        writeIntoBuffer(buf, 13, y, string2)
        writeIntoBuffer(buf, 22, y, string3)
        writeIntoBuffer(buf, 31, y, string4)
    }

    private fun writeEq(buf: ByteArray, x: Int, y: Int, eqLevel: Int) {
        if (eqLevel <= 0) return
        val index = equalizerBars.size - eqLevel
        if (index >= equalizerBars.size) return
        val sprite = equalizerBars[index]
        sprite.rows.forEachIndexed { spriteY, bytes ->
            writeIntoBuffer(buf, x, y + spriteY, bytes)
        }
    }

}

fun createDummySamples(): List<Sample> {
    val ret = mutableListOf<Sample>()
    ret.add(Sample(makeSineSampleData(11, 100)))
    ret.add(Sample(makeSineSampleData(9, 110)))
    ret.add(Sample(makeRandomSampleData(200)))
    return ret
}

fun makeSineSampleData(period: Int, totalSize: Int): DoubleArray {
    val samples = mutableListOf<Double>()
    var pos = 0.0
    val step = 1.0 / period * Math.PI * 2.0
    while (samples.size < totalSize) {
        val sin = Math.sin(pos)
        val T = 1.0 * samples.size / totalSize // 0.0 .. 1.0
        pos += step
        var envelope = (1.0 - T) * 2.0 // 2.0 .. 0.0
        if (envelope > 1.0) envelope = 1.0
        samples.add(sin * envelope)
    }
    return samples.toDoubleArray()
}

fun makeRandomSampleData(totalSize: Int): DoubleArray {
    val samples = mutableListOf<Double>()
    while (samples.size < totalSize) {
        val amp = (1.0 - 2.0 * Random.nextDouble()) / 2.0
        val T = 1.0 * samples.size / totalSize // 0.0 .. 1.0
        var envelope = (1.0 - T) * 2.0 // 2.0 .. 0.0
        if (envelope > 1.0) envelope = 1.0
        samples.add(amp * envelope)
    }
    return samples.toDoubleArray()
}

fun createDummyPatterns(): List<Pattern> {
    val ret = mutableListOf<Pattern>()
    repeat(1) {
        val rows = mutableListOf<Row>()
        repeat(64) {
            val note1 = createNote(1)
            val note2 = createNote(2)
            val note3 = createNote(3)
            val note4 = createNote(4)
            rows.add(Row(note1, note2, note3, note4))
        }
        ret.add(Pattern(rows))
    }
    return ret
}

val notes = listOf(
    "C\u005f3",
    "C-5",
    "G-2",
    "E-3",
    "F\u005F3",
    "A\u005F1",
)

fun createNote(channel: Int): Note {
    if (Random.nextDouble() > 0.25) return Note("---", 0)
    val instr = 1 + channel*3 + Random.nextInt(3)
    val note = notes[Random.nextInt(notes.size)]
    return Note(note, instr)
}

fun main() {
    val font = getTeletextFont()
    val imagePanel = createPreviewImagePanel(font)
    val data = File("gfx/protracker.bin").readBytes()
    font.renderTeletextPackets(imagePanel.getImageGraphics(), data)
    val tracker = ProTrackerEffect(data, createDummyPatterns(), createDummySamples())
    val quadraPreview = ImagePanel(tracker.quadrascopeImage)
    showInFrame("quadrascope", quadraPreview)
    while (true) {
        var buffer = tracker.render()
        font.renderTeletextPackets(imagePanel.getImageGraphics(), buffer)
        imagePanel.repaint()
        quadraPreview.repaint()
        tracker.tick()
        Thread.sleep(20L)
    }
}

fun createDocDemoPatterns(): List<Pattern> = listOf(
    Pattern(listOf(
        Row(Note("G-4", 1),Note("A#5", 3),Note("G-5", 1),Note("D-6", 3)), // 0 -------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("G-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("F-4", 1),Note("A-5", 3),Note("F-5", 1),Note("C-6", 3)), // 8 -------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("F-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("D#5", 3),Note("C-5", 1),Note("G-5", 3)), // 16 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("G-5", 3),Note("C-5", 1),Note("C-6", 3)), // 32 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("D-4", 1),Note("A-5", 3),Note("D-5", 1),Note("D-6", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("G-4", 1),Note("A#5", 3),Note("G-5", 1),Note("D-6", 3)), // 40 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("G-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("F-4", 1),Note("A-5", 3),Note("F-5", 1),Note("C-6", 3)), // 48 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("F-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("D#5", 3),Note("C-5", 1),Note("G-5", 3)), // 48 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("G-5", 3),Note("C-5", 1),Note("C-6", 3)), // 56 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("D-4", 1),Note("A-5", 3),Note("D-5", 1),Note("D-6", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
    )),
    Pattern(listOf(
        Row(Note("G-4", 1),Note("A#5", 3),Note("G-5", 1),Note("D-6", 3)), // 0 -------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("G-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("G-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("F-4", 1),Note("A-5", 3),Note("F-5", 1),Note("C-6", 3)), // 8 -------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("F-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("F-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("D#5", 3),Note("C-5", 1),Note("G-5", 3)), // 16 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("G-5", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("G-5", 3),Note("C-5", 1),Note("C-6", 3)), // 32 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("D-4", 1),Note("A-5", 3),Note("D-5", 1),Note("D-6", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("G-4", 1),Note("A#5", 3),Note("G-5", 1),Note("D-6", 3)), // 40 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("G-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("G-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("F-4", 1),Note("A-5", 3),Note("F-5", 1),Note("C-6", 3)), // 48 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("F-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("F-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("D#5", 3),Note("C-5", 1),Note("G-5", 3)), // 48 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D#5", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D#5", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-5", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("C-4", 1),Note("G-5", 3),Note("C-5", 1),Note("C-6", 3)), // 56 ------------
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("C-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("D-4", 1),Note("A-5", 3),Note("D-5", 1),Note("D-6", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("D-6", 1),Note("...", 3)), //
        Row(Note("...", 1),Note("...", 3),Note("...", 1),Note("...", 3)), //
    )),
    Pattern(listOf(
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("F-6",0)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-5",7)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("F-6",0)),
        Row(Note("G-6",1),Note("F-5",8),Note("G-4",4),Note("C-4",5)),
        Row(Note("...",0),Note("...",0),Note("...",0),Note("...",0)),
        Row(Note("...",0),Note("G-5",9),Note("G-4",4),Note("...",0)),
        Row(Note("...",0),Note("G-5",9),Note("G-4",4),Note("...",0)),
        Row(Note("G-5",1),Note("G-5",9),Note("G-4",4),Note("...",0)),
        Row(Note("A-5",1),Note("G-4",4),Note("G-4",4),Note("...",0)),
        Row(Note("A#5",1),Note("G-4",4),Note("G-5",4),Note("C-5",7)),
        Row(Note("G-5",1),Note("G-5",4),Note("G-4",4),Note("...",0)),
        Row(Note("F-6",1),Note("F-5",8),Note("G-4",4),Note("G-5",1)),
        Row(Note("E-6",1),Note("G-4",4),Note("G-4",4),Note("...",0)),
        Row(Note("D-6",1),Note("G-4",4),Note("G-5",4),Note("C-5",7)),
        Row(Note("C-6",1),Note("G-5",4),Note("G-4",4),Note("...",0)),
        Row(Note("G-5",1),Note("F-5",8),Note("G-4",4),Note("G-5",1)),
        Row(Note("A-5",1),Note("G-4",4),Note("G-4",4),Note("...",0)),
        Row(Note("A#5",1),Note("G-4",4),Note("G-5",4),Note("C-5",7)),
        Row(Note("G-5",1),Note("G-5",4),Note("G-4",4),Note("...",0)),
        Row(Note("F-5",1),Note("F-5",8),Note("G-4",4),Note("G-5",1)),
        Row(Note("E-4",1),Note("G-4",4),Note("G-4",4),Note("...",0)),
        Row(Note("C-5",1),Note("G-4",4),Note("G-5",4),Note("C-5",7)),
        Row(Note("B-4",1),Note("G-5",4),Note("G-4",4),Note("...",0)),
        Row(Note("C-5",1),Note("F-5",8),Note("F-4",4),Note("G-5",1)),
        Row(Note("...",0),Note("F-4",4),Note("F-4",4),Note("...",0)),
        Row(Note("...",0),Note("F-4",4),Note("F-5",4),Note("C-5",7)),
        Row(Note("...",0),Note("F-5",4),Note("F-4",4),Note("...",0)),
        Row(Note("G-5",9),Note("F-5",8),Note("F-4",4),Note("G-5",1)),
        Row(Note("G-5",9),Note("F-4",4),Note("F-4",4),Note("...",0)),
        Row(Note("D-5",9),Note("F-4",4),Note("F-5",4),Note("C-5",7)),
        Row(Note("D-5",9),Note("F-5",4),Note("F-4",4),Note("...",0)),
        Row(Note("C-6",1),Note("F-5",8),Note("F-4",4),Note("G-5",1)),
        Row(Note("A-5",0),Note("F-4",4),Note("F-4",4),Note("...",0)),
        Row(Note("G-5",0),Note("F-4",4),Note("F-5",4),Note("C-5",7)),
        Row(Note("A-5",0),Note("F-5",4),Note("F-4",4),Note("...",0)),
        Row(Note("C-6",0),Note("F-5",8),Note("F-4",4),Note("G-5",1)),
        Row(Note("G-5",0),Note("F-4",4),Note("F-4",4),Note("...",0)),
        Row(Note("C-6",0),Note("F-4",4),Note("F-5",4),Note("C-5",7)),
        Row(Note("B-5",0),Note("F-5",4),Note("F-4",4),Note("C-5",7)),
    )),
)

