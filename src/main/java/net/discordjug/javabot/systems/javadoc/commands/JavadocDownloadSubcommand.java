package net.discordjug.javabot.systems.javadoc.commands;

import net.discordjug.javabot.systems.javadoc.model.JavadocPackageHost;
import net.discordjug.javabot.systems.javadoc.model.download_strategy.JavadocDownloadStrategy;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class JavadocDownloadSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public JavadocDownloadSubcommand() {
		setCommandData(
				new SubcommandData("download", "Downloads a Javadoc package.")
						.addOptions(
								new OptionData(OptionType.STRING, "group-id", "The package's groupId", true),
								new OptionData(OptionType.STRING, "artifact-id", "The package's artifactId", true),
								new OptionData(OptionType.STRING, "version", "The version to download.", true),
								new OptionData(OptionType.INTEGER, "host", "The package's host.", false)
										.addChoices(Arrays.stream(JavadocPackageHost.values()).map(c -> new Command.Choice(c.getHost(), c.name())).toArray(Command.Choice[]::new))
						)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		final String groupId = event.getOption("group-id", OptionMapping::getAsString);
		final String artifactId = event.getOption("artifact-id", OptionMapping::getAsString);
		final String version = event.getOption("version", OptionMapping::getAsString);
		final String rawHost = event.getOption("host", JavadocPackageHost.JAVADOC_IO.name(), OptionMapping::getAsString);
		if (groupId == null || artifactId == null || version == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		event.deferReply().queue();
		final JavadocPackageHost host = JavadocPackageHost.valueOf(rawHost);
		try {
			final JavadocDownloadStrategy downloadStrategy = host.getDownloadStrategy();
			final URL url = downloadStrategy.buildDownloadUrl(groupId, artifactId, version);
			Responses.info(event.getHook(), "Downloading Package...", "Downloading package from " + url).queue();
			downloadStrategy.download(url);
		} catch (IOException e) {
			event.reply("no good url").queue();
		}
	}
}
