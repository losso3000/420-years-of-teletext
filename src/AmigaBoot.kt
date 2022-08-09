import java.io.File
import java.lang.Math.random
import kotlin.math.sin

enum class AmigaBootState(val duration: Double) {
    BLACK(0.5),
    DARK(1.0),
    GREY(1.0),
    WHITE(2.0),
    LOAD(3.0),
    DECRUNCH(4.0),
    WAIT(1.0);
    companion object {
        fun getStateByElapsedTime(time: Double): AmigaBootState {
            var until = 0.0
            values().forEach { state ->
                until += state.duration
                if (time < until) return state
            }
            return values().last()
        }
    }
}

class AmigaBootEffect: Effect("AmigaDOS boot") {
    var state: AmigaBootState = AmigaBootState.BLACK
    var stateTicks = 0
    val page = TeletextPage()
    val amigaDosWindow = File("gfx/amigados.bin").readBytes()
    val amigaDosWindowCrunch = File("gfx/amigados-crunch.bin").readBytes()

    override fun doReset() {
        state = AmigaBootState.BLACK
    }

    override fun doTick(ticks: Int, elapsed: Double) {
        stateTicks++
        val newState = AmigaBootState.getStateByElapsedTime(elapsed)
        if (newState != state) {
            state = newState
            stateTicks = 0
        }
        when (newState) {
            AmigaBootState.BLACK -> renderSingleColor(TeletextColor.BLACK)
            AmigaBootState.DARK -> renderSingleColor(TeletextColor.BLUE)
            AmigaBootState.GREY -> renderSingleColor(TeletextColor.CYAN)
            AmigaBootState.WHITE -> renderSingleColor(TeletextColor.WHITE)
            AmigaBootState.LOAD -> renderAmigaDos(stateTicks)
            AmigaBootState.DECRUNCH -> renderDecrunch(stateTicks)
            AmigaBootState.WAIT -> renderAmigaDos(1000)
        }
        formatAsPage(page.data, 0x100, buf)
    }

    private fun renderDecrunch(ticks: Int) {
        page.takeDataFrom(amigaDosWindow)
        val decrunchAmount = 0.5 + sin(ticks * 0.1) * 0.3
        for (y in 1 until PAGE_ROWS) {
            if (random() < decrunchAmount) {
                for (x in 0 until PAGE_COLS) {
                    page.set(x, y, amigaDosWindowCrunch[PAGE_COLS*y+x])
                }
            }
        }
    }

    private fun renderAmigaDos(ticks: Int) {
        page.takeDataFrom(amigaDosWindow)
        if (ticks < 2) {
            page.drawString(5, 3, " ".repeat(31))
            page.drawString(5, 4, " ".repeat(31))
            page.drawString(5, 5, " ".repeat(31))
            page.set(4, 3, 0x11.toByte()) // gfx red
            page.set(5, 3, 0x7f.toByte()) // block
            page.set(6, 3, 0x17.toByte()) // gfx white
            page.set(5, 6, 0x20.toByte()) // hide final cursor
        } else if (ticks < 4) {
            page.drawString(5, 4, " ".repeat(31))
            page.drawString(5, 5, " ".repeat(31))
            page.set(4, 4, 0x11.toByte()) // gfx red
            page.set(5, 4, 0x7f.toByte()) // block
            page.set(6, 4, 0x17.toByte()) // gfx white
            page.set(5, 6, 0x20.toByte()) // hide final cursor
        } else if (ticks < 6) {
            page.drawString(5, 5, " ".repeat(31))
            page.set(25, 4, 0x11.toByte()) // gfx red
            page.set(26, 4, 0x7f.toByte()) // block
            page.set(27, 4, 0x17.toByte()) // gfx white
            page.set(5, 6, 0x20.toByte()) // hide final cursor
        }
    }

    private fun renderSingleColor(color: TeletextColor) {
        page.data.indices.forEach { page.data[it] = 0x20.toByte() }
        for (y in 1 until PAGE_ROWS) {
            page.set(0, y, color.ordinal.toByte())
            page.set(1, y, 0x1d.toByte()) // new background
        }
    }

}