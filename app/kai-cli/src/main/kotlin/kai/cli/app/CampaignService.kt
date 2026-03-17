package kai.cli.app

import java.nio.file.Paths
import kai.engine.event.CampaignEvent

class CampaignService(
    private val runtime: KaiRuntime
) {
    fun run(configPath: String, onEvent: (CampaignEvent) -> Unit) {
        val config = runtime.parser.parse(Paths.get(configPath))
        runtime.engine.run(config, onEvent).getOrThrow()
    }
}
