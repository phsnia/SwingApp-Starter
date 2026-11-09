package com.example.HMS.Manga.repo;

import com.example.HMS.Manga.entity.Manga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MangaRepository extends JpaRepository<Manga, String> {

    Optional<Manga> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Manga> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Manga> findByStatus(String status, Pageable pageable);

    Page<Manga> findByTitleContainingIgnoreCaseAndStatus(
            String title,
            String status,
            Pageable pageable
    );
}
