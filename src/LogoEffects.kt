import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JCheckBox
import kotlin.math.roundToInt


class UcLogoEffect {
    val ucLogoBuffer = File("gfx/420-years.bin").readBytes()
    val background = BufferedImage(GFXBUFFER_W, GFXBUFFER_H, BufferedImage.TYPE_INT_RGB)
    val whiteDot = ImageIO.read(File("gfx/whitedot.png"))
    val backgroundRegions = extractGraphicRegions(ucLogoBuffer, 4)
    val yellowRegions = extractGraphicRegions(ucLogoBuffer, 6)

    fun drawDot(g: Graphics2D, angle: Double, ampX: Double, ampY: Double, sizeScale0to1: Double) {
        val w = Math.round(whiteDot.width  * sizeScale0to1).toInt()
        val h = Math.round(whiteDot.height * sizeScale0to1).toInt()
        g.drawImage(whiteDot,
            Math.round(GFXBUFFER_W/2.0 - w/2.0 + Math.sin(angle)*ampX).toInt(),
            Math.round(GFXBUFFER_H/2.0 - h/2.0 + Math.cos(angle)*ampY).toInt() + 1,
            w,
            h,
            null)
    }

    fun renderMasked(buffer: ByteArray, image: BufferedImage, regions: FrameRegions, inverted: Boolean = true): ByteArray {
        val ret = buffer.clone()
        val ditherBuf = convertImage(img = image, inverted = inverted)
        regions.lineRegions.forEach { line ->
            line.cols.forEach { col ->
                val src = buffer[line.row*40+col].toInt()
                val dst = ditherBuf[line.row*40+col].toInt()
                ret[line.row*40+col] = parity(src and dst).toByte()
            }
        }
        return ret
    }
}

