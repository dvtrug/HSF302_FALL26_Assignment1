package com.fu.cinema.service;

import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.entity.Genre;
import com.fu.cinema.entity.Movie;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.exception.ResourceNotFoundException;
import com.fu.cinema.repository.GenreRepository;
import com.fu.cinema.repository.MovieRepository;
import com.fu.cinema.repository.ShowtimeRepository;
import com.fu.cinema.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieServiceUnitTest {

    private final MovieRepository movies = mock(MovieRepository.class);
    private final GenreRepository genres = mock(GenreRepository.class);
    private final ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    private final MovieService service = new MovieServiceImpl(movies, genres, showtimes);

    @Test
    void adminSearchUsesAscendingTitleAndRequestedStatus() {
        var paging = PageRequest.of(0, 10, Sort.by("title").ascending());
        var statuses = EnumSet.of(MovieStatus.SHOWING);
        var result = Page.<Movie>empty(paging);
        when(movies.search("Mùa", 2L, statuses, paging)).thenReturn(result);

        assertSame(result, service.searchForAdmin(" Mùa ", 2L, MovieStatus.SHOWING, -1, 10, "title"));
    }

    @Test
    void customerSearchExcludesStoppedMovies() {
        var paging = PageRequest.of(1, 10, Sort.by("releaseDate").descending());
        var statuses = EnumSet.of(MovieStatus.SHOWING, MovieStatus.COMING_SOON);
        var result = Page.<Movie>empty(paging);
        when(movies.search(null, null, statuses, paging)).thenReturn(result);

        assertSame(result, service.searchForCustomer(" ", null, 1, 10));
    }

    @Test
    void createsMovieWithTrimmedFieldsAndMapsItBackToForm() {
        var form = sampleForm();
        var genre = new Genre("Hoạt hình", "Mô tả");
        genre.setGenreId(2L);
        when(genres.findById(2L)).thenReturn(Optional.of(genre));

        service.create(form);
        var saved = ArgumentCaptor.forClass(Movie.class);
        verify(movies).save(saved.capture());
        var movie = saved.getValue();
        movie.setMovieId(3L);
        when(movies.findWithGenreById(3L)).thenReturn(Optional.of(movie));

        var mapped = service.getForm(3L);
        assertEquals(3L, mapped.getMovieId());
        assertEquals("Mùa hè", mapped.getTitle());
        assertEquals("An", mapped.getDirector());
        assertEquals(form.getDuration(), mapped.getDuration());
        assertEquals(form.getReleaseDate(), mapped.getReleaseDate());
        assertEquals(form.getAgeRating(), mapped.getAgeRating());
        assertEquals("Tóm tắt", mapped.getDescription());
        assertEquals("https://example.test/poster", mapped.getPosterUrl());
        assertEquals(form.getGenreId(), mapped.getGenreId());
        assertEquals(form.getStatus(), mapped.getStatus());
    }

    @Test
    void updateRejectsAnUnknownGenreWithoutChangingTheMovie() {
        var movie = new Movie();
        movie.setTitle("Tên cũ");
        when(movies.findById(3L)).thenReturn(Optional.of(movie));

        assertThrows(ResourceNotFoundException.class, () -> service.update(3L, sampleForm()));
        assertEquals("Tên cũ", movie.getTitle());
        verify(movies, never()).save(any());
    }

    @Test
    void stopsMovieWithShowtimesAndDeletesMovieWithoutShowtimes() {
        var scheduled = new Movie();
        var unscheduled = new Movie();
        when(movies.findById(1L)).thenReturn(Optional.of(scheduled));
        when(movies.findById(2L)).thenReturn(Optional.of(unscheduled));
        when(showtimes.existsByMovie_MovieId(1L)).thenReturn(true);

        assertFalse(service.delete(1L));
        assertEquals(MovieStatus.STOPPED, scheduled.getStatus());
        verify(movies, never()).delete(scheduled);
        assertTrue(service.delete(2L));
        verify(movies).delete(unscheduled);
    }

    @Test
    void exposesOnlySchedulableMoviesAndCountsShowingMovies() {
        var available = List.of(new Movie());
        when(movies.findByStatusNotOrderByTitleAsc(MovieStatus.STOPPED)).thenReturn(available);
        when(movies.countByStatus(MovieStatus.SHOWING)).thenReturn(2L);

        assertEquals(available, service.findSchedulable());
        assertEquals(2, service.countShowing());
    }

    private MovieForm sampleForm() {
        var form = new MovieForm();
        form.setTitle(" Mùa hè ");
        form.setDirector(" An ");
        form.setDescription(" Tóm tắt ");
        form.setPosterUrl(" https://example.test/poster ");
        form.setDuration(95);
        form.setReleaseDate(LocalDate.of(2026, 11, 9));
        form.setAgeRating(AgeRating.P);
        form.setGenreId(2L);
        form.setStatus(MovieStatus.SHOWING);
        return form;
    }
}
