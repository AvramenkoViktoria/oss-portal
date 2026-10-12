package ua.edu.ukma.oss.organization;

/**
 * Stable identifier of a student body. The two bodies are tenants of one application, so they are
 * data rows keyed by this code rather than separate classes.
 */
public enum OrgCode {
    /** Student Board (Студентська колегія). */
    SK,
    /** Student Congregation (Конгрегація студентів). */
    KS
}
