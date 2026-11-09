package com.example.HMS.GUI.Screens.Manga;

import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;
import com.example.HMS.Manga.service.MangaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceMangaDataSourceTest {

    @Mock
    private MangaService service;

    @InjectMocks
    private ServiceMangaDataSource dataSource;

    @Test
    void findAllDelegatesToTheService() {
        MangaResponse response = response("1", "One Piece");
        when(service.list(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        List<MangaResponse> result = dataSource.findAll();

        assertThat(result).containsExactly(response);
    }

    @Test
    void createUpdateAndDeleteDelegate() {
        MangaRequest request = request("One Piece");
        MangaResponse response = response("1", "One Piece");
        when(service.create(request)).thenReturn(response);
        when(service.update("1", request)).thenReturn(response);

        assertThat(dataSource.create(request)).isEqualTo(response);
        assertThat(dataSource.update("1", request)).isEqualTo(response);

        dataSource.delete("1");

        verify(service).create(request);
        verify(service).update("1", request);
        verify(service).delete("1");
    }

    private static MangaRequest request(String title) {
        return new MangaRequest(title, null, null, null, null, null, null, null, null);
    }

    private static MangaResponse response(String id, String title) {
        return new MangaResponse(id, title, "one-piece", null, null, null, null,
                "ongoing", "manga", (short) 1997, null, null);
    }
}
