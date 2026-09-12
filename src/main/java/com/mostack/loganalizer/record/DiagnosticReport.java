package com.mostack.loganalizer.record;

public record DiagnosticReport(
        String summary,
        String rootCause,
        String suggestedFix,
        String severity
) {}
