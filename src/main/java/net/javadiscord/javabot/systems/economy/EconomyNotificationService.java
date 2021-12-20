package net.javadiscord.javabot.systems.economy;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.economy.dao.AccountRepository;
import net.javadiscord.javabot.systems.economy.model.Transaction;

import java.awt.*;
import java.sql.SQLException;
import java.time.ZoneOffset;

@Slf4j
public class EconomyNotificationService {

	/**
	 * Sends notifications to the recipient of a transaction. The sender should
	 * already be aware of the transaction, so there's no need to send them a
	 * message as well.
	 * @param transaction The transaction to send messages about.
	 * @param event The event which triggered the transaction.
	 */
	public void sendTransactionNotification(Transaction transaction, SlashCommandEvent event) {
		if (transaction.getToUserId() != null) {
			try (var con = Bot.dataSource.getConnection()) {
				var prefs = new AccountRepository(con).getPreferences(transaction.getToUserId());
				if (prefs.isReceiveTransactionDms()) {
					Bot.asyncPool.submit(() -> {
						var toUser = event.getJDA().retrieveUserById(transaction.getToUserId()).complete();
						String fromUserName = "System";
						if (transaction.getFromUserId() != null) {
							var fromUser = event.getJDA().retrieveUserById(transaction.getFromUserId()).complete();
							fromUserName = fromUser.getAsTag();
						}
						String transactionMessage = "";
						if (transaction.getMessage() != null) {
							transactionMessage = "The following message was sent:\n> " + transaction.getMessage() + "\n";
						}
						var message = String.format(
								"""
										You have received `%,d` credits from **%s**.
										%sFor more information, please use the `/economy account` command anywhere in the Java Discord server.
										If you would like to stop receiving these notifications, please change your preferences with the `/economy preferences` command.""",
								transaction.getValue(),
								fromUserName,
								transactionMessage
						);
						EmbedBuilder embedBuilder = new EmbedBuilder()
								.setTitle("Transaction Notification")
								.setDescription(message)
								.setTimestamp(transaction.getCreatedAt().atOffset(ZoneOffset.UTC))
								.setColor(Color.GREEN);
						toUser.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessageEmbeds(embedBuilder.build())).queue();
					});
				}
			} catch (SQLException e) {
				log.error("SQL error while sending transaction notification.", e);
			}
		}
	}
}
