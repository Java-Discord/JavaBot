package net.javadiscord.javabot.api.exception;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;

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
		ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST, e.getLocalizedMessage(),
				String.format("%s should be of type %s", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getName() : ""));
		return new ResponseEntity<>(error, error.status());
	}

	/**
	 * Handles all {@link InvalidEntityIdException}s.
	 *
	 * @param e The {@link InvalidEntityIdException} which was thrown.
	 * @return The {@link ResponseEntity} containing the {@link ErrorResponse}.
	 */
	@ExceptionHandler(InvalidEntityIdException.class)
	public ResponseEntity<ErrorResponse> handleInvalidEntityIdException(@NotNull InvalidEntityIdException e) {
		ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST, e.getLocalizedMessage(), "Entity should be of type: " + e.getRequiredEntity().getName());
		return new ResponseEntity<>(error, error.status());
	}

	/**
	 * Handles all {@link SQLException}s.
	 *
	 * @param e The {@link SQLException} which was thrown.
	 * @return The {@link ResponseEntity} containing the {@link ErrorResponse}.
	 */
	@ExceptionHandler({ InternalServerException.class })
	public ResponseEntity<ErrorResponse> handleInternalServerException(@NotNull InternalServerException e) {
		ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST, e.getCause().getLocalizedMessage(), e.getLocalizedMessage());
		return new ResponseEntity<>(error, error.status());
	}
}
