package fr.alphart.bungeeadmintools.modules.core;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.I18n.I18n;
import fr.alphart.bungeeadmintools.Permissions;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.CommandSender;

import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColorAndAddPrefix;

@CommandAlias("bat")
public class CoreCommand extends BaseCommand {
    private final BungeeAdminToolsPlugin plugin;

    public CoreCommand(final BungeeAdminToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission(Permissions.RELOAD)
    public void onReload(final CommandSender sender) {
        try {
            BungeeAdminToolsPlugin.getInstance().getConfiguration().reload();
        } catch (InvalidConfigurationException e) {
            BungeeAdminToolsPlugin.getInstance().getLogger().severe("Error during reload of main configuration :");
            e.printStackTrace();
        }
        I18n.reload();
        BungeeAdminToolsPlugin.getInstance().getModules().unloadModules();
        BungeeAdminToolsPlugin.getInstance().getModules().loadModules();
        sender.sendMessage(formatWithColorAndAddPrefix("Reload successfully executed ..."));
    }

    @Subcommand("version")
    @CommandPermission(Permissions.VERSION)
    public void onVersion(final CommandSender sender) {
        sender.sendMessage("BAT version "+plugin.getDescription().getVersion());
    }

    @HelpCommand
    @CommandPermission(Permissions.HELP)
    public void onHelp(CommandHelp help){
        help.showHelp();
    }
}
