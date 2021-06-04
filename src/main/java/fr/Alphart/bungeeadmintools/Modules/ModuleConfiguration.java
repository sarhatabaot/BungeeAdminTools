package fr.alphart.bungeeadmintools.modules;

import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import lombok.Getter;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.cubespace.Yamler.Config.YamlConfig;

import java.io.File;

public abstract class ModuleConfiguration extends YamlConfig {
	@Getter//TODO
	private final boolean enabled = true;

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

}