package kai.storage.api

import kai.domain.campaign.CampaignState
import kai.domain.finding.Finding
import kai.domain.finding.FindingKind
import kai.domain.finding.FindingStatus
import kai.domain.id.CampaignId
import kai.domain.id.FindingId
import kai.domain.id.OracleId
import kai.domain.id.StrategyId
import kai.domain.id.TestCaseId
import kai.domain.testcase.TestCase

data class CorpusQuery(
    val strategyId: StrategyId? = null,
    val parentId: TestCaseId? = null
)

data class FindingQuery(
    val status: FindingStatus? = null,
    val oracleId: OracleId? = null,
    val kind: FindingKind? = null
)

interface CorpusStore {
    fun saveTestCase(testCase: TestCase): Result<TestCaseId>
    fun loadTestCase(id: TestCaseId): Result<TestCase?>
    fun listTestCases(): Result<List<TestCase>>
    fun findTestCases(query: CorpusQuery): Result<List<TestCase>>
}

interface FindingsStore {
    fun saveFinding(finding: Finding): Result<FindingId>
    fun loadFinding(id: FindingId): Result<Finding?>
    fun listFindings(): Result<List<Finding>>
    fun findFindings(query: FindingQuery): Result<List<Finding>>
}

interface CampaignStore {
    fun saveCampaign(state: CampaignState): Result<CampaignId>
    fun loadCampaign(id: CampaignId): Result<CampaignState?>
}

data class StoragePorts(
    val corpus: CorpusStore,
    val findings: FindingsStore,
    val campaigns: CampaignStore
)
