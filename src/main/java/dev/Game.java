package dev;

import dev.display.Display;
import dev.network.Client;
import dev.network.Server;
import dev.network.User;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
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
    private byte myTeam = -1;

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
            MapBuilder.buildBombDefusalMap(obstacles);
            bombState = MapBuilder.createBombSite();
        } else {
            MapBuilder.buildDeathmatchMap(obstacles, movingObstacles);
        }

        netInit(host);

        if (isServer && gameMode == GameMode.BOMB_DEFUSAL) {
            server.setBombActionListener((playerId, action) -> {
                if (action == 0) {
                    GameLogic.handlePlantRequest(playerId, server, bombState, roundOver);
                } else if (action == 1) {
                    GameLogic.handleDefuseRequest(playerId, server, bombState, roundOver);
                }
            });
        }

        display = new Display(title, width, height);
    }

    private void tick() {
        if (display.getKeyboard().escape) System.exit(0);
        if (!player.isDead) {
            if (display.getKeyboard().up) player.incrementPosY(-Player.speed);
            if (display.getKeyboard().down) player.incrementPosY(Player.speed);
            if (display.getKeyboard().left) player.incrementPosX(-Player.speed);
            if (display.getKeyboard().right) player.incrementPosX(Player.speed);
            if (display.getMouse().leftClick) {
                display.getMouse().leftClick = false;
                Bullet b = new Bullet(player, display.getMouse().point.x, display.getMouse().point.y);
                client.sendBullet(b);
            }
            if (display.getKeyboard().interact && gameMode == GameMode.BOMB_DEFUSAL) {
                display.getKeyboard().interact = false;
                User me = client.getUsers().get(client.getMyId());
                if (me != null) {
                    myTeam = me.team;
                    client.sendBombAction(myTeam == 0 ? 0 : 1);
                }
            }
        }

        for (MovingObstacle mo : movingObstacles) {
            mo.tick();
        }

        if (isServer) {
            for (int i = 1; i < 5; i++) {
                GameLogic.checkObjectCollision(server, obstacles, movingObstacles);
            }
            GameLogic.checkMovingObstaclePlayerCollision(movingObstacles, server);
            server.broadCastBullets();
            server.tick();

            tickDeathmatchTimer();
            tickBombDefusalMode();
        }

        totalTicks++;
        client.sendPosition(System.getProperty("user.name"), player.getPosX(), player.getPosY());
        client.tick();
    }

    private void tickDeathmatchTimer() {
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
                matchWinner = GameLogic.findMatchWinner(server);
            }
        }
        if (matchOver && System.currentTimeMillis() - matchOverTime > 5000) {
            server.restartGame();
            matchStarted = false;
            matchOver = false;
            remainingSeconds = MATCH_DURATION_SECONDS;
            matchWinner = "";
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
    }

    private void tickBombDefusalMode() {
        if (gameMode != GameMode.BOMB_DEFUSAL) return;

        if (!bombRoundStarted && server.getClientCount() >= 2) {
            bombRoundStarted = true;
            startNewBombRound();
        }
        if (!bombRoundStarted || roundOver) {
            if (roundOver && System.currentTimeMillis() - roundOverTime > 5000) {
                startNewBombRound();
            }
            return;
        }

        byte winner = GameLogic.tickBombDefusal(bombState, server);
        if (winner == 0) {
            endBombRound((byte) 0, "Terrorists Win!");
        } else if (winner == 1) {
            endBombRound((byte) 1, "Counter-Terrorists Win!");
        }

        server.broadcastBombState(bombState);
    }

    private void startNewBombRound() {
        roundOver = false;
        roundResultMessage = "";
        GameLogic.resetRound(server, bombState, T_SPAWN_X, T_SPAWN_Y, CT_SPAWN_X, CT_SPAWN_Y);
    }

    private void endBombRound(byte winningTeam, String message) {
        roundOver = true;
        roundOverTime = System.currentTimeMillis();
        roundResultMessage = message;
        if (winningTeam == 0) tRoundWins++;
        else ctRoundWins++;
        server.broadcastRoundResult(winningTeam, tRoundWins, ctRoundWins);
    }

    private void render() {
        BufferStrategy bs = display.getCanvas().getBufferStrategy();
        if (bs == null) {
            display.getCanvas().createBufferStrategy(3);
            return;
        }
        Graphics g = bs.getDrawGraphics();
        Graphics2D g2 = (Graphics2D) g;
        g.clearRect(0, 0, display.getCanvas().getWidth(), display.getCanvas().getHeight());

        if (gameMode == GameMode.BOMB_DEFUSAL && bombState != null) {
            GameRenderer.drawBombSite(g2, bombState, isServer, client);
        }

        GameRenderer.drawLocalPlayer(g, player, myTeam, gameMode);
        GameRenderer.drawAimLine(g, player, display.getMouse().point);
        myTeam = GameRenderer.drawRemotePlayers(g, client, player, gameMode);
        GameRenderer.drawObstacles(g, obstacles, movingObstacles);
        GameRenderer.drawBullets(g, client.getBulletQueue());
        GameRenderer.drawScoreboard(g2, client, gameMode);

        if (gameMode == GameMode.DEATHMATCH) {
            int seconds = isServer ? remainingSeconds : client.getRemainingSeconds();
            boolean over = isServer ? matchOver : client.isMatchOver();
            GameRenderer.drawTimer(g2, seconds, over, matchWinner);
        }

        if (gameMode == GameMode.BOMB_DEFUSAL) {
            GameRenderer.drawBombHUD(g2, isServer, bombState, client, myTeam,
                    tRoundWins, ctRoundWins, roundResultMessage, roundOverTime);
        }

        g.setColor(Color.BLACK);
        g.drawString("fps= " + fps, Launcher.width - 70, 10);

        bs.show();
        g.dispose();
        Toolkit.getDefaultToolkit().sync();
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
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Player getPlayer() { return player; }
    public Client getClient() { return client; }
    public Display getDisplay() { return display; }
}
