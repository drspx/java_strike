package dev;

public class Bullet {

    private final float playerX;
    private final float playerY;
    private final float mouseX;
    private final float mouseY;
    private float posX;
    private float posY;

    private float speed = 1.0f;

    private final float vecX;
    private final float vecY;

    public Bullet(Player player, int mouseX, int mouseY) {
        this.playerX = player.getPosX();
        this.playerY = player.getPosY();
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.posX = playerX + (player.getWidth())/4f;
        this.posY = playerY + (player.getHeight())/4f;


        //måden det gøres på er ved at lave en vektor fra spilleren til musens position også lave den om til en enhedsvektor
        //og derefter gange den med game tick
        //først findes længden af vektoren som går fra spiller til mus
        float vectorX = (mouseX-posX);
        float vectorY = (mouseY-posY);
        vecX = (float) (vectorX / ( Math.sqrt( (vectorX*vectorX)+(vectorY*vectorY) )));
        vecY = (float) (vectorY / ( Math.sqrt( (vectorX*vectorX)+(vectorY*vectorY) )));

    }

    public float getPosX() {
        return posX;
    }

    public float getPosY() {
        return posY;
    }

    public Bullet tick() {
        posX = (posX + vecX*speed);
        posY = (posY + vecY*speed);
        return this;
    }

    public float getVecX() {
        return vecX;
    }

    public float getVecY() {
        return vecY;
    }
}
