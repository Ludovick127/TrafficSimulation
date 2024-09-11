import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * TrafficSimulation class to simulate traffic using a graphical interface.
 */
public class TrafficSimulation {

    public static final float[] FRACTIONS = {0f, 0.33f, 0.34f, 0.66f, 0.67f, 1f};
    public static final Color[] COLORS = {
            new Color(0, 0, 0), new Color(0, 255, 0),
            new Color(0, 0, 0), new Color(0, 0, 255),
            new Color(0, 0, 0), new Color(255, 0, 0),
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Traffic");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1080, 200);
            frame.setMaximumSize(new Dimension(2080, 200));
            List<List<Car>> cars = generateCars();
            frame.setContentPane(new Traffic(cars));
            frame.setVisible(true);
        });
    }

    /**
     * Generates a list of cars for the simulation.
     *
     * @return a list of lists of Car objects.
     */
    private static List<List<Car>> generateCars() {
        List<List<Car>> carLists = new ArrayList<>();
        generateCarsRoute1(carLists);
        generateCarsRoute2(carLists);
        return carLists;
    }

    private static void generateCarsRoute2(List<List<Car>> carLists) {
        List<Car> cars = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < 100; i += 5) {
            float progress = (float) i / 100;
            Color color = blendColors(FRACTIONS, COLORS, progress);
            cars.add(new Car(i, 70, 7, color, k));
            k++;
        }

        cars.add(new Car(200, 70, 4, Color.MAGENTA, k));
        carLists.add(cars);
    }

    private static void generateCarsRoute1(List<List<Car>> carLists) {
        List<Car> cars = new ArrayList<>();
        int t = 0;
        for (int i = 0; i < 30; i += 5) {
            float progress = (float) i / 100;
            Color color = blendColors(FRACTIONS, COLORS, progress);
            Car car = new Car(i, 50, 5 + t, color, t);
            System.out.println(car);
            cars.add(car);
            t++;
        }
        carLists.add(cars);
    }

    /**
     * Blends colors based on the given fractions and progress.
     *
     * @param fractions array of fractions.
     * @param colors array of colors.
     * @param progress the progress value.
     * @return the blended color.
     */
    public static Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions == null || colors == null || fractions.length != colors.length) {
            throw new IllegalArgumentException("Fractions and colors must have equal number of elements and cannot be null");
        }

        int[] indices = getFractionIndices(fractions, progress);
        float[] range = {fractions[indices[0]], fractions[indices[1]]};
        Color[] colorRange = {colors[indices[0]], colors[indices[1]]};

        float max = range[1] - range[0];
        float value = progress - range[0];
        float weight = value / max;

        return blend(colorRange[0], colorRange[1], 1f - weight);
    }

    /**
     * Blends two colors based on the given ratio.
     *
     * @param color1 the first color.
     * @param color2 the second color.
     * @param ratio the blend ratio.
     * @return the blended color.
     */
    public static Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = 1.0f - r;

        float[] rgb1 = color1.getColorComponents(null);
        float[] rgb2 = color2.getColorComponents(null);

        float red = rgb1[0] * r + rgb2[0] * ir;
        float green = rgb1[1] * r + rgb2[1] * ir;
        float blue = rgb1[2] * r + rgb2[2] * ir;

        return new Color(clamp(red), clamp(green), clamp(blue));
    }

    /**
     * Clamps the color component value between 0 and 1.
     *
     * @param value the color component value.
     * @return the clamped value.
     */
    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

    /**
     * Gets the fraction indices for the given progress.
     *
     * @param fractions array of fractions.
     * @param progress the progress value.
     * @return an array of two indices.
     */
    public static int[] getFractionIndices(float[] fractions, float progress) {
        int startPoint = 0;
        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            startPoint++;
        }

        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }

        return new int[]{startPoint - 1, startPoint};
    }
}
