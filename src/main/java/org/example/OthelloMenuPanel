import javax.swing.*;
import java.awt.*;

public class OthelloMenuPanel extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JPanel menuPanel;
    private final JPanel waitPanel;
    private final JComboBox<Integer> sizeCombo;

    // 状態通知のためのリスナー
    public interface MenuListener {
        void onStartGame(int boardSize);
        void onExit();
    }

    public OthelloMenuPanel(MenuListener listener) {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // ----- メニュー画面 -----
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("オセロゲーム", SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        menuPanel.add(Box.createVerticalStrut(20));
        menuPanel.add(titleLabel);

        JLabel sizeLabel = new JLabel("盤面サイズを選択:");
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuPanel.add(Box.createVerticalStrut(10));
        menuPanel.add(sizeLabel);

        sizeCombo = new JComboBox<>(new Integer[]{6, 8, 10});
        sizeCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sizeCombo.setMaximumSize(new Dimension(100, 25));
        sizeCombo.setPreferredSize(new Dimension(100, 25));
        menuPanel.add(sizeCombo);

        JPanel btnPanel = new JPanel();
        JButton startBtn = new JButton("ゲーム開始");
        JButton exitBtn = new JButton("終了");

        startBtn.addActionListener(e -> {
            int size = (Integer) sizeCombo.getSelectedItem();
            listener.onStartGame(size);
        });
        exitBtn.addActionListener(e -> listener.onExit());

        btnPanel.add(startBtn);
        btnPanel.add(exitBtn);
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuPanel.add(Box.createVerticalStrut(20));
        menuPanel.add(btnPanel);

        // ----- 待機画面 -----
        waitPanel = new JPanel(new BorderLayout());
        JLabel waitLabel = new JLabel("相手がゲームを開始するまでお待ちください...", SwingConstants.CENTER);
        waitLabel.setFont(waitLabel.getFont().deriveFont(Font.PLAIN, 16f));
        waitPanel.add(waitLabel, BorderLayout.CENTER);

        // カードに追加
        cardPanel.add(menuPanel, "menu");
        cardPanel.add(waitPanel, "wait");

        setLayout(new BorderLayout());
        add(cardPanel, BorderLayout.CENTER);
    }

    // --- 表示切り替えメソッド ---
    public void showMenu() {
        cardLayout.show(cardPanel, "menu");
    }

    public void showWait() {
        cardLayout.show(cardPanel, "wait");
    }

    // 現在選択中の盤面サイズ（必要なら外部から取得用）
    public int getSelectedBoardSize() {
        return (Integer) sizeCombo.getSelectedItem();
    }
}
