import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link Car} class.
 */
public class CarTest {

    @Test
    public void constructorSetsProperties() {
        Car car = new Car(10, 20, 5, Color.RED, 1);
        assertEquals(10, car.getX());
        assertEquals(20, car.getY());
        assertEquals(1, car.getId());
        assertEquals(Color.RED, car.getColor());
    }

    @Test
    public void forwardAndBackwardMoveByMaxSpeed() {
        Car car = new Car(0, 0, 5, Color.RED, 1);
        car.forwardX();
        assertEquals(5, car.getX());
        car.backwardX();
        assertEquals(0, car.getX());
        car.forwardY();
        assertEquals(5, car.getY());
        car.backwardY();
        assertEquals(0, car.getY());
    }

    @Test
    public void accelerateXIncreasesSpeedUpToMaxSpeed() {
        Car car = new Car(0, 0, 3, Color.RED, 1);
        car.accelerateX(); // moves by 1, speed -> 2
        assertEquals(1, car.getX());
        car.accelerateX(); // moves by 2, speed -> 3
        assertEquals(3, car.getX());
        car.accelerateX(); // speed at max, moves by 3
        assertEquals(6, car.getX());
        car.accelerateX(); // stays at max
        assertEquals(9, car.getX());
    }

    @Test
    public void slowXNeverMovesBackward() {
        Car car = new Car(0, 0, 10, Color.RED, 1);
        for (int i = 0; i < 5; i++) {
            car.accelerateX();
        }
        int previousX = car.getX();
        for (int i = 0; i < 10; i++) {
            car.slowX();
            assertTrue("Car should never move backward when slowing down", car.getX() >= previousX);
            previousX = car.getX();
        }
    }

    @Test
    public void stopXMovesByMinSpeed() {
        Car car = new Car(0, 0, 5, Color.RED, 1);
        car.stopX();
        assertEquals(1, car.getX());
    }

    @Test
    public void setPositionUpdatesCoordinates() {
        Car car = new Car(0, 0, 5, Color.RED, 1);
        car.setX(42);
        car.setY(24);
        assertEquals(42, car.getX());
        assertEquals(24, car.getY());
    }

    @Test
    public void toStringContainsIdAndPosition() {
        Car car = new Car(3, 7, 5, Color.RED, 9);
        String s = car.toString();
        assertTrue(s.contains("id=9"));
        assertTrue(s.contains("x=3"));
        assertTrue(s.contains("y=7"));
    }
}
