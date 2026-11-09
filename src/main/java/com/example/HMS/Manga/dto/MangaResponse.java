package com.example.HMS.Manga.dto;

import com.example.HMS.Manga.entity.Manga;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public record MangaResponse(
        String id,
        String title,
        String slug,
        String description,
        String coverUrl,
        String author,
        String artist,
        String status,
        String type,
        Short releaseYear,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MangaResponse from(Manga manga) {
        return new MangaResponse(
                manga.getId(),
                manga.getTitle(),
                manga.getSlug(),
                manga.getDescription(),
                manga.getCoverUrl(),
                manga.getAuthor(),
                manga.getArtist(),
                manga.getStatus(),
                manga.getType(),
                manga.getReleaseYear(),
                toLocalDateTime(manga.getCreatedAt()),
                toLocalDateTime(manga.getUpdatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null
                ? null
                : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
