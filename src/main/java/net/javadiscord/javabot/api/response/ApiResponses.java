package net.javadiscord.javabot.api.response;

public class ApiResponses {
		public static String INTERNAL_SERVER_ERROR = buildError("internal_server_error", "Internal server error encountered");
		public static String NOT_FOUND_ERROR = buildError("not_found", "Resource not found");
		public static String BAD_REQUEST = buildError("bad_request", "Bad Request");
		public static String REQUEST_METHOD_NOT_SUPPORTED = buildError("unsupported_method", "Request method not supported");
		public static String INVALID_GUILD_IN_REQUEST = buildError("invalid_guild", "Invalid 'guild_id' in request");
		public static String INVALID_USER_IN_REQUEST = buildError("invalid_user", "Invalid 'user_id' in request");
		public static String INVALID_NUMBER_IN_REQUEST = buildError("invalid_number", "Invalid number in request");

		private static String buildError(String name, String description) {
			return new ApiResponseBuilder().add("error", name).add("error_description", description).build();
		}
}
