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
    private List<Point> blockCandidates = new ArrayList<>();
    private boolean blockingMode = false;
    private Point blockedCell = null;
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
                if (listener == null) return;
                int cellSize = getWidth() / board.length;
                int col = e.getX() / cellSize;
                int row = e.getY() / cellSize;
                if (row < 0 || row >= board.length || col < 0 || col >= board.length) return;

                if (blockingMode) {
                    for (Point p : blockCandidates) {
                        if (p.x == col && p.y == row) {
                            listener.onClick(row, col);
                            return;
                        }
                    }
                } else {
                    if (!myTurn) return;
                    for (Point p : validMoves) {
                        if (p.x == col && p.y == row) {
                            listener.onClick(row, col);
                            return;
                        }
                    }
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

    public void setBlockingMode(boolean blocking) {
        this.blockingMode = blocking;
            if (!blocking) {
        blockCandidates.clear();   // ← 妨害モード終了時に候補を消す
        }
        updateBlockCandidates();
        repaint();
    }

    public void setBlockedCell(int row, int col) {
        this.blockedCell = new Point(col, row);
        repaint();
    }

    public Point getBlockedCell() {
        return blockedCell;
    }

    public void clearBlockedCell() {
        this.blockedCell = null;
        repaint();
    }

    public void refresh() {
        updateValidMoves();
        repaint();
    }

    private void updateValidMoves() {
        validMoves.clear();
        if (!myTurn || blockingMode) return;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board.length; c++) {
                if (canPlace(r, c, currentPlayer)) {
                    validMoves.add(new Point(c, r));
                }
            }
        }
    }

    private void updateBlockCandidates() {
        blockCandidates.clear();
        if (!blockingMode) return;

        int opponent = (currentPlayer == 1) ? 2 : 1;

        // blockedCellを一時的に無効化して合法手リストを作る
        Point oldBlocked = blockedCell;
        blockedCell = null;

        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board.length; c++) {
                if (board[r][c] == 0 && canPlace(r, c, opponent)) {
                    blockCandidates.add(new Point(c, r));
                }
            }
        }
        blockedCell = oldBlocked;
    }

    public void setBlockCandidates(List<Point> candidates) {
        blockCandidates.clear();
        if (candidates != null) blockCandidates.addAll(candidates);
        repaint();
    }

    public boolean canPlace(int row, int col, int player) {
        if (board[row][col] != 0) return false;
        if (blockedCell != null && blockedCell.x == col && blockedCell.y == row) return false;
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

        int size = board.length;
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
        if (myTurn && !blockingMode) {
            g2.setColor(new Color(150, 0, 0, 100));
            for (Point p : validMoves) {
                int x = p.x * cellSize + cellSize / 6;
                int y = p.y * cellSize + cellSize / 6;
                int ovalSize = cellSize * 2 / 3;
                g2.fillOval(x, y, ovalSize, ovalSize);
            }
        }

        // ブロック候補 ×印
        if (blockingMode) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
            for (Point p : blockCandidates) {
                int x = p.x * cellSize + 15;
                int y = p.y * cellSize + 15;
                int blockSize = cellSize - 30;
                g2.drawLine(x, y, x + blockSize, y + blockSize);
                g2.drawLine(x, y + blockSize, x + blockSize, y);
            }
        }

        // ブロック済みマスにも×を太く描画
        if (blockedCell != null) {
            g2.setColor(Color.RED.darker());
            g2.setStroke(new BasicStroke(4));
            int x = blockedCell.x * cellSize + 12;
            int y = blockedCell.y * cellSize + 12;
            int blockSize = cellSize - 30;
            g2.drawLine(x, y, x + blockSize, y + blockSize);
            g2.drawLine(x, y + blockSize, x + blockSize, y);
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
}

