import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.lang.Math.*
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel

val nodes : ArrayList<ArrayList<Double>> = arrayListOf(
    arrayListOf(-1.0, -1.0, -1.0),
    arrayListOf(-1.0, -1.0,  1.0),
    arrayListOf(-1.0,  1.0, -1.0),
    arrayListOf(-1.0,  1.0,  1.0),
    arrayListOf( 1.0, -1.0, -1.0),
    arrayListOf( 1.0, -1.0,  1.0),
    arrayListOf( 1.0,  1.0, -1.0),
    arrayListOf( 1.0,  1.0,  1.0)
)
val edges : ArrayList<Pair<Int, Int>> = arrayListOf(
    Pair(0, 1),
    Pair(1, 3),
    Pair(3, 2),
    Pair(2, 0),
    Pair(4, 5),
    Pair(5, 7),
    Pair(7, 6),
    Pair(6, 4),
    Pair(0, 4),
    Pair(1, 5),
    Pair(2, 6),
    Pair(3, 7)
)

var cubeScale = 20.0

fun main() {
    val img = BufferedImage(GFXBUFFER_W, GFXBUFFER_H, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    val icon = ImageIcon(img)
    val label = JLabel(icon)
    val frame = JFrame("cubo")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane = label
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
    var angleA = 0.0
    var angleB = 1.0
    var t = 0.0
    while (true) {
        cubeScale = 10.0 * sin(t) + 25.0
        drawCube(g, angleA, angleB, cubeScale)
        icon.image = img
        label.repaint()
        angleA += 0.033
        angleB += 0.027
        t += 0.0212
        Thread.sleep(20L)
    }
}

fun drawCube(g: Graphics2D, angleX: Double, angleY : Double, scale: Double) {
    g.color = Color.BLACK
    g.fillRect(0, 0,  GFXBUFFER_W, GFXBUFFER_H)
    g.color = Color.WHITE

    val sinA = sin(angleX)
    val cosA = cos(angleX)
    val sinB = sin(angleY)
    val cosB = cos(angleY)

    val nodes2d = nodes.mapIndexed { i, node ->
        val x = node[0] * scale
        val y = node[1] * scale
        var z = node[2] * scale

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
    g.translate(GFXBUFFER_W / 2, GFXBUFFER_H / 2)
    for (edge in edges) {
        val xy1 = nodes2d[edge.first]
        val xy2 = nodes2d[edge.second]

        g.drawLine(
            round(xy1.first).toInt(), round(xy1.second).toInt(),
            round(xy2.first).toInt(), round(xy2.second).toInt()
        )
    }
    g.translate(-GFXBUFFER_W / 2, -GFXBUFFER_H / 2)
}
