import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;

public class HomePanel extends JPanel {

    private Image backgroundImage, icon;
    private final LinkedHashMap<String, String> difficulties = new LinkedHashMap<>();
    private final JList<String> difficultyList;
    private final JButton start;
    private String difficulty;
    private SimpleAudioPlayer audioPlayer;
    
    public HomePanel() {
        initializeDifficulties();
        initializeAudio();
        loadImages();
        setPreferredSize(new Dimension(1440, 900));
        setLayout(null);
        
        // list for difficulties
        DefaultListModel<String> listModel = new DefaultListModel<>();
        difficulties.values().forEach(listModel::addElement);
        
        difficultyList = new JList<>(listModel);
        difficultyList.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        difficultyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        difficultyList.setBounds(700, 300, 600, 300);
        difficultyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedValue = difficultyList.getSelectedValue();
                difficulty = difficulties.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(selectedValue))
                    .map(entry -> entry.getKey())
                    .findFirst().orElse(null);
            }
        });
        add(difficultyList);
        
        // start button
        start = new JButton("Start");
        start.setFont(new Font("Arial", Font.BOLD, 26));
        start.setBounds(700, 650, 600, 50);
        start.addActionListener(e -> {
            if (difficulty != null) {
                try {
                audioPlayer.stop();
                } catch (Exception ex) {
                System.out.println(ex.getMessage());
                }
                switchToGame(difficulty);
                
            } else {
                JOptionPane.showMessageDialog(this, "Please select a difficulty first!");
            }
        });
        add(start);
    }

    // puts difficulties in a hashmap
    private void initializeDifficulties() {
        difficulties.put("Beginner", "Beginner: Harumachi Clover - R3 Music Box");
        difficulties.put("Easy", "Easy: Runengon - antiPLUR");
        difficulties.put("Normal", "Normal: TSLove - O2i3");
        difficulties.put("Hard", "Hard: Six trillion years and overnight story - Kemu feat. IA");
        difficulties.put("Insane", "Insane: cyanine - jioyi");
        difficulties.put("Extra1", "Extra 1: Gallaxy Collapse - Kurokotei");
        difficulties.put("Extra2", "Extra 2: Dakara boku wa ongaku o yameta - Yorushika");
    }
    
    // new audio by using audio player class
    private void initializeAudio() {
        try {
            audioPlayer = new SimpleAudioPlayer("src/music/MainMusic.wav", true);
            audioPlayer.play();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    // load main menu images
    private void loadImages() {
        try {
            icon = new ImageIcon("src/assets/icon.png").getImage();
            backgroundImage = new ImageIcon("src/assets/bg.png").getImage();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    // switch to game panel when start button is pressed
    public void switchToGame(String difficulty) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        frame.getContentPane().removeAll();
        Game game = new Game(difficulty);
        frame.getContentPane().add(game);
        frame.revalidate();
        frame.repaint();
        SwingUtilities.invokeLater(() -> game.requestFocusInWindow());
    }

    // method for render images
    private void renderImage(Graphics g, Image image, int x, int y, int width, int height) {
        Graphics2D g2D = (Graphics2D) g;
        g2D.drawImage(image, x, y, width, height, null);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        int centerX = (getWidth() - backgroundImage.getWidth(null)) / 2;
        int centerY = (getHeight() - backgroundImage.getHeight(null)) / 2;
        int iconX = 100;
        int iconY = (900 - 500) / 2;

        renderImage(g, backgroundImage, centerX, centerY, backgroundImage.getWidth(null), backgroundImage.getHeight(null));
        renderImage(g, icon, iconX, iconY, icon.getWidth(null), icon.getHeight(null));
    }
}
