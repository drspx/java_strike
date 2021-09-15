package dev;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {

    public static final int speed = 3;

    public boolean isDead = false;
    private String name;
    private List<Bullet> bullets = new ArrayList<>();

    public static final float width = 10;
    public static final float height = 10;

    public Player(float x, float y){
        super(x, y, width, height);
    }

    public Player() {
        super((float) (Math.random() * Launcher.width), (float) (Math.random() * Launcher.width), width, height);
    }


    public void incrementPosX(float posX) {
        if (this.getPosX() < width) {
            this.setPosX(width);
            return;
        }

        if (this.getPosX() > (Launcher.width - 20)) {
            this.setPosX(Launcher.width - 20);
            return;
        }
        this.setPosX(this.getPosX()+posX);
    }

    public void incrementPosY(float posY) {
        if (this.getPosY() < height) {
            this.setPosY(height);
            return;
        }

        if (this.getPosY() > (Launcher.height - 20)) {
            this.setPosY(Launcher.height - 20);
            return;
        }
        this.setPosY(this.getPosY()+posY);
    }

}
