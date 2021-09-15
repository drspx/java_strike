package dev.display;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class Keyboard extends KeyAdapter {

    public boolean up, down, left, right, escape, botSpawn, restart;

    @Override
    public void keyPressed(KeyEvent k) {
        if (k.getKeyCode() == KeyEvent.VK_W) {
            up = true;
        }
        if (k.getKeyCode() == KeyEvent.VK_S) {
            down = true;
        }
        if (k.getKeyCode() == KeyEvent.VK_A) {
            left = true;
        }
        if (k.getKeyCode() == KeyEvent.VK_D) {
            right = true;
        }
        if (k.getKeyCode() == KeyEvent.VK_ESCAPE) {
            escape = true;
        }
        if (k.getKeyCode() == KeyEvent.VK_B) {
            botSpawn = true;
        }
        if (k.getKeyCode() == KeyEvent.VK_R) {
            restart = true;
        }

    }

    @Override
    public void keyReleased(KeyEvent k) {
        if (k.getKeyCode() == KeyEvent.VK_W) {
            up = false;
        }
        if (k.getKeyCode() == KeyEvent.VK_S) {
            down = false;
        }
        if (k.getKeyCode() == KeyEvent.VK_A) {
            left = false;
        }
        if (k.getKeyCode() == KeyEvent.VK_D) {
            right = false;
        }
        if (k.getKeyCode() == KeyEvent.VK_ESCAPE) {
            escape = false;
        }
        if (k.getKeyCode() == KeyEvent.VK_R) {
            restart = false;
        }

    }
}
