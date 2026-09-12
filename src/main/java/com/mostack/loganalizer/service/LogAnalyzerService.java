package com.mostack.loganalizer.service;

import com.mostack.loganalizer.record.DiagnosticReport;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class LogAnalyzerService {

    private final ChatClient chatClient;

    public LogAnalyzerService(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    public CompletableFuture<DiagnosticReport> listenAndAnalyze(String logPath) {
        if (!StringUtils.hasText(logPath)) {
            throw new IllegalArgumentException("logPath must not be blank");
        }

        File logFile = resolveOrCreate(logPath);

        CompletableFuture<DiagnosticReport> futureReport = new CompletableFuture<>();

        TailerListenerAdapter listener = buildListener(futureReport);

        Tailer tailer = Tailer.builder()
                .setFile(logFile)
                .setTailerListener(listener)
                .setDelayDuration(Duration.ofMillis(200))
                .setTailFromEnd(false)
                .get();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(tailer);

        futureReport.orTimeout(3, TimeUnit.MINUTES);

        return futureReport.whenComplete((result, ex) -> {
            tailer.close();
            executor.shutdownNow();
        });
    }

    private File resolveOrCreate(String logPath) {
        File logFile = new File(logPath);
        try {
            logFile = logFile.getCanonicalFile();
        } catch (IOException e) {
            logFile = logFile.getAbsoluteFile();
        }

        if (!logFile.exists()) {
            try {
                File parent = logFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Could not create parent directories: " + parent);
                }
                if (!logFile.createNewFile()) {
                    throw new IllegalStateException("Could not create log file: " + logFile.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create missing log file: " + logFile.getAbsolutePath(), e);
            }
        }
        return logFile;
    }

    private TailerListenerAdapter buildListener(CompletableFuture<DiagnosticReport> futureReport) {
        return new TailerListenerAdapter() {
            private final StringBuilder errorBuffer = new StringBuilder();
            private boolean capturing = false;

            @Override
            public void handle(String line) {
                if (futureReport.isDone()) return;

                if (line.contains("ERROR") || line.contains("Exception")) {
                    capturing = true;
                    errorBuffer.setLength(0);
                }

                if (capturing) {
                    errorBuffer.append(line).append("\n");

                    if (line.trim().isEmpty() || line.contains("\tat ") || line.contains("At ")) {
                        String fullStackTrace = errorBuffer.toString();
                        CompletableFuture.runAsync(() -> {
                            try {
                                futureReport.complete(callOllama(fullStackTrace));
                            } catch (Exception e) {
                                futureReport.completeExceptionally(e);
                            }
                        });
                        capturing = false;
                    }
                }
            }
        };
    }

    private DiagnosticReport callOllama(String stackTrace) {
        String prompt = """
            Analyze the following Java stack trace and return a concise diagnostic report.

            Stack Trace:
            %s
            """.formatted(stackTrace);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(DiagnosticReport.class);
    }
}