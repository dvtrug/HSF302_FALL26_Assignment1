package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Genre;

/**
 * Record đại diện cho một dòng hiển thị trên bảng thể loại Desktop.
 */
public record GenreRow(
        Long genreId,
        String genreName,
        String description
) {
    public static GenreRow from(Genre g) {
        return new GenreRow(
                g.getGenreId(),
                g.getGenreName(),
                g.getDescription()
        );
    }
}