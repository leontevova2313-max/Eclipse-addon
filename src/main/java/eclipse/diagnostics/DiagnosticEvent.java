package eclipse.diagnostics;

public record DiagnosticEvent(
    long elapsedMs,
    DiagnosticCategory category,
    String type,
    String detail,
    String moduleContext
) {
}
