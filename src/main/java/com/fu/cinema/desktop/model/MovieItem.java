package com.fu.cinema.desktop.model;

import com.fu.cinema.enums.AgeRating;

/**
 * Immutable record representing a movie item in dropdowns and selection controls.
 *
 * @param id the unique ID of the movie
 * @param title the movie title
 * @param duration the duration in minutes
 * @param ageRating the movie age rating classification
 */
public record MovieItem(Long id, String title, int duration, AgeRating ageRating) {

    @Override
    public String toString() {
        return title + " (" + duration + "')";
    }
}
