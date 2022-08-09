import java.awt.Color
import java.awt.image.BufferedImage

// snatched from https://stackoverflow.com/questions/65953579/how-to-implement-1-bit-dithering-using-java

fun dither(image: BufferedImage): BufferedImage {
    val palette: Array<Color3i> = arrayOf<Color3i>(
        Color3i(0, 0, 0),
        Color3i(255, 255, 255)
    )
    val width = image.width
    val height = image.height
    val buffer: Array<Array<Color3i?>> = Array<Array<Color3i?>>(height) {
        arrayOfNulls<Color3i>(
            width
        )
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            buffer[y][x] = Color3i(image.getRGB(x, y))
        }
    }
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val old: Color3i = buffer[y][x] ?: Color3i(0)
            val nem: Color3i = findClosestPaletteColor(old, palette)
            image.setRGB(x, y, nem.toColor().getRGB())
            val error: Color3i = old.sub(nem)
            if (x + 1 < width) buffer[y][x + 1] = buffer[y][x + 1]?.add(error.mul(7.0 / 16))
            if (x - 1 >= 0 && y + 1 < height) buffer[y + 1][x - 1] = buffer[y + 1][x - 1]?.add(error.mul(3.0 / 16))
            if (y + 1 < height) buffer[y + 1][x] = buffer[y + 1][x]?.add(error.mul(5.0 / 16))
            if (x + 1 < width && y + 1 < height) buffer[y + 1][x + 1] = buffer[y + 1][x + 1]?.add(error.mul(1.0 / 16))
        }
    }
    return image
}

private fun findClosestPaletteColor(match: Color3i, palette: Array<Color3i>): Color3i {
    var closest: Color3i = palette[0]
    for (color in palette) {
        if (color.diff(match) < closest.diff(match)) {
            closest = color
        }
    }
    return closest
}

internal class Color3i {
    private var r: Int
    private var g: Int
    private var b: Int

    constructor(c: Int) {
        val color = Color(c)
        r = color.red
        g = color.green
        b = color.blue
    }

    constructor(r: Int, g: Int, b: Int) {
        this.r = r
        this.g = g
        this.b = b
    }

    fun add(o: Color3i): Color3i {
        return Color3i(r + o.r, g + o.g, b + o.b)
    }

    fun sub(o: Color3i): Color3i {
        return Color3i(r - o.r, g - o.g, b - o.b)
    }

    fun mul(d: Double): Color3i {
        return Color3i((d * r).toInt(), (d * g).toInt(), (d * b).toInt())
    }

    fun diff(o: Color3i): Int {
        val Rdiff = o.r - r
        val Gdiff = o.g - g
        val Bdiff = o.b - b
        return Rdiff * Rdiff + Gdiff * Gdiff + Bdiff * Bdiff
    }

    fun toRGB(): Int {
        return toColor().rgb
    }

    fun toColor(): Color {
        return Color(clamp(r), clamp(g), clamp(b))
    }

    fun clamp(c: Int): Int {
        return Math.max(0, Math.min(255, c))
    }
}