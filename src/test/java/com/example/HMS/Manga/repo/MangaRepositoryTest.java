package com.example.HMS.Manga.repo;

import com.example.HMS.Manga.entity.Manga;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MangaRepositoryTest {

    @Autowired
    private MangaRepository repository;

    @Test
    void savePersistsIdAndTimestamps() {
        Manga saved = repository.saveAndFlush(manga("One Piece", "one-piece", "ongoing"));

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("ongoing");
        assertThat(saved.getType()).isEqualTo("manga");
    }

    @Test
    void findsBySlug() {
        repository.saveAndFlush(manga("One Piece", "one-piece", "ongoing"));

        assertThat(repository.findBySlug("one-piece"))
                .isPresent()
                .get()
                .extracting(Manga::getTitle)
                .isEqualTo("One Piece");
    }

    @Test
    void findsByTitleAndStatus() {
        repository.saveAndFlush(manga("One Piece", "one-piece", "ongoing"));
        repository.saveAndFlush(manga("Naruto", "naruto", "completed"));

        Page<Manga> page = repository.findByTitleContainingIgnoreCaseAndStatus(
                "piece",
                "ongoing",
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getSlug()).isEqualTo("one-piece");
    }

    private Manga manga(String title, String slug, String status) {
        Manga manga = new Manga();
        Date now = new Date();
        manga.setId(UUID.randomUUID().toString());
        manga.setTitle(title);
        manga.setSlug(slug);
        manga.setStatus(status);
        manga.setType("manga");
        manga.setCreatedAt(now);
        manga.setUpdatedAt(now);
        return manga;
    }
}
