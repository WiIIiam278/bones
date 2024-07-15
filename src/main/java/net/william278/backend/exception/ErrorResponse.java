package net.william278.backend.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Schema(description = "A response with an error message.")
public record ErrorResponse(
        @Schema(
                description = "The error message.",
                example = "Error message."
        )
        String error
) {

    @NotNull
    public ResponseEntity<ErrorResponse> toResponseEntity(@NotNull HttpStatus status) {
        return ResponseEntity.status(status).body(this);
    }

}
