package pl.mielecmichal.filewatcher;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
@Wither
public class FilesystemConstraints implements Predicate<FilesystemEvent> {

    @RequiredArgsConstructor
    public enum FileType {
        REGULAR(BasicFileAttributes::isRegularFile),
        DIRECTORY(BasicFileAttributes::isDirectory),
        LINK(BasicFileAttributes::isSymbolicLink),
        OTHER(BasicFileAttributes::isOther);

        private final Predicate<BasicFileAttributes> predicate;
    }

    private final List<String> filenameSubstrings = List.of();
    private final List<Pattern> filenamePatterns = List.of();
    private final List<FileType> fileTypes = List.of();

    @Override
    public boolean test(FilesystemEvent event) {
        Path path = event.getPath();
        BasicFileAttributes fileAttributes = readAttributes(path);
        if (!fileTypes.isEmpty()) {
            if (fileTypes.stream().noneMatch(fileType -> fileType.predicate.test(fileAttributes))) {
                return false;
            }
        }

        String filename = path.getFileName().toString();
        if (!filenameSubstrings.isEmpty()) {
            if (filenameSubstrings.stream().noneMatch(filename::contains)) {
                return false;
            }
        }

        if (!filenamePatterns.isEmpty()) {
            if (filenamePatterns.stream().noneMatch(pattern -> pattern.matcher(filename).matches())) {
                return false;
            }
        }

        return true;

    }

    private BasicFileAttributes readAttributes(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
