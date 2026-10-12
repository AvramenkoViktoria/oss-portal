package ua.edu.ukma.oss.protocol.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.edu.ukma.oss.shared.NotFoundException;

class LocalFileStorageTest {

    @TempDir
    Path dir;

    @Test
    void storesAndReadsBack() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(dir.toString());

        String key = storage.store(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), "Minutes.PDF");

        assertThat(key).endsWith(".pdf");
        assertThat(new String(storage.open(key).readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void deleteRemovesFile() {
        LocalFileStorage storage = new LocalFileStorage(dir.toString());
        String key = storage.store(new ByteArrayInputStream(new byte[] {1}), "a.docx");

        storage.delete(key);

        assertThatThrownBy(() -> storage.open(key)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void refusesKeysEscapingTheRoot() {
        LocalFileStorage storage = new LocalFileStorage(dir.toString());

        assertThatThrownBy(() -> storage.open("../secret.txt")).isInstanceOf(IllegalArgumentException.class);
    }
}
