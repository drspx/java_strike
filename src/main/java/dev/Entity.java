package dev;

public abstract class Entity {

    private float posX;
    private float posY;
    private float width;
    private float height;

    public Entity(float x, float y, float width, float height) {
        this.posX = x;
        this.posY = y;
        this.width = width;
        this.height = height;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float x) {
        this.posX = x;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean collides(Entity other) {
        // Full AABB overlap check (fixes bullets passing through obstacles)
        boolean overlapX = this.posX < other.getPosX() + other.getWidth()
                && this.posX + this.width > other.getPosX();
        boolean overlapY = this.posY < other.getPosY() + other.getHeight()
                && this.posY + this.height > other.getPosY();
        boolean collision = overlapX && overlapY;
        if (collision) {
            System.out.println("collision");
        }
        return collision;
    }

    public boolean isOutOfFrame() {
        return posX < 0 || posX > Launcher.width || posY < 0 || posY > Launcher.height;
    }
}
