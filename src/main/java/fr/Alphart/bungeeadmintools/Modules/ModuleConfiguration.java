package fr.alphart.bungeeadmintools.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import net.cubespace.Yamler.Config.YamlConfig;

public abstract class ModuleConfiguration extends YamlConfig {
	
	// We must use an init method because if we use the super constructor, it doesn't work properly (field of children class are overwritten)
	public void init(final String moduleName){
       try {
        initThrowingExceptions(moduleName);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Unlike {@link ModuleConfiguration#init()} this init method throw the exception and doesn't
	 * print it in the console
	 */
	public void initThrowingExceptions(final String moduleName) throws InvalidConfigurationException{
	    CONFIG_HEADER = new String[] { "BungeeAdminTools - " + moduleName + " configuration file" };
	    CONFIG_FILE = new File(BungeeAdminToolsPlugin.getInstance().getDataFolder(), moduleName + ".yml");
        init();
        load();
	}

	@Getter
	private boolean enabled = true;

	private Map<String, Boolean> commands = new HashMap<>();

	/**
	 * Get the names of the enabled commands for this module
	 * 
	 * @return list of the enabled commands
	 */
	public List<String> getEnabledCmds() {
		final List<String> enabledCmds = new ArrayList<>();
		for (final Entry<String, Boolean> entry : commands.entrySet()) {
			if (entry.getValue()) {
				enabledCmds.add(entry.getKey());
			}
		}
		return enabledCmds;
	}

	/**
	 * Add commands provided by this module into the configuration file
	 * 
	 * @param commands
	 *            list
	 */
	public void setProvidedCmds(final List<String> commands) {
		Collections.sort(commands);
		// Add new commands if there are
		for (final String cmdName : commands) {
			if (!this.commands.containsKey(cmdName)) {
				this.commands.put(cmdName, true);
			}
		}
		// Iterate through the commands map and remove the ones who don't exist (e.g because of an update)
		this.commands.entrySet().removeIf(cmdEntry -> !commands.contains(cmdEntry.getKey()));
	}
}