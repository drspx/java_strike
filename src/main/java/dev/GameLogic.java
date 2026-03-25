package dev;

import dev.network.Server;
import dev.network.User;
import dev.network.UserBullet;

import java.util.List;

public final class GameLogic {

    private GameLogic() {}

    public static void checkObjectCollision(Server server, List<Obstacle> obstacles,
                                             List<MovingObstacle> movingObstacles) {
        List<UserBullet> bullets = server.getUserBullets();
        for (int j = 0; j < bullets.size(); j++) {
            UserBullet bullet = bullets.get(j);

            // Check if bullet hit any player
            for (User user : server.getUsers()) {
                if (user.getId() == bullet.getId()) continue;
                if (bullet.collides(user) && !user.isDead) {
                    System.out.println(server.getUserNameFromId(bullet.getId()) + " hit " + user.getUsername());
                    user.isDead = true;
                    user.deaths++;
                    byte shooterId = bullet.getId();
                    for (User shooter : server.getUsers()) {
                        if (shooter.getId() == shooterId) {
                            shooter.kills++;
                            break;
                        }
                    }
                }
            }

            bullet.tick();
            if (bullet.isOutOfFrame()) {
                bullets.remove(j--);
                continue;
            }

            // Check static obstacles
            boolean hitObstacle = false;
            for (Obstacle obs : obstacles) {
                if (obs.collides(bullet)) {
                    bullets.remove(j--);
                    hitObstacle = true;
                    break;
                }
            }
            if (hitObstacle) continue;

            // Check moving obstacles
            for (MovingObstacle mo : movingObstacles) {
                if (mo.collides(bullet)) {
                    bullets.remove(j--);
                    hitObstacle = true;
                    break;
                }
            }
            if (hitObstacle) continue;
        }
    }

    public static void checkMovingObstaclePlayerCollision(List<MovingObstacle> movingObstacles, Server server) {
        for (MovingObstacle mo : movingObstacles) {
            for (User user : server.getUsers()) {
                if (!user.isDead && mo.collides(user)) {
                    System.out.println(user.getUsername() + " was killed by a moving obstacle");
                    user.isDead = true;
                }
            }
        }
    }

    public static String findMatchWinner(Server server) {
        String winner = "";
        int maxKills = -1;
        for (User u : server.getUsers()) {
            if (u.kills > maxKills) {
                maxKills = u.kills;
                winner = u.getUsername();
            }
        }
        return winner;
    }

    public static void handlePlantRequest(byte playerId, Server server, BombState bombState, boolean roundOver) {
        if (bombState == null || roundOver) return;
        User planter = findUser(server, playerId);
        if (planter == null || planter.isDead || planter.team != 0 || !planter.hasBomb) return;
        if (bombState.isInBombSite(planter.getPosX(), planter.getPosY())) {
            bombState.startPlanting(planter.getPosX(), planter.getPosY(), playerId);
        }
    }

    public static void handleDefuseRequest(byte playerId, Server server, BombState bombState, boolean roundOver) {
        if (bombState == null || roundOver) return;
        User defuser = findUser(server, playerId);
        if (defuser == null || defuser.isDead || defuser.team != 1) return;
        float dx = defuser.getPosX() - bombState.getBombX();
        float dy = defuser.getPosY() - bombState.getBombY();
        if (Math.sqrt(dx * dx + dy * dy) < 50) {
            bombState.startDefusing(playerId);
        }
    }

    /**
     * Ticks bomb defusal logic. Returns the winning team byte (0=T, 1=CT) or -1 if no winner yet.
     */
    public static byte tickBombDefusal(BombState bombState, Server server) {
        if (bombState.tickPlanting()) {
            System.out.println("Bomb has been planted!");
        }
        if (bombState.tickDefusing()) {
            System.out.println("Bomb has been defused!");
            return 1; // CT wins
        }
        if (bombState.tickBombTimer()) {
            System.out.println("Bomb exploded!");
            return 0; // T wins
        }

        boolean anyTAlive = false, anyCTAlive = false;
        for (User u : server.getUsers()) {
            if (u.team == 0 && !u.isDead) anyTAlive = true;
            if (u.team == 1 && !u.isDead) anyCTAlive = true;
        }

        if (!anyTAlive && bombState.getState() < BombState.STATE_PLANTED) {
            return 1; // CT wins
        }
        if (!anyCTAlive && bombState.getState() < BombState.STATE_PLANTED) {
            return 0; // T wins
        }

        return -1; // no winner yet
    }

    public static void resetRound(Server server, BombState bombState,
                                    float tSpawnX, float tSpawnY,
                                    float ctSpawnX, float ctSpawnY) {
        server.assignTeams();
        server.assignBombCarrier();
        for (User u : server.getUsers()) {
            u.isDead = false;
            if (u.team == 0) {
                u.setPosX(tSpawnX + (float) (Math.random() * 60));
                u.setPosY(tSpawnY + (float) (Math.random() * 80 - 40));
            } else {
                u.setPosX(ctSpawnX + (float) (Math.random() * 60));
                u.setPosY(ctSpawnY + (float) (Math.random() * 80 - 40));
            }
        }
        byte carrierId = 0;
        for (User u : server.getUsers()) {
            if (u.hasBomb) { carrierId = u.getId(); break; }
        }
        bombState.reset(carrierId);
        server.getUserBullets().clear();
    }

    private static User findUser(Server server, byte playerId) {
        for (User u : server.getUsers()) {
            if (u.getId() == playerId) return u;
        }
        return null;
    }
}
