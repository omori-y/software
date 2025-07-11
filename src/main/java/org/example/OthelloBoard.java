import java.util.ArrayList;
import java.util.List;

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
        board[3][3] = 2;
        board[3][4] = 1;
        board[4][3] = 1;
        board[4][4] = 2;
    }

    public boolean canPlace(int row, int col, int player) {
        if (!isInBounds(row, col) || board[row][col] != 0) return false;

        int opponent = (player == 1) ? 2 : 1;

        for (int d = 0; d < 8; d++) {
            int r = row + DX[d];
            int c = col + DY[d];
            boolean hasOpponentBetween = false;
            while (isInBounds(r, c)) {
                if (board[r][c] == opponent) {
                    hasOpponentBetween = true;
                } else if (board[r][c] == player) {
                    if (hasOpponentBetween) return true;
                    else break;
                } else {
                    break;
                }
                r += DX[d];
                c += DY[d];
            }
        }
        return false;
    }

    public void flip(int row, int col, int player) {
        int opponent = (player == 1) ? 2 : 1;
        board[row][col] = player;

        for (int d = 0; d < 8; d++) {
            int r = row + DX[d];
            int c = col + DY[d];
            boolean hasOpponentBetween = false;

            while (isInBounds(r, c)) {
                if (board[r][c] == opponent) {
                    hasOpponentBetween = true;
                } else if (board[r][c] == player) {
                    if (hasOpponentBetween) {
                        int flipR = row + DX[d];
                        int flipC = col + DY[d];
                        while (flipR != r || flipC != c) {
                            board[flipR][flipC] = player;
                            flipR += DX[d];
                            flipC += DY[d];
                        }
                    }
                    break;
                } else {
                    break;
                }
                r += DX[d];
                c += DY[d];
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

    public int countStones(int color) {
        int count = 0;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c] == color) {
                    count++;
                }
            }
        }
        return count;
    }

}
