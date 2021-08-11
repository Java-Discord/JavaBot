package com.javadiscord.javabot.data.commands;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.data.MigrationUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * This subcommand shows a list of all available migrations, and a short preview
 * of their source code.
 */
public class MigrationsListSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		try (var s = Files.list(MigrationUtils.getMigrationsDirectory())) {
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("List of Runnable Migrations");
			var paths = s.filter(path -> path.getFileName().toString().endsWith(".sql")).collect(Collectors.toList());
			if (paths.isEmpty()) {
				embedBuilder.setDescription("There are no migrations to run. Please add them to the `/migrations/` resource directory.");
				return event.replyEmbeds(embedBuilder.build());
			}
			paths.forEach(path -> {
				StringBuilder sb = new StringBuilder(150);
				sb.append("```sql\n");
				try {
					String sql = Files.readString(path);
					sb.append(sql, 0, Math.min(sql.length(), 100));
					if (sql.length() > 100) sb.append("...");
				} catch (IOException e) {
					e.printStackTrace();
					sb.append("Error: Could not read SQL: ").append(e.getMessage());
				}
				sb.append("\n```");
				embedBuilder.addField(path.getFileName().toString(), sb.toString(), false);
			});
			return event.replyEmbeds(embedBuilder.build());
		} catch (IOException | URISyntaxException e) {
			return Responses.error(event, e.getMessage());
		}
	}
}
