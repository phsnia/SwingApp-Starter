package com.example.HMS.Manga.controller;

import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;
import com.example.HMS.Manga.exception.DuplicateSlugException;
import com.example.HMS.Manga.exception.GlobalExceptionHandler;
import com.example.HMS.Manga.exception.MangaNotFoundException;
import com.example.HMS.Manga.service.MangaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MangaController.class)
@Import(GlobalExceptionHandler.class)
class MangaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MangaService service;

    @Test
    void listReturnsPagedManga() throws Exception {
        MangaResponse manga = response();
        when(service.list(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(manga)));

        mockMvc.perform(get("/api/manga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("id-1"))
                .andExpect(jsonPath("$.content[0].title").value("One Piece"));
    }

    @Test
    void getByIdReturnsManga() throws Exception {
        when(service.getById("id-1")).thenReturn(response());

        mockMvc.perform(get("/api/manga/id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("one-piece"));
    }

    @Test
    void getBySlugReturnsManga() throws Exception {
        when(service.getBySlug("one-piece")).thenReturn(response());

        mockMvc.perform(get("/api/manga/slug/one-piece"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void createReturnsCreatedWithLocation() throws Exception {
        when(service.create(any(MangaRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/manga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/manga/id-1"))
                .andExpect(jsonPath("$.title").value("One Piece"));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/manga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void updateReturnsUpdatedManga() throws Exception {
        when(service.update(eq("id-1"), any(MangaRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/manga/id-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(service).delete("id-1");

        mockMvc.perform(delete("/api/manga/id-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingMangaReturnsNotFound() throws Exception {
        when(service.getById("missing"))
                .thenThrow(MangaNotFoundException.byId("missing"));

        mockMvc.perform(get("/api/manga/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Manga not found: missing"));
    }

    @Test
    void duplicateSlugReturnsConflict() throws Exception {
        when(service.create(any(MangaRequest.class)))
                .thenThrow(new DuplicateSlugException("one-piece"));

        mockMvc.perform(post("/api/manga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Manga slug already exists: one-piece"));
    }

    private MangaResponse response() {
        return new MangaResponse(
                "id-1",
                "One Piece",
                "one-piece",
                "description",
                "https://example.com/cover.jpg",
                "Author",
                "Artist",
                "ongoing",
                "manga",
                (short) 1997,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String validRequest() {
        return """
                {
                  "title": "One Piece",
                  "description": "A pirate adventure",
                  "author": "Eiichiro Oda",
                  "artist": "Eiichiro Oda",
                  "status": "ongoing",
                  "type": "manga",
                  "releaseYear": 1997
                }
                """;
    }
}
