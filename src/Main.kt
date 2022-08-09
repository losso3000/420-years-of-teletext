import java.awt.BorderLayout
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

const val PADW = 10
const val PADH = 5

val fillingHeader = byteArrayOf(
    0x02, 0x15, // magazine 1 adr 0
    ham(0xf).toByte(), ham(0xf).toByte(),  // page "FF"
    0x15, 0x15, 0x15, 0x15,        // sub-page "0000"
    ham(0b0001).toByte(), 0x15, // flags: C7 Suppress header
    0x37, 0x37,
    0x37, 0x37, 0x37, 0x37, 0x37,
    0x37, 0x37, 0x37, 0x37, 0x37,
    0x37, 0x37, 0x37, 0x37, 0x37,
    0x37, 0x37, 0x37, 0x37, 0x37,
    0x37, 0x37,

    // last 8 bytes update the header clock portion (even with C7 "suppress header" set (?))

    parity(0x20).toByte(), parity(0x20).toByte(), parity(0x20).toByte(),
    parity(0x20).toByte(), parity(0x20).toByte(), parity(0x20).toByte(), parity(0x20).toByte(), parity(0x20).toByte(),
)

abstract class Effect(val name: String) {
    var lastElapsed: Double? = null
    var buf = formatAsPage(ByteArray(PAGE_COLS * PAGE_ROWS) { 0x20 }, 0x100)
    var ticks: Int = 0
    var startTime: Double = 0.0
    fun reset(referenceTime: Double = System.currentTimeMillis() / 1000.0) {
        ticks = 0
        startTime = referenceTime
        doReset()
    }
    fun tick(referenceTime: Double? = null): ByteArray {
        val elapsed: Double
        if (referenceTime != null) {
            elapsed = referenceTime - startTime
        } else {
            // hack: start cleanly with elapsed = 0.0 on first tick
            if (ticks == 0) {
                startTime = System.currentTimeMillis() / 1000.0
            }
            elapsed = (System.currentTimeMillis() / 1000.0) - startTime
        }
        doTick(ticks++, elapsed)
        lastElapsed = elapsed
        return buf
    }
    override fun toString() = name
    abstract fun doReset()
    abstract fun doTick(ticks: Int, elapsed: Double)
}

class PlaceholderEffect : Effect("Placeholder") {
    override fun doReset() = Unit

    override fun doTick(ticks: Int, elapsed: Double) {
        writeIntoBuffer(buf, 2, 2, "Ticks = %6d  ".format(ticks))
        writeIntoBuffer(buf, 2, 3, "Elaps = %6.3f ".format(elapsed))
    }
}

