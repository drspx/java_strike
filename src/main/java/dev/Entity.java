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

    public float getPosY() {
        return posY;
    }

    public void setPosX(float x) {
        this.posX = x;
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
        boolean c1 = this.posX > other.getPosX();
        boolean c2 = this.posX < other.getPosX() + other.getWidth();
        boolean c3 = this.posY > other.getPosY();
        boolean c4 = this.posY < other.getPosY() + other.getHeight();
        boolean collision = c1 && c2 && c3 && c4;
        if (collision){
            System.out.println("collision");
        }
        return collision;
    }

    public boolean isOutOfFrame() {
        return posX < 0 || posX > Launcher.width || posY < 0 || posY > Launcher.height;
    }
}
