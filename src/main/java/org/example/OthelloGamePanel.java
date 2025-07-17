import javax.swing.*;
import java.awt.*;

public class OthelloGamePanel extends JPanel {
    private OthelloBoardPanel othelloPanel;
    private JLabel statusLabel;
    private JButton backToMenuButton;

    public interface GameListener {
        void onReturnToMenu();
    }

    public OthelloGamePanel(OthelloBoardPanel othelloPanel, GameListener listener) {
        this.othelloPanel = othelloPanel;
        setLayout(new BorderLayout());

        // ステータス表示
        statusLabel = new JLabel("ゲーム中", SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH);

        // 盤面表示
        add(othelloPanel, BorderLayout.CENTER);

        // ボタンエリア
        JPanel btnPanel = new JPanel();
        backToMenuButton = new JButton("メニューに戻る");

        backToMenuButton.addActionListener(e -> listener.onReturnToMenu());

        btnPanel.add(backToMenuButton);

        add(btnPanel, BorderLayout.SOUTH);
    }

    // ステータスラベルの更新
    public void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    public OthelloBoardPanel getOthelloPanel() {
        return othelloPanel;
    }
}


