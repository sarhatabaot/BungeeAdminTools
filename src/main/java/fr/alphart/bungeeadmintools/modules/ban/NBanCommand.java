package fr.alphart.bungeeadmintools.modules.ban;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import com.google.common.base.Preconditions;
import fr.alphart.bungeeadmintools.BAT;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.modules.core.PermissionManager;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColor;


public class NBanCommand extends BaseCommand {
    private final Ban ban;

    public NBanCommand(final Ban ban) {
        this.ban = ban;
    }

    @CommandAlias("ban")
    @CommandPermission("bat.ban")
    public void onBan(final CommandSender sender, final String target, final String reason) {
        final ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(target);

        Preconditions.checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.BAN, targetPlayer));
        Preconditions.checkArgument(!ban.isBan(targetPlayer), formatWithColor("alreadyBan"));

        final String banMessage = ban.ban(target, IModule.GLOBAL_SERVER,sender.getName(), 0 ,reason);
        BAT.broadcast(banMessage,PermissionManager.Action.BAN_BROADCAST.getPermission());
    }
    @CommandAlias("banip")
    @CommandPermission("bat.ban.ip")
    public void onBanIP(final CommandSender sender, final String ip, final String reason) {
        if(BAT.getInstance().getConfiguration().getWhitelistedIp().contains(ip)) {
            sender.sendMessage("You cannot ban this ip.");
            return;
        }

        final String banMessage = ban.banIp(ip, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
        BAT.broadcast(banMessage, PermissionManager.Action.BAN_BROADCAST.getPermission());
    }

    @CommandAlias("tempban")
    @CommandPermission("bat.tempban")
    public void onTempBan(final CommandSender sender, final String target, final String duration, final String reason) {
        final ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(target);
        final long expirationTimestamp = Utils.parseDuration(duration);

        Preconditions.checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.BAN, targetPlayer));
        Preconditions.checkArgument(!ban.isBan(targetPlayer), formatWithColor("alreadyBan"));

        final String banMessage = ban.ban(target , IModule.GLOBAL_SERVER, sender.getName(), expirationTimestamp, reason);
        BAT.broadcast(banMessage, PermissionManager.Action.BAN_BROADCAST.getPermission());
    }

    @CommandAlias("tempbanip")
    @CommandPermission("bat.tempbanip")
    public void onTempBanIp(final CommandSender sender, final String ip, final String duration, final String reason) {
        if(BAT.getInstance().getConfiguration().getWhitelistedIp().contains(ip)) {
            sender.sendMessage("You cannot ban this ip.");
            return;
        }

        final long expirationTimestamp = Utils.parseDuration(duration);
        final String banMessage = ban.banIp(ip, IModule.GLOBAL_SERVER, sender.getName(), expirationTimestamp, reason);
        BAT.broadcast(banMessage, PermissionManager.Action.BAN_BROADCAST.getPermission());
    }

    @CommandAlias("unban|pardon")
    @CommandPermission("bat.unban")
    public void onUnban(final CommandSender sender, final String target, final String reason) {
        final String banMessage = ban.unBan(target, IModule.GLOBAL_SERVER, sender.getName(), reason);
        BAT.broadcast(banMessage, PermissionManager.Action.BAN_BROADCAST.getPermission());
    }

    @CommandAlias("unbanip|pardonip")
    @CommandPermission("bat.unbanip")
    public void onUnbanIp(final CommandSender sender, final String ip, final String reason){
        final String banMessage = ban.unBanIP(ip, IModule.GLOBAL_SERVER, sender.getName(), reason);
        BAT.broadcast(banMessage, PermissionManager.Action.BAN_BROADCAST.getPermission());
    }
}
