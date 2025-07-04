import java.io.*;
import java.net.*;
import javax.swing.*;

public class OthelloClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private OthelloBoard board;
    private OthelloPanel panel;
    private boolean myTurn = false; // クライアント（白）は後手

    public OthelloClient(String host, int port) {
        board = new OthelloBoard();  // 盤面初期化
        panel = new OthelloPanel(board.board); // GUI表示

        try {
            socket = new Socket(host, port);
            System.out.println("サーバーに接続しました: " + socket.getInetAddress());

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            listenForMoves(); // サーバーからの受信開始
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForMoves() {
        new Thread(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println("受信: " + line);
                    if (line.startsWith("MOVE")) {
                        String[] parts = line.split(" ");
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);

                        // サーバー（黒）の手を反映
                        if (board.canPlace(row, col, 1)) {
                            board.flip(row, col, 1);
                            panel.refresh();
                        }
                        myTurn = true;
                    }
                }
            } catch (IOException e) {
                System.out.println("通信エラー: " + e.getMessage());
            }
        }).start();
    }

    public void sendMove(int row, int col) {
        if (!myTurn) return;

        if (board.canPlace(row, col, 2)) {
            board.flip(row, col, 2);
            panel.refresh();
            out.println("MOVE " + row + " " + col);
            System.out.println("送信: MOVE " + row + " " + col);
            myTurn = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OthelloClient("localhost", 6000));
    }
}
