package kai.cli.commands

import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ServiceLoader
import kai.plugin.executor.ExecutorPlugin
import kai.plugin.oracle.OraclePlugin
import kai.plugin.reducer.ReducerPlugin
import kai.plugin.registry.PluginRegistry
import kai.plugin.scheduler.SchedulerPlugin
import kai.plugin.strategy.StrategyPlugin

internal object PluginDiscovery {
    fun load(pluginDir: String?): PluginRegistry {
        val loader = classLoader(pluginDir)
        return PluginRegistry().also { registry ->
            services<StrategyPlugin>(loader).forEach(registry::registerStrategy)
            services<ExecutorPlugin>(loader).forEach(registry::registerExecutor)
            services<OraclePlugin>(loader).forEach(registry::registerOracle)
            services<ReducerPlugin>(loader).forEach(registry::registerReducer)
            services<SchedulerPlugin>(loader).forEach(registry::registerScheduler)
        }
    }

    private fun classLoader(pluginDir: String?): ClassLoader {
        val parent = javaClass.classLoader
        if (pluginDir == null) {
            return parent
        }
        val dir = Paths.get(pluginDir)
        val urls = jarUrls(dir)
        if (urls.isEmpty()) {
            return parent
        }
        return URLClassLoader(urls.toTypedArray(), parent)
    }

    private fun jarUrls(dir: Path): List<java.net.URL> {
        if (!Files.exists(dir)) {
            return emptyList()
        }
        val stream = Files.list(dir)
        try {
            return stream
                .filter { it.fileName.toString().endsWith(".jar") }
                .map { it.toUri().toURL() }
                .toArray()
                .map { it as java.net.URL }
        } finally {
            stream.close()
        }
    }

    private inline fun <reified T : Any> services(loader: ClassLoader): List<T> {
        return ServiceLoader.load(T::class.java, loader).toList()
    }
}
