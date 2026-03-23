package kai.plugins.scheduler.roundrobin

import kai.domain.campaign.CampaignStats
import kai.domain.id.StrategyId
import kai.plugin.scheduler.SchedulerPlugin
import kai.plugin.strategy.FitnessSignal

class RoundRobinScheduler : SchedulerPlugin {
    override fun next(
        strategies: List<StrategyId>,
        signals: Map<StrategyId, List<FitnessSignal>>,
        stats: CampaignStats
    ): StrategyId {
        require(strategies.isNotEmpty()) { "No strategies configured" }
        return strategies[stats.totalGenerated % strategies.size]
    }
}
