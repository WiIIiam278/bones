package net.william278.backend.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MediaTypeUtils {

    public static final String APPLICATION_ZIP_VALUE = "application/zip";
    public static final MediaType APPLICATION_ZIP = MediaType.parseMediaType(APPLICATION_ZIP_VALUE);

    @Nullable
    public static MediaType fromFileName(final String name) {
        final int index = name.lastIndexOf('.');
        if (index != -1) {
            return fromFileExtension(name.substring(index + 1));
        }
        return null;
    }

    @NotNull
    public static MediaType fromFileExtension(final String extension) {
        return switch (extension) {
            case "mcpack" -> APPLICATION_ZIP;
            default -> MediaTypeFactory.getMediaType("." + extension).orElse(MediaType.APPLICATION_OCTET_STREAM);
        };
    }

}
