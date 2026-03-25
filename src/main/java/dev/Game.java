package dev;

import dev.display.Display;
import dev.network.Client;
import dev.network.Server;
import dev.network.User;
import dev.network.UserBullet;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.*;
import java.util.List;

public class Game implements Runnable {

    //Networking
    private static final int PORT = 4890;
    private Server server;
    private Client client;

    private int fps;

    private Display display;
    public int width, height;
    public String title;

    private boolean running = false;
    private Thread thread;

    private BufferStrategy bs;
    private Graphics g;

    private Player player = new Player();

    int totalTicks = 0;

    boolean isServer = true;

    // Deathmatch timer
    private boolean matchStarted = false;
    private long matchStartTime = 0;
    private static final int MATCH_DURATION_SECONDS = 60;
    private int remainingSeconds = MATCH_DURATION_SECONDS;
    private boolean matchOver = false;
    private String matchWinner = "";
    private long matchOverTime = 0;

    // Game mode
    private GameMode gameMode = GameMode.DEATHMATCH;

    // Bomb defusal state (server-side authoritative)
    private BombState bombState;
    private int tRoundWins = 0;
    private int ctRoundWins = 0;
    private boolean roundOver = false;
    private long roundOverTime = 0;
    private String roundResultMessage = "";
    private boolean bombRoundStarted = false;
    private byte myTeam = -1; // local player's team

    // Spawn areas for bomb defusal
    private static final float T_SPAWN_X = 50;
    private static final float T_SPAWN_Y = 400;
    private static final float CT_SPAWN_X = Launcher.width - 100;
    private static final float CT_SPAWN_Y = 400;

    private static Game instance = null;
    private List<Obstacle> obstacles = new ArrayList<>();
    private List<MovingObstacle> movingObstacles = new ArrayList<>();

    public Game(String title, int width, int height) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public static Game getInstance() {
        if (instance == null) {
            instance = new Game(Launcher.title, Launcher.width, Launcher.height);
        }
        return instance;
    }

    private void netInit(String hostname) {
        if (isServer) {
            server = new Server(PORT);
            client = new Client("localhost", PORT);
            server.start();
        } else {
            client = new Client(hostname, PORT);
        }
        client.start();

    }

    private void init() {
        int result = JOptionPane.showConfirmDialog(null, "Host?", "Host or Join?", JOptionPane.YES_NO_CANCEL_OPTION);
        String host = null;
        if (result == JOptionPane.NO_OPTION) {
            host = JOptionPane.showInputDialog("type ip of host");
            isServer = false;
        }
        if (result == JOptionPane.CANCEL_OPTION) System.exit(0);

        if (isServer) {
            String[] modes = {"Deathmatch", "Bomb Defusal"};
            int modeChoice = JOptionPane.showOptionDialog(null, "Select Game Mode", "Game Mode",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, modes, modes[0]);
            if (modeChoice == 1) {
                gameMode = GameMode.BOMB_DEFUSAL;
            }
        }

        if (gameMode == GameMode.BOMB_DEFUSAL) {
            addBombDefusalObstacles();
            initBombDefusal();
        } else {
            addObstacles();
        }

        netInit(host);

        if (isServer && gameMode == GameMode.BOMB_DEFUSAL) {
            server.setBombActionListener((playerId, action) -> {
                if (action == 0) { // Plant
                    handlePlantRequest(playerId);
                } else if (action == 1) { // Defuse
                    handleDefuseRequest(playerId);
                }
            });
        }

        display = new Display(title, width, height);
    }

