package dev;

public class MovingObstacle extends Obstacle {

    private float velX;
    private float velY;
    private float minX, maxX, minY, maxY;

    public MovingObstacle(float x, float y, float width, float height,
                          float velX, float velY,
                          float minX, float maxX, float minY, float maxY) {
        super(x, y, width, height);
        this.velX = velX;
        this.velY = velY;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public void tick() {
        float nx = getPosX() + velX;
        float ny = getPosY() + velY;

        if (nx < minX || nx + getWidth() > maxX) {
            velX = -velX;
            nx = getPosX() + velX;
        }
        if (ny < minY || ny + getHeight() > maxY) {
            velY = -velY;
            ny = getPosY() + velY;
        }

        setPosX(nx);
        setPosY(ny);
    }
}
