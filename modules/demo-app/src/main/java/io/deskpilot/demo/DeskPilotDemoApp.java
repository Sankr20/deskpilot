package io.deskpilot.demo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DeskPilotDemoApp {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DeskPilotDemoApp::start);
    }

    private static void start() {
        JFrame frame = new JFrame("DeskPilot Demo App");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Root
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top bar: input + button
        JPanel top = new JPanel(new BorderLayout(8, 8));

        JTextField input = new JTextField();
        input.setName("SEARCH_INPUT");              // Optional, for human clarity
        input.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton searchBtn = new JButton("Search");
        searchBtn.setName("SEARCH_BUTTON");
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchBtn.setFocusable(false);

        top.add(input, BorderLayout.CENTER);
        top.add(searchBtn, BorderLayout.EAST);

        // Status
        JLabel status = new JLabel("Status: idle");
        status.setName("STATUS_LABEL");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Results panel (we want this to visibly change for pixel verification)
        JTextArea results = new JTextArea(12, 60);
        results.setName("RESULTS_PANEL");
        results.setFont(new Font("Consolas", Font.PLAIN, 13));
        results.setEditable(false);
        results.setLineWrap(true);
        results.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(results);

        // Bottom info
        JLabel hint = new JLabel("Tip: Type something and click Search. Results will update + color will flip.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(new Color(80, 80, 80));

        // Behavior: deterministic, visible changes
        Runnable doSearch = () -> {
            String q = input.getText().trim();
            if (q.isEmpty()) q = "<empty>";

            String now = LocalTime.now().format(TIME);

            // Make output deterministic + clearly different
            String output = ""
                    + "=== DeskPilot Demo Result ===\n"
                    + "Time: " + now + "\n"
                    + "Query: " + q + "\n"
                    + "Length: " + q.length() + "\n"
                    + "Upper: " + q.toUpperCase() + "\n"
                    + "-----------------------------\n";

            results.setText(output);

            // Flip background to guarantee a pixel-diff
            Color bg = results.getBackground();
            boolean isLight = bg.getRed() > 200;
            results.setBackground(isLight ? new Color(230, 255, 230) : new Color(255, 240, 230));

            status.setText("Status: searched");
        };

        searchBtn.addActionListener(e -> doSearch.run());
        input.addActionListener(e -> doSearch.run()); // ENTER triggers search too

        // Layout
        root.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(status, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        center.add(hint, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Start in a known visual state
        results.setBackground(new Color(255, 240, 230));
    }
}
