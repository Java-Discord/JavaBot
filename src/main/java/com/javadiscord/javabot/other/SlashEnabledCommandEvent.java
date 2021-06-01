package com.javadiscord.javabot.other;

import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.Optional;

public class SlashEnabledCommandEvent {
	private final CommandEvent messageCommandEvent;
	private final SlashCommandEvent slashCommandEvent;

	private final JDA jda;
	private final MessageChannel channel;
	private final User user;

	public SlashEnabledCommandEvent(CommandEvent messageCommandEvent) {
		this.messageCommandEvent = messageCommandEvent;
		this.slashCommandEvent = null;
		this.jda = messageCommandEvent.getJDA();
		this.channel = messageCommandEvent.getChannel();
		this.user = messageCommandEvent.getAuthor();
	}

	public SlashEnabledCommandEvent(SlashCommandEvent slashCommandEvent) {
		this.slashCommandEvent = slashCommandEvent;
		this.messageCommandEvent = null;
		this.jda = slashCommandEvent.getJDA();
		this.channel = slashCommandEvent.getChannel();
		this.user = slashCommandEvent.getUser();
	}

	public Optional<CommandEvent> getMessageCommandEvent() {
		return Optional.ofNullable(messageCommandEvent);
	}

	public Optional<SlashCommandEvent> getSlashCommandEvent() {
		return Optional.ofNullable(slashCommandEvent);
	}

	public JDA getJDA() {
		return jda;
	}

	public MessageChannel getChannel() {
		return channel;
	}

	public User getUser() {
		return user;
	}

	public void reply(MessageEmbed embed) {
		if (this.messageCommandEvent != null) {
			this.messageCommandEvent.reply(embed);
		} else {
			this.slashCommandEvent.replyEmbeds(embed).queue();
		}
	}
}
