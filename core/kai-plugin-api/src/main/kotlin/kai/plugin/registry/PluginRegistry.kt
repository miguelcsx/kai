package kai.plugin.registry

import kai.domain.campaign.CampaignConfig
import kai.domain.id.InterfaceVersion
import kai.plugin.executor.ExecutorPlugin
import kai.plugin.oracle.OraclePlugin
import kai.plugin.reducer.ReducerPlugin
import kai.plugin.scheduler.SchedulerPlugin
import kai.plugin.strategy.StrategyPlugin

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
}

class PluginRegistry(
    private val expectedVersion: InterfaceVersion = InterfaceVersion.V1
) {
    private val strategies = linkedMapOf<String, StrategyPlugin>()
    private val executors = linkedMapOf<String, ExecutorPlugin>()
    private val oracles = linkedMapOf<String, OraclePlugin>()
    private val reducers = linkedMapOf<String, ReducerPlugin>()
    private var scheduler: SchedulerPlugin? = null

    fun registerStrategy(plugin: StrategyPlugin) {
        strategies[plugin.id.value] = plugin
    }

    fun registerExecutor(plugin: ExecutorPlugin) {
        executors[plugin.id.value] = plugin
    }

    fun registerOracle(plugin: OraclePlugin) {
        oracles[plugin.id.value] = plugin
    }

    fun registerReducer(plugin: ReducerPlugin) {
        reducers[plugin.id.value] = plugin
    }

    fun registerScheduler(plugin: SchedulerPlugin) {
        scheduler = plugin
    }

    fun resolveStrategy(id: String): StrategyPlugin {
        return requireNotNull(strategies[id]) { "Unknown strategy: $id" }
    }

    fun resolveExecutor(id: String): ExecutorPlugin {
        return requireNotNull(executors[id]) { "Unknown executor: $id" }
    }

    fun resolveOracle(id: String): OraclePlugin {
        return requireNotNull(oracles[id]) { "Unknown oracle: $id" }
    }

    fun resolveReducer(id: String): ReducerPlugin {
        return requireNotNull(reducers[id]) { "Unknown reducer: $id" }
    }

    fun resolveScheduler(): SchedulerPlugin {
        return requireNotNull(scheduler) { "Scheduler is not registered" }
    }

    fun validateConfig(config: CampaignConfig): ValidationResult {
        val errors = mutableListOf<String>()
        validateStrategies(config, errors)
        validateExecutor(config, errors)
        validateOracles(config, errors)
        validateReducers(config, errors)
        if (scheduler == null) {
            errors += "Missing scheduler plugin"
        }
        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Failure(errors)
    }

    private fun validateStrategies(
        config: CampaignConfig,
        errors: MutableList<String>
    ) {
        config.strategyIds.forEach { id ->
            val plugin = strategies[id.value]
            if (plugin == null) {
                errors += "Missing strategy plugin: ${id.value}"
            } else {
                validateVersion("strategy", id.value, plugin.version, errors)
            }
        }
    }

    private fun validateExecutor(
        config: CampaignConfig,
        errors: MutableList<String>
    ) {
        val plugin = executors[config.executorId.value]
        if (plugin == null) {
            errors += "Missing executor plugin: ${config.executorId.value}"
            return
        }
        validateVersion("executor", config.executorId.value, plugin.version, errors)
    }

    private fun validateOracles(
        config: CampaignConfig,
        errors: MutableList<String>
    ) {
        config.oracleIds.forEach { id ->
            val plugin = oracles[id.value]
            if (plugin == null) {
                errors += "Missing oracle plugin: ${id.value}"
            } else {
                validateVersion("oracle", id.value, plugin.version, errors)
            }
        }
    }

    private fun validateReducers(
        config: CampaignConfig,
        errors: MutableList<String>
    ) {
        config.reducerIds.forEach { id ->
            val plugin = reducers[id.value]
            if (plugin == null) {
                errors += "Missing reducer plugin: ${id.value}"
            } else {
                validateVersion("reducer", id.value, plugin.version, errors)
            }
        }
    }

    private fun validateVersion(
        type: String,
        id: String,
        actual: InterfaceVersion,
        errors: MutableList<String>
    ) {
        if (!actual.isCompatibleWith(expectedVersion)) {
            errors += "Incompatible $type plugin version for $id: expected $expectedVersion, got $actual"
        }
    }
}
