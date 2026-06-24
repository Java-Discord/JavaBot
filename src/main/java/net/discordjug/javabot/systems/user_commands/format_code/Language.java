package net.discordjug.javabot.systems.user_commands.format_code;

/**
 * The programming languages supported by the code-formatting commands. Each constant maps a
 * human-readable {@link #displayName} to the {@link #discordName} tag Discord uses for
 * syntax highlighting inside code blocks.
 */
public enum Language {
	/**
	 * The C programming language.
	 */
	C("C", "c"),
	/**
	 * The C++ programming language.
	 */
	CPP("C++", "cpp"),
	/**
	 * The C# programming language.
	 */
	CSHARP("C#", "csharp"),
	/**
	 * Cascading Style Sheets.
	 */
	CSS("CSS", "css"),
	/**
	 * The D programming language.
	 */
	D("D", "d"),
	/**
	 * The Go programming language.
	 */
	GO("Go", "go"),
	/**
	 * HyperText Markup Language.
	 */
	HTML("HTML", "html"),
	/**
	 * The Java programming language.
	 */
	JAVA("Java", "java"),
	/**
	 * The JavaScript programming language.
	 */
	JAVASCRIPT("JavaScript", "javascript"),
	/**
	 * The Kotlin programming language.
	 */
	KOTLIN("Kotlin", "kotlin"),
	/**
	 * The PHP programming language.
	 */
	PHP("PHP", "php"),
	/**
	 * The Python programming language.
	 */
	PYTHON("Python", "python"),
	/**
	 * The Ruby programming language.
	 */
	RUBY("Ruby", "ruby"),
	/**
	 * The Rust programming language.
	 */
	RUST("Rust", "rust"),
	/**
	 * Structured Query Language.
	 */
	SQL("SQL", "sql"),
	/**
	 * The Swift programming language.
	 */
	SWIFT("Swift", "swift"),
	/**
	 * The TypeScript programming language.
	 */
	TYPESCRIPT("TypeScript", "typescript"),
	/**
	 * Extensible Markup Language.
	 */
	XML("XML", "xml"),
	/**
	 * A structured data format.
	 */
	JSON("JSON", "json"),
	/**
	 * Fallback used when the language is unrecognised; renders as plain text.
	 */
	UNKNOWN("Unknown", "txt");

	private final String displayName;
	private final String discordName;

	Language(String displayName, String discordName) {
		this.displayName = displayName;
		this.discordName = discordName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDiscordName() {
		return discordName;
	}


	/**
	 * Resolves a language from a string (e.g. the value of the /format-code "format"
	 * option) by matching its Discord code-fence name, falling back to {@link #UNKNOWN}.
	 *
	 * @param name the code-fence name to look up (case-insensitive)
	 * @return the matching language, or {@link #UNKNOWN} if none matches
	 */
	public static Language fromString(String name) {
		try {
			return Language.valueOf(name.toUpperCase());
		} catch (IllegalArgumentException e) {
			return UNKNOWN;
		}
	}
}