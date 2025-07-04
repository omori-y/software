import javax.swing.*;
import java.io.*;
import java.net.*;
import java.awt.BorderLayout;

public class OthelloClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private OthelloBoard board;
    private OthelloPanel panel;
    private JLabel statusLabel; // ⭐ ステータス表示用

    private int myPlayer = 0; // 1か2が割り当てられる
    private boolean myTurn = false;

    public OthelloClient(String host, int port) {
        board = new OthelloBoard();
        panel = new OthelloPanel(board.board);

        // ⭐ GUIセットアップ
        JFrame frame = new JFrame("Othello Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);

        statusLabel = new JLabel("接続中...", SwingConstants.CENTER); // ⭐ 初期表示
        frame.add(statusLabel, BorderLayout.SOUTH); // ⭐ 下に追加

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.addClickListener((r, c) -> {
            if (!myTurn) return;
            if (!board.canPlace(r, c, myPlayer)) return;

            out.println("MOVE " + r + " " + c);
            myTurn = false;
            panel.setMyTurn(false);
            statusLabel.setText("相手のターンです"); // ⭐ 自分が置いたら切り替え
        });

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(this::listenServer).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("PLAYER")) {
                    myPlayer = Integer.parseInt(line.split(" ")[1]);
                    panel.setCurrentPlayer(myPlayer);
                    statusLabel.setText("あなたはプレイヤー " + myPlayer);
                } else if (line.startsWith("BOARD")) {
                    String[] parts = line.substring(6).split(",");
                    for (int i = 0; i < parts.length; i++) {
                        board.board[i / 8][i % 8] = Integer.parseInt(parts[i]);
                    }
                    panel.refresh();
                } else if (line.equals("YOUR_TURN")) {
                    myTurn = true;
                    panel.setMyTurn(true);
                    statusLabel.setText("あなたのターンです"); // ⭐ 表示更新
                } else if (line.equals("WAIT")) {
                    myTurn = false;
                    panel.setMyTurn(false);
                    statusLabel.setText("相手のターンです"); // ⭐ 表示更新
                } else if (line.equals("GAME_OVER")) {
                    JOptionPane.showMessageDialog(panel, "ゲーム終了！");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OthelloClient("localhost", 6000));
    }
}
