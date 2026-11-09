package com.example.HMS.Manga.service;

import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;
import com.example.HMS.Manga.entity.Manga;
import com.example.HMS.Manga.exception.DuplicateSlugException;
import com.example.HMS.Manga.exception.MangaNotFoundException;
import com.example.HMS.Manga.repo.MangaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MangaService {

    private final MangaRepository repository;

    public MangaService(MangaRepository repository) {
        this.repository = repository;
    }

    public Page<MangaResponse> list(String title, String status, Pageable pageable) {
        String titleFilter = normalizeFilter(title);
        String statusFilter = normalizeFilter(status);

        Page<Manga> page;
        if (titleFilter != null && statusFilter != null) {
            page = repository.findByTitleContainingIgnoreCaseAndStatus(
                    titleFilter,
                    statusFilter,
                    pageable
            );
        } else if (titleFilter != null) {
            page = repository.findByTitleContainingIgnoreCase(titleFilter, pageable);
        } else if (statusFilter != null) {
            page = repository.findByStatus(statusFilter, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(MangaResponse::from);
    }

    public MangaResponse getById(String id) {
        return MangaResponse.from(findById(id));
    }

    public MangaResponse getBySlug(String slug) {
        return repository.findBySlug(slug)
                .map(MangaResponse::from)
                .orElseThrow(() -> MangaNotFoundException.bySlug(slug));
    }

    @Transactional
    public MangaResponse create(MangaRequest request) {
        String slug = resolveSlug(request.slug(), request.title());
        if (repository.existsBySlug(slug)) {
            throw new DuplicateSlugException(slug);
        }

        Manga manga = new Manga();
        applyRequest(manga, request, true);

        // The entity is generated from the table, so application rules live here:
        // the DB has no default for `id`, and timestamps are NOT NULL.
        Date now = new Date();
        manga.setId(UUID.randomUUID().toString());
        manga.setCreatedAt(now);
        manga.setUpdatedAt(now);
        manga.setSlug(slug);
        return MangaResponse.from(repository.save(manga));
    }

    @Transactional
    public MangaResponse update(String id, MangaRequest request) {
        Manga manga = findById(id);
        String slug = resolveSlug(request.slug(), request.title());

        if (!slug.equals(manga.getSlug()) && repository.existsBySlug(slug)) {
            throw new DuplicateSlugException(slug);
        }

        applyRequest(manga, request, false);
        manga.setUpdatedAt(new Date());
        manga.setSlug(slug);
        return MangaResponse.from(repository.save(manga));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(findById(id));
    }

    private Manga findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> MangaNotFoundException.byId(id));
    }

    private void applyRequest(Manga manga, MangaRequest request, boolean creating) {
        manga.setTitle(request.title().trim());
        manga.setDescription(request.description());
        manga.setCoverUrl(request.coverUrl());
        manga.setAuthor(request.author());
        manga.setArtist(request.artist());
        manga.setReleaseYear(request.releaseYear());

        if (request.status() != null && !request.status().isBlank()) {
            manga.setStatus(request.status().trim());
        } else if (creating) {
            manga.setStatus("ongoing");
        }

        if (request.type() != null && !request.type().isBlank()) {
            manga.setType(request.type().trim());
        } else if (creating) {
            manga.setType("manga");
        }
    }

    private String resolveSlug(String requestedSlug, String title) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            return requestedSlug.trim().toLowerCase(Locale.ROOT);
        }

        String slug = slugify(title);
        return slug.isBlank() ? UUID.randomUUID().toString() : slug;
    }

    private String slugify(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
