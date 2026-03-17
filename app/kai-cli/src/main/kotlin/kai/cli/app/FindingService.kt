package kai.cli.app

import kai.domain.finding.Finding
import kai.domain.id.FindingId
import kai.domain.observations.ExecutionResult

class FindingService(
    private val runtime: KaiRuntime
) {
    fun replay(findingId: String): FindingReplay {
        val finding = loadFinding(findingId)
        val campaign = loadCampaign(finding)
        val executor = runtime.registry.resolveExecutor(campaign.config.executorId.value)
        val target = finding.reduced ?: finding.pending.testCase
        val results = executor.execute(target, campaign.config.executionConfig)
        return FindingReplay(finding, results)
    }

    fun regress(): List<FindingReplay> {
        return runtime.storage.listFindings().getOrThrow().map { finding ->
            val campaign = loadCampaign(finding)
            val executor = runtime.registry.resolveExecutor(campaign.config.executorId.value)
            val target = finding.reduced ?: finding.pending.testCase
            FindingReplay(finding, executor.execute(target, campaign.config.executionConfig))
        }
    }

    private fun loadFinding(id: String): Finding {
        val findingId = FindingId(id)
        return runtime.storage.loadFinding(findingId).getOrThrow()
            ?: error("Unknown finding: ${findingId.value}")
    }

    private fun loadCampaign(finding: Finding): kai.domain.campaign.CampaignState {
        val campaignId = finding.pending.testCase.provenance.campaignId
        return runtime.storage.loadCampaign(campaignId).getOrThrow()
            ?: error("Missing campaign state for finding ${finding.id.value}")
    }
}

data class FindingReplay(
    val finding: Finding,
    val results: List<ExecutionResult>
)
