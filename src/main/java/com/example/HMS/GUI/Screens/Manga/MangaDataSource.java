package com.example.HMS.GUI.Screens.Manga;

import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;

import java.util.List;

/**
 * Data access seam for the Manga screen. Keeps the UI independent of Spring
 * and makes headless tests possible.
 */
public interface MangaDataSource {

    List<MangaResponse> findAll();

    MangaResponse create(MangaRequest request);

    MangaResponse update(String id, MangaRequest request);

    void delete(String id);
}
