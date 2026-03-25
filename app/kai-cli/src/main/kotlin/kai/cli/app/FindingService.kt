package kai.cli.app

import kai.domain.finding.Finding
import kai.domain.id.FindingId
import kai.domain.observations.ExecutionResult
import kai.domain.result.KaiResult
import kai.domain.result.kaiResult

class FindingService(
    private val runtime: KaiRuntime
) {
    fun replay(findingId: String): KaiResult<FindingReplay> = kaiResult {
        val finding = loadFinding(findingId)
        val campaign = loadCampaign(finding)
        val executor = runtime.registry.resolveExecutor(campaign.config.executorId.value)
        val target = finding.reduced ?: finding.pending.testCase
        FindingReplay(finding, executor.execute(target, campaign.config.executionConfig))
    }

    fun regress(): KaiResult<List<FindingReplay>> = kaiResult {
        runtime.storage.listFindings().getOrThrow().map { finding ->
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
