package dev;

import java.util.List;

public final class MapBuilder {

    private MapBuilder() {}

    public static void buildDeathmatchMap(List<Obstacle> obstacles, List<MovingObstacle> movingObstacles) {
        int W = Launcher.width;
        int H = Launcher.height;

        // --- Static obstacles ---
        // Centre vertical wall with a gap in the middle
        obstacles.add(new Obstacle(W / 2f - 5, 0,              15, H / 3f));
        obstacles.add(new Obstacle(W / 2f - 5, H * 2 / 3f,    15, H / 3f));

        // Left side horizontal cover
        obstacles.add(new Obstacle(W * 0.15f, H * 0.3f,  120, 15));
        obstacles.add(new Obstacle(W * 0.15f, H * 0.65f, 120, 15));

        // Right side horizontal cover
        obstacles.add(new Obstacle(W * 0.72f, H * 0.3f,  120, 15));
        obstacles.add(new Obstacle(W * 0.72f, H * 0.65f, 120, 15));

        // Corner boxes
        obstacles.add(new Obstacle(W * 0.05f, H * 0.05f, 50, 50));
        obstacles.add(new Obstacle(W * 0.87f, H * 0.05f, 50, 50));
        obstacles.add(new Obstacle(W * 0.05f, H * 0.87f, 50, 50));
        obstacles.add(new Obstacle(W * 0.87f, H * 0.87f, 50, 50));

        // --- Moving obstacles ---
        // Horizontal patrol across the centre gap
        movingObstacles.add(new MovingObstacle(
                W / 2f - 30, H * 0.45f, 60, 15,
                1.5f, 0,
                W * 0.3f, W * 0.7f, 0, H));

        // Vertical patrol on the left lane
        movingObstacles.add(new MovingObstacle(
                W * 0.35f, H * 0.2f, 15, 60,
                0, 1.2f,
                0, W, H * 0.1f, H * 0.9f));

        // Vertical patrol on the right lane
        movingObstacles.add(new MovingObstacle(
                W * 0.62f, H * 0.55f, 15, 60,
                0, -1.2f,
                0, W, H * 0.1f, H * 0.9f));
    }

    public static void buildBombDefusalMap(List<Obstacle> obstacles) {
        int W = Launcher.width;
        int H = Launcher.height;

        // T spawn area walls (left side enclosure)
        obstacles.add(new Obstacle(0, H * 0.3f, 100, 15));
        obstacles.add(new Obstacle(0, H * 0.65f, 100, 15));

        // CT spawn area walls (right side)
        obstacles.add(new Obstacle(W - 100, H * 0.25f, 100, 15));
        obstacles.add(new Obstacle(W - 100, H * 0.7f, 100, 15));

        // Mid corridor walls
        obstacles.add(new Obstacle(W * 0.35f, 0, 15, H * 0.4f));
        obstacles.add(new Obstacle(W * 0.35f, H * 0.55f, 15, H * 0.45f));

        obstacles.add(new Obstacle(W * 0.6f, 0, 15, H * 0.35f));
        obstacles.add(new Obstacle(W * 0.6f, H * 0.6f, 15, H * 0.4f));

        // Upper connector / A site approach
        obstacles.add(new Obstacle(W * 0.45f, H * 0.15f, 150, 15));
        obstacles.add(new Obstacle(W * 0.5f, H * 0.05f, 15, 80));

        // Lower connector
        obstacles.add(new Obstacle(W * 0.42f, H * 0.78f, 150, 15));

        // Cover boxes near bomb site
        obstacles.add(new Obstacle(W * 0.72f, H * 0.12f, 40, 40));
        obstacles.add(new Obstacle(W * 0.82f, H * 0.08f, 30, 30));

        // Cover boxes in mid
        obstacles.add(new Obstacle(W * 0.47f, H * 0.45f, 35, 35));
        obstacles.add(new Obstacle(W * 0.25f, H * 0.5f, 40, 25));

        // Lower cover
        obstacles.add(new Obstacle(W * 0.2f, H * 0.8f, 50, 30));
        obstacles.add(new Obstacle(W * 0.7f, H * 0.82f, 45, 30));
    }

    public static BombState createBombSite() {
        int W = Launcher.width;
        int H = Launcher.height;
        return new BombState(W * 0.7f, H * 0.02f, W * 0.2f, H * 0.25f);
    }
}
