import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import kotlin.experimental.and
import kotlin.math.*

enum class TitleState(val duration: Double) {
    // PLASMA_ONLY(100.0),
    TITLE_CIRCLE(2.0),
    TRANSITION_TO_TITLE(0.7),
    TITLE_FADE_IN_PLASMA(3.0),
    KEEP_TITLE(3.5),
    TRANSITION_TO_OF(1.0),
    KEEP_OF(0.5),
    TRANSITION_TO_TELETEXT(0.7),
    KEEP_TELTEXT(3.0),
    TRANSITION_TO_TELETEXT_SYMBOL(0.5),
    KEEP_TELETEXT_SYMBOL(1.0),
    FADE_TELETEXT_SYMBOL_WHITE(0.5),
    KEEP_TELETEXT_SYMBOL_WHITE(1.0),
    TRANSITION_TO_DINO(1.0),
    KEEP_DINO_NOTEXT1(3.0),
    KEEP_DINO_420(4.0),
    KEEP_DINO_NOTEXT2(1.0),
    KEEP_DINO_DIG1(0.5),
    KEEP_DINO_DIG2(0.5),
    KEEP_DINO_DIG3(0.5),
    KEEP_DINO_DIG4(2.0),
    FADE_ALL_OUT(0.6),
    BLACK(1.0)
    ;
    companion object {
        fun getStateByElapsedTime(time: Double): TitleState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class TitleEffect: Effect("Title 420") {
    var page = TeletextPage()
    var page420Untouched = TeletextPage()
    var state: TitleState = TitleState.values().first()
    var stateTicks = 0
    var stateStart = 0.0
    val startByState = mutableMapOf<TitleState, Double>()
    val titleCircle = File("gfx/420-circle.bin").readBytes()
    val title420 = File("gfx/420-hol.bin").readBytes()
    val titleOfText = File("gfx/420-of-txt.bin").readBytes()
    val titleOfTextSymbol = File("gfx/420-of.bin").readBytes()
    val titleOnlyOf = File("gfx/420-only-of.bin").readBytes()
    val titleDig = File("gfx/420-dig.bin").readBytes()
    val plasmaEffect = PlasmaEffect()
    val protectedCenterChars = mutableListOf<PointXY>()
    val yellowRegions = extractGraphicRegions(title420, 6)

    val circleLeft = 16
    val circleTop = 6
    val circleW = 19
    val circleH = 13
    val circleImage = BufferedImage(circleW*2, circleH*3, BufferedImage.TYPE_INT_RGB)
    val circleG = circleImage.createGraphics()

    init {
    }

    val teletextSymbolColorSetters = listOf(
        PointXY(13,8),
        PointXY(12,9),
        PointXY(12,10),
        PointXY(11,11),
        PointXY(11,12),
        PointXY(11,13),
        PointXY(12,14),
        PointXY(12,15),
        PointXY(13,16),
    )

    override fun doReset() {
        stateTicks = 0
        stateStart = 0.0
        state = TitleState.values().first()
        startByState.clear()
        plasmaEffect.doReset()
        page420Untouched.takeDataFrom(title420)
        collectCenterArea(page420Untouched, protectedCenterChars)
        plasmaEffect.renderer.dots = listOf(
            DotFromCenter(plasmaEffect.dots[0], 0.57),
            DotFromCenter(plasmaEffect.dots[0], 0.41),
            DotFromCenter(plasmaEffect.dots[2], 0.12),
            DotFromCenter(plasmaEffect.dots[2], 0.61),
            DotFromCenter(plasmaEffect.dots[4], 0.43),
            DotFromCenter(plasmaEffect.dots[4], 0.5),
            DotFromCenter(plasmaEffect.dots[0], 0.4),
            DotFromCenter(plasmaEffect.dots[0], 0.3),
            DotFromCenter(plasmaEffect.dots[2], 0.25),
            DotFromCenter(plasmaEffect.dots[2], 0.33),
            DotFromCenter(plasmaEffect.dots[4], 0.5),
            DotFromCenter(plasmaEffect.dots[4], 0.4),
            DotFromCenter(plasmaEffect.dots[1], 0.3),
            DotFromCenter(plasmaEffect.dots[3], 0.21),
        )
    }

    private fun renderBlackOverlay(g: Graphics2D, w: Int, h: Int, blackness: Double) {
        var blackAlpha = lerp(blackness, 32.0, 255.0).toInt()
        if (blackAlpha <= 0) return
        if (blackAlpha > 255) blackAlpha = 255
        g.color = Color(0,0,0,blackAlpha)
        g.fillRect(0, 0, w, h)
    }

    private fun collectCenterArea(page: TeletextPage, centerChars: MutableList<PointXY>) {
        centerChars.clear()
        yloop@for (y in 1 until PAGE_ROWS) {
            var protected = false
            for (x in 3 until PAGE_COLS-3) {
                val ch = page.get(x, y).toInt()
                if (protected && ch == 0x14) {
                    continue@yloop
                } else if (!protected && (ch == 0x16 || ch == 0x17)) {
                    protected = true
                }
                if (protected) centerChars += PointXY(x, y)
            }
        }
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = TitleState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
            stateStart = elapsed
            startByState[newState] = elapsed
        }
        val stateT = (elapsed - stateStart) / newState.duration
        when (newState) {
            // TitleState.PLASMA_ONLY -> plasmaOnly(ticks, elapsed)
            TitleState.TITLE_CIRCLE -> circleOnly(stateT, elapsed)
            TitleState.TRANSITION_TO_TITLE -> titleBuildUp(stateT, elapsed)
            TitleState.TITLE_FADE_IN_PLASMA -> doTitleWithPlasma(stateT, ticks, elapsed)
            TitleState.KEEP_TITLE -> doTitleWithPlasma(1.0, ticks, elapsed)
            TitleState.TRANSITION_TO_OF -> showOf(stateT, ticks, elapsed)
            TitleState.KEEP_OF -> showOf(1.0, ticks, elapsed)
            TitleState.TRANSITION_TO_TELETEXT -> showTeletextLogo(stateT, ticks, elapsed)
            TitleState.KEEP_TELTEXT -> showTeletextLogo(1.0, ticks, elapsed)
            TitleState.TRANSITION_TO_TELETEXT_SYMBOL -> showTeletextSymbol(stateT, ticks, elapsed, 0.0)
            TitleState.KEEP_TELETEXT_SYMBOL -> showTeletextSymbol(1.0, ticks, elapsed, 0.0)
            TitleState.FADE_TELETEXT_SYMBOL_WHITE -> showTeletextSymbol(1.0, ticks, elapsed, stateT)
            TitleState.KEEP_TELETEXT_SYMBOL_WHITE -> showTeletextSymbol(1.0, ticks, elapsed, 1.0)
            TitleState.TRANSITION_TO_DINO -> showDino(stateT, ticks, elapsed, "     \n     \n     \n        ")
            TitleState.KEEP_DINO_NOTEXT1,
                TitleState.KEEP_DINO_NOTEXT2 -> showDino(1.0, ticks, elapsed, "     \n     \n     \n        ", true)
            TitleState.KEEP_DINO_420 -> showDino(1.0, ticks, elapsed, "420  \nYEARS?\n     \n        ")
            TitleState.KEEP_DINO_DIG1 -> showDino(1.0, ticks, elapsed, "\n     \n     \n        ")
            TitleState.KEEP_DINO_DIG2 -> showDino(1.0, ticks, elapsed, "\n\n     \n        ")
            TitleState.KEEP_DINO_DIG3 -> showDino(1.0, ticks, elapsed, "\n\n\n        ")
            TitleState.KEEP_DINO_DIG4 -> showDino(1.0, ticks, elapsed, "")
            TitleState.FADE_ALL_OUT -> fadeAllOut(stateT, ticks, elapsed)
            TitleState.BLACK -> page.applyFadeToBlack(1.0)
        }
        formatAsPage(page.data, 0x100, buf)
    }

    private fun plasmaOnly(ticks: Int, elapsed: Double) {
        plasmaEffect.renderer.overlayRenderBeforeRastering = { g, w, h -> renderBlackOverlay(g, w, h, 0.0) }
        plasmaEffect.doTick(ticks, elapsed)
        page.takeDataFrom(plasmaEffect.renderer.page.data)
    }

    private fun circleOnly(stateT: Double, elapsed: Double) {
        page.takeDataFrom(titleCircle)
        page.replace(0x14, 0x20)
        addCircleStuff(elapsed)
        page.applyFadeToBlack(stateT)
    }

    private fun addCircleStuff(elapsed: Double, thicking: Double = 0.0) {
        circleG.color = Color.BLACK
        circleG.fillRect(0, 0, circleImage.width, circleImage.height)
        circleG.color = Color.WHITE

        val circleCenterX = 38
        val circleCenterY = 38
        val numCircles = 8
        val circleShift = 1.0 / numCircles.toDouble() * (elapsed * 1.5 % 1.0)
        val oldStroke = circleG.stroke
        circleG.stroke = BasicStroke(lerp(thicking, 1.0, 14.0).toFloat())
        for (i in 0 until numCircles) {
            var circleSize = i.toDouble() / numCircles.toDouble() + circleShift
            circleSize = easeInCubic(circleSize, 0.0, 1.0)
            val circleDrawHalfW = (36.0 / 2.0 * circleSize).roundToInt()
            val circleDrawHalfH = (39.0 / 2.0 * circleSize).roundToInt()
            val circleDrawX = (circleCenterX / 2 - circleDrawHalfW)
            val circleDrawY = (circleCenterY / 2 - circleDrawHalfH)
            val circleDrawW = circleDrawHalfW * 2
            val circleDrawH = circleDrawHalfH * 2

            circleG.drawArc(circleDrawX, circleDrawY, circleDrawW + 1, circleDrawH, 0, 360)
            //circleG.drawRect(circleDrawX, circleDrawY, circleDrawW+1, circleDrawH)
        }

        val buf = convertImage(circleImage, 0x20, circleW, circleH, null, true)
        require(buf.size == circleW * circleH) { "buf size ${buf.size} != $circleW * $circleH = ${circleW * circleH}" }
        yloop@for (y in circleTop until circleTop+circleH) {
            var inYellow = false
            var srcY = y - circleTop
            xloop@for (x in circleLeft-5 until circleLeft + circleW + 4) {
                var srcX = x - (circleLeft-5)
                val ch = page.get(x, y)
                if (ch == 0x13.toByte()) {
                    inYellow = true
                    continue@xloop
                }
                if (ch in 0x10..0x17) {
                    continue@yloop
                }
                if (!inYellow) {
                    continue@xloop
                }
                val srcCh = buf[srcY * circleW + srcX]
                val dstCh: Byte
                if (srcCh == 0x20.toByte() || ch == 0x20.toByte()) {
                    dstCh = 0x20.toByte()
                } else {
                    dstCh = srcCh and ch
                }
                page.set(x, y, dstCh)
            }
        }
    }

    private fun fadeAllOut(stateT: Double, ticks: Int, elapsed: Double) {
        showDino(1.0, ticks, elapsed, "     \n     \n     \n        ")
        val fasterFade = min(1.0, (1.0-stateT) * 1.2)
        page.applyFadeToBlack(fasterFade)
    }

    private fun titleBuildUp(T: Double, totalElapsed: Double) {
        page.takeDataFrom(titleCircle)
        val fromY = 6
        val toY = round(lerp(T, 6.0, 18.0)).toInt()
        for (y in fromY..toY) {
            for (x in 10 until PAGE_COLS-10) {
                val ch = title420[PAGE_COLS*y + x]
                page.set(x, y, ch)
            }
        }
        page.replaceInline(0x14, 0x10)
        addCircleStuff(totalElapsed)
    }

    private fun showOf(T: Double, ticks: Int, elapsed: Double) {
        page.takeDataFrom(title420)
        val fromY = 6
        val toY = round(lerp(T, 6.0, 18.0)).toInt()
        for (y in fromY..toY) {
            for (x in 10 until PAGE_COLS-10) {
                val ch = titleOnlyOf[PAGE_COLS*y + x]
                page.set(x, y, ch)
            }
        }
        addCircleStuff(elapsed)
        mergePlasma(1.0, ticks, elapsed)
    }

    private fun makeRainbow(totalElapsed: Double) {
        var rainbowPos = (1000.0 - totalElapsed) * 0.7 * RAINBOW_COLORS.size
        for (ry in 19 downTo 5) {
            val col = RAINBOW_COLORS[rainbowPos.toInt() % RAINBOW_COLORS.size]
            replaceCyan(ry, col)
            rainbowPos += 0.4
        }
    }

    private fun replaceCyan(y: Int, col: TeletextColor) {
        for (x in 1 until PAGE_COLS) {
            if (page.get(x, y) == 0x16.toByte()) {
                page.set(x, y, 0x10 or col.ordinal)
            }
        }
    }

    fun getElapsed(state: TitleState, totalElapsed: Double) = totalElapsed - startByState.getOrDefault(state, 0.0)

    private fun showDino(textFadeIn: Double, ticks: Int, elapsed: Double, text: String, sideEye: Boolean = false) {
        page.takeDataFrom(titleDig)
        // eye
        if (elapsed % 5.0 < 2.0) {
            page.set(21, 9, 0x6c)
        } else {
            page.set(21, 9, 0x3c)
        }
        // insert text
        var tx = 13
        var ty = 10
        text.forEach { ch ->
            if (ch == '\n') {
                tx = 13
                ty++
            } else {
                page.set(tx, ty, ch.code)
                tx++
            }
        }
        val fromY = round(lerp(textFadeIn, 6.0, 18.0)).toInt()
        val toY = 18
        for (y in fromY..toY) {
            for (x in 10 until PAGE_COLS-10) {
                val ch = titleOfTextSymbol[PAGE_COLS*y + x]
                page.set(x, y, ch)
                teletextSymbolColorSetters.filter { it.y == y }.forEach { page.set(it.x, y, 0x17) } // white teletext symbol
            }
        }
        addCircleStuff(elapsed, textFadeIn)
        makeRainbow(elapsed)
        mergePlasma(1.0, ticks, elapsed)
    }

    private fun showTeletextLogo(textFadeIn: Double, ticks: Int, elapsed: Double) {
        page.takeDataFrom(titleOnlyOf)
        val fromY = 6
        val toY = round(lerp(textFadeIn, 6.0, 18.0)).toInt()
        for (y in fromY..toY) {
            for (x in 10 until PAGE_COLS-10) {
                val ch = titleOfText[PAGE_COLS*y + x]
                page.set(x, y, ch)
            }
        }
        addCircleStuff(elapsed)
        makeRainbow(elapsed)
        mergePlasma(1.0, ticks, elapsed)
    }

    private fun showTeletextSymbol(textFadeIn: Double, ticks: Int, elapsed: Double, fadeToWhite: Double) {
        page.takeDataFrom(titleOfText)
        val fromY = 6
        val toY = round(lerp(textFadeIn, 6.0, 18.0)).toInt()
        for (y in fromY..toY) {
            for (x in 10 until PAGE_COLS-10) {
                val ch = titleOfTextSymbol[PAGE_COLS*y + x]
                page.set(x, y, ch)
            }
        }
        addCircleStuff(elapsed)
        makeRainbow(elapsed)
        val fasterFadeToWhite = min(1.0, fadeToWhite * 1.15)
        if (fasterFadeToWhite > 0.0) {
            teletextSymbolColorSetters.forEach { p ->
                val ch = page.get(p.x, p.y)
                require(ch in 0x10..0x17) { "expected color setter at ${p.x},${p.y}, got %02x".format(ch) }
                val colIndex = ch and 7
                val col = TeletextColor.values()[colIndex.toInt()]
                val newCol = col.applyFadeToWhite(fasterFadeToWhite)
                page.set(p.x, p.y, 0x10 or newCol.ordinal)
            }
        }
        mergePlasma(1.0, ticks, elapsed)
    }

    private fun doTitleWithPlasma(plasmaFadeIn: Double, ticks: Int, elapsed: Double) {
        page.takeDataFrom(title420)
        addCircleStuff(elapsed)
        mergePlasma(plasmaFadeIn, ticks, elapsed)
    }

    private fun mergePlasma(plasmaFadeIn: Double, ticks: Int, elapsed: Double) {
        plasmaEffect.renderer.overlayRenderBeforeRastering = { g, w, h -> renderBlackOverlay(g, w, h, 1.0-plasmaFadeIn) }
        plasmaEffect.doTick(ticks, elapsed)
        val plasmaPage = plasmaEffect.renderer.page
        for (y in 1 until PAGE_ROWS) {
            val states = page420Untouched.getLineStates(y)
            val plasmaStates = plasmaEffect.renderer.page.getLineStates(y)
            for (x in 1 until PAGE_COLS) {
                // first color setter -> take from plasma page
                val plasmaVal = plasmaPage.get(x, y).toInt() and 0x7f
                if (x == 1) {
                    if (plasmaVal == ' '.code) {
                        page.set(x, y, 0x17.toByte())
                    } else {
                        require(isColorSetter(plasmaVal.toByte())) {
                            "Expected color setter at %d,%d, got %02x".format(
                                x,
                                y,
                                plasmaVal
                            )
                        }
                        page.set(x, y, plasmaVal.toByte())
                    }
                    continue
                }
                // switch back to blue graphics in source page? take color from plasma render at that x position
                if (page420Untouched.get(x, y) == 0x14.toByte() && x+1 < PAGE_COLS) {
                    var newColor = plasmaStates[x].color
                    if (newColor != plasmaStates[x+1].color) newColor = plasmaStates[x+1].color
                    if (newColor != plasmaStates[x+2].color) newColor = plasmaStates[x+2].color
                    page.set(x, y, (0x10 or (newColor and 7)).toByte())
                    continue
                }
                // blue graphics block in source page
                if (states[x].color != TeletextColor.BLUE.ordinal) continue
                if (plasmaVal == ' '.code || page420Untouched.get(x, y) == 0x20.toByte()) {
                    page.set(x, y, ' '.code.toByte())
                    continue
                }
                if (!isGraphicData(plasmaVal)) {
                    page.set(x, y, (0x10 or (plasmaStates[x].color and 7)).toByte())
                    continue
                }
                val src = plasmaEffect.renderer.page.get(x, y)
                val dst = page.get(x, y)
                val combined = src and dst
                page.set(x, y, combined)
            }
        }
        // extra pass: fix color glitches when switching back to plasma
        yloop@for (y in 1 until PAGE_ROWS) {
            var inBlueGfx = false
            val pageStates = page.getLineStates(y)
            val plasmaStates = plasmaPage.getLineStates(y)
            for (x in 20 until PAGE_COLS-2) {
                if (!inBlueGfx) {
                    if (page420Untouched.get(x, y) == 0x14.toByte()) inBlueGfx = true
                }
                else {
                    // compare colors after first full-set char in source
                    if (page420Untouched.get(x - 1, y) and 0x7f == 0x7f.toByte() && pageStates[x].color != plasmaStates[x].color) {
                        page.set(x, y, (0x10 or (plasmaStates[x].color and 7)).toByte())
                        continue@yloop
                    }
                }
            }
        }
    }
}