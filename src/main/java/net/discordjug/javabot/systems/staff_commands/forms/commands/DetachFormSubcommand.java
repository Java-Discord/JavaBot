package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

/**
 * The `/form detach` command.
 */
public class DetachFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public DetachFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("detach", "Detach a form from a message")
				.addOptions(new OptionData(OptionType.INTEGER, "form-id", "ID of the form to attach", true, true)));
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

		if (!form.isAttached()) {
			event.getHook().sendMessage("This form doesn't seem to be attached to a message").queue();
			return;
		}

		detachFromMessage(form, event.getGuild());
		formsRepo.detachForm(form);

		event.getHook().sendMessage("Form detached!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		event.replyChoices(
				formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.getId())).toList())
				.queue();
	}

	/**
	 * Detaches the form from a message it's attached to, deleting any associated
	 * buttons. Fails silently if the message was not found.
	 *
	 * @param form  form to detach
	 * @param guild guild this form is contained in
	 */
	public static void detachFromMessage(FormData form, Guild guild) {
		if(!form.isAttached()) return;
		TextChannel formChannel = guild.getTextChannelById(form.getMessageChannel().get());
		formChannel.retrieveMessageById(form.getMessageId().get()).queue(msg -> {
			List<ActionRow> components = msg.getActionRows().stream().map(row -> {
				ItemComponent[] cpts = row.getComponents().stream().filter(cpt -> {
					if (cpt instanceof Button btn) {
						String cptId = btn.getId();
						String[] split = ComponentIdBuilder.split(cptId);
						if (split[0].equals(FormInteractionManager.FORM_COMPONENT_ID)) {
							return !split[1].equals(Long.toString(form.getId()));
						}
					}
					return true;
				}).toList().toArray(new ItemComponent[0]);
				if (cpts.length == 0) {
					return null;
				}
				return ActionRow.of(cpts);
			}).filter(Objects::nonNull).toList();
			msg.editMessageComponents(components).queue();
		}, t -> {});
	}

}
