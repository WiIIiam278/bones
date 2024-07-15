package net.william278.backend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
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

    @ExceptionHandler(UploadFailed.class)
    @ResponseBody
    public ResponseEntity<?> uploadFailed(final UploadFailed exception) {
        return this.error(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred while uploading a file.");
    }

    @ExceptionHandler(ChannelNotFound.class)
    @ResponseBody
    public ResponseEntity<?> downloadNotFound(final ChannelNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Download not found.");
    }

    @ExceptionHandler(DistributionNotFound.class)
    @ResponseBody
    public ResponseEntity<?> distributionNotFound(final DistributionNotFound exception) {
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

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseBody
    public ResponseEntity<?> endpointNotFound(final NoHandlerFoundException exception) {
        return this.error(HttpStatus.NOT_FOUND, "Endpoint not found.");
    }

    @ExceptionHandler(NotAuthenticated.class)
    @ResponseBody
    public ResponseEntity<?> notAuthenticated(final NotAuthenticated exception) {
        return this.error(HttpStatus.UNAUTHORIZED, "You must be logged in to perform this action.");
    }

    @ExceptionHandler(NoPermission.class)
    @ResponseBody
    public ResponseEntity<?> noPermission(final NoPermission exception) {
        return this.error(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");
    }

    @NotNull
    private ResponseEntity<?> error(final HttpStatus status, final String error) {
        return new ErrorResponse(error).toResponseEntity(status);
    }

}