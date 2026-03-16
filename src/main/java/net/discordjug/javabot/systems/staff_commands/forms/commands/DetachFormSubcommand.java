package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.util.ExceptionLogger;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponentUnion;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

/**
 * The `/form detach` command. This command detaches this form from the message
 * it's attached to. Detaching a form means that any buttons that could be used
 * to bring its input dialog will be removed. See {@link AttachFormSubcommand}
 * for a command for attaching the form to a message.
 * 
 * @see FormData
 */
public class DetachFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public DetachFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("detach",
				"Remove any buttons that could be used to bring the form's input modal")
				.addOptions(new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "ID of the form to attach", true, true)));
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

		if (form.getAttachmentInfo().isEmpty()) {
			event.getHook().sendMessage("This form doesn't seem to be attached to a message").queue();
			return;
		}

		detachFromMessage(form, event.getGuild());
		formsRepo.detachForm(form);

		event.getHook().sendMessage("Form detached!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}

	/**
	 * Detaches the form from a message it's attached to, deleting any associated
	 * buttons. Fails silently if the message was not found.
	 *
	 * @param form  form to detach
	 * @param guild guild this form is contained in
	 */
	public static void detachFromMessage(FormData form, Guild guild) {
		form.getAttachmentInfo().ifPresent(info -> {
			long messageChannelId = info.messageChannelId();
			long messageId = info.messageId();
			TextChannel formChannel = guild.getTextChannelById(messageChannelId);
			if (formChannel != null) {
				formChannel.retrieveMessageById(messageId).queue(msg -> {
					List<ActionRow> components = msg.getComponents().stream().map(msgComponent -> {
						ActionRow row = msgComponent.asActionRow();
						List<ActionRowChildComponentUnion> cpts = row.getComponents().stream().filter(cpt -> {
							if (cpt instanceof Button btn) {
								String cptId = btn.getCustomId();
								String[] split = ComponentIdBuilder.split(cptId);
								if (split[0].equals(FormInteractionManager.FORM_COMPONENT_ID)) {
									return !split[1].equals(Long.toString(form.id()));
								}
							}
							return true;
						}).toList();
						if (cpts.isEmpty()) {
							return null;
						}
						return ActionRow.of(cpts);
					}).filter(Objects::nonNull).toList();
					msg.editMessageComponents(components).queue();
				}, ExceptionLogger::capture);
			}
		});
	}

}
