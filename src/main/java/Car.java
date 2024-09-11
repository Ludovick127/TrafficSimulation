import java.awt.*;

/**
 * The Car class represents a car in the traffic simulation.
 * It contains properties for the car's position, speed, and color,
 * and methods to manipulate the car's movement.
 */
public class Car {
    private final int id;
    private int x;
    private int y;
    private final int maxSpeed;
    private final int minSpeed = 1;
    private final int v0 = 1;
    private int v1X = v0;
    private int v1Y = v0;
    private final int acceleration = 1;
    private final Color color;

    /**
     * Constructs a Car with the specified position, maximum speed, color, and ID.
     *
     * @param x the initial x-coordinate of the car
     * @param y the initial y-coordinate of the car
     * @param maxSpeed the maximum speed of the car
     * @param color the color of the car
     * @param id the unique identifier of the car
     */
    public Car(int x, int y, int maxSpeed, Color color, int id) {
        this.x = x;
        this.y = y;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.id = id;
    }

    /**
     * Sets the x-coordinate of the car.
     *
     * @param x the new x-coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the y-coordinate of the car.
     *
     * @param y the new y-coordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Returns the unique identifier of the car.
     *
     * @return the car's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the x-coordinate of the car.
     *
     * @return the car's x-coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y-coordinate of the car.
     *
     * @return the car's y-coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Moves the car forward along the x-axis by its maximum speed.
     */
    public void forwardX() {
        x += maxSpeed;
    }

    /**
     * Moves the car forward along the y-axis by its maximum speed.
     */
    public void forwardY() {
        y += maxSpeed;
    }

    /**
     * Moves the car backward along the x-axis by its maximum speed.
     */
    public void backwardX() {
        x -= maxSpeed;
    }

    /**
     * Moves the car backward along the y-axis by its maximum speed.
     */
    public void backwardY() {
        y -= maxSpeed;
    }

    /**
     * Resets the x-axis speed to its initial value.
     */
    public void resetV1X() {
        v1X = v0;
    }

    /**
     * Resets the y-axis speed to its initial value.
     */
    public void resetV1Y() {
        v1Y = v0;
    }

    /**
     * Accelerates the car along the x-axis.
     * The car's speed increases until it reaches the maximum speed.
     */
    public void accelerateX() {
        if (v1X < maxSpeed) {
            x += v1X;
            v1X += acceleration;
        } else {
            x += maxSpeed;
        }
    }

    /**
     * Slows down the car along the x-axis.
     * The car's speed decreases until it reaches the minimum speed.
     */
    public void slowX() {
        if (v1X > minSpeed) {
            v1X -= 2 * acceleration;
            x += v1X;
        } else {
            x += minSpeed;
        }
    }

    /**
     * Stops the car along the x-axis.
     */
    public void stopX() {
        x += minSpeed;
    }

    /**
     * Moves the car backward along the x-axis with acceleration.
     */
    public void negateAccelerateX() {
        if (v1X < maxSpeed) {
            x -= v1X;
            v1X += acceleration;
        } else {
            x -= maxSpeed;
        }
    }

    /**
     * Accelerates the car along the y-axis.
     * The car's speed increases until it reaches the maximum speed.
     */
    public void accelerateY() {
        if (v1Y < maxSpeed) {
            y += v1Y;
            v1Y += acceleration;
        } else {
            y += maxSpeed;
        }
    }

    /**
     * Moves the car backward along the y-axis with acceleration.
     */
    public void negateAccelerateY() {
        if (v1Y < maxSpeed) {
            y -= v1Y;
            v1Y += acceleration;
        } else {
            y -= maxSpeed;
        }
    }

    /**
     * Slows down the car along the y-axis.
     */
    public void slowY() {
        if (y > 0) {
            y -= 1;
        }
    }

    /**
     * Returns the color of the car.
     *
     * @return the car's color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns a string representation of the car.
     *
     * @return a string representation of the car
     */
    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", maxSpeed=" + maxSpeed +
                ", color=" + color +
                '}';
    }
}
