import java.io.File
import java.lang.IllegalStateException
import kotlin.math.min
import kotlin.math.roundToInt

class ScrollerFont {
    val glyphByChar = mutableMapOf<Char, TeletextArea>()
    val widthMarker = ','.code.toByte() // ',' = middle dots
    val glyphHeight = 6
    init {
        addGlyphs("gfx/coolfont-a_l.bin",     "ABCDEFGHIJKL")
        addGlyphs("gfx/coolfont-m_x.bin",     "MNOPQRSTUVWX")
        addGlyphs("gfx/coolfont-y_rest.bin",  "YZ2!?.,-Ö&:")
        addGlyphs("gfx/coolfont-symbols.bin", "h _s@ae") // heart, space, badchar, smile, @, ATW, evoke
    }

    private fun addGlyphs(filename: String, chars: String) {
        val page = TeletextPage.read(filename)
        var charPos = 0
        var y = 1
        while (y < page.h-glyphHeight) {
            var x = 1
            while (x < page.w) {
                val glyph = extractGlyph(page, x, y)
                if (glyph != null) {
                    require(charPos < chars.length) { "out of chars to assign at char $charPos" }
                    val char = chars[charPos++]
                    glyphByChar[char] = glyph
                    x += glyph.w
                } else {
                    x++
                }
            }
            y += glyphHeight+1
        }
    }

    private fun extractGlyph(page: TeletextPage, x: Int, y: Int): TeletextArea? {
        var w = 0
        while (x+w < page.w && page.get(x+w, y) == widthMarker) w++
        if (w == 0) return null
        return page.copyRect(x, y+1, w, glyphHeight)
    }

    fun getGlyph(char: Char): TeletextArea {
        return glyphByChar[char] ?: glyphByChar['_'] ?: throw IllegalStateException("Cannot find glyph '$char' or '_'")
    }
}

class ScrollerRenderer(var text: String, val font: ScrollerFont, val columnsPerTick: Int = 1, var colors: List<TeletextColor> = mutableListOf(TeletextColor.RED)) {
    var firstCharX = 0
    var currentGlyphW = 0
    var textPos = 0
    var colorPos = 0
    fun ensureText(newText: String) {
        if (text == newText) return
        text = newText
        firstCharX = 0
        currentGlyphW = 0
        textPos = 0
        colorPos = 0
    }
    fun tick() {
        firstCharX--
        if (firstCharX < -currentGlyphW) {
            textPos = (textPos + 1) % text.length
            val char = text[textPos]
            val glyph = font.getGlyph(char)
            firstCharX += currentGlyphW
            currentGlyphW = glyph.w + 1
            colorPos = (colorPos + 1) % colors.size
        }
    }
    fun render(page: TeletextPage, scrollerY: Int) {
        var x = firstCharX
        var textRenderPos = textPos
        for (y in scrollerY until scrollerY+font.glyphHeight) {
            for (x in 1 until page.w) {
                page.set(x, y, 0x20)
            }
        }
        val originalColorCommand = 0x10 + TeletextColor.MAGENTA.ordinal // set graphic magenta
        while (x < page.w) {
            val color = colors[textRenderPos % colors.size]
            val replaceColorCommand = 0x10 + color.ordinal
            val glyph = font.getGlyph(text[textRenderPos]).replace(originalColorCommand, replaceColorCommand)
            page.drawSprite(glyph, x, scrollerY)
            if (x < 1) {
                cleverlyAdjustLeftBorder(page, glyph, x, scrollerY)
            }
            x += glyph.w + 1
            textRenderPos = (textRenderPos + 1) % text.length
        }
        for (y in scrollerY until scrollerY+font.glyphHeight) {
            page.set(0, y, 0x1e) // hold graphics
        }
    }

    private fun cleverlyAdjustLeftBorder(page: TeletextPage, glyph: TeletextArea, x: Int, y: Int) {
        for (glyphY in 0 until glyph.h) {
            if (page.get(1, y+glyphY) < 0x20) continue
            var cmd = ' '.code.toByte()
            for (glyphX in -x+2 downTo 0) {
                if (glyphX < 0) continue
                if (glyphX >= glyph.w) continue
                val ch = glyph.get(glyphX, glyphY)
                if (isColorSetter(ch)) {
                    cmd = ch
                    break
                }
            }
            page.set(1, y+glyphY, cmd)
        }
    }

}

enum class ScrollerState(val duration: Double) {
    ATWTEXT_IN(2.0),
    ATWTEXT_KEEP1(5.0),

    FADE_OUT_TOP(0.5),
    SCROLL_TOP(5.8),
    FADE_IN_TOP(0.5),

    ATWTEXT_KEEP2(1.8),

    FADE_OUT_MID(0.5),
    SCROLL_MID(5.8),
    FADE_IN_MID(0.5),

    ATWTEXT_KEEP3(1.8),

    FADE_OUT_BOT(0.5),
    SCROLL_BOT(13.0-0.5),
    FADE_IN_BOT(0.5),

    // T = 38

    KEEP_SHOWING(3.22+0.5),
    ATWTEXT_OUT(2.0),

    ENDING_KEEP(3.0),
    ENDING_OUT(1.0),

