package kai.cli.app

import kai.engine.event.CampaignEvent

object EventPrinter {
    fun emit(event: CampaignEvent, json: Boolean) {
        if (json) {
            println(toJson(event))
            return
        }
        println(toText(event))
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
}
