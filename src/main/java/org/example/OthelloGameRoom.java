import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Point;

public class OthelloGameRoom implements Runnable {
    private List<ClientHandler> clients = new ArrayList<>();
    private OthelloBoard board = new OthelloBoard();
    private int currentPlayer = 1;
    private boolean isBlockingPhase = false;
    private Point blockedCell = null;

    public boolean isFull() {
        return clients.size() >= 2;
    }

    public void addPlayer(Socket socket) throws IOException {
        ClientHandler ch = new ClientHandler(socket, clients.size() + 1);
        clients.add(ch);
        new Thread(ch).start();
    }

    public void run() {
        System.out.println("新しい部屋でゲーム開始");

        while (clients.size() < 2) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        broadcastBoard();
        sendTurnInfo();
    }

    private void broadcastBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append("BOARD ");
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                sb.append(board.board[r][c]);
                if (!(r == 7 && c == 7)) sb.append(",");
            }
        }
        String msg = sb.toString();
        for (ClientHandler ch : clients) {
            ch.send(msg);
        }

        int black = 0, white = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board.board[r][c] == 1) black++;
                else if (board.board[r][c] == 2) white++;
            }
        }
        for (ClientHandler ch : clients) {
            ch.send("SCORE " + black + " " + white);
        }
    }

    private void sendTurnInfo() {
        for (ClientHandler ch : clients) {
            if (ch.player == currentPlayer) {
                ch.send("YOUR_TURN");
            } else {
                ch.send("WAIT");
            }
        }
    }

    private void sendTurnInfoBlockingPhase() {
        for (ClientHandler ch : clients) {
            ch.send("WAIT");
        }
    }

    private synchronized void handleMove(int player, int row, int col) {
        if (isBlockingPhase) return;
        if (player != currentPlayer) return;
        if (!board.canPlace(row, col, player)) return;

        board.blockedCell = null;
        blockedCell = null;

        board.flip(row, col, player);
        System.out.println("Player " + player + " placed at " + row + "," + col);

        isBlockingPhase = true;
        blockedCell = null;

        broadcastBoard();
        sendTurnInfoBlockingPhase();

        board.blockedCell = null;
        blockedCell = null;

        for (ClientHandler ch : clients) {
            ch.send("CLEAR_BLOCK " + player);
        }
    }

    private synchronized void handleBlock(int player, int row, int col) {
    if (!isBlockingPhase) return;
    if (player != currentPlayer) return;
    if (board.board[row][col] != 0) return;

    Point p = new Point(row, col);
    board.blockedCell = p;
    blockedCell = p;

    System.out.println("Player " + player + " blocked " + row + "," + col);

    isBlockingPhase = false;
    currentPlayer = (currentPlayer == 1) ? 2 : 1;

    for (ClientHandler ch : clients) {
        ch.send("BLOCK " + row + " " + col);
    }

    broadcastBoard();

    if (!board.hasAnyValidMove(currentPlayer)) {
        System.out.println("Player " + currentPlayer + " has no valid move. Game over.");

        int winner = board.getWinner();

        // 駒数カウント
        int blackCount = 0;
        int whiteCount = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board.board[r][c] == 1) blackCount++;
                else if (board.board[r][c] == 2) whiteCount++;
            }
        }

        String result;
        if (winner == 0) result = "DRAW";
        else if (winner == 1) result = "BLACK";
        else result = "WHITE";

        for (ClientHandler ch : clients) {
            // 形式: GAME_OVER 勝者 駒数黒 駒数白
            ch.send("GAME_OVER " + result + " " + blackCount + " " + whiteCount);
        }
        return;
    }

    sendTurnInfo();
}


    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int player;

        ClientHandler(Socket socket, int player) throws IOException {
            this.socket = socket;
            this.player = player;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            send("PLAYER " + player);
        }

        public void send(String msg) {
            out.println(msg);
        }

        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println("Player " + player + "からの受信: " + line);
                    if (line.startsWith("MOVE")) {
                        String[] parts = line.split(" ");
                        int r = Integer.parseInt(parts[1]);
                        int c = Integer.parseInt(parts[2]);
                        handleMove(player, r, c);
                    } else if (line.startsWith("BLOCK")) {
                        String[] parts = line.split(" ");
                        int r = Integer.parseInt(parts[1]);
                        int c = Integer.parseInt(parts[2]);
                        handleBlock(player, r, c);
                    }
                }
            } catch (IOException e) {
                System.out.println("Player " + player + "切断");
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }
} 
