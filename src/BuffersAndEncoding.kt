import java.io.File
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

// Format Teletext packets and pages

fun insertPageHeader(buf: ByteArray, pos: Int, pageNumber: Int, subPage: Int, vararg flags: PageHeaderControlBit) {
    val pageDigit1 = (pageNumber and 0b1111_0000_0000) shr 8
    val pageDigit2 = (pageNumber and 0b0000_1111_0000) shr 4
    val pageDigit3 = (pageNumber and 0b0000_0000_1111)
    val magazine = pageDigit1
    val mpag = encodeMpag(magazine, 0)
    buf[pos+0] = mpag[0].toByte()
    buf[pos+1] = mpag[1].toByte()

    buf[pos+2] = ham(pageDigit3).toByte()
    buf[pos+3] = ham(pageDigit2).toByte()
    var s1 = (subPage and 0b0000_0000_0000_1111)
    var s2 = (subPage and 0b0000_0000_0111_0000) shr 4
    var s3 = (subPage and 0b0000_1111_0000_0000) shr 8
    var s4 = (subPage and 0b0011_0000_0000_0000) shr 12
    var ctrl1 = 0
    var ctrl2 = 0

    // set flags
    if (flags.contains(PageHeaderControlBit.C4_ERASE_PAGE))           s2    = s2    or 0b1000

    if (flags.contains(PageHeaderControlBit.C5_NEWSFLASH))            s4    = s4    or 0b0100
    if (flags.contains(PageHeaderControlBit.C6_SUBTITLE))             s4    = s4    or 0b1000

    if (flags.contains(PageHeaderControlBit.C7_SUPPRESS_HEADER))      ctrl1 = ctrl1 or 0b0001
    if (flags.contains(PageHeaderControlBit.C8_UPDATE_INDICATOR))     ctrl1 = ctrl1 or 0b0010
    if (flags.contains(PageHeaderControlBit.C9_INTERRUPTED_SEQUENCE)) ctrl1 = ctrl1 or 0b0100
    if (flags.contains(PageHeaderControlBit.C10_INHIBIT_DISPLAY))     ctrl1 = ctrl1 or 0b1000

    if (flags.contains(PageHeaderControlBit.C11_MAGAZINE_SERIAL))     ctrl2 = ctrl2 or 0b0001

    // sub-page
    buf[pos+4] = ham(s1).toByte()
    buf[pos+5] = ham(s2).toByte()
    buf[pos+6] = ham(s3).toByte()
    buf[pos+7] = ham(s4).toByte()
    // flags
    buf[pos+8] = ham(ctrl1).toByte()
    buf[pos+9] = ham(ctrl2).toByte()
}

fun formatAsPage(source: ByteArray, pageNumber: Int, buf: ByteArray = ByteArray(PACKET_LEN * PAGE_ROWS) { 0x20 }): ByteArray {
    val pageDigit1 = (pageNumber and 0b1111_0000_0000) shr 8
    val pageDigit2 = (pageNumber and 0b0000_1111_0000) shr 4
    val pageDigit3 = (pageNumber and 0b0000_0000_1111)
    val magazine = pageDigit1
    var sourcePos = 0
    for (y in 0 until PAGE_ROWS) {
        val rawLineStart = PACKET_LEN*y
        val mpag = encodeMpag(magazine, y)
        buf[rawLineStart+0] = mpag[0].toByte()
        buf[rawLineStart+1] = mpag[1].toByte()

        for (i in 0 until PAGE_COLS) {
            buf[rawLineStart+2+i] = parity(source[sourcePos++].toInt()).toByte()
        }

        // Special case page header
        if (y == 0) {
            buf[rawLineStart+2] = ham(pageDigit3).toByte()
            buf[rawLineStart+3] = ham(pageDigit2).toByte()
            // sub-page
            buf[rawLineStart+4] = 0x15.toByte()
            buf[rawLineStart+5] = 0x15.toByte()
            buf[rawLineStart+6] = 0x15.toByte()
            buf[rawLineStart+7] = 0x15.toByte()
            // flags
            buf[rawLineStart+8] = 0x15.toByte()
            buf[rawLineStart+9] = 0x15.toByte()
        }
    }
    return buf
}


