package kai.domain.campaign

import kai.domain.execution.ExecutionConfig
import kai.domain.id.CampaignId
import kai.domain.id.ExecutorId
import kai.domain.id.OracleId
import kai.domain.id.ReducerId
import kai.domain.id.StrategyId
import kai.domain.testcase.CompilerProfile

data class CampaignBudget(
    val maxIterations: Int,
    val maxFindings: Int,
    val maxReductionIterations: Int
) {
    companion object {
        fun create(
            maxIterations: Int,
            maxFindings: Int,
            maxReductionIterations: Int
        ): CampaignBudget {
            require(maxIterations > 0) { "maxIterations must be positive" }
            require(maxFindings > 0) { "maxFindings must be positive" }
            require(maxReductionIterations > 0) { "maxReductionIterations must be positive" }
            return CampaignBudget(maxIterations, maxFindings, maxReductionIterations)
        }
    }
}

data class CampaignConfig(
    val id: CampaignId,
    val strategyIds: List<StrategyId>,
    val executorId: ExecutorId,
    val oracleIds: List<OracleId>,
    val reducerIds: List<ReducerId>,
    val compilerProfiles: List<CompilerProfile>,
    val budget: CampaignBudget,
    val seedCorpusIds: List<String>,
    val executionConfig: ExecutionConfig
)

enum class CampaignStatus {
    INITIALIZING,
    RUNNING,
    FINISHED,
    FAILED
}

data class CampaignStats(
    val totalGenerated: Int,
    val totalExecuted: Int,
    val totalInteresting: Int
) {
    companion object {
        fun zero(): CampaignStats {
            return CampaignStats(0, 0, 0)
        }
    }
}

data class CampaignState(
    val id: CampaignId,
    val config: CampaignConfig,
    val status: CampaignStatus,
    val stats: CampaignStats
) {
    companion object {
        fun initial(config: CampaignConfig): CampaignState {
            return CampaignState(config.id, config, CampaignStatus.INITIALIZING, CampaignStats.zero())
        }
    }
}
