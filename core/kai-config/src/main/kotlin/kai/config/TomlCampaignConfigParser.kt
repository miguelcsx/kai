package kai.config

import java.nio.file.Path
import kai.domain.campaign.CampaignBudget
import kai.domain.campaign.CampaignConfig
import kai.domain.execution.ExecutionConfig
import kai.domain.id.CampaignId
import kai.domain.id.ExecutorId
import kai.domain.id.OracleId
import kai.domain.id.ReducerId
import kai.domain.id.StrategyId
import kai.domain.testcase.CompilerProfile
import org.tomlj.Toml
import org.tomlj.TomlArray
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable

class TomlCampaignConfigParser {
    fun parse(path: Path): CampaignConfig {
        val result = Toml.parse(path)
        validate(result)

        val campaign = table(result, "campaign")
        val budget = table(result, "campaign.budget")
        val strategy = table(result, "campaign.strategy")
        val executor = table(result, "campaign.executor")
        val oracles = table(result, "campaign.oracles")
        val reducers = table(result, "campaign.reducers")
        val seeds = table(result, "campaign.seeds")
        val execution = table(result, "campaign.execution")

        return CampaignConfig(
            id = CampaignId(string(campaign, "id")),
            strategyIds = strings(strategy, "ids").map(::StrategyId),
            executorId = ExecutorId(string(executor, "id")),
            oracleIds = strings(oracles, "ids").map(::OracleId),
            reducerIds = strings(reducers, "ids").map(::ReducerId),
            compilerProfiles = profiles(result),
            budget = CampaignBudget.create(
                maxIterations = long(budget, "max_iterations").toInt(),
                maxFindings = long(budget, "max_findings").toInt()
            ),
            seedCorpusIds = strings(seeds, "corpus_ids"),
            executionConfig = ExecutionConfig.create(
                timeoutMillis = long(execution, "timeout_ms")
            )
        )
    }

    private fun validate(result: TomlParseResult) {
        require(!result.hasErrors()) {
            result.errors().joinToString("\n") { it.toString() }
        }
    }

    private fun profiles(result: TomlParseResult): List<CompilerProfile> {
        val ids = strings(table(result, "campaign.profiles"), "ids")
        require(ids.isNotEmpty()) { "campaign.profiles.ids must not be empty" }
        return ids.map { profileId ->
            val table = table(result, "campaign.profile.$profileId")
            CompilerProfile.create(
                name = profileId,
                binary = string(table, "binary"),
                flags = strings(table, "flags")
            )
        }
    }

    private fun table(result: TomlParseResult, key: String): TomlTable {
        return requireNotNull(result.getTable(key)) { "Missing TOML table: $key" }
    }

    private fun string(table: TomlTable, key: String): String {
        return requireNotNull(table.getString(key)) { "Missing TOML string: $key" }
    }

    private fun long(table: TomlTable, key: String): Long {
        return requireNotNull(table.getLong(key)) { "Missing TOML number: $key" }
    }

    private fun strings(table: TomlTable, key: String): List<String> {
        val array = requireNotNull(table.getArray(key)) { "Missing TOML array: $key" }
        return stringValues(array, key)
    }

    private fun stringValues(array: TomlArray, key: String): List<String> {
        return (0 until array.size()).map { index ->
            requireNotNull(array.getString(index)) {
                "Expected string at $key[$index]"
            }
        }
    }
}
