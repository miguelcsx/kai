package kai.plugins.strategy.mutation

import kai.domain.id.InterfaceVersion
import kai.domain.id.StrategyId
import kai.domain.observations.Observations
import kai.domain.testcase.Provenance
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase
import kai.plugin.strategy.FitnessSignal
import kai.plugin.strategy.GenerationContext
import kai.plugin.strategy.MutationContext
import kai.plugin.strategy.StrategyPlugin

class MutationStrategyPlugin : StrategyPlugin {
    override val id = StrategyId("ast-mutator")
    override val version = InterfaceVersion.V1

    override fun generate(context: GenerationContext): Sequence<TestCase> {
        val base = context.corpusSample.firstOrNull() ?: return sequenceOf(seedCase(context))
        val mutant = mutate(
            base,
            MutationContext(context.campaignId, context.seed, context.compilerProfiles, context.iteration + 1)
        )
        return sequenceOf(mutant)
    }

    override fun mutate(seed: TestCase, context: MutationContext): TestCase {
        val source = seed.sources.first()
        val content = source.content + "\nfun marker${context.iteration}() { val unusedMutation = ${context.iteration} }"
        return TestCase.create(
            sources = listOf(SourceFile.create(source.relativePath, content)),
            buildConfig = seed.buildConfig,
            provenance = seed.provenance.copy(
                strategyId = id,
                parentId = seed.id,
                generationSeed = context.seed,
                mutationDepth = seed.provenance.mutationDepth + 1
            )
        )
    }

    override fun feedback(observations: Observations, testCase: TestCase): FitnessSignal {
        return FitnessSignal(observations.results.size.toDouble(), "profiles=${observations.results.size}")
    }

    private fun seedCase(context: GenerationContext): TestCase {
        return TestCase.create(
            sources = listOf(SourceFile.create("Main.kt", "fun main() { println(\"seed\") }")),
            buildConfig = kai.domain.testcase.BuildConfig.create(
                compilerProfiles = context.compilerProfiles
            ),
            provenance = Provenance(
                campaignId = context.campaignId,
                strategyId = id,
                parentId = null,
                generationSeed = context.seed,
                mutationDepth = 0
            )
        )
    }
}
