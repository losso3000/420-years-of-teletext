import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val GREETSW = 39*2
const val GREETSH = 24*3

class Logo(val centerX: Int, val centerY: Int, val img: BufferedImage, val col: TeletextColor) {
    override fun toString() = "[logo at %3d,%3d; %3d*%3d]".format(centerX, centerY, img.width, img.height)
    val mover = SineMover(
        1.0 + SineRND.nextDouble(1.0),
        1.5 + SineRND.nextDouble(1.5),
        5.3 + SineRND.nextDouble(4.8),
        3.0 + SineRND.nextDouble(2.9))
    fun tick() {
        mover.tick()
    }
    fun draw(g: Graphics2D, leftShift: Double, debug: Boolean = false) {
        val x = (getX() - img.width/2.0 - leftShift).roundToInt()
        val y = (getY() - img.height/2.0).roundToInt()
        g.drawImage(img, x, y, null)
        if (debug) {
            g.color = Color.ORANGE
            val cx = (getX() - leftShift).roundToInt()
            val cy = (getY()).roundToInt()
            g.fillRect(cx - 1, cy - 1, 2, 2)
        }
    }
    fun getX() = centerX + mover.x
    fun getY() = centerY + mover.y
    fun distance(x: Int, y: Int) = distance(x.toDouble(), y.toDouble())
    fun distance(x: Double, y: Double): Double {
        val dx = getX() - x
        val dy = getY() - y
        return sqrt(dx*dx + dy*dy)
    }
}

class VoronoiEffect(val debug: Boolean = false) : Effect("Voronoi greets") {
    val logosImage = ImageIO.read(File("gfx/logos.png"))
    val areasImage = ImageIO.read(File("gfx/logos-areas.png"))
    val logoColors = listOf(TeletextColor.RED, TeletextColor.YELLOW, TeletextColor.GREEN, TeletextColor.MAGENTA, TeletextColor.GREEN, TeletextColor.WHITE)
    val logos = extractLogos(logosImage, areasImage)
    var leftShift: Double = 0.0
    val viewPortY = 3 * PAGE_ROWS
    val viewPortX = 0
    val page = TeletextPage()
    val canvas = BufferedImage(GREETSW, GREETSH, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()

    override fun doReset() {
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        leftShift = elapsed * 40.0 - 40.0
        // println("left %4.2f".format(leftShift))

        g.color = Color.BLACK
        g.fillRect(0, 0, canvas.width, canvas.height)
        g.translate(-viewPortX, -viewPortY)
        drawConnections(g, canvas, Color.WHITE)
        g.translate(viewPortX, viewPortY)
        val connBuf = convertImage(canvas, includeStartGfxCharacter =  0x14, inverted = true) // blue
        page.takeDataFrom(connBuf)

        g.color = Color.BLACK
        g.fillRect(0, 0, canvas.width, canvas.height)
        g.translate(-viewPortX, -viewPortY)
        logos.forEach { it.tick(); it.draw(g, leftShift, debug) }
        g.translate(viewPortX, viewPortY)
        val logoBuf = convertImage(canvas, includeStartGfxCharacter = 0x17, inverted = true)
        mergeIntoPage(logoBuf)
        formatAsPage(page.data, 0x100, buf)
    }

    private fun mergeIntoPage(logoBuf: ByteArray) {
        for (y in 0 until PAGE_ROWS) {

            var inLogo = false
            for (x in 0 until PAGE_COLS) {
                val thisCh = logoBuf[PAGE_COLS * y + x]
                val nextCh = if (x >= PAGE_COLS -1) 0x20 else logoBuf[PAGE_COLS * y + x + 1]

                // fix top-left
                if (y == 0 && x == 8) {
                    if (nextCh == 0x20.toByte()) {
                        page.set(x, y, 0x14)
                        inLogo = false
                    } else {
                        page.set(x, y, 0x17)
                        inLogo = true
                    }
                    continue
                } else if (thisCh != 0x20.toByte() && x > 0) {
                    page.set(x, y, thisCh)
                } else if (inLogo && nextCh == 0x20.toByte()) {
                    page.set(x, y, 0x14)
                    inLogo = false
                } else if (!inLogo && nextCh != 0x20.toByte()) {
                    page.set(x, y, 0x17)
                    inLogo = true
                } else if (inLogo) {
                    page.set(x, y, thisCh)
                }
            }

        }
    }

    val minDistX = 60
    val minDistY = 60

    fun drawConnections(g: Graphics2D, img: BufferedImage, col: Color = Color.BLUE) {
        logos.forEach { logo ->
            val x = logo.getX()
            val y = logo.getY()
            g.color = Color.BLUE
            val others =  logos.filter { it != logo }.filter { abs(it.getX()-x) < minDistX && abs(it.getY()-y) < minDistY }.sortedBy { it.distance(x, y) }
            others.getOrNull(0)?.let { drawLine(g, logo, it, leftShift) }
            others.getOrNull(1)?.let { drawLine(g, logo, it, leftShift) }
            others.getOrNull(2)?.let { drawLine(g, logo, it, leftShift) }
        }
    }

    private fun drawLine(g: Graphics2D, a: Logo, b: Logo, leftShift: Double) {
        g.drawLine((a.getX()-leftShift).roundToInt(), a.getY().roundToInt(), (b.getX()-leftShift).roundToInt(), b.getY().roundToInt())
    }

    fun drawVoronois(g: Graphics2D, img: BufferedImage) {
        g.color = Color.BLACK
        g.fillRect(0, 0, img.width, img.height)
        for (y in 0 until img.height step 7) {
            for (x in 0 until img.width step 7) {
                val sorted = logos.filter { abs(it.getX()-x) < minDistX && abs(it.getY()-y) < minDistY }.sortedBy { it.distance(x, y) }
                sorted.firstOrNull()?.let { img.setRGB(x, y, it.col.rgb or 0xff000000.toInt()) }
            }
        }
    }

    private fun extractLogos(logosImage: BufferedImage, areasImage: BufferedImage): List<Logo> {
        val ret = mutableListOf<Logo>()
        var num = 0
        for (y in 1 until areasImage.height) {
            for (x in 1 until areasImage.width) {
                // if (isBlack(areasImage, x, y)) println("BLK $x $y")
                if (!isBlack(areasImage, x, y) || isBlack(areasImage, x-1, y) || isBlack(areasImage, x, y-1)) continue
                var xTo = x
                var yTo = y
                while (isBlack(areasImage, xTo, y)) xTo++
                while (isBlack(areasImage, x, yTo)) yTo++
                val w = xTo - x + 1
                val h = yTo - y + 1
                val extractedImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                val g = extractedImage.createGraphics()
                g.drawImage(logosImage, -x, -y, null)
                g.dispose()
                val cx = x + w/2
                val cy = y + h/2
                val logo = Logo(cx, cy, extractedImage, logoColors[num++ % logoColors.size])
                // println(logo)
                ret += logo
            }
        }
        return ret
    }

    private fun isBlack(img: BufferedImage, x: Int, y: Int) = x < img.width && y < img.height && img.getRGB(x, y) and 0xff000000.toInt() != 0

}

fun main() {
    val v = VoronoiEffect()
    v.reset()
    val tp = TeletextDisplayPanel(getTeletextFont())
    showInFrame("vtest", tp)
    val insight = ImagePanel(v.canvas)
    showInFrame("rendi", insight)
    while (true) {
        v.tick()
        tp.render(v.page.data)
        insight.repaint()
        Thread.sleep(40)
    }
}