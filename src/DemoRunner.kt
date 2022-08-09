import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.swing.Box
import javax.swing.JFrame
import kotlin.math.abs
import kotlin.system.exitProcess

class DemoEvent(min: Int, sec: Int, milli: Int, val effectIndex: Int, val handler: ((Double, Effect?) -> Unit)? = null) {
    val time = min.toDouble() * 60.0 + sec.toDouble() + milli.toDouble() / 1000.0
}

class DemoScript(val audio: AudioOutput?, val insertTimeStampTopLeft: Boolean): Effect("DEMO") {
    var currentEffectIndex: Int? = null
    var currentEffect: Effect? = null
    val page = TeletextPage()
    val runInTime = 4.0
    val music = audio?.prepareClip("sfx/teletext_demo2.wav")
    val events = mutableListOf<DemoEvent>()
    var playingClip: Clip? = null
    val probablyOnRaspi = System.getProperty("os.name")?.contains("Linux") ?: false
    var clipStart: Long? = null
    override fun doReset() {
        currentEffect = null
        currentEffectIndex = null
        events.clear()
        events.addAll(getDemoEvents())
        playingClip?.stop()
        playingClip = null
        clipStart = null
    }
    fun getDemoEvents(): List<DemoEvent> {
        val ret = mutableListOf<DemoEvent>()
        ret += DemoEvent(0, 0,  0,    2) // > AmigaBoot
        ret += DemoEvent(0,17,  0,    3) // > DocDemo

        ret += DemoEvent(0,32,677,    3) /*             * EVENT */ { _, effect -> if (effect is DocDemoEffect) effect.renderer.flashLogo(12) }
        ret += DemoEvent(0,33,622,    3) /*             * EVENT */ { _, effect -> if (effect is DocDemoEffect) effect.renderer.flashLogo(11) }
        ret += DemoEvent(0,34,562,    3) /*             * EVENT */ { _, effect -> if (effect is DocDemoEffect) effect.renderer.flashLogo(10) }
        ret += DemoEvent(0,35,516,    3) /*             * EVENT */ { _, effect -> if (effect is DocDemoEffect) effect.renderer.flashLogo(13) }
        ret += DemoEvent(0,36,416,    3) /*             * EVENT */ { _, effect -> if (effect is DocDemoEffect) effect.renderer.flashLogo(14) }
        ret += DemoEvent(0,37,405,    3) /*             * EVENT */ { _, effect -> if (effect is DocDemoEffect) effect.renderer.flashLogo(15) }

        ret += DemoEvent(0,37,766,    4) // > ProTracker
        ret += DemoEvent(0,52, 66,    5) // > GuruCrash
        ret += DemoEvent(1,52, 10,    5) // >           * blip blip * (eNable vector?)
        ret += DemoEvent(1,53,533,    6) // > Vector
        ret += DemoEvent(2,12,  0,    7) //   EvokeLogo
        ret += DemoEvent(2,31,433,    8) // > Title
        ret += DemoEvent(3, 1,447,    9) //   History
        ret += DemoEvent(4,31,  0,   10) //   Voronoi greetz
        ret += DemoEvent(4,53, 44,   10) //             * 4 take tom-tom dribbel
        ret += DemoEvent(4,54,450,   11) //   Scroller
        ret += DemoEvent(10,0,0,1)       //   End marker
        return ret
    }

