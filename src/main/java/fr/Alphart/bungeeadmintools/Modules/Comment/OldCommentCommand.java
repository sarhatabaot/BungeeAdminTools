package fr.alphart.bungeeadmintools.modules.comment;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColor;
import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColorAndAddPrefix;


import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.I18n.I18n;
import fr.alphart.bungeeadmintools.modules.BATCommand;
import fr.alphart.bungeeadmintools.modules.CommandHandler;
import fr.alphart.bungeeadmintools.modules.InvalidModuleException;
import fr.alphart.bungeeadmintools.modules.core.Core;
import fr.alphart.bungeeadmintools.modules.core.PermissionManager;
import fr.alphart.bungeeadmintools.utils.FormatUtils;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.google.common.base.Joiner;




public class OldCommentCommand extends CommandHandler {
	private static Comment comment;
	
	protected OldCommentCommand(final Comment commentModule) {
		super(commentModule);
		comment = commentModule;
	}
	
	@BATCommand.RunAsync
	public static class AddCommentCmd extends BATCommand{
		public AddCommentCmd() { 
			super("comment", "<entity> <reason>", "Write a comment about the player.", "bat.comment.create", "note");
			// We need this command to handle the /comment help
			setMinArgs(1);
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			if (args[0].equals("help")) {
				try {
					FormatUtils.showFormattedHelp(BungeeAdminToolsPlugin.getInstance().getModules().getModule("comment").getCommands(),
							sender, "COMMENT");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
			if(args.length < 2){
				throw new IllegalArgumentException();
			}
			if(!confirmedCmd && Core.getPlayerIP(args[0]).equals("0.0.0.0")){
				mustConfirmCommand(sender, "bat " + getName() + " " + Joiner.on(' ').join(args),
						I18n.formatWithColor("operationUnknownPlayer", new String[] {args[0]}));
				return;
			}
			
			checkArgument(comment.hasLastcommentCooledDown(args[0]), formatWithColor("cooldownUnfinished"));
			comment.insertComment(args[0], Utils.getFinalArg(args, 1), CommentEntry.Type.NOTE, sender.getName());
			sender.sendMessage(formatWithColorAndAddPrefix("commentAdded"));
		}
	}
	
	@BATCommand.RunAsync
	public static class ClearCommentCmd extends BATCommand {
		public ClearCommentCmd() { super("clearcomment", "<entity> [commentID]", "Clear all the comments and warnings or the specified one of the player.", "bat.comment.clear");}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
			sender.sendMessage(BungeeAdminToolsPlugin.colorizeAndAddPrefix(comment.clearComments(args[0], ((args.length == 2) ? Integer.parseInt(args[1]) : -1) )));
		}
	}
	
	@BATCommand.RunAsync
	public static class WarnCmd extends BATCommand {
		public WarnCmd() { super("warn", "<player> <reason>", "Warn a player and add warning note on player's info text.", PermissionManager.Action.WARN.getPermission());}

		@Override
		public void onCommand(CommandSender sender, String[] args, boolean confirmedCmd)
				throws IllegalArgumentException {
			final ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
			final String reason = Utils.getFinalArg(args, 1);
			if(target == null){
				if(!confirmedCmd && Core.getPlayerIP(args[0]).equals("0.0.0.0")){
					mustConfirmCommand(sender, getName() + " " + Joiner.on(' ').join(args),
							I18n.formatWithColor("operationUnknownPlayer", new String[] {args[0]}));
					return;
				}
			}
			
			if(sender instanceof ProxiedPlayer){
				checkArgument(PermissionManager.canExecuteAction(PermissionManager.Action.WARN , sender, ((ProxiedPlayer)sender).getServer().getInfo().getName()),
						formatWithColor("noPerm"));
			}
	          checkArgument(comment.hasLastcommentCooledDown(args[0]), formatWithColor("cooldownUnfinished"));
			comment.insertComment(args[0], reason, CommentEntry.Type.WARNING, sender.getName());
			if(target != null){
			  target.sendMessage(I18n.formatWithColorAndAddPrefix("wasWarnedNotif", new String[] {reason}));
			}
			  
			BungeeAdminToolsPlugin.broadcast(I18n.formatWithColor("warnBroadcast", new String[]{args[0], sender.getName(), reason}), PermissionManager.Action.WARN_BROADCAST.getPermission());
		}
	}
}
