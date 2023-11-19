package net.discordjug.javabot.util;

/**
 * Utility class to help indent strings that contain code.
 */
public class IndentationHelper {
	/**
	 * Enum which denotes the different types of indentation possible.
	 * When {@link #NULL} is passed to any method in this class, the output of said method should match the input.
	 */
	public enum IndentationType {
		/**
		 * A tab character (\t) should be used for indentation.
		 */
		TABS("\t"),
		/**
		 * Four spaces should be used for indentation.
		 */
		FOUR_SPACES("    "),
		/**
		 * Two spaces should be used for indentation.
		 */
		TWO_SPACES("  "),
		/**
		 * Doesn't define an indentation type but
		 * Is used as a substitute to null to indicate that a String given to any method in {@link IndentationHelper} should not be changed.
		 */
		NULL("");

		/**
		 * Holds the characters used for indentation of the type.
		 */
		private final String pattern;

		/**
		 * Constructs the indentation type.
		 *
		 * @param pattern The pattern to be used as indentation
		 */
		IndentationType(String pattern) {
			this.pattern = pattern;
		}

		/**
		 * Get the pattern for a given Indentation type.
		 *
		 * @return the pattern to be used for indenting.
		 */
		public String getPattern() {
			return pattern;
		}

		/**
		 * Returns the number of characters a pattern is using.
		 *
		 * @return the number of characters the pattern of this type consists of.
		 */
		private int getNumberOfChars() {
			return pattern.length();
		}
	}

	/**
	 * Private enum to denote the current State of the Indentation Process.
	 */
	private enum IndentationState {
		/**
		 * Denotes that the Process is currently in a codeblock and should indent the code accordingly.
		 */
		CODE,
		/**
		 * Denotes that the Process is currently in a String literal.
		 */
		STRING,
		/**
		 * Denotes that the Indentation Process is currently in a character literal.
		 */
		CHARACTER,
		/**
		 * Denotes that the Process is currently in a single line comment.
		 */
		SINGLE_LINE_COMMENT,
		/**
		 * Denotes that the process is inside a multi line comment or a javadoc.
		 */
		MULTI_LINE_COMMENT
	}

	private final String text;
	private final IndentationType type;
	private boolean startOfLine = true;
	private int numberOfBrackets = 0;
	private StringBuilder builder;

	private IndentationHelper(String text, IndentationType type) {
		this.text = text;
		this.type = type;
		this.builder = new StringBuilder((int) (text.length() * 1.25f));
	}

	/**
	 * Aims to indent the given String using the pattern provided. Will return the String unchanged if {@link IndentationHelper.IndentationType#NULL} is passed as the IndentationType parameter.
	 *
	 * @param text The text that should be indented.
	 * @param type The type of indentation to be used.
	 * @return The indented String with the format specified.
	 */
	public static String formatIndentation(String text, IndentationType type) {
		return new IndentationHelper(text, type).formatIndentation();
	}

	private String formatIndentation() {
		if (type == IndentationType.NULL) {
			return text;
		}
		IndentationState currentState = IndentationState.CODE;
		for (int i = 0; i < text.length(); i++) {
			char current = text.charAt(i);
			if (startOfLine && Character.isWhitespace(current) && current != '\n') {
				continue;
			}
			builder.append(current);
			if (current == '\n') {
				builder.append(type.getPattern().repeat(Math.max(numberOfBrackets, 0)));
				startOfLine = true;
			}
			currentState = switch (currentState) {
				case CODE -> {
					IndentationState state = processTokenInCode(i,current);
					i += state == IndentationState.MULTI_LINE_COMMENT ? 1:0;
					yield state;
				}
				case STRING -> {
					if (current == '\"' && !isEscaped(builder, builder.length() - 1)) {
						yield IndentationState.CODE;
					}
					yield IndentationState.STRING;
				}
				case CHARACTER -> {
					if (current == '\'' && !isEscaped(builder, builder.length() - 1)) {
						yield IndentationState.CODE;
					}
					yield IndentationState.CHARACTER;
				}
				case SINGLE_LINE_COMMENT ->
					current == '\n' ? IndentationState.CODE : IndentationState.SINGLE_LINE_COMMENT;
				case MULTI_LINE_COMMENT -> {
					if (current == '*' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
						yield IndentationState.CODE;
					}
					yield IndentationState.MULTI_LINE_COMMENT;
				}
			};
			if (!Character.isWhitespace(current) && startOfLine) {
				startOfLine = false;
			}
		}
		return builder.toString();
	}

	private IndentationState processTokenInCode(int i, char current) {
		switch (current) {
			case '{' -> numberOfBrackets++;
			case '}' -> {
				numberOfBrackets--;
				if (startOfLine && builder.length() - type.getNumberOfChars() - 1 >= 0) {
					builder.replace(builder.length() - type.getNumberOfChars() - 1, builder.length(), "}");
				}
			}
			case '\'' -> {
				return IndentationState.CHARACTER;
			}
			case '\"' -> {
				return IndentationState.STRING;
			}
			case '/' -> {
				if (i + 1 < text.length()) {
					if (text.charAt(i + 1) == '/') {
						return IndentationState.SINGLE_LINE_COMMENT;
					} else if (text.charAt(i + 1) == '*') {
						builder.append(text.charAt(++i));
						return IndentationState.MULTI_LINE_COMMENT;
					}
				}
			}
		}
		return IndentationState.CODE;
	}

	/**
	 * Determines if the character in the StringBuilder at the specified position is escaped.
	 *
	 * @param builder the StringBuilder which holds the current String
	 * @param index   The index at which the character to be checked is located.
	 * @return True, if the escape character is referring to the character at the index, false otherwise.
	 */
	private static boolean isEscaped(StringBuilder builder, int index) {
		int numberOfCharacters = 0;
		for (--index;index > 0 && index < builder.length() && builder.charAt(index) == '\\'; index--) {
			numberOfCharacters++;
		}
		return numberOfCharacters % 2 == 1;
	}
}