package net.javadiscord.javabot.api.response;

/**
 * Utility class which contains some basic JSON responses.
 */
public class ApiResponses {
	/**
	 * Response used for internal server errors (such as exceptions).
	 */
	public static final String INTERNAL_SERVER_ERROR = buildError("internal_server_error", "Internal server error encountered");
	/**
	 * Response used for invalid {@link net.dv8tion.jda.api.entities.Guild} ids.
	 */
	public static final String INVALID_GUILD_IN_REQUEST = buildError("invalid_guild", "Invalid 'guild_id' in request");
	/**
	 * Response used for invalid {@link net.dv8tion.jda.api.entities.User} ids.
	 */
	public static final String INVALID_USER_IN_REQUEST = buildError("invalid_user", "Invalid 'user_id' in request");
	/**
	 * Response used for invalid integers in the request.
	 */
	public static final String INVALID_NUMBER_IN_REQUEST = buildError("invalid_number", "Invalid number in request");

	private ApiResponses() {
	}

	private static String buildError(String name, String description) {
		return new ApiResponseBuilder().add("error", name).add("error_description", description).build();
	}
}
