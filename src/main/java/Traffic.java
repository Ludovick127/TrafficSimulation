import javax.swing.JPanel;
import java.awt.Graphics;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Traffic class represents a JPanel that simulates traffic with cars.
 * It implements Runnable to update the traffic simulation in a separate thread.
 */
public class Traffic extends JPanel implements Runnable {

    private static final Logger logger = LogManager.getLogger(Traffic.class);
    private static Thread thread;
    private final List<List<Car>> carLists;
    private final int FPS = 10;
    private final int targetTime = 1000 / FPS;
    private boolean running = true;

    /**
     * Constructs a Traffic panel with the specified list of car lists.
     *
     * @param carLists the list of car lists to be simulated
     */
    public Traffic(List<List<Car>> carLists) {
        super(null);
        this.carLists = carLists;
    }

    /**
     * Called by the AWT when this component is added to a container.
     * Starts the simulation thread if it is not already running.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Updates the state of all cars in the simulation.
     */
    public void update() {
        for (List<Car> cars : carLists) {
            updateCars(cars);
        }
    }

    /**
     * Updates the state of the cars in the specified list.
     *
     * @param cars the list of cars to be updated
     */
    public void updateCars(List<Car> cars) {
        IntStream.range(0, cars.size()).forEachOrdered(i -> {
            Car car = cars.get(i);
            int currentX = car.getX();
            if (currentX < this.getWidth()) {
                Car nextCar = (i < cars.size() - 1) ? cars.get(i + 1) : cars.get(0);
                adjustSpeedToNextCar(car, nextCar);
                logger.info("Car: {}, X={}, Next car: {}, X={}, d={}", car.getId(), car.getX(), nextCar.getId(), nextCar.getX(), nextCar.getX() - car.getX());
            } else {
                car.setX(0);
            }
        });
    }

    /**
     * Runs the traffic simulation, updating and repainting the panel at a fixed frame rate.
     */
    @Override
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
                Thread.sleep(Math.max(waitTime, 0));
            } catch (Exception e) {
                logger.error("Error in run method", e);
            }
        }
    }

    /**
     * Adjusts the speed of the current car based on the position of the next car.
     *
     * @param currentCar the current car
     * @param nextCar    the next car
     */
    private void adjustSpeedToNextCar(Car currentCar, Car nextCar) {
        if (nextCar.getY() == currentCar.getY()) {
            int distance = Math.abs(nextCar.getX() - currentCar.getX());
            if (distance <= 20) {
                if (distance > 5) {
                    logger.info("Car {} close ({}) from {} slow down speed", currentCar.getId(), distance, nextCar.getId());
                    currentCar.slowX();
                } else {
                    logger.info("Car {} too close ({}) from {} stop !", currentCar.getId(), distance, nextCar.getId());
                    currentCar.stopX();
                }
            } else {
                logger.info("Car {} good distance ({}) from {} accelerate !", currentCar.getId(), distance, nextCar.getId());
                currentCar.accelerateX();
            }
        } else {
            currentCar.accelerateX();
        }
    }

    /**
     * Paints the component, drawing the traffic lines and cars.
     *
     * @param g the Graphics object to protect
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawLine(0, 40, this.getWidth(), 40);
        g.drawLine(0, 60, this.getWidth(), 60);
        g.drawLine(0, 80, this.getWidth(), 80);
        g.drawLine(0, 100, this.getWidth(), 100);

        carLists.forEach(cars -> drawCars(g, cars));
    }

    /**
     * Draws the cars in the specified list.
     *
     * @param g    the Graphics object to protect
     * @param cars the list of cars to be drawn
     */
    private void drawCars(Graphics g, List<Car> cars) {
        for (Car car : cars) {
            createObject(g, car);
        }
    }

    /**
     * Draws a single car on the panel.
     *
     * @param g   the Graphics object to protect
     * @param car the car to be drawn
     */
    private void createObject(Graphics g, Car car) {
        g.setColor(car.getColor());
        g.fillRect(car.getX(), car.getY(), 5, 5);
        g.drawString(String.valueOf(car.getId()), car.getX(), car.getY() - 5);
    }
}
