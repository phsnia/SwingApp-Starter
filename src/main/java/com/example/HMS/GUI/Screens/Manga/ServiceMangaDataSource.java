package com.example.HMS.GUI.Screens.Manga;

import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;
import com.example.HMS.Manga.service.MangaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Runs the desktop app against the Spring service in the same JVM.
 */
public final class ServiceMangaDataSource implements MangaDataSource {
    private static final int LOAD_LIMIT = 1000;

    private final MangaService service;

    public ServiceMangaDataSource(MangaService service) {
        this.service = service;
    }

    @Override
    public List<MangaResponse> findAll() {
        return service.list(null, null,
                PageRequest.of(0, LOAD_LIMIT, Sort.by("title").ascending())).getContent();
    }

    @Override
    public MangaResponse create(MangaRequest request) {
        return service.create(request);
    }

    @Override
    public MangaResponse update(String id, MangaRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(String id) {
        service.delete(id);
    }
}