fun formatAsPage(textLines: List<String>, pageNumber: Int, buf: ByteArray = ByteArray(PACKET_LEN * PAGE_ROWS) { 0x20 }): ByteArray {
    val pageDigit1 = (pageNumber and 0b1111_0000_0000) shr 8
    val pageDigit2 = (pageNumber and 0b0000_1111_0000) shr 4
    val pageDigit3 = (pageNumber and 0b0000_0000_1111)
    val magazine = pageDigit1
    for (y in 0 until textLines.size) {
        val rawLineStart = PACKET_LEN*y
        val mpag = encodeMpag(magazine, y)
        buf[rawLineStart+0] = mpag[0].toByte()
        buf[rawLineStart+1] = mpag[1].toByte()

        textLines[y].toCharArray().forEachIndexed { i, ch ->
            buf[rawLineStart+2+i] = parity(ch.code).toByte()
        }

        // Special case page header
        if (y == 0) {
            buf[rawLineStart+2] = ham(pageDigit3).toByte()
            buf[rawLineStart+3] = ham(pageDigit2).toByte()
            // sub-page
            buf[rawLineStart+4] = 0x15.toByte()
            buf[rawLineStart+5] = 0x15.toByte()
            buf[rawLineStart+6] = 0x15.toByte()
            buf[rawLineStart+7] = 0x15.toByte()
            // flags
            buf[rawLineStart+8] = 0x15.toByte()
            buf[rawLineStart+9] = 0x15.toByte()
        }
    }
    return buf
}

fun encodeMpag(magazine: Int, y: Int): Array<Int> {
    val byteValue = magazine or (y shl 3)
    val nibble1 = (byteValue and 0b00001111)
    val nibble2 = (byteValue and 0b11110000) shr 4
    return arrayOf(ham(nibble1), ham(nibble2))
}

// Teletext packet encoding/decoding

fun unham(b:Int): Int {
    val d1 = if (b and 0b00000010 == 0) 0 else 0b0001
    val d2 = if (b and 0b00001000 == 0) 0 else 0b0010
    val d3 = if (b and 0b00100000 == 0) 0 else 0b0100
    val d4 = if (b and 0b10000000 == 0) 0 else 0b1000
    return d1 or d2 or d3 or d4
}

fun ham(d:Int): Int {
    val d1 = d and 1
    val d2 = d shr 1 and 1
    val d3 = d shr 2 and 1
    val d4 = d shr 3 and 1

    val p1 = 1 + d1 + d3 + d4 and 1
    val p2 = 1 + d1 + d2 + d4 and 1
    val p3 = 1 + d1 + d2 + d3 and 1
    val p4 = 1 + p1 + d1 + p2 + d2 + p3 + d3 + d4 and 1

    return p1 or
            (d1 shl 1) or
            (p2 shl 2) or
            (d2 shl 3) or
            (p3 shl 4) or
            (d3 shl 5) or
            (p4 shl 6) or
            (d4 shl 7)

}

fun parity(d: Int): Int {
    var d: Int = d
    d = d and 0x7f
    var p: Int = 1
    var t: Int = d
    var i: Int
    i = 0
    while (i < 7) {
        p += t and 1
        t = t shr 1
        i++
    }
    p = p and 1
    return d or (p shl 7)
}

// Messing with Teletext buffers

enum class PageHeaderControlBit {
    C4_ERASE_PAGE,
    C5_NEWSFLASH,
    C6_SUBTITLE,
    C7_SUPPRESS_HEADER,
    C8_UPDATE_INDICATOR,
    C9_INTERRUPTED_SEQUENCE,
    C10_INHIBIT_DISPLAY,
    C11_MAGAZINE_SERIAL
}