    BLACK(50.0)
    ;
    companion object {
        fun debugTimes() {
            var until = 0.0
            values().forEach { state ->

                until += state.duration
                println("$state: until $until")
            }
        }
        fun getStateByElapsedTime(time: Double): ScrollerState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class ScrollerEffect(text: String): Effect("Scroller") {
    val topY = 6
    val midY = 12
    val botY = 18
    var scrollerY: Int? = null
    val scrollerRenderer = ScrollerRenderer(text, ScrollerFont(), 1, RAINBOW_COLORS)
    val atwText = File("gfx/atwtext.bin").readBytes()
    val luv = File("gfx/love-txt.bin").readBytes()
    val page = TeletextPage()
    val textPage = TeletextPage()
    var state = ScrollerState.values().first()
    var stateTicks = 0
    var stateStart = 0.0
    override fun doReset() {
        textPage.takeDataFrom(atwText)
        // ScrollerState.debugTimes()
    }
    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = ScrollerState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
            stateStart = elapsed
        }
        val stateElapsed = elapsed - stateStart
        val stateT = stateElapsed / newState.duration

        scrollerY = null

        when (newState) {
            ScrollerState.ATWTEXT_IN -> rollIn(stateT)
            ScrollerState.FADE_IN_TOP  -> applyFades(stateT,     1.0, 1.0)
            ScrollerState.FADE_OUT_TOP -> applyFades(1.0-stateT, 1.0, 1.0)
            ScrollerState.FADE_IN_MID  -> applyFades(1.0, stateT,     1.0)
            ScrollerState.FADE_OUT_MID -> applyFades(1.0, 1.0-stateT, 1.0)
            ScrollerState.FADE_IN_BOT  -> applyFades(1.0, 1.0, stateT    )
            ScrollerState.FADE_OUT_BOT -> applyFades(1.0, 1.0, 1.0-stateT)
            ScrollerState.SCROLL_TOP -> { scrollerY = topY; applyFades(0.0, 1.0, 1.0); scrollerRenderer.ensureText("             CODE: LOSSO            ") }
            ScrollerState.SCROLL_MID -> { scrollerY = midY; applyFades(1.0, 0.0, 1.0); scrollerRenderer.ensureText("             MUSIC: BOD             ") }
            ScrollerState.SCROLL_BOT -> { scrollerY = botY; applyFades(1.0, 1.0, 0.0); scrollerRenderer.ensureText("             RASPI-TELETEXT: ALISTAIR BUXTON                  ") }
            ScrollerState.ATWTEXT_OUT -> rollIn(1.0-stateT)
            ScrollerState.ENDING_KEEP -> page.takeDataFrom(luv)
            ScrollerState.ENDING_OUT -> { page.takeDataFrom(luv); page.applyFadeToBlack(1.0-stateT) }
            ScrollerState.BLACK -> page.fill(0x20)
            else -> page.takeDataFrom(atwText)
        }

        scrollerY?.let {
            scrollerRenderer.render(page, it)
            scrollerRenderer.tick()
        }
        formatAsPage(page.data, 0x100, buf)
    }

    private fun applyFades(a: Double, b: Double, c: Double) {
        page.takeDataFrom(atwText)
        textPage.takeDataFrom(atwText)
        textPage.applyFadeToBlack(min(a, min(b, c)))
        if (a < 1.0) for (y in  6 until 12) for (x in 0 until PAGE_COLS) page.set(x, y, textPage.get(x, y))
        if (b < 1.0) for (y in 12 until 18) for (x in 0 until PAGE_COLS) page.set(x, y, textPage.get(x, y))
        if (c < 1.0) for (y in 18 until 24) for (x in 0 until PAGE_COLS) page.set(x, y, textPage.get(x, y))
    }

    private fun rollIn(rolledIn: Double) {
        page.fill(0x20)
        val delay1 = easeOutQuad(transLerp(rolledIn, 0.0, 0.7), 40.0, 0.0).roundToInt()
        val delay2 = easeOutQuad(transLerp(rolledIn, 0.2, 0.8), 40.0, 0.0).roundToInt()
        val delay3 = easeOutQuad(transLerp(rolledIn, 0.4, 0.9), 40.0, 0.0).roundToInt()
        val delay4 = easeOutQuad(transLerp(rolledIn, 0.6, 1.0), 40.0, 0.0).roundToInt()
        for (y in 1 until 6) drawRowShifted(y, delay1)
        for (y in 6 until 12) drawRowShifted(y, delay2)
        for (y in 12 until 18) drawRowShifted(y, delay3)
        for (y in 18 until 24) drawRowShifted(y, delay4)
    }

    private fun drawRowShifted(y: Int, shift: Int) {
        var srcPos = 0
        textPage.takeDataFrom(atwText)
        for (x in 0 until PAGE_COLS) {
            val ch = if (x >= shift) textPage.get(srcPos++, y) else 0x20
            page.set(x, y, ch)
        }
    }
}

fun main() {
    val fx = ScrollerEffect("  BLAH LBLAH     BLAJKS     ")
    val displayPanel = TeletextDisplayPanel(getTeletextFont())
    showInFrame("doc", displayPanel)
    fx.reset()
    while (true) {
        fx.tick()
        displayPanel.render(fx.buf)
        Thread.sleep(40)
    }
}
