package fr.alphart.bungeeadmintools.modules.core;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;


public class LookupCommand extends BaseCommand {
    private LookupFormatter lookupFormatter;

    public LookupCommand() {
        lookupFormatter = new LookupFormatter();
    }

    @CommandAlias("lookup")
    @CommandPermission("bat.lookup")
    public void onLookup(final CommandSender sender, final String target, final String module,@Optional int page) {
        if(page == 0) {
            for (final BaseComponent[] msg : lookupFormatter.getSummaryLookupPlayer(target,
                    sender.hasPermission(PermissionManager.Action.LOOKUP.getPermission() + ".displayip"))) {
                sender.sendMessage(msg);
            }
            return;
        }

        if(page < 0) {
            page = 1;
        }


    }

    @CommandAlias("stafflookup")
    @CommandPermission("bat.stafflookup")
    public void onStaffLookup(final CommandSender sender, final String staff, final String module,@Optional int page) {
        if(page == 0) {
            for (final BaseComponent[] msg : lookupFormatter.getSummaryStaffLookup(staff,
                    sender.hasPermission(PermissionManager.Action.LOOKUP.getPermission() + ".displayip"))) {
                sender.sendMessage(msg);
            }
            return;
        }

        if(page < 0) {
            page = 1;
        }
    }
}
