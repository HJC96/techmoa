package com.techmoa.admin.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSourceRequest(
        @NotBlank String name,
        @NotBlank String baseUrl,
        String feedUrl,
        @NotBlank String parserType,
        @NotNull @Min(1) @Max(1440) Integer intervalMin,
        @NotNull Boolean active
) {
}
