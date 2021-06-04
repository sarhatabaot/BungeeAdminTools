package fr.alphart.bungeeadmintools.modules.mute;

import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColor;
import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColorAndAddPrefix;

import java.io.Serial;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import co.aikar.commands.BaseCommand;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.I18n.I18n;
import fr.alphart.bungeeadmintools.modules.core.Core;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.modules.ModuleConfiguration;
import fr.alphart.bungeeadmintools.utils.FormatUtils;
import fr.alphart.bungeeadmintools.utils.UUIDNotFoundException;
import fr.alphart.bungeeadmintools.utils.Utils;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import fr.alphart.bungeeadmintools.database.SQLQueries;
import lombok.Getter;
import net.cubespace.Yamler.Config.Comment;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;


/**
 * This module handles all the mute.<br>
 * The mute data of online players are <b>cached</b> in order to avoid lag.
 */
public class Mute implements IModule, Listener {
	private final String name = "mute";
	private ConcurrentHashMap<String, PlayerMuteData> mutedPlayers;
	private ScheduledTask task;
	private final MuteConfig config;
	private BaseCommand muteCommand;

	public Mute() {
		config = new MuteConfig();
	}

	@Override
	public BaseCommand getCommand() {
		return muteCommand;
	}


	@Override
	public String getMainCommand() {
		return "mute";
	}

