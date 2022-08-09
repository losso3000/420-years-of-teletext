import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val filenamePattern = "gfx/guru-typ/frames/smaller-typ1_%05d.png"
    val frames = mutableListOf<BufferedImage>()
    var num = 1
    while(File(filenamePattern.format(num)).exists()) {
        frames += ImageIO.read(File(filenamePattern.format(num++)))
    }
    println("frames read: ${frames.size}")
    // 0,2+66,53
    clipAndThresh(frames, 0, 2, 66, 53, 120)
    frames.forEachIndexed { index, frame -> ImageIO.write(frame, "png", File("gfx/guru-typ/processed-frames/%03d.png".format(index))) }
    val imagePanel = ImagePanel(frames[0])
    showInFrame("roto", imagePanel)
    while (true) {
        num = (num + 1) % frames.size
        imagePanel.image = frames[num]
        imagePanel.repaint()
        Thread.sleep(20)
    }
}

fun clipAndThresh(frames: MutableList<BufferedImage>, left: Int, top: Int, w: Int, h: Int, thresh: Int) {
    frames.forEachIndexed { index, frame ->
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val green = (frame.getRGB(x+left, y+top) shr 16) and 0xff
                if (green >= thresh) image.setRGB(x, y, 0xff_ffffff.toInt())
            }
        }
        frames.set(index, image)
    }

}
