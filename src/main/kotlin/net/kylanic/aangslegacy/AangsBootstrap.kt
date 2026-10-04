@file:Suppress("UnstableApiUsage")
package net.kylanic.aangslegacy

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents


class AangsBootstrap : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        context.lifecycleManager.registerEventHandler(
            LifecycleEvents.DATAPACK_DISCOVERY.newHandler { event ->
                val uri = AangsBootstrap::class.java.getResource("/pack")!!.toURI()
                event.registrar().discoverPack(uri, "aangslegacy")
            }
        )
    }
}