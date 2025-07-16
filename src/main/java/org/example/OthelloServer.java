import java.io.*;
import java.net.*;
import java.util.*;

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

        // 1番がメニュー、2番が待機状態
        clients.get(0).send("SHOW_MENU");
        clients.get(1).send("SHOW_WAIT");
    }

    // クライアントからコマンドを受けたとき
    private synchronized void handleCommand(int player, String line) {
        if (line.startsWith("START")) {
            // クライアント1から受信
            String[] parts = line.split(" ");
            boardSize = Integer.parseInt(line.split(" ")[1]);
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
            // ★妨害マスコマンド: BLOCK r c
            String[] parts = line.split(" ");
            int r = Integer.parseInt(parts[1]);
            int c = Integer.parseInt(parts[2]);
            handleBlock(player, r, c);
        } else if (line.equals("MENU")) {
            // 両者をメニューに戻す
            clients.get(0).send("SHOW_MENU");
            clients.get(1).send("SHOW_WAIT");
        } else if (line.equals("EXIT")) {
            // 切断。サーバー自体は続行
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
        // 妨害セルを設定
        board.setBlockedCell(r, c);
        // もう一人のプレイヤーへ通知
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
        String msg = sb.toString();
        broadcast(msg);
    }

    private void sendTurnInfo() {
        if (gameMode == GameMode.BLOCK) {
        }
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
        broadcastBoard(); // 盤面はここでどの分岐でも送ってOK

        if (board.hasAnyValidMove(nextPlayer)) {
            // ====== 相手が置ける場合だけブロック選択 ======
            currentPlayer = nextPlayer;
            if (gameMode == GameMode.BLOCK) {
                for (ClientHandler ch : clients) {
                    if (ch.player == player) {
                        ch.send("SELECT_BLOCK");
                    }
                }
            } else {
                sendTurnInfo();
            }
        } else if (board.hasAnyValidMove(currentPlayer)) {
            // ====== 相手がパス（自分が連続で打てる）======
            // ブロック選択画面を出さない
            sendTurnInfo(); // currentPlayerは変えない
        } else {
            // ====== 両者とも置けない＝ゲーム終了 ======
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

    // =======================
    // クライアント処理スレッド
    // =======================
    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int player; // 1 or 2

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

    // =======================
    // main
    // =======================
    public static void main(String[] args) throws IOException {
        new OthelloServer();
    }
}
