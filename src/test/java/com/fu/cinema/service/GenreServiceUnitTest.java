package com.fu.cinema.service;

import com.fu.cinema.dto.GenreForm;
import com.fu.cinema.entity.Genre;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.GenreRepository;
import com.fu.cinema.repository.MovieRepository;
import com.fu.cinema.service.impl.GenreServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenreServiceUnitTest {

    private final GenreRepository genres = mock(GenreRepository.class);
    private final MovieRepository movies = mock(MovieRepository.class);
    private final GenreService service = new GenreServiceImpl(genres, movies);

    @Test
    void rejectsDuplicateNamesAfterTrimming() {
        when(genres.existsByGenreNameIgnoreCase("Hành động")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(form()));
        verify(genres, never()).save(any());
    }

    @Test
    void createsGenreWithTrimmedNameAndDescription() {
        service.create(form());

        verify(genres).save(argThat(genre ->
                "Hành động".equals(genre.getGenreName()) && "Mô tả".equals(genre.getDescription())));
    }

    @Test
    void editsGenreAndMapsUpdatedValuesToTheForm() {
        var existing = new Genre("Cũ", "Cũ");
        existing.setGenreId(1L);
        when(genres.findById(1L)).thenReturn(Optional.of(existing));

        service.update(1L, form());

        verify(genres).existsByGenreNameIgnoreCaseAndGenreIdNot("Hành động", 1L);
        var mapped = service.getForm(1L);
        assertEquals(1L, mapped.getGenreId());
        assertEquals("Hành động", mapped.getGenreName());
        assertEquals("Mô tả", mapped.getDescription());
    }

    @Test
    void refusesToDeleteAGenreWithMovies() {
        var existing = new Genre("Hành động", "Mô tả");
        when(genres.findById(1L)).thenReturn(Optional.of(existing));
        when(movies.existsByGenre_GenreId(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.delete(1L));
        verify(genres, never()).delete(existing);
    }

    @Test
    void deletesAnUnusedGenreAndReportsMissingIds() {
        var existing = new Genre("Hành động", "Mô tả");
        when(genres.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(genres).delete(existing);
        assertThrows(ResourceNotFoundException.class, () -> service.getForm(2L));
    }

    private GenreForm form() {
        var form = new GenreForm();
        form.setGenreName(" Hành động ");
        form.setDescription(" Mô tả ");
        return form;
    }
}