    override fun doTick(ticks: Int, elapsedWithRunIn: Double) {
        val elapsed: Double
        var clip = playingClip
        if (clip != null) {
            if (probablyOnRaspi) {
                // avoid Clip.getMicrosecondPosition at all costs on raspi!
                val start = clipStart ?: System.currentTimeMillis()
                if (clipStart == null) clipStart = start
                elapsed = (System.currentTimeMillis() - start) / 1000.0
            } else {
                elapsed = clip.microsecondPosition / 1_000_000.0
            }
        } else {
            elapsed = elapsedWithRunIn - runInTime
        }

        if (elapsed < 0.0) {
            drawCountDown(elapsed)
        } else {
            for (i in 0 until events.size - 1) {
                val nextStart = events[i+1].time
                if (elapsed < nextStart) {
                    val event = events[i]
                    ensureCurrentEffect(event.effectIndex, elapsed)
                    event.handler?.invoke(elapsed, currentEffect)
                    break
                }
            }
        }

        val effect = currentEffect
        if (playingClip?.isRunning != false) {
            effect?.tick(elapsed)
        }
        if (effect != null) {
            buf = effect.buf
        } else {
            buf = formatAsPage(page.data, 0x100)
        }
        if (insertTimeStampTopLeft) {
            insertTimestamp(buf, elapsed, currentEffect?.lastElapsed)
        }
    }

    private fun ensureCurrentEffect(index: Int, elapsed: Double) {
        if (currentEffectIndex == index) return
        currentEffectIndex = index
        if (currentEffect == null && playingClip == null) {
            // first effect - start audio
            music?.microsecondPosition = 0L
            music?.start()
            playingClip = music
        }
        currentEffect = allEffects[index]
        println("\nnew effect: $index = ${currentEffect?.name} @ $elapsed")
        currentEffect?.reset(elapsed)
    }

    fun goBack() {
        val index = currentEffectIndex ?: return
        goto(index - 1)
    }

    fun goForward() {
        val index = currentEffectIndex ?: return
        goto(index + 1)
    }

    private fun goto(newIndex: Int) {
        println("\ngoto $newIndex")
        events.filter { it.effectIndex == newIndex }.firstOrNull()?.let { event ->
            val micros = (event.time * 1_000_000.0).toLong()
            currentEffect = null
            playingClip?.microsecondPosition = micros
            ensureCurrentEffect(newIndex, event.time)
            return
        }
        println("goto: effect $newIndex not found")
    }

    private fun insertTimestamp(buf: ByteArray, elapsed: Double, effectElapsed: Double?) {
        doInsertTimestamp(buf, 0, elapsed)
        effectElapsed?.let { doInsertTimestamp(buf, 1, it) }
    }

    private fun doInsertTimestamp(buf: ByteArray, y: Int, elapsed: Double) {
        val neg = elapsed < 0.0
        val totalMillis = (abs(elapsed) * 1000.0).toInt()
        val totalSecs = totalMillis / 1000
        val millis = totalMillis % 1000
        val secs = totalSecs % 60
        var mins = totalSecs / 60
        val formatted = "\u001C\u0007%s%d:%02d.%02d".format(if (neg) "-" else "", mins, secs, millis/10 )
        formatted.forEachIndexed { index, c -> buf[PACKET_LEN * y + PACKET_LEN - formatted.length + index] = parity(c.code).toByte() }
    }

    private fun drawCountDown(elapsedWithRunIn: Double) {
        page.fill(0x20)
        val count = abs(elapsedWithRunIn).toInt()
        val ch = if (count > 0) '0'.code + count else ' '.code
        page.set(PAGE_COLS/2, PAGE_ROWS/2, ch)
    }

    fun keyPressed() {
        val micros = playingClip?.microsecondPosition ?: return
        val totalMillis = micros / 1000L
        val totalSecs = totalMillis / 1000
        val millis = totalMillis % 1000
        val secs = totalSecs % 60
        var mins = totalSecs / 60
        println("        ret += DemoEvent(%d,%2d,%3d,    %d) /*             * EVENT */".format(mins, secs, millis/10, currentEffectIndex ?: -1))
    }

}

class DemoRunnerConsole(val runner: DemoRunner): Effect("Console") {
    val page = TeletextPage()
    override fun doReset() {
        clear()
    }

