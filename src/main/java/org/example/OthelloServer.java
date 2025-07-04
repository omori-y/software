import javax.swing.*;
import java.io.*;
import java.net.*;

public class OthelloServer extends JFrame {
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private OthelloBoard board;
    private OthelloPanel panel;
    private boolean myTurn = true; // サーバー（黒）先手

    public OthelloServer() {
        this(6000);
    }

    public OthelloServer(int port) {
        super("Othello Server (Black)");  // JFrameタイトル設定
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        board = new OthelloBoard();
        panel = new OthelloPanel(board.board);

        // パネルをフレームにセット
        add(panel);

        pack();  // サイズ調整
        setLocationRelativeTo(null); // 中央表示
        setVisible(true);

        panel.addClickListener((row, col) -> {
            if (!myTurn) return;
            if (!board.canPlace(row, col, 1)) return; // 黒は1
            board.flip(row, col, 1);
            panel.refresh();

            sendMove(row, col);

            if (!board.hasAnyValidMove(2)) {
                myTurn = true;
                updateStatus("相手は置けません。あなたのターン(黒)");
            } else {
                myTurn = false;
                updateStatus("相手のターン(白)");
            }
        });

        new Thread(() -> startServer(port)).start();
    }

    private void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("サーバー起動。クライアントの接続待ち...");

            socket = serverSocket.accept();
            System.out.println("クライアント接続: " + socket.getInetAddress());

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            listenForMoves();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    private void listenForMoves() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                System.out.println("受信: " + line);
                if (line.startsWith("MOVE")) {
                    String[] parts = line.split(" ");
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);

                    if (board.canPlace(row, col, 2)) { // クライアントは白2
                        board.flip(row, col, 2);
                        panel.refresh();

                        if (!board.hasAnyValidMove(1)) {
                            myTurn = false;
                            updateStatus("あなたは置けません。相手のターン(白)");
                        } else {
                            myTurn = true;
                            updateStatus("あなたのターン(黒)");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("通信エラー: " + e.getMessage());
        }
    }

    private void sendMove(int row, int col) {
        if (out != null) {
            String message = "MOVE " + row + " " + col;
            out.println(message);
            System.out.println("送信: " + message);
        }
    }

    private void updateStatus(String text) {
        // 今はコンソール表示のみ。GUIにJLabelを追加すればここで更新可能。
        System.out.println("STATUS: " + text);
    }

    private void closeConnections() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(OthelloServer::new);
    }
}
