package ua.edu.ukma.oss.protocol;

import java.io.InputStream;

/**
 * Content of a protocol document together with its original name. The caller closes the stream.
 */
public record ProtocolFile(String fileName, InputStream content) {
}
