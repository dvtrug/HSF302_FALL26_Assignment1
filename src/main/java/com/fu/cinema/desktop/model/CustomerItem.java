package com.fu.cinema.desktop.model;

public record CustomerItem(Long id, String fullName, String mobile, int age) {
    @Override public String toString() { return fullName + " — " + mobile + " — " + age + " tuổi"; }
}
