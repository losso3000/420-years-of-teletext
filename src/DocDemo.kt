import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

enum class DocDemoState(val duration: Double) {
    SHOW_ALL(4.0),
    APPEAR_DEMON1(2.0),
    MOVE_DEMON1(3.0),
    START_DEMON2(3.0),
    START_DEMON3(2.0),
    KEEP_SHOWING(6.0),
    FADE_OUT(0.6),
    BLACK(5.0)
    ;
    companion object {
        fun getStateByElapsedTime(time: Double): DocDemoState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class DocDemoEffect: Effect("D.O.C. demo") {
    val renderer = DocDemoRenderer()
    var demonMoveStarted = false
    var state = DocDemoState.values().first()
    var stateTicks = 0
    var stateStart = 0.0
    init {
        renderer.scrollText =
                // ##[----------------><------------------]
            "s0" +
                    "        IGNITION!                  " +
                    "               KICK DOWN!               w4s1" +
                    "   LET'S DO IT!        A.T.W (ATTENTIONWHORE) STRIKES FORWARD !!!             s2 WELCOME TO ONE OF THE MOST SUCCESSFUL AND POWERFUL TELETEXT DEMOS !!!               s1 TO MANTAIN THE HIGH PERFORMANCE AND RELIABILITY"
    }
    override fun doReset() {
        state = DocDemoState.values().first()
        stateTicks = 0
        stateStart = 0.0
        renderer.demons.clear()
        renderer.scrollPos = 0
        demonMoveStarted = false
    }
    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = DocDemoState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
            stateStart = elapsed
        }
        val stateElapsed = elapsed - stateStart
        val stateT = stateElapsed / newState.duration
        renderer.tick()
        when (newState) {
            DocDemoState.APPEAR_DEMON1 -> {
                if (renderer.demons.isEmpty()) {
                    renderer.demons += Demon(PI, PI)
                    renderer.demons[0].sinStep = 0.0
                    renderer.demons[0].cosStep = 0.0
                }
            }
            DocDemoState.MOVE_DEMON1 -> {
                if (!demonMoveStarted) {
                    renderer.demons[0].sinStep = -0.05
                    renderer.demons[0].cosStep = -0.05
                    demonMoveStarted = true
                }
            }
            DocDemoState.START_DEMON2 -> {
                if (renderer.demons.size == 1) {
                    renderer.demons += Demon(PI, PI)
                    renderer.demons[1].pauseWhenBallVisible = 0
                    renderer.demons[1].animateInFromBallPhase = true
                    renderer.demons[1].sinStep = 0.0
                    renderer.demons[1].cosStep = 0.0
                    renderer.demons[1].sinStep = -0.05
                    renderer.demons[1].cosStep = -0.05
                }
            }
            DocDemoState.START_DEMON3 -> {
                if (renderer.demons.size == 2) {
                    renderer.demons += Demon(PI, PI)
                    renderer.demons[2].pauseWhenBallVisible = 0
                    renderer.demons[2].animateInFromBallPhase = true
                    renderer.demons[2].sinStep = 0.0
                    renderer.demons[2].cosStep = 0.0
                    renderer.demons[2].sinStep = -0.05
                    renderer.demons[2].cosStep = -0.05
                }
            }
            DocDemoState.FADE_OUT -> renderer.page.applyFadeToBlack(1.0-stateT)
            DocDemoState.BLACK -> renderer.page.applyFadeToBlack(0.0)
            else -> Unit
        }
        formatAsPage(renderer.page.data, 0x100, buf)
    }
}

class Demon(var sinPos: Double = 0.0, var cosPos: Double = 0.0, var pauseWhenBallVisible: Int = 20) {
    var x = 0
    var y = 0
    var wingPhase = 1 // 0 1 2 3
    var wingSpeed = 2
    var wingSpeedWait = 0
    var sinStep = 0.1
    var cosStep = 0.1
    var fromBallWait = 4
    var fromBallSpeed = 4
    var fromBallPhase = 0
    var fromBall = true
    val wingPhases = listOf(0, 1, 2, 3, 2, 1, 0)
    var pauseTicks = 0
    var animateInFromBallPhase = false
    fun tick() {
        x = round(17*2 + sin(sinPos)*12*2).toInt()
        y = round(8*3  + cos(cosPos)*7*3).toInt()
        if (pauseTicks-- > 0) return
        // from ball?
        if (fromBall) {
            if (animateInFromBallPhase) {
                sinPos += sinStep
                cosPos += cosStep
            }
            if (fromBallWait-- > 0) return
            fromBallWait = fromBallSpeed
            fromBallPhase++
            if (fromBallPhase == 3) {
                pauseTicks = pauseWhenBallVisible
            }
            if (fromBallPhase >= 6) {
                fromBall = false
                return
            }
            return
        }
        sinPos += sinStep
        cosPos += cosStep
        // animate wings
        if (wingSpeedWait-- > 0) return
        wingPhase = (wingPhase + 1) % wingPhases.size
        wingSpeedWait = wingSpeed
    }
    fun getWingImageIndex() = wingPhases[wingPhase]
}

