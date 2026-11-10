package com.fu.cinema.desktop.facade;

import com.fu.cinema.desktop.model.GenreItem;
import com.fu.cinema.desktop.model.MovieRow;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.service.GenreService;
import com.fu.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MovieFacade {

    private final MovieService movieService;
    private final GenreService genreService;
    private final DesktopSession session;

    public PageResult<MovieRow> search(String keyword, Long genreId, MovieStatus status, int page, int size) {
        var springPage = movieService.searchForAdmin(keyword, genreId, status, page, size, "releaseDate");
        return PageResult.of(springPage, MovieRow::from);
    }

    public List<GenreItem> findAllGenres() {
        return genreService.findAll().stream()
                .map(GenreItem::from)
                .toList();
    }

    public MovieForm getForm(Long movieId) {
        return movieService.getForm(movieId);
    }
}