fun writeIntoBuffer(buf: ByteArray, x: Int, y: Int, s: String) {
    writeIntoBuffer(buf, x, y, s.encodeToByteArray().asList())
}
fun writeIntoBuffer(buf: ByteArray, x: Int, y: Int, bytes: List<Byte>) {
    val raw = buf.size % PACKET_LEN == 0
    val stride = if (raw) PACKET_LEN else PAGE_COLS
    val off = if (raw) 2 else 0
    var pos = y * stride + x + off
    for (element in bytes) {
        buf[pos++] = parity(element.toInt()).toByte()
    }
}


// Telext "sprites"

open class TeletextArea(val w: Int, val h: Int, val data: ByteArray = ByteArray(w * h) { 0x20 }) {
    init {
        require(data.size == w * h) { "data has invalid size (expected: $w * $h = ${w*h}, actual: ${data.size}" }
    }
    fun get(x: Int, y: Int): Byte {
        require(x >= 0 && y >= 0 && x < w && y < h) { "get($x,$y): out of bounds ($w*$h)" }
        return data[y*w + x] and 0x7f.toByte()
    }
    fun set(x: Int, y: Int, value: Byte) {
        require(x >= 0 && y >= 0 && x < w && y < h) { "set($x,$y,$value): out of bounds ($w*$h)" }
        data[y*w + x] = value
    }
    fun set(x: Int, y: Int, value: Int) = set(x, y, value.toByte())
    fun copyRect(srcX: Int, srcY: Int, w: Int, h: Int): TeletextArea? {
        val area = TeletextArea(w, h)
        require(srcX >= 0 && srcY >= 0 && srcX+w-1 < this.w && srcY+h-1 < this.h) { "sub-area $srcX,$srcY..${srcX+w-1},${srcY+h-1} is out of bounds ($this)" }
        for (y in 0 until h) {
            for (x in 0 until w) {
                area.set(x, y, this.get(srcX+x, srcY+y))
            }
        }
        return area
    }

    override fun toString() = "TeletextArea($w*$h)"

    fun drawSprite(other: TeletextArea, dstX: Int, dstY: Int) {
        var drawY = dstY
        for (y in 0 until other.h) {
            if (drawY in 0 until h) {
                var drawX = dstX
                for (x in 0 until other.w) {
                    if (drawX in 0 until w) {
                        set(drawX, drawY, other.get(x, y))
                    }
                    drawX++
                }
            }
            drawY++
        }
    }

    fun replace(from: Int, to: Int): TeletextArea {
        val newData = data.clone()
        newData.indices.forEach { if (newData[it] and 0x7f == from.toByte()) newData[it] = to.toByte() }
        return TeletextArea(w, h, newData)
    }

    fun replaceInline(from: Int, to: Int) {
        data.indices.forEach {  if (data[it] and 0x7f == from.toByte()) data[it] = to.toByte() }
    }

    fun takeDataFrom(buf: ByteArray) {
        require(buf.size >= data.size) { "source buffer (${buf.size}) needs to be >= ${data.size}" }
        data.forEachIndexed { index, byte -> data[index] = buf[index] }
    }

    fun drawString(x: Int, y: Int, s: String) {
        s.indices.filter { x+it < w }.forEach { set(x+it, y, s[it].code.toByte()) }
    }

    fun applyFadeToBlack(fade: Double) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val byte   = get(x, y)
                if (!isColorSetter(byte)) continue

                val col    = byte.toInt() and 0b0000_0111
                val gfxBit = byte.toInt() and 0b0001_0000

