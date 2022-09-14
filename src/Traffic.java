import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


public class Traffic extends JPanel implements Runnable {

    private final List<Car> cars = new ArrayList<>();
    private boolean running = true;
    private static Thread thread;
    private final int FPS = 10;
    private final int targetTime = 1000 / FPS;

    public static final float[] FRACTIONS = new float[]{
            0f, 0.33f,
            0.34f, 0.66f,
            0.67f, 1f};
    public static final Color[] COLORS = new Color[]{
            new Color(0, 0, 0), new Color(0, 255, 0),
            new Color(0, 0, 0), new Color(0, 0, 255),
            new Color(0, 0, 0), new Color(255, 0, 0),
    };

    Traffic() {
        super(null);
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
    }

    public void addNotify() {
        super.addNotify();
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }


    public void update() {
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            int currentX = car.getX();
            if (currentX < this.getWidth()) {
                Car nextCar;
                if (i < (cars.size() - 1)) {
                    nextCar = cars.get(i + 1);
                    adjustSpeedToNextCar(car, nextCar);
                } else {
                    nextCar = getNextClosestFrontCar(car);
                    if (null != nextCar) {
                        adjustSpeedToNextCar(car, nextCar);
                    } else {
                        car.accelerateX();
                    }
                }
                if (null != nextCar) {
                    // System.out.println("Car: " + car.getId() + ", X=" + car.getX() + ", Next car:" + nextCar.getId() + ", X=" + nextCar.getX() + ", d="+(nextCar.getX()- car.getX()));
                }
            } else {
                car.setX(0);
            }
        }

    }

    public void run() {


        long startTime;
        long urdTime;
        long waitTime;

        while (running) {
            startTime = System.nanoTime();
            update();
            repaint();
            urdTime = (System.nanoTime() - startTime) / 1000000;
            waitTime = targetTime - urdTime;

            try {
                Thread.sleep(waitTime);
            } catch (Exception e) {
            }
        }
    }

    private Car getNextClosestFrontCar(Car currentCar) {
        List<Car> copyCars = new ArrayList<>(List.copyOf(cars));
        copyCars.remove(currentCar);
        Car closestFrontCar = copyCars.get(0);
        for (int i = 1; i < copyCars.size(); i++) {
            Car potentialNext = copyCars.get(i);
            if (potentialNext.getY() == currentCar.getY()) {
                int potentialClosestX = distance(currentCar.getX(), potentialNext.getX());
                int currentClosestX = distance(currentCar.getX(), closestFrontCar.getX());
                if (currentClosestX > 0
                        && potentialClosestX > 0
                        && (currentClosestX < potentialClosestX)) {
                    closestFrontCar = potentialNext;
                }
            }
        }
        return closestFrontCar;
    }

    private int distance(int x1, int x2) {
        return x2 - x1;
    }

    private void adjustSpeedToNextCar(Car currentCar, Car nextCar) {
        if (null != nextCar
                && nextCar.getY() == currentCar.getY()) {
            int distance;
            if (nextCar.getX() - currentCar.getX() > 0) {
                distance = nextCar.getX() - currentCar.getX();
            } else {
                distance = currentCar.getX() - nextCar.getX();
            }
            //System.out.println("Car " + currentCar.getId() + ", x=" + currentCar.getX() +", Next car " + nextCar.getId() +  ", x=" + nextCar.getX() +", distance =  " + distance );
            if (Math.abs(nextCar.getX() - currentCar.getX()) <= 20) {
                if (Math.abs(distance) > 5) {
                    System.out.println("Car " + currentCar.getId() + " close from " + nextCar.getId() + " slow down speed");
                    currentCar.slowX();
                } else {
                    System.out.println("Car " + currentCar.getId() + " too close from " + nextCar.getId() + " stop !");
                    currentCar.stopX();
                }
            } else {
                currentCar.accelerateX();
            }
        } else {
            currentCar.accelerateX();
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawLine(0, 40, this.getWidth(), 40);
        g.drawLine(0, 60, this.getWidth(), 60);
        g.drawLine(0, 80, this.getWidth(), 80);
        g.drawLine(0, 100, this.getWidth(), 100);


        for (Car car : cars) {
            createObject(g, car);
        }
    }

    private void createObject(Graphics g, Car car) {
        g.setColor(car.getColor());
        g.fillRect(car.getX(), car.getY(), 5, 5);
        g.drawString(String.valueOf(car.getId()), car.getX(), car.getY() - 5);
    }

    public Color blendColors(float[] fractions, Color[] colors, float progress) {
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

    public int[] getFractionIndicies(float[] fractions, float progress) {
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

    public Color blend(Color color1, Color color2, double ratio) {
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

}