    private void addObstacles() {
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

    private void addBombDefusalObstacles() {
        int W = Launcher.width;
        int H = Launcher.height;

        // --- Bomb Defusal Map: asymmetric with chokepoints ---

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

    private void initBombDefusal() {
        int W = Launcher.width;
        int H = Launcher.height;
        // Bomb site A — upper right area
        float siteX = W * 0.7f;
        float siteY = H * 0.02f;
        float siteW = W * 0.2f;
        float siteH = H * 0.25f;
        bombState = new BombState(siteX, siteY, siteW, siteH);
    }

    private void handlePlantRequest(byte playerId) {
        if (bombState == null || roundOver) return;
        User planter = null;
        for (User u : server.getUsers()) {
            if (u.getId() == playerId) { planter = u; break; }
        }
        if (planter == null || planter.isDead || planter.team != 0 || !planter.hasBomb) return;
        if (bombState.isInBombSite(planter.getPosX(), planter.getPosY())) {
            bombState.startPlanting(planter.getPosX(), planter.getPosY(), playerId);
        }
    }

    private void handleDefuseRequest(byte playerId) {
        if (bombState == null || roundOver) return;
        User defuser = null;
        for (User u : server.getUsers()) {
            if (u.getId() == playerId) { defuser = u; break; }
        }
        if (defuser == null || defuser.isDead || defuser.team != 1) return;
        // Must be near the bomb
        float dx = defuser.getPosX() - bombState.getBombX();
        float dy = defuser.getPosY() - bombState.getBombY();
        if (Math.sqrt(dx * dx + dy * dy) < 50) {
            bombState.startDefusing(playerId);
        }
    }

    private void tickBombDefusal() {
        if (!bombRoundStarted && server.getClientCount() >= 2) {
            bombRoundStarted = true;
            startNewBombRound();
        }
        if (!bombRoundStarted || roundOver) return;

        // Tick bomb state
        if (bombState.tickPlanting()) {
            System.out.println("Bomb has been planted!");
        }
        if (bombState.tickDefusing()) {
            System.out.println("Bomb has been defused!");
            endBombRound((byte) 1, "Counter-Terrorists Win!");
            return;
        }
        if (bombState.tickBombTimer()) {
            System.out.println("Bomb exploded!");
            endBombRound((byte) 0, "Terrorists Win!");
            return;
        }

        // Check if all Ts or all CTs are dead
        boolean anyTAlive = false, anyCTAlive = false;
        for (User u : server.getUsers()) {
            if (u.team == 0 && !u.isDead) anyTAlive = true;
            if (u.team == 1 && !u.isDead) anyCTAlive = true;
        }

        if (!anyTAlive && bombState.getState() < BombState.STATE_PLANTED) {
            endBombRound((byte) 1, "Counter-Terrorists Win!");
            return;
        }
        if (!anyCTAlive) {
            // CTs all dead, but if bomb isn't planted yet Ts still need to plant
            if (bombState.getState() >= BombState.STATE_PLANTED) {
                // Bomb planted, no CTs to defuse — Ts win when it explodes (let timer run)
            } else {
                endBombRound((byte) 0, "Terrorists Win!");
                return;
            }
        }

        // Broadcast bomb state
        server.broadcastBombState(bombState);
    }

    private void startNewBombRound() {
        roundOver = false;
        roundResultMessage = "";
        // Assign teams and bomb carrier
        server.assignTeams();
        server.assignBombCarrier();
        // Reset all players
        for (User u : server.getUsers()) {
            u.isDead = false;
            if (u.team == 0) {
                // T spawn — randomized within area
                u.setPosX(T_SPAWN_X + (float)(Math.random() * 60));
                u.setPosY(T_SPAWN_Y + (float)(Math.random() * 80 - 40));
            } else {
                // CT spawn
                u.setPosX(CT_SPAWN_X + (float)(Math.random() * 60));
                u.setPosY(CT_SPAWN_Y + (float)(Math.random() * 80 - 40));
            }
        }
        // Reset bomb — find the carrier
        byte carrierId = 0;
        for (User u : server.getUsers()) {
            if (u.hasBomb) { carrierId = u.getId(); break; }
        }
        bombState.reset(carrierId);
        // Clear bullets
        server.getUserBullets().clear();
    }

    private void endBombRound(byte winningTeam, String message) {
        roundOver = true;
        roundOverTime = System.currentTimeMillis();
        roundResultMessage = message;
        if (winningTeam == 0) tRoundWins++;
        else ctRoundWins++;
        server.broadcastRoundResult(winningTeam, tRoundWins, ctRoundWins);
    }

    private void tick() {
        if (display.getKeyboard().escape) System.exit(0);
        if (!player.isDead) {
            if (display.getKeyboard().up) player.incrementPosY(-Player.speed);
            if (display.getKeyboard().down) player.incrementPosY(Player.speed);
            if (display.getKeyboard().left) player.incrementPosX(-Player.speed);
            if (display.getKeyboard().right) player.incrementPosX(Player.speed);
            //player shoots
            if (display.getMouse().leftClick) {
                display.getMouse().leftClick = false;
                Bullet b = new Bullet(player, display.getMouse().point.x, display.getMouse().point.y);
                client.sendBullet(b);
            }
            // E key — bomb plant/defuse action
            if (display.getKeyboard().interact && gameMode == GameMode.BOMB_DEFUSAL) {
                display.getKeyboard().interact = false;
                // Determine team from client user list
                User me = client.getUsers().get(client.getMyId());
                if (me != null) {
                    myTeam = me.team;
                    if (myTeam == 0) {
                        client.sendBombAction(0); // Plant
                    } else if (myTeam == 1) {
                        client.sendBombAction(1); // Defuse
                    }
                }
            }
        }


        // Tick moving obstacles every frame (client-side deterministic)
        for (MovingObstacle mo : movingObstacles) {
            mo.tick();
        }

        if (isServer) {
            //tjekker om der er nogle der er blevet ramt.
            //samt beregner lidt flere punkter end der bliver regnet med i ticks for præcision
            for (int i = 1; i < 5; i++) {

                checkObjectCollision();

            }
            // Check if moving obstacles hit any player
            for (MovingObstacle mo : movingObstacles) {
                for (User user : server.getUsers()) {
                    if (!user.isDead && mo.collides(user)) {
                        System.out.println(user.getUsername() + " was killed by a moving obstacle");
                        user.isDead = true;
                    }
                }
            }
            server.broadCastBullets();
            server.tick();

            // Deathmatch timer logic
            if (!matchStarted && server.getClientCount() >= 2) {
                matchStarted = true;
                matchStartTime = System.currentTimeMillis();
                matchOver = false;
            }
            if (matchStarted && !matchOver) {
                long elapsed = (System.currentTimeMillis() - matchStartTime) / 1000;
                remainingSeconds = Math.max(0, MATCH_DURATION_SECONDS - (int) elapsed);
                if (remainingSeconds <= 0) {
                    matchOver = true;
                    matchOverTime = System.currentTimeMillis();
                    // Find winner (most kills)
                    matchWinner = findMatchWinner();
                }
            }
            // Auto-restart 5 seconds after match ends
            if (matchOver && System.currentTimeMillis() - matchOverTime > 5000) {
                server.restartGame();
                matchStarted = false;
                matchOver = false;
                remainingSeconds = MATCH_DURATION_SECONDS;
                matchWinner = "";
                // Reset kills/deaths for new match
                for (User u : server.getUsers()) {
                    u.kills = 0;
                    u.deaths = 0;
                }
            }
            server.broadcastTime(remainingSeconds, matchOver);

            if (display.getKeyboard().restart) {
                server.restartGame();
                matchStarted = false;
                matchOver = false;
                remainingSeconds = MATCH_DURATION_SECONDS;
                matchWinner = "";
            }

            // Bomb defusal mode logic
            if (gameMode == GameMode.BOMB_DEFUSAL) {
                tickBombDefusal();
                // Auto-restart round 5 seconds after it ends
                if (roundOver && System.currentTimeMillis() - roundOverTime > 5000) {
                    startNewBombRound();
                }
            }
        }

        totalTicks++;
        client.sendPosition(System.getProperty("user.name"), player.getPosX(), player.getPosY());
        client.tick();
    }

    private void checkObjectCollision() {
        for (int j = 0; j < server.getUserBullets().size(); j++) { //check if users are hit
            for (User user : server.getUsers()) {
                if (user.getId() == server.getUserBullets().get(j).getId()) continue;

                if (server.getUserBullets().get(j).collides(user)) {
                    System.out.println(server.getUserNameFromId(server.getUserBullets().get(j).getId()) + " hit " + user.getUsername());
                    if (!user.isDead) {
                        user.isDead = true;
                        user.deaths++;
                        // Credit the kill to the shooter
                        byte shooterId = server.getUserBullets().get(j).getId();
                        for (User shooter : server.getUsers()) {
                            if (shooter.getId() == shooterId) {
                                shooter.kills++;
                                break;
                            }
                        }
                    }
                }
            }
            server.getUserBullets().get(j).tick();
            if (server.getUserBullets().get(j).isOutOfFrame()) {
                server.getUserBullets().remove(j--); //check if bullet is out of frame
                continue;
            }
            boolean hitObstacle = false;
            for (int i = 0; i < obstacles.size(); i++) {
                if (obstacles.get(i).collides(server.getUserBullets().get(j))) {
                    server.getUserBullets().remove(j--);
                    hitObstacle = true;
                    break;
                }
            }
            if (hitObstacle) continue;
            for (int i = 0; i < movingObstacles.size(); i++) {
                if (movingObstacles.get(i).collides(server.getUserBullets().get(j))) {
                    server.getUserBullets().remove(j--);
                    hitObstacle = true;
                    break;
                }
            }
            if (hitObstacle) continue;

        }
    }

    private void render() {
        bs = display.getCanvas().getBufferStrategy();
        if (bs == null) {
            display.getCanvas().createBufferStrategy(3);
            return;
        }
        g = bs.getDrawGraphics();
        g.clearRect(0, 0, display.getCanvas().getWidth(), display.getCanvas().getHeight());

        // Draw bomb site area if bomb defusal mode
        if (gameMode == GameMode.BOMB_DEFUSAL && bombState != null) {
            renderBombSite((Graphics2D) g);
        }

        // Local player
        if (!player.isDead) {
            g.setColor(getTeamColor(myTeam, true));
            g.fillRect((int) player.getPosX(), (int) player.getPosY(), 10, 10);
            g.setColor(Color.BLACK);
        }

        g.drawLine((int) (player.getPosX() + player.getWidth() / 2),
                (int) (player.getPosY()  + player.getHeight() / 2),
                display.getMouse().point.x, display.getMouse().point.y);

        for (User user : client.getUsers().values()) {
            if (user.getId() != client.getMyId()) {
                if (!user.isDead) {
                    g.setColor(getTeamColor(user.team, false));
                    g.fillRect((int) user.getPosX(), (int) user.getPosY(), (int) player.getWidth(), (int) player.getHeight());
                    g.drawString(user.getUsername(), (int) user.getPosX(), (int) user.getPosY());
                    g.setColor(Color.BLACK);
                } else {
                    drawDeadUser(user);
                }
            } else {
                myTeam = user.team;
                if (user.isDead) {
                    player.isDead = true;
                    drawDeadUser(user);
                }
            }
        }

        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i < obstacles.size(); i++) {
            g.fillRect((int) obstacles.get(i).getPosX(), (int) obstacles.get(i).getPosY(), (int) obstacles.get(i).getWidth(), (int) obstacles.get(i).getHeight());
        }

        g.setColor(new Color(180, 60, 0)); // orange-red for moving obstacles
        for (MovingObstacle mo : movingObstacles) {
            g.fillRect((int) mo.getPosX(), (int) mo.getPosY(), (int) mo.getWidth(), (int) mo.getHeight());
        }
        g.setColor(Color.BLACK);


        Iterator<UserBullet> bulletIterator = client.getBulletQueue().iterator();
        g.setColor(Color.RED);
        while (bulletIterator.hasNext()) {
            UserBullet b = bulletIterator.next();
            b.hasBeenDrawn();
            g.fillOval((int) b.getPosX(), (int) b.getPosY(), UserBullet.width, UserBullet.height);
            bulletIterator.remove();

        }


        // --- Scoreboard ---
        renderScoreboard((Graphics2D) g);

        // --- Timer (deathmatch only) ---
        if (gameMode == GameMode.DEATHMATCH) {
            renderTimer((Graphics2D) g);
        }

        // --- Bomb Defusal HUD ---
        if (gameMode == GameMode.BOMB_DEFUSAL) {
            renderBombHUD((Graphics2D) g);
        }

        g.drawString("fps= " + fps, Launcher.width - 70, 10);

        bs.show();
        g.dispose();
        Toolkit.getDefaultToolkit().sync();
        //System.out.println(client.getUsers().keySet().size());

    }

    private void drawDeadUser(User user) {
        int x = (int) user.getPosX();
        int y = (int) user.getPosY();
        int factor = 10;
        g.drawPolygon(new int[]{x - factor, x + factor, x, x - factor, x + factor}
                , new int[]{y - factor, y + factor, y, y + factor, y - factor}, 5);
    }

    private void renderTimer(Graphics2D g2) {
        int seconds = isServer ? remainingSeconds : client.getRemainingSeconds();
        boolean over = isServer ? matchOver : client.isMatchOver();
        String timeStr = String.format("%d:%02d", seconds / 60, seconds % 60);

        g2.setFont(new Font("Monospaced", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(timeStr);
        int x = (Launcher.width - textWidth) / 2;
        int y = 30;

        // Background pill
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x - 12, y - 22, textWidth + 24, 32, 10, 10);

        // Timer text — red when < 10 seconds
        g2.setColor(seconds <= 10 && !over ? Color.RED : Color.WHITE);
        g2.drawString(timeStr, x, y);

        // Match over overlay
        if (over) {
            String winMsg = isServer ? "Winner: " + matchWinner : "Match Over!";
            g2.setFont(new Font("Monospaced", Font.BOLD, 36));
            fm = g2.getFontMetrics();
            textWidth = fm.stringWidth(winMsg);
            x = (Launcher.width - textWidth) / 2;
            y = Launcher.height / 2;

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(x - 20, y - 35, textWidth + 40, 50, 12, 12);
            g2.setColor(Color.YELLOW);
            g2.drawString(winMsg, x, y);
        }
    }

    private String findMatchWinner() {
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

    private Color getTeamColor(byte team, boolean isLocal) {
        if (gameMode != GameMode.BOMB_DEFUSAL) return Color.BLACK;
        if (team == 0) return isLocal ? new Color(200, 150, 0) : new Color(220, 170, 30); // T = gold/yellow
        if (team == 1) return isLocal ? new Color(30, 100, 200) : new Color(50, 120, 220); // CT = blue
        return Color.BLACK;
    }

    private void renderBombSite(Graphics2D g2) {
        float sx = bombState.getSiteX();
        float sy = bombState.getSiteY();
        float sw = bombState.getSiteW();
        float sh = bombState.getSiteH();

        // Bomb site highlight
        g2.setColor(new Color(255, 50, 50, 40));
        g2.fillRect((int) sx, (int) sy, (int) sw, (int) sh);
        g2.setColor(new Color(255, 50, 50, 120));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect((int) sx, (int) sy, (int) sw, (int) sh);
        g2.setStroke(new BasicStroke(1));

        // "A" label
        g2.setFont(new Font("Monospaced", Font.BOLD, 28));
        g2.setColor(new Color(255, 80, 80, 180));
        g2.drawString("A", (int) (sx + sw / 2 - 8), (int) (sy + sh / 2 + 10));

        // Render planted bomb
        int bState = isServer ? bombState.getState() : client.getBombState();
        float bx = isServer ? bombState.getBombX() : client.getBombX();
        float by = isServer ? bombState.getBombY() : client.getBombY();
        if (bState >= BombState.STATE_PLANTED && bState <= BombState.STATE_DEFUSING) {
            // Blinking bomb icon
            boolean blink = (System.currentTimeMillis() / 300) % 2 == 0;
            g2.setColor(blink ? Color.RED : new Color(180, 0, 0));
            g2.fillRect((int) bx - 6, (int) by - 6, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.drawString("B", (int) bx - 4, (int) by + 4);
        }
    }

    private void renderBombHUD(Graphics2D g2) {
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
            renderProgressBar(g2, "PLANTING", plantProg, new Color(220, 170, 30));
        }
        if (defuseProg > 0 && bState == BombState.STATE_DEFUSING) {
            renderProgressBar(g2, "DEFUSING", defuseProg, new Color(50, 120, 220));
        }

        // Round result overlay
        String resultMsg = isServer ? roundResultMessage : client.getRoundResultMsg();
        long resultTime = isServer ? roundOverTime : client.getRoundResultTime();
        if (!resultMsg.isEmpty() && System.currentTimeMillis() - resultTime < 5000) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 36));
            fm = g2.getFontMetrics();
            textWidth = fm.stringWidth(resultMsg);
            x = (Launcher.width - textWidth) / 2;
            y = Launcher.height / 2;
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(x - 20, y - 35, textWidth + 40, 50, 12, 12);
            g2.setColor(Color.YELLOW);
            g2.drawString(resultMsg, x, y);
        }
    }

