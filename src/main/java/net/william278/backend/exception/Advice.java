/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
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

    @ExceptionHandler(DownloadNotFound.class)
    @ResponseBody
    public ResponseEntity<?> downloadNotFound(final ChannelNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Download not found.");
    }

    @ExceptionHandler(ChannelNotFound.class)
    @ResponseBody
    public ResponseEntity<?> channelNotFound(final ChannelNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Channel not found.");
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

    @ExceptionHandler(DocsPageNotFound.class)
    @ResponseBody
    public ResponseEntity<?> docsPageNotFound(final DocsPageNotFound exception) {
        return this.error(HttpStatus.NOT_FOUND, "Docs page not found.");
    }

    @ExceptionHandler(FailedToUpdateDocs.class)
    @ResponseBody
    public ResponseEntity<?> failedToUpdateDocs(final FailedToUpdateDocs exception) {
        return this.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update docs.");
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

    @ExceptionHandler(InvalidRole.class)
    @ResponseBody
    public ResponseEntity<?> invalidRole(final InvalidRole exception) {
        return this.error(HttpStatus.BAD_REQUEST, "Invalid role.");
    }

    @ExceptionHandler(UndocumentedProject.class)
    @ResponseBody
    public ResponseEntity<?> undocumentedProject(final UndocumentedProject exception) {
        return this.error(HttpStatus.BAD_REQUEST, "Project does not have documentation.");
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