class DocDemoRenderer {
    val background: ByteArray
    val perspectiveImage: BufferedImage
    val checkerImage: BufferedImage
    val checkerRendered: BufferedImage
    val checkerRenderedG: Graphics2D
    var checkerTicks = 0
    var checkerSize = 128
    val page = TeletextPage()
    var scrollText: String? = null
    var scrollPauseTicks = 0
    var scrollPos = 0
    var scrollSpeed = 0
    var scrollWaitTicks = 0
    val scrollBuffer = mutableListOf<Char>()
    val demonImages = mutableListOf<BufferedImage>()
    val demonBallImages = mutableListOf<BufferedImage>()
    val demons = mutableListOf<Demon>()
    var atwFlashFrames = 0
    var checkerSpeed = 11
    init {
        background = File("gfx/doc-demo-template.bin").readBytes()
        perspectiveImage = ImageIO.read(File("gfx/doc-checker-perspective.png"))
        checkerImage = ImageIO.read(File("gfx/doc-checker.png"))
        checkerRendered = BufferedImage(checkerImage.width, checkerImage.height, BufferedImage.TYPE_INT_RGB)
        checkerRenderedG = checkerRendered.createGraphics()
        repeat(38) { scrollBuffer += ' ' }
        demonImages += ImageIO.read(File("gfx/demon1.png"))
        demonImages += ImageIO.read(File("gfx/demon2.png"))
        demonImages += ImageIO.read(File("gfx/demon3.png"))
        demonImages += ImageIO.read(File("gfx/demon4.png"))
        demonBallImages += ImageIO.read(File("gfx/demonballa.png"))
        demonBallImages += ImageIO.read(File("gfx/demonballb.png"))
        demonBallImages += ImageIO.read(File("gfx/demonballc.png"))
        demonBallImages += ImageIO.read(File("gfx/demonball1.png"))
        demonBallImages += ImageIO.read(File("gfx/demonball2.png"))
        demonBallImages += ImageIO.read(File("gfx/demonball3.png"))
    }

    var lastFlashEventId = 0
    fun flashLogo(eventId: Int) {
        if (eventId == lastFlashEventId) return
        atwFlashFrames = 9
        lastFlashEventId = eventId
    }

    private fun performFlashLogo() {
        if (atwFlashFrames-- > 0) {
            val col1: TeletextColor
            val col2: TeletextColor
            when (atwFlashFrames/2) {
                0 ->    { col1 = TeletextColor.CYAN;   col2 = TeletextColor.CYAN }
                1 ->    { col1 = TeletextColor.YELLOW; col2 = TeletextColor.CYAN }
                2 ->    { col1 = TeletextColor.YELLOW; col2 = TeletextColor.YELLOW }
                3 ->    { col1 = TeletextColor.WHITE;  col2 = TeletextColor.YELLOW }
                4 ->    { col1 = TeletextColor.WHITE;  col2 = TeletextColor.WHITE }
                else -> { col1 = TeletextColor.CYAN;   col2 = TeletextColor.CYAN }
            }
            // 12,7  8  9
            page.set(12, 7, 0x10 + col2.ordinal)
            page.set(12, 8, 0x10 + col1.ordinal)
            page.set(12, 9, 0x10 + col2.ordinal)
        }
    }

    fun tick() {
        page.takeDataFrom(background)
        performFlashLogo()
        updateScroller()
        val checkerImage = renderCheckerImage()
        updateDemon(checkerImage)
        updateCheckerBoard(checkerImage)
    }

