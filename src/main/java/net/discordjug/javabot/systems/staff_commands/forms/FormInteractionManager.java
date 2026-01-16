package net.discordjug.javabot.systems.staff_commands.forms;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import xyz.dynxsty.dih4jda.interactions.components.ModalHandler;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

/**
 * Handle forms interactions, including buttons and submissions modals.
 */
@AutoDetectableComponentHandler(FormInteractionManager.FORM_COMPONENT_ID)
@RequiredArgsConstructor
public class FormInteractionManager implements ButtonHandler, ModalHandler {

	/**
	 * Date and time format used in forms.
	 */
	public static final DateFormat DATE_FORMAT;

	/**
	 * String representation of the date and time format used in forms.
	 */
	public static final String DATE_FORMAT_STRING;

	/**
	 * Component ID used for form buttons and modals.
	 */
	public static final String FORM_COMPONENT_ID = "modal-form";
	private static final String FORM_NOT_FOUND_MSG = "This form was not found in the database. Please report this to the server staff.";

	private final FormsRepository formsRepo;

	static {
		DATE_FORMAT_STRING = "dd/MM/yyyy HH:mm";
		DATE_FORMAT = new SimpleDateFormat(FormInteractionManager.DATE_FORMAT_STRING, Locale.ENGLISH);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Closes the form, preventing further submissions and disabling associated
	 * buttons from a message this form is attached to, if any.
	 *
	 * @param guild guild this form is located in.
	 * @param form  form to close.
	 */
	public void closeForm(Guild guild, FormData form) {
		formsRepo.closeForm(form);

		if (form.isAttached()) {
			TextChannel formChannel = guild.getTextChannelById(form.getMessageChannel().get());
			formChannel.retrieveMessageById(form.getMessageId().get()).queue(msg -> {
				mapFormMessageButtons(msg, btn -> {
					String cptId = btn.getId();
					String[] split = ComponentIdBuilder.split(cptId);
					if (split[0].equals(FormInteractionManager.FORM_COMPONENT_ID)
							&& split[1].equals(Long.toString(form.id()))) {
						return btn.asDisabled();
					}
					return btn;
				});
			}, t -> {});
		}
	}

	@Override
	public void handleButton(ButtonInteractionEvent event, Button button) {
		long formId = Long.parseLong(ComponentIdBuilder.split(button.getId())[1]);
		Optional<FormData> formOpt = formsRepo.getForm(formId);
		if (!formOpt.isPresent()) {
			event.reply(FORM_NOT_FOUND_MSG).setEphemeral(true).queue();
			return;
		}
		FormData form = formOpt.get();
		if (!checkNotClosed(form)) {
			event.reply("This form is not accepting new submissions.").setEphemeral(true).queue();
			if (!form.closed()) {
				closeForm(event.getGuild(), form);
			}
			return;
		}

		if (form.onetime() && formsRepo.hasSubmitted(event.getUser(), form)) {
			event.reply("You have already submitted this form").setEphemeral(true).queue();
			return;
		}

		Modal modal = createFormModal(form);

		event.replyModal(modal).queue();
	}

	@Override
	public void handleModal(ModalInteractionEvent event, List<ModalMapping> values) {
		event.deferReply().setEphemeral(true).queue();
		long formId = Long.parseLong(ComponentIdBuilder.split(event.getModalId())[1]);
		Optional<FormData> formOpt = formsRepo.getForm(formId);
		if (!formOpt.isPresent()) {
			event.reply(FORM_NOT_FOUND_MSG).setEphemeral(true).queue();
			return;
		}

		FormData form = formOpt.get();

		if (!checkNotClosed(form)) {
			event.getHook().sendMessage("This form is not accepting new submissions.").queue();
			return;
		}

		if (form.onetime() && formsRepo.hasSubmitted(event.getUser(), form)) {
			event.getHook().sendMessage("You have already submitted this form").queue();
			return;
		}

		TextChannel channel = event.getGuild().getTextChannelById(form.submitChannel());
		if (channel == null) {
			event.getHook()
					.sendMessage("We couldn't receive your submission due to an error. Please contact server staff.")
					.queue();
			return;
		}

		channel.sendMessageEmbeds(createSubmissionEmbed(form, values, event.getMember())).queue(msg -> {
			formsRepo.addSubmission(event.getUser(), form, msg);
		});

		event.getHook()
				.sendMessage(form.submitMessage() == null ? "Your submission was received!" : form.submitMessage())
				.queue();
	}

	/**
	 * Modifies buttons in a message using given function for mapping.
	 *
	 * @param msg    message to modify buttons in.
	 * @param mapper mapping function.
	 */
	public void mapFormMessageButtons(Message msg, Function<Button, Button> mapper) {
		List<ActionRow> components = msg.getActionRows().stream().map(row -> {
			ItemComponent[] cpts = row.getComponents().stream().map(cpt -> {
				if (cpt instanceof Button btn) {
					return mapper.apply(btn);
				}
				return cpt;
			}).toList().toArray(new ItemComponent[0]);
			if (cpts.length == 0) {
				return null;
			}
			return ActionRow.of(cpts);
		}).filter(Objects::nonNull).toList();
		msg.editMessageComponents(components).queue();
	}

	/**
	 * Re-opens the form, re-enabling associated buttons in message it's attached
	 * to, if any.
	 *
	 * @param guild guild this form is contained in.
	 * @param form  form to re-open.
	 */
	public void reopenForm(Guild guild, FormData form) {
		formsRepo.reopenForm(form);

		if (form.isAttached()) {
			TextChannel formChannel = guild.getTextChannelById(form.getMessageChannel().get());
			formChannel.retrieveMessageById(form.getMessageId().get()).queue(msg -> {
				mapFormMessageButtons(msg, btn -> {
					String cptId = btn.getId();
					String[] split = ComponentIdBuilder.split(cptId);
					if (split[0].equals(FormInteractionManager.FORM_COMPONENT_ID)
							&& split[1].equals(Long.toString(form.id()))) {
						return btn.asEnabled();
					}
					return btn;
				});
			}, t -> {});
		}
	}

	/**
	 * Creates a submission modal for the given form.
	 *
	 * @param form form to open submission modal for.
	 * @return submission modal to be presented to the user.
	 */
	public static Modal createFormModal(FormData form) {
		Modal modal = Modal.create(ComponentIdBuilder.build(FORM_COMPONENT_ID, form.id()), form.title())
				.addComponents(form.createComponents()).build();
		return modal;
	}

	/**
	 * Gets expiration time from the slash comamnd event.
	 *
	 * @param event slash event to get expiration from.
	 * @return an optional containing expiration time,
	 *         {@link FormData#EXPIRATION_PERMANENT} if none given, or an empty
	 *         optional if it's invalid.
	 * @throws IllegalArgumentException if the date doesn't follow the format.
	 */
	public static Optional<Instant> parseExpiration(SlashCommandInteractionEvent event)
			throws IllegalArgumentException {
		String expirationStr = event.getOption("expiration", null, OptionMapping::getAsString);
		Optional<Instant> expiration;
		if (expirationStr == null) {
			expiration = Optional.empty();
		} else {
			try {
				expiration = Optional.of(FormInteractionManager.DATE_FORMAT.parse(expirationStr).toInstant());
			} catch (ParseException e) {
				throw new IllegalArgumentException("Invalid date. You should follow the format `"
						+ FormInteractionManager.DATE_FORMAT_STRING + "`.");
			}
		}

		if (expiration.isPresent() && expiration.get().isBefore(Instant.now())) {
			throw new IllegalArgumentException("The expiration date shouldn't be in the past");
		}
		return expiration;
	}

	private static boolean checkNotClosed(FormData data) {
		if (data.closed() || data.hasExpired()) {
			return false;
		}

		return true;
	}

	private static MessageEmbed createSubmissionEmbed(FormData form, List<ModalMapping> values, Member author) {
		EmbedBuilder builder = new EmbedBuilder().setTitle("New form submission received")
				.setAuthor(author.getEffectiveName(), null, author.getEffectiveAvatarUrl()).setTimestamp(Instant.now());
		builder.addField("Sender", author.getAsMention(), true).addField("Title", form.title(), true);

		int len = Math.min(values.size(), form.fields().size());
		for (int i = 0; i < len; i++) {
			ModalMapping mapping = values.get(i);
			FormField field = form.fields().get(i);
			String value = mapping.getAsString();
			builder.addField(field.label(), value == null ? "*Empty*" : "```\n" + value + "\n```", false);
		}

		return builder.build();
	}
}
