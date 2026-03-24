package kai.cli.commands

class RegressCommand {
    fun run(args: List<String>) {
        val options = parseOptions(args)
        val storage = defaultStorage(options.storageDir)
        val registry = defaultRegistry(options.pluginDir)
        val findings = storage.listFindings().getOrThrow()
        println("regressing ${findings.size} finding(s)")
        findings.forEach { finding ->
            val campaign = storage.loadCampaign(finding.pending.testCase.provenance.campaignId).getOrThrow()
                ?: error("Missing campaign state for finding ${finding.id.value}")
            val executor = registry.resolveExecutor(campaign.config.executorId.value)
            val testCase = finding.reduced ?: finding.pending.testCase
            val results = executor.execute(testCase, finding.pending.testCase.buildConfig.let {
                kai.domain.execution.ExecutionConfig.create(30_000)
            })
            println("${finding.id.value}: ${results.joinToString { "${it.profileName}=${it.exitCode}" }}")
        }
    }
}
