package net.discordjug.javabot.systems.custom_vc;

import java.util.function.BiConsumer;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.annotations.AutoDetectableComponentHandler;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

/**
 * Handles buttons for managing custom voice channels.
 */
@AutoDetectableComponentHandler(CustomVCButtonHandler.COMPONENT_ID)
@RequiredArgsConstructor
public class CustomVCButtonHandler implements ButtonHandler {
	static final String COMPONENT_ID = "custom-vc";
	
	private static final String MAKE_PRIVATE_ID = "make-private";
	private static final String MAKE_PUBLIC_ID = "make-public";
	
	private final CustomVCRepository repository;
	
	public Button createMakePrivateButton() {
		return Button.primary(ComponentIdBuilder.build(COMPONENT_ID, MAKE_PRIVATE_ID), "make VC private");
	}
	
	public Button createMakePublicButton() {
		return Button.primary(ComponentIdBuilder.build(COMPONENT_ID, MAKE_PUBLIC_ID), "make VC public");
	}

	@Override
	public void handleButton(ButtonInteractionEvent event, Button button) {
		if(!repository.isCustomVoiceChannel(event.getChannelIdLong()) ||
				repository.getOwnerId(event.getChannelIdLong()) != event.getMember().getIdLong()) {
			Responses.error(event, "Only the VC owner can use this.").queue();
			return;
		}
		String[] id = ComponentIdBuilder.split(button.getCustomId());
		switch (id[1]) {
		case MAKE_PRIVATE_ID -> changeVisibility(event, createMakePublicButton(), "This voice channel is now private.",
				PermissionOverrideAction::setDenied);

		case MAKE_PUBLIC_ID -> changeVisibility(event, createMakePrivateButton(), "This voice channel is now public.",
				PermissionOverrideAction::setAllowed);
		default -> Responses.error(event, "Unknown button").queue();
		}
	}

	private void changeVisibility(ButtonInteractionEvent event, Button newButton, String messageContent,
			BiConsumer<PermissionOverrideAction, Permission> permissionModifier) {
		PermissionOverrideAction permissionOverrideAction = event
			.getGuildChannel()
			.asVoiceChannel()
			.upsertPermissionOverride(event.getGuild().getPublicRole());
		permissionModifier.accept(permissionOverrideAction, Permission.VIEW_CHANNEL);
		permissionOverrideAction.queue();
		event
			.editButton(newButton)
			.flatMap(edited -> {
				return event
						.getHook()
						.setEphemeral(true)
						.sendMessage(messageContent);
			})
			.queue();
	}
}
