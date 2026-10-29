import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TrafficSimulation with circuit selection (Silverstone / Manhattan) and configurable 2 lanes
 */
public class TrafficSimulation {

    private static final Logger logger = LogManager.getLogger(TrafficSimulation.class);

    public static final float[] FRACTIONS = {0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f};
    public static final Color[] COLORS = {
            new Color(255, 80, 80),   // rouge vif - visible sur asphalte gris
            new Color(80, 180, 255),  // bleu ciel
            new Color(80, 255, 120),  // vert lime
            new Color(255, 220, 80),  // jaune lumineux
            new Color(255, 165, 50),  // orange
            new Color(220, 80, 255),  // magenta/violet
    };

    public static void main(String[] args) {
        // Show config screen first (French as requested)
        SwingUtilities.invokeLater(() -> {
            ConfigScreen.showConfig();
        });
    }

    /**
     * Launch simulation with chosen circuit and car counts per lane (config screen)
     */
    public static void launchWithConfig(Circuit circuit, int[] carsPerLane) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Traffic - " + circuit.getName());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1280, 780);
            frame.setMinimumSize(new Dimension(900, 600));
            frame.setMaximumSize(new Dimension(1920, 1080));
            frame.setLocationRelativeTo(null);

            List<List<Car>> cars = generateCarsForCircuit(circuit, carsPerLane);
            Traffic traffic = new Traffic(cars, circuit);

            // Add config menu bar to allow changing circuit/cars without restart
            JMenuBar menuBar = new JMenuBar();
            JMenu menu = new JMenu("Configuration");
            JMenuItem changeItem = new JMenuItem("Changer de circuit / voitures...");
            changeItem.addActionListener(e -> {
                frame.dispose();
                // stop traffic thread? Traffic will be GC'd
                ConfigScreen.showConfig();
            });
            menu.add(changeItem);
            // Add info about current circuit
            JMenuItem infoItem = new JMenuItem(circuit.getInfo(cars.stream().mapToInt(List::size).sum()));
            infoItem.setEnabled(false);
            menu.add(infoItem);
            menuBar.add(menu);
            frame.setJMenuBar(menuBar);

            frame.setContentPane(traffic);
            frame.setVisible(true);

            logger.info("{} initialized: length={}m, totalCars={}, perLane={}", circuit.getName(), circuit.getTotalLengthMeters(), cars.stream().mapToInt(List::size).sum(), java.util.Arrays.toString(carsPerLane));
        });
    }

    /**
     * Generic generator for any circuit with configurable lanes
     * French code: all cars start on voie de droite (lane 0), other lanes only for overtaking
     */
    public static List<List<Car>> generateCarsForCircuit(Circuit circuit, int[] carsPerLane) {
        int laneCount = circuit.getLaneCount();
        List<List<Car>> carLists = new ArrayList<>();
        for (int i=0;i<laneCount;i++) carLists.add(new ArrayList<>());
        // Total cars = sum of per-lane choices (user sets 2 voies counts, but all start on right)
        int total = 0;
        for (int c : carsPerLane) total += Math.max(0, c);
        if (total == 0) total = 8; // default
        if (total > 30) total = 30;
        List<Car> rightLane = carLists.get(0);
        for (int i=0;i<total;i++) {
            double progress = (double)i / total;
            // slight offset to avoid exact overlap, keep all on right lane initially
            float colorProgress = (float)i / Math.max(1,total-1);
            Color color = blendColors(FRACTIONS, COLORS, colorProgress);
            // Vary speed: mix of slow/fast for overtaking demonstration
            int baseSpeed = 4 + (i % 3); // 4,5,6 cycle to create speed differences
            int maxSpeed = baseSpeed + (i % 2);
            if (maxSpeed>8) maxSpeed=8;
            if (maxSpeed<3) maxSpeed=3;
            Car car = new Car(progress, 0, maxSpeed, color, i);
            logger.info("Generated {} lane 0 (voie de droite) {}", circuit.getName(), car);
            rightLane.add(car);
        }
        // Keep magenta distinctive car on right lane as well for variety
        if (total>0 && total<20) {
            Car extra = new Car(0.53, 0, 7, new Color(255,50,230), total);
            rightLane.add(extra);
        }
        return carLists;
    }

    /**
     * Legacy Silverstone generator (kept for tests/backward compat) - now delegates
     */
    private static List<List<Car>> generateCarsSilverstone(SilverstoneCircuit circuit) {
        return generateCarsForCircuit(circuit, new int[]{8,10,8});
    }

    private static List<Car> generateLane(Circuit circuit, int lane, int count, int idOffset) {
        List<Car> cars = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            progress = (progress + (lane * 0.03)) % 1.0;
            float colorProgress = (float) i / count;
            Color color = blendColors(FRACTIONS, COLORS, colorProgress);
            int baseSpeed = 4 + lane;
            int maxSpeed = baseSpeed + (i % 3);
            if (maxSpeed > 8) maxSpeed = 8;
            if (maxSpeed < 3) maxSpeed = 3;
            Car car = new Car(progress, lane, maxSpeed, color, idOffset + i);
            logger.info("Generated lane {} {}", lane, car);
            cars.add(car);
        }
        return cars;
    }

    /**
     * Legacy generator for straight roads (kept for tests / fallback)
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
            logger.info("Generated {}", car);
            cars.add(car);
            t++;
        }
        carLists.add(cars);
    }

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

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }

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
