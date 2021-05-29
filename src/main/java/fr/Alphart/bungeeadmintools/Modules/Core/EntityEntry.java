package fr.alphart.bungeeadmintools.modules.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import fr.alphart.bungeeadmintools.database.SQLQueries;
import fr.alphart.bungeeadmintools.modules.InvalidModuleException;
import fr.alphart.bungeeadmintools.modules.ModulesManager;
import fr.alphart.bungeeadmintools.modules.ban.BanEntry;
import fr.alphart.bungeeadmintools.modules.comment.CommentEntry;
import fr.alphart.bungeeadmintools.modules.kick.KickEntry;
import fr.alphart.bungeeadmintools.modules.mute.MuteEntry;
import fr.alphart.bungeeadmintools.utils.UUIDNotFoundException;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;



/**
 * Summit all type of informations available with the plugin about an specific
 * entity.
 */
public class EntityEntry {
	public static final Timestamp noDateFound = new Timestamp(877478400); 
	// No use of the standard timestamp because the standard one is a good way to find when error happens (as 1970 isn't an usual date on mcserver)
	
	private final String entity;

	private final List<BanEntry> bans = new ArrayList<>();
	private final List<MuteEntry> mutes = new ArrayList<>();
	private final List<KickEntry> kicks = new ArrayList<>();
	private final List<CommentEntry> comments = new ArrayList<>();

	private Timestamp firstLogin;
	private Timestamp lastLogin;
	private String lastIP = "0.0.0.0";

	private final List<String> ipUsers = new ArrayList<>();

	private boolean exist = true;
	private boolean player = false;

	public EntityEntry(final String entity) {
		this.entity = entity;

		// This is a player
		if (!Utils.validIP(entity)) {
			// Get players basic information (first/last login, last ip)
			player = true;
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
				statement = (DataSourceHandler.isSQLite()) ? conn
						.prepareStatement(SQLQueries.Core.SQLite.getPlayerData) : conn
						.prepareStatement(SQLQueries.Core.getPlayerData);
						statement.setString(1, Core.getUUID(entity));

						resultSet = statement.executeQuery();

						if (resultSet.next()) {
							if (DataSourceHandler.isSQLite()) {
								firstLogin = new Timestamp(resultSet.getLong("strftime('%s',firstlogin)") * 1000);
								lastLogin = new Timestamp(resultSet.getLong("strftime('%s',lastlogin)") * 1000);
							} else {
								firstLogin = resultSet.getTimestamp("firstlogin");
								lastLogin = resultSet.getTimestamp("lastlogin");
							}
							final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entity);
							if (player != null) {
								lastIP = Utils.getPlayerIP(player);
							} else {
								lastIP = resultSet.getString("lastip");
							}
						}
						if(firstLogin == null){
						    firstLogin = noDateFound;
						}
						if(lastLogin == null){
						    lastLogin = noDateFound;
						}
			} catch (final SQLException e) {
				DataSourceHandler.handleException(e);
			} finally {
				DataSourceHandler.close(statement, resultSet);
			}
		}

		// This is an ip
		else {
			// Get users from this ip
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
				statement = conn.prepareStatement(SQLQueries.Core.getIpUsers);
				statement.setString(1, entity);

				resultSet = statement.executeQuery();

				while (resultSet.next()) {
					ipUsers.add(resultSet.getString("BAT_player"));
				}
			} catch (final SQLException e) {
				DataSourceHandler.handleException(e);
			} finally {
				DataSourceHandler.close(statement, resultSet);
			}
		}
		
		// Load the data related to this entity of each modules
		final ModulesManager modules = BungeeAdminToolsPlugin.getInstance().getModules();
		try {
			if (modules.isLoaded("ban")) {
				bans.addAll(modules.getBanModule().getBanData(entity));
			}
			if (modules.isLoaded("mute")) {
				mutes.addAll(modules.getMuteModule().getMuteData(entity));
			}
			// No ip kick
			if (modules.isLoaded("kick") && ipUsers.isEmpty()) {
				kicks.addAll(modules.getKickModule().getKickData(entity));
			}
			if(modules.isLoaded("comment")){
				comments.addAll(modules.getCommentModule().getComments(entity));
			}		
		} catch (final InvalidModuleException | UUIDNotFoundException e) {
			if(e instanceof UUIDNotFoundException){
				exist = false;
			}
		}

	}

	public String getEntity() {
		return entity;
	}

	public List<BanEntry> getBans() {
		return bans;
	}

	public List<MuteEntry> getMutes() {
		return mutes;
	}

	public List<KickEntry> getKicks() {
		return kicks;
	}

	public List<CommentEntry> getComments(){
		return comments;
	}
	
	public boolean exist() {
		return exist;
	}

	public boolean isPlayer() {
		return player;
	}

	public Timestamp getFirstLogin() {
		return firstLogin;
	}

	public Timestamp getLastLogin() {
		return lastLogin;
	}

	public String getLastIP() {
		return lastIP;
	}

	/**
	 * Get the players who have this ip as last ip used <br>
	 * Only works if the <b>entity is an adress ip</b>
	 * 
	 * @return list of players name
	 */
	public List<String> getUsers() {
		return ipUsers;
	}
}