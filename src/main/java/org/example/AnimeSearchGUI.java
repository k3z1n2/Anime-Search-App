package org.example;

import com.google.gson.Gson;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnimeSearchGUI extends JFrame {
    private JTextField searchField;
    private JButton searchButton;
    private JTextArea resultsArea;
    private JScrollPane scrollPane;
    private JLabel statusLabel;

    private static final HttpClient client = HttpClient.newHttpClient();

    public AnimeSearchGUI() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setFrameProperties();
    }

    private void initializeComponents() {
        // Search components
        searchField = new JTextField(20);
        searchButton = new JButton("Search");

        // Results area
        resultsArea = new JTextArea(20, 50);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultsArea.setBackground(Color.WHITE);
        scrollPane = new JScrollPane(resultsArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Status label
        statusLabel = new JLabel("Enter an anime name to search");
        statusLabel.setForeground(Color.GRAY);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Top panel for search
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Search Anime:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Center panel for results
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel for status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);

        // Add panels to frame
        add(searchPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        // Search button action
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });

        // Enter key in search field
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an anime name to search.", "No Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Disable search while processing
        searchButton.setEnabled(false);
        statusLabel.setText("Searching...");
        statusLabel.setForeground(Color.BLUE);
        resultsArea.setText("Searching for: " + query + "\nPlease wait...");

        // Perform async search
        searchAnime(query).thenAccept(results -> {
            SwingUtilities.invokeLater(() -> {
                displayResults(results, query);
                searchButton.setEnabled(true);
            });
        }).exceptionally(throwable -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Search failed: " + throwable.getMessage());
                statusLabel.setForeground(Color.RED);
                resultsArea.setText("Error occurred while searching:\n" + throwable.getMessage());
                searchButton.setEnabled(true);
            });
            return null;
        });
    }

    private void displayResults(List<Anime> results, String query) {
        StringBuilder sb = new StringBuilder();

        if (results.isEmpty()) {
            sb.append("No anime found for: ").append(query);
            statusLabel.setText("No results found");
            statusLabel.setForeground(Color.ORANGE);
        } else {
            sb.append("Found ").append(results.size()).append(" anime(s) for: ").append(query).append("\n\n");

            int count = 1;
            for (Anime anime : results) {
                sb.append(count++).append(". ");
                sb.append("Title: ").append(anime.getTitle()).append("\n");

                String synopsis = anime.getSynopsis();
                if (synopsis != null && !synopsis.trim().isEmpty()) {
                    // Word wrap synopsis
                    String wrappedSynopsis = wrapText(synopsis, 80);
                    sb.append("Synopsis: ").append(wrappedSynopsis).append("\n");
                } else {
                    sb.append("Synopsis: No synopsis available\n");
                }

                sb.append("─────────────────────────────────────────────────────────────────────────────────\n\n");
            }

            statusLabel.setText("Found " + results.size() + " result(s)");
            statusLabel.setForeground(Color.GREEN.darker());
        }

        resultsArea.setText(sb.toString());
        resultsArea.setCaretPosition(0); // Scroll to top
    }

    private String wrapText(String text, int lineLength) {
        if (text == null || text.length() <= lineLength) {
            return text;
        }

        StringBuilder wrapped = new StringBuilder();
        String[] words = text.split("\\s+");
        int currentLineLength = 0;

        for (String word : words) {
            if (currentLineLength + word.length() + 1 > lineLength) {
                wrapped.append("\n           "); // Indent continuation lines
                currentLineLength = 11; // Account for indent
            } else if (currentLineLength > 0) {
                wrapped.append(" ");
                currentLineLength++;
            }

            wrapped.append(word);
            currentLineLength += word.length();
        }

        return wrapped.toString();
    }

    public static CompletableFuture<List<Anime>> searchAnime(String query) {
        Gson gson = new Gson();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.jikan.moe/v4/anime?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=5&fields=title,synopsis"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(json -> gson.fromJson(json, ApiResponse.class))
                .thenApply(ApiResponse::getData);
    }

    private void setFrameProperties() {
        setTitle("Anime Search Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null); // Center on screen
        setMinimumSize(new Dimension(600, 400));

        // Set application icon (you can add your own icon)
        try {
            // If you have an icon file, you can load it here
            // setIconImage(ImageIO.read(new File("anime_icon.png")));
        } catch (Exception e) {
            // Ignore if no icon
        }
    }

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and show GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AnimeSearchGUI().setVisible(true);
            }
        });
    }
}