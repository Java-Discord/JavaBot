package net.javadiscord.javabot.systems.staff_commands.self_roles;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.components.ButtonHandler;
import com.dynxsty.dih4jda.interactions.components.ModalHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.javadiscord.javabot.util.Constants;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.util.Responses;
import org.apache.commons.validator.routines.EmailValidator;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * Handles all Interactions related to the Self Role System.
 */
@Slf4j
@RequiredArgsConstructor
@AutoDetectableComponentHandler("self-role")
public class SelfRoleInteractionManager implements ButtonHandler, ModalHandler {

	private final BotConfig botConfig;

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, Button button) {
		Member member = event.getMember();
		if (event.getGuild() == null || !event.isFromGuild() || member == null) {
			Responses.error(event.getHook(), "This may only be used inside a server.").queue();
			return;
		}
		String[] args = ComponentIdBuilder.split(event.getComponentId());
		Role role = event.getGuild().getRoleById(args[2]);
		if (role == null) return;
		boolean permanent = Boolean.parseBoolean(args[3]);
		switch (args[1]) {
			case "default" -> handleSelfRole(event, role, event.getGuild(), permanent);
			case "staff" -> buildStaffApplication(event, role, member);
			case "expert" -> buildExpertApplication(event, member);
			default -> event.deferReply().queue(h -> Responses.error(h, "Unknown Interaction.").queue());
		}
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, List<ModalMapping> values) {
		String[] args = ComponentIdBuilder.split(event.getModalId());
		event.deferReply(true).queue();
		GuildConfig config = botConfig.get(event.getGuild());
		switch (args[1]) {
			case "staff" -> sendStaffSubmission(event, config, args[2], args[3]).queue();
			case "expert" -> sendExpertSubmission(event, config.getModerationConfig(), args[2]).queue();
			default -> event.deferReply().queue(h -> Responses.error(h, "Unknown Interaction.").queue());
		}
	}

	/**
	 * Builds and replies with a Staff Application Modal.
	 *
	 * @param event     The {@link ButtonInteractionEvent} that is fired upon clicking a button.
	 * @param role      The corresponding {@link Role}.
	 * @param applicant The Applicant.
	 */
	private void buildStaffApplication(@NotNull ButtonInteractionEvent event, Role role, @NotNull Member applicant) {
		if (applicant.getRoles().contains(role)) {
			event.reply("You already have Role: " + role.getAsMention()).setEphemeral(true).queue();
			return;
		}
		TextInput name = TextInput.create("name", "Real Name", TextInputStyle.SHORT)
				.setRequired(true)
				.setPlaceholder("John Doe")
				.build();
		TextInput age = TextInput.create("age", "Age", TextInputStyle.SHORT)
				.setRequired(true)
				.setPlaceholder("24")
				.setRequiredRange(1, 2)
				.build();
		TextInput email = TextInput.create("email", "Email", TextInputStyle.SHORT)
				.setRequired(true)
				.setPlaceholder("moontm@javadiscord.net")
				.setMaxLength(254)
				.build();
		TextInput timezone = TextInput.create("timezone", "Timezone", TextInputStyle.SHORT)
				.setRequired(true)
				.build();
		TextInput extraRemarks = TextInput.create("extra-remarks", "Anything else?", TextInputStyle.PARAGRAPH)
				.setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
				.build();
		Modal modal = Modal.create(String.format("self-role:staff:%s:%s", role.getId(), applicant.getId()), "Apply for " + role.getName())
				.addActionRows(ActionRow.of(name), ActionRow.of(age), ActionRow.of(email), ActionRow.of(timezone), ActionRow.of(extraRemarks))
				.build();
		event.replyModal(modal).queue();
	}

	/**
	 * Builds and replies with an Expert Application Modal.
	 *
	 * @param event     The {@link ButtonInteractionEvent} that is fired upon clicking a button.
	 * @param applicant The Applicant.
	 */
	private void buildExpertApplication(@NotNull ButtonInteractionEvent event, @NotNull Member applicant) {
		Role role = botConfig.get(event.getGuild()).getModerationConfig().getExpertRole();
		if (applicant.getRoles().contains(role)) {
			event.reply("You already have the Expert Role: " + role.getAsMention()).setEphemeral(true).queue();
			return;
		}
		TextInput experience = TextInput.create("java-experience", "How much Java experience do you have?", TextInputStyle.PARAGRAPH)
				.setPlaceholder("How much experience do you have with the Java Programming Language?")
				.setRequired(true)
				.build();
		TextInput projectInfo = TextInput.create("project-info", "Present us a fitting Java Project", TextInputStyle.PARAGRAPH)
				.setPlaceholder("Choose a fitting Java Project you've done yourself and present it to us.")
				.setRequired(true)
				.build();
		TextInput projectLinks = TextInput.create("project-links", "Please provide a link to your project", TextInputStyle.SHORT)
				.setPlaceholder(Constants.GITHUB_LINK)
				.setRequired(true)
				.build();
		TextInput reason = TextInput.create("reason", "Why should we accept this submission?", TextInputStyle.PARAGRAPH)
				.setRequired(true)
				.build();
		Modal modal = Modal.create(String.format("self-role:expert:%s", applicant.getId()), "Apply for " + role.getName())
				.addActionRows(ActionRow.of(experience), ActionRow.of(projectInfo), ActionRow.of(projectLinks), ActionRow.of(reason))
				.build();
		event.replyModal(modal).queue();
	}

	/**
	 * Handles a single Self Role Interaction.
	 *
	 * @param event     The {@link ButtonInteractionEvent} that is fired upon clicking a button.
	 * @param role      The Role that should be assigned/removed
	 * @param guild     The current {@link Guild}.
	 * @param permanent Whether the user is able to remove the role again.
	 */
	private void handleSelfRole(@NotNull ButtonInteractionEvent event, Role role, @NotNull Guild guild, boolean permanent) {
		event.deferEdit().queue();
		guild.retrieveMemberById(event.getUser().getId()).queue(member -> {
			if (member.getRoles().contains(role)) {
				if (!permanent) {
					guild.removeRoleFromMember(member, role).queue();
					event.getHook().sendMessage("Removed Role: " + role.getAsMention()).setEphemeral(true).queue();
				} else {
					event.getHook().sendMessage("You already have Role: " + role.getAsMention()).setEphemeral(true).queue();
				}
			} else {
				guild.addRoleToMember(member, role).queue();
				event.getHook().sendMessage("Added Role: " + role.getAsMention()).setEphemeral(true).queue();
			}
		}, e -> log.error("Could not retrieve Member with Id: " + event.getUser().getIdLong()));
	}

	/**
	 * Sends the Staff Applications contents to the {@link ModerationConfig#getApplicationChannel()}.
	 *
	 * @param event  The {@link ModalInteractionEvent} that is fired upon submitting a Modal.
	 * @param config The {@link GuildConfig} for the current Guild.
	 * @param roleId The role's id that was applied for.
	 * @param userId The applicant's id.
	 * @return The {@link WebhookMessageCreateAction}.
	 */
	private WebhookMessageCreateAction<Message> sendStaffSubmission(@NotNull ModalInteractionEvent event, GuildConfig config, String roleId, String userId) {
		ModalMapping nameOption = event.getValue("name");
		ModalMapping ageOption = event.getValue("age");
		ModalMapping emailOption = event.getValue("email");
		ModalMapping timezoneOption = event.getValue("timezone");
		ModalMapping extraRemarksOption = event.getValue("extra-remarks");
		if (nameOption == null || ageOption == null || emailOption == null || timezoneOption == null || extraRemarksOption == null) {
			return Responses.replyMissingArguments(event.getHook());
		}
		if (!EmailValidator.getInstance().isValid(emailOption.getAsString())) {
			return Responses.error(event.getHook(), "`%s` is not a valid email-address. Please try again.", emailOption.getAsString());
		}
		Role role = config.getGuild().getRoleById(roleId);
		if (role == null) {
			return Responses.error(event.getHook(), "Unknown Role. Please contact an Administrator if this issue persists");
		}
		config.getGuild().retrieveMemberById(userId).queue(
				member -> {
					User user = member.getUser();
					MessageEmbed embed = new EmbedBuilder()
							.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
							.setTitle(String.format("%s applied for %s", user.getAsTag(), role.getName()))
							.setColor(Responses.Type.SUCCESS.getColor())
							.addField("Real Name", nameOption.getAsString(), false)
							.addField("Age", ageOption.getAsString(), true)
							.addField("Email", String.format("`%s`", emailOption.getAsString()), true)
							.addField("Timezone", String.format("`%s`", timezoneOption.getAsString()), true)
							.addField("Server joined", String.format("<t:%s:R>", member.getTimeJoined().toEpochSecond()), true)
							.addField("Account created", String.format("<t:%s:R>", member.getUser().getTimeCreated().toEpochSecond()), true)
							.addField("Extra Remarks", extraRemarksOption.getAsString().isEmpty() ? "N/A" : extraRemarksOption.getAsString(), false)
							.setTimestamp(Instant.now())
							.build();
					config.getModerationConfig().getApplicationChannel().sendMessageEmbeds(embed).queue();
				}
		);
		return Responses.info(event.getHook(), "Submission sent!",
				"Your Submission has been sent to our Moderators! Please note that spamming submissions may result in a ban.");
	}

	/**
	 * Sends the Expert Applications contents to the {@link ModerationConfig#getApplicationChannel()}.
	 *
	 * @param event  The {@link ModalInteractionEvent} that is fired upon submitting a Modal.
	 * @param config The {@link ModerationConfig} for the current Guild.
	 * @param userId The applicant's id.
	 * @return The {@link WebhookMessageCreateAction}.
	 */
	private @NotNull WebhookMessageCreateAction<Message> sendExpertSubmission(@NotNull ModalInteractionEvent event, ModerationConfig config, String userId) {
		if (event.getGuild() == null || !event.isFromGuild()) {
			return Responses.error(event.getHook(), "This may only be used inside a server.");
		}
		ModalMapping experienceOption = event.getValue("java-experience");
		ModalMapping projectInfoOption = event.getValue("project-info");
		ModalMapping projectLinksOption = event.getValue("project-links");
		ModalMapping reasonOption = event.getValue("reason");
		if (experienceOption == null || projectInfoOption == null || projectLinksOption == null || reasonOption == null) {
			return Responses.replyMissingArguments(event.getHook());
		}
		event.getGuild().retrieveMemberById(userId).queue(
				member -> {
					User user = member.getUser();
					EmbedBuilder embed = new EmbedBuilder()
							.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
							.setTitle(String.format("%s applied for %s", user.getAsTag(), config.getExpertRole().getName()))
							.setColor(config.getExpertRole().getColor())
							.addField("How much Java experience do you have?", experienceOption.getAsString(), false)
							.addField("Present us a fitting Java Project", projectInfoOption.getAsString(), false)
							.addField("Please provide a link to your project", projectLinksOption.getAsString(), false)
							.addField("Why should we accept this submission?", reasonOption.getAsString(), false)
							.setTimestamp(Instant.now());
					config.getApplicationChannel().sendMessageEmbeds(embed.build()).queue();
				}
		);
		return Responses.info(event.getHook(), "Submission sent!",
				"Your Submission has been sent to our Moderators! Please note that spamming submissions may result in a ban.");
	}
}
