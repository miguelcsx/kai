package kai.cli.commands

import kai.cli.app.FindingService
import kai.cli.app.KaiRuntimeFactory
import kai.domain.result.KaiResult

class RegressCommand {
    fun run(args: List<String>) {
        val options = parseOptions(args)
        when (val replays = FindingService(KaiRuntimeFactory.create(options.storageDir, options.pluginDir)).regress()) {
            is KaiResult.Success -> {
                println("regressing ${replays.value.size} finding(s)")
                replays.value.forEach { replay ->
                    println("${replay.finding.id.value}: ${replay.results.joinToString { "${it.profileName}=${it.exitCode}" }}")
                }
            }
            is KaiResult.Failure -> error(replays.message)
        }
    }
}
