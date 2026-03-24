package kai.cli.commands

import java.nio.file.Path
import java.nio.file.Paths
import kai.config.TomlCampaignConfigParser
import kai.engine.CampaignEngine
import kai.engine.event.CampaignEvent
import kai.plugin.registry.PluginRegistry
import kai.storage.fs.FileSystemStorage

internal fun parser(): TomlCampaignConfigParser {
    return TomlCampaignConfigParser()
}

internal fun defaultStorage(path: String?): FileSystemStorage {
    val resolved = if (path == null) Paths.get(".kai") else Paths.get(path)
    return FileSystemStorage(resolved)
}

internal fun defaultRegistry(pluginDir: String?): PluginRegistry {
    return PluginDiscovery.load(pluginDir)
}

internal fun defaultEngine(
    storage: FileSystemStorage,
    pluginDir: String?
): CampaignEngine {
    return CampaignEngine(defaultRegistry(pluginDir), storage.ports)
}

internal fun emitEvent(event: CampaignEvent, json: Boolean) {
    if (json) {
        println(toJson(event))
    } else {
        println(toText(event))
    }
}

private fun toText(event: CampaignEvent): String {
    return when (event) {
        is CampaignEvent.Started -> "campaign started: ${event.state.id.value}"
        is CampaignEvent.TestCaseGenerated -> "testcase: ${event.testCase.id.value}"
        is CampaignEvent.FindingStored -> "finding: ${event.finding.id.value}"
        is CampaignEvent.StateSaved -> "state: ${event.state.status} generated=${event.state.stats.totalGenerated}"
        is CampaignEvent.Finished -> "campaign finished: findings=${event.state.stats.totalInteresting}"
    }
}

private fun toJson(event: CampaignEvent): String {
    return when (event) {
        is CampaignEvent.Started -> """{"event":"started","campaignId":"${event.state.id.value}"}"""
        is CampaignEvent.TestCaseGenerated -> """{"event":"testcase","id":"${event.testCase.id.value}"}"""
        is CampaignEvent.FindingStored -> """{"event":"finding","id":"${event.finding.id.value}"}"""
        is CampaignEvent.StateSaved -> """{"event":"state","status":"${event.state.status}","generated":${event.state.stats.totalGenerated}}"""
        is CampaignEvent.Finished -> """{"event":"finished","findings":${event.state.stats.totalInteresting}}"""
    }
}

internal data class CommonOptions(
    val storageDir: String?,
    val pluginDir: String?,
    val json: Boolean,
    val positional: List<String>
)

internal fun parseOptions(args: List<String>): CommonOptions {
    var storageDir: String? = null
    var pluginDir: String? = null
    var json = false
    val positional = mutableListOf<String>()
    var index = 0
    while (index < args.size) {
        when (val arg = args[index]) {
            "--json" -> json = true
            "--storage-dir" -> {
                index++
                storageDir = args.getOrNull(index) ?: error("--storage-dir requires a value")
            }
            "--plugins-dir" -> {
                index++
                pluginDir = args.getOrNull(index) ?: error("--plugins-dir requires a value")
            }
            else -> positional += arg
        }
        index++
    }
    return CommonOptions(storageDir, pluginDir, json, positional)
}