                val brightness = COLORS_SORTED_BY_BRIGHTNESS.indexOf(TeletextColor.values()[col])
                val fadedBrightness = (brightness.toDouble() * fade).toInt().coerceAtMost(COLORS_SORTED_BY_BRIGHTNESS.size-1)
                val newColor = COLORS_SORTED_BY_BRIGHTNESS[fadedBrightness]
                val newByte = (newColor.ordinal or gfxBit).toByte()
                set(x, y, newByte)
            }
        }
    }
    fun applyFadeToWhite(fade: Double) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val byte   = get(x, y)
                if (!isColorSetter(byte)) continue

                val col    = byte.toInt() and 0b0000_0111
                val gfxBit = byte.toInt() and 0b0001_0000

                val brightness = COLORS_SORTED_BY_BRIGHTNESS.indexOf(TeletextColor.values()[col])
                var newColorIndex = lerp(fade, brightness.toDouble(), (COLORS_SORTED_BY_BRIGHTNESS.size - 1).toDouble()).toInt()
                if (newColorIndex < 0) newColorIndex = 0
                if (newColorIndex > COLORS_SORTED_BY_BRIGHTNESS.size - 1) newColorIndex = COLORS_SORTED_BY_BRIGHTNESS.size - 1
                val newColor = COLORS_SORTED_BY_BRIGHTNESS[newColorIndex]
                val newByte = (newColor.ordinal or gfxBit).toByte()
                set(x, y, newByte)
            }
        }
    }
    fun applyClippingFadeToWhite(fade: Double) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val byte   = get(x, y)
                if (!isColorSetter(byte)) continue

                val col    = byte.toInt() and 0b0000_0111
                val gfxBit = byte.toInt() and 0b0001_0000

                val brightness = COLORS_SORTED_BY_BRIGHTNESS.indexOf(TeletextColor.values()[col])
                val maxBrightness = ((COLORS_SORTED_BY_BRIGHTNESS.size-1) * fade).roundToInt().coerceIn(0, COLORS_SORTED_BY_BRIGHTNESS.size-1)
                val newBrightness = max(brightness, maxBrightness)
                val newColor = COLORS_SORTED_BY_BRIGHTNESS[newBrightness]
                val newByte = (newColor.ordinal or gfxBit).toByte()
                set(x, y, newByte)
            }
        }
    }
    fun applyClippingFadeToBlack(fade: Double, protected: List<PointXY>? = null) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (protected?.contains(PointXY(x, y)) == true) continue
                val byte   = get(x, y)
                if (!isColorSetter(byte)) continue

                val col    = byte.toInt() and 0b0000_0111
                val gfxBit = byte.toInt() and 0b0001_0000

                val brightness = COLORS_SORTED_BY_BRIGHTNESS.indexOf(TeletextColor.values()[col])
                var fadedBrightness = (brightness.toDouble() * fade).toInt()
                if (fadedBrightness < 0) fadedBrightness = 0
                val newColor = COLORS_SORTED_BY_BRIGHTNESS[min(brightness, fadedBrightness)]
                val newByte = (newColor.ordinal or gfxBit).toByte()
                set(x, y, newByte)
            }
        }
    }

    fun fill(ch: Int) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                set(x, y, ch)
            }
        }
    }
}

data class TeletextState(val color: Int, val splitGraphics: Boolean)

class TeletextPage(data: ByteArray = ByteArray(PAGE_COLS * PAGE_ROWS) { 0x20 }): TeletextArea(PAGE_COLS, PAGE_ROWS, data) {
    fun getLineStates(y: Int): List<TeletextState> {
        var color = 7
        var splitGraphics = false
        val ret = mutableListOf<TeletextState>()
        for (x in 0 until w) {
            when (data[y*w+x].toInt() and 0x7f) {
                0x00, 0x10 -> color = 0
                0x01, 0x11 -> color = 1
                0x02, 0x12 -> color = 2
                0x03, 0x13 -> color = 3
                0x04, 0x14 -> color = 4
                0x05, 0x15 -> color = 5
                0x06, 0x16 -> color = 6
                0x07, 0x17 -> color = 7
                0x19 -> splitGraphics = false
                0x1a -> splitGraphics = true
            }
            ret += TeletextState(color, splitGraphics)
        }
        return ret
    }

