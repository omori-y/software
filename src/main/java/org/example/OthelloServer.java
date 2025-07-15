import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Point;

public class OthelloServer {
    private static final int PORT = 6000;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private OthelloBoard board;
    private int currentPlayer = 1; // 黒が先手

    // ブロック待ち中かどうか
    private boolean isBlockingPhase = false;
    private Point blockedCell = null;

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

    // ブロック待ち中は全員WAIT送信（石置き禁止）
    private void sendTurnInfoBlockingPhase() {
        for (ClientHandler ch : clients) {
            ch.send("WAIT");
        }
    }

    private synchronized void handleMove(int player, int row, int col) {
        if (isBlockingPhase) return; // ブロック待ち中は置けない
        if (player != currentPlayer) return; // 他プレイヤーの手は無視
        if (!board.canPlace(row, col, player)) return;

        // 石を置いた後、ブロック解除
        board.blockedCell = null;
        blockedCell = null;


        board.flip(row, col, player);
        System.out.println("Player " + player + " placed at " + row + "," + col);

        // ブロック待ち状態に移行
        isBlockingPhase = true;
        blockedCell = null;

        broadcastBoard();
        sendTurnInfoBlockingPhase(); // 全員WAIT（ブロック待ち）
    }

    private synchronized void handleBlock(int player, int row, int col) {
        if (!isBlockingPhase) return; // ブロック待ちじゃないなら無視
        if (player != currentPlayer) return; // 今のプレイヤーしかブロックできない
        if (board.board[row][col] != 0) return; // 空きマス以外ブロック不可
        board.blockedCell = new Point(row, col);  // ← board 側にも保持
blockedCell = new Point(row, col);        // ← 既存（描画用）の保持もそのまま


        blockedCell = new Point(row, col);
        System.out.println("Player " + player + " blocked " + row + "," + col);

        isBlockingPhase = false;

        // 次のプレイヤーにターンを渡す
        currentPlayer = (currentPlayer == 1) ? 2 : 1;

        for (ClientHandler ch : clients) {
            ch.send("BLOCK " + row + " " + col);
        }

        broadcastBoard();
        sendTurnInfo();
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

    public static void main(String[] args) throws IOException {
        new OthelloServer();
    }
}
