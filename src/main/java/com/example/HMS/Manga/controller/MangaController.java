package com.example.HMS.Manga.controller;

import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;
import com.example.HMS.Manga.service.MangaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/manga")
public class MangaController {

    private final MangaService service;

    public MangaController(MangaService service) {
        this.service = service;
    }

    @GetMapping
    public Page<MangaResponse> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "title") Pageable pageable
    ) {
        return service.list(title, status, pageable);
    }

    @GetMapping("/{id}")
    public MangaResponse getById(@PathVariable String id) {
        return service.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public MangaResponse getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }

    @PostMapping
    public ResponseEntity<MangaResponse> create(@Valid @RequestBody MangaRequest request) {
        MangaResponse created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/manga/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public MangaResponse update(
            @PathVariable String id,
            @Valid @RequestBody MangaRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
