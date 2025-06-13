package org.example;

import javax.swing.*;
import java.awt.*;

public class OthelloGame extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private static final Color BOARD_COLOR = new Color(0, 128, 0);
    private static final Color LINE_COLOR = Color.BLACK;

    private int[][] board; // 0 = empty, 1 = black, 2 = white
    private int currentPlayer; // 1 = black, 2 = white
    private GamePanel gamePanel;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private JButton newGameButton;

    public OthelloGame() {
        setTitle("Othello Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializeGame();
        setupUI();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeGame() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        currentPlayer = 1; // Black starts first

        // Set up initial pieces
        board[3][3] = 2; // White
        board[3][4] = 1; // Black
        board[4][3] = 1; // Black
        board[4][4] = 2; // White
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Game panel
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(Color.LIGHT_GRAY);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status and score
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        statusLabel = new JLabel("Black's turn", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        infoPanel.add(statusLabel);
        infoPanel.add(scoreLabel);
        controlPanel.add(infoPanel, BorderLayout.CENTER);

        // New game button
        newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> {
            initializeGame();
            gamePanel.repaint();
        });
        controlPanel.add(newGameButton, BorderLayout.EAST);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            setBackground(BOARD_COLOR);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw grid lines
            g2d.setColor(LINE_COLOR);
            g2d.setStroke(new BasicStroke(2));

            for (int i = 0; i <= BOARD_SIZE; i++) {
                g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
                g2d.drawLine(0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE, i * CELL_SIZE);
            }

            // Draw pieces
            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    if (board[row][col] != 0) {
                        drawPiece(g2d, row, col, board[row][col]);
                    }
                }
            }
        }

        private void drawPiece(Graphics2D g2d, int row, int col, int player) {
            int x = col * CELL_SIZE + 5;
            int y = row * CELL_SIZE + 5;
            int size = CELL_SIZE - 10;

            if (player == 1) {
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }

            g2d.fillOval(x, y, size, size);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, size, size);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new OthelloGame();
        });
    }
}