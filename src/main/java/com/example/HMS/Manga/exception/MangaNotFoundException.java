package com.example.HMS.Manga.exception;

public class MangaNotFoundException extends RuntimeException {

    public MangaNotFoundException(String message) {
        super(message);
    }

    public static MangaNotFoundException byId(String id) {
        return new MangaNotFoundException("Manga not found: " + id);
    }

    public static MangaNotFoundException bySlug(String slug) {
        return new MangaNotFoundException("Manga not found: " + slug);
    }
}
