package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormUser;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form submissions-export` command.
 */
public class SubmissionsExportFormSubcommand extends Subcommand implements AutoCompletable {

	private final FormsRepository formsRepo;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public SubmissionsExportFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("submissions-export", "Export all of the form's submissions").addOptions(
				new OptionData(OptionType.INTEGER, "form-id", "The ID of a form to get submissions for", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		event.deferReply().setEphemeral(false).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption("form-id", OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("Couldn't find a form with this id").queue();
			return;
		}

		FormData form = formOpt.get();
		Map<FormUser, Integer> submissions = formsRepo.getSubmissionsCountPerUser(form);
		JsonObject root = new JsonObject();
		JsonObject details = new JsonObject();
		JsonArray users = new JsonArray();
		for (Entry<FormUser, Integer> entry : submissions.entrySet()) {
			JsonObject uobj = new JsonObject();
			uobj.addProperty("username", entry.getKey().username());
			uobj.addProperty("submissions", entry.getValue());
			details.add(Long.toString(entry.getKey().id()), uobj);
			users.add(entry.getKey().username());
		}
		root.add("users", users);
		root.add("details", details);
		event.getHook().sendFiles(FileUpload.fromData(gson.toJson(root).getBytes(StandardCharsets.UTF_8),
				"submissions_" + form.id() + ".json")).queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		event.replyChoices(
				formsRepo.getAllForms().stream().map(form -> new Choice(form.toString(), form.id())).toList())
				.queue();
	}
}