	@Override
	public ModuleConfiguration getConfig() {
		return config;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean load() {
		// Init table
		Statement statement = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.createStatement();
			if (DataSourceHandler.isSQLite()) {
				for (final String query : SQLQueries.Mute.SQLite.createTable) {
					statement.executeUpdate(query);
				}
			} else {
				statement.executeUpdate(SQLQueries.Mute.createTable);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

		// Register commands
		muteCommand = new MuteCommand(this);
		//commandHandler = new OldMuteCommand(this);
		//commandHandler.loadCommands();

		mutedPlayers = new ConcurrentHashMap<>();

		final MuteTask muteTask = new MuteTask(this);
		task = ProxyServer.getInstance().getScheduler().schedule(BungeeAdminToolsPlugin.getInstance(), muteTask, 0, 10, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public boolean unload() {
		task.cancel();
		mutedPlayers.clear();
		return true;
	}

	public class MuteConfig extends ModuleConfiguration {
		public MuteConfig() {
			init(name);
		}
		
		@Comment("Forbidden commands when a player is mute")
		@Getter
		private List<String> forbiddenCmds = new ArrayList<>(){
			@Serial
			private static final long serialVersionUID = 1L;

		{
			add("msg");
		}};
	}

	public void loadMuteMessage(final String pName, final String server){
		if(!mutedPlayers.containsKey(pName)){
			return;
		}
		String reason = "";
		Timestamp expiration = null;
		Timestamp begin = null;
		String staff = null;
		
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.prepareStatement(DataSourceHandler.isSQLite()
					? SQLQueries.Mute.SQLite.getMuteMessage
					: SQLQueries.Mute.getMuteMessage);
			try{
				statement.setString(1, Core.getUUID(pName));
				statement.setString(2, Core.getPlayerIP(pName));
				statement.setString(3, server);
			}catch(final UUIDNotFoundException e){
				BungeeAdminToolsPlugin.getInstance().getLogger().severe("Error during retrieving of the UUID of " + pName + ". Please report this error :");
				e.printStackTrace();
			}
			resultSet = statement.executeQuery();
	
			if(resultSet.next()) {
				if(DataSourceHandler.isSQLite()){
					begin = new Timestamp(resultSet.getLong("strftime('%s',mute_begin)") * 1000);
					String endStr = resultSet.getString("mute_end");
					expiration = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
				}else{
					begin = resultSet.getTimestamp("mute_begin");
					expiration = resultSet.getTimestamp("mute_end");
				}
				reason = (resultSet.getString("mute_reason") != null) ? resultSet.getString("mute_reason") : IModule.NO_REASON;
				staff = resultSet.getString("mute_staff");
			}else{
				throw new SQLException("No active mute found.");
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		if(expiration != null){
			mutedPlayers.get(pName).setMuteMessage(I18n.formatWithColor("isMuteTemp",
					new String[]{ reason , "{expiration}", Core.defaultDF.format(begin), staff }), expiration);
		}else{
			mutedPlayers.get(pName).setMuteMessage(I18n.formatWithColor("isMute",
					new String[]{ reason, Core.defaultDF.format(begin), staff }), null);
		}
	}
	
	/**
	 * Check if both ip and name of this player are muted<br>
	 * Use <b>cached data</b>
	 * 
	 * @param player
	 * @param server
	 * @return <ul>
	 *         <li>1 if the player is muted from this server</li>
	 *         <li>0 if he's not muted from this server</li>
	 *         <li>-1 if the data are loading</li>
	 *         </ul>
	 */
	public int isMute(final ProxiedPlayer player, final String server) {
		final PlayerMuteData pMuteData = mutedPlayers.get(player.getName());
		if (pMuteData != null) {
			if (pMuteData.isMute(server)) {
				return 1;
			}
			return 0;
		}

		return -1;
	}

	public boolean isMute(final String target) {
		return isMute(target, GLOBAL_SERVER, false);
	}

	/**
	 * Check if this entity (player or ip) is muted<br>
	 * <b>Use uncached data. Use {@link #isMute(ProxiedPlayer, String)} instead
	 * of this method if the player is available</b>
	 * 
	 * @param mutedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if server equals to (any) check if the player is mute on a
	 *            server
	 * @param forceUseUncachedData
	 *            | use uncached data, for example to handle player or player's
	 *            ip related mute
	 * @return
	 */
	public boolean isMute(final String mutedEntity, final String server, final boolean forceUseUncachedData) {
		// Check if the entity is an online player, in this case we're going to
		// use the cached method
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(mutedEntity);
		if (!forceUseUncachedData && player != null) {
			final int result = isMute(player, server);
			// If the data aren't loading
			if (result != -1) {
				return (result == 1);
			}
		}

		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			// If this is an ip which may be muted
			if (Utils.validIP(mutedEntity)) {
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Mute.isMuteIP
						: SQLQueries.Mute.isMuteServerIP);
				statement.setString(1, mutedEntity);
				if (!ANY_SERVER.equals(server)) {
					statement.setString(2, server);
				}
			}
			// If this is a player which may be muted
			else {
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Mute.isMute
						: SQLQueries.Mute.isMuteServer);
				statement.setString(1, Core.getUUID(mutedEntity));
				if (!ANY_SERVER.equals(server)) {
					statement.setString(2, server);
				}
			}
			resultSet = statement.executeQuery();

			// If there are a result
			if (resultSet.next()) {
				return true;
			}

		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return false;
	}

	/**
	 * Mute this entity (player or ip) <br>
	 * 
	 * @param mutedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            ; set to "(global)", to global mute
	 * @param staff
	 * @param expirationTimestamp
	 *            ; set to 0 for mute def
	 * @param reason
	 *            | optional
	 * @return
	 */
	public String mute(final String mutedEntity, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			if (Utils.validIP(mutedEntity)) {
				statement = conn.prepareStatement(SQLQueries.Mute.createMuteIP);
				statement.setString(1, mutedEntity);
				statement.setString(2, staff);
				statement.setString(3, server);
				statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
				statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
				statement.executeUpdate();
				statement.close();


				    	for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
						if (Utils.getPlayerIP(player).equals(mutedEntity)) {
							if (server.equals(GLOBAL_SERVER)) {
								mutedPlayers.get(player.getName()).setGlobal();
							} else {
								mutedPlayers.get(player.getName()).addServer(server);
							}
							if (server.equals(GLOBAL_SERVER) || player.getServer().getInfo().getName().equalsIgnoreCase(server)) {
								player.sendMessage(I18n.formatWithColorAndAddPrefix("wasMutedNotif", new String[] { reason }));
							}
						}
					}


				if (expirationTimestamp > 0) {
					return I18n.formatWithColor("muteTempBroadcast", new String[] {mutedEntity, FormatUtils.getDuration(expirationTimestamp),
							staff, server, reason });
				} else {
					return I18n.formatWithColor("muteBroadcast", new String[] {mutedEntity, staff, server, reason });
				}
			}

			// Otherwise it's a player
			else {
				final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(mutedEntity);
			    	statement = conn.prepareStatement(SQLQueries.Mute.createMute);
		    	    	statement.setString(1, Core.getUUID(mutedEntity));
		    	    	statement.setString(2, staff);
		    	    	statement.setString(3, server);
		    	    	statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
		    	    	statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
		    	    	statement.executeUpdate();
		    	    	statement.close();
			    
			    	if (player != null) {
						updateMuteData(player.getName());
						if(server.equals(GLOBAL_SERVER) || player.getServer().getInfo().getName().equalsIgnoreCase(server)){
							player.sendMessage(I18n.formatWithColorAndAddPrefix("wasMutedNotif", new String[] { reason }));
						}
					}
			    	if (expirationTimestamp > 0) {
						return I18n.formatWithColor("muteTempBroadcast", new String[] {mutedEntity, FormatUtils.getDuration(expirationTimestamp),
							staff, server, reason });
					} else {
						return I18n.formatWithColor("muteBroadcast", new String[] {mutedEntity, staff, server, reason });
					}

			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Mute the ip of an online player
	 * 
	 * @param server
	 *            ; set to "(global)", to global mute
	 * @param staff
	 * @param duration
	 *            ; set to 0 for mute def
	 * @param reason
	 *            | optional
	 * @param ip
	 */
	public String muteIP(final ProxiedPlayer player, final String server, final String staff,
			final long expirationTimestamp, final String reason) {
		mute(Utils.getPlayerIP(player), server, staff, expirationTimestamp, reason);
		return I18n.formatWithColor("muteBroadcast", new String[] { player.getName() + "'s IP", staff, server, reason });
	}

	/**
	 * Unmute an entity (player or ip)
	 * 
	 * @param mutedEntity
	 *            | can be an ip or a player name
	 * @param server
	 *            | if equals to (any), unmute from all servers | if equals to
	 *            (global), remove global mute
	 * @param staff
	 * @param reason
	 * @param unMuteIP
	 */
	public String unMute(final String mutedEntity, final String server, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			// If the mutedEntity is an ip
			if (Utils.validIP(mutedEntity)) {
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Mute.SQLite.unMuteIP)
							: conn.prepareStatement(SQLQueries.Mute.unMuteIP);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, mutedEntity);
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Mute.SQLite.unMuteIPServer) : conn
							.prepareStatement(SQLQueries.Mute.unMuteIPServer);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, mutedEntity);
					statement.setString(4, server);
				}
				statement.executeUpdate();
				statement.close();

				return I18n.formatWithColor("unmuteBroadcast", new String[] {mutedEntity, staff, server, reason });
			}

