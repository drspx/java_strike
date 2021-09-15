package dev;

import dev.display.Display;
import dev.network.Client;
import dev.network.Server;
import dev.network.User;
import dev.network.UserBullet;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Iterator;
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

    private static Game instance = null;
    private List<Obstacle> obstacles = new ArrayList<>();

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
        //TODO gør start menu'en mere avanceret og smart
        int result = JOptionPane.showConfirmDialog(null, "Host?", "Host or Join?", JOptionPane.YES_NO_CANCEL_OPTION);
        String host = null;
        if (result == JOptionPane.NO_OPTION) {
            host = JOptionPane.showInputDialog("type ip of host");
            isServer = false;
        }
        if (result == JOptionPane.CANCEL_OPTION) System.exit(0);

        addObstacles();
        netInit(host);
        display = new Display(title, width, height);
    }

    private void addObstacles() {
        Obstacle o1 = new Obstacle((float) Launcher.width/2, (float) 0, (float) 10, Launcher.height/2);
        obstacles.add(o1);
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
        }


        if (isServer) {
            //tjekker om der er nogle der er blevet ramt.
            //samt beregner lidt flere punkter end der bliver regnet med i ticks for præcision
            for (int i = 1; i < 5; i++) {

                checkObjectCollision();

            }
            server.broadCastBullets();
            server.tick();
            if (display.getKeyboard().restart) {
                server.restartGame();
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
                    user.isDead = true;
                }
            }
            server.getUserBullets().get(j).tick();
            if (server.getUserBullets().get(j).isOutOfFrame()) {
                server.getUserBullets().remove(j--); //check if bullet is out of frame
                continue;
            }
            for (int i = 0; i < obstacles.size(); i++) {
                if (obstacles.get(i).collides(server.getUserBullets().get(j))) {
                    server.getUserBullets().remove(j--);
                    continue;
                }
            }

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

        if (!player.isDead) {
            g.fillRect((int) player.getPosX(), (int) player.getPosY(), 10, 10);
            g.setColor(Color.BLACK);
        }

        g.drawLine((int) (player.getPosX() + player.getWidth() / 2),
                (int) (player.getPosY()  + player.getHeight() / 2),
                display.getMouse().point.x, display.getMouse().point.y);

        for (User user : client.getUsers().values()) {
            if (!new Byte(user.getId()).equals(client.getMyId())) {
                if (!user.isDead) {
                    g.fillRect((int) user.getPosX(), (int) user.getPosY(), (int) player.getWidth(), (int) player.getHeight());
                    g.drawString(user.getUsername(), (int) user.getPosX(), (int) user.getPosY());
                } else {
                    drawDeadUser(user);
                }
            } else {
                if (user.isDead) {
                    player.isDead = true;
                    drawDeadUser(user);
                }

            }
        }

        for (int i = 0; i < obstacles.size(); i++) {
            g.fillRect((int) obstacles.get(i).getPosX(), (int) obstacles.get(i).getPosY(), (int)obstacles.get(i).getWidth(), (int) obstacles.get(i).getHeight());
        }


        Iterator<UserBullet> bulletIterator = client.getBulletQueue().iterator();
        g.setColor(Color.RED);
        while (bulletIterator.hasNext()) {
            UserBullet b = bulletIterator.next();
            b.hasBeenDrawn();
            g.fillOval((int) b.getPosX(), (int) b.getPosY(), UserBullet.width, UserBullet.height);
            bulletIterator.remove();

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
