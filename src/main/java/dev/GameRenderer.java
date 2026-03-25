package dev;

import dev.network.Client;
import dev.network.User;
import dev.network.UserBullet;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class GameRenderer {

    // Player visual constants
    private static final int BODY_RADIUS = 8;
    private static final int GUN_LENGTH = 14;
    // Pre-cached colors — avoids per-frame allocations
    private static final Color BG_COLOR = new Color(30, 32, 36);
    private static final Color GRID_COLOR = new Color(45, 48, 54);
    private static final Color PLAYER_GREEN = new Color(50, 180, 80);
    private static final Color PLAYER_GRAY = new Color(60, 60, 65);
    private static final Color PLAYER_T_LOCAL = new Color(200, 150, 0);
    private static final Color PLAYER_T_REMOTE = new Color(220, 170, 30);
    private static final Color PLAYER_CT_LOCAL = new Color(30, 100, 200);
    private static final Color PLAYER_CT_REMOTE = new Color(50, 120, 220);
    private static final Color PLAYER_OUTLINE = new Color(200, 200, 200, 180);
    private static final Color PLAYER_CENTER_DOT = new Color(255, 255, 255, 200);
    private static final Color GUN_COLOR = new Color(80, 80, 90);
    private static final Color GUN_TIP = new Color(60, 60, 65);
    private static final Color GUN_SHADOW = new Color(0, 0, 0, 80);
    private static final Color AIM_LINE_COLOR = new Color(255, 255, 255, 50);
    private static final Color CROSSHAIR_COLOR = new Color(255, 255, 255, 140);
    private static final Color OBS_FILL = new Color(80, 85, 95);
    private static final Color OBS_BORDER = new Color(110, 115, 125);
    private static final Color OBS_SHADOW = new Color(0, 0, 0, 40);
    private static final Color MOB_FILL = new Color(200, 60, 10);
    private static final Color MOB_BORDER = new Color(255, 100, 30);
    private static final Color MOB_GLOW = new Color(255, 60, 0, 25);
    private static final Color BULLET_GLOW = new Color(255, 100, 20, 80);
    private static final Color BULLET_CORE = new Color(255, 220, 100);
    private static final Color DEAD_GLOW = new Color(200, 0, 0, 35);
    private static final Color DEAD_X = new Color(180, 30, 30);
    private static final Color NAME_SHADOW = new Color(0, 0, 0, 160);
    private static final Color HUD_BG = new Color(0, 0, 0, 150);
    private static final Color HUD_BG_DARK = new Color(0, 0, 0, 180);
    private static final Color BOMB_SITE_FILL = new Color(255, 50, 50, 25);
    private static final Color BOMB_SITE_BORDER = new Color(255, 50, 50, 100);
    private static final Color BOMB_SITE_LABEL = new Color(255, 80, 80, 140);
    private static final Color BOMB_BODY = new Color(50, 50, 50);
    private static final Color BOMB_HUD_BG = new Color(180, 0, 0, 180);
    private static final Color SCORE_T = new Color(220, 170, 30);
    private static final Color SCORE_CT = new Color(100, 160, 255);
    private static final Color CT_HUD = new Color(50, 120, 220);
    private static final Color FPS_COLOR = new Color(150, 150, 150);
    // Pre-cached fonts
    private static final Font FONT_NAME = new Font("SansSerif", Font.BOLD, 11);
    private static final Font FONT_SCOREBOARD_HEADER = new Font("Monospaced", Font.BOLD, 13);
    private static final Font FONT_SCOREBOARD_ROW = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font FONT_TIMER = new Font("Monospaced", Font.BOLD, 24);
    private static final Font FONT_OVERLAY = new Font("Monospaced", Font.BOLD, 36);
    private static final Font FONT_BOMB_LABEL = new Font("Monospaced", Font.BOLD, 28);
    private static final Font FONT_BOMB_SMALL = new Font("Monospaced", Font.BOLD, 10);
    private static final Font FONT_BOMB_HUD = new Font("Monospaced", Font.BOLD, 20);
    private static final Font FONT_BOMB_TIMER = new Font("Monospaced", Font.BOLD, 18);
    private static final Font FONT_TEAM = new Font("Monospaced", Font.BOLD, 14);
    private static final Font FONT_PROGRESS = new Font("Monospaced", Font.BOLD, 12);
    private static final Font FONT_FPS = new Font("Monospaced", Font.PLAIN, 11);
    // Pre-cached strokes
    private static final BasicStroke STROKE_1 = new BasicStroke(1f);
    private static final BasicStroke STROKE_1_5 = new BasicStroke(1.5f);
    private static final BasicStroke STROKE_2 = new BasicStroke(2f);
    private static final BasicStroke STROKE_3_ROUND = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_GUN = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_GUN_SHADOW = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final float[] AIM_DASH = {4f, 6f};
    private static final BasicStroke STROKE_AIM = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, AIM_DASH, 0f);
    // Reusable shape objects (not thread-safe but game is single-threaded render)
    private static final Ellipse2D.Float tmpEllipse = new Ellipse2D.Float();
    private static final Line2D.Float tmpLine = new Line2D.Float();
    private GameRenderer() {
    }

    public static void enableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static void drawBackground(Graphics2D g2, int w, int h) {
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, w, h);
        g2.setColor(GRID_COLOR);
        for (int x = 0; x < w; x += 100) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 100) g2.drawLine(0, y, w, y);
    }

    // --- Player drawing ---

    public static void drawLocalPlayer(Graphics2D g2, Player player, byte myTeam,
                                       GameMode gameMode, Point mousePoint) {
        if (player.isDead) return;
        float cx = player.getPosX() + player.getWidth() / 2;
        float cy = player.getPosY() + player.getHeight() / 2;
        double angle = Math.atan2(mousePoint.y - cy, mousePoint.x - cx);
        drawPlayerFigure(g2, cx, cy, angle, getTeamColor(myTeam, true, gameMode), true);
    }

    public static void drawAimLine(Graphics2D g2, Player player, Point mousePoint) {
        if (player.isDead) return;
        float cx = player.getPosX() + player.getWidth() / 2;
        float cy = player.getPosY() + player.getHeight() / 2;

        g2.setColor(AIM_LINE_COLOR);
        g2.setStroke(STROKE_AIM);
        tmpLine.setLine(cx, cy, mousePoint.x, mousePoint.y);
        g2.draw(tmpLine);
        g2.setStroke(STROKE_1);

        // Crosshair
        g2.setColor(CROSSHAIR_COLOR);
        g2.setStroke(STROKE_1_5);
        g2.drawLine(mousePoint.x - 6, mousePoint.y, mousePoint.x + 6, mousePoint.y);
        g2.drawLine(mousePoint.x, mousePoint.y - 6, mousePoint.x, mousePoint.y + 6);
        g2.setStroke(STROKE_1);
    }

    public static byte drawRemotePlayers(Graphics2D g2, Client client, Player player, GameMode gameMode) {
        byte myTeam = -1;
        for (User user : client.getUsers().values()) {
            if (user.getId() != client.getMyId()) {
                if (!user.isDead) {
                    float cx = user.getPosX() + player.getWidth() / 2;
                    float cy = user.getPosY() + player.getHeight() / 2;
                    drawPlayerFigure(g2, cx, cy, 0, getTeamColor(user.team, false, gameMode), false);
                    // Username
                    g2.setFont(FONT_NAME);
                    String name = user.getUsername();
                    if (name.length() > 12) name = name.substring(0, 12);
                    int tw = g2.getFontMetrics().stringWidth(name);
                    g2.setColor(NAME_SHADOW);
                    g2.drawString(name, (int) cx - tw / 2 + 1, (int) cy - BODY_RADIUS - 5);
                    g2.setColor(Color.WHITE);
                    g2.drawString(name, (int) cx - tw / 2, (int) cy - BODY_RADIUS - 6);
                } else {
                    drawDeadUser(g2, user, player);
                }
            } else {
                myTeam = user.team;
                if (user.isDead) {
                    player.isDead = true;
                    drawDeadUser(g2, user, player);
                }
            }
        }
        return myTeam;
    }

    private static void drawPlayerFigure(Graphics2D g2, float cx, float cy,
                                         double aimAngle, Color bodyColor, boolean isLocal) {
        float cosA = (float) Math.cos(aimAngle);
        float sinA = (float) Math.sin(aimAngle);

        // Gun barrel
        float gsx = cx + cosA * (BODY_RADIUS - 2);
        float gsy = cy + sinA * (BODY_RADIUS - 2);
        float gex = cx + cosA * (BODY_RADIUS + GUN_LENGTH);
        float gey = cy + sinA * (BODY_RADIUS + GUN_LENGTH);

        tmpLine.setLine(gsx, gsy, gex, gey);
        g2.setColor(GUN_SHADOW);
        g2.setStroke(STROKE_GUN_SHADOW);
        g2.draw(tmpLine);
        g2.setColor(GUN_COLOR);
        g2.setStroke(STROKE_GUN);
        g2.draw(tmpLine);
        g2.setStroke(STROKE_1);

        // Body
        tmpEllipse.setFrame(cx - BODY_RADIUS, cy - BODY_RADIUS, BODY_RADIUS * 2, BODY_RADIUS * 2);
        g2.setColor(bodyColor);
        g2.fill(tmpEllipse);

        // Outline
        g2.setColor(isLocal ? Color.WHITE : PLAYER_OUTLINE);
        g2.setStroke(STROKE_1_5);
        g2.draw(tmpEllipse);
        g2.setStroke(STROKE_1);

        // Local player dot
        if (isLocal) {
            g2.setColor(PLAYER_CENTER_DOT);
            tmpEllipse.setFrame(cx - 2, cy - 2, 4, 4);
            g2.fill(tmpEllipse);
        }
    }

    // --- Obstacles ---

    public static void drawObstacles(Graphics2D g2, List<Obstacle> obstacles,
                                     List<MovingObstacle> movingObstacles) {
        for (Obstacle obs : obstacles) {
            int ox = (int) obs.getPosX(), oy = (int) obs.getPosY();
            int ow = (int) obs.getWidth(), oh = (int) obs.getHeight();

            g2.setColor(OBS_SHADOW);
            g2.fillRoundRect(ox + 2, oy + 2, ow, oh, 4, 4);
            g2.setColor(OBS_FILL);
            g2.fillRoundRect(ox, oy, ow, oh, 4, 4);
            g2.setColor(OBS_BORDER);
            g2.drawRoundRect(ox, oy, ow, oh, 4, 4);
        }

        for (MovingObstacle mo : movingObstacles) {
            int mx = (int) mo.getPosX(), my = (int) mo.getPosY();
            int mw = (int) mo.getWidth(), mh = (int) mo.getHeight();

            g2.setColor(MOB_GLOW);
            g2.fillRoundRect(mx - 2, my - 2, mw + 4, mh + 4, 6, 6);
            g2.setColor(MOB_FILL);
            g2.fillRoundRect(mx, my, mw, mh, 4, 4);
            g2.setColor(MOB_BORDER);
            g2.drawRoundRect(mx, my, mw, mh, 4, 4);
        }
    }

    // --- Bullets ---

    public static void drawBullets(Graphics2D g2, Collection<UserBullet> bulletQueue) {
        Iterator<UserBullet> it = bulletQueue.iterator();
        g2.setColor(BULLET_CORE);
        while (it.hasNext()) {
            UserBullet b = it.next();
            b.hasBeenDrawn();
            g2.fillOval((int) b.getPosX(), (int) b.getPosY(), 5, 5);
            it.remove();
        }
    }

    // --- Dead player ---

    private static void drawDeadUser(Graphics2D g2, User user, Player player) {
        float cx = user.getPosX() + player.getWidth() / 2;
        float cy = user.getPosY() + player.getHeight() / 2;
        int s = 8;

        g2.setColor(DEAD_GLOW);
        tmpEllipse.setFrame(cx - s - 3, cy - s - 3, (s + 3) * 2, (s + 3) * 2);
        g2.fill(tmpEllipse);

        g2.setColor(DEAD_X);
        g2.setStroke(STROKE_3_ROUND);
        tmpLine.setLine(cx - s, cy - s, cx + s, cy + s);
        g2.draw(tmpLine);
        tmpLine.setLine(cx + s, cy - s, cx - s, cy + s);
        g2.draw(tmpLine);
        g2.setStroke(STROKE_1);
    }

    // --- HUD elements ---

    public static void drawScoreboard(Graphics2D g2, Client client, GameMode gameMode) {
        int panelX = 10, panelY = 10, rowHeight = 18, headerHeight = 22;
        var users = client.getUsers().values();
        int panelHeight = headerHeight + rowHeight * Math.max(users.size(), 1) + 6;

        g2.setColor(HUD_BG);
        g2.fillRoundRect(panelX, panelY, 220, panelHeight, 8, 8);

        g2.setColor(Color.WHITE);
        g2.setFont(FONT_SCOREBOARD_HEADER);
        g2.drawString("Player", panelX + 6, panelY + 16);
        g2.drawString("K", panelX + 140, panelY + 16);
        g2.drawString("D", panelX + 175, panelY + 16);

        g2.setFont(FONT_SCOREBOARD_ROW);
        int y = panelY + headerHeight + 14;
        for (User user : users) {
            String name = user.getUsername();
            if (name.length() > 14) name = name.substring(0, 14);
            boolean isMe = user.getId() == client.getMyId();
            if (gameMode == GameMode.BOMB_DEFUSAL) {
                g2.setColor(user.team == 0 ? SCORE_T : user.team == 1 ? SCORE_CT : Color.WHITE);
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
        g2.setFont(FONT_TIMER);
        int textWidth = g2.getFontMetrics().stringWidth(timeStr);
        int x = (Launcher.width - textWidth) / 2, y = 30;

        g2.setColor(HUD_BG);
        g2.fillRoundRect(x - 12, y - 22, textWidth + 24, 32, 10, 10);
        g2.setColor(seconds <= 10 && !over ? Color.RED : Color.WHITE);
        g2.drawString(timeStr, x, y);

        if (over) {
            drawCenteredOverlay(g2, winner.isEmpty() ? "Match Over!" : "Winner: " + winner);
        }
    }

    public static void drawBombSite(Graphics2D g2, BombState bombState, boolean isServer, Client client) {
        int sx = (int) bombState.getSiteX(), sy = (int) bombState.getSiteY();
        int sw = (int) bombState.getSiteW(), sh = (int) bombState.getSiteH();

        g2.setColor(BOMB_SITE_FILL);
        g2.fillRect(sx, sy, sw, sh);
        g2.setColor(BOMB_SITE_BORDER);
        g2.setStroke(STROKE_2);
        g2.drawRect(sx, sy, sw, sh);
        g2.setStroke(STROKE_1);

        g2.setFont(FONT_BOMB_LABEL);
        g2.setColor(BOMB_SITE_LABEL);
        g2.drawString("A", sx + sw / 2 - 8, sy + sh / 2 + 10);

        int bState = isServer ? bombState.getState() : client.getBombState();
        float bx = isServer ? bombState.getBombX() : client.getBombX();
        float by = isServer ? bombState.getBombY() : client.getBombY();
        if (bState >= BombState.STATE_PLANTED && bState <= BombState.STATE_DEFUSING) {
            boolean blink = (System.currentTimeMillis() / 300) % 2 == 0;
            g2.setColor(blink ? Color.RED : BOMB_HUD_BG);
            tmpEllipse.setFrame(bx - 7, by - 7, 14, 14);
            g2.fill(tmpEllipse);
            g2.setColor(Color.WHITE);
            g2.setFont(FONT_BOMB_SMALL);
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

        String scoreStr = "T " + tw + " - " + ctw + " CT";
        g2.setFont(FONT_BOMB_HUD);
        int textWidth = g2.getFontMetrics().stringWidth(scoreStr);
        int x = (Launcher.width - textWidth) / 2, y = 28;

        g2.setColor(HUD_BG);
        g2.fillRoundRect(x - 12, y - 20, textWidth + 24, 30, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString(scoreStr, x, y);

        String teamStr = myTeam == 0 ? "TERRORIST" : myTeam == 1 ? "COUNTER-TERRORIST" : "";
        if (!teamStr.isEmpty()) {
            g2.setFont(FONT_TEAM);
            g2.setColor(myTeam == 0 ? SCORE_T : CT_HUD);
            g2.drawString(teamStr, Launcher.width / 2 - g2.getFontMetrics().stringWidth(teamStr) / 2, 52);
        }

        if (bombTimer >= 0 && (bState == BombState.STATE_PLANTED || bState == BombState.STATE_DEFUSING)) {
            String btStr = "BOMB: " + bombTimer + "s";
            g2.setFont(FONT_BOMB_TIMER);
            textWidth = g2.getFontMetrics().stringWidth(btStr);
            x = (Launcher.width - textWidth) / 2;
            y = 75;
            g2.setColor(BOMB_HUD_BG);
            g2.fillRoundRect(x - 10, y - 18, textWidth + 20, 26, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(btStr, x, y);
        }

        if (plantProg > 0 && bState == BombState.STATE_PLANTING) {
            drawProgressBar(g2, "PLANTING", plantProg, SCORE_T);
        }
        if (defuseProg > 0 && bState == BombState.STATE_DEFUSING) {
            drawProgressBar(g2, "DEFUSING", defuseProg, CT_HUD);
        }

        String resultMsg = isServer ? roundResultMessage : client.getRoundResultMsg();
        long resultTime = isServer ? roundOverTime : client.getRoundResultTime();
        if (!resultMsg.isEmpty() && System.currentTimeMillis() - resultTime < 5000) {
            drawCenteredOverlay(g2, resultMsg);
        }
    }

    public static void drawFPS(Graphics2D g2, int fps) {
        g2.setFont(FONT_FPS);
        g2.setColor(FPS_COLOR);
        g2.drawString("FPS " + fps, Launcher.width - 60, Launcher.height - 10);
    }

    // --- Private helpers ---

    private static Color getTeamColor(byte team, boolean isLocal, GameMode gameMode) {
        if (gameMode != GameMode.BOMB_DEFUSAL) return isLocal ? PLAYER_GREEN : PLAYER_GRAY;
        if (team == 0) return isLocal ? PLAYER_T_LOCAL : PLAYER_T_REMOTE;
        if (team == 1) return isLocal ? PLAYER_CT_LOCAL : PLAYER_CT_REMOTE;
        return isLocal ? PLAYER_GREEN : PLAYER_GRAY;
    }

    private static void drawProgressBar(Graphics2D g2, String label, int progress, Color color) {
        int barW = 200, barH = 20;
        int x = (Launcher.width - barW) / 2, y = Launcher.height - 80;

        g2.setColor(HUD_BG);
        g2.fillRoundRect(x - 4, y - 4, barW + 8, barH + 8, 6, 6);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y, barW, barH);
        g2.setColor(color);
        g2.fillRect(x, y, barW * progress / 100, barH);
        g2.setColor(Color.WHITE);
        g2.setFont(FONT_PROGRESS);
        g2.drawString(label + " " + progress + "%", x + barW / 2 - 40, y + 15);
    }

    private static void drawCenteredOverlay(Graphics2D g2, String message) {
        g2.setFont(FONT_OVERLAY);
        int textWidth = g2.getFontMetrics().stringWidth(message);
        int x = (Launcher.width - textWidth) / 2, y = Launcher.height / 2;

        g2.setColor(HUD_BG_DARK);
        g2.fillRoundRect(x - 20, y - 35, textWidth + 40, 50, 12, 12);
        g2.setColor(Color.YELLOW);
        g2.drawString(message, x, y);
    }
}
