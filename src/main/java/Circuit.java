import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Common interface for all circuits (Silverstone, Manhattan, etc.)
 */
public interface Circuit {
    String getName();
    double getTotalLengthMeters();
    List<Point2D.Double> getCenterScreenPoints(int panelWidth, int panelHeight);
    List<Point2D.Double> getLaneScreenPoints(int lane, int panelWidth, int panelHeight);
    double getTotalLengthScreen(int lane, int panelWidth, int panelHeight);
    Point2D.Double getPosition(int lane, double progress, int panelWidth, int panelHeight);
    double getAngle(int lane, double progress, int panelWidth, int panelHeight);
    void draw(Graphics2D g2, int panelWidth, int panelHeight);
    String getInfo(int totalCars);
    int getLaneCount();
}
