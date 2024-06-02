package com.github.ajalt.mordant.internal.syscalls

import com.github.ajalt.mordant.internal.Size
import com.oracle.svm.core.annotate.Delete
import com.sun.jna.*

@Delete
@Suppress("ClassName", "PropertyName", "MemberVisibilityCanBePrivate", "SpellCheckingInspection")
private interface PosixLibC : Library {

    @Suppress("unused")
    @Structure.FieldOrder("ws_row", "ws_col", "ws_xpixel", "ws_ypixel")
    class winsize : Structure() {
        @JvmField
        var ws_row: Short = 0

        @JvmField
        var ws_col: Short = 0

        @JvmField
        var ws_xpixel: Short = 0

        @JvmField
        var ws_ypixel: Short = 0
    }

    @Structure.FieldOrder(
        "c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc", "c_ispeed", "c_ospeed"
    )
    class termios : Structure() {
        @JvmField
        var c_iflag: Int = 0

        @JvmField
        var c_oflag: Int = 0

        @JvmField
        var c_cflag: Int = 0

        @JvmField
        var c_lflag: Int = 0

        @JvmField
        var c_line: Byte = 0

        @JvmField
        var c_cc: ByteArray = ByteArray(32)

        @JvmField
        var c_ispeed: Int = 0

        @JvmField
        var c_ospeed: Int = 0
    }


    fun isatty(fd: Int): Int
    fun ioctl(fd: Int, cmd: Int, data: winsize?): Int

    @Throws(LastErrorException::class)
    fun tcgetattr(fd: Int, termios: termios)

    @Throws(LastErrorException::class)
    fun tcsetattr(fd: Int, cmd: Int, termios: termios)
}

@Delete
internal object SyscallHandlerJnaLinux : SyscallHandlerJnaPosix() {
    private const val TIOCGWINSZ = 0x00005413
    private const val TCSADRAIN: Int = 0x1
    private val libC: PosixLibC = Native.load(Platform.C_LIBRARY_NAME, PosixLibC::class.java)
    override fun isatty(fd: Int): Int = libC.isatty(fd)

    override fun getTerminalSize(): Size? {
        val size = PosixLibC.winsize()
        return if (libC.ioctl(STDIN_FILENO, TIOCGWINSZ, size) < 0) {
            null
        } else {
            Size(width = size.ws_col.toInt(), height = size.ws_row.toInt())
        }
    }

    override fun getStdinTermios(): Termios {
        val termios = PosixLibC.termios()
        libC.tcgetattr(STDIN_FILENO, termios)
        return Termios(
            iflag = termios.c_iflag.toUInt(),
            oflag = termios.c_oflag.toUInt(),
            cflag = termios.c_cflag.toUInt(),
            lflag = termios.c_lflag.toUInt(),
            cline = termios.c_line,
            cc = termios.c_cc.copyOf(),
            ispeed = termios.c_ispeed.toUInt(),
            ospeed = termios.c_ospeed.toUInt(),
        )
    }

    override fun setStdinTermios(termios: Termios) {
        val nativeTermios = PosixLibC.termios()
        nativeTermios.c_iflag = termios.iflag.toInt()
        nativeTermios.c_oflag = termios.oflag.toInt()
        nativeTermios.c_cflag = termios.cflag.toInt()
        nativeTermios.c_lflag = termios.lflag.toInt()
        nativeTermios.c_line = termios.cline
        termios.cc.copyInto(nativeTermios.c_cc)
        nativeTermios.c_ispeed = termios.ispeed.toInt()
        nativeTermios.c_ospeed = termios.ospeed.toInt()
        libC.tcsetattr(STDIN_FILENO, TCSADRAIN, nativeTermios)
    }
}