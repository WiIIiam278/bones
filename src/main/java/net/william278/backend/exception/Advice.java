package net.william278.backend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
class Advice {
    private final ObjectMapper json;

    @Autowired
    private Advice(final ObjectMapper json) {
        this.json = json;
    }

    @ExceptionHandler(DownloadFailed.class)
    @ResponseBody
    public ResponseEntity<?> downloadFailed(final DownloadFailed exception) {
        return this.error(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred while serving your download.");
    }

    @ExceptionHandler(ChannelNotFound.class)
    @ResponseBody
    public ResponseEntity<?> downloadNotFound(final ChannelNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Release channel not found.");
    }

    @ExceptionHandler(DistributionNotFound.class)
    @ResponseBody
    public ResponseEntity<?> buildNotFound(final DistributionNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Distribution not found.");
    }

    @ExceptionHandler(ProjectNotFound.class)
    @ResponseBody
    public ResponseEntity<?> projectNotFound(final ProjectNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Project not found.");
    }

    @ExceptionHandler(VersionNotFound.class)
    @ResponseBody
    public ResponseEntity<?> versionNotFound(final VersionNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Version not found.");
    }

    @ExceptionHandler(UserNotFound.class)
    @ResponseBody
    public ResponseEntity<?> userNotFound(final UserNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "User not found.");
    }

    @ExceptionHandler(NotAuthenticated.class)
    @ResponseBody
    public ResponseEntity<?> notAuthenticated(final NotAuthenticated exception) {
        return this.error(HttpStatus.UNAUTHORIZED, "Not logged in.");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseBody
    public ResponseEntity<?> endpointNotFound(final NoHandlerFoundException exception) {
        return this.error(HttpStatus.NOT_FOUND, "Endpoint not found.");
    }

    private ResponseEntity<?> error(final HttpStatus status, final String error) {
        return new ResponseEntity<>(
                this.json.createObjectNode().put("error", error), status
        );
    }
}