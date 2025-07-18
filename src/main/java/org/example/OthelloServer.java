package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class OthelloServer {
    private static final int PORT = 6000;
    private ServerSocket serverSocket;
    private final List<ClientHandler> lobby = new ArrayList<>();

    public OthelloServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("サーバー起動: 複数マッチ対応");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler client = new ClientHandler(socket);
            new Thread(client).start();

            synchronized (lobby) {
                lobby.add(client);
                if (lobby.size() >= 2) {
                    ClientHandler p1 = lobby.remove(0);
                    ClientHandler p2 = lobby.remove(0);
                    GameSession session = new GameSession(p1, p2);
                    new Thread(session).start();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new OthelloServer();
    }

    // ゲームセッション
    static class GameSession implements Runnable {
        private ClientHandler player1, player2;
        private OthelloBoard board;
        private int currentPlayer = 1;
        private GameMode gameMode = GameMode.NORMAL;
        private int boardSize = 8;
        private boolean waitingForBlock = false;

        enum GameMode { NORMAL, BLOCK }

        public GameSession(ClientHandler p1, ClientHandler p2) {
            this.player1 = p1;
            this.player2 = p2;
            board = new OthelloBoard(boardSize);
        }

        public void run() {
            player1.init(this, 1);
            player2.init(this, 2);
            player1.send("SHOW_MENU");
            player2.send("SHOW_WAIT");
        }

        public synchronized void handleCommand(int player, String line) {
            try {
                if (line.startsWith("START")) {
                    String[] parts = line.split(" ");
                    boardSize = Integer.parseInt(parts[1]);
                    gameMode = (parts.length >= 3 && parts[2].equals("BLOCK")) ? GameMode.BLOCK : GameMode.NORMAL;
                    board = new OthelloBoard(boardSize);
                    currentPlayer = 1;     // ←ここで先手を必ず1にリセットする
                    waitingForBlock = false;
                    broadcast("START_GAME " + boardSize + " " + gameMode);
                    sendPlayers();
                    broadcastBoard();
                    sendTurnInfo();
                } else if (line.startsWith("MOVE")) {
                    if (waitingForBlock) return;
                    int r = Integer.parseInt(line.split(" ")[1]);
                    int c = Integer.parseInt(line.split(" ")[2]);
                    handleMove(player, r, c);
                } else if (line.startsWith("BLOCK")) {
                    if (!waitingForBlock) return;
                    waitingForBlock = false;

                    int r = Integer.parseInt(line.split(" ")[1]);
                    int c = Integer.parseInt(line.split(" ")[2]);
                    board.setBlockedCell(r, c);
                    getOpponent(player).send("BLOCK " + r + " " + c);

                    int nextPlayer = (player == 1) ? 2 : 1;

                    if (gameMode == GameMode.BLOCK && board.countValidMoves(nextPlayer) == 0) {
                        if(board.countValidMoves(player) == 0) sendGameResult();
                        currentPlayer = player;
                        sendTurnInfo();
                        getPlayer(currentPlayer).send("CLEAR_BLOCK_AND_YOUR_TURN");
                        getOpponent(currentPlayer).send("WAIT");
                    } else if (board.hasAnyValidMove(nextPlayer)) {
                        currentPlayer = nextPlayer;
                        sendTurnInfo();
                    } else if (board.hasAnyValidMove(player)) {
                        currentPlayer = player;
                        sendTurnInfo();
                    } else {
                        sendGameResult();
                    }
                } else if (line.equals("MENU")) {
                    player1.send("SHOW_MENU");
                    player2.send("SHOW_WAIT");
                } else if (line.equals("EXIT")) {
                    getOpponent(player).send((player == 1 ? "HOST_EXITED" : "GUEST_EXITED"));
                }
            } catch (Exception e) {
                System.err.println("コマンド処理中のエラー: " + e);
            }
        }

        private static final int BLOCK_FORBIDDEN_TURNS_REMAINING = 6;
        private void handleMove(int player, int r, int c) {
            if (waitingForBlock) return;          // ブロック中は操作禁止
            if (player != currentPlayer) return;  // 自分のターンでなければ無視
            if (!board.canPlace(r, c, player)) return;

            board.clearBlockedCell();
            board.flip(r, c, player);
            broadcastBoard();

            int nextPlayer = (currentPlayer == 1) ? 2 : 1;
            int emptyCells = boardSize * boardSize - board.countStones(currentPlayer) - board.countStones(nextPlayer);
            

            if (board.hasAnyValidMove(nextPlayer)) {
                if (gameMode == GameMode.BLOCK && emptyCells > BLOCK_FORBIDDEN_TURNS_REMAINING) {
                    waitingForBlock = true;
                    getPlayer(currentPlayer).send("SELECT_BLOCK");
                } else {
                    currentPlayer = nextPlayer;
                    sendTurnInfo();
                }
            } else if (board.hasAnyValidMove(currentPlayer)) {
                sendTurnInfo();  // 自分が続行
            } else {
                sendGameResult();
            }
            
        }



        private void sendPlayers() {
            player1.send("PLAYER 1");
            player2.send("PLAYER 2");
        }

        private void sendTurnInfo() {
            getPlayer(currentPlayer).send("YOUR_TURN");
            getOpponent(currentPlayer).send("WAIT");
        }

        private void broadcast(String msg) {
            player1.send(msg);
            player2.send(msg);
        }

        private void broadcastBoard() {
            StringBuilder sb = new StringBuilder("BOARD ");
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    sb.append(board.board[r][c]);
                    if (!(r == boardSize - 1 && c == boardSize - 1)) sb.append(",");
                }
            }
            broadcast(sb.toString());
        }

        private void sendGameResult() {
            int black = board.countStones(1);
            int white = board.countStones(2);
            String result = (black > white) ? "RESULT BLACK " : (white > black) ? "RESULT WHITE " : "RESULT DRAW ";
            broadcast(result + black + " " + white);
        }

        private ClientHandler getPlayer(int num) {
            return (num == 1) ? player1 : player2;
        }

        private ClientHandler getOpponent(int num) {
            return (num == 1) ? player2 : player1;
        }
    }

    // クライアント処理
    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int player;
        private GameSession session;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        public void init(GameSession session, int player) {
            this.session = session;
            this.player = player;
        }

        public void send(String msg) {
            out.println(msg);
        }

        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (session != null) {
                        session.handleCommand(player, line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Player " + player + "切断");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
