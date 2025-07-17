import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Point;

public class OthelloServer {
    private static final int PORT = 6000;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private OthelloBoard board;
    private int currentPlayer = 1; // 1=黒,2=白
    private int boardSize = 8; // デフォルト
    private GameMode gameMode = GameMode.NORMAL;
    public enum GameMode { NORMAL, BLOCK }

    public OthelloServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("サーバー起動。クライアント2人の接続待ち...");

        while (clients.size() < 2) {
            Socket socket = serverSocket.accept();
            ClientHandler ch = new ClientHandler(socket, clients.size() + 1);
            clients.add(ch);
            new Thread(ch).start();
            System.out.println("クライアント" + ch.player + "接続: " + socket.getInetAddress());
        }

        clients.get(0).send("SHOW_MENU");
        clients.get(1).send("SHOW_WAIT");
    }

    private synchronized void handleCommand(int player, String line) {
        if (line.startsWith("START")) {
            String[] parts = line.split(" ");
            boardSize = Integer.parseInt(parts[1]);
            if (parts.length >= 3 && parts[2].equalsIgnoreCase("BLOCK")) {
                gameMode = GameMode.BLOCK;
            } else {
                gameMode = GameMode.NORMAL;
            }
            board = new OthelloBoard(boardSize);
            currentPlayer = 1;
            broadcast("START_GAME " + boardSize + " " + gameMode);
            sendPlayers();
            broadcastBoard();
            sendTurnInfo();
        } else if (line.startsWith("MOVE")) {
            String[] parts = line.split(" ");
            int r = Integer.parseInt(parts[1]);
            int c = Integer.parseInt(parts[2]);
            handleMove(player, r, c);
        } else if (line.startsWith("BLOCK")) {
            String[] parts = line.split(" ");
            int r = Integer.parseInt(parts[1]);
            int c = Integer.parseInt(parts[2]);
            handleBlock(player, r, c);
        } else if (line.equals("MENU")) {
            clients.get(0).send("SHOW_MENU");
            clients.get(1).send("SHOW_WAIT");
        } else if (line.equals("EXIT")) {
            System.out.println("Player " + player + "切断");
            if (player == 1 && clients.size() > 1) {
                clients.get(1).send("HOST_EXITED");
                System.exit(0);
            } else if (player == 2 && clients.size() > 1) {
                clients.get(0).send("GUEST_EXITED");
            }
        }
    }

    private void handleBlock(int player, int r, int c) {
        board.setBlockedCell(r, c);
        for (ClientHandler ch : clients) {
            if (ch.player != player) {
                ch.send("BLOCK " + r + " " + c);
            }
        }
        sendTurnInfo();
    }

    private void sendPlayers() {
        for (ClientHandler ch : clients) {
            ch.send("PLAYER " + ch.player);
        }
    }

    private void broadcast(String msg) {
        for (ClientHandler ch : clients) ch.send(msg);
    }

    private void broadcastBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append("BOARD ");
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                sb.append(board.board[r][c]);
                if (!(r == boardSize - 1 && c == boardSize - 1)) sb.append(",");
            }
        }
        broadcast(sb.toString());
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

    private synchronized void handleMove(int player, int row, int col) {
        if (player != currentPlayer) return;
        if (!board.canPlace(row, col, player)) return;
        board.clearBlockedCell();
        board.flip(row, col, player);

        int nextPlayer = (currentPlayer == 1) ? 2 : 1;
        broadcastBoard();

        if (board.hasAnyValidMove(nextPlayer)) {
            currentPlayer = nextPlayer;
            if (gameMode == GameMode.BLOCK) {
                List<int[]> rawMoves = board.getValidMoves(nextPlayer);
                if (rawMoves.size() >= 2) {
                    for (ClientHandler ch : clients) {
                        if (ch.player == player) {
                            ch.send("SELECT_BLOCK");
                        }
                    }
                } else if (rawMoves.size() == 1) {
                    // ブロック可能なマスが1つだけならゲーム終了
                    currentPlayer = 0;
                    sendGameResult();
                } else {
                    sendTurnInfo();
                }
            } else {
                sendTurnInfo();
            }
        } else if (board.hasAnyValidMove(currentPlayer)) {
            sendTurnInfo();
        } else {
            currentPlayer = 0;
            sendGameResult();
        }
    }

    private void sendGameResult() {
        int blackCount = board.countStones(1);
        int whiteCount = board.countStones(2);

        String result;
        if (blackCount > whiteCount) {
            result = "RESULT BLACK " + blackCount + " " + whiteCount;
        } else if (whiteCount > blackCount) {
            result = "RESULT WHITE " + blackCount + " " + whiteCount;
        } else {
            result = "RESULT DRAW " + blackCount + " " + whiteCount;
        }

        broadcast(result);
        System.out.println("ゲーム終了: " + result);
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
        }

        public void send(String msg) {
            out.println(msg);
        }

        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println("Player " + player + "からの受信: " + line);
                    handleCommand(player, line);
                }
            } catch (IOException e) {
                System.out.println("Player " + player + "切断");
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new OthelloServer();
    }
}
