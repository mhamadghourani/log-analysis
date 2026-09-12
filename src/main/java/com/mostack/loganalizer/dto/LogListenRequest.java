package com.mostack.loganalizer.dto;

import jakarta.validation.constraints.NotBlank;

public record LogListenRequest(
        @NotBlank(message = "logPath must not be blank")
        String logPath
) {}
