package kai.cli.commands

import kai.cli.app.FindingService
import kai.cli.app.KaiRuntimeFactory
import kai.domain.result.KaiResult

class ReplayCommand {
    fun run(args: List<String>) {
        val options = parseOptions(args)
        require(options.positional.isNotEmpty()) { "replay requires a finding id" }
        when (val replay = FindingService(KaiRuntimeFactory.create(options.storageDir, options.pluginDir)).replay(options.positional[0])) {
            is KaiResult.Success -> {
                println("replayed ${replay.value.finding.id.value} with ${replay.value.results.size} profile(s)")
                replay.value.results.forEach {
                    println("${it.profileName}: exit=${it.exitCode}")
                }
            }
            is KaiResult.Failure -> error(replay.message)
        }
    }
}
