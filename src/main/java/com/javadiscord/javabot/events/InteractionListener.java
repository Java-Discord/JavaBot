package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.help.HelpChannelManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.javadiscord.javabot.events.Startup.preferredGuild;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class InteractionListener extends ListenerAdapter {

	// TODO: add Context-Menu Commands (once they're available in JDA)

	@Override
	public void onButtonClick(ButtonClickEvent event) {
		if (event.getUser().isBot()) return;
		event.deferEdit().queue();

		Guild guild = preferredGuild;
		MongoDatabase database = mongoClient.getDatabase("other");
		String[] id = event.getComponentId().split(":");
		switch (id[0]) {
			case "dm-submission" -> this.handleDmSubmission(database, guild, event);
			case "submission" -> this.handleSubmission(database, guild, event);
			case "reaction-role" -> this.handleReactionRoles(event);
			case "help-channel" -> this.handleHelpChannel(event, id[1]);
		}
	}

	private void handleDmSubmission(MongoDatabase database, Guild guild, ButtonClickEvent event) {
		MongoCollection<Document> openSubmissions = database.getCollection("open_submissions");
		Document document = openSubmissions.find(eq("guild_id", guild.getId())).first();
		JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
		String text = root.get("text").getAsString();
		String[] id = event.getComponentId().split(":");
		switch (id[1]) {
			case "send" -> new SubmissionListener().dmSubmissionSend(event, text);
			case "cancel" -> new SubmissionListener().dmSubmissionCancel(event);
		}
		openSubmissions.deleteOne(document);
	}

	private void handleSubmission(MongoDatabase database, Guild guild, ButtonClickEvent event) {
		MongoCollection<Document> submissionMessages = database.getCollection("submission_messages");
		Document document = submissionMessages.find(eq("guild_id", guild.getId())).first();
		JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
		String userID = root.get("user_id").getAsString();
		String[] id = event.getComponentId().split(":");
		switch (id[1]) {
			case "approve" -> new SubmissionListener().submissionApprove(event, userID);
			case "decline" -> new SubmissionListener().submissionDecline(event);
			case "delete" -> new SubmissionListener().submissionDelete(event);
		}
		submissionMessages.deleteOne(document);
	}

	private void handleReactionRoles(ButtonClickEvent event) {
		String[] id = event.getComponentId().split(":");
		String roleID = id[1];
		boolean permanent = Boolean.parseBoolean(id[2]);

		Member member = event.getGuild().retrieveMemberById(event.getUser().getId()).complete();
		Role role = event.getGuild().getRoleById(roleID);

		if (member.getRoles().contains(role)) {
			if (!permanent) {
				event.getGuild().removeRoleFromMember(member, role).queue();
				event.getHook().sendMessage("Removed Role: " + role.getAsMention()).setEphemeral(true).queue();
			} else {
				event.getHook().sendMessage("You already have Role: " + role.getAsMention()).setEphemeral(true).queue();
			}
		} else {
			event.getGuild().addRoleToMember(member, role).queue();
			event.getHook().sendMessage("Added Role: " + role.getAsMention()).setEphemeral(true).queue();
		}
	}

	private void handleHelpChannel(ButtonClickEvent event, String action) {
		var config = Bot.config.get(event.getGuild()).getHelp();
		var channelManager = new HelpChannelManager(config);
		TextChannel channel = event.getTextChannel();
		User owner = channelManager.getReservedChannelOwner(channel);
		// If a reserved channel doesn't have an owner, it's in an invalid state, but the system will handle it later automatically.
		if (owner == null) {
			// Remove the original message, just to make sure no more interactions are sent.
			if (event.getMessage() != null) {
				event.getMessage().delete().queue();
			}
			return;
		}

		// Check that the user is allowed to do the interaction.
		if (
			event.getUser().equals(owner) ||
			(event.getMember() != null && event.getMember().getRoles().contains(Bot.config.get(event.getGuild()).getModeration().getStaffRole()))
		) {
			if (action.equals("done")) {
				log.info("Unreserving channel {} because it was marked as done.", channel.getAsMention());
				if (event.getMessage() != null) {
					event.getMessage().delete().queue();
				}
				channelManager.unreserveChannel(channel).queue();
			} else if (action.equals("not-done")) {
				log.info("Removing timeout check message in {} because it was marked as not-done.", channel.getAsMention());
				if (event.getMessage() != null) {
					event.getMessage().delete().queue();
				}
				channel.sendMessage(String.format(
						"Okay, we'll keep this channel reserved for you, and check again in **%d** minutes.",
						config.getInactivityTimeoutMinutes()
				)).queue();
			}
		}
	}
}
