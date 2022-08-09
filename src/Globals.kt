import java.lang.Math.pow
import kotlin.math.round

// Constants and global vals

const val GFXBUFFER_W = 80-2  // 39 cols
const val GFXBUFFER_H = 72    // 24 rows

const val PACKET_LEN = 42
const val PAGE_COLS = 40
const val PAGE_ROWS = 24

enum class TeletextColor(val rgb: Int) {
    BLACK(0x000000),
    RED(0xFF0000),
    GREEN(0x00FF00),
    YELLOW(0xFFFF00),
    BLUE(0xF0000F),
    MAGENTA(0xFF00FF),
    CYAN(0x00FFFF),
    WHITE(0xFFFFFF);
    fun applyFadeToWhite(fade: Double): TeletextColor {
        val brightness = COLORS_SORTED_BY_BRIGHTNESS.indexOf(this)
        var newColorIndex = round(lerp(fade, brightness.toDouble(), (COLORS_SORTED_BY_BRIGHTNESS.size - 1).toDouble())).toInt()
        if (newColorIndex < 0) newColorIndex = 0
        if (newColorIndex > COLORS_SORTED_BY_BRIGHTNESS.size - 1) newColorIndex = COLORS_SORTED_BY_BRIGHTNESS.size - 1
        return COLORS_SORTED_BY_BRIGHTNESS[newColorIndex]
    }
}

val COLORS_SORTED_BY_BRIGHTNESS = listOf(
    TeletextColor.BLACK,
    TeletextColor.BLUE,
    TeletextColor.RED,
    TeletextColor.MAGENTA,
    TeletextColor.GREEN,
    TeletextColor.CYAN,
    TeletextColor.YELLOW,
    TeletextColor.WHITE,
)

val RAINBOW_COLORS = listOf(
    TeletextColor.RED,
    TeletextColor.YELLOW,
    TeletextColor.GREEN,
    TeletextColor.CYAN,
    TeletextColor.BLUE,
    TeletextColor.MAGENTA,
)

const val GFX_RED = 0x11.toByte()
const val GFX_YEL = 0x13.toByte()
const val GFX_GRN = 0x12.toByte()
const val TXT_BLUE = 0x04.toByte()
const val BOX3 = 0x7f.toByte()
const val BOX2 = 0x7c.toByte()
const val BOX1 = 0x70.toByte()

// Extensions and global helpers

fun Int.clamp(min: Int, max: Int): Int {
    if (this < min) return min
    if (this > max) return max
    return this
}
fun Double.clamp(min: Double, max: Double): Double {
    if (this < min) return min
    if (this > max) return max
    return this
}

fun binformat(i: Int, w: Int) = "%${w}s".format(i.toString(2)).replace(' ', '0')

val textPage = listOf(
    "12345678AttentionText           13:37:00", //  Header Y= 0
    "0123456789012345678901234567890123456789", //         Y= 1
    "0                                      9", //         Y= 2
    "0 234567890123456789012345678901234567 9", //         Y= 3
    "0 234567890123456789012345678901234567 9", //         Y= 4
    "0 234567890123456789012345678901234567 9", //         Y= 5
    "0 234567890123456789012345678901234567 9", //         Y= 6
    "0 234567890123456789012345678901234567 9", //         Y= 7
    "0 234567890123456789012345678901234567 9", //         Y= 8
    "0 234567890123456789012345678901234567 9", //         Y= 9
    "0 234567890123456789012345678901234567 9", //         Y=10
    "0 234567890123456789012345678901234567 9", //         Y=11
    "0 234567890123456789012345678901234567 9", //         Y=12
    "0 234567890123456789012345678901234567 9", //         Y=13
    "0 234567890123456789012345678901234567 9", //         Y=14
    "0 234567890123456789012345678901234567 9", //         Y=15
    "0 234567890123456789012345678901234567 9", //         Y=16
    "0 234567890123456789012345678901234567 9", //         Y=17
    "0 234567890123456789012345678901234567 9", //         Y=18
    "0 234567890123456789012345678901234567 9", //         Y=19
    "0 234567890123456789012345678901234567 9", //         Y=20
    "0 234567890123456789012345678901234567 9", //         Y=21
    "0                                      9", //         Y=22
    "0123456789012345678901234567890123456789", //         Y=23
)

fun lerp(T: Double, a: Double, b: Double): Double = 1.0 * a + (b - a) * T
fun easeInQuad(T: Double, a: Double, b: Double) = lerp(T * T, a, b)
fun easeOutQuad(T: Double, a: Double, b: Double) = lerp(1.0 - (1.0 - T) * (1.0 - T), a, b)
fun easeInCubic(T: Double, a: Double, b: Double) = lerp(T * T * T, a, b)
fun easeOutBack(x: Double, a: Double, b: Double): Double {
    val c1 = 1.70158;
    val c3 = c1 + 1.0
    val T = 1.0 + c3 * pow(x - 1.0, 3.0) + c1 * pow(x - 1.0, 2.0)
    return lerp(T, a, b)
}

fun transLerp(T: Double, start: Double, end: Double, minDefault: Double?, maxDefault: Double?): Double? {
    if (T <= start) return minDefault
    if (T > end) return maxDefault
    return ((T - start)/(end - start)).coerceIn(0.0, 1.0)
}

fun transLerp(T: Double, start: Double, end: Double): Double {
    if (T <= start) return 0.0
    if (T > end) return 1.0
    return ((T - start)/(end - start)).coerceIn(0.0, 1.0)
}

fun measureMillis(name: String, block: () -> Unit) {
    val start = System.currentTimeMillis()
    block.invoke()
    val millis =  System.currentTimeMillis() - start
    println("$name: $millis ms")
}

var lastMeasureStart = 0L
fun measureStart() { lastMeasureStart = System.currentTimeMillis() }
fun measureReport(name: String) { println("$name: ${System.currentTimeMillis() - lastMeasureStart} ms") }
