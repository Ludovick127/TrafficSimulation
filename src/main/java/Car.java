import java.awt.*;

/**
 * The Car class represents a car in the traffic simulation.
 * It contains properties for the car's position, speed, and color,
 * and methods to manipulate the car's movement.
 * Supports both classic straight-road mode (x/y) and Silverstone circuit mode (trackProgress + lane).
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

    // Silverstone circuit fields
    private double trackProgress = 0.0; // 0..1 fraction of lap
    private int lane = -1; // -1 = classic mode, 0/1/2 = circuit lanes
    private int trackSpeed = v0; // current speed along track (pixels per tick equivalent)

    // Overtaking (code de la route: voie de droite obligatoire)
    private boolean overtaking = false;
    private Car overtakeTarget = null;
    private int leftLaneTicks = 0; // ticks spent in left lane not overtaking (for forced return)

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
        this.lane = -1;
        this.trackProgress = 0.0;
        this.trackSpeed = v0;
    }

    /**
     * Constructor for Silverstone circuit mode.
     * @param trackProgress initial progress 0..1 around lap
     * @param lane lane index 0..2
     * @param maxSpeed maximum speed
     * @param color color
     * @param id id
     */
    public Car(double trackProgress, int lane, int maxSpeed, Color color, int id) {
        this.x = 0;
        this.y = 0;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.id = id;
        this.lane = lane;
        this.trackProgress = wrapProgress(trackProgress);
        this.trackSpeed = v0;
        this.v1X = v0;
    }

    private static double wrapProgress(double p) {
        p = p % 1.0;
        if (p < 0) p += 1.0;
        return p;
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

    // ---- Circuit getters/setters ----
    public double getProgress() { return trackProgress; }
    public void setProgress(double p) { this.trackProgress = wrapProgress(p); }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
    public boolean isCircuitMode() { return lane >= 0; }
    public int getMaxSpeed() { return maxSpeed; }
    public int getTrackSpeed() { return trackSpeed; }
    public void setTrackSpeed(int s) { this.trackSpeed = Math.max(minSpeed, Math.min(maxSpeed, s)); }
    public int getV1X() { return v1X; }
    public boolean isOvertaking() { return overtaking; }
    public void setOvertaking(boolean o) { this.overtaking = o; if (!o) leftLaneTicks = 0; }
    public Car getOvertakeTarget() { return overtakeTarget; }
    public void setOvertakeTarget(Car c) { this.overtakeTarget = c; }
    public int getLeftLaneTicks() { return leftLaneTicks; }
    public void incrementLeftLaneTicks() { leftLaneTicks++; }
    public void resetLeftLaneTicks() { leftLaneTicks = 0; }

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
        trackSpeed = v0;
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
            v1X = Math.max(minSpeed, v1X - 2 * acceleration);
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

    // ---- Circuit speed control (operates on trackSpeed, not x) ----
    public void accelerateTrack() {
        if (trackSpeed < maxSpeed) {
            trackSpeed += acceleration;
            // keep v1X in sync for logging compatibility
            v1X = trackSpeed;
        }
        // movement is handled by Traffic advancing progress
    }

    public void slowTrack() {
        if (trackSpeed > minSpeed) {
            trackSpeed = Math.max(minSpeed, trackSpeed - 2 * acceleration);
            v1X = trackSpeed;
        }
    }

    public void stopTrack() {
        // keep speed at minSpeed but don't advance extra; Traffic will advance by minSpeed
        trackSpeed = minSpeed;
        v1X = minSpeed;
    }

    /**
     * Advance progress by current trackSpeed relative to lane total length.
     * @param totalLengthScreen total screen length of lane path
     */
    public void advanceTrack(double totalLengthScreen) {
        if (totalLengthScreen < 1e-6) return;
        double delta = (double) trackSpeed / totalLengthScreen;
        trackProgress = wrapProgress(trackProgress + delta);
    }

    /**
     * Advance progress by minSpeed (used when stopped but still creeping)
     */
    public void advanceTrackMin(double totalLengthScreen) {
        if (totalLengthScreen < 1e-6) return;
        double delta = (double) minSpeed / totalLengthScreen;
        trackProgress = wrapProgress(trackProgress + delta);
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
        if (isCircuitMode()) {
            return "Car{" +
                    "id=" + id +
                    ", progress=" + String.format("%.3f", trackProgress) +
                    ", lane=" + lane +
                    ", speed=" + trackSpeed +
                    ", maxSpeed=" + maxSpeed +
                    ", color=" + color +
                    '}';
        }
        return "Car{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", maxSpeed=" + maxSpeed +
                ", color=" + color +
                '}';
    }
}
