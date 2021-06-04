package fr.alphart.bungeeadmintools.modules.kick;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import com.google.common.base.Preconditions;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.i18n.I18n;
import fr.alphart.bungeeadmintools.Permissions;
import fr.alphart.bungeeadmintools.modules.core.PermissionManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class KickCommand extends BaseCommand {
    private final Kick kick;

    public KickCommand(final Kick kick) {
        this.kick = kick;
    }

    @CommandAlias("kick")
    @Description("Kick the player from his current server to the lobby")
    @CommandPermission(Permissions.KICK)
    public void onKick(final CommandSender sender, final String target, final String reason) {
        final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
        final String playerServer = player.getServer().getInfo().getName();
        Preconditions.checkArgument(
                playerServer != null && !playerServer.equals(player.getPendingConnection().getListener().getDefaultServer()),
                I18n.formatWithColor("cantKickDefaultServer", new String[]{target}));

        Preconditions.checkArgument(
                PermissionManager.canExecuteAction(PermissionManager.Action.KICK, sender, player.getServer().getInfo().getName()),
                I18n.formatWithColor("noPerm"));

        Preconditions.checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.KICK, target), I18n.formatWithColor("isExempt"));

        final String kickMessage = kick.kick(player, sender.getName(), reason);
        BungeeAdminToolsPlugin.broadcast(kickMessage, PermissionManager.Action.KICK_BROADCAST.getPermission());

    }

    @CommandAlias("gkick")
    @Description("Kick the player from the network")
    @CommandPermission(Permissions.KICK_GLOBAL)
    public void onKickGlobal(final CommandSender sender, final String target, final String reason) {
        final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
        Preconditions.checkArgument(player != null, I18n.formatWithColor("playerNotFound"));

        Preconditions.checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.KICK, target), I18n.formatWithColor("isExempt"));

        final String kickMessage = kick.gKick(player, sender.getName(), reason);
        BungeeAdminToolsPlugin.broadcast(kickMessage, PermissionManager.Action.KICK_BROADCAST.getPermission());
    }

}