    private fun updateDemon(checkerImage: BufferedImage) {
        val copy = demons.toList()
        copy.forEach { demon ->
            demon.tick()
            val image: BufferedImage
            if (demon.fromBall) {
                image = demonBallImages[demon.fromBallPhase]
            } else {
                image = demonImages[demon.getWingImageIndex()]
            }
            val pageX = demon.x/2
            val pageY = demon.y/3
            val shiftX = demon.x and 0x01
            val shiftY = demon.y % 3
            val imgCols = 7
            val imgRows = if (shiftY == 0) 3 else 4
            val rendered = BufferedImage(imgCols*2, imgRows*3, BufferedImage.TYPE_INT_RGB)
            val g = rendered.createGraphics()
            g.color = Color.WHITE
            g.fillRect(0, 0, rendered.width, rendered.height)
            g.drawImage(image, shiftX, shiftY, null)
            val buf = convertImage(rendered, 0x20, imgCols, imgRows, null)
            for (y in 0 until imgRows) {
                val states = page.getLineStates(pageY + y)

                // draw narrower if start/end column are empty (test third row)
                val startX = if (buf[imgCols*2] == 0x20.toByte()) 1 else 0
                val endX = if (buf[imgCols*2-1] == 0x20.toByte()) imgCols-1 else imgCols

                if (states[pageX + startX - 1].splitGraphics) page.set(pageX + startX - 2, pageY + y, 0x19.toByte()) // deactivate split
                page.set(pageX + startX - 1, pageY + y, 0x11.toByte()) // graphic red
                for (x in startX until endX) {
                    page.set(pageX + x, pageY + y, buf[x + y*imgCols])
                }
                // restore color
                if (states[pageX + endX].color != 1) page.set(pageX + endX, pageY + y, (0x10 + states[pageX + endX].color).toByte())
                // restore split
                if (states[pageX + endX].splitGraphics) page.set(pageX + endX + 1, pageY + y, 0x1a.toByte())

                drawDemonShadow(checkerImage, image, demon.x, demon.y)
            }
        }
    }

    private fun drawDemonShadow(checkerImage: BufferedImage, demon: BufferedImage, x: Int, y: Int) {
        val g = checkerImage.createGraphics()
        val drawY = round(5.0*y.toDouble()/(15.0*3.0) + 2.0).toInt() // 0..(8+7)*3 -> 2..7
        g.color = Color.BLACK
        g.drawImage(demon, x-2, drawY, demon.width, 4, null)
    }

    private fun updateScroller() {
        // always draw current buffer
        for (x in 2 until 40) { page.set(x, 22, scrollBuffer[x-2].code.toByte()) }

        val text = scrollText
        if (scrollPauseTicks-- > 0) return
        if (text == null) return
        if (scrollWaitTicks-- > 0) return
        scrollWaitTicks = scrollSpeed

        // special command "wait"
        if (text[scrollPos] == 'w') {
            scrollPauseTicks = (text[scrollPos+1].code - '0'.code) * 10
            scrollPos += 2
            // println("encountered wait in scroller: ${text[scrollPos-1]}, scrollPauseTicks=$scrollPauseTicks")
            return
        }
        // special command "speed"
        if (text[scrollPos] == 's') {
            scrollSpeed = text[scrollPos+1].code - '0'.code
            scrollPos += 2
            // println("encountered speed in scroller: ${text[scrollPos-1]}, scrollSpeed=$scrollSpeed")
        }
        scrollBuffer.removeFirst()
        scrollBuffer += text[scrollPos]
        // next char
        scrollPos = (scrollPos + 1) % text.length
    }

    private fun updateCheckerBoard(checkerImage: BufferedImage) {
        checkerTicks += checkerSpeed
        val checkerBuf = convertImage(checkerImage, 0x20, 39, 4, null, true)
        require(checkerBuf.size == 39 * 4) { "checker buf size?! expected 39*4 = ${39*4}, got ${checkerBuf.size}" }
        for (y in 0 until 4) {
            for (x in 0 until 39) {
                page.set(x+1, y+18, checkerBuf[x+39*y])
            }
        }
    }

    fun renderCheckerImage(): BufferedImage {
        for (y in 0 until checkerImage.height) {
            val z = ((perspectiveImage.getRGB(perspectiveImage.width/2, y) and 0xff0000) shr 16) + checkerTicks
            val invert = (z % checkerSize) < checkerSize/2
            for (x in 0 until checkerImage.width) {
                val rgb = checkerImage.getRGB(x, y)
                val set = (rgb and 0xff) > 100
                val col = if (set xor invert) 0xff_ffffff.toInt() else 0xff_000000.toInt()
                checkerRendered.setRGB(x, y, col)
            }
        }
        return checkerRendered
    }
}


fun main() {
    val fx = DocDemoEffect()
    val displayPanel = TeletextDisplayPanel(getTeletextFont())
    showInFrame("doc", displayPanel)
    while (true) {
        fx.tick()
        displayPanel.render(fx.buf)
        Thread.sleep(40)
    }
}
