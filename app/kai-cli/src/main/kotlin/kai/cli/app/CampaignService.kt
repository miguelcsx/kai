package kai.cli.app

import java.nio.file.Paths
import kai.domain.result.KaiResult
import kai.engine.event.CampaignEvent

class CampaignService(
    private val runtime: KaiRuntime
) {
    fun run(configPath: String, onEvent: (CampaignEvent) -> Unit) {
        when (val config = runtime.parser.parse(Paths.get(configPath))) {
            is KaiResult.Success -> when (val run = runtime.engine.run(config.value, onEvent)) {
                is KaiResult.Success -> Unit
                is KaiResult.Failure -> error(run.message)
            }
            is KaiResult.Failure -> error(config.message)
        }
    }
}
