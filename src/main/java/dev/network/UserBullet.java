package dev.network;

import dev.Entity;

public class UserBullet extends Entity {

    public static int width = 5;
    public static int height = width;

    private final float speed = 5f;

    private float posX;
    private float posY;
    private float vecX;
    private float vecY;
    private byte id;

    int drawn = 0;
    private boolean isObsolete;

    public UserBullet(float posX, float posY) {
        super(posX, posY, width, height);
    }

    public UserBullet addVectors(float vecX, float vecY) {
        this.vecX = vecX;
        this.vecY = vecY;
        return this;
    }

    public UserBullet addUserId(byte id) {
        this.id = id;
        return this;
    }

    public void tick() {
        setPosX(getPosX()+vecX*speed);
        setPosY(getPosY()+vecY*speed);
    }

    public void hasBeenDrawn() {
        drawn++;
    }

    public int timesDrawn() {
        return drawn;
    }

    public void setObsolete(boolean b) {
        this.isObsolete = b;
    }

    public boolean isObsolete() {
        return isObsolete;
    }

    public byte getId() {
        return id;
    }
}
