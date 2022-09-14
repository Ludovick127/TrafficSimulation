import java.awt.*;
import java.util.UUID;

public class Car {

    public Car(int x, int y, int maxSpeed, Color color, int id) {
        this.x = x;
        this.y = y;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.id = id;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getId() {
        return id;
    }

    private int id;
    private int x = 0;
    private int y = 100;
    private final int maxSpeed;
    private final int minSpeed = 1;
    private final int v0 = 1;
    private int v1X = v0;
    private int v1Y = v0;
    private final int acceleration = 1;
    private Color color = Color.BLACK;


    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void forwardX() {
        x += maxSpeed;
    }

    public void forwardY() {
        y += maxSpeed;
    }

    public void backwardX() {
        x -= maxSpeed;
    }

    public void backwardY() {
        y = y - maxSpeed;
    }

    public void resetV1X() {
        v1X = v0;
    }

    public void resetV1Y() {
        v1Y = v0;
    }

    public void accelerateX() {
        if (v1X < maxSpeed) {
            x = x + v1X;
            v1X = v1X + acceleration;
        } else {
            x = x + maxSpeed;
        }
    }

    public void slowX() {
        if (v1X > minSpeed) {
            v1X = v1X - 2*acceleration;
            x = x + v1X;
        } else {
            x = x + minSpeed;
        }
    }

    public void stopX(){
        x = x + minSpeed;
    }

    public void negateAccelerateX() {
        if (v1X < maxSpeed) {
            x = x - v1X;
            v1X = v1X + acceleration;
        } else {
            x = x - maxSpeed;
        }
    }

    public void accelerateY() {
        if (v1Y < maxSpeed) {
            y = y + v1Y;
            v1Y = v1Y + acceleration;
        } else {
            y = y + maxSpeed;
        }
    }

    public void negateAccelerateY() {
        if (v1Y < maxSpeed) {
            y = y - v1Y;
            v1Y = v1Y + acceleration;
        } else {
            y = y - maxSpeed;
        }
    }

    public void slowY() {
        if (y > 0) {
            y -= 1;
        }
    }

    public Color getColor() {
        return color;
    }
}
