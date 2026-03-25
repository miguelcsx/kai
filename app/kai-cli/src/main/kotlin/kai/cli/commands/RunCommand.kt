package kai.cli.commands

import kai.cli.app.CampaignService
import kai.cli.app.EventPrinter
import kai.cli.app.KaiRuntimeFactory

class RunCommand {
    fun run(args: List<String>) {
        val options = parseOptions(args)
        require(options.positional.isNotEmpty()) { "run requires a config path" }
        val runtime = KaiRuntimeFactory.create(options.storageDir, options.pluginDir)
        CampaignService(runtime).run(options.positional[0]) {
            EventPrinter.emit(it, options.json)
        }
    }
}
