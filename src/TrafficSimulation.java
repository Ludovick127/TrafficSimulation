import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class TrafficSimulation {

    public static final float[] FRACTIONS = new float[]{
            0f, 0.33f,
            0.34f, 0.66f,
            0.67f, 1f};
    public static final Color[] COLORS = new Color[]{
            new Color(0, 0, 0), new Color(0, 255, 0),
            new Color(0, 0, 0), new Color(0, 0, 255),
            new Color(0, 0, 0), new Color(255, 0, 0),
    };

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
        List<Car> cars = generateCars();
        frame.setContentPane(new Traffic(cars));
        frame.setVisible(true);
    }
    private static List<Car> generateCars() {
        List<Car> cars = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < 100; i = i + 5) {
            float progress = (float) i / (float) 100;
            Color color = blendColors(FRACTIONS, COLORS, progress);
            cars.add(new Car(i, 70, 7, color, k));
            k++;
        }

        cars.add(new Car(200, 70, 4, Color.MAGENTA, k));
        int t = 0;
        for (int i = 0; i < 30; i = i + 5) {
            float progress = (float) i / (float) 100;
            Color color = blendColors(FRACTIONS, COLORS, progress);
            cars.add(new Car(i, 50, 5 + t, color, t));
            t++;
        }
        return cars;
    }
    public static Color blendColors(float[] fractions, Color[] colors, float progress) {
        Color color = null;
        if (fractions != null) {
            if (colors != null) {
                if (fractions.length == colors.length) {
                    int[] indicies = getFractionIndicies(fractions, progress);

                    float[] range = new float[]{fractions[indicies[0]], fractions[indicies[1]]};
                    Color[] colorRange = new Color[]{colors[indicies[0]], colors[indicies[1]]};

                    float max = range[1] - range[0];
                    float value = progress - range[0];
                    float weight = value / max;

                    color = blend(colorRange[0], colorRange[1], 1f - weight);
                } else {
                    throw new IllegalArgumentException("Fractions and colours must have equal number of elements");
                }
            } else {
                throw new IllegalArgumentException("Colours can't be null");
            }
        } else {
            throw new IllegalArgumentException("Fractions can't be null");
        }
        return color;
    }

    public static Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = (float) 1.0 - r;

        float[] rgb1 = new float[3];
        float[] rgb2 = new float[3];

        color1.getColorComponents(rgb1);
        color2.getColorComponents(rgb2);

        float red = rgb1[0] * r + rgb2[0] * ir;
        float green = rgb1[1] * r + rgb2[1] * ir;
        float blue = rgb1[2] * r + rgb2[2] * ir;

        if (red < 0) {
            red = 0;
        } else if (red > 255) {
            red = 255;
        }
        if (green < 0) {
            green = 0;
        } else if (green > 255) {
            green = 255;
        }
        if (blue < 0) {
            blue = 0;
        } else if (blue > 255) {
            blue = 255;
        }

        Color color = null;
        try {
            color = new Color(red, green, blue);
        } catch (IllegalArgumentException exp) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            System.out.println(nf.format(red) + "; " + nf.format(green) + "; " + nf.format(blue));
            exp.printStackTrace();
        }
        return color;
    }

    public static int[] getFractionIndicies(float[] fractions, float progress) {
        int[] range = new int[2];

        int startPoint = 0;
        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            startPoint++;
        }

        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }

        range[0] = startPoint - 1;
        range[1] = startPoint;

        return range;
    }
}
