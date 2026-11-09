package com.example.HMS.Manga.service;

import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;
import com.example.HMS.Manga.entity.Manga;
import com.example.HMS.Manga.exception.DuplicateSlugException;
import com.example.HMS.Manga.exception.MangaNotFoundException;
import com.example.HMS.Manga.repo.MangaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MangaServiceTest {

    @Mock
    private MangaRepository repository;

    @InjectMocks
    private MangaService service;

    @Test
    void createGeneratesSlugFromTitle() {
        when(repository.existsBySlug("one-piece")).thenReturn(false);
        when(repository.save(any(Manga.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MangaResponse response = service.create(request("One Piece", null));

        assertThat(response.title()).isEqualTo("One Piece");
        assertThat(response.slug()).isEqualTo("one-piece");
        assertThat(response.status()).isEqualTo("ongoing");
        assertThat(response.type()).isEqualTo("manga");
    }

    @Test
    void createRejectsDuplicateSlug() {
        when(repository.existsBySlug("one-piece")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("One Piece", null)))
                .isInstanceOf(DuplicateSlugException.class)
                .hasMessageContaining("one-piece");

        verify(repository, never()).save(any(Manga.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(MangaNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void updateChangesFieldsAndSlug() {
        Manga existing = manga("id-1", "Old Title", "old-title");
        when(repository.findById("id-1")).thenReturn(Optional.of(existing));
        when(repository.existsBySlug("new-title")).thenReturn(false);
        when(repository.save(any(Manga.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MangaResponse response = service.update("id-1", request("New Title", null));

        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.slug()).isEqualTo("new-title");
    }

    @Test
    void deleteRemovesExistingManga() {
        Manga existing = manga("id-1", "One Piece", "one-piece");
        when(repository.findById("id-1")).thenReturn(Optional.of(existing));

        service.delete("id-1");

        verify(repository).delete(existing);
    }

    private MangaRequest request(String title, String slug) {
        return new MangaRequest(
                title,
                slug,
                "description",
                "https://example.com/cover.jpg",
                "Author",
                "Artist",
                null,
                null,
                (short) 1997
        );
    }

    private Manga manga(String id, String title, String slug) {
        Manga manga = new Manga();
        manga.setId(id);
        manga.setTitle(title);
        manga.setSlug(slug);
        return manga;
    }
}
