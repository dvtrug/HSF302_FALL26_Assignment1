package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.AgeRating;

/** Counter-specific choice avoids owning the shared scheduling model of member D. */
public record CounterMovieItem(Long id, String title, int duration, AgeRating ageRating) {
    @Override public String toString() { return title; }
}
