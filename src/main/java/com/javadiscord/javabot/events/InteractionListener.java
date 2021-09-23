package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.javadiscord.javabot.events.Startup.preferredGuild;
import static com.mongodb.client.model.Filters.eq;

public class InteractionListener extends ListenerAdapter {

	// TODO: add Context-Menu Commands (once they're available in JDA)

	@Override
	public void onButtonClick(ButtonClickEvent event) {
		if (event.getUser().isBot()) return;

		event.deferEdit().queue();

		Guild guild = preferredGuild;
		MongoDatabase database = mongoClient.getDatabase("other");
		String[] id = event.getComponentId().split(":");
		System.out.println(event.getComponentId());
		switch (id[0]) {
			case "dm-submission" -> this.handleDmSubmission(database, guild, event);
			case "submission" -> this.handleSubmission(database, guild, event);
			case "reaction-role" -> this.handleReactionRoles(event);
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
		boolean b = Boolean.parseBoolean(id[2]);

		Member member = event.getGuild().retrieveMemberById(event.getUser().getId()).complete();
		Role role = event.getGuild().getRoleById(roleID);

		if (member.getRoles().contains(role)) {
			if (!b) {
				event.getGuild().removeRoleFromMember(member, role).queue();
				event.getHook().sendMessage("Removed Role: " + role.getAsMention()).setEphemeral(true).queue();
			} else event.getHook().sendMessage("You already have Role: " + role.getAsMention()).setEphemeral(true).queue();
		} else {
			event.getGuild().addRoleToMember(member, role).queue();
			event.getHook().sendMessage("Added Role: " + role.getAsMention()).setEphemeral(true).queue();
		}
	}
}
