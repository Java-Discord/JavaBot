package net.discordjug.javabot.api.exception;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Handles all Rest Exceptions.
 */
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Handles all {@link MethodArgumentTypeMismatchException}s.
	 *
	 * @param e The {@link MethodArgumentTypeMismatchException} which was thrown.
	 * @return The {@link ResponseEntity} containing the {@link ErrorResponse}.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(@NotNull MethodArgumentTypeMismatchException e) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getLocalizedMessage(),
				String.format("%s should be of type %s", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getName() : ""));
	}

	/**
	 * Handles all {@link InvalidEntityIdException}s.
	 *
	 * @param e The {@link InvalidEntityIdException} which was thrown.
	 * @return The {@link ResponseEntity} containing the {@link ErrorResponse}.
	 */
	@ExceptionHandler(InvalidEntityIdException.class)
	public ResponseEntity<ErrorResponse> handleInvalidEntityIdException(@NotNull InvalidEntityIdException e) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getLocalizedMessage(),
				"Entity should be of type: " + e.getRequiredEntity().getName());
	}

	/**
	 * Handles all {@link InternalServerException}s.
	 *
	 * @param e The {@link InternalServerException} which was thrown.
	 * @return The {@link ResponseEntity} containing the {@link ErrorResponse}.
	 */
	@ExceptionHandler(InternalServerException.class)
	public ResponseEntity<ErrorResponse> handleInternalServerException(@NotNull InternalServerException e) {
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getCause().getLocalizedMessage(), e.getLocalizedMessage());
	}

	/**
	 * Handles all generic {@link Exception}.
	 *
	 * @param e The {@link Exception} which was thrown.
	 * @return The {@link ResponseEntity} containing the {@link ErrorResponse}.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(@NotNull Exception e) {
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
	}

	private @NotNull ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String... errors) {
		ErrorResponse error = new ErrorResponse(status, message, errors);
		return new ResponseEntity<>(error, status);
	}
}
