package ua.edu.ukma.oss.petition;

public record SubmitCmd(Long authorId, Long orgId, String title, String text) {
}
