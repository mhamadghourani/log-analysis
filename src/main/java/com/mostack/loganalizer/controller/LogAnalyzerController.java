package com.mostack.loganalizer.controller;

import com.mostack.loganalizer.dto.LogListenRequest;
import com.mostack.loganalizer.record.DiagnosticReport;
import com.mostack.loganalizer.service.LogAnalyzerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/logs")
public class LogAnalyzerController {

    private final LogAnalyzerService logAnalyzerService;

    public LogAnalyzerController(LogAnalyzerService logAnalyzerService) {
        this.logAnalyzerService = logAnalyzerService;
    }

    @PostMapping("/listen")
    public CompletableFuture<ResponseEntity<DiagnosticReport>> listenForError(
            @Valid @RequestBody LogListenRequest request) {

        return logAnalyzerService.listenAndAnalyze(request.logPath())
                .thenApply(ResponseEntity::ok);
    }
}


