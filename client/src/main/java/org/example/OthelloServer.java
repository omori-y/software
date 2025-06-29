import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class OthelloServer extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private static final Color BOARD_COLOR = new Color(0, 128, 0);
    private static final Color LINE_COLOR = Color.BLACK;

    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE]; // 0=空,1=黒,2=白
    private int currentPlayer = 1; // 黒からスタート（サーバー側）
    private boolean myTurn = true; // サーバーは先手

    private JPanel gamePanel;
    private JLabel statusLabel;
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public OthelloServer() {
        setTitle("Othello Server (Black)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        initializeBoard();
        setupUI();
        setupNetwork();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeBoard() {
        for(int r=0; r<BOARD_SIZE; r++)
            for(int c=0; c<BOARD_SIZE; c++)
                board[r][c] = 0;
        // 初期配置
        board[3][3] = 2; board[3][4] = 1;
        board[4][3] = 1; board[4][4] = 2;
    }

    private void setupUI() {
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
        gamePanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!myTurn) return; // 自分のターンでないなら無視

                int col = e.getX() / CELL_SIZE;
                int row = e.getY() / CELL_SIZE;

                if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return;
                if (board[row][col] != 0) return;

                // ここに置けるかルール判定を入れても良いですがとりあえず置けるならOKとします
                board[row][col] = currentPlayer;
                repaint();

                sendMove(row, col);

                myTurn = false;
                updateStatus();
            }
        });

        statusLabel = new JLabel("あなたのターン (黒)", SwingConstants.CENTER);

        add(gamePanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(() -> {
            if (myTurn) {
                statusLabel.setText("あなたのターン (黒)");
            } else {
                statusLabel.setText("相手のターン (白)");
            }
        });
    }

    private void setupNetwork() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(6000);
                System.out.println("クライアント接続待機中...");
                socket = serverSocket.accept();
                System.out.println("クライアント接続されました: " + socket.getInetAddress());

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 相手からの手を受信
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("受信: " + line);
                    if (line.startsWith("MOVE")) {
                        String[] parts = line.split(" ");
                        int r = Integer.parseInt(parts[1]);
                        int c = Integer.parseInt(parts[2]);
                        receiveMove(r, c);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMove(int row, int col) {
        String move = "MOVE " + row + " " + col;
        out.println(move);
        System.out.println("送信: " + move);
    }

    private void receiveMove(int row, int col) {
        board[row][col] = 2; // クライアント側は白なので2
        myTurn = true;
        updateStatus();
        repaint();
    }

    private class GamePanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D)g;
            g2d.setColor(BOARD_COLOR);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 線を描画
            g2d.setColor(LINE_COLOR);
            for (int i = 0; i <= BOARD_SIZE; i++) {
                g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
                g2d.drawLine(0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE, i * CELL_SIZE);
            }

            // 石を描画
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    if (board[r][c] != 0) {
                        drawPiece(g2d, r, c, board[r][c]);
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
        SwingUtilities.invokeLater(() -> new OthelloServer());
    }
}
