package net.discordjug.javabot.listener.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * This {@link MessageFilter} replaces all occurrences of 'fuck' in incoming messages with 'hug'.
 */
@Component
@RequiredArgsConstructor
public class HugFilter implements MessageFilter {

	private static final Pattern FUCKER = Pattern.compile(
			"(fuck)(ing|er|ed|k+)?",
			Pattern.CASE_INSENSITIVE
	);

	@Override
	public MessageModificationStatus processMessage(MessageContent content) {
		if (!content.event().getMessage().getMentions().getUsers().isEmpty()) {
			return MessageModificationStatus.NOT_MODIFIED;
		}
		String before = content.messageText().toString();
		String processed = replaceFucks(content.messageText().toString());
		if (!before.equals(processed)) {
			content.messageText().setLength(0);
			content.messageText().append(processed);
			return MessageModificationStatus.MODIFIED;
		} else {
			return MessageModificationStatus.NOT_MODIFIED;
		}
	}

	private static String processHug(String originalText) {
		// FucK -> HuG, FuCk -> Hug
		return String.valueOf(copyCase(originalText, 0, 'h')) + copyCase(originalText, 1, 'u') +
				copyCase(originalText, 3, 'g');
	}

	private static String replaceFucks(String str) {
		return FUCKER.matcher(str).replaceAll(matchResult -> {
			String theFuck = matchResult.group(1);
			String suffix = Objects.requireNonNullElse(matchResult.group(2), "");
			String processedSuffix = switch (suffix.toLowerCase()) {
				case "er", "ed", "ing" -> copyCase(suffix, 0, 'g') + suffix; // fucking, fucker, fucked
				case "" -> ""; // just fuck
				default -> copyCase(suffix, "g".repeat(suffix.length())); // fuckkkkk...
			};
			return processHug(theFuck) + processedSuffix;
		});
	}

	private static String copyCase(String source, String toChange) {
		if (source.length() != toChange.length()) {
			throw new IllegalArgumentException("lengths differ");
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			sb.append(copyCase(source, i, toChange.charAt(i)));
		}
		return sb.toString();
	}

	private static char copyCase(String original, int index, char newChar) {
		if (Character.isUpperCase(original.charAt(index))) {
			return Character.toUpperCase(newChar);
		} else {
			return newChar;
		}
	}
}
