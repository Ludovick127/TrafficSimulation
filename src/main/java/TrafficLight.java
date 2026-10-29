import java.util.Random;

/**
 * TrafficLight for Manhattan intersections - French code de la route
 * Green -> Yellow -> Red -> Green randomly, cars must stop at red.
 */
public class TrafficLight {
    public enum State { GREEN, YELLOW, RED }

    private final double progress; // position along road 0..1 where stop line is
    private final String name; // e.g. "7th & 42nd"
    private final int roadId; // 0=outer, 1=vertical, 2=horizontal
    private State state = State.GREEN;
    private int timer = 0;
    private int currentDuration;
    private final Random rnd = new Random();

    // durations in ticks (30 FPS): green 90-180 (3-6s), yellow 30 (1s), red 90-180
    private static final int YELLOW_TICKS = 30;
    private static final int GREEN_MIN = 90, GREEN_MAX = 180;
    private static final int RED_MIN = 90, RED_MAX = 180;

    public TrafficLight(double progress, String name) { this(progress, name, 0); }
    public TrafficLight(double progress, String name, int roadId) {
        this.progress = ((progress % 1.0) + 1.0) % 1.0;
        this.name = name;
        this.roadId = roadId;
        this.currentDuration = randomGreen();
        this.timer = rnd.nextInt(60);
    }

    private int randomGreen() { return GREEN_MIN + rnd.nextInt(GREEN_MAX - GREEN_MIN + 1); }
    private int randomRed() { return RED_MIN + rnd.nextInt(RED_MAX - RED_MIN + 1); }

    public double getProgress() { return progress; }
    public String getName() { return name; }
    public int getRoadId() { return roadId; }
    public State getState() { return state; }
    public boolean isGreen() { return state == State.GREEN; }
    public boolean isYellow() { return state == State.YELLOW; }
    public boolean isRed() { return state == State.RED; }

    /** Call each tick (30 FPS) */
    public void update() {
        timer++;
        if (timer >= currentDuration) {
            timer = 0;
            switch (state) {
                case GREEN:
                    state = State.YELLOW;
                    currentDuration = YELLOW_TICKS;
                    break;
                case YELLOW:
                    state = State.RED;
                    currentDuration = randomRed();
                    break;
                case RED:
                    state = State.GREEN;
                    currentDuration = randomGreen();
                    break;
            }
        }
    }

    /** Distance ahead from car to light along its road (0..totalLength), wraps for loops, clamped for straight */
    public double distanceToLight(Car car, double totalLength) {
        if (car.getRoadId() != roadId) return Double.MAX_VALUE;
        double delta = (progress - car.getProgress() + 1.0) % 1.0;
        // For straight inner roads (not loops), delta >0.5 means light behind, not ahead
        if (car.getRoadId() != 0) {
            // inner roads are not loops in our design (straight), so only ahead if progress < light progress
            if (car.getProgress() > progress) return Double.MAX_VALUE;
            delta = progress - car.getProgress();
        }
        return delta * totalLength;
    }

    /** Should car stop? If red and within stopDistance ahead, and not yet passed */
    public boolean shouldStop(Car car, double totalLength, double stopDistance) {
        if (!isRed() && !isYellow()) return false;
        double dist = distanceToLight(car, totalLength);
        // Only if light is ahead within stopDistance, and not too close behind (passed)
        // dist 0..stopDistance means light ahead
        // Yellow: must stop if can safely (French: orange = préparer arrêt, if too close then pass)
        // We treat yellow as red for simplicity, but allow if very close (<8) to pass
        if (state == State.YELLOW && dist < 8) return false; // too close to stop, pass on yellow
        return dist > 0.5 && dist < stopDistance;
    }

    @Override public String toString() { return name + "@" + String.format("%.2f", progress) + " " + state; }
}