class SyncTestEffect : Effect("Sync test") {
    val circleR = 42
    val circleCols = circleR/2
    val circleRows = circleR/3
    val img = BufferedImage(circleR, circleR, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    val colors = listOf(TeletextColor.RED, TeletextColor.YELLOW, TeletextColor.GREEN, TeletextColor.WHITE, TeletextColor.BLUE, TeletextColor.MAGENTA, TeletextColor.CYAN)
    val bg = File("gfx/refresh-test.bin").readBytes()
    val page = TeletextPage()
    var lastLastElapsed: Double? = null
    val bleep = AudioOutput().prepareClip("sfx/blip.wav")
    override fun doReset() = Unit
    override fun doTick(ticks: Int, elapsed: Double) {
        page.takeDataFrom(bg)

        // draw circle
        val amp = circleR/2
        val cx = img.width/2
        val cy = img.height/2
        g.color = Color.WHITE
        g.fillRect(0, 0, img.width, img.height)
        g.color = Color.BLACK
        g.fillArc(0, 0, circleR-1, circleR-1, 0, 360)

        // draw line
        val toX = Math.round(amp * Math.sin(Math.PI * 2.0 * -(0.5 + elapsed % 1.0))).toInt()
        val toY = Math.round(amp * Math.cos(Math.PI * 2.0 * -(0.5 + elapsed % 1.0))).toInt()
        g.color = Color.WHITE
        g.drawLine(cx, cy, cx+toX, cy+toY)
        g.drawLine(cx+1, cy, cx+toX+1, cy+toY)
        g.drawLine(cx, cy+1, cx+toX, cy+toY+1)
        g.drawLine(cx+1, cy+1, cx+toX+1, cy+toY+1)

        // insert circle
        val cBuf = convertImage(img, 0x20, circleCols, circleRows, null, false)
        for (y in 0 until circleRows) {
            for (x in 0 until circleCols) {
                page.set(1+x, 6+y, cBuf[circleCols*y + x])
            }
        }

        // color change every second
        val col = colors[elapsed.toInt() % colors.size]
        for (y in 6..19) page.set(0, y, 0x10 or col.ordinal)

        // moving dot
        val dotPos = lerp(elapsed % 1.0, 2.0, 38.0).roundToInt()
        page.set(dotPos, 22, 0x7f)

        // current fps
        val fpsString: String
        val reference = lastLastElapsed
        if (reference == null) {
            fpsString = "?"
        } else {
            val elapsedSinceLastTick = elapsed - reference
            val fps = 2.0 / elapsedSinceLastTick
            fpsString = "%3.1f".format(fps)
        }
        page.drawString(35, 6, "%5s".format(fpsString))
        page.drawString(35, 7, "%5d".format(ticks))
        page.drawString(35, 8, "%5s".format("%3.1f".format(elapsed)))

        // second roll-over?
        val second = elapsed % 1.0
        val lastSecond = (lastElapsed ?: 0.9) % 1.0
        if (second < lastSecond) {
            bleep.microsecondPosition = 0L
            bleep.start()
        }

        lastLastElapsed = lastElapsed
        lastElapsed = elapsed

        // chars
        var code = (ticks % 0x60) + 0x20
        for (x in 25..39) {
            page.set(x, 17, code)
            code++
            if (code > 0x7f) code = 0x20
        }

        formatAsPage(page.data, 0x100, buf)
    }
}

fun makeStaticFileEffect(file: File): Effect {
    return object : Effect(file.name) {
        val data = file.readBytes()
        init {
            formatAsPage(data, 0x100, buf)
        }
        override fun doReset() = Unit
        override fun doTick(ticks: Int, elapsed: Double) = Unit
    }
}

class PlasmaEffect: Effect("Plasma") {
    val dots = extractDotImages("gfx/colordots.png")
    val center = PointXY(PLASMA_W/2, PLASMA_H/2)
    var movingDots = listOf(
        MovingDot(dots[0], center, SineMover(20.0, 25.3, 3.4, 5.0).chain(SineMover(19.0, 5.0, 6.4, 12.5))),
        MovingDot(dots[0], center, SineMover(12.0, 17.0, 6.5, 4.7).chain(SineMover(17.3, 8.4, 5.7, 9.1))),
        MovingDot(dots[2], center, SineMover(30.0, 13.5, 4.8, 2.7)),
        MovingDot(dots[2], center, SineMover(13.0, 21.5, 4.8, 4.7)),
        MovingDot(dots[4], center, SineMover(12.0, 11.0, 3.3, 1.9).chain(SineMover(11.0, 9.0, 6.1, 4.5))),
        MovingDot(dots[4], center, SineMover(21.0, 24.0, 6.3, 3.9)),
    )
    val renderer = PlasmaRenderer(movingDots)
    override fun doReset() = Unit
    override fun doTick(ticks: Int, elapsed: Double) {
        renderer.tick(elapsed)
        renderer.paint()
        formatAsPage(renderer.page.data, 0x100, buf)
    }
}

class PlasmaEffect2: Effect("Plasma2") {
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
    val renderer = PlasmaRenderer(movingDots, true)
    override fun doReset() = Unit
    override fun doTick(ticks: Int, elapsed: Double) {
        renderer.tick(elapsed)
        renderer.paint()
        formatAsPage(renderer.page.data, 0x100, buf)
    }
}

val allEffects = mutableListOf(
    PlaceholderEffect(),
    SyncTestEffect(),

    AmigaBootEffect(),
    DocDemoEffect(),
    ProTrackerEffect(),
    GuruCrashEffect(),
    VectorEffect(),
    EvokeLogoEffect(),
    TitleEffect(),
    HistoryEffect(),
    VoronoiEffect(),
    ScrollerEffect("            CODE: LOSSO     MUSIC: BOD       THANKS TO ALISTAIR BUXTON (ALI1234, RASPI-TELETEXT GUY) AND ZXNET.CO.UK       JETZT SAUFEN MIT a @ e!                   "),
    PlasmaEffect(),
    PlasmaEffect2(),
)

enum class Pref {
    SMOL,
    FPS,
    HOST,
    HOST_PORT,
    AUTORUN,
    PNG_DIR,
    PNG_BLACK_ON_WHITE,
}

fun main() {
    activateNimbus()
    addEffectEntryForEachBinFile("gfx/")
    showInFrame("teletext-tools", makeMainComponent())
}

fun addEffectEntryForEachBinFile(dir: String) {
    File(dir).listFiles({ dir, name -> name.endsWith(".bin") }).forEach { allEffects.add(makeStaticFileEffect(it)) }
}

interface BufRenderer {
    fun render(buf: ByteArray)
}

class EffectThread(var effect: Effect?, var fps: Int, val sink: BufRenderer) {
    private var running = false
    fun start() {
        running = true
        Thread {
            effect?.reset()
            val start = System.currentTimeMillis().toDouble()
            var next = start
            while (running) {
                effect?.let {
                    var buf: ByteArray = it.buf
                    buf = it.tick()
                    sink.render(buf)
                }
                next += 1000.0 / fps
                val wait = next - System.currentTimeMillis()
                if (wait > 0) {
                    Thread.sleep(wait.toLong())
                }
            }
        }.start()
    }
    fun stop() {
        running = false
    }
}

fun debugPacket(buf: ByteArray) {
    val packets = buf.size / PACKET_LEN
    val out = StringBuilder()
    if (packets * PACKET_LEN != buf.size) out.append("Warning: odd buffer size: ${buf.size} - ")
    out.append(packets).append(" pkts: ")
    for (i in buf.indices step PACKET_LEN) {
        val b0 = unham(buf[i].toInt())
        val b1 = unham(buf[i+1].toInt())
        val mpag = (b1 shl 4) or b0
        val mag = (mpag and 0b00000111)
        val adr = (mpag and 0b11111000) shr 3
        if (adr == 0) {
            val pageDigit3 = unham(buf[i+2].toInt() and 0xff)
            val pageDigit2 = unham(buf[i+3].toInt() and 0xff)
            out.append("[m").append(mag).append(".").append(if (adr < 10) "0" else "").append(adr)
                .append(" page=")
                .append(pageDigit2.toString(16))
                .append(pageDigit3.toString(16))
                .append("] ")
        } else {
            out.append("[m").append(mag).append(".").append(if (adr < 10) "0" else "").append(adr).append("]")
        }
    }
    println(out)
}

class NetworkConnection(val ip: String, val port: Int, val status: JLabel?): BufRenderer {
    val socket: Socket
    val out: OutputStream
    var spinnerPos = 0
    val spinnerChars = "/-\\-"
    init {
        status?.text = "connecting to $ip:$port"
        status?.foreground = null
        socket = Socket(ip, port)
        out = socket.getOutputStream()
        status?.text = "connected to $ip:$port"
        status?.foreground = Color(0x008000)
    }
    override fun render(buf: ByteArray) {
        send(buf)
    }
    fun send(buf: ByteArray) {
        val raw = buf.size % PACKET_LEN == 0
        val sendBuf: ByteArray
        if (raw) {
            sendBuf = buf
        } else {
            sendBuf = formatAsPage(buf, 0x100)
        }
        try {
            out.write(sendBuf)
            out.flush()
            status?.text = "OK sent: ${sendBuf.size} [${spinnerChars[spinnerPos]}]"
            for (x in 8 until PAGE_COLS) fillingHeader[x] = buf[x]
            insertPageHeader(fillingHeader, 0, 0x1ff, 0, PageHeaderControlBit.C7_SUPPRESS_HEADER)
            out.write(fillingHeader)
            out.flush()
            status?.text = "OK sent: ${sendBuf.size} [${spinnerChars[spinnerPos]}] ${fillingHeader.size}"
            spinnerPos = (spinnerPos+1) % spinnerChars.length
        } catch (e: Exception) {
            e.printStackTrace()
            if (status == null) {
                throw java.lang.IllegalStateException("network is gone", e)
            }
            status?.text = "send error (${e.javaClass.simpleName} ${e.message})"
            status?.foreground = Color(0x800000)
        }
    }

