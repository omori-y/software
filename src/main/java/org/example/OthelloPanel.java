import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class OthelloPanel extends JPanel {
    private static final int CELL_SIZE = 60;
    private static final int BOARD_SIZE = 8;

    private int[][] board; // 盤面情報
    private int currentPlayer = 1; // 自分のプレイヤー番号（1=黒, 2=白）
    private boolean myTurn = false;

    private List<Point> validMoves = new ArrayList<>();
    private List<Point> blockCandidates = new ArrayList<>();

    private boolean blockingMode = false;
    private Point blockedCell = null;

    private ClickListener listener;

    public interface ClickListener {
        void onClick(int row, int col);
    }

    public OthelloPanel(int[][] board) {
        this.board = board;
        setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
        setBackground(new Color(0, 128, 0)); // 緑の盤面

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (listener == null) return;

                int col = e.getX() / CELL_SIZE;
                int row = e.getY() / CELL_SIZE;

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
        updateBlockCandidates();
        repaint();
    }

    public void setBlockedCell(int row, int col) {
    if(row < 0 || col < 0) {
        this.blockedCell = null;
    } else {
        this.blockedCell = new Point(col, row);
    }
    repaint();
    }


    public void refresh() {
    if (blockingMode) {
        updateBlockCandidates();
    } else {
        updateValidMoves();
    }
    repaint();
}

    private void updateValidMoves() {
        validMoves.clear();
        if (!myTurn) return;

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
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

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c] == 0 && canPlace(r, c, opponent)) {
                    blockCandidates.add(new Point(c, r));
                }
            }
        }
    }

@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    // 盤面の線
    g2.setColor(Color.BLACK);
    for (int i = 0; i <= BOARD_SIZE; i++) {
        g2.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
        g2.drawLine(0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE, i * CELL_SIZE);
    }

    // 石の描画
    for (int r = 0; r < BOARD_SIZE; r++) {
        for (int c = 0; c < BOARD_SIZE; c++) {
            if (board[r][c] == 1) {
                drawPiece(g2, r, c, Color.BLACK);
            } else if (board[r][c] == 2) {
                drawPiece(g2, r, c, Color.WHITE);
            }
        }
    }

    // ハイライト（有効手）
    if (myTurn && !blockingMode) {
        g2.setColor(new Color(150, 0, 0, 100));
        for (Point p : validMoves) {
            int x = p.x * CELL_SIZE + 10;
            int y = p.y * CELL_SIZE + 10;
            int size = CELL_SIZE - 20;
            g2.fillOval(x, y, size, size);
        }
    }

    if (blockingMode && myTurn) {  // ×マーク（ブロック候補）c
        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(3));
        for (Point p : blockCandidates) {
            int x = p.x * CELL_SIZE + 15;
            int y = p.y * CELL_SIZE + 15;
            int size = CELL_SIZE - 30;
            g2.drawLine(x, y, x + size, y + size);
            g2.drawLine(x, y + size, x + size, y);
        }
    }

    // ブロック済みマスの×（相手のターンかつブロックモード中のみ表示）
    if (blockedCell != null) { 
        g2.setColor(Color.RED.darker());
        g2.setStroke(new BasicStroke(4));
        int x = blockedCell.x * CELL_SIZE + 15;
        int y = blockedCell.y * CELL_SIZE + 15;
        int size = CELL_SIZE - 30;
        g2.drawLine(x, y, x + size, y + size);
        g2.drawLine(x, y + size, x + size, y);
    }
    
}


    private void drawPiece(Graphics2D g2, int row, int col, Color color) {
        int x = col * CELL_SIZE + 5;
        int y = row * CELL_SIZE + 5;
        int size = CELL_SIZE - 10;

        g2.setColor(color);
        g2.fillOval(x, y, size, size);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(x, y, size, size);
    }

    // クライアント側の簡易canPlace（blockedCellも考慮）
    private boolean canPlace(int row, int col, int player) {
        if (board[row][col] != 0) return false;

        if (blockedCell != null && blockedCell.x == col && blockedCell.y == row) {
            return false;
        }

        int opponent = (player == 1) ? 2 : 1;
        int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int d = 0; d < 8; d++) {
            int r = row + DY[d];
            int c = col + DX[d];
            boolean foundOpponent = false;

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                if (board[r][c] == opponent) {
                    foundOpponent = true;
                } else if (board[r][c] == player) {
                    if (foundOpponent) return true;
                    else break;
                } else break;
                r += DY[d];
                c += DX[d];
            }
        }
        return false;
    }
}
