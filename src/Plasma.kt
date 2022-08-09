import composite.BlendComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

const val PLASMA_SCALE_UP_FACTOR = 4

const val PLASMA_W = 39*PLASMA_SCALE_UP_FACTOR
const val PLASMA_H = 24*PLASMA_SCALE_UP_FACTOR

// 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
//                                     22
//                            11
enum class PLASMA_COLOR(val teletextColor: TeletextColor, val brightness: Int, val drawColor: Color) {
    BLACK        (TeletextColor.BLACK,   2, Color.decode("#000000")),
    BLUE         (TeletextColor.BLUE,    2, Color.decode("#0000cc")),
    RED          (TeletextColor.RED,     2, Color.decode("#cc0000")),
    MAGENTA      (TeletextColor.MAGENTA, 2, Color.decode("#cc00cc")),
    GREEN        (TeletextColor.GREEN,   2, Color.decode("#00cc00")),
    CYAN         (TeletextColor.CYAN,    2, Color.decode("#00cccc")),
    YELLOW       (TeletextColor.YELLOW,  2, Color.decode("#cccc00")),
    WHITE        (TeletextColor.WHITE,   2, Color.decode("#cccccc")),
    HALF_RED     (TeletextColor.RED,     1, Color.decode("#990000")),
    HALF_GREEN   (TeletextColor.GREEN,   1, Color.decode("#009900")),
    HALF_YELLOW  (TeletextColor.YELLOW,  1, Color.decode("#999900")),
    HALF_BLUE    (TeletextColor.BLUE,    1, Color.decode("#000099")),
    HALF_MAGENTA (TeletextColor.MAGENTA, 1, Color.decode("#990099")),
    HALF_CYAN    (TeletextColor.CYAN,    1, Color.decode("#009999")),
    HALF_WHITE   (TeletextColor.WHITE,   1, Color.decode("#999999")),
    QUART_RED    (TeletextColor.RED,     0, Color.decode("#660000")),
    QUART_GREEN  (TeletextColor.GREEN,   0, Color.decode("#006600")),
    QUART_YELLOW (TeletextColor.YELLOW,  0, Color.decode("#666600")),
    QUART_BLUE   (TeletextColor.BLUE,    0, Color.decode("#000066")),
    QUART_MAGENTA(TeletextColor.MAGENTA, 0, Color.decode("#660066")),
    QUART_CYAN   (TeletextColor.CYAN,    0, Color.decode("#006666")),
    QUART_WHITE  (TeletextColor.WHITE,   0, Color.decode("#666666"));

    fun getSetColorCommand(): Byte {
        return when (teletextColor) {
            TeletextColor.BLACK   -> parity(0x20).toByte()
            TeletextColor.RED     -> parity(0x10 + TeletextColor.RED    .ordinal).toByte()
            TeletextColor.GREEN   -> parity(0x10 + TeletextColor.GREEN  .ordinal).toByte()
            TeletextColor.YELLOW  -> parity(0x10 + TeletextColor.YELLOW .ordinal).toByte()
            TeletextColor.BLUE    -> parity(0x10 + TeletextColor.BLUE   .ordinal).toByte()
            TeletextColor.MAGENTA -> parity(0x10 + TeletextColor.MAGENTA.ordinal).toByte()
            TeletextColor.CYAN    -> parity(0x10 + TeletextColor.CYAN   .ordinal).toByte()
            TeletextColor.WHITE   -> parity(0x10 + TeletextColor.WHITE  .ordinal).toByte()
        }
    }

    fun getPixelChar(): Byte {
        if (teletextColor == TeletextColor.BLACK) return parity(0x20).toByte()
        return when (brightness) {
            0 -> parity(0x28).toByte()
            1 -> parity(0x39).toByte()
            else -> parity(0x7f).toByte()
        }
    }

