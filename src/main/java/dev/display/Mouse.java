package dev.display;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Mouse extends MouseAdapter {

    public boolean leftClick, rightClick;

    public Point point = new Point();

    @Override
    public void mousePressed(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClick = true;
        }
        if (e.getButton() == MouseEvent.BUTTON2) {
            rightClick = true;
        }
        point = e.getPoint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClick = false;
        }
        if (e.getButton() == MouseEvent.BUTTON2) {
            rightClick = false;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        point = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClick = true;
        }
        if (e.getButton() == MouseEvent.BUTTON2) {
            rightClick = true;
        }
        point = e.getPoint();
    }
}
