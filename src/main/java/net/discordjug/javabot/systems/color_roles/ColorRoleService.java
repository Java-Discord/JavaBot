package net.discordjug.javabot.systems.color_roles;

import java.awt.Color;
import java.util.List;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.color_roles.dao.ColorRoleRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

@Service
public class ColorRoleService extends ListenerAdapter {
	
	private final ColorRoleRepository repo;
	private final BotConfig config;
	
	public ColorRoleService(ColorRoleRepository repo, BotConfig config) {
		this.repo = repo;
		this.config = config;
	}
	
	public synchronized void addColor(Member member, Color color) {
		repo.getFromColor(member.getGuild().getIdLong(), color)
			.ifPresentOrElse(colorRole -> {
				member.getGuild().addRoleToMember(member, member.getGuild().getRoleById(colorRole.roleId())).queue();
				repo.addRoleToMember(colorRole.roleId(), member.getIdLong());
			}, () -> {
				member.getGuild().createRole()
					.setName(encodeColor(color))
					.setColor(color)
					.queue(role -> {
						member.getGuild().addRoleToMember(member, role).queue();
						repo.addColorRole(new ColorRole(member.getGuild().getIdLong(), role.getIdLong(), color), member.getIdLong());
						member.getGuild().modifyRolePositions()
							.selectPosition(role)
							.moveAbove(config.get(member.getGuild()).getModerationConfig().getColorRoleChangeRole())
							.queue();
					});
			});
	}

	public synchronized boolean removeColor(Member member) {
		boolean changed = false;
		for (ColorRole color : repo.getColorRolesOfMember(member.getGuild().getIdLong(), member.getIdLong())) {
			repo.removeRoleFromMember(color.roleId(), member.getIdLong());
			Role role = member.getGuild().getRoleById(color.roleId());
			if (repo.cleanRoleIfNoMembers(color.roleId())) {
				role.delete().queue();
			} else {
				member.getGuild().removeRoleFromMember(member, role).queue();
			}
			changed = true;
		}
		return changed;
	}
	
	@Override
	public void onGuildMemberUpdate(GuildMemberUpdateEvent event) {
		// Remove color roles if a member if they lost the relevant role
		// since members are not cached, we cannot use specific event types
		if (!event.getMember().getUnsortedRoles().contains(config.get(event.getGuild()).getModerationConfig().getColorRoleChangeRole())) {
			removeColor(event.getMember());
		}
	}
	
	@Override
	public synchronized void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		for (ColorRole color : repo.getColorRolesOfMember(event.getGuild().getIdLong(), event.getUser().getIdLong())) {
			repo.removeRoleFromMember(color.roleId(), event.getUser().getIdLong());
			Role role = event.getGuild().getRoleById(color.roleId());
			if (repo.cleanRoleIfNoMembers(color.roleId())) {
				role.delete().queue();
			}
		}
	}

	public List<ColorRole> getColorRoles(Guild guild) {
		return repo.getColorRoles(guild.getIdLong());
	}
	
	public String encodeColor(Color color) {
		return String.format("#%2h%2h%2h", color.getRed(), color.getGreen(), color.getBlue()).replace(' ', '0');
	}
}
