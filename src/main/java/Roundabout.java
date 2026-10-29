import java.util.List;

/**
 * Roundabout - French code: priorité aux véhicules déjà dans l'anneau.
 * Entering cars must yield (céder le passage) to circulating cars.
 * On Manhattan: placed at Columbus Circle and Times Square style corners.
 */
public class Roundabout {
    private final double centerProgress; // center of roundabout along track
    private final double radiusProgress; // half-length of roundabout interval (e.g. 0.035 = ~7% of lap)
    private final String name;
    private final double entryProgress; // yield line just before entering

    public Roundabout(double centerProgress, double radiusProgress, String name) {
        this.centerProgress = ((centerProgress % 1.0) + 1.0) % 1.0;
        this.radiusProgress = Math.abs(radiusProgress);
        this.name = name;
        // entry is just before roundabout interval
        double entryDelta = radiusProgress + 0.015; // 1.5% before
        this.entryProgress = ((centerProgress - entryDelta + 1.0) % 1.0);
    }

    public double getCenterProgress() { return centerProgress; }
    public String getName() { return name; }
    public double getEntryProgress() { return entryProgress; }

    /** Is car inside roundabout circulation? */
    public boolean isInside(Car car) {
        double delta = (car.getProgress() - centerProgress + 1.0) % 1.0;
        if (delta > 0.5) delta = 1.0 - delta; // shortest circular distance
        return delta < radiusProgress;
    }

    /** Distance from car to entry yield line */
    public double distanceToEntry(Car car, double totalLength) {
        double delta = (entryProgress - car.getProgress() + 1.0) % 1.0;
        return delta * totalLength;
    }

    /** Is car approaching entry within approachDist? */
    public boolean isApproaching(Car car, double totalLength, double approachDist) {
        double d = distanceToEntry(car, totalLength);
        return d > 0.5 && d < approachDist;
    }

    /** Check if any circulating car has priority - i.e., is inside and close to entry */
    public boolean hasCirculatingPriority(List<Car> allCars, double totalLength) {
        for (Car other : allCars) {
            if (isInside(other)) {
                // distance from other to entry (how close is circulating car to conflict point)
                // For simplicity: if any car inside, entering must yield
                // More precise: check if circulating car is within 40 ahead of entry
                double otherToEntry = (entryProgress - other.getProgress() + 1.0) % 1.0 * totalLength;
                // otherToEntry small means circulating car is near entry (about to exit or pass)
                // We'll consider priority if circulating car is within roundabout and within 60 of entry
                if (otherToEntry < 60 && otherToEntry > 0.5) return true;
                // Also if circulating car just entered, still priority
                return true;
            }
        }
        return false;
    }

    /** Should approaching car yield? */
    public boolean shouldYield(Car car, List<Car> allCars, double totalLength) {
        if (!isApproaching(car, totalLength, 22)) return false;
        // If already inside, no yield
        if (isInside(car)) return false;
        return hasCirculatingPriority(allCars, totalLength);
    }

    public double getRadiusProgress() { return radiusProgress; }
}
