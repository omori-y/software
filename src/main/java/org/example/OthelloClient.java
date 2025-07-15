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
    private JLabel statusLabel;

    private int myPlayer = 0;
    private boolean myTurn = false;
    private boolean waitingForBlock = false;

    public OthelloClient(String host, int port) {
        board = new OthelloBoard();
        panel = new OthelloPanel(board.board);

        JFrame frame = new JFrame("Othello Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);

        statusLabel = new JLabel("接続中...", SwingConstants.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.addClickListener((r, c) -> {
            if (waitingForBlock) {
                // ブロックマスをサーバーに送信
                out.println("BLOCK " + r + " " + c);
                panel.setBlockedCell(r, c);
                waitingForBlock = false;
                panel.setBlockingMode(false);
                statusLabel.setText("相手のターンです（ブロック完了）");
                panel.setMyTurn(false);
                return;
            }

            if (!myTurn) return;
            if (!board.canPlace(r, c, myPlayer)) return;

            // 自分の石を置く
            out.println("MOVE " + r + " " + c);
            myTurn = false;
            // 石を置いたらブロック待ちに移行
            waitingForBlock = true;
            panel.setBlockingMode(true);
            panel.setMyTurn(true);
            statusLabel.setText("ブロックしたいマスをクリックしてください");
            
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
                    waitingForBlock = false;  // 念のためリセット
                    panel.setMyTurn(true);
                    panel.setBlockingMode(false);
                    statusLabel.setText("あなたのターンです");
                } else if (line.equals("WAIT")) {
                    myTurn = false;
                    panel.setMyTurn(false);
                    statusLabel.setText("相手のターンです");
                } else if (line.startsWith("BLOCK")) {
                    // 相手が置いたブロック位置を受け取って表示
                    String[] parts = line.split(" ");
                    int r = Integer.parseInt(parts[1]);
                    int c = Integer.parseInt(parts[2]);
                    panel.setBlockedCell(r, c);
                    panel.setBlockingMode(false);
                    statusLabel.setText("相手がブロックしました。あなたのターンをお待ちください。");
                } else if (line.startsWith("GAME_OVER")) {
                    String[] parts = line.split(" ");
                    String result = (parts.length > 1) ? parts[1] : "";
                    int blackCount = (parts.length > 2) ? Integer.parseInt(parts[2]) : -1;
                    int whiteCount = (parts.length > 3) ? Integer.parseInt(parts[3]) : -1;

                    String message;
                    switch (result) {
                        case "BLACK":
                            message = "黒の勝利です！\n黒:" + blackCount + "コマ, 白:" + whiteCount + "コマ";
                            break;
                        case "WHITE":
                            message = "白の勝利です！\n黒:" + blackCount + "コマ, 白:" + whiteCount + "コマ";
                            break;
                        case "DRAW":
                            message = "引き分けです。\n黒:" + blackCount + "コマ, 白:" + whiteCount + "コマ";
                            break;
                        default:
                            message = "ゲーム終了！";
                    }
                    JOptionPane.showMessageDialog(panel, message);
                    System.exit(0);
                } else if (line.startsWith("CLEAR_BLOCK")) {
                    String[] parts = line.split(" ");
                    int blockerPlayer = (parts.length > 1) ? Integer.parseInt(parts[1]) : -1;

                    panel.setBlockedCell(-1, -1);

                    if (blockerPlayer == myPlayer) {
                        panel.setBlockingMode(true); 
                        waitingForBlock = true;
                        panel.setMyTurn(true);
                        statusLabel.setText("ブロックしたいマスをクリックしてください");
                    } else {
                        panel.setBlockingMode(false);
                        waitingForBlock = false;
                        panel.setMyTurn(false);
                        statusLabel.setText("相手がブロック中です");
                    }

                    panel.refresh();
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
