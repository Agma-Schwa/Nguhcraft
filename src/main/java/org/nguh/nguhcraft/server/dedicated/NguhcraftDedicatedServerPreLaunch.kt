package org.nguh.nguhcraft.server.dedicated

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy
import org.apache.logging.log4j.core.layout.PatternLayout
import org.nguh.nguhcraft.server.ServerUtils.IsDedicatedServer
import org.nguh.nguhcraft.server.dedicated.NguhcraftDedicatedServer.Companion.ReplaceLogger
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Environment(EnvType.SERVER)
class NguhcraftAppender(Rewriter: RewriteAppender? = null) : AbstractAppender(
    "Nguhcraft",
    null,
    PatternLayout.newBuilder().build(),
    false,
    arrayOf()
) {
    val LogFile = FileOutputStream(File("logs/latest.log"), true)

    // The rewrite policy and rewriter function are from the stack deobfuscation mod;
    // this way, we can use it without having to depend on it during the build process.
    val Policy = run {
        if (Rewriter == null) return@run null
        val Field = Rewriter.javaClass.getDeclaredField("rewritePolicy")
        Field.isAccessible = true
        return@run Field.get(Rewriter) as RewritePolicy
    }

    val ClassNameRewriter = run {
        if (Policy == null) return@run null
        val MappingsField = Policy.javaClass.getDeclaredField("mappings")
        val RemapString = MappingsField.type.getDeclaredMethod("remapString", String::class.java)
        MappingsField.isAccessible = true
        RemapString.isAccessible = true
        val Mappings = MappingsField.get(Policy)
        return@run { Raw: String -> RemapString.invoke(Mappings, Raw) as String }
    }

    override fun append(Raw: LogEvent) {
        if (Raw.level == Level.TRACE || Raw.level == Level.DEBUG) return
        val E = Policy?.rewrite(Raw) ?: Raw
        val T = LocalDateTime.ofInstant(Instant.ofEpochSecond(E.instant.epochSecond), ZoneOffset.systemDefault())
        val C = E.source.className
        val Date = T.format(DTF)
        val Msg = layout.toSerializable(E).toString()
        var Class = (ClassNameRewriter?.invoke(C) ?: C).split('.').last()
        if (Class.endsWith("\$Companion")) Class = Class.dropLast("\$Companion".length)

        // Print message with colours to stdout.
        print(
            "\u001b[34m[${Date}]\u001b[m " +
            "\u001b[${ThreadFormat(E.level)}m[${E.threadName}/${E.level}]\u001b[m " +
            "\u001b[36m($Class)\u001b[m " +
            "\u001b[${MsgFormat(E.level)}m$Msg\u001b[m"
        )

        // And to the log file w/o colours.
        LogFile.write("[${Date}] [${E.threadName}/${E.level}] ($Class) $Msg".toByteArray(Charsets.UTF_8))
    }

    companion object {
        val DTF: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        private fun MsgFormat(L: Level) = when (L) {
            Level.FATAL, Level.ERROR -> "1;31"
            Level.WARN -> "33"
            else -> ""
        }

        private fun ThreadFormat(L: Level) = when (L) {
            Level.FATAL, Level.ERROR -> "31"
            Level.WARN -> "33"
            else -> "32"
        }
    }
}

class NguhcraftDedicatedServerPreLaunch : PreLaunchEntrypoint {
    /** Replace the default appender with one that adds pretty colours. */
    override fun onPreLaunch() {
        if (IsDedicatedServer()) {
            println("[Nguhcraft] [PreLaunch] Setting up formatted logger...")
            ReplaceLogger("SysOut")
        }
    }
}