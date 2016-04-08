package com.almightyalpaca.discord.bot.plugin.carbon;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.almightyalpaca.discord.bot.system.config.Config;
import com.almightyalpaca.discord.bot.system.events.manager.EventHandler;
import com.almightyalpaca.discord.bot.system.exception.PluginLoadingException;
import com.almightyalpaca.discord.bot.system.exception.PluginUnloadingException;
import com.almightyalpaca.discord.bot.system.plugins.Plugin;
import com.almightyalpaca.discord.bot.system.plugins.PluginInfo;
import com.google.common.util.concurrent.MoreExecutors;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;

public class CarbonPlugin extends Plugin {

	private static final PluginInfo INFO = new PluginInfo("com.almightyalpaca.discord.bot.plugin.carbon", "1.0.0", "Almighty Alpaca", "Carbon Plugin", "Send bot statistics to carbonitex.com");

	private Config carbonConfig;

	private ScheduledExecutorService scheduledExecutor;

	public CarbonPlugin() {
		super(CarbonPlugin.INFO);
	}

	@Override
	public void load() throws PluginLoadingException {

		this.carbonConfig = this.getSharedConfig("carbonitex");

		if (carbonConfig.getString("key", "Your Key") == "Your Key") {
			throw new PluginLoadingException("Pls add your carbonitex key to the config");
		}

		this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Carbon Reporter Thread"));

		this.scheduledExecutor.scheduleAtFixedRate(() -> {
			this.sendStats();
		} , 30, 60, TimeUnit.MINUTES);

		this.registerEventHandler(this);
	}

	@EventHandler
	public void onGuildJoinEvent(final GuildJoinEvent event) {
		// Commented until JDA doesn't send it for guilds getting available
		// this.sendStats();
	}

	@EventHandler
	public void onGuildLeaveEvent(final GuildLeaveEvent event) {
		// Commented until JDA doesn't send it for guilds getting available
		// this.sendStats();
	}

	public void sendStats() {
		try {
			final String response = Unirest.post("https://www.carbonitex.net/discord/data/botdata.php").field("key", this.carbonConfig.getString("key"))
				.field("servercount", this.getJDA().getGuilds().size()).asString().getBody();
			System.out.println("Successfully posted the botdata to carbonitex.com: " + response);
		} catch (final UnirestException e) {
			System.out.println("An error occured while posting the botdata to carbonitex.com");
			e.printStackTrace();
		}
	}

	@Override
	public void unload() throws PluginUnloadingException {
		MoreExecutors.shutdownAndAwaitTermination(scheduledExecutor, 10, TimeUnit.SECONDS);
	}
}