    companion object {
        fun byBrightness(rgb: Int): PLASMA_COLOR {
            var bright1 = (rgb and 0x00f000) shr 12 // 0..15
            var bright2 = (rgb and 0x0000f0) shr  4 // 0..15
            bright1 = (bright1+bright2) / 4
            return values()[bright1]
        }
        fun closest(rgb: Int): PLASMA_COLOR {
            val dists = values().map {
                var e1r = (rgb and 0xff0000) shr 16
                var e1g = (rgb and 0x00ff00) shr 8
                var e1b = (rgb and 0x0000ff)
                val rmean = (e1r + it.drawColor.red) / 2.0
                val r = e1r - it.drawColor.red
                val g = e1g - it.drawColor.green
                val b = e1b - it.drawColor.blue
                Math.sqrt((((512.0 + rmean) * r * r) / 256.0) + 4.0 * g * g + (((767.0 - rmean) * b * b) / 256.0))
            }
            var best = Double.MAX_VALUE
            var bestIndex = 0
            dists.forEachIndexed() { index, dist ->
                if (dist < best) {
                    best = dist
                    bestIndex = index
                }
            }
            return values()[bestIndex]
        }
    }
}

val SineRND = Random(0x1337)

data class PointXY(val x: Int, val y: Int)

class SineMover(var xAmp: Double,
                var yAmp: Double,
                var xSpeed: Double,
                var ySpeed: Double,
                var xPos: Double = SineRND.nextDouble() * 2.0 * PI,
                var yPos: Double = SineRND.nextDouble() * 2.0 * PI
) {
    init {
        xAmp *= PLASMA_SCALE_UP_FACTOR
        yAmp *= PLASMA_SCALE_UP_FACTOR
    }
    var successor: SineMover? = null
    var x = 0.0
    var y = 0.0
    fun tick() {
        x = Math.sin(xPos) * xAmp
        y = Math.sin(yPos) * yAmp
        successor?.let {
            it.tick()
            x += it.x
            y += it.y
        }
        xPos += xSpeed/100.0
        yPos += ySpeed/100.0
    }
    fun chain(other: SineMover): SineMover {
        successor = other
        return this
    }
}

interface MovingThing {
    fun tick(elapsed: Double)
    fun draw(g: Graphics2D)
}

class MovingDot(val img: BufferedImage, val center: PointXY, val mover: SineMover): MovingThing {
    override fun tick(elapsed: Double) = mover.tick()
    override fun draw(g: Graphics2D) {
        val x = center.x + Math.round(mover.x - img.width/2).toInt()
        val y = center.y + Math.round(mover.y - img.height/2).toInt()
        g.drawImage(img, x, y, null)
    }
}

class DotFromCenter(val img: BufferedImage, val speed: Double): MovingThing {
    var pos = 0.0
    var angle = 0.0
    var x = 0.0
    var y = 0.0
    var currentSpeed = speed
    var lastStart = 0.0
    var angleSkew = 0.0

    val centerX = PLASMA_W/2
    val centerY = PLASMA_H/2
    val maxLen = PLASMA_W

    init {
        restart(0.0)
    }

    private fun restart(elapsed: Double) {
        pos = 0.0
        angle = SineRND.nextDouble(2.0 * PI)
        currentSpeed = speed * SineRND.nextDouble(0.8, 1.2)
        lastStart = elapsed
        x = 0.0
        y = 0.0
        angleSkew = SineRND.nextDouble(-0.5, 0.5)
    }

    override fun tick(elapsed: Double) {
        val dotElapsed = elapsed - lastStart
        pos = dotElapsed * currentSpeed
        if (pos > 1.0) {
            restart(elapsed)
        }
        x = centerX.toDouble() + sin(angle + angleSkew * dotElapsed) * pos * maxLen.toDouble()
        y = centerY.toDouble() + cos(angle + angleSkew * dotElapsed) * pos * maxLen.toDouble()
    }

    override fun draw(g: Graphics2D) {
        val sizeW = lerp(pos, 10.0 * PLASMA_SCALE_UP_FACTOR, img.width.toDouble() * 1.2)
        val sizeH = sizeW * 2.0 / 3.0
        val drawX = x-sizeW/2.0
        val drawY = y-sizeH/2.0
        g.drawImage(img, drawX.roundToInt(), drawY.roundToInt(), sizeW.roundToInt(), sizeH.roundToInt(), null)
    }

}

