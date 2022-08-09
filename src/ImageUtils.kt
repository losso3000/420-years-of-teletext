import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

// Encode bitmpaps into Teletext characters

fun convertImage(img: BufferedImage,
                 emptyCellCharacter: Int = 0x20,
                 cols: Int = 39,
                 rows: Int = 24,
                 includeStartGfxCharacter: Int? = 0x17,
                 inverted: Boolean = false)
        : ByteArray {
    val out = ByteArrayOutputStream()
    for (row in 0 until rows) {
        includeStartGfxCharacter?.let { out.write(it) } // turn on graphics
        for (col in 0 until cols) {
            val b = encodeAt(img, col*2, row*3, emptyCellCharacter, inverted)
            out.write(b)
        }
    }
    return out.toByteArray()
}

// Teletext bitmap graphics
// first char = switch to gfx
// remaining 39 chars = data
// 1 char = 2*3 "pixels"
//
// Resulting resolution:
//
// 39*23 chars = 78*69 pixels
// 39*24 chars = 78*72 pixels (when using header line also)


// Control chars:
//
// 00 Nothing          10 Nothing
// 01 Alpha Red        11 Graphic Red
// 02 Alpha Green      12 Graphic Green
// 03 Alpha Yellow     13 Graphic Yellow
// 04 Alpha Blue       14 Graphic Blue
// 05 Alpha Magenta    15 Graphic Magenta
// 06 Alpha Cyan       16 Graphic Cyan
// 07 Alpha White *    17 Graphic White
// 08 Flash            18 Conceal Display
// 09 Steady *         19 Contiguous Graphics *
// 0A Nothing          1A Separated Graphics
// 0B Nothing          1B Nothing
// 0C Normal Height *  1C Black Background *
// 0D Double height    1D New Background
// 0E Nothing          1E Hold Graphics
// 0F Nothing          1F Release Graphics *

// Gfx encoding (for each 2*3 block)
//
// Bits in character:
//
//   0 1
//   2 3
//   4 5
//
// Upper two rows:
//
// |  00  | |  #0  | |  0#  | |  ##  | |  00  | |  #0  | |  0#  | |  ##  | |  00  |
// |  00  | |  00  | |  00  | |  00  | |  #0  | |  #0  | |  #0  | |  #0  | |  0#  |  etc.
// |  ..  | |  ..  | |  ..  | |  ..  | |  ..  | |  ..  | |  ..  | |  ..  | |  ..  |
// ....0000 ....0001 ....0010 ....0011 ....0100 ....0101 ....0110 ....0111 ....1000
//    .0       .1       .2       .3       .4       .5       .6       .7       .8
//
// Lower row:
//
// |  ..  | |  ..  | |  ..  | |  ..  |
// |  ..  | |  ..  | |  ..  | |  ..  |
// |  00  | |  #0  | |  0#  | |  ##  |
// 0000.... 0001.... 0010.... 0011....
//    A.       B.       E.       F.
//

fun encodeAt(img: BufferedImage, x: Int, y: Int, emptyCellCharacter: Int = 0x20, inverted: Boolean = false): Int {
    var upper = 0
    if (x+1 >= img.width || y+2 >= img.height) return emptyCellCharacter
    if (img.getRGB(x+0, y+0) and 0xff == 0) upper = upper or 0b0001
    if (img.getRGB(x+1, y+0) and 0xff == 0) upper = upper or 0b0010
    if (img.getRGB(x+0, y+1) and 0xff == 0) upper = upper or 0b0100
    if (img.getRGB(x+1, y+1) and 0xff == 0) upper = upper or 0b1000
    var lower = 0
    if (img.getRGB(x+0, y+2) and 0xff == 0) lower = lower or 0b01
    if (img.getRGB(x+1, y+2) and 0xff == 0) lower = lower or 0b10
    if (inverted) {
        upper = upper xor 0b1111
        lower = lower xor 0b11
    }
    if (upper == 0 && lower == 0) return emptyCellCharacter
    return when(lower) {
        0b00 -> 0xA0 or upper
        0b01 -> 0xB0 or upper
        0b10 -> 0xE0 or upper
        0b11 -> 0xF0 or upper
        else -> throw IllegalStateException("wat")
    }
}

