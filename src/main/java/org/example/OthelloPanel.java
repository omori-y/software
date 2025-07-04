import javax.swing.*;
import java.io.*;
import java.net.*;

public class OthelloClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private OthelloBoard board;
    private OthelloPanel panel;
    private int myPlayer = 0; // 1か2が割り当てられる
    private boolean myTurn = false;

    public OthelloClient(String host, int port) {
        board = new OthelloBoard();
        panel = new OthelloPanel(board.board);

        JFrame frame = new JFrame("Othello Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.addClickListener((r, c) -> {
            if (!myTurn) return;
            if (!board.canPlace(r, c, myPlayer)) return;

            out.println("MOVE " + r + " " + c);
            myTurn = false;
            panel.setMyTurn(false);
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
                    System.out.println("あなたはプレイヤー " + myPlayer + " です");
                } else if (line.startsWith("BOARD")) {
                    String[] parts = line.substring(6).split(",");
                    for (int i = 0; i < parts.length; i++) {
                        board.board[i / 8][i % 8] = Integer.parseInt(parts[i]);
                    }
                    panel.refresh();
                } else if (line.equals("YOUR_TURN")) {
                    myTurn = true;
                    panel.setMyTurn(true);
                    System.out.println("あなたのターンです");
                } else if (line.equals("WAIT")) {
                    myTurn = false;
                    panel.setMyTurn(false);
                    System.out.println("相手のターンです");
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
