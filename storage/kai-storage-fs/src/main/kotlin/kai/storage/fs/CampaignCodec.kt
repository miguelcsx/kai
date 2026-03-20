package kai.storage.fs

import kai.domain.campaign.CampaignBudget
import kai.domain.campaign.CampaignConfig
import kai.domain.campaign.CampaignState
import kai.domain.campaign.CampaignStats
import kai.domain.campaign.CampaignStatus
import kai.domain.execution.ExecutionConfig
import kai.domain.id.CampaignId
import kai.domain.id.ExecutorId
import kai.domain.id.OracleId
import kai.domain.id.ReducerId
import kai.domain.id.StrategyId
import kai.domain.testcase.CompilerProfile

object CampaignCodec {
    fun encode(state: CampaignState): JsonValue {
        return JsonValue.Obj(
            linkedMapOf(
                "id" to JsonValue.Str(state.id.value),
                "status" to JsonValue.Str(state.status.name),
                "config" to encodeConfig(state.config),
                "stats" to JsonValue.Obj(
                    linkedMapOf(
                        "totalGenerated" to JsonValue.Num(state.stats.totalGenerated.toLong()),
                        "totalExecuted" to JsonValue.Num(state.stats.totalExecuted.toLong()),
                        "totalInteresting" to JsonValue.Num(state.stats.totalInteresting.toLong())
                    )
                )
            )
        )
    }

    fun decode(value: JsonValue): CampaignState {
        val config = decodeConfig(value.get("config"))
        val statsNode = value.obj("stats")
        return CampaignState(
            id = CampaignId(value.string("id")),
            config = config,
            status = CampaignStatus.valueOf(value.string("status")),
            stats = CampaignStats(
                totalGenerated = statsNode.values.getValue("totalGenerated").asLong().toInt(),
                totalExecuted = statsNode.values.getValue("totalExecuted").asLong().toInt(),
                totalInteresting = statsNode.values.getValue("totalInteresting").asLong().toInt()
            )
        )
    }

    private fun encodeConfig(config: CampaignConfig): JsonValue {
        return JsonValue.Obj(
            linkedMapOf(
                "id" to JsonValue.Str(config.id.value),
                "strategyIds" to JsonValue.Arr(config.strategyIds.map { JsonValue.Str(it.value) }),
                "executorId" to JsonValue.Str(config.executorId.value),
                "oracleIds" to JsonValue.Arr(config.oracleIds.map { JsonValue.Str(it.value) }),
                "reducerIds" to JsonValue.Arr(config.reducerIds.map { JsonValue.Str(it.value) }),
                "compilerProfiles" to JsonValue.Arr(config.compilerProfiles.map { encodeProfile(it) }),
                "seedCorpusIds" to JsonValue.Arr(config.seedCorpusIds.map { JsonValue.Str(it) }),
                "budget" to JsonValue.Obj(
                    linkedMapOf(
                        "maxIterations" to JsonValue.Num(config.budget.maxIterations.toLong()),
                        "maxFindings" to JsonValue.Num(config.budget.maxFindings.toLong())
                    )
                ),
                "execution" to JsonValue.Obj(
                    linkedMapOf(
                        "timeoutMillis" to JsonValue.Num(config.executionConfig.timeoutMillis)
                    )
                )
            )
        )
    }

    private fun decodeConfig(value: JsonValue): CampaignConfig {
        val budget = value.obj("budget")
        val execution = value.obj("execution")
        return CampaignConfig(
            id = CampaignId(value.string("id")),
            strategyIds = value.array("strategyIds").map { StrategyId(it.asString()) },
            executorId = ExecutorId(value.string("executorId")),
            oracleIds = value.array("oracleIds").map { OracleId(it.asString()) },
            reducerIds = value.array("reducerIds").map { ReducerId(it.asString()) },
            compilerProfiles = value.array("compilerProfiles").map { decodeProfile(it) },
            budget = CampaignBudget.create(
                maxIterations = budget.values.getValue("maxIterations").asLong().toInt(),
                maxFindings = budget.values.getValue("maxFindings").asLong().toInt()
            ),
            seedCorpusIds = value.array("seedCorpusIds").map { it.asString() },
            executionConfig = ExecutionConfig.create(execution.values.getValue("timeoutMillis").asLong())
        )
    }

    private fun encodeProfile(profile: CompilerProfile): JsonValue {
        return JsonValue.Obj(
            linkedMapOf(
                "name" to JsonValue.Str(profile.name),
                "binary" to JsonValue.Str(profile.binary),
                "flags" to JsonValue.Arr(profile.flags.map { JsonValue.Str(it) })
            )
        )
    }

    private fun decodeProfile(value: JsonValue): CompilerProfile {
        val node = value.asObject()
        return CompilerProfile.create(
            name = node.values.getValue("name").asString(),
            binary = node.values.getValue("binary").asString(),
            flags = node.values.getValue("flags").asArray().map { it.asString() }
        )
    }
}