			// Otherwise it's a player
			else {
				if (ANY_SERVER.equals(server)) {
					statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Mute.SQLite.unMute)
							: conn.prepareStatement(SQLQueries.Mute.unMute);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, Core.getUUID(mutedEntity));
				} else {
					statement = (DataSourceHandler.isSQLite()) ? conn
							.prepareStatement(SQLQueries.Mute.SQLite.unMuteServer) : conn
							.prepareStatement(SQLQueries.Mute.unMuteServer);
					statement.setString(1, reason);
					statement.setString(2, staff);
					statement.setString(3, Core.getUUID(mutedEntity));
					statement.setString(4, server);
				}
				statement.executeUpdate();
				statement.close();

				final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(mutedEntity);
				if (player != null) {
					updateMuteData(player.getName());
					if(ANY_SERVER.equals(server) || GLOBAL_SERVER.equals(server) || player.getServer().getInfo().getName().equalsIgnoreCase(server)){
						player.sendMessage(I18n.formatWithColorAndAddPrefix("wasUnmutedNotif", new String[] { reason }));
					}
				}

				return I18n.formatWithColor("unmuteBroadcast", new String[] {mutedEntity, staff, server, reason });
			}
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Unmute the ip of this entity
	 * 
	 * @param entity
	 * @param server
	 *            | if equals to (any), unmute from all servers | if equals to
	 *            (global), remove global mute
	 * @param staff
	 * @param reason
	 *            | optional
	 * @param duration
	 *            ; set to 0 for mute def
	 */
	public String unMuteIP(final String entity, final String server, final String staff, final String reason) {
		if (Utils.validIP(entity)) {
			return unMute(entity, server, staff, reason);
		} else {
			unMute(Core.getPlayerIP(entity), server, staff, reason);
			updateMuteData(entity);
			return I18n.formatWithColor("unmuteBroadcast", new String[] { entity + "'s IP", staff, server, reason });
		}
	}

	/**
	 * Get all mute data of an entity <br>
	 * <b>Should be run async to optimize performance</b>
	 * 
	 * @param entity
	 *            | can be an ip or a player name
	 * @return List of MuteEntry of the entity
	 */
	public List<MuteEntry> getMuteData(final String entity) {
		final List<MuteEntry> muteList = new ArrayList<>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			// If the entity is an ip
			if (Utils.validIP(entity)) {
				statement = conn.prepareStatement((DataSourceHandler.isSQLite())
						? SQLQueries.Mute.SQLite.getMuteIP
						: SQLQueries.Mute.getMuteIP);
				statement.setString(1, entity);
				resultSet = statement.executeQuery();
			}
			// Otherwise if it's a player
			else {
				statement = conn.prepareStatement((DataSourceHandler.isSQLite())
						? SQLQueries.Mute.SQLite.getMute
						: SQLQueries.Mute.getMute);
				statement.setString(1, Core.getUUID(entity));
				resultSet = statement.executeQuery();
			}
			
			while (resultSet.next()) {
				final Timestamp beginDate;
				final Timestamp endDate;
				final Timestamp unmuteDate;
				if(DataSourceHandler.isSQLite()){
					beginDate = new Timestamp(resultSet.getLong("strftime('%s',mute_begin)") * 1000);
					final String endStr = resultSet.getString("mute_end");
					endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
					final long unbanLong = resultSet.getLong("strftime('%s',mute_unmutedate)") * 1000;
					unmuteDate = (unbanLong == 0) ? null : new Timestamp(unbanLong);
				}else{
					beginDate = resultSet.getTimestamp("mute_begin");
					endDate = resultSet.getTimestamp("mute_end");
					unmuteDate = resultSet.getTimestamp("mute_unmutedate");
				}
				
				final String server = resultSet.getString("mute_server");
				String reason = resultSet.getString("mute_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				final String staff = resultSet.getString("mute_staff");
				final boolean active = (resultSet.getBoolean("mute_state"));
				String unmuteReason = resultSet.getString("mute_unmutereason");
				if(unmuteReason == null){
					unmuteReason = NO_REASON;
				}
				final String unmuteStaff = resultSet.getString("mute_unmutestaff");
				muteList.add(new MuteEntry(entity, server, reason, staff, beginDate, endDate, unmuteDate, unmuteReason, unmuteStaff, active));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return muteList;
	}

	/**
	 * This class is used to cache the mute data of a player.
	 */
	public static class PlayerMuteData {
		private final String pName;
		private final List<String> servers;
		private boolean globalMute = false;
		private Map.Entry<String, Timestamp> muteMessage;

		public PlayerMuteData(final String pName, final List<String> servers) {
			this.pName = pName;
			// Override the arraylist implementation to make used methods non-case sensitive
			this.servers = new ArrayList<String>(){
			    @Override
			    public void add(int index, String element) {
			        super.add(index, element.toLowerCase());
			    }
			    @Override
			    public boolean add(String e) {
			        return super.add(e.toLowerCase());
			    }
			    @Override
			    public boolean contains(Object o) {
			        if(o instanceof String){
			            return super.contains(((String)o).toLowerCase());
			        }
			        return super.contains(o);
			    }
			};
			for(final String server : servers){
			    servers.add(server);
			}
		}

		public void setGlobal() {
			globalMute = true;
		}

		public void unsetGlobal() {
			globalMute = false;
		}

		public void addServer(final String server) {
			if (!servers.contains(server)) {
				servers.add(server);
			}
		}

		public void removeServer(final String server) {
			servers.remove(server);
		}

		public void clearServers() {
			servers.clear();
		}

		public boolean isMute(final String server) {
			if (globalMute) {
				return true;
			}else if( (ANY_SERVER.equals(server) && !servers.isEmpty()) ){
				return true;
			}else return servers.contains(server);
		}
	
		public BaseComponent[] getMuteMessage(final Mute module){
			if(muteMessage != null){
				if(muteMessage.getValue() != null){
					if(muteMessage.getValue().getTime() >= System.currentTimeMillis()){
						return BungeeAdminToolsPlugin.colorizeAndAddPrefix(muteMessage.getKey().replace("{expiration}", FormatUtils.getDuration(muteMessage.getValue().getTime())));
					}
					// If it's not synchronized with the db, force the update of mute data
					else{
						Statement statement = null;
						try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
							statement = conn.createStatement();
							if (DataSourceHandler.isSQLite()) {
								statement.executeUpdate(SQLQueries.Mute.SQLite.updateExpiredMute);
							} else {
								statement.executeUpdate(SQLQueries.Mute.updateExpiredMute);
							}
						} catch (final SQLException e) {
							DataSourceHandler.handleException(e);
						} finally {
							DataSourceHandler.close(statement);
						}
						module.updateMuteData(pName);
					}
				}
				else{
					return BungeeAdminToolsPlugin.colorizeAndAddPrefix(muteMessage.getKey());
				}
			}
			return I18n.formatWithColorAndAddPrefix("wasUnmutedNotif", new String[]{ NO_REASON });
		}
		
		public void setMuteMessage(final String messagePattern, final Timestamp expiration){
			muteMessage = new AbstractMap.SimpleEntry<String, Timestamp>(messagePattern, expiration);
		}
	}

	public void updateMuteData(final String pName) {
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
		if (player == null) {
			return;
		}
		PlayerMuteData pMuteData;
		if (mutedPlayers.containsKey(pName)) {
			pMuteData = mutedPlayers.get(pName);
			pMuteData.clearServers();
			pMuteData.unsetGlobal();
		} else {
			pMuteData = new PlayerMuteData(pName, new ArrayList<>());
		}
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.prepareStatement("SELECT mute_server FROM `BAT_mute` WHERE UUID = ? AND mute_state = 1;");
			statement.setString(1, Core.getUUID(pName));
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				final String server = resultSet.getString("mute_server");
				if (GLOBAL_SERVER.equals(server)) {
					pMuteData.setGlobal();
				} else {
					pMuteData.addServer(server);
				}
			}
			resultSet.close();
			statement.close();

			statement = conn
					.prepareStatement("SELECT mute_server FROM `BAT_mute` WHERE mute_ip = ? AND mute_state = 1;");
			statement.setString(1, Core.getPlayerIP(pName));
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				final String server = resultSet.getString("mute_server");
				if (GLOBAL_SERVER.equals(server)) {
					pMuteData.setGlobal();
				} else {
					pMuteData.addServer(server);
				}
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		mutedPlayers.put(pName, pMuteData);
		if(pMuteData.isMute(GLOBAL_SERVER)){
			loadMuteMessage(pName, GLOBAL_SERVER);
		}else if(player.getServer() != null && pMuteData.isMute(player.getServer().getInfo().getName())){
			loadMuteMessage(pName, player.getServer().getInfo().getName());
		}
	}
	
	public List<MuteEntry> getManagedMute(final String staff){
		final List<MuteEntry> muteList = new ArrayList<>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.prepareStatement((DataSourceHandler.isSQLite())
					? SQLQueries.Mute.SQLite.getManagedMute
					: SQLQueries.Mute.getManagedMute);
			statement.setString(1, staff);
			statement.setString(2, staff);
			resultSet = statement.executeQuery();
			
			while (resultSet.next()) {
				final Timestamp beginDate;
				final Timestamp endDate;
				final Timestamp unmuteDate;
				if(DataSourceHandler.isSQLite()){
					beginDate = new Timestamp(resultSet.getLong("strftime('%s',mute_begin)") * 1000);
					String endStr = resultSet.getString("mute_end");
					endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
					long unmuteLong = resultSet.getLong("strftime('%s',mute_unmutedate)") * 1000;
					unmuteDate = (unmuteLong == 0) ? null : new Timestamp(unmuteLong);
				}else{
					beginDate = resultSet.getTimestamp("mute_begin");
					endDate = resultSet.getTimestamp("mute_end");
					unmuteDate = resultSet.getTimestamp("mute_unmutedate");
				}

				
				// Make it compatible with sqlite (date: get an int with the sfrt and then construct a timestamp)
				final String server = resultSet.getString("mute_server");
				String reason = resultSet.getString("mute_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				String entity = (resultSet.getString("mute_ip") != null) 
						? resultSet.getString("mute_ip")
						: Core.getPlayerName(resultSet.getString("UUID"));
				// If the UUID search failed
				if(entity == null){
					entity = "UUID:" + resultSet.getString("UUID");
				}
				final boolean active = (resultSet.getBoolean("mute_state"));
				String unmuteReason = resultSet.getString("mute_unmutereason");
				if(unmuteReason == null){
					unmuteReason = NO_REASON;
				}
				final String unmuteStaff = resultSet.getString("mute_unmutestaff");
				muteList.add(new MuteEntry(entity, server, reason, staff, beginDate, endDate, unmuteDate, unmuteReason, unmuteStaff, active));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return muteList;
	}

	public void unloadMuteData(final ProxiedPlayer player) {
		mutedPlayers.remove(player.getName());
	}

	// Event Listener
	@EventHandler
	public void onServerConnect(final ServerConnectedEvent e) {
		final ProxiedPlayer player = e.getPlayer();
		final String pName = player.getName();
		final int muteState = isMute(player, e.getServer().getInfo().getName());
		if (muteState == -1) {
			// Load mute data with a little bit of delay to handle server switching operations which may take some time
			BungeeAdminToolsPlugin.getInstance().getProxy().getScheduler().schedule(BungeeAdminToolsPlugin.getInstance(), new Runnable() {
				@Override
				public void run() {
					updateMuteData(pName);
				}
			}, 250, TimeUnit.MILLISECONDS);
		} else if (muteState == 1) {
			PlayerMuteData pMuteData = mutedPlayers.get(pName);
			if(pMuteData.isMute(GLOBAL_SERVER)){
				loadMuteMessage(pName, GLOBAL_SERVER);
			}else if(pMuteData.isMute(e.getServer().getInfo().getName())){
				loadMuteMessage(pName, e.getServer().getInfo().getName());
			}
			player.sendMessage(pMuteData.getMuteMessage(this));
		}
	}

	@EventHandler
	public void onPlayerDisconnect(final PlayerDisconnectEvent e) {
		unloadMuteData(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(final ChatEvent e) {
		if(!(e.getSender() instanceof ProxiedPlayer) || ((ProxiedPlayer) e.getSender()).getServer() == null){
			return;
		}
		final ProxiedPlayer player = (ProxiedPlayer) e.getSender();
		final int muteState = isMute(player, player.getServer().getInfo().getName());
		if (muteState == 0) {
			return;
		}
		if (e.isCommand()) {
			final String command = e.getMessage().replaceAll("/", "").toLowerCase() + " ";
			// There is a bug when overriding the contains method of the arraylist, so we do the contains here
			boolean contains = false;
			for(final String forbiddenCmd : config.getForbiddenCmds()){
			  // Add a space because if we block "/r", we don't want to block "/replay"
				if(command.startsWith(forbiddenCmd.toLowerCase() + " ")){
					contains = true;
					break;
				}
			}
			if (!contains) {
				return;
			}
		}
		if (muteState == 1) {
			player.sendMessage(mutedPlayers.get(player.getName()).getMuteMessage(this));
			e.setCancelled(true);
		} else if (muteState == -1) {
			player.sendMessage(I18n.formatWithColorAndAddPrefix("loadingMutedata"));
			e.setCancelled(true);
		}
	}
}
