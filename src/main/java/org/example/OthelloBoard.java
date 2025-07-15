import java.util.ArrayList;
import java.util.List;
import java.awt.Point;

public class OthelloBoard {
    public static final int BOARD_SIZE = 8;
    public int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

    private static final int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};

    public OthelloBoard() {
        initializeBoard();
    }

    private void initializeBoard() {
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                board[r][c] = 0;
        // 初期配置
        board[3][3] = 2; // 白
        board[3][4] = 1; // 黒
        board[4][3] = 1; // 黒
        board[4][4] = 2; // 白
    }

    public boolean canPlace(int row, int col, int player) {
    if (board[row][col] != 0) return false;

    // 🔒 blockedCell に指定されたマスなら false を返す（= 置けない）
    if (blockedCell != null && blockedCell.x == row && blockedCell.y == col) {
        return false;
    }

    int opponent = (player == 1) ? 2 : 1;
    int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
    int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};

    for (int d = 0; d < 8; d++) {
        int r = row + DY[d];
        int c = col + DX[d];
        boolean foundOpponent = false;

        while (r >= 0 && r < 8 && c >= 0 && c < 8) {
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

   
public Point blockedCell = null;


    public void flip(int row, int col, int player) {
        int opponent = (player == 1) ? 2 : 1;
        board[row][col] = player;

        for (int d = 0; d < 8; d++) {
            int r = row + DY[d];
            int c = col + DX[d];
            boolean hasOpponentBetween = false;

            while (isInBounds(r, c)) {
                if (board[r][c] == opponent) {
                    hasOpponentBetween = true;
                } else if (board[r][c] == player) {
                    if (hasOpponentBetween) {
                        int flipR = row + DY[d];
                        int flipC = col + DX[d];
                        while (flipR != r || flipC != c) {
                            board[flipR][flipC] = player;
                            flipR += DY[d];
                            flipC += DX[d];
                        }
                    }
                    break;
                } else {
                    break;
                }
                r += DY[d];
                c += DX[d];
            }
        }
    }

    public boolean hasAnyValidMove(int player) {
        return !getValidMoves(player).isEmpty();
    }

    public List<int[]> getValidMoves(int player) {
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (canPlace(r, c, player)) {
                    moves.add(new int[]{r, c});
                }
            }
        }
        return moves;
    }

    private boolean isInBounds(int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
    }
}
