package ua.edu.ukma.oss.protocol;

import java.io.InputStream;

/**
 * Binary storage for protocol documents; metadata lives in the database, content here.
 */
public interface FileStorage {

    /**
     * Stores the content and returns an opaque key.
     */
    String store(InputStream content, String originalName);

    InputStream open(String key);

    void delete(String key);
}
