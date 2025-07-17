package org.example;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OthelloBoard {
    private int BOARD_SIZE;
    public int[][] board;
    private static final int[] DX = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] DY = {-1, 0, 1, -1, 1, -1, 0, 1};

    private Point blockedCell = null;

    public OthelloBoard(int size) {
        this.BOARD_SIZE = size;
        board = new int[size][size];
        initializeBoard();
    }

    private void initializeBoard() {
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                board[r][c] = 0;
        int mid1 = BOARD_SIZE / 2 - 1;
        int mid2 = BOARD_SIZE / 2;
        board[mid1][mid1] = 2;
        board[mid2][mid2] = 2;
        board[mid1][mid2] = 1;
        board[mid2][mid1] = 1;
        blockedCell = null;
    }

    public void setBlockedCell(int r, int c) {
        blockedCell = new Point(r, c);
    }
    public void clearBlockedCell() {
        blockedCell = null;
    }
    public Point getBlockedCell() {
        return blockedCell;
    }

    public boolean canPlace(int row, int col, int player) {
        if (!isInBounds(row, col) || board[row][col] != 0) return false;
        if (blockedCell != null && blockedCell.x == row && blockedCell.y == col) return false;
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
