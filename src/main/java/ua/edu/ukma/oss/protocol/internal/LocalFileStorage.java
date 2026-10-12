package ua.edu.ukma.oss.protocol.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.protocol.FileStorage;
import ua.edu.ukma.oss.shared.NotFoundException;

/**
 * Filesystem implementation of {@link FileStorage}. It stands in for an S3-compatible bucket;
 * swapping it only requires another bean implementing the same interface.
 */
@Component
class LocalFileStorage implements FileStorage {

    private final Path root;

    LocalFileStorage(@Value("${oss.storage.path:storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String store(InputStream content, String originalName) {
        String key = UUID.randomUUID() + extensionOf(originalName);
        try {
            Files.createDirectories(root);
            Files.copy(content, root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store file", e);
        }
        return key;
    }

    @Override
    public InputStream open(String key) {
        Path file = resolve(key);
        try {
            if (!Files.exists(file)) {
                throw new NotFoundException("File", key);
            }
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete file", e);
        }
    }

    /**
     * Resolves a key inside the root and refuses anything that would escape it.
     */
    private Path resolve(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return file;
    }

    private static String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^.a-z0-9]", "");
    }
}
