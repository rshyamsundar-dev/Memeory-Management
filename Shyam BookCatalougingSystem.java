Class Design

// Book.java
public class Book {
    private String title;
    private String author;
    private String genre;
    private int publicationYear;

    public Book(String title, String author, String genre, int publicationYear) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publicationYear = publicationYear;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public int getPublicationYear() { return publicationYear; }

    @Override
    public String toString() {
        return title + " | " + author + " | " + genre + " | " + publicationYear;
    }
}

Catalog Management Using Collections and Streams

import java.util.*;
import java.util.stream.Collectors;

public class BookCatalog {
    private List<Book> books;

    public BookCatalog() {
        books = new ArrayList<>();
    }

    public void addBook(Book book) {
        books.add(book);
    }

    public void removeBook(String title) {
        books.removeIf(book -> book.getTitle().equalsIgnoreCase(title));
    }

    public List<Book> searchByTitle(String title) {
        return books.stream()
                .filter(book -> book.getTitle().equalsIgnoreCase(title))
                .collect(Collectors.toList());
    }

    public List<Book> searchByAuthor(String author) {
        return books.stream()
                .filter(book -> book.getAuthor().equalsIgnoreCase(author))
                .collect(Collectors.toList());
    }

    public List<Book> searchByGenre(String genre) {
        return books.stream()
                .filter(book -> book.getGenre().equalsIgnoreCase(genre))
                .collect(Collectors.toList());
    }

    public void reportByAuthor(String author) {
        books.stream()
                .filter(book -> book.getAuthor().equalsIgnoreCase(author))
                .forEach(System.out::println);
    }

    public void reportByGenre(String genre) {
        books.stream()
                .filter(book -> book.getGenre().equalsIgnoreCase(genre))
                .forEach(System.out::println);
    }
}


GUI Implementation Using Swing

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BookCatalogGUI {
    private BookCatalog catalog;
    private JFrame frame;

    public BookCatalogGUI() {
        catalog = new BookCatalog();
        frame = new JFrame("Book Cataloging System");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 2));

        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField genreField = new JTextField();
        JTextField yearField = new JTextField();

        JButton addButton = new JButton("Add Book");
        JButton searchButton = new JButton("Search by Title");

        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Author:"));
        panel.add(authorField);
        panel.add(new JLabel("Genre:"));
        panel.add(genreField);
        panel.add(new JLabel("Publication Year:"));
        panel.add(yearField);
        panel.add(addButton);
        panel.add(searchButton);

        JTextArea outputArea = new JTextArea();
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // Button Actions
        addButton.addActionListener(e -> {
            String title = titleField.getText();
            String author = authorField.getText();
            String genre = genreField.getText();
            int year = Integer.parseInt(yearField.getText());
            catalog.addBook(new Book(title, author, genre, year));
            outputArea.setText("Book added successfully!\n");
        });

        searchButton.addActionListener(e -> {
            String title = titleField.getText();
            var results = catalog.searchByTitle(title);
            outputArea.setText("Search Results:\n");
            results.forEach(book -> outputArea.append(book + "\n"));
        });

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BookCatalogGUI::new);
    }
}
