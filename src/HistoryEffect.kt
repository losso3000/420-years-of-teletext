import java.io.File
import kotlin.experimental.and
import kotlin.math.*
import kotlin.random.Random

enum class HistoryState(val duration: Double) {
    FADE_IN_PLASMA(5.0),
    KEEP_PLASMA(1.0),

    INVENTOR1_APPEAR(1.0),
    INVENTOR1_KEEP(11.0),
    INVENTOR1_DISAPPEAR(1.0),

    INVENTOR2_APPEAR(1.0),
    INVENTOR2_KEEP(11.0),
    INVENTOR2_DISAPPEAR(1.0),

    INVENTOR3_APPEAR(1.0),
    INVENTOR3_KEEP(11.0),
    INVENTOR3_DISAPPEAR(1.0),

    INVENTOR4_APPEAR(1.0),
    INVENTOR4_KEEP(11.0),
    INVENTOR4_DISAPPEAR(1.0),

    INVENTOR5_APPEAR(1.0),
    INVENTOR5_KEEP(11.0),
    INVENTOR5_DISAPPEAR(1.0),

    INVENTOR6_APPEAR(1.0),
    INVENTOR6_KEEP(11.0),
    INVENTOR6_DISAPPEAR(1.0),

    FADE_OUT_PLASMA(5.0),
    BLACK(1.0)
    ;
    companion object {
        fun getStateByElapsedTime(time: Double): HistoryState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class HistoryEffect: Effect("History") {
    var page = TeletextPage()
    var state: HistoryState = HistoryState.values().first()
    var stateTicks = 0
    var stateStart = 0.0
    val startByState = mutableMapOf<HistoryState, Double>()
    val inventors1 = File("gfx/inventors1.bin").readBytes()
    val inventors2 = File("gfx/inventors2.bin").readBytes()
    val inventors3 = File("gfx/inventors3.bin").readBytes()
    val inventors1Alt = File("gfx/inventors1-alt.bin").readBytes()
    val inventors2Alt = File("gfx/inventors2-alt.bin").readBytes()
    val inventors3Alt = File("gfx/inventors3-alt.bin").readBytes()

    override fun doReset() {
        stateTicks = 0
        stateStart = 0.0
        state = HistoryState.values().first()
        startByState.clear()
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = HistoryState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
            stateStart = elapsed
            startByState[newState] = elapsed
        }
        renderPlasma(elapsed)
        val stateT = (elapsed - stateStart) / newState.duration
        when (newState) {
            HistoryState.FADE_IN_PLASMA -> renderPlasma(elapsed, stateT)
            HistoryState.KEEP_PLASMA -> renderPlasma(elapsed, 1.0)

            HistoryState.INVENTOR1_APPEAR -> whiteStripeInFromRight(stateT, 7)
            HistoryState.INVENTOR1_KEEP -> inventorShow(stateT, 7, 1)
            HistoryState.INVENTOR1_DISAPPEAR -> whiteStripeOutToLeft(stateT, 7)

            HistoryState.INVENTOR2_APPEAR -> whiteStripeInFromRight(stateT, 11)
            HistoryState.INVENTOR2_KEEP -> inventorShow(stateT, 11, 2)
            HistoryState.INVENTOR2_DISAPPEAR -> whiteStripeOutToLeft(stateT, 11)

            HistoryState.INVENTOR3_APPEAR -> whiteStripeInFromRight(stateT, 3)
            HistoryState.INVENTOR3_KEEP -> inventorShow(stateT, 3, 3)
            HistoryState.INVENTOR3_DISAPPEAR -> whiteStripeOutToLeft(stateT, 3)

            HistoryState.INVENTOR4_APPEAR -> whiteStripeInFromLeft(stateT, 10)
            HistoryState.INVENTOR4_KEEP -> inventorShow(stateT, 10, 4)
            HistoryState.INVENTOR4_DISAPPEAR -> whiteStripeOutToRight(stateT, 10)

            HistoryState.INVENTOR5_APPEAR -> whiteStripeInFromLeft(stateT, 3)
            HistoryState.INVENTOR5_KEEP -> inventorShow(stateT, 3, 5)
            HistoryState.INVENTOR5_DISAPPEAR -> whiteStripeOutToRight(stateT, 3)

            HistoryState.INVENTOR6_APPEAR -> whiteStripeInFromLeft(stateT, 10)
            HistoryState.INVENTOR6_KEEP -> inventorShow(stateT, 10, 6)
            HistoryState.INVENTOR6_DISAPPEAR -> whiteStripeOutToRight(stateT, 10)

            HistoryState.FADE_OUT_PLASMA -> renderPlasma(elapsed, 1.0-stateT)

            HistoryState.BLACK -> page.applyFadeToBlack(0.0)
        }
        formatAsPage(page.data, 0x100, buf)
    }

    var plasmaCharEvenY = 0x39
    var plasmaCharOddY = 0x66

    private fun renderPlasma(elapsed: Double, multiplicator: Double = 1.0) {
        for (y in 1 until PAGE_ROWS) {
            page.set(0, y, 0x1e) // hold
            var lastColor: TeletextColor? = null
            for (x in 1 until PAGE_COLS) {
                val xx = (x.toDouble() - 20.0) / 40.0
                val yy = (y.toDouble() - 12.0) / 30.0
                val value = plasma(xx, yy, elapsed * 50.0) * multiplicator
                var colorIndex = round(value * COLORS_SORTED_BY_BRIGHTNESS.size).toInt()
                if (colorIndex < 0) colorIndex = 0
                if (colorIndex >= COLORS_SORTED_BY_BRIGHTNESS.size) colorIndex = COLORS_SORTED_BY_BRIGHTNESS.size - 1
                val color = COLORS_SORTED_BY_BRIGHTNESS[colorIndex]

                val plasmaChar = if (y % 2 == 0) plasmaCharEvenY else plasmaCharOddY

                if (x == 1) {
                    page.set(x, y, 0x10 or color.ordinal)
                    lastColor = color
                } else if (x == 2) {
                    page.set(x, y, plasmaChar)
                    lastColor = null
                } else if (color == lastColor) {
                    page.set(x, y, plasmaChar)
                } else {
                    page.set(x, y, 0x10 or color.ordinal)
                    lastColor = color
                }
            }
        }
        // println("page %02x %02x %02x %02x %02x".format(page.get(0, 18), page.get(1,18), page.get(2,18), page.get(3,18), page.get(4,18)))
    }

    private fun dist(a: Double, b: Double, c: Double, d: Double) = sqrt((a - c) * (a - c) + (b - d) * (b - d))

    private fun plasma(x: Double, y: Double, time: Double): Double {
        var value1 =
            -1.0 * sin(
                dist(x, y, sin(1.3+time/93.0)+0.2, 0.0)*3.0 + time/40.0
            )
        var value3 =
            sin(
                dist(x, y, 0.5, sin(0.8-time/41.0)+0.2)*4.5 + time/20.0
            )
        var value2 = sin(time/30.0 + (x+y) * 3.0 * (sin(time/17.0)*0.3 + y-x) * 4.0)

        // in and out
        val mult = sin(time/49.5)
        value2 = min(mult, value2)

        // if (true) return (value2 + 1.0) / 2.0
        return (value1 + value2 + value3 + 3.0) / 6.0
    }

    val inventorH = 10

    private fun inventorShow(T: Double, yPos: Int, num: Int) {
        for (y in yPos until yPos+inventorH) {
            page.set(0, y, 0x1e) // hold
            page.set(1, y, 0x1d) // new bg
            for (x in 2 until PAGE_COLS) { page.set(x, y, 0x20) }
        }
        var data: ByteArray
        val dataY: Int
        val textX1: Int
        val textX2: Int
        val headX1: Int
        val headX2: Int
        when (num) {
            1 -> { data = inventors1;    dataY =  2; textX1 =  4; textX2 = 24; headX1 = 27; headX2 = 39 }
            2 -> { data = inventors1Alt; dataY = 13; textX1 = 18; textX2 = 37; headX1 =  2; headX2 = 14 }

            3 -> { data = inventors2; dataY =  1; textX1 = 17; textX2 = 38; headX1 =  2; headX2 = 14 }
            4 -> { data = inventors2; dataY = 13; textX1 =  4; textX2 = 25; headX1 = 28; headX2 = 39 }

            5 -> { data = inventors3; dataY =  1; textX1 = 17; textX2 = 39; headX1 =  2; headX2 = 15 }
            6 -> { data = inventors3; dataY = 13; textX1 =  4; textX2 = 25; headX1 = 28; headX2 = 39 }
            else -> throw IllegalArgumentException("inventor $num?!")
        }
        val textData = data

        // special winks
        val galiWink = transLerp(T, 0.65, 0.8, null, null)
        val teslaDown = transLerp(T, 0.55 , 1.0, null, 1.0)
        val einsti = transLerp(T, 0.8, 1.0, null, 1.0)
        val hopper = transLerp(T, 0.7, 1.0, null, 1.0)
        val tvShit = transLerp(T, 0.6, 1.0, null, 1.0)
        val gWink = transLerp(T, 0.6, 1.0, null, 1.0)
        if (num == 1) galiWink?.let { data = inventors1Alt }
        if (num == 2) teslaDown?.let { data = inventors1 }
        if (num == 3) einsti?.let { data = inventors2Alt }
        if (num == 4) hopper?.let { data = inventors2Alt }
        if (num == 5) tvShit?.let { data = inventors3Alt }
        if (num == 6) gWink?.let { data = inventors3Alt }


        val headInT  = transLerp(T, 0.0, 0.07, 0.0, null)
        val headOutT = transLerp(T, 0.95, 1.0)

        val typeInT  = transLerp(T, 0.08, 0.6)
        val typeOutT = transLerp(T, 0.95, 1.0, null, 1.0)

        val headShow = headInT ?: 1.0 - headOutT

        val charsSkipped: Int
        val maxCharsShown: Int
        if (typeOutT != null) {
            charsSkipped = lerp(typeOutT, 1.0, 8.0 * 22.0).toInt()
            maxCharsShown = 1000
        } else {
            charsSkipped = 0
            maxCharsShown = lerp(typeInT, 0.0, 9.0 * 22.0).toInt()
        }
        var charsShown = 0
        for (y in 0 until inventorH) {
            for (x in 0 until PAGE_COLS) {
                val textCh = textData[(dataY + y)*PAGE_COLS + x]
                val gfxCh = data[(dataY + y)*PAGE_COLS + x]
                // type
                if (x in (textX1..textX2) && isText(textCh)) {
                    if (charsShown in charsSkipped until maxCharsShown) {
                        page.set(x, y + yPos, textCh)
                    } else {
                        page.set(x, y + yPos, 0x20)
                    }
                    charsShown++
                } else if (x in (headX1..headX2)) {
                    val set = Random.nextDouble() < headShow
                    if (set || !isGraphicData(gfxCh)) {
                        page.set(x, y + yPos, gfxCh)
                    } else {
                        page.set(x, y + yPos, 0x7f)
                    }
                } else {
                    page.set(x, y + yPos, gfxCh)
                }
            }
        }
        // stylish 1px border top and bottom
        for (x in 0 until PAGE_COLS) if (isGraphicData(page.get(x, yPos-1)))         page.set(x, yPos-1,         page.get(x, yPos-1)         and 0b010_1111)
        for (x in 0 until PAGE_COLS) if (isGraphicData(page.get(x, yPos+inventorH))) page.set(x, yPos+inventorH, page.get(x, yPos+inventorH) and 0b111_1100)

        // page.drawString(0, 23, "T = %1.2f".format(T))
    }

    private fun isText(ch: Byte) = ch in (0x20..0x7e)

    val fadeInStripeW = 24
    val rastersOdd = listOf(0x66, 0x67, 0x67, 0x6f, 0x6f, 0x7f)
    val rastersEven = listOf(0x39, 0x3b, 0x3b, 0x3f, 0x3f, 0x7f)

    private fun whiteStripeInFromRight(T: Double, yPos: Int) = whiteStripeTransition(T, yPos, true)
    private fun whiteStripeOutToRight(T: Double, yPos: Int)  = whiteStripeTransition(1.0-T, yPos, true)
    private fun whiteStripeInFromLeft(T: Double, yPos: Int)  = whiteStripeTransition(1.0-T, yPos, false)
    private fun whiteStripeOutToLeft(T: Double, yPos: Int)   = whiteStripeTransition(T, yPos, false)

    private fun whiteStripeTransition(T: Double, yPos: Int, rightToLeft: Boolean = false) {
        val xStart: Int = lerp(T, PAGE_COLS.toDouble(), -fadeInStripeW.toDouble()).roundToInt()
        val xEnd: Int = PAGE_COLS

        for (y in yPos until yPos + inventorH) {
            var stripPos = -1
            for (x in xStart until min(PAGE_COLS, xEnd)) {
                stripPos++
                if (x < 1) continue

                val useStripPos = if (rightToLeft) stripPos else fadeInStripeW - stripPos
                var stripeT = (useStripPos.toDouble() / fadeInStripeW.toDouble()).coerceIn(0.0, 1.0)
                val rasterT: Double?
                val whiteT: Double?

                if (stripeT <= 0.5) {
                    rasterT = stripeT * 2.0
                    whiteT = null
                } else {
                    rasterT = null
                    whiteT = (stripeT - 0.5) * 2.0
                }

                val plasmaStates = page.getLineStates(y)
                var ch = page.get(x, y)
                var filledWhite = false
                if (rasterT != null) {
                    // make raster solid
                    val rasters = if (y % 2 == 0)  rastersEven else rastersOdd
                    val patternIndex = lerp(rasterT, 0.0, rasters.size.toDouble()).roundToInt().coerceIn(0, rasters.size-1)
                    if (!isColorSetter(ch)) {
                        page.set(x, y, rasters[patternIndex])
                    }
                } else if (whiteT != null) {
                    // fade to white
                    val currentCol = TeletextColor.values()[plasmaStates[x].color]
                    val fadedCol = currentCol.applyFadeToWhite(whiteT)
                    filledWhite = plasmaStates[x].color > COLORS_SORTED_BY_BRIGHTNESS.size/2 // wrong compare value, but looks nice
                    if (x == 2) {
                        page.set(x, y, 0x7f)
                    } else if (isColorSetter(ch) || currentCol != fadedCol) {
                        page.set(x, y, 0x10 or fadedCol.ordinal)
                    } else if (!isColorSetter(ch)) {
                        page.set(x, y, 0x7f)
                    }
                }
                // stylish 1px border top and bottom
                if (y == yPos) {
                    (y-1).let         { if (filledWhite && x > 1 && isGraphicData(page.get(x, it))) page.set(x, it, page.get(x, it) and 0b010_1111) }
                    (y+inventorH).let { if (filledWhite && x > 1 && isGraphicData(page.get(x, it))) page.set(x, it, page.get(x, it) and 0b111_1100) }
                }
            }
            if (!rightToLeft) {
                if (xStart > 1) {
                    page.set(1, y, 0x17)
                    for (x in 2 until min(xStart, PAGE_COLS)) {
                        page.set(x, y, 0x7f)
                        if (y == yPos) {
                            (y-1).let         { if (x > 1 && isGraphicData(page.get(x, it))) page.set(x, it, page.get(x, it) and 0b010_1111) }
                            (y+inventorH).let { if (x > 1 && isGraphicData(page.get(x, it))) page.set(x, it, page.get(x, it) and 0b111_1100) }
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    var prev = TeletextDisplayPanel(getTeletextFont())
    var eff = HistoryEffect()
    showInFrame("hist", prev)
    eff.reset()
    while (true) {
        eff.tick()
        prev.render(eff.page.data)
        Thread.sleep(40)
    }
}