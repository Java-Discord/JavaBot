package net.javadiscord.javabot.api.routes.qotw_leaderboard;

import net.dv8tion.jda.api.entities.Guild;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.api.routes.qotw_leaderboard.model.QOTWLeaderboardData;
import net.javadiscord.javabot.api.response.ApiResponseBuilder;
import net.javadiscord.javabot.api.response.ApiResponses;
import net.javadiscord.javabot.api.routes.JDAEntity;
import net.javadiscord.javabot.api.routes.qotw_leaderboard.model.QOTWMemberData;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.util.Checks;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QOTWLeaderboardController implements JDAEntity {

	@GetMapping(
			value = "{guild_id}/qotw/leaderboard",
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<String> getQOTWLeaderboard(
			@PathVariable(value = "guild_id") String guildId,
			@RequestParam(value = "amount", defaultValue = "3") String amountParam
	) {
		Guild guild = getJDA().getGuildById(guildId);
		if (guild == null) {
			return new ResponseEntity<>(ApiResponses.INVALID_GUILD_IN_REQUEST, HttpStatus.BAD_REQUEST);
		}
		if (!Checks.checkInteger(amountParam)) {
			return new ResponseEntity<>(ApiResponses.INVALID_NUMBER_IN_REQUEST, HttpStatus.BAD_REQUEST);
		}
		int amount = Integer.parseInt(amountParam);
		QOTWPointsService service = new QOTWPointsService(Bot.getDataSource());
		List<QOTWMemberData> members = service.getTopMembers(amount, guild).stream()
				.map(p -> {
					QOTWMemberData data = new QOTWMemberData();
					data.setUserId(p.second().getIdLong());
					data.setUserName(p.second().getUser().getName());
					data.setDiscriminator(p.second().getUser().getDiscriminator());
					data.setEffectiveAvatarUrl(p.second().getEffectiveAvatarUrl());
					data.setAccount(p.first());
					return data;
				})
				.toList();
		QOTWLeaderboardData data = new QOTWLeaderboardData();
		data.setAccounts(members);
		return new ResponseEntity<>(new ApiResponseBuilder().add("leaderboard", members).build(), HttpStatus.OK);
	}
}
