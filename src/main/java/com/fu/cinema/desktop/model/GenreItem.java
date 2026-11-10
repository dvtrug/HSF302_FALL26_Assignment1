package com.fu.cinema.desktop.model;

import com.fu.cinema.entity.Genre;

public record GenreItem(Long genreId, String genreName) {
    public static GenreItem from(Genre g) {
        return new GenreItem(g.getGenreId(), g.getGenreName());
    }

    @Override
    public String toString() {
        return genreName;
    }
}