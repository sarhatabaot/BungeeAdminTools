package fr.alphart.bungeeadmintools.modules.comment;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.i18n.I18n;
import fr.alphart.bungeeadmintools.Permissions;
import fr.alphart.bungeeadmintools.modules.core.PermissionManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.alphart.bungeeadmintools.i18n.I18n.formatWithColor;
import static fr.alphart.bungeeadmintools.i18n.I18n.formatWithColorAndAddPrefix;


@CommandAlias("comment|note")
@CommandPermission(Permissions.COMMENT)
public class CommentCommand extends BaseCommand {
    private final Comment comment;

    public CommentCommand(final Comment comment) {
        this.comment = comment;
    }

    @Default
    @CommandPermission(Permissions.COMMENT_CREATE)
    public void onAddComment(final CommandSender sender, final String target, final String reason) {
        checkArgument(comment.hasLastcommentCooledDown(target), formatWithColor("cooldownUnfinished"));
        comment.insertComment(target, reason, CommentEntry.Type.NOTE, sender.getName());
        sender.sendMessage(formatWithColorAndAddPrefix("commentAdded"));
    }

    @Subcommand("clear")
    @CommandAlias("clearcomment")
    @Description("Clear all the comments and warnings or the specified one of the player.")
    @CommandPermission(Permissions.COMMENT_CLEAR)
    public void onClearComment(final CommandSender sender, final String target, final int commentId) {
        final String commentMessage = comment.clearComments(target, commentId);
        sender.sendMessage(BungeeAdminToolsPlugin.colorizeAndAddPrefix(commentMessage));
    }

    @CommandAlias("warn")
    @Description("Warn a player and add warning note on player's info text.")
    @CommandPermission(Permissions.WARN)
    public void onWarn(final CommandSender sender, final String target, final String reason) {
        final ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(target);
        if(targetPlayer == null) {
            sender.sendMessage(I18n.formatWithColor("operationUnknownPlayer", new String[]{target}));
            return;
        }
        checkArgument(comment.hasLastcommentCooledDown(target), formatWithColor("cooldownUnfinished"));
        comment.insertComment(target, reason, CommentEntry.Type.WARNING, sender.getName());
        targetPlayer.sendMessage(I18n.formatWithColorAndAddPrefix("wasWarnedNotif", new String[]{reason}));

        BungeeAdminToolsPlugin.broadcast(I18n.formatWithColor("warnBroadcast", new String[]{target, sender.getName(), reason}), PermissionManager.Action.WARN_BROADCAST.getPermission());
    }

    @HelpCommand
    public void onHelp(CommandHelp help) {
        help.showHelp();
    }
}
