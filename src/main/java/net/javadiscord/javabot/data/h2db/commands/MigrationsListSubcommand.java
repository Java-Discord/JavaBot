package net.javadiscord.javabot.data.h2db.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.MigrationUtils;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * <h3>This class represents the /db-admin migrations-list command.</h3>
 * This subcommand shows a list of all available migrations, and a short preview
 * of their source code.
 */
public class MigrationsListSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public MigrationsListSubcommand() {
		setSubcommandData(new SubcommandData("migrations-list", "(ADMIN ONLY) Shows a list with all available database migrations."));
		requireUsers(Bot.config.getSystems().getAdminConfig().getAdminUsers());
		requirePermissions(Permission.MANAGE_SERVER);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		try (var s = Files.list(MigrationUtils.getMigrationsDirectory())) {
			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("List of Runnable Migrations");
			List<Path> paths = s.filter(path -> path.getFileName().toString().endsWith(".sql")).toList();
			if (paths.isEmpty()) {
				embedBuilder.setDescription("There are no migrations to run. Please add them to the `/migrations/` resource directory.");
				event.replyEmbeds(embedBuilder.build()).queue();
				return;
			}
			paths.forEach(path -> {
				StringBuilder sb = new StringBuilder(150);
				sb.append("```sql\n");
				try {
					String sql = Files.readString(path);
					sb.append(sql, 0, Math.min(sql.length(), 100));
					if (sql.length() > 100) sb.append("...");
				} catch (IOException e) {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					sb.append("Error: Could not read SQL: ").append(e.getMessage());
				}
				sb.append("\n```");
				embedBuilder.addField(path.getFileName().toString(), sb.toString(), false);
			});
			event.replyEmbeds(embedBuilder.build()).queue();
		} catch (IOException | URISyntaxException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, e.getMessage()).queue();
		}
	}
}