    fun disconnect() {
        try {
            out.close()
            status?.text = "closed"
            status?.foreground = null
        } catch (e: Exception) {
            e.printStackTrace()
            status?.text = "close error (${e.javaClass.simpleName} ${e.message})"
        }
    }
    override fun toString() = "$ip:$port"
}

fun makeMainComponent(): JPanel {
    var currentEffectThread: EffectThread? = null
    var currentNetworkConnection: NetworkConnection? = null
    var currentAppendStream: OutputStream? = null

    val main = JPanel(BorderLayout())
    main.border = BorderFactory.createEmptyBorder(PADH, PADW, PADH, PADW)

    // display, font
    val font = getTeletextFont()
    val teletextDisplay = TeletextDisplayPanel(font)
    teletextDisplay.border = BorderFactory.createTitledBorder("Teletext screen")

    // smol checkbox
    val smol = JCheckBox("smol font")
    smol.isSelected = getBooleanPref(Pref.SMOL, false)
    smol.addActionListener {
        teletextDisplay.font = getTeletextFont(smol.isSelected)
        setPref(Pref.SMOL, smol.isSelected.toString())
    }
    teletextDisplay.font = getTeletextFont(smol.isSelected)

    // fps select
    val fps = JComboBox(arrayOf(50,25,24,15,10,7,6,5,4,3,2,1))
    fps.selectedItem = getIntPref(Pref.FPS, 25)
    fps.addActionListener {
        setPref(Pref.FPS, fps.selectedItem.toString())
        currentEffectThread?.fps = fps.selectedItem as Int
    }

    // network
    val targetHost = JTextField(getPref(Pref.HOST, "192.168.178.100"))
    val targetPort = JTextField(getIntPref(Pref.HOST_PORT, 2000).toString())
    val networkStatus = JLabel("not connected")
    val connectButton = JButton("(re)connect")
    connectButton.addActionListener {
        Thread {
            try {
                currentNetworkConnection?.disconnect()
                currentNetworkConnection = NetworkConnection(targetHost.text, targetPort.text.toInt(), networkStatus)
                teletextDisplay.networkSink = currentNetworkConnection
            } catch (e: Exception) {
                e.printStackTrace()
                networkStatus.text = "${e.javaClass.simpleName}: ${e.message}"
                networkStatus.foreground = Color(0x800000)
                teletextDisplay.networkSink = null
                JOptionPane.showMessageDialog(main, "Connect error: $e", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }.start()
    }

    // file dump
    val appendFileInput = JTextField("teletext-frames.raw")
    val appendCheckbox = JCheckBox("dump to file")
    appendCheckbox.addActionListener {
        if (appendCheckbox.isSelected) {
            try {
                currentAppendStream = FileOutputStream(appendFileInput.text)
                teletextDisplay.outputSink = currentAppendStream
            } catch (e: Exception) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(main, "Error: $e", "Error", JOptionPane.ERROR_MESSAGE)
            }
        } else {
            currentAppendStream?.close()
            teletextDisplay.outputSink = null
            val dumpFile = File(appendFileInput.text)
            JOptionPane.showMessageDialog(main, "Wrote ${dumpFile.absolutePath} (size: ${dumpFile.length()}", "OK", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    // effects
    val effectSelect = JComboBox(allEffects.toTypedArray())
    var autoRun = JCheckBox("auto run")
    autoRun.isSelected = getBooleanPref(Pref.AUTORUN, true)
    val runButton = JButton("run")
    val stopButton = JButton("stop")
    stopButton.isEnabled = false
    val runAction = {
        runButton.isEnabled = false
        stopButton.isEnabled = true
        currentEffectThread?.let { it.stop() }
        currentEffectThread = EffectThread(effectSelect.selectedItem as Effect, fps.selectedItem as Int, teletextDisplay)
        currentEffectThread?.let { it.start() }
    }
    val stopAction = {
        runButton.isEnabled = true
        stopButton.isEnabled = false
        currentEffectThread?.let { it.stop() }
    }
    runButton.addActionListener { runAction() }
    stopButton.addActionListener { stopAction() }
    effectSelect.addActionListener {
        stopAction()
        if (autoRun.isSelected) runAction()
    }

    // util
    val convertButton = JButton("convert PNG\u2026")
    val blackOnWhite = JCheckBox("black on white")
    blackOnWhite.isSelected = getBooleanPref(Pref.PNG_BLACK_ON_WHITE, true)
    convertButton.addActionListener {
        try {
            val dir = File(getPref(Pref.PNG_DIR, "."))
            val chooser = JFileChooser(dir)
            chooser.fileFilter = FileNameExtensionFilter("PNG files", "png")
            val ret = chooser.showOpenDialog(main)
            if (ret == JFileChooser.APPROVE_OPTION) {
                val conv = convertImage(img = ImageIO.read(chooser.selectedFile), inverted = !blackOnWhite.isSelected)
                teletextDisplay.render(conv)
                setPref(Pref.PNG_DIR, chooser.selectedFile.parentFile.toString())
                chooser.selectedFile = File(chooser.selectedFile.parentFile, chooser.selectedFile.name.replace(".png", ".bin"))
                chooser.fileFilter = FileNameExtensionFilter("bin files", "bin")
                val ret2 = chooser.showSaveDialog(main)
                if (ret2 == JFileChooser.APPROVE_OPTION) {
                    FileOutputStream(chooser.selectedFile).use { it.write(conv) }
                    JOptionPane.showMessageDialog(main, "Wrote ${chooser.selectedFile.absolutePath}", "OK", JOptionPane.INFORMATION_MESSAGE)
                    setPref(Pref.PNG_BLACK_ON_WHITE, "${blackOnWhite.isSelected}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(main, "Error: $e", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    // put together
    val fpsControls = JPanel()
    fpsControls.add(JLabel("fps"))
    fpsControls.add(fps)
    fpsControls.add(smol)
    val netControls = JPanel()
    netControls.add(JLabel("target host"))
    netControls.add(targetHost)
    netControls.add(JLabel("port"))
    netControls.add(targetPort)
    netControls.add(connectButton)
    val dumpControls = JPanel()
    dumpControls.add(appendCheckbox)
    dumpControls.add(appendFileInput)
    val netControls2 = JPanel()
    netControls2.add(JLabel("status:"))
    netControls2.add(networkStatus)
    val effectControls = JPanel()
    effectControls.add(JLabel("effect"))
    effectControls.add(effectSelect)
    effectControls.add(runButton)
    effectControls.add(stopButton)
    effectControls.add(autoRun)
    var utilControls = JPanel()
    utilControls.add(convertButton)
    utilControls.add(blackOnWhite)
    val controls = Box.createVerticalBox()
    controls.add(fpsControls)
    controls.add(netControls)
    controls.add(netControls2)
    controls.add(dumpControls)
    controls.add(effectControls)
    controls.add(utilControls)
    main.add(teletextDisplay, BorderLayout.CENTER)
    main.add(controls, BorderLayout.EAST)

    teletextDisplay.render(formatAsPage(textPage, 0x100))

    return main
}

fun getBooleanPref(key: Any, defaultValue: Boolean): Boolean {
    return getPref(key, defaultValue.toString()).toBoolean()
}

fun getIntPref(key: Any, defaultValue: Int): Int {
    return getPref(key, defaultValue.toString()).toInt()
}

fun getPref(key: Any, defaultValue: String): String = Preferences.userRoot().get(toPrefKey(key), defaultValue)

fun setPref(key: Any, value: String) = Preferences.userRoot().put(toPrefKey(key), value)

fun toPrefKey(key: Any): String = "teletext.$key"

fun activateNimbus() {
    UIManager.getInstalledLookAndFeels().first { "Nimbus".equals(it.name) }?.let { UIManager.setLookAndFeel(it.className) }
}

fun createPreviewImagePanel(font: TeletextFont) : ImagePanel {
    val img = BufferedImage(font.charW*40, font.charH*25, BufferedImage.TYPE_INT_RGB)
    val imagePanel = ImagePanel(img)
    imagePanel.border = BorderFactory.createTitledBorder("Teletext display")
    return imagePanel
}
