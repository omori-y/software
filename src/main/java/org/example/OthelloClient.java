import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class OthelloClient {
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private OthelloBoardPanel othelloPanel;
    private OthelloMenuPanel menuPanel;
    private OthelloGamePanel gamePanel;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int myPlayer = 0;
    private boolean myTurn = false;
    private int boardSize = 8;

    public OthelloClient(String host, int port) {
        // === UIセットアップ ===
        frame = new JFrame("Othello Client");
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // --- メニュー ---
        menuPanel = new OthelloMenuPanel(new OthelloMenuPanel.MenuListener() {
            public void onStartGame(int selectedSize) {
                boardSize = selectedSize;
                sendToServer("START " + boardSize);
                menuPanel.showWait();    
            }
            public void onExit() {
                sendToServer("EXIT");
                frame.dispose();
            }
        });

        //ウィンドウを閉じたときもEXITを送る
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                sendToServer("EXIT");  // サーバーにEXITコマンド送信
            }
        });

        // --- ゲーム盤面部（ダミー8x8で作成、後でサイズ変更） ---
        othelloPanel = new OthelloBoardPanel(new int[boardSize][boardSize]);
        gamePanel = new OthelloGamePanel(othelloPanel, new OthelloGamePanel.GameListener() {
            public void onReturnToMenu() {
                sendToServer("MENU");
                menuPanel.showMenu();
                cardLayout.show(mainPanel, "menu");  
            }
        });

        mainPanel.add(menuPanel, "menu");
        mainPanel.add(gamePanel, "game");
        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        cardLayout.show(mainPanel, "menu");

        // === 通信開始 ===
        connectToServer(host, port);
        new Thread(this::listenToServer).start();
    }

    // サーバー送信
    private void sendToServer(String msg) {
        if (out != null) out.println(msg);
    }

    // サーバー接続
    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            showErrorAndExit("サーバー接続失敗: " + e.getMessage());
        }
    }

    // サーバー受信ループ
    private void listenToServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleServerMessage(line);
            }
        } catch (IOException e) {
            showErrorAndExit("通信エラー: " + e.getMessage());
        }
    }

    // サーバーからの指示に応じたUI・状態制御
    private void handleServerMessage(String line) {
        SwingUtilities.invokeLater(() -> {
            if (line.equals("SHOW_MENU")) {
                menuPanel.showMenu();
                cardLayout.show(mainPanel, "menu");
            } else if (line.equals("SHOW_WAIT")) {
                menuPanel.showWait();
                cardLayout.show(mainPanel, "menu");
            } else if (line.startsWith("START_GAME")) {
                // 盤面サイズ
                String[] parts = line.split(" ");
                boardSize = Integer.parseInt(parts[1]);
                // 新しい盤面をセット
                othelloPanel = new OthelloBoardPanel(new int[boardSize][boardSize]);
                othelloPanel.addClickListener((row, col) -> {
                    if (!myTurn) return;
                    if (!othelloPanel.canPlace(row, col, myPlayer)) return;
                    sendToServer("MOVE " + row + " " + col);
                    myTurn = false;
                    othelloPanel.setMyTurn(false);
                });
                gamePanel = new OthelloGamePanel(othelloPanel, new OthelloGamePanel.GameListener() {
                    public void onReturnToMenu() {
                        sendToServer("MENU");
                        menuPanel.showMenu();
                        cardLayout.show(mainPanel, "menu");
                    }
                });
                mainPanel.remove(gamePanel);
                mainPanel.add(gamePanel, "game");
                cardLayout.show(mainPanel, "game");
            } else if (line.startsWith("PLAYER")) {
                myPlayer = Integer.parseInt(line.split(" ")[1]);
                othelloPanel.setCurrentPlayer(myPlayer);
                gamePanel.setStatus("あなたはプレイヤー " + myPlayer);
                if (myPlayer == 1) {
                    frame.setTitle("Othello Client（ホスト）");
                } else if (myPlayer == 2) {
                    frame.setTitle("Othello Client（ゲスト）");
                }
            } else if (line.equals("HOST_EXITED")) {
                JOptionPane.showMessageDialog(frame,
                    "ホストが切断されました。\nアプリケーションを終了します。",
                    "切断",
                    JOptionPane.WARNING_MESSAGE);
                frame.dispose(); // ウィンドウを閉じる（自動終了）
                System.exit(0);
            } else if (line.equals("GUEST_EXITED")) {
                JOptionPane.showMessageDialog(frame,
                    "ゲストが切断されました。\nアプリケーションを終了します。",
                    "切断",
                    JOptionPane.WARNING_MESSAGE);
                frame.dispose(); // ウィンドウを閉じる
                System.exit(0);
            } else if (line.startsWith("BOARD")) {
                String[] parts = line.substring(6).split(",");
                for (int i = 0; i < parts.length; i++) {
                    int r = i / boardSize, c = i % boardSize;
                    othelloPanel.board[r][c] = Integer.parseInt(parts[i]);
                }
                othelloPanel.refresh();
            } else if (line.equals("YOUR_TURN")) {
                myTurn = true;
                othelloPanel.setMyTurn(true);
                gamePanel.setStatus("あなたのターンです");
            } else if (line.equals("WAIT")) {
                myTurn = false;
                othelloPanel.setMyTurn(false);
                gamePanel.setStatus("相手のターンです");
            } else if (line.startsWith("RESULT")) {
                String[] parts = line.split(" ");
                String info;
                if (parts[1].equals("BLACK")) {
                    info = "黒の勝ち！ 黒:" + parts[2] + " 白:" + parts[3];
                } else if (parts[1].equals("WHITE")) {
                    info = "白の勝ち！ 黒:" + parts[2] + " 白:" + parts[3];
                } else {
                    info = "引き分け！ 黒:" + parts[2] + " 白:" + parts[3];
                }
                gamePanel.setStatus("ゲーム終了: " + info);
                JOptionPane.showMessageDialog(othelloPanel, info);;
            }
        });
    }

    // エラー時
    private void showErrorAndExit(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "エラー", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OthelloClient("localhost", 6000));
    }
}