class PlasmaRenderer(var dots: List<MovingThing>, var useBrightness: Boolean = false) {
    val canvasScaledUp = BufferedImage(PLASMA_W, PLASMA_H, DataBuffer.TYPE_INT)
    val g = canvasScaledUp.createGraphics()
    val page = TeletextPage()
    var overlayRenderBeforeRastering: ((g: Graphics2D, w: Int, h: Int) -> Unit)? = null
    fun tick(elapsed: Double) {
        dots.forEach { it.tick(elapsed) }
    }
    fun paint(): BufferedImage {
        g.color = Color.BLACK
        g.fillRect(0, 0, canvasScaledUp.width, canvasScaledUp.height)
        val oldComposite = g.composite
        g.composite = BlendComposite.Screen
        dots.forEach { it.draw(g) }
        g.composite = oldComposite
        overlayRenderBeforeRastering?.invoke(g, canvasScaledUp.width, canvasScaledUp.height)
        val canvas = scaleDown(canvasScaledUp, PLASMA_SCALE_UP_FACTOR)
        for (y in 0 until canvas.height) {
            page.set(0, y, parity(0x1e).toByte()) // hold graphics
            var lastPlasmaCol: PLASMA_COLOR? = null
            for (x in 0 until canvas.width) {

                // leave out " P 100  " area
                if (y == 0 && x < 8) {
                    if (x == 7) page.set(7, y, parity(0x1e).toByte()) // hold graphics
                    continue
                }

                val rgb = canvas.getRGB(x, y)
                val plasmaCol = if (useBrightness) PLASMA_COLOR.byBrightness(rgb) else PLASMA_COLOR.closest(rgb)
                // set color change or pixel pattern
                if (lastPlasmaCol?.teletextColor != plasmaCol.teletextColor) {
                    page.set(x+1, y, plasmaCol.getSetColorCommand())
                } else {
                    page.set(x+1, y, plasmaCol.getPixelChar())
                }
                // canvas.setRGB(x, y, plasmaCol.drawColor.rgb)
                lastPlasmaCol = plasmaCol
            }
        }
        return canvas
    }

    private fun scaleDown(img: BufferedImage, factor: Int): BufferedImage {
        val newW = img.width/factor
        val newH = img.height/factor
        val newImg = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
        val g = newImg.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(img, 0, 0, newW, newH, null)
        g.dispose()
        return newImg
    }
}

fun extractDotImages(filename: String): List<BufferedImage> {
    val srcImage = ImageIO.read(File(filename))
    val image = BufferedImage(srcImage.width, srcImage.height, DataBuffer.TYPE_INT)
    image.createGraphics().drawImage(srcImage, 0, 0, null)
    val size = image.height
    var x = 0
    val dots = mutableListOf<BufferedImage>()
    while (x + size <= image.width) {
        val image = image.getSubimage(x, 0, size, size)
        require(image != null) { "$filename: subimage $x,0 is null?!" }
        dots.add(image)
        x += size
    }
    require(!dots.isEmpty()) { "no dots were found in $filename" }
    return dots
}

fun main() {
    val dots = extractDotImages("gfx/colordots.png")
    val center = PointXY(PLASMA_W/2, PLASMA_H/2)
    val movingDots = listOf(
        MovingDot(dots[0], center, SineMover(20.0, 25.3, 3.4, 5.0).chain(SineMover(19.0, 5.0, 6.4, 12.5))),
        MovingDot(dots[0], center, SineMover(12.0, 17.0, 6.5, 4.7).chain(SineMover(17.3, 8.4, 5.7, 9.1))),
        MovingDot(dots[2], center, SineMover(30.0, 13.5, 4.8, 2.7)),
        MovingDot(dots[2], center, SineMover(13.0, 21.5, 4.8, 4.7)),
        MovingDot(dots[4], center, SineMover(12.0, 11.0, 3.3, 1.9).chain(SineMover(11.0, 9.0, 6.1, 4.5))),
        MovingDot(dots[4], center, SineMover(21.0, 24.0, 6.3, 3.9)),
    )
    val renderer = PlasmaRenderer(movingDots)
    val imagePanel = ImagePanel(renderer.canvasScaledUp, 4)
    showInFrame("moving dots", imagePanel)
    val font = getTeletextFont()
    val displayPanel = TeletextDisplayPanel(font)
    showInFrame("plasmer preview", displayPanel)
    while (true) {
        renderer.tick(0.0)
        renderer.paint()
        imagePanel.repaint()
        displayPanel.render(renderer.page.data)
        Thread.sleep(30)
    }
}

