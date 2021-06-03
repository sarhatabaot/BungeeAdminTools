package fr.alphart.bungeeadmintools.modules.mute;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.Permissions;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.modules.core.Core;
import fr.alphart.bungeeadmintools.modules.core.PermissionManager;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.CommandSender;


import static com.google.common.base.Preconditions.checkArgument;
import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColor;

public class MuteCommand extends BaseCommand {
    private final Mute mute;

    public MuteCommand(final Mute mute) {
        this.mute = mute;
    }

    @CommandAlias("mute|gmute")
    @CommandPermission(Permissions.MUTE)
    public void onMute(final CommandSender sender, final String target, final String reason){
        checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.MUTE, target), formatWithColor("isExempt"));
        checkArgument(!mute.isMute(target), formatWithColor("alreadyMute"));

        final String muteMessage =  mute.mute(target, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
        BungeeAdminToolsPlugin.broadcast(muteMessage, PermissionManager.Action.MUTE_BROADCAST.getPermission());
    }


    @CommandAlias("muteip|gmuteip")
    @CommandPermission(Permissions.MUTE_IP)
    public void onMuteIp(final CommandSender sender, final String targetIp, final String reason) {
        if (!Utils.validIP(targetIp)) {
            String ip = Core.getPlayerIP(targetIp);
            checkArgument(!"0.0.0.0".equals(ip), formatWithColor("ipUnknownPlayer"));
        }

        checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.MUTE, targetIp), formatWithColor("isExempt"));
        checkArgument(!mute.isMute(targetIp), formatWithColor("alreadyMute"));
        final String muteMessage =  mute.mute(targetIp, IModule.GLOBAL_SERVER, sender.getName(), 0, reason);
        BungeeAdminToolsPlugin.broadcast(muteMessage, PermissionManager.Action.MUTE_BROADCAST.getPermission());
    }

    @CommandAlias("tempmute|gtempmute")
    @Description("Temporarily mute the player on name basis on all servers (the whole network) for duration. No player logged in with that IP will be able to speak.")
    @CommandPermission(Permissions.TEMP_MUTE)
    public void onTempMute(final CommandSender sender, final String target, final String duration, final String reason) {
        final long expirationTimestamp = Utils.parseDuration(duration);
        checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.MUTE, target), formatWithColor("isExempt"));
        checkArgument(!mute.isMute(target), formatWithColor("alreadyMute"));

        final String muteMessage =  mute.mute(target, IModule.GLOBAL_SERVER, sender.getName(), expirationTimestamp, reason);
        BungeeAdminToolsPlugin.broadcast(muteMessage, PermissionManager.Action.MUTE_BROADCAST.getPermission());
    }

    @CommandAlias("tempmuteip|gtempmuteip")
    @Description("Temporarily mute the player on IP basis on all servers (the whole network) for duration. No player logged in with that IP will be able to speak.")
    @CommandPermission(Permissions.TEMP_MUTE_IP)
    public void onTempMuteIp(final CommandSender sender, final String targetIp, final String duration, final String reason) {
        final long expirationTimestamp = Utils.parseDuration(duration);
        if (!Utils.validIP(targetIp)) {
            String ip = Core.getPlayerIP(targetIp);
            checkArgument(!"0.0.0.0".equals(ip), formatWithColor("ipUnknownPlayer"));
        }
        checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.MUTE, targetIp), formatWithColor("isExempt"));
        checkArgument(!mute.isMute(targetIp), formatWithColor("alreadyMute"));

        final String muteMessage =  mute.mute(targetIp, IModule.GLOBAL_SERVER, sender.getName(), expirationTimestamp, reason);
        BungeeAdminToolsPlugin.broadcast(muteMessage, PermissionManager.Action.MUTE_BROADCAST.getPermission());
    }

    @CommandAlias("unmute|gunmute")
    @CommandPermission(Permissions.UN_MUTE)
    @Description("Unmute the player on an name basis from all servers (the whole network).")
    public void unMute(final CommandSender sender, final String target, final String reason) {
        final String unMuteMessage = mute.unMute(target,IModule.GLOBAL_SERVER, sender.getName(),reason);
        BungeeAdminToolsPlugin.broadcast(unMuteMessage,PermissionManager.Action.MUTE_BROADCAST.getPermission());
    }

    @CommandAlias("unmuteip|gunmuteip")
    @CommandPermission(Permissions.UN_MUTE_IP)
    @Description("Unmute the player on an IP basis from all servers (the whole network).")
    public void onUnMuteIp(final CommandSender sender, final String targetIp, final String reason) {
        if (!Utils.validIP(targetIp)) {
            String ip = Core.getPlayerIP(targetIp);
            checkArgument(!"0.0.0.0".equals(ip), formatWithColor("ipUnknownPlayer"));
        }

        final String unMuteMessage = mute.unMute(targetIp,IModule.GLOBAL_SERVER, sender.getName(),reason);
        BungeeAdminToolsPlugin.broadcast(unMuteMessage,PermissionManager.Action.MUTE_BROADCAST.getPermission());
    }
}
