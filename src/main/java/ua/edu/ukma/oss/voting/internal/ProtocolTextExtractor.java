package ua.edu.ukma.oss.voting.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * Plain text of a PDF or DOCX protocol.
 */
final class ProtocolTextExtractor {

    private static final Pattern PARAGRAPH_END = Pattern.compile("</w:p>");
    private static final Pattern BREAK = Pattern.compile("<w:(?:tab|br)\\s*/>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private ProtocolTextExtractor() {
    }

    static String extract(String fileName, byte[] content) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) {
                return pdf(content);
            }
            if (name.endsWith(".docx")) {
                return docx(content);
            }
        } catch (IOException | RuntimeException e) {
            throw new BusinessRuleException("Не вдалося прочитати документ протоколу: файл пошкоджений");
        }
        throw new BusinessRuleException("Приймаються лише файли PDF та DOCX");
    }

    private static String pdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String docx(byte[] content) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals("word/document.xml")) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    xml = PARAGRAPH_END.matcher(xml).replaceAll("\n");
                    xml = BREAK.matcher(xml).replaceAll(" ");
                    return unescape(TAG.matcher(xml).replaceAll(""));
                }
            }
        }
        throw new IOException("word/document.xml is missing");
    }

    private static String unescape(String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }
}
