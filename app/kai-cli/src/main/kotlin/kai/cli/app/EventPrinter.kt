package kai.cli.app

import kai.engine.event.CampaignEvent

object EventPrinter {
    fun emit(event: CampaignEvent, json: Boolean) {
        println(if (json) toJson(event) else toText(event))
    }

    private fun toText(event: CampaignEvent): String {
        return when (event) {
            is CampaignEvent.Started -> "campaign started: ${event.state.id.value}"
            is CampaignEvent.TestCaseGenerated -> "testcase: ${event.testCase.id.value}"
            is CampaignEvent.FindingStored -> "finding: ${event.finding.id.value}"
            is CampaignEvent.StateSaved -> "state: ${event.state.status} generated=${event.state.stats.totalGenerated}"
            is CampaignEvent.Finished -> "campaign finished: findings=${event.state.stats.totalInteresting}"
            is CampaignEvent.Failed -> "campaign failed: ${event.message}"
        }
    }

    private fun toJson(event: CampaignEvent): String {
        return when (event) {
            is CampaignEvent.Started -> """{"event":"started","campaignId":${json(event.state.id.value)}}"""
            is CampaignEvent.TestCaseGenerated -> """{"event":"testcase","id":${json(event.testCase.id.value)}}"""
            is CampaignEvent.FindingStored -> """{"event":"finding","id":${json(event.finding.id.value)}}"""
            is CampaignEvent.StateSaved -> """{"event":"state","status":${json(event.state.status.name)},"generated":${event.state.stats.totalGenerated}}"""
            is CampaignEvent.Finished -> """{"event":"finished","findings":${event.state.stats.totalInteresting}}"""
            is CampaignEvent.Failed -> """{"event":"failed","campaignId":${json(event.state.id.value)},"message":${json(event.message)}}"""
        }
    }

    private fun json(value: String): String {
        return buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }
    }
}
