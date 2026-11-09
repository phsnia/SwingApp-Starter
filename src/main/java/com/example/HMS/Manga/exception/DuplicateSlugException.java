package com.example.HMS.Manga.exception;

public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException(String slug) {
        super("Manga slug already exists: " + slug);
    }
}
