import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.geom.Area
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Box
import javax.swing.JPanel
import javax.swing.JSlider
import kotlin.math.*

enum class VectorState(val duration: Double) {
    GEILO_CUBE(9.0),
    ZOOM_OUT_CUBE(1.0),
    ZOOM_IN_ATW(4.0),
    KEEP_ATW(0.5),
    ATW_LOGO_FROM_WHITE(0.5),
    ATW_LOGO_SHOW(5.0),
    ;
    companion object {
        fun getStateByElapsedTime(time: Double): VectorState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class VectorEffect: Effect("Vector") {
    var page = TeletextPage()
    val img = BufferedImage(GFXBUFFER_W, GFXBUFFER_H, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    val backgroundImage = BufferedImage(GFXBUFFER_W, GFXBUFFER_H, BufferedImage.TYPE_INT_RGB)
    val backgroundG = backgroundImage.createGraphics()
    val atwLogo = AtwVector()
    var state = VectorState.values().first()
    var stateTicks = 0
    var stateStart = 0.0
    val startByState = mutableMapOf<VectorState, Double>()
    val atwLogoChars = File("gfx/atwlogo-blacktext.bin").readBytes()
    val bigCubeAmp = 10.0
    val bigCubeScaleSpeed = 3.0
    val bigCubeScaler: (Double) -> Double = { totalElapsed ->
        val elapsed = totalElapsed - startByState.getOrDefault(VectorState.GEILO_CUBE, 0.0)
        val scale = sin(elapsed * bigCubeScaleSpeed) * bigCubeAmp + min(25.0, elapsed * 10.0)
        scale
    }
    val blueLineStart = bigCubeScaleSpeed / 2.0 // start when scale sin goes up first (half period)

    override fun doReset() {
        state = VectorState.values().first()
        stateTicks = 0
        stateStart = 0.0
        startByState.clear()
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = VectorState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
            stateStart = elapsed
            startByState[newState] = elapsed
        }
        val stateElapsed = elapsed - stateStart
        val stateT = (elapsed - stateStart) / newState.duration
        var angleY = elapsed * 1.09
        var angleX = elapsed * 1.07
        when (newState) {
            VectorState.GEILO_CUBE -> drawCube(elapsed, angleX, angleY, bigCubeScaler(elapsed), (stateElapsed - blueLineStart) * 50.0)
            VectorState.ZOOM_OUT_CUBE -> drawCube(elapsed, angleX, angleY, (1.0 - stateT) * bigCubeScaler(elapsed), 100.0)
            VectorState.ZOOM_IN_ATW -> drawLogo(stateT, elapsed)
            VectorState.KEEP_ATW -> { drawLogo(1.0, elapsed); insertBgCommands(page); page.applyFadeToWhite(stateT) }
            VectorState.ATW_LOGO_FROM_WHITE -> showAtwLogo(stateT)
            VectorState.ATW_LOGO_SHOW -> showAtwLogo(1.0)
        }
        formatAsPage(page.data, 0x100, buf)
    }

    private fun showAtwLogo(T: Double) {
        page.takeDataFrom(atwLogoChars)
        page.applyClippingFadeToWhite(1.0-T)
    }

    private fun insertBgCommands(page: TeletextPage) {
        for (y in 1 until PAGE_ROWS) {
            var col = if (isColorSetter(page.get(0, y))) page.get(0, y).toInt() else 0x17
            if (isColorSetter(page.get(1, y))) col = page.get(1, y).toInt()
            if (isColorSetter(page.get(2, y))) col = page.get(2, y).toInt()
            page.set(0, y, 0x10.toByte()) // color black
            page.set(1, y, 0x1d.toByte()) // new background
            page.set(2, y, col.toByte())  // original color
        }
    }

    fun drawCube(totalElapsed: Double, angleX: Double, angleY: Double, scale: Double, blueLineLength: Double) {
        drawCube(g, angleX, angleY, scale)
        val cubeBuf = convertImage(img = img, inverted = true)
        page.takeDataFrom(cubeBuf)
        if (blueLineLength > 0.0) {
            fillWithBlueStuff(page)
            udpateBackground(totalElapsed, blueLineLength)
            insertBackground()
        }
        page.set(8, 0, 0x17.toByte())
    }

    fun drawLogo(T: Double, totalElapsed: Double) {
        val angleX = easeOutQuad(T, -6.0, 0.0)
        val angleY = lerp(T, 7.5, 0.0)
        val scale = easeOutQuad(T, 2.0, 71.0)
        atwLogo.drawLogo(g, angleX, angleY, scale)
        val cubeBuf = convertImage(img = img, inverted = true)
        page.takeDataFrom(cubeBuf)
        fillWithBlueStuff(page)
        udpateBackground(totalElapsed, 100.0)
        insertBackground()
        page.set(8, 0, 0x17.toByte())
    }

    fun drawLogo(angleX: Double, angleY: Double, scale: Double) {
        atwLogo.drawLogo(g, angleX, angleY, scale)
        val cubeBuf = convertImage(img = img, inverted = true)
        page.takeDataFrom(cubeBuf)
        page.set(8, 0, 0x17.toByte())
        formatAsPage(page.data, 0x100, buf)
    }

    private fun insertBackground() {
        val backBuf = convertImage(img = backgroundImage, inverted = true)
        val backgroundRegions = extractGraphicRegions(page.data, 4)
        backgroundRegions.lineRegions.forEach { line ->
            line.cols.forEach { col ->
                page.set(col, line.row, backBuf[col + PAGE_COLS * line.row])
            }
        }
    }

    private fun udpateBackground(elapsed: Double, calculatedlineLength: Double) {
        backgroundG.color = Color.BLACK
        backgroundG.fillRect(0, 0, backgroundImage.width, backgroundImage.height)
        backgroundG.color = Color.WHITE
        val cx = PAGE_COLS*2/2
        val cy = PAGE_ROWS*3/2
        val lineLength = if (calculatedlineLength < 0.0) 0.0 else min(100.0, calculatedlineLength)
        for (i in 0 until 23) {
            val angle1 = i/23.0 * PI * 2.0 + elapsed * 0.7
            val angle2 = i/23.0 * PI * 2.0 + elapsed * 0.7 + 0.05
            val dx1 = round(sin(angle1) * lineLength).toInt()
            val dy1 = round(cos(angle1) * lineLength).toInt()
            val dx2 = round(sin(angle2) * lineLength).toInt()
            val dy2 = round(cos(angle2) * lineLength).toInt()
            val xs = listOf(cx, cx+dx1, cx+dx2, cx).toIntArray()
            val ys = listOf(cy, cy+dy1, cy+dy2, cy).toIntArray()
            backgroundG.fillPolygon(xs, ys, xs.size)
        }
    }

    private fun fillWithBlueStuff(page: TeletextPage) {
        for (y in 1 until PAGE_ROWS) {
            page.set(0, y, 0x14.toByte()) // blue gfx

            var firstGfxCol = -1
            var lastGfxCol = -1
            for (x in 1 until PAGE_COLS) {
                if (page.get(x, y) == 0x20.toByte()) continue
                if (firstGfxCol == -1) firstGfxCol = x
                lastGfxCol = x
            }

            if (firstGfxCol == -1 || lastGfxCol == -1) {
                for (x in 1 until PAGE_COLS) page.set(x, y, 0x7f.toByte()) // full
            } else {
                for (x in 1 until firstGfxCol) page.set(x, y, 0x7f.toByte())
                for (x in lastGfxCol+1 until PAGE_COLS) page.set(x, y, 0x7f.toByte())
                page.set(firstGfxCol-1, y, 0x17.toByte())
                if (lastGfxCol < PAGE_COLS-1) page.set(lastGfxCol+1, y, 0x14.toByte())
            }
        }
    }

}

class AtwVector {
    val outerTriangle = listOf(
    PointXY(428, 44),// # point  0
    PointXY(410, 44),// # point  1
    PointXY(399, 56),// # point  2
    PointXY( 35,723),// # point  3
    PointXY( 30,740),// # point  4
    PointXY( 38,752),// # point  5
    PointXY( 50,760),// # point  6
    PointXY(786,760),// # point  7
    PointXY(800,752),// # point  8
    PointXY(808,744),// # point  9
    PointXY(806,723),// # point 10
    PointXY(440, 55),// # point 11 # outer
    )
    val innerTriangle = listOf(
    PointXY(427,153),// # point 12
    PointXY(412,153),// # point 13
    PointXY(120,687),// # point 14
    PointXY(127,699),// # point 15
    PointXY(710,699),// # point 16
    PointXY(718,686),// # point 17 # inner
    )
    val lana = listOf(
    PointXY(476,283),// # point 18
    PointXY(412,277),// # point 19
    PointXY(386,290),// # point 20
    PointXY(370,284),// # point 21
    PointXY(355,290),// # point 22
    PointXY(358,304),// # point 23
    PointXY(397,342),// # point 24
    PointXY(415,406),// # point 25
    PointXY(416,419),// # point 26
    PointXY(383,472),// # point 27
    PointXY(381,535),// # point 28
    PointXY(350,612),// # point 29
    PointXY(348,642),// # point 30
    PointXY(335,680),// # point 31
    PointXY(354,669),// # point 32
    PointXY(363,648),// # point 33
    PointXY(374,605),// # point 34
    PointXY(407,617),// # point 35
    PointXY(446,611),// # point 36
    PointXY(455,646),// # point 37
    PointXY(451,663),// # point 38
    PointXY(467,682),// # point 39
    PointXY(485,680),// # point 40
    PointXY(467,641),// # point 41
    PointXY(468,463),// # point 42
    PointXY(466,437),// # point 43
    PointXY(479,385),// # point 44
    PointXY(479,374),// # point 45
    PointXY(490,357),// # point 46
    PointXY(478,336),// # point 47
    PointXY(478,317),// # point 48
    PointXY(488,297),// # point 49 # lana
    )
    val lanaArm = listOf(
    PointXY(446,294),// # point 50
    PointXY(469,295),// # point 51
    PointXY(458,317),// # point 52 # armhole
    )
    val lanaLeg = listOf(
    PointXY(421,496),// # point 53
    PointXY(399,559),// # point 54
    PointXY(386,593),// # point 55
    PointXY(412,599),// # point 56
    PointXY(444,598),// # point 57
    PointXY(437,519),// # point 58 # leghole
    )

    val minX = outerTriangle.minOf { it.x }.toDouble()
    val minY = outerTriangle.minOf { it.y }.toDouble()
    val maxX = outerTriangle.maxOf { it.x }.toDouble()
    val maxY = outerTriangle.maxOf { it.y }.toDouble()

    private fun normalizeX(x: Int) = -1.0+(x-minX)/(maxX-minX)*2.0
    private fun normalizeY(y: Int) = -1.0+(y-minY)/(maxY-minY)*2.0
    private fun toPolygon(coordPairs: List<Pair<Double, Double>>) = Polygon(
        coordPairs.map { round(it.first).toInt() }.toIntArray(),
        coordPairs.map { round(it.second).toInt() }.toIntArray(),
        coordPairs.size)

    fun drawLogo(g: Graphics2D, angleX: Double, angleY : Double, scale: Double) {
        g.color = Color.BLACK
        g.fillRect(0, 0,  GFXBUFFER_W, GFXBUFFER_H)
        g.color = Color.WHITE
        val sinA = Math.sin(angleX)
        val cosA = Math.cos(angleX)
        val sinB = Math.sin(angleY)
        val cosB = Math.cos(angleY)

        val outerCoords = getRotatedCoords(outerTriangle, sinA, cosA, sinB, cosB, scale)
        val innerCoords = getRotatedCoords(innerTriangle, sinA, cosA, sinB, cosB, scale)
        val outerPoly = toPolygon(outerCoords)
        val innerPoly = toPolygon(innerCoords)
        val triangle = Area(outerPoly)
        triangle.subtract(Area(innerPoly))

        val lanaCoords = getRotatedCoords(lana, sinA, cosA, sinB, cosB, scale)
        val lanaHole1 = getRotatedCoords(lanaArm, sinA, cosA, sinB, cosB, scale)
        val lanaHole2 = getRotatedCoords(lanaLeg, sinA, cosA, sinB, cosB, scale)
        val lana = Area(toPolygon(lanaCoords))
        lana.subtract(Area(toPolygon(lanaHole1)))
        lana.subtract(Area(toPolygon(lanaHole2)))

        g.translate(GFXBUFFER_W / 2, GFXBUFFER_H / 2)
        g.fill(triangle)
        g.fill(lana)
        g.translate(-GFXBUFFER_W / 2, -GFXBUFFER_H / 2)
    }

    private fun getRotatedCoords(
        points: List<PointXY>,
        sinA: Double,
        cosA: Double,
        sinB: Double,
        cosB: Double,
        scale: Double
    ): List<Pair<Double, Double>> {
        return points.map { p ->
            val x = normalizeX(p.x) * scale
            val y = normalizeY(p.y) * scale
            var z = 0.0

            val xx = x * cosA - y * sinA
            val yy = x * sinA + y * cosA
            val zz = z

            val xxx = xx * cosB - zz * sinB
            val yyy = yy
            val zzz = xx * sinB + zz * cosB

            val D = 55.0
            val Z = if (D + zzz == 0.0) 0.001 else D + zzz
            val projX = xxx * D / (D + Z)
            val projY = yyy * D / (D + Z)

            Pair(projX, projY)
        }
    }

}

fun main() {
    val effect = VectorEffect()
    val preview = TeletextDisplayPanel(getTeletextFont())
    var angleX = 0.0
    var angleY = 0.0
    var scale = 71.0
    var slideX = JSlider(-200, 200, 0)
    var slideY = JSlider(-200, 200, 0)
    var slideZ = JSlider(0, 100, 71)
    val printer: () -> Unit = { println("angle x %3.2f y %3.2f scale %3.2f".format(angleX, angleY, scale)) }
    slideX.addChangeListener { angleX = slideX.value/100.0 * PI; printer() }
    slideY.addChangeListener { angleY = slideY.value/100.0 * PI; printer() }
    slideZ.addChangeListener { scale = slideZ.value.toDouble(); printer() }
    var sliders = Box.createVerticalBox()
    sliders.add(slideX)
    sliders.add(slideY)
    sliders.add(slideZ)
    var panel = JPanel(BorderLayout())
    panel.add(preview, BorderLayout.CENTER)
    panel.add(sliders, BorderLayout.SOUTH)
    showInFrame("vec prev", panel)
    while (true) {
        effect.drawLogo(angleX, angleY, scale)
        preview.render(effect.buf)
        Thread.sleep(20)
    }
}