package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
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
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

/**
 * The `/form attach` command. This command can be used to attach a form to an
 * existing message. "Attaching" a form to message in this case mean that the
 * bot will modify the target message with a button, that when interacted with,
 * will bring up a modal where the user can input their data. See
 * {@link DetachFormSubcommand} for a command used to detach the form from a
 * message.
 * 
 * @see FormData
 */
public class AttachFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public AttachFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("attach", "Add a button for bringing up the form to a message").addOptions(
				new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "ID of the form to attach", true, true),
				new OptionData(OptionType.STRING, "message-id", "ID of the message to attach the form to", true),
				new OptionData(OptionType.CHANNEL, "channel",
						"Channel of the message. Required if the message is in a different channel"),
				new OptionData(OptionType.STRING, "button-label", "Label of the submit button. Default is \"Submit\""),
				new OptionData(OptionType.STRING, "button-style", "Submit button style. Defaults to primary", false,
						true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		event.deferReply().setEphemeral(true).queue();

		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();

		if (form.isAttached()) {
			event.getHook()
					.sendMessage("The form seems to already be attached to a message. Detach it before continuing.")
					.queue();
			return;
		}

		if (form.fields().isEmpty()) {
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
		net.dv8tion.jda.api.components.buttons.ButtonStyle style = event.getOption("button-style", ButtonStyle.PRIMARY,
				t -> {
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
		}, _ -> event.getHook().sendMessage("A message with this ID was not found").queue());
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		if (!handleFormIDAutocomplete(event, target) && "button-style".equals(target.getName())) {
			event.replyChoices(
					Set.of(ButtonStyle.DANGER, ButtonStyle.PRIMARY, ButtonStyle.SECONDARY, ButtonStyle.SUCCESS).stream()
							.map(style -> new Choice(style.name(), style.name())).toList())
					.queue();
		}
	}

	private static void attachFormToMessage(Message message, String buttonLabel, ButtonStyle style, FormData form) {
		List<ActionRow> rows = new ArrayList<>(
				message.getComponents().stream().map(MessageTopLevelComponentUnion::asActionRow).toList());

		Button button = Button.of(style, ComponentIdBuilder.build(FormInteractionManager.FORM_COMPONENT_ID, form.id()),
				buttonLabel);

		if (form.closed() || form.hasExpired()) {
			button = button.asDisabled();
		}

		if (rows.isEmpty() || rows.get(rows.size() - 1).getActionComponents().size() >= 5) {
			rows.add(ActionRow.of(button));
		} else {
			ActionRow lastRow = rows.get(rows.size() - 1);
			List<ActionRowChildComponent> components = new ArrayList<>(lastRow.getComponents().size() + 1);
			Collections.copy(components, lastRow.getComponents());
			components.set(components.size() - 1, button);
			rows.set(rows.size() - 1, ActionRow.of(components));
		}

		message.editMessageComponents(rows.toArray(new ActionRow[0])).queue();
	}

}
