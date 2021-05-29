package fr.alphart.bungeeadmintools.modules.core;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;

//TODO
@CommandAlias("bat")
public class CoreCommand extends BaseCommand {

    @Subcommand("reload")
    @CommandPermission("bat.reload")
    public void onReload() {

    }

    @Subcommand("modules")
    @CommandPermission("bat.modules")
    public void onModules() {

    }

    @Subcommand("version")
    @CommandPermission("bat.version")
    public void onVersion() {

    }

    @Subcommand("backup")
    @CommandPermission("bat.backup")
    public void onBackup() {

    }

    @HelpCommand
    @CommandPermission("bat.help")
    public void onHelp(){


    }
}
