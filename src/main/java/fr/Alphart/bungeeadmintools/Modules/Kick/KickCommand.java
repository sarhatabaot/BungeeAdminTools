package fr.alphart.bungeeadmintools.modules.kick;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.UUID;

import com.google.common.base.Preconditions;

import fr.alphart.bungeeadmintools.BAT;
import fr.alphart.bungeeadmintools.I18n.I18n;
import fr.alphart.bungeeadmintools.modules.BATCommand;
import fr.alphart.bungeeadmintools.modules.CommandHandler;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.modules.InvalidModuleException;
import fr.alphart.bungeeadmintools.modules.core.PermissionManager;
import fr.alphart.bungeeadmintools.utils.FormatUtils;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;

public class KickCommand extends CommandHandler {
	private static Kick kick;

	public KickCommand(final Kick kickModule) {
		super(kickModule);
		kick = kickModule;
	}

	@BATCommand.RunAsync
	public static class KickCmd extends BATCommand {
		public KickCmd() {
			super("kick", "<player> [reason]", "Kick the player from his current server to the lobby", PermissionManager.Action.KICK
					.getPermission());
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			if (args[0].equals("help")) {
				try {
					FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("kick").getCommands(),
							sender, "KICK");
				} catch (final InvalidModuleException e) {
					e.printStackTrace();
				}
				return;
			}
                        
            Preconditions.checkArgument(args.length != 1 || !BAT.getInstance().getConfiguration().isMustGiveReason(),
                I18n.formatWithColor("noReasonInCommand"));
                        
			final String pName = args[0];
	    	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
	    	// The player is online on the proxy
	    	if(player != null){
	    		final String pServer = player.getServer().getInfo().getName();
   				Preconditions.checkArgument(
					pServer != null && !pServer.equals(player.getPendingConnection().getListener().getDefaultServer()),
					I18n.formatWithColor("cantKickDefaultServer", new String[] { pName }));

   				Preconditions.checkArgument(
					PermissionManager.canExecuteAction(PermissionManager.Action.KICK, sender, player.getServer().getInfo().getName()),
					I18n.formatWithColor("noPerm"));

   				Preconditions.checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.KICK, pName), I18n.formatWithColor("isExempt"));

   				final String returnedMsg = kick.kick(player, sender.getName(),
					(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
   				BAT.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
	    	}else{
	    		if(!BAT.getInstance().getRedis().isRedisEnabled()){
	    			throw new IllegalArgumentException(I18n.formatWithColor("playerNotFound"));
	    		}
	    		// Check if the per server kick with Redis is working fine.
		    	final UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
		    	final String pServer = RedisBungee.getApi().getServerFor(pUUID).getName();
		    	Preconditions.checkArgument(pUUID != null, I18n.formatWithColor("playerNotFound"));
		    	// Check if the server of the target isn't the default one. We assume there is the same default server on both Bungee
		    	// TODO: Add a method to check if it's really on default server
		    	String defaultServer = null;
		    	for(final ListenerInfo listener : ProxyServer.getInstance().getConfig().getListeners()){
		    		defaultServer = listener.getDefaultServer();
		    	}
		    	if(defaultServer == null || pServer.equals(defaultServer)){
		    		throw new IllegalArgumentException(I18n.formatWithColor("cantKickDefaultServer", new String[] { pName }));
		    	}
		    	
                Preconditions.checkArgument(PermissionManager.canExecuteAction(PermissionManager.Action.KICK, sender, pServer), I18n.formatWithColor("noPerm"));
		    	
		    	final String returnedMsg;
		    	returnedMsg = kick.kickSQL(pUUID, RedisBungee.getApi().getServerFor(pUUID).getName(), sender.getName(), 
		    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
	    	    BAT.getInstance().getRedis().sendMoveDefaultServerPlayer(pUUID);
    	    	BAT.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
	    	}
		}
	}

	@BATCommand.RunAsync
	public static class GKickCmd extends BATCommand {
		public GKickCmd() {
			super("gkick", "<player> [reason]", "Kick the player from the network", PermissionManager.Action.KICK.getPermission()
					+ ".global");
		}

		@Override
		public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
				throws IllegalArgumentException {
			final String pName = args[0];
                        
            Preconditions.checkArgument(args.length != 1 || !BAT.getInstance().getConfiguration().isMustGiveReason(),
                    I18n.formatWithColor("noReasonInCommand"));

			if (BAT.getInstance().getRedis().isRedisEnabled()) {
			    	UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
			    	Preconditions.checkArgument(pUUID != null, I18n.formatWithColor("playerNotFound"));
			    	
			    	Preconditions.checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.KICK, pName), I18n.formatWithColor("isExempt"));
			    	
			    	final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			    	final String returnedMsg;
			    	if (player != null) {
			    	    	returnedMsg = kick.gKick(player, sender.getName(),
			    	    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
			    	} else {
				    	returnedMsg = kick.gKickSQL(pUUID, sender.getName(),
				    		(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));
			    	        BAT.getInstance().getRedis().sendGKickPlayer(pUUID, returnedMsg);
			    	}
		    	    	BAT.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
			} else {
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
				Preconditions.checkArgument(player != null, I18n.formatWithColor("playerNotFound"));

				Preconditions.checkArgument(PermissionManager.isExemptFrom(PermissionManager.Action.KICK, pName), I18n.formatWithColor("isExempt"));

				final String returnedMsg = kick.gKick(player, sender.getName(),
					(args.length == 1) ? IModule.NO_REASON : Utils.getFinalArg(args, 1));

				BAT.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
			}
		}
	}
}