    companion object {
        fun read(filename: String) = read(File(filename))
        fun read(file: File): TeletextPage {
            require(file.length().toInt() >= PAGE_COLS * PAGE_ROWS) { "$file is too small (expected: ${PAGE_COLS * PAGE_ROWS}, actual: ${file.length()}" }
            return TeletextPage(file.readBytes().copyOf(PAGE_COLS * PAGE_ROWS))
        }
    }
}

data class TeletextSprite(val rows: List<List<Byte>>)

// Extract graphical regions of specific color

data class LineRegion(val row: Int, val cols: MutableList<Int> = mutableListOf())

data class FrameRegions(val lineRegions: MutableList<LineRegion> = mutableListOf())

fun extractGraphicRegions(buf: ByteArray, color: Int): FrameRegions {
    val ret = FrameRegions()
    for (y in 1 until 24) {
        var current: LineRegion? = null
        for (x in 0 until 40) {
            val char = buf[y*40 + x].toInt() and 0x7f
            if (current != null) {
                if (isGraphicData(char)) {
                    current.cols.add(x)
                } else {
                    if (!current.cols.isEmpty()) {
                        ret.lineRegions.add(current)
                    }
                    current = null
                }
            } else {
                if (char == 0x10 or color) {
                    current = LineRegion(y)
                }
            }
        }
        if (current != null) ret.lineRegions.add(current)
    }
    // ret.lineRegions.forEach { println(it) }
    return ret
}

fun isGraphicData(char: Int) = (char and 0x7f) >= 0x20
fun isGraphicData(char: Byte) = isGraphicData(char.toInt())

fun isColorSetter(ch: Byte): Boolean = (ch and 0x7f) in 0x00..0x07 || (ch and 0x7f) in 0x10..0x17

// Old encoding test

fun main() {
    val x = intArrayOf(0x20,0x57,0xe5,0x64,0x20,0x31,0x31,0x20,0x46,0xe5,0x62,0x20,0x31,0xb9,0xba,0xb0,0xb9,0x2f,0x32,0x34)
    print("'")
    for (i in x) {
        val c = i and 0x7f
        print(c.toChar())
    }
    println("'")

    for (i in 0..15) {
        val ham = ham(i)
        val un = unham(ham)
        println("ham check: %x - %02x - %x".format(i, ham, un))
    }

    val mpags = intArrayOf(0x02,0x15,
        0xc7,0x15,
        0x02,0x02,
        0xc7,0x02,
        0x02,0x49,
        0xc7,0x49,
        0x02,0x5e,
        0xc7,0x5e,
        0x02,0x64,
        0xc7,0x64,
        0x02,0x73,
        0xc7,0x73,
        0x02,0x38,
        0xc7,0x38,
        0x02,0x2f,
        0xc7,0x2f,
        0x02,0xd0,
        0xc7,0xd0,
        0x02,0xc7,
        0xc7,0xc7,
        0x02,0x8c,
        0xc7,0x8c,
        0x02,0x9b,
        0xc7,0x9b)
    println("| raw | unham | MPAG | mag | adr | encodeMpag test ")
    println("|-----|-------|------|-----|-----|---|")
    for (i in mpags.indices step 2) {
        val b0 = unham(mpags[i])
        val b1 = unham(mpags[i+1])
        val mpag = (b1 shl 4) or b0
        val mag = (mpag and 0b00000111)
        val adr = (mpag and 0b11111000) shr 3
        val enc = encodeMpag(mag, adr)
        println("| %02x %02x | %01x %1x = %4s %4s | %02x = %8s | %2d | %2d | %02x %02x".format(mpags[i],mpags[i+1], b0, b1, binformat(b0,4), binformat(b1,4), mpag, binformat(mpag, 8), mag, adr, enc[0], enc[1]))
    }
}


