package kai.plugin.scheduler

import kai.domain.campaign.CampaignStats
import kai.domain.id.StrategyId
import kai.plugin.strategy.FitnessSignal

interface SchedulerPlugin {
    fun next(
        strategies: List<StrategyId>,
        signals: Map<StrategyId, List<FitnessSignal>>,
        stats: CampaignStats
    ): StrategyId
}
