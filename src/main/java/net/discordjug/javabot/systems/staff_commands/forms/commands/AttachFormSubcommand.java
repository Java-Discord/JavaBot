package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

/**
 * The `/form attach` command.
 */
public class AttachFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public AttachFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("attach", "Attach a form to a message").addOptions(
				new OptionData(OptionType.INTEGER, "form-id", "ID of the form to attach", true, true),
				new OptionData(OptionType.STRING, "message-id", "ID of the message to attach the form to", true),
				new OptionData(OptionType.CHANNEL, "channel",
						"Channel of the message. Required if the message is in a different channel"),
				new OptionData(OptionType.STRING, "button-label", "Label of the submit button. Default is \"Submit\""),
				new OptionData(OptionType.STRING, "button-style", "Submit button style. Defaults to primary", false,
						true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		event.deferReply().setEphemeral(true).queue();

		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();

		if (form.getMessageChannel() != null && form.getMessageId() != null) {
			event.getHook()
					.sendMessage("The form seems to already be attached to a message. Detach it before continuing.")
					.queue();
			return;
		}

		if (form.getFields().isEmpty()) {
			event.getHook().sendMessage("You can't attach a form with no fields.").queue();
			return;
		}

		String messageId = event.getOption("message-id", OptionMapping::getAsString);
		GuildChannel channel = event.getOption("channel", event.getChannel().asGuildMessageChannel(),
				OptionMapping::getAsChannel);

		if (channel == null) {
			event.getHook().sendMessage("A channel with this ID was not found.").setEphemeral(true).queue();
			return;
		}

		if (!(channel instanceof MessageChannel msgChannel)) {
			event.getHook().sendMessage("You must specify a message channel").setEphemeral(true).queue();
			return;
		}

		String buttonLabel = event.getOption("button-label", "Submit", OptionMapping::getAsString);
		ButtonStyle style = event.getOption("button-style", ButtonStyle.PRIMARY, t -> {
			try {
				return ButtonStyle.valueOf(t.getAsString().toUpperCase());
			} catch (IllegalArgumentException e) {
				return ButtonStyle.PRIMARY;
			}
		});

		msgChannel.retrieveMessageById(messageId).queue(message -> {
			attachFormToMessage(message, buttonLabel, style, form);
			formsRepo.attachForm(form, msgChannel, message);
			event.getHook()
					.sendMessage("Successfully attached the form to the [message](" + message.getJumpUrl() + ")!")
					.queue();
		}, t -> event.getHook().sendMessage("A message with this ID was not found").queue());
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		switch (target.getName()) {
			case "form-id" -> event.replyChoices(
					formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
					.queue();
			case "button-style" -> event.replyChoices(
					Set.of(ButtonStyle.DANGER, ButtonStyle.PRIMARY, ButtonStyle.SECONDARY, ButtonStyle.SUCCESS).stream()
							.map(style -> new Choice(style.name(), style.name())).toList())
					.queue();
			default -> {}
		}
	}

	private static void attachFormToMessage(Message message, String buttonLabel, ButtonStyle style, FormData form) {
		List<ActionRow> rows = new ArrayList<>(message.getActionRows());

		Button button = Button.of(style,
				ComponentIdBuilder.build(FormInteractionManager.FORM_COMPONENT_ID, form.getId()), buttonLabel);

		if (form.isClosed() || form.hasExpired()) {
			button = button.asDisabled();
		}

		if (rows.isEmpty() || rows.get(rows.size() - 1).getActionComponents().size() >= 5) {
			rows.add(ActionRow.of(button));
		} else {
			ActionRow lastRow = rows.get(rows.size() - 1);
			ItemComponent[] components = new ItemComponent[lastRow.getComponents().size() + 1];
			System.arraycopy(lastRow.getComponents().toArray(new ItemComponent[0]), 0, components, 0,
					lastRow.getComponents().size());
			components[components.length - 1] = button;
			rows.set(rows.size() - 1, ActionRow.of(components));
		}

		message.editMessageComponents(rows.toArray(new ActionRow[0])).queue();
	}

}
