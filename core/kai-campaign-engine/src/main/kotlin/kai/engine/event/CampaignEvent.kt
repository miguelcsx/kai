package kai.engine.event

import kai.domain.campaign.CampaignState
import kai.domain.finding.Finding
import kai.domain.testcase.TestCase

sealed class CampaignEvent {
    data class Started(val state: CampaignState) : CampaignEvent()
    data class TestCaseGenerated(val testCase: TestCase) : CampaignEvent()
    data class FindingStored(val finding: Finding) : CampaignEvent()
    data class StateSaved(val state: CampaignState) : CampaignEvent()
    data class Finished(val state: CampaignState) : CampaignEvent()
}
