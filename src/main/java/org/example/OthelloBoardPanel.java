import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class OthelloBoardPanel extends JPanel {
    public int[][] board; // 可変サイズ盤面
    private int currentPlayer = 1;
    private boolean myTurn = false;
    

    private List<Point> validMoves = new ArrayList<>();
    private ClickListener listener;

    public interface ClickListener {
        void onClick(int row, int col);
    }

    public OthelloBoardPanel(int[][] board) {
        this.board = board;
        setPreferredSize(new Dimension(board.length * 60, board.length * 60));
        setBackground(new Color(0, 128, 0));

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!myTurn || listener == null) return;
                int cellSize = getWidth() / board.length; // 盤面サイズで計算
                int col = e.getX() / cellSize;
                int row = e.getY() / cellSize;
                if (row >= 0 && row < board.length && col >= 0 && col < board.length) {
                    listener.onClick(row, col);
                }
            }
        });
    }

    public void addClickListener(ClickListener listener) {
        this.listener = listener;
    }

    public void setCurrentPlayer(int player) {
        this.currentPlayer = player;
    }

    public void setMyTurn(boolean turn) {
        this.myTurn = turn;
        updateValidMoves();
        repaint();
    }

    public void refresh() {
        updateValidMoves();
        repaint();
    }

    private void updateValidMoves() {
        validMoves.clear();
        if (!myTurn) return;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board.length; c++) {
                if (canPlace(r, c, currentPlayer)) {
                    validMoves.add(new Point(c, r));
                }
            }
        }
    }

    public boolean canPlace(int row, int col, int player) {
        if (board[row][col] != 0) return false;
        int opponent = (player == 1) ? 2 : 1;
        int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};
        int size = board.length;

        for (int d = 0; d < 8; d++) {
            int r = row + DY[d];
            int c = col + DX[d];
            boolean foundOpponent = false;
            while (r >= 0 && r < size && c >= 0 && c < size) {
                if (board[r][c] == opponent) {
                    foundOpponent = true;
                } else if (board[r][c] == player) {
                    if (foundOpponent) return true;
                    else break;
                } else {
                    break;
                }
                r += DY[d];
                c += DX[d];
            }
        }
        return false;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int size = board.length; // 可変
        int cellSize = getWidth() / size;

        Graphics2D g2 = (Graphics2D) g;

        // グリッド描画
        g2.setColor(Color.BLACK);
        for (int i = 0; i <= size; i++) {
            g2.drawLine(i * cellSize, 0, i * cellSize, size * cellSize);
            g2.drawLine(0, i * cellSize, size * cellSize, i * cellSize);
        }

        // 石描画
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == 1) {
                    drawPiece(g2, r, c, cellSize, Color.BLACK);
                } else if (board[r][c] == 2) {
                    drawPiece(g2, r, c, cellSize, Color.WHITE);
                }
            }
        }

        // 置ける場所のハイライト
        if (myTurn) {
            g2.setColor(new Color(150, 0, 0, 100));
            for (Point p : validMoves) {
                int x = p.x * cellSize + cellSize / 6;
                int y = p.y * cellSize + cellSize / 6;
                int ovalSize = cellSize * 2 / 3;
                g2.fillOval(x, y, ovalSize, ovalSize);
            }
        }
    }

    private void drawPiece(Graphics2D g2, int row, int col, int cellSize, Color color) {
        int x = col * cellSize + cellSize / 12;
        int y = row * cellSize + cellSize / 12;
        int size = cellSize * 5 / 6;
        g2.setColor(color);
        g2.fillOval(x, y, size, size);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(x, y, size, size);
    }


    // 可変盤面の内容を外部からセットしたい場合
    public void setBoard(int[][] newBoard) {
        if (newBoard.length != board.length) {
            throw new IllegalArgumentException("盤面サイズが一致しません");
        }
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board.length; c++) {
                board[r][c] = newBoard[r][c];
            }
        }
        repaint();
    }

    // セル単位で値をセットする場合
    public void setCell(int row, int col, int value) {
        board[row][col] = value;
        repaint();
    }
}
