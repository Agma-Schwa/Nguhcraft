package org.nguh.nguhcraft.server.dedicated

import com.mojang.logging.LogUtils
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.dedicated.MinecraftDedicatedServer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender

@Environment(EnvType.SERVER)
class NguhcraftDedicatedServer : DedicatedServerModInitializer {
    val LOGGER = LogUtils.getLogger()

    override fun onInitializeServer() {
        // The stack deobfuscator likes to replace our appender, so replace
        // it again, but make sure to grab the rewriter so we can use it.
        if (FabricLoader.getInstance().isModLoaded(STACK_DEOBFUSCATOR_MOD_ID)) {
            val Root = LogManager.getRootLogger()
            val Ctx = LogManager.getContext(false) as LoggerContext
            val Config = Ctx.configuration.getLoggerConfig(Root.name)
            val Rewriter = Config.appenders[STACK_DEOBFUSCATOR_APPENDER_NAME] as? RewriteAppender
            if (Rewriter != null) {
                ReplaceLogger(STACK_DEOBFUSCATOR_APPENDER_NAME, Rewriter)
                LOGGER.info("Successfully wrapped stack deobfuscator!")
            }
        }

        ServerLifecycleEvents.SERVER_STARTING.register {
            NguhcraftAppender.Server = it as MinecraftDedicatedServer
            Discord.Start(it)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            Discord.Stop()
        }
    }

    companion object {
        const val STACK_DEOBFUSCATOR_APPENDER_NAME = "StackDeobfAppender"
        const val STACK_DEOBFUSCATOR_MOD_ID = "stackdeobfuscator"

        internal fun ReplaceLogger(ToReplace: String, Rewriter: RewriteAppender? = null) {
            val Root = LogManager.getRootLogger()
            val Ctx = LogManager.getContext(false) as LoggerContext
            val Config = Ctx.configuration.getLoggerConfig(Root.name)
            val A = NguhcraftAppender(Rewriter)
            A.start()
            Config.removeAppender(ToReplace)
            Config.addAppender(A, Config.level, null)
            Ctx.updateLoggers()
        }
    }
}
