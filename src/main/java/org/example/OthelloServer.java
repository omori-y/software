import java.io.*;
import java.net.*;
import java.util.*;

public class OthelloServer {
    private static final int PORT = 6000;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private OthelloBoard board;
    private int currentPlayer = 1; // 黒が先手

    public OthelloServer() throws IOException {
        board = new OthelloBoard();
        serverSocket = new ServerSocket(PORT);
        System.out.println("サーバー起動。クライアント2人の接続待ち...");

        while (clients.size() < 2) {
            Socket socket = serverSocket.accept();
            ClientHandler ch = new ClientHandler(socket, clients.size() + 1);
            clients.add(ch);
            new Thread(ch).start();
            System.out.println("クライアント" + ch.player + "接続: " + socket.getInetAddress());
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
        if (player != currentPlayer) return; // 他プレイヤーの手は無視
        if (!board.canPlace(row, col, player)) return;

        board.flip(row, col, player);
        System.out.println("Player " + player + " placed at " + row + "," + col);

        // 次のターン判定
        int nextPlayer = (currentPlayer == 1) ? 2 : 1;
        if (board.hasAnyValidMove(nextPlayer)) {
            currentPlayer = nextPlayer;
        } else if (board.hasAnyValidMove(currentPlayer)) {
            // 相手パスで自分のターン続行
            System.out.println("Player " + nextPlayer + " has no valid move, turn stays.");
        } else {
            // 両者パス＝ゲーム終了
            currentPlayer = 0;
            broadcastBoard();
            sendGameResult();
            return;
        }

        broadcastBoard();
        sendTurnInfo();
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

        for (ClientHandler ch : clients) {
            ch.send(result);
        }
        System.out.println("ゲーム終了: " + result);
    }


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
                    }
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
