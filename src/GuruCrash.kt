import java.awt.image.BufferedImage
import java.io.File
import java.lang.Math.min
import javax.imageio.ImageIO
import kotlin.math.*

enum class GuruState(val duration: Double) {
    BLACK(1.0),
    DARK(1.0),
    GREY(1.0),
    BLACK_BEFORE_GURU(0.2),
    GURU_ONLY(4.0),
    ROTOSCOPE(22.5),
    ROTOSCOPE_FADE_OUT(0.25),
    BLACK_BEFORE_BOOK(0.5),
    BOOK_COVER_FADE_IN(0.5),
    BOOK_COVER_SHOW(4.0),
    BOOK_COVER_FADE_OUT(0.5),
    BOOK_PAGES_FADE_IN(0.5),
    BOOK_PAGES_SHOW(1.5),
    BOOK_DETAIL_FADE_IN(0.3),
    DETAIL_PAUSE(1.0),
    BOOK_DETAIL_WRITER1(2.0),
    BOOK_DETAIL_WRITER2(2.0),
    BOOK_DETAIL_WRITER3(2.0),
    BOOK_DETAIL_WRITER4(2.0),
    FINAL_PAUSE(3.0),
    FADE_OUT_EXCEPT_NOT_RECOVERABLE(0.5),

    NOT_RECOVERABLE_PAUSE(2.0),
    MOVE_CHARS(5.5),
    OR_ENABLE_PAUSE(2.5),
    PAUSE_WITH_QUESTION_MARK(1.0),
    FALL_DOWN_CHARS(1.6),
    EFFECT_OVER(1.0),

