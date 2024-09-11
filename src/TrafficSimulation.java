import javax.swing.*;
import java.awt.*;

public class TrafficSimulation {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Traffic") {
            @Override
            public void paint(Graphics g) {
                Dimension d = getSize();
                Dimension m = getMaximumSize();
                boolean resize = d.width > m.width || d.height > m.height;
                d.width = Math.min(m.width, d.width);
                d.height = Math.min(m.height, d.height);
                if (resize) {
                    Point p = getLocation();
                    setVisible(false);
                    setSize(d);
                    setLocation(p);
                    setVisible(true);
                }
                super.paint(g);
            }
        };
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1080, 200);
        frame.setMaximumSize(new Dimension(2080, 200));
        frame.setContentPane(new Traffic());
        frame.setVisible(true);
    }
}