    private void renderProgressBar(Graphics2D g2, String label, int progress, Color color) {
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

    private void renderScoreboard(Graphics2D g2) {
        int panelX = 10, panelY = 10;
        int rowHeight = 18;
        int headerHeight = 22;

        // Collect all players from client
        var users = client.getUsers().values();
        int rowCount = users.size();
        int panelWidth = 220;
        int panelHeight = headerHeight + rowHeight * Math.max(rowCount, 1) + 6;

        // Semi-transparent background
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 8, 8);

        // Header
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.drawString("Player", panelX + 6, panelY + 16);
        g2.drawString("K", panelX + 140, panelY + 16);
        g2.drawString("D", panelX + 175, panelY + 16);

        // Rows
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int y = panelY + headerHeight + 14;
        for (User user : users) {
            String name = user.getUsername();
            if (name.length() > 14) name = name.substring(0, 14);
            boolean isMe = user.getId() == client.getMyId();
            if (gameMode == GameMode.BOMB_DEFUSAL) {
                g2.setColor(user.team == 0 ? new Color(220, 170, 30) : user.team == 1 ? new Color(100, 160, 255) : Color.WHITE);
            } else {
                g2.setColor(isMe ? Color.YELLOW : Color.WHITE);
            }
            g2.drawString(name, panelX + 6, y);
            g2.drawString(String.valueOf(user.kills), panelX + 140, y);
            g2.drawString(String.valueOf(user.deaths), panelX + 175, y);
            y += rowHeight;
        }
    }

    public void run() {
        init();

        int tps = 60;

        long lastTime = System.nanoTime();
        final double ns = 1000000000.0 / (double) tps;
        double delta = 0;

        long fpsLastTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                render();
                delta--;
                this.fps = (int) (1000000000.0 / (double) (System.nanoTime() - fpsLastTime));
            }
            fpsLastTime = System.nanoTime();
        }
        stop();
    }

    public synchronized void start() {
        if (running)
            return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        if (!running)
            return;

        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public Client getClient() {
        return client;
    }

    public Display getDisplay() {
        return display;
    }
}
