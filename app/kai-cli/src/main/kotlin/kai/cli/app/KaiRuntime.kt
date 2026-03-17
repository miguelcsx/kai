package kai.cli.app

import java.nio.file.Path
import java.nio.file.Paths
import kai.config.TomlCampaignConfigParser
import kai.engine.CampaignEngine
import kai.plugin.registry.PluginRegistry
import kai.storage.fs.FileSystemStorage

data class KaiRuntime(
    val parser: TomlCampaignConfigParser,
    val storage: FileSystemStorage,
    val registry: PluginRegistry,
    val engine: CampaignEngine
)

object KaiRuntimeFactory {
    fun create(storageDir: String?, pluginDir: String?): KaiRuntime {
        val storagePath = resolveStoragePath(storageDir)
        val storage = FileSystemStorage(storagePath)
        val registry = kai.cli.commands.PluginDiscovery.load(pluginDir)
        return KaiRuntime(
            parser = TomlCampaignConfigParser(),
            storage = storage,
            registry = registry,
            engine = CampaignEngine(registry, storage.ports)
        )
    }

    private fun resolveStoragePath(storageDir: String?): Path {
        return if (storageDir == null) Paths.get(".kai") else Paths.get(storageDir)
    }
}
