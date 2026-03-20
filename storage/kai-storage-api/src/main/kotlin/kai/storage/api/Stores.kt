package kai.storage.api

import kai.domain.campaign.CampaignState
import kai.domain.finding.Finding
import kai.domain.id.CampaignId
import kai.domain.id.FindingId
import kai.domain.id.TestCaseId
import kai.domain.testcase.TestCase

interface CorpusStore {
    fun saveTestCase(testCase: TestCase): Result<TestCaseId>
    fun loadTestCase(id: TestCaseId): Result<TestCase?>
    fun listTestCases(): Result<List<TestCase>>
}

interface FindingsStore {
    fun saveFinding(finding: Finding): Result<FindingId>
    fun loadFinding(id: FindingId): Result<Finding?>
    fun listFindings(): Result<List<Finding>>
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
