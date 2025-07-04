import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.BiConsumer;

public class OthelloPanel extends JPanel {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private static final Color BOARD_COLOR = new Color(0, 128, 0);
    private static final Color LINE_COLOR = Color.BLACK;

    private int[][] board;
    private BiConsumer<Integer, Integer> clickListener;

    public OthelloPanel(int[][] board) {
        this.board = board;
        setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int col = e.getX() / CELL_SIZE;
                int row = e.getY() / CELL_SIZE;
                if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
                    if (clickListener != null) {
                        clickListener.accept(row, col);
                    }
                }
            }
        });
    }

    public void addClickListener(BiConsumer<Integer, Integer> listener) {
        this.clickListener = listener;
    }

    public void refresh() {
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 盤面背景
        g2d.setColor(BOARD_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // グリッド線
        g2d.setColor(LINE_COLOR);
        for (int i = 0; i <= BOARD_SIZE; i++) {
            g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
            g2d.drawLine(0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE, i * CELL_SIZE);
        }

        // 石の描画
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
