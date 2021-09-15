package dev;

public class Launcher {

    public static final int height = 850;
    public static final int width = (16 * height) / 9;
    public static final String title = "Java strike zero";

    public static void main(String[] args) {

        Game.getInstance().start();

    }
}
