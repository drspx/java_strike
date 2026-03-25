package dev;

import dev.network.Client;
import dev.network.User;
import dev.network.UserBullet;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class GameRenderer {

    private GameRenderer() {}

    public static void drawLocalPlayer(Graphics g, Player player, byte myTeam, GameMode gameMode) {
        if (!player.isDead) {
            g.setColor(getTeamColor(myTeam, true, gameMode));
            g.fillRect((int) player.getPosX(), (int) player.getPosY(), 10, 10);
            g.setColor(Color.BLACK);
        }
    }

    public static void drawAimLine(Graphics g, Player player, Point mousePoint) {
        g.drawLine((int) (player.getPosX() + player.getWidth() / 2),
                (int) (player.getPosY() + player.getHeight() / 2),
                mousePoint.x, mousePoint.y);
    }

    public static byte drawRemotePlayers(Graphics g, Client client, Player player, GameMode gameMode) {
        byte myTeam = -1;
        for (User user : client.getUsers().values()) {
            if (user.getId() != client.getMyId()) {
                if (!user.isDead) {
                    g.setColor(getTeamColor(user.team, false, gameMode));
                    g.fillRect((int) user.getPosX(), (int) user.getPosY(),
                            (int) player.getWidth(), (int) player.getHeight());
                    g.drawString(user.getUsername(), (int) user.getPosX(), (int) user.getPosY());
                    g.setColor(Color.BLACK);
                } else {
                    drawDeadUser(g, user);
                }
            } else {
                myTeam = user.team;
                if (user.isDead) {
                    player.isDead = true;
                    drawDeadUser(g, user);
                }
            }
        }
        return myTeam;
    }

    public static void drawObstacles(Graphics g, List<Obstacle> obstacles, List<MovingObstacle> movingObstacles) {
        g.setColor(Color.DARK_GRAY);
        for (Obstacle obs : obstacles) {
            g.fillRect((int) obs.getPosX(), (int) obs.getPosY(),
                    (int) obs.getWidth(), (int) obs.getHeight());
        }

        g.setColor(new Color(180, 60, 0));
        for (MovingObstacle mo : movingObstacles) {
            g.fillRect((int) mo.getPosX(), (int) mo.getPosY(),
                    (int) mo.getWidth(), (int) mo.getHeight());
        }
        g.setColor(Color.BLACK);
    }

    public static void drawBullets(Graphics g, Collection<UserBullet> bulletQueue) {
        Iterator<UserBullet> it = bulletQueue.iterator();
        g.setColor(Color.RED);
        while (it.hasNext()) {
            UserBullet b = it.next();
            b.hasBeenDrawn();
            g.fillOval((int) b.getPosX(), (int) b.getPosY(), UserBullet.width, UserBullet.height);
            it.remove();
        }
    }

    public static void drawScoreboard(Graphics2D g2, Client client, GameMode gameMode) {
        int panelX = 10, panelY = 10;
        int rowHeight = 18;
        int headerHeight = 22;

        var users = client.getUsers().values();
        int rowCount = users.size();
        int panelWidth = 220;
        int panelHeight = headerHeight + rowHeight * Math.max(rowCount, 1) + 6;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 8, 8);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.drawString("Player", panelX + 6, panelY + 16);
        g2.drawString("K", panelX + 140, panelY + 16);
        g2.drawString("D", panelX + 175, panelY + 16);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int y = panelY + headerHeight + 14;
        for (User user : users) {
            String name = user.getUsername();
            if (name.length() > 14) name = name.substring(0, 14);
            boolean isMe = user.getId() == client.getMyId();
            if (gameMode == GameMode.BOMB_DEFUSAL) {
                g2.setColor(user.team == 0 ? new Color(220, 170, 30) :
                            user.team == 1 ? new Color(100, 160, 255) : Color.WHITE);
            } else {
                g2.setColor(isMe ? Color.YELLOW : Color.WHITE);
            }
            g2.drawString(name, panelX + 6, y);
            g2.drawString(String.valueOf(user.kills), panelX + 140, y);
            g2.drawString(String.valueOf(user.deaths), panelX + 175, y);
            y += rowHeight;
        }
    }

    public static void drawTimer(Graphics2D g2, int seconds, boolean over, String winner) {
        String timeStr = String.format("%d:%02d", seconds / 60, seconds % 60);

        g2.setFont(new Font("Monospaced", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(timeStr);
        int x = (Launcher.width - textWidth) / 2;
        int y = 30;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x - 12, y - 22, textWidth + 24, 32, 10, 10);

        g2.setColor(seconds <= 10 && !over ? Color.RED : Color.WHITE);
        g2.drawString(timeStr, x, y);

        if (over) {
            drawCenteredOverlay(g2, winner.isEmpty() ? "Match Over!" : "Winner: " + winner);
        }
    }

    public static void drawBombSite(Graphics2D g2, BombState bombState, boolean isServer, Client client) {
        float sx = bombState.getSiteX();
        float sy = bombState.getSiteY();
        float sw = bombState.getSiteW();
        float sh = bombState.getSiteH();

        g2.setColor(new Color(255, 50, 50, 40));
        g2.fillRect((int) sx, (int) sy, (int) sw, (int) sh);
        g2.setColor(new Color(255, 50, 50, 120));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect((int) sx, (int) sy, (int) sw, (int) sh);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(new Font("Monospaced", Font.BOLD, 28));
        g2.setColor(new Color(255, 80, 80, 180));
        g2.drawString("A", (int) (sx + sw / 2 - 8), (int) (sy + sh / 2 + 10));

        int bState = isServer ? bombState.getState() : client.getBombState();
        float bx = isServer ? bombState.getBombX() : client.getBombX();
        float by = isServer ? bombState.getBombY() : client.getBombY();
        if (bState >= BombState.STATE_PLANTED && bState <= BombState.STATE_DEFUSING) {
            boolean blink = (System.currentTimeMillis() / 300) % 2 == 0;
            g2.setColor(blink ? Color.RED : new Color(180, 0, 0));
            g2.fillRect((int) bx - 6, (int) by - 6, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.drawString("B", (int) bx - 4, (int) by + 4);
        }
    }

    public static void drawBombHUD(Graphics2D g2, boolean isServer, BombState bombState,
                                    Client client, byte myTeam,
                                    int tRoundWins, int ctRoundWins,
                                    String roundResultMessage, long roundOverTime) {
        int bState = isServer ? (bombState != null ? bombState.getState() : 0) : client.getBombState();
        int bombTimer = isServer ? (bombState != null ? bombState.getRemainingBombTime() : -1) : client.getBombTimer();
        int plantProg = isServer ? (bombState != null ? bombState.getPlantProgress() : 0) : client.getPlantProgress();
        int defuseProg = isServer ? (bombState != null ? bombState.getDefuseProgress() : 0) : client.getDefuseProgress();
        int tw = isServer ? tRoundWins : client.getTWins();
        int ctw = isServer ? ctRoundWins : client.getCTWins();

        // Round score at top center
        String scoreStr = "T " + tw + " - " + ctw + " CT";
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(scoreStr);
        int x = (Launcher.width - textWidth) / 2;
        int y = 28;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x - 12, y - 20, textWidth + 24, 30, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString(scoreStr, x, y);

        // Team indicator
        String teamStr = myTeam == 0 ? "TERRORIST" : myTeam == 1 ? "COUNTER-TERRORIST" : "";
        if (!teamStr.isEmpty()) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.setColor(myTeam == 0 ? new Color(220, 170, 30) : new Color(50, 120, 220));
            g2.drawString(teamStr, Launcher.width / 2 - g2.getFontMetrics().stringWidth(teamStr) / 2, 52);
        }

        // Bomb timer when planted
        if (bombTimer >= 0 && (bState == BombState.STATE_PLANTED || bState == BombState.STATE_DEFUSING)) {
            String btStr = "BOMB: " + bombTimer + "s";
            g2.setFont(new Font("Monospaced", Font.BOLD, 18));
            fm = g2.getFontMetrics();
            textWidth = fm.stringWidth(btStr);
            x = (Launcher.width - textWidth) / 2;
            y = 75;
            g2.setColor(new Color(180, 0, 0, 180));
            g2.fillRoundRect(x - 10, y - 18, textWidth + 20, 26, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(btStr, x, y);
        }

        // Plant/defuse progress bar
        if (plantProg > 0 && bState == BombState.STATE_PLANTING) {
            drawProgressBar(g2, "PLANTING", plantProg, new Color(220, 170, 30));
        }
        if (defuseProg > 0 && bState == BombState.STATE_DEFUSING) {
            drawProgressBar(g2, "DEFUSING", defuseProg, new Color(50, 120, 220));
        }

        // Round result overlay
        String resultMsg = isServer ? roundResultMessage : client.getRoundResultMsg();
        long resultTime = isServer ? roundOverTime : client.getRoundResultTime();
        if (!resultMsg.isEmpty() && System.currentTimeMillis() - resultTime < 5000) {
            drawCenteredOverlay(g2, resultMsg);
        }
    }

    // --- Private helpers ---

    private static void drawDeadUser(Graphics g, User user) {
        int x = (int) user.getPosX();
        int y = (int) user.getPosY();
        int factor = 10;
        g.drawPolygon(new int[]{x - factor, x + factor, x, x - factor, x + factor},
                      new int[]{y - factor, y + factor, y, y + factor, y - factor}, 5);
    }

    private static Color getTeamColor(byte team, boolean isLocal, GameMode gameMode) {
        if (gameMode != GameMode.BOMB_DEFUSAL) return Color.BLACK;
        if (team == 0) return isLocal ? new Color(200, 150, 0) : new Color(220, 170, 30);
        if (team == 1) return isLocal ? new Color(30, 100, 200) : new Color(50, 120, 220);
        return Color.BLACK;
    }

    private static void drawProgressBar(Graphics2D g2, String label, int progress, Color color) {
        int barW = 200, barH = 20;
        int x = (Launcher.width - barW) / 2;
        int y = Launcher.height - 80;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x - 4, y - 4, barW + 8, barH + 8, 6, 6);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y, barW, barH);
        g2.setColor(color);
        g2.fillRect(x, y, barW * progress / 100, barH);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.drawString(label + " " + progress + "%", x + barW / 2 - 40, y + 15);
    }

    private static void drawCenteredOverlay(Graphics2D g2, String message) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 36));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(message);
        int x = (Launcher.width - textWidth) / 2;
        int y = Launcher.height / 2;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(x - 20, y - 35, textWidth + 40, 50, 12, 12);
        g2.setColor(Color.YELLOW);
        g2.drawString(message, x, y);
    }
}
