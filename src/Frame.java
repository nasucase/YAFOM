import java.awt.*;
import javax.swing.JFrame;

public class Frame extends JFrame{
    HomePanel titleScreenPanel;
    Container c;
    
    public Frame() {
        initialize();
    }

    private void initialize() {
        titleScreenPanel = new HomePanel();
        setTitle("YAFOM - realase 1.0");
        setSize(1440, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setResizable(false);
        add(titleScreenPanel);
        System.out.println();
    }

    public void launch() {
        setVisible(true);
    }

    




}