    private fun clear() {
        for (y in 0 until PAGE_ROWS) {
            for (x in 0 until PAGE_COLS) {
                page.set(x, y, 0x20)
            }
        }
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        page.drawString(0, PAGE_ROWS-1, "T $ticks")
        page.drawString(8, 0, " ] Demo Runner [")
        page.drawString(0, 1, "Effects:")
        var y = 1
        page.drawString(22, y++, "Settings:")
        y++
        page.drawString(22, y++, "fps    = %d".format(runner.fps))
        page.drawString(22, y++, "target = %s".format(runner.sink))
        y++
        page.drawString(22, y++, "Commands:")
        y++
        page.drawString(22, y++, "list")
        page.drawString(22, y++, "effect <n>")
        page.drawString(22, y++, "+")
        page.drawString(22, y++, "-")
        page.drawString(22, y++, "fps <n>")
        page.drawString(22, y++, "stop")
        page.drawString(22, y++, "demo")
        page.drawString(22, y++, "quit")
        formatAsPage(page.data, 0x100, buf)
    }
}

class DemoRunner(val sink: BufRenderer, var fps: Int, val audio: AudioOutput?, val timestamp: Boolean) {
    var conn: NetworkConnection? = null
    var thread: EffectThread? = null
    var effect: Effect? = null
    var lastEffectNum = 0
    val console = DemoRunnerConsole(this)
    fun run() {
        ensureEffectThread(console, fps)
        println("Activate Teletext now.")
        println()
        commandLoop()
    }

    private fun commandLoop() {
        listEffects(console.page)
        println()
        var cmd: List<String>

        // "readln" is too new, cannot be fucked to update kotlin libs for that
        val reader = BufferedReader(InputStreamReader(System.`in`))

        while (true) {
            print("[go, fps x, quit, list, effect x, +, -, stop, demo]> ")
            System.out.flush()
            cmd = reader.readLine().split(' ')
            when (cmd.getOrElse(0) { "" }) {
                "q", "quit" -> quit()
                "f", "fps" -> setFps(cmd.getOrNull(1))
                "l","list" -> {
                    ensureEffectThread(console, fps)
                    listEffects(console.page)
                }
                "effect" -> setEffect(cmd.getOrNull(1))
                "+" -> nextEffectInRunner()
                "-" -> prevEffectInRunner()
                "stop" -> { thread?.stop(); thread = null }
                "demo" -> ensureEffectThread(DemoScript(audio, timestamp), fps)
                "pause" -> {
                    val effect = thread?.effect
                    if (effect is DemoScript) {
                        effect.playingClip?.stop()
                    } else {
                        println("not in demo runner")
                    }
                }
                "resume" -> {
                    val effect = thread?.effect
                    if (effect is DemoScript) {
                        effect.playingClip?.start()
                    } else {
                        println("not in demo runner")
                    }
                }
            }
        }
    }

    private fun prevEffectInRunner() {
        thread?.effect?.takeIf { it is DemoScript }?.let { (it as DemoScript).goBack() }
    }

    private fun nextEffectInRunner() {
        thread?.effect?.takeIf { it is DemoScript }?.let { (it as DemoScript).goForward() }
    }

    private fun listEffects(page: TeletextPage? = null) {
        println("Available effects:")
        allEffects.forEachIndexed { index, effect ->
            println(" %2d - %s".format(index, effect.name))
            page?.drawString(0, index+3, "%2d %s".format(index, effect.name))
        }
    }

    private fun setEffect(numString: String?) {
        if (numString == null || !numString.matches("[0-9]+".toRegex())) {
            println("effect $numString ?!")
        } else {
            val effect = allEffects.getOrNull(numString.toInt())
            if (effect == null) {
                println("No such effect: $numString")
            } else {
                ensureEffectThread(effect, fps)
                lastEffectNum = numString.toInt()
            }
        }
    }

    private fun setFps(fpsString: String?) {
        if (fpsString == null || !fpsString.matches("[0-9]+".toRegex())) {
            println("fps $fpsString ?!")
        } else {
            val fps = fpsString.toInt()
            thread?.fps = fps
            this.fps = fps
            println("fps set to $fps.")
        }
    }

    private fun quit() {
        println("Bye.")
        exitProcess(0)
    }

