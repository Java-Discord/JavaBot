package com.javadiscord.javabot.help;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.time.OffsetDateTime;

public class HelpChannelUpdater implements Runnable {
	private final JDA jda;
	private final HelpChannelManager channelManager;

	public HelpChannelUpdater(JDA jda) {
		this.jda = jda;
		this.channelManager = new HelpChannelManager();
	}


	@Override
	public void run() {
		for (var guild : this.jda.getGuilds()) {
			var config = Bot.config.get(guild).getHelp();
			var channels = guild.getTextChannels();
			for (var channel : channels) {
				if (channel.getName().startsWith(config.getReservedChannelPrefix())) {
					this.checkReservedChannel(channel, config);
				}
			}
		}
	}

	private void checkReservedChannel(TextChannel channel, HelpConfig config) {
		channel.getHistoryFromBeginning(1).queue(history -> {
			if (history.isEmpty()) {
				// Revert to open channel.
				this.channelManager.open(channel, config);
			} else {
				Message firstMsg = history.getRetrievedHistory().get(0);
				var channelBecomesInactiveAt = firstMsg.getTimeCreated().plusSeconds(config.getInactivityTimeoutSeconds());
				if (OffsetDateTime.now().isAfter(channelBecomesInactiveAt)) {
					User user = firstMsg.getAuthor();
				}
			}
		});
	}
}
