package fr.alphart.bungeeadmintools.modules.comment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.cubespace.Yamler.Config.YamlConfig;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

@AllArgsConstructor
@NoArgsConstructor
public class Trigger extends YamlConfig {
	@Getter
	private int triggerNumber = 3;
	@Getter
	private List<String> pattern = Collections.singletonList("");
	private List<String> commands = Arrays.asList("alert {player} sparks a trigger. Reason: {reason}","gtempmute {player} 30m");
	
	public void onTrigger(final String pName, final String reason){
		final PluginManager pm = ProxyServer.getInstance().getPluginManager();
		final CommandSender console = ProxyServer.getInstance().getConsole();
		long delay = 100;
		for (final String command : commands) {
		    ProxyServer.getInstance().getScheduler().schedule(BungeeAdminToolsPlugin.getInstance(), () -> pm.dispatchCommand(console, command.replace("{player}", pName).replace("{reason}", reason)), delay, TimeUnit.MILLISECONDS);
		    delay += 500;
		}
	}
	
}