enum class EvokeState(val duration: Double) {
    ATWLOGO_KEEP(1.0),
    ATWLOGO_OUT(0.4),
    PAUSE_BEFORE_AT(0.3),
    WHITE_IN(0.6),
    AT_IN(0.6),
    AT_KEEP(2.0),
    AT_OUT(0.6),
    EVOKE_IN(1.0),
    EVOKE_KEEP_FG(3.0),
    EVOKE_GREEN_IN(1.0),
    EVOKE_KEEP_FULL(3.0),
    EVOKE_GREEN_OUT(1.0),
    EVOKE_OUT(1.0),
    PRESENTING_IN(0.6),
    PRESENTING_KEEP(2.0),
    PRESENTING_OUT(0.6),
    WHITE_OUT(0.6),
    BLACK(1.0),
    ;
    companion object {
        fun getStateByElapsedTime(time: Double): EvokeState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class EvokeLogoEffect: Effect("at Evoke") {
    val atwLogo = File("gfx/atwlogo-blacktext.bin").readBytes()
    val at = ImageIO.read(File("gfx/evoke-at.png"))
    val presenting = ImageIO.read(File("gfx/evoke-presenting.png"))
    val fgBuffer = File("gfx/evoke22-fg.bin").readBytes()
    val fullBuffer = File("gfx/evoke22.bin").readBytes()
    val page = TeletextPage()
    val fadePic = BufferedImage(at.width, at.height, BufferedImage.TYPE_INT_RGB)
    val fadePicG = fadePic.createGraphics()

    var state = EvokeState.values().first()
    var stateTicks = 0
    var stateStart = 0.0
    override fun doReset() {
        state = EvokeState.values().first()
        stateTicks = 0
        stateStart = 0.0
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = EvokeState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
            stateStart = elapsed
        }
        val stateElapsed = elapsed - stateStart
        val stateT = stateElapsed / newState.duration
        when (newState) {
            EvokeState.ATWLOGO_KEEP -> drawAtwLogo(1.0)
            EvokeState.ATWLOGO_OUT -> drawAtwLogo(1.0-stateT)
            EvokeState.WHITE_IN -> whiteIn(stateT)
            EvokeState.AT_IN -> drawPresentingAt(1.0-stateT, at)
            EvokeState.AT_KEEP -> drawPresentingAt(0.0, at)
            EvokeState.AT_OUT -> drawPresentingAt(stateT, at)
            EvokeState.EVOKE_IN -> drawForegroundLogoVerticallyFaded(stateT, true)
            EvokeState.EVOKE_KEEP_FG -> page.takeDataFrom(fgBuffer)
            EvokeState.EVOKE_GREEN_IN -> drawFullLogoVerticallyFaded(stateT, false)
            EvokeState.EVOKE_KEEP_FULL -> page.takeDataFrom(fullBuffer)
            EvokeState.EVOKE_GREEN_OUT -> drawFullLogoVerticallyFaded(1.0-stateT, true)
            EvokeState.EVOKE_OUT -> drawForegroundLogoVerticallyFaded(1.0-stateT, false)
            EvokeState.PRESENTING_IN -> drawPresentingAt(1.0-stateT, presenting)
            EvokeState.PRESENTING_KEEP -> drawPresentingAt(0.0, presenting)
            EvokeState.PRESENTING_OUT -> drawPresentingAt(stateT, presenting)
            EvokeState.WHITE_OUT -> whiteIn(1.0-stateT)
            EvokeState.BLACK, EvokeState.PAUSE_BEFORE_AT -> page.fill(0x20)
        }
        formatAsPage(page.data, 0x100, buf)
    }

    private fun whiteIn(stateT: Double) {
        page.fill(0x20)
        val col = TeletextColor.BLACK.applyFadeToWhite(stateT)
        for (y in 1 until PAGE_ROWS) {
            page.set(0, y, 0x10 or col.ordinal)
            page.set(1, y, 0x1d) // new bg
        }
    }

    private fun drawPresentingAt(T: Double, image: BufferedImage) {
        fadePicG.drawImage(image, 0, 0, null)
        fadePicG.color = Color(0, 0, 0, lerp(T, 0.0, 255.0).roundToInt())
        fadePicG.fillRect(0, 0, fadePic.width, fadePic.height)
        page.takeDataFrom(convertImage(dither(fadePic)))
        for (x in 0 until PAGE_COLS) page.set(x, 0, 0x20)
    }

    private fun drawAtwLogo(T: Double) {
        page.takeDataFrom(atwLogo)
        page.applyClippingFadeToBlack(T)
    }

    fun drawForegroundLogoVerticallyFaded(visibility: Double, up: Boolean) {
        page.takeDataFrom(fgBuffer)
        val useVisibility = (if (up) 1.0 - visibility else visibility).coerceIn(0.0, 1.0)
        val totalSteps = 25 + COLORS_SORTED_BY_BRIGHTNESS.size*2
        val startLine = Math.round(useVisibility * totalSteps).toInt() - COLORS_SORTED_BY_BRIGHTNESS.size
        var colorIndex = startLine
        for (y in 1 until PAGE_ROWS) {
            var useColor = (COLORS_SORTED_BY_BRIGHTNESS.size - colorIndex + y).coerceIn(0, COLORS_SORTED_BY_BRIGHTNESS.size-1)
            if (up) useColor = COLORS_SORTED_BY_BRIGHTNESS.size - 1 - useColor
            page.set(2, y, 0x10 or COLORS_SORTED_BY_BRIGHTNESS[useColor].ordinal) 
        }
    }

    fun drawFullLogoVerticallyFaded(visibility: Double, up: Boolean) {
        page.takeDataFrom(fullBuffer)
        val useVisibility = (if (up) 1.0 - visibility else visibility).coerceIn(0.0, 1.0)
        val totalSteps = 25 + COLORS_SORTED_BY_BRIGHTNESS.size*2
        val startLine = Math.round(useVisibility * totalSteps).toInt() - COLORS_SORTED_BY_BRIGHTNESS.size
        var colorIndex = startLine
        for (y in 1 until PAGE_ROWS) {
            var useColor = (COLORS_SORTED_BY_BRIGHTNESS.size - colorIndex + y).coerceIn(0, COLORS_SORTED_BY_BRIGHTNESS.size-1)
            if (up) useColor = COLORS_SORTED_BY_BRIGHTNESS.size - 1 - useColor
            // only fade from white to green
            useColor = useColor.clamp(COLORS_SORTED_BY_BRIGHTNESS.indexOf(TeletextColor.GREEN), COLORS_SORTED_BY_BRIGHTNESS.indexOf(TeletextColor.WHITE))
            // replace all color-set commands of green with color
            for (x in 0 until PAGE_COLS) {
                val idx = PAGE_COLS*y+x
                val src = fullBuffer[idx].toInt()
                if (src == 0x12) {
                    page.set(x, y, 0x10 or COLORS_SORTED_BY_BRIGHTNESS[useColor].ordinal)
                } else {
                    page.set(x, y, src)
                }
            }
        }
    }

}

fun main() {
    val evoke = EvokeLogoEffect()
    val renderer = TeletextDisplayPanel(getTeletextFont())

    showInFrame("hist", renderer)
    evoke.reset()
    while (true) {
        evoke.tick()
        renderer.render(evoke.page.data)
        Thread.sleep(40)
    }

    // OLDE

    /*
        val logo = UcLogoEffect()
        //val dotPreview = ImagePanel(logo.background)
        //showInFrame("bg", dotPreview)
        while (true) {
            val g = logo.background.createGraphics()
            g.color = Color.BLACK
            g.fillRect(0, 0, GFXBUFFER_W, GFXBUFFER_H)
            logo.drawDot(g, t, 12.0, 10.0, 0.6)
            logo.drawDot(g, t * 1.57, 11.0, 9.0, 1.0)
            logo.drawDot(g, -t * 1.29, 11.5, 9.0, 0.6)
            logo.drawDot(g, t * 2.1, 19.0, 16.0, 0.6)
            logo.drawDot(g, t * 2.0, 19.0, 16.0, 0.6)
            logo.drawDot(g, -t * 1.9, 19.0, 16.0, 0.5)
            //dotPreview.repaint()
            dither(logo.background)
            val buf = logo.renderMasked(logo.ucLogoBuffer, logo.background, logo.backgroundRegions)
            val buf2 = logo.renderMasked(buf, logo.background, logo.yellowRegions, false)
            writeIntoBuffer(buf2, 39 - 7, 0, "13:37:00")
            writeIntoBuffer(buf2, 9, 0, "Ooops, wrong party")
            buf2[39 - 8] = parity(0x02).toByte()
            buf2[8] = parity(0x07).toByte()
            font.renderTeletextPackets(previewImage.image.createGraphics(), buf2)
            previewImage.repaint()
            Thread.sleep(100)
            t += 0.1
        }
    */
}



