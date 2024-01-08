package net.discordjug.javabot.systems.staff_commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.config.ScheduledTaskHolder;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import xyz.dynxsty.dih4jda.util.AutoCompleteUtils;

/**
 * This command allows manually executing scheduled tasks.
 */
public class RunScheduledTaskCommand extends SlashCommand implements AutoCompletable{
	
	private ScheduledTaskHolder taskHolder;
	
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig the configuration of the bot
	 * @param taskHolder A Spring object managing scheduled tasks
	 */
	public RunScheduledTaskCommand(BotConfig botConfig, ScheduledTaskHolder taskHolder) {
		setCommandData(Commands.slash("run-task", "(ADMIN ONLY) Run scheduled tasks")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
				.setGuildOnly(true)
				.addOption(OptionType.STRING, "name", "Class name of the task", true, true)
		);
		setRequiredUsers(botConfig.getSystems().getAdminConfig().getAdminUsers());
		
		this.taskHolder = taskHolder;
		
		
	}
	
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String name = event.getOption("name", "", OptionMapping::getAsString);
		if (name.isEmpty()) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		taskHolder
			.getScheduledTasks()
			.stream()
			.filter(r -> name.equals(r.toString()))
			.findAny()
			.ifPresentOrElse(r -> {
				event.deferReply(true).queue();
				try {
					r.getTask().getRunnable().run();
					Responses.success(event.getHook(), "Task successful", "Task was executed successfully").queue();
					//CHECKSTYLE:OFF This is a handler for all sort of failures that could possibly happen
				}catch (RuntimeException e) {
					//CHECKSTYLE:ON
					Responses.error(event, "Task failed with an exception", e.getClass().getName() + (e.getMessage() == null ? "" : ": "+e.getMessage()));
				}
			}, () -> {
				Responses.error(event, "Cannot find task `%s`", name).queue();
			});
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		List<Choice> choices = taskHolder
				.getScheduledTasks()
				.stream()
				.map(r -> new Command.Choice(r.toString(), r.toString()))
				.collect(Collectors.toCollection(ArrayList::new));
		event.replyChoices(AutoCompleteUtils.filterChoices(event, choices)).queue();
	}
}
