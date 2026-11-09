package com.example.HMS.Manga.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MangaRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 255)
        String slug,

        String description,

        String coverUrl,

        @Size(max = 255)
        String author,

        @Size(max = 255)
        String artist,

        @Size(max = 50)
        String status,

        @Size(max = 50)
        String type,

        @PositiveOrZero
        @Max(32767)
        Short releaseYear
) {
}
