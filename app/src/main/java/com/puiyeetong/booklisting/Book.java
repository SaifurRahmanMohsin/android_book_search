package com.puiyeetong.booklisting;

/**
 * {@Book} represents a book. It holds the details
 * of that book such as title, authors and description.
 */
public class Book {

    /** Title of the book */
    public final String title;

    /** Authors of the book */
    public final String authors;

    /** Description of the book */
    public final String description;

    /**
     * Constructs a new {@link Book}.
     *
     * @param title is the title of the book
     * @param authors are the authors of the book
     * @param description is the description of the book
     */
    public Book(String title, String authors, String description) {
        this.title = title;
        this.authors = authors;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getDescription() {
        return description;
    }
}