    private fun ensureEffectThread(effect: Effect, fps: Int) {
        this.effect = effect
        this.fps = fps
        thread?.let {
            println("effect ${effect.name}, $fps $fps")
            it.fps = fps
            effect.reset()
            it.effect = effect
            return
        }
        thread = EffectThread(effect, this.fps, sink)
        thread?.start()
        this.effect = effect
        this.conn = conn
    }

    fun keyPressed() {
        thread?.effect?.takeIf { it is DemoScript }?.let { (it as DemoScript).keyPressed() }
    }
}

class RunnerConf(val host: String?, val port: Int?, val fps: Int?, val preview: Boolean?, val sound: Boolean?, val timestamp: Boolean?) {
    fun hasNetworkData() = host != null && port != null
    companion object {
        fun parse(args: Array<String>): RunnerConf {
            val map = mutableMapOf<String, String>()
            for (i in 0 until args.size-1) {
                if (args[i].startsWith('-')) {
                    map[args[i].substring(1)] = args[i+1]
                }
            }
            return RunnerConf(map["host"], map["port"]?.toIntOrNull(), map["fps"]?.toIntOrNull(), map["preview"]?.toBoolean(), map["sound"]?.toBoolean(), map["time"]?.toBoolean())
        }
    }
}

class CompoundSink(val a: BufRenderer, val b: BufRenderer): BufRenderer {
    override fun render(buf: ByteArray) {
        a.render(buf)
        b.render(buf)
    }
}

class AudioOutput {
    fun prepareClip(filename: String): Clip {
        val stream = AudioSystem.getAudioInputStream(File(filename))
        val format = stream.format
        val info = DataLine.Info(Clip::class.java, format)
        val clip = AudioSystem.getLine(info) as Clip
        clip.open(stream)
        return clip
    }
}

fun main(args: Array<String>) {
    println("\u001b[41m teletext demo runner \u001B[0m")
    val conf = RunnerConf.parse(args)
    val networkSink: NetworkConnection?
    val previewSink: TeletextDisplayPanel?
    var usePreview = conf.preview ?: false
    var frame: JFrame? = null
    if (!conf.hasNetworkData() && !usePreview) {
        println("Neither network target nor preview specified, using graphical preview.")
        println("Run with \"-host <host> -port <port> [-preview true]\" to send via TCP.")
        usePreview = true
    }
    if (usePreview) {
        val fpsMon = PaintListener()
        previewSink = TeletextDisplayPanel(getTeletextFont())
        previewSink.paintListener = fpsMon
        val prevPanel = Box.createVerticalBox()
        prevPanel.add(previewSink)
        prevPanel.add(fpsMon)
        frame = showInFrame("Teletext preview", prevPanel)
        frame.location = Point(0, 0)
    } else {
        previewSink = null
    }
    if (conf.hasNetworkData()) {
        requireNotNull(conf.host) { "no host" }
        requireNotNull(conf.port) { "no port" }
        println("Connect to ${conf.host}:${conf.port}...")
        networkSink = NetworkConnection(conf.host, conf.port, null)
    } else {
        networkSink = null
    }
    val sink: BufRenderer
    if (previewSink != null && networkSink != null) {
        sink = CompoundSink(previewSink, networkSink)
    } else if (previewSink != null) {
        sink = previewSink
    } else if (networkSink != null) {
        sink = networkSink
    } else {
        throw IllegalStateException("no network or display preview sink")
    }
    val audio = if (true || conf.sound != false) AudioOutput() else null
    val timestamp = conf.timestamp ?: false
    val fps = conf.fps ?: 25
    try {
        val runner = DemoRunner(sink, fps, audio, timestamp)
        val keyListener = object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                runner.keyPressed()
            }
        }
        frame?.addKeyListener(keyListener)
        runner.run()
    } catch (e: Exception) {
        e.printStackTrace(System.out)
        println()
        errorExit(e.toString())
        return
    }
}

fun errorExit(s: String) {
    println("Error: $s")
    println()
    println("Usage: java DemoRunnerKt [-fps <fps>] [-host <host> -port <port>] [-preview true] [-sound true] [-ts true]")
    exitProcess(5)
}