    ;
    companion object {
        fun getStateByElapsedTime(time: Double): GuruState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class MovingChar(var char: Char, val x: Int, val y: Int, val toX: Int, val toY: Int, val transitionTime: Double) {
    var start: Double? = null
    var fallDownStart: Double? = null

    fun reset() {
        start = null
        fallDownStart = null
    }

    fun ensureStart(time: Double) {
        if (start == null) start = time
    }

    fun ensureFallDownStart(time: Double) {
        if (fallDownStart == null) fallDownStart = time
    }

    fun getX(time: Double): Int {
        val myStart = start ?: return x
        var elapsed = time - myStart
        var T = (elapsed / transitionTime).coerceIn(0.0, 1.0)
        val y = easeOutQuad(T, x.toDouble(), toX.toDouble())
        return y.toInt()
    }
    fun getY(time: Double): Int {
        val myStart = start ?: return y
        var elapsed = time - myStart
        var T = (elapsed / transitionTime).coerceIn(0.0, 1.0)
        val y = easeOutQuad(T, y.toDouble(), toY.toDouble())
        return y.toInt()
    }
    fun getFallX(time: Double) = toX
    fun getFallY(time: Double) = toY + fallDown(time)

    private fun fallDown(time: Double): Int {
        val myStart = fallDownStart ?: return 0
        var elapsed = time - myStart
        var T = (elapsed / 0.5).coerceIn(0.0, 1.0)
        return easeInCubic(T, 0.0, 6.0 + abs(sin(x.toDouble()*13.0)*5.0)).roundToInt()
    }

    private fun doLerp(time: Double, a: Int, b: Int): Int {
        val myStart = start ?: return a
        var elapsed = time - myStart
        var T = elapsed / transitionTime
        if (T > 1.0) return b
        val value = round(1.0 * a + (b - a) * T).toInt()
        return value
    }
}

class GuruCrashEffect: Effect("Guru/crash") {
    var page = TeletextPage()
    var state: GuruState = GuruState.BLACK
    var stateTicks = 0
    var stateStart = 0.0
    val guruCover = File("gfx/guru-manual-cover.bin").readBytes()
    val guruPages = File("gfx/guru-manual-pages.bin").readBytes()
    val guruDetail = File("gfx/guru-manual-detail.bin").readBytes()
    val orEnable = File("gfx/or-enable.bin").readBytes()
    var guruDetailPage = TeletextPage()
    var frames = readFrames("gfx/guru-typ/processed-frames")
    val framesFps = 25
    val frameCols = 66/2
    val frameRows = 54/3
    val movingChars = listOf(
        // o  4 .
        // r  6 .
        //
        // e 10 .
        // N 12 .
        // a 14 .
        // b 16 .
        // l 18 .
        // e 20 .
        //
        // v 24 .
        // e 26 -
        // c 28 .
        // t 30 .
        // o 32 .
        // r 34 .
        // . 36 .
        MovingChar('N',   13, 16,   12,  9, 0.2),
        MovingChar('o',   14, 16,    4,  9, 0.2),
        MovingChar('t',   15, 16,   30,  9, 0.2),
        MovingChar('r',   17, 16,    6,  9, 0.2),
        MovingChar('e',   18, 16,   10,  9, 0.2),
        MovingChar('c',   19, 16,   28,  9, 0.2),
        MovingChar('o',   20, 16,   32,  9, 0.2),
        MovingChar('v',   21, 16,   24,  9, 0.2),
        MovingChar('e',   22, 16,   20,  9, 0.2),
        MovingChar('r',   23, 16,   34,  9, 0.2),
        MovingChar('a',   24, 16,   14,  9, 0.2),
        MovingChar('b',   25, 16,   16,  9, 0.2),
        MovingChar('l',   26, 16,   18,  9, 0.2),
        MovingChar('e',   27, 16,   26,  9, 0.2),
        MovingChar('.',   28, 16,   36,  9, 0.2),
    )

    init {
        guruDetailPage.takeDataFrom(guruDetail)
    }

    private fun readFrames(dir: String): List<BufferedImage> {
        val ret = mutableListOf<BufferedImage>()
        File(dir).listFiles().sorted().filter { it.name.endsWith(".png") }.forEach {  ret += ImageIO.read(it) }
        return ret
    }

    override fun doReset() {
        state = GuruState.BLACK
        movingChars.forEach { it.reset() }
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = GuruState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
            stateStart = elapsed
        }
        val stateT = (elapsed - stateStart) / newState.duration
        when (newState) {
            GuruState.BLACK -> black()
            GuruState.DARK -> renderSingleColor(TeletextColor.BLUE)
            GuruState.GREY -> renderSingleColor(TeletextColor.CYAN)
            GuruState.BLACK_BEFORE_GURU -> black()
            GuruState.GURU_ONLY -> { black(); copyGuru() }
            GuruState.ROTOSCOPE -> rotoscopeAnim(elapsed - stateStart)
            GuruState.ROTOSCOPE_FADE_OUT -> { fadeOut(stateT); copyGuru() }
            GuruState.BLACK_BEFORE_BOOK -> { black(); copyGuru() }
            GuruState.BOOK_COVER_FADE_IN -> bookCoverFadeIn(stateT)
            GuruState.BOOK_COVER_SHOW -> page.takeDataFrom(guruCover)
            GuruState.BOOK_COVER_FADE_OUT -> { fadeOut(stateT); copyGuru() }
            GuruState.BOOK_PAGES_FADE_IN -> bookPagesFadeIn(stateT)
            GuruState.BOOK_PAGES_SHOW -> page.takeDataFrom(guruPages)
            GuruState.BOOK_DETAIL_FADE_IN -> bookDetailFadeIn(stateT)
            GuruState.BOOK_DETAIL_WRITER1 -> { bookDetailFadeIn(1.0); revealDetailLine(13, stateT) }
            GuruState.BOOK_DETAIL_WRITER2 -> { bookDetailFadeIn(1.0); revealDetailLine(13, 1.0); revealDetailLine(15, stateT) }
            GuruState.BOOK_DETAIL_WRITER3 -> { revealDetailLine(15, 1.0); revealDetailLine(16, stateT, 12) }
            GuruState.BOOK_DETAIL_WRITER4 -> { revealDetailLine(16, stateT) }
            GuruState.FADE_OUT_EXCEPT_NOT_RECOVERABLE -> fadeOutExceptNotRecoverable(stateT)
            GuruState.MOVE_CHARS -> moveChars(stateT)
            GuruState.OR_ENABLE_PAUSE -> moveChars(1.0)
            GuruState.PAUSE_WITH_QUESTION_MARK -> { questionMark(stateT) }
            GuruState.FALL_DOWN_CHARS -> fallDownChars(stateT)
            GuruState.EFFECT_OVER -> page.fill(0x20)
            else -> Unit
        }
        formatAsPage(page.data, 0x100, buf)
    }

    private fun questionMark(T: Double) {
        page.takeDataFrom(orEnable)
        val col1 = transLerp(T, 0.0, 0.6, null, null)
        val col2 = transLerp(T, 0.2, 0.8, null, null)
        val col3 = transLerp(T, 0.4, 1.0, null, null)
        val ord1 = if (col1 != null) lerp(1.0-col1, 0.0, COLORS_SORTED_BY_BRIGHTNESS.size-1.0).toInt() else 0
        val ord2 = if (col2 != null) lerp(1.0-col2, 0.0, COLORS_SORTED_BY_BRIGHTNESS.size-1.0).toInt() else 0
        val ord3 = if (col3 != null) lerp(1.0-col3, 0.0, COLORS_SORTED_BY_BRIGHTNESS.size-1.0).toInt() else 0
        for (y in 2 until 18) {
            for (x in 10 until PAGE_COLS) {
                val ch = page.get(x, y)
                when (ch.toInt()) {
                    0x11 -> page.set(x, y, 0x10 + COLORS_SORTED_BY_BRIGHTNESS[ord1].ordinal)
                    0x13 -> page.set(x, y, 0x10 + COLORS_SORTED_BY_BRIGHTNESS[ord2].ordinal)
                    0x17 -> page.set(x, y, 0x10 + COLORS_SORTED_BY_BRIGHTNESS[ord3].ordinal)
                }
            }
        }
        // inner to outer = red, yellow white
            page.set(36, 9, '?'.code)
            movingChars.last().char = '?'
    }

    private fun fallDownChars(T: Double) {
        val startIndex = (transLerp(T, 0.0, 0.3) * movingChars.size).toInt()
        for (i in 0 until min(movingChars.size, startIndex)) {
            if (movingChars[i].char != '?')
                movingChars[i].ensureFallDownStart(T)
        }
        black()
        movingChars.last().char = '?'
        movingChars.forEach { m ->
            val x = m.getFallX(T)
            val y = m.getFallY(T)
            if (y < PAGE_ROWS) {
                page.set(x, y, m.char.code.toByte())
                page.set(0, y, 0x07)
            }
        }
        transLerp(T, 0.3, 1.0).let { page.applyFadeToBlack(max(1.0-it, 0.2)) }
        page.drawString(0,1,"FALL $T")
    }

    private fun moveChars(T: Double) {
        val startIndex = round((movingChars.size) * lerp(T, 0.0, 3.5)).toInt()
        for (i in 0 until min(movingChars.size, startIndex)) {
            movingChars[i].ensureStart(T)
        }
        black()
        movingChars.forEach { m ->
            val x = m.getX(T)
            val y = m.getY(T)
            page.set(x, y, m.char.code.toByte())
        }
    }

    //  9 fade yellow
    // 10 fade cyan
    // 11 fade blue
    // 12
    // 13   Error code ...
    // 14
    // 15   Caused by ...
    // 16   nostalgia ...
    // 17
    // 18 fade blue
    // 19 fade cyan
    // 20 fade yellow --> total 12 lines
    private fun bookDetailFadeIn(fadeIn: Double) {
        var h = round(8*fadeIn).toInt()
        for (y in 15-h until 15+h) {
            for (x in 0 until PAGE_COLS) {
                val ch = if (y in 12..17) 0x20.toByte() else guruDetailPage.get(x, y)
                page.set(x, y, ch)
            }
        }
    }

    private fun revealDetailLine(y: Int, fadeIn: Double, maxX: Int = PAGE_COLS-1) {
        val cols = round(PAGE_COLS * fadeIn).toInt() + 1
        for (x in 0 .. min(cols, maxX)) {
            page.set(x, y, guruDetailPage.get(x, y))
        }
    }

    private fun fadeOutExceptNotRecoverable(fadeOut: Double) {
        page.takeDataFrom(guruDetail)
        page.applyClippingFadeToBlack(1.0 - fadeOut)
        page.set(12, 16, 0x07.toByte()) // keep "Not recoverable."
    }

    private fun bookPagesFadeIn(fadeIn: Double) {
        page.takeDataFrom(guruPages)
        page.applyFadeToBlack(fadeIn)
        copyGuru()
    }

    private fun bookCoverFadeIn(fadeIn: Double) {
        page.takeDataFrom(guruCover)
        page.applyFadeToBlack(fadeIn)
        copyGuru()
    }

    private fun fadeOut(fadeout: Double) {
        page.applyClippingFadeToBlack(1.0 - fadeout)
    }

    private fun rotoscopeAnim(elapsed: Double) {
        var frameIndex = (elapsed * framesFps).toInt()
        if (frameIndex >= frames.size) frameIndex = frames.size - 1
        val frameBuf = convertImage(frames[frameIndex], 0x20, frameCols, frameRows, 0x17, true)
        for (y in 0 until frameRows) {
            for (x in 0 until frameCols) {
                page.set(x, y+6, frameBuf[x+y*(frameCols+1)])
            }
        }
    }

    private fun copyGuru() {
        for (y in 1..5) {
            for (x in 0 until PAGE_COLS) {
                page.set(x, y, guruCover[x+PAGE_COLS*y])
            }
        }
    }

    private fun black() {
        page.data.indices.forEach { page.data[it] = 0x20.toByte() }
    }

    private fun renderSingleColor(color: TeletextColor) {
        page.data.indices.forEach { page.data[it] = 0x20.toByte() }
        for (y in 1 until PAGE_ROWS) {
            page.set(0, y, color.ordinal.toByte())
            page.set(1, y, 0x1d.toByte()) // new background
        }
    }
}

fun main() {
    val fx = GuruCrashEffect()
    val displayPanel = TeletextDisplayPanel(getTeletextFont())
    showInFrame("doc", displayPanel)
    while (true) {
        fx.tick()
        displayPanel.render(fx.buf)
        Thread.sleep(40)
    }
}
