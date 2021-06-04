package fr.alphart.bungeeadmintools;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.cubespace.Yamler.Config.Path;
import net.cubespace.Yamler.Config.YamlConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Getter
public class Configuration extends YamlConfig {
	public Configuration(){
		CONFIG_HEADER = new String[]{"Bungee Admin Tools - Configuration file"};
		CONFIG_FILE = new File(BungeeAdminToolsPlugin.getInstance().getDataFolder(), "config.yml");
		try {
			init();
			save();
		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	private final String language = "en";
	private final String prefix = "&6[&4BAT&6]&e ";
	
    @Comment("Force players to give reason when /ban /unban /kick /mute /unmute etc.")
	private final boolean mustGiveReason= false;
	@Comment("Enable /bat confirm, to confirm command such as action on unknown player.")
	private final boolean confirmCommand = true;
	@Comment("Enable or disable simple aliases to bypass the /bat prefix for core commands")
	private final Map<String, Boolean> simpleAliasesCommands = Maps.newHashMap();
	@Comment("Make the date more readable."
			+ "If the date correspond to today, tmw or yda, it will replace the date by the corresponding word")
	private final boolean litteralDate = true;
	@Comment("Enable BETA (experimental) Redis support, requires RedisBungee")
	private final boolean redisSupport = false;
	@Comment("The debug mode enables verbose logging. All the logged message will be in the debug.log file in BAT folder")
	private final boolean debugMode = false;

	@Comment("Whitelist of ips not to ip ban. Usually put your server ip here.")
	private final List<String> whitelistedIp = new ArrayList<>();
	
	
	@Comment("Set to true to use MySQL. Otherwise SQL Lite will be used")
	@Setter
    @Path(value = "mysql.enabled")
	private boolean mysql_enabled = true;
    @Path(value = "mysql.user")
	private final String mysql_user = "user";
    @Path(value = "mysql.password")
	private final String mysql_password = "password";
    @Path(value = "mysql.database")
	private final String mysql_database = "database";
    @Path(value = "mysql.host")
	private final String mysql_host = "localhost";
	@Comment("If you don't know it, just leave it like this (3306 = default mysql port)")
    @Path(value = "mysql.port")
	private final String mysql_port = "3306";
	public Locale getLocale() {
		if (language.length() != 2) {
			BungeeAdminToolsPlugin.getInstance().getLogger().severe("Incorrect language set ... The language was set to english.");
			return new Locale("en");
		}
		return new Locale(language);
	}
}
