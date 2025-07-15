import javax.swing.*;
import java.awt.*;

public class OthelloGamePanel extends JPanel {
    private JLabel statusLabel;

    public interface GameListener {
        void onReturnToMenu();
    }

    public OthelloGamePanel(OthelloBoardPanel boardPanel, GameListener listener) {
        setLayout(new BorderLayout());

        // ステータスラベル（上部）
        statusLabel = new JLabel("ゲーム開始", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);

        // 盤面（中央）
        add(boardPanel, BorderLayout.CENTER);

        // メニューボタン（下部）
        JButton backButton = new JButton("メニューに戻る");
        backButton.addActionListener(e -> listener.onReturnToMenu());
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
