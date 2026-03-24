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
            loadStrategies(loader).forEach(registry::registerStrategy)
            loadExecutors(loader).forEach(registry::registerExecutor)
            loadOracles(loader).forEach(registry::registerOracle)
            loadReducers(loader).forEach(registry::registerReducer)
            loadSchedulers(loader).forEach(registry::registerScheduler)
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

    private fun loadStrategies(loader: ClassLoader): List<StrategyPlugin> {
        return ServiceLoader.load(StrategyPlugin::class.java, loader).toList()
    }

    private fun loadExecutors(loader: ClassLoader): List<ExecutorPlugin> {
        return ServiceLoader.load(ExecutorPlugin::class.java, loader).toList()
    }

    private fun loadOracles(loader: ClassLoader): List<OraclePlugin> {
        return ServiceLoader.load(OraclePlugin::class.java, loader).toList()
    }

    private fun loadReducers(loader: ClassLoader): List<ReducerPlugin> {
        return ServiceLoader.load(ReducerPlugin::class.java, loader).toList()
    }

    private fun loadSchedulers(loader: ClassLoader): List<SchedulerPlugin> {
        return ServiceLoader.load(SchedulerPlugin::class.java, loader).toList()
    }
}
