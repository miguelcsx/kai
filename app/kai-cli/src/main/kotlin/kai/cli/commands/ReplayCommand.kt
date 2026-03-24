package kai.cli.commands

import kai.domain.execution.ExecutionConfig

class ReplayCommand {
    fun run(args: List<String>) {
        val options = parseOptions(args)
        require(options.positional.isNotEmpty()) { "replay requires a finding id" }
        val findingId = kai.domain.id.FindingId(options.positional[0])
        val storage = defaultStorage(options.storageDir)
        val registry = defaultRegistry(options.pluginDir)
        val finding = storage.loadFinding(findingId).getOrThrow() ?: error("Unknown finding: ${findingId.value}")
        val campaign = storage.loadCampaign(finding.pending.testCase.provenance.campaignId).getOrThrow()
            ?: error("Missing campaign state for finding ${findingId.value}")
        val executor = registry.resolveExecutor(campaign.config.executorId.value)
        val target = finding.reduced ?: finding.pending.testCase
        val results = executor.execute(target, ExecutionConfig.create(30_000))
        println("replayed ${findingId.value} with ${results.size} profile(s)")
        results.forEach {
            println("${it.profileName}: exit=${it.exitCode}")
        }
    }
}
