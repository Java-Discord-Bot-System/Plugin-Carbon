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
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;

public class CarbonPlugin extends Plugin {

	private static final PluginInfo	INFO	= new PluginInfo("com.almightyalpaca.discord.bot.plugin.carbon", "1.0.0", "Almighty Alpaca", "Carbon Plugin", "Send bot statistics to carbonitex.com");

	Config							carbonConfig;

	ScheduledExecutorService		scheduledExecutor;

	public CarbonPlugin() {
		super(CarbonPlugin.INFO);
	}

	@Override
	public void load() throws PluginLoadingException {
		this.carbonConfig = this.getBridge().getSecureConfig("carbonitex");
		this.scheduledExecutor = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Carbon Reporter Thread"));

		this.scheduledExecutor.scheduleAtFixedRate(() -> {
			this.sendStats();
		}, 0, 1, TimeUnit.HOURS);

		this.registerEventHandler(this);
	}

	@EventHandler
	public void onGuildJoinEvent(final GuildJoinEvent event) {
		this.sendStats();
	}

	@EventHandler
	public void onGuildLeaveEvent(final GuildLeaveEvent event) {
		this.sendStats();
	}

	public void sendStats() {
		try {
			final String response = Unirest.post("https://www.carbonitex.net/discord/data/botdata.php").field("key", this.carbonConfig.getString("key")).field("servercount", this.getBridge().getJDA()
					.getGuilds().size()).asString().getBody();
			System.out.println("Successfully posted the botdata to carbonitex.com: " + response);
		} catch (final UnirestException e) {
			System.out.println("An error occured while posting the botdata to carbonitex.com");
			e.printStackTrace();
		}
	}

	@Override
	public void unload() throws PluginUnloadingException {
		this.scheduledExecutor.shutdown();
		try {
			if (!this.scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				this.scheduledExecutor.shutdownNow();
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}
