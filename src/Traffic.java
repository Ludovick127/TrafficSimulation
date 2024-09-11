import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class Traffic extends JPanel implements Runnable {

    private static Thread thread;
    private final List<Car> cars;
    private final int FPS = 10;
    private final int targetTime = 1000 / FPS;
    private boolean running = true;

    Traffic(List<Car> cars) {
        super(null);
        this.cars = cars;
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

}
