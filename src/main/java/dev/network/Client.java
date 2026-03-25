package dev.network;

import dev.Bullet;
import dev.Game;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.network.NetValues.TIMEOUT;

public class Client {

    private byte myId = 0;
    private DatagramSocket udpSocket;
    private Map<Byte, User> users = new ConcurrentHashMap<Byte, User>();
    private Collection<UserBullet> bulletQueue = new ConcurrentLinkedQueue<UserBullet>();
    // Timer state received from server
    private volatile int remainingSeconds = 60;
    private volatile boolean matchOver = false;
    // Bomb defusal state received from server
    private volatile int bombState = 0;
    private volatile float bombX, bombY;
    private volatile int bombTimer = -1;
    private volatile int plantProgress = 0;
    private volatile int defuseProgress = 0;
    // Round results
    private volatile int tWins = 0;
    private volatile int ctWins = 0;
    private volatile String roundResultMsg = "";
    private volatile long roundResultTime = 0;
    public Client(String hostname, int port) {
        try {
            udpSocket = new DatagramSocket();
            udpSocket.connect(InetAddress.getByName(hostname), port);
            System.out.println("Client Connected: " + udpSocket.isConnected());
            System.out.println(udpSocket.getInetAddress() + " " + udpSocket.getPort());
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    public byte getMyId() {
        if (myId == 0) {
            askId();
            return 0;
        }
        return myId;
    }

    public void start() {
        new Thread() {
            public void run() {
                byte[] data = new byte[256];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                while (udpSocket.isConnected()) {
                    try {
                        udpSocket.receive(packet);
                        byte packetId = packet.getData()[0];
                        switch (packetId) {
                            case 1:
                                updatePlayerPosition(packet);
                                break;
                            case 2:
                                updateBullet(packet);
                                break;
                            case 3:
                                positionBullet(packet);
                                break;
                            case 4:
                                receiveId(packet);
                                break;
                            case 5:
                                restartGame(packet);
                                break;
                            case 7:
                                receiveTime(packet);
                                break;
                            case 9:
                                receiveBombState(packet);
                                break;
                            case 12:
                                receiveRoundResult(packet);
                                break;
                        }

                    } catch (IOException e) {

                    }
                }
            }
        }.start();
    }

    private void receiveTime(DatagramPacket packet) {
        byte[] data = packet.getData();
        remainingSeconds = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
        matchOver = data[3] == 1;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean isMatchOver() {
        return matchOver;
    }

    private void receiveBombState(DatagramPacket packet) {
        byte[] data = packet.getData();
        bombState = data[1] & 0xFF;
        bombX = ByteBuffer.wrap(data, 2, 4).getFloat();
        bombY = ByteBuffer.wrap(data, 6, 4).getFloat();
        bombTimer = ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);
        plantProgress = data[12] & 0xFF;
        defuseProgress = data[13] & 0xFF;
    }

    private void receiveRoundResult(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte winTeam = data[1];
        tWins = data[2] & 0xFF;
        ctWins = data[3] & 0xFF;
        roundResultMsg = winTeam == 0 ? "Terrorists Win!" : "Counter-Terrorists Win!";
        roundResultTime = System.currentTimeMillis();
    }

    public void sendBombAction(int action) {
        byte[] data = new byte[]{11, (byte) action};
        try {
            udpSocket.send(new DatagramPacket(data, data.length));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getBombState() {
        return bombState;
    }

    public float getBombX() {
        return bombX;
    }

    public float getBombY() {
        return bombY;
    }

    public int getBombTimer() {
        return bombTimer;
    }

    public int getPlantProgress() {
        return plantProgress;
    }

    public int getDefuseProgress() {
        return defuseProgress;
    }

    public int getTWins() {
        return tWins;
    }

    public int getCTWins() {
        return ctWins;
    }

    public String getRoundResultMsg() {
        return roundResultMsg;
    }

    public long getRoundResultTime() {
        return roundResultTime;
    }

    private void restartGame(DatagramPacket packet) {
        Game.getInstance().getPlayer().isDead = false;
        matchOver = false;
        remainingSeconds = 60;
    }

    private void receiveId(DatagramPacket packet) {
        myId = packet.getData()[1];
    }

    public void askId() {
        try {
            udpSocket.send(new DatagramPacket(new byte[]{4}, 1, udpSocket.getInetAddress(), udpSocket.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void positionBullet(DatagramPacket packet) {
        byte[] data = packet.getData();
        float x = ByteBuffer.wrap(data, 2, 4).getFloat();
        float y = ByteBuffer.wrap(data, 6, 4).getFloat();
        bulletQueue.add(new UserBullet(x, y).addUserId(data[1]));
    }

    public void sendBullet(Bullet b) {
        byte[] data = new byte[2 + 4 + 4 + 4 + 4];
        data[0] = 3;
        byte[] posX = ByteBuffer.allocate(4).putFloat(b.getPosX()).array();
        byte[] posY = ByteBuffer.allocate(4).putFloat(b.getPosY()).array();
        byte[] vecX = ByteBuffer.allocate(4).putFloat(b.getVecX()).array();
        byte[] vecY = ByteBuffer.allocate(4).putFloat(b.getVecY()).array();
        for (int i = 0; i < 4; i++) {
            data[i + 2] = posX[i];
            data[i + 6] = posY[i];
            data[i + 10] = vecX[i];
            data[i + 14] = vecY[i];
        }
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            udpSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void updateBullet(DatagramPacket packet) {
        byte[] data = packet.getData();
        float x = ByteBuffer.wrap(data, 2, 4).getFloat();
        float y = ByteBuffer.wrap(data, 6, 4).getFloat();
        System.out.println("client received bullet ");
        bulletQueue.add(new UserBullet(x, y));
    }

    public Collection<UserBullet> getBulletQueue() {
        return bulletQueue;
    }

    private void cleanUp() {
        for (Byte key : users.keySet()) {
            if (System.currentTimeMillis() - users.get(key).lastConfirmationOfLife() > TIMEOUT) {
                users.remove(key);
            }
        }
    }

    public void tick() {
        cleanUp();
    }

    protected void updatePlayerPosition(DatagramPacket packet) {
        byte[] data = packet.getData();
        byte id = data[1];

        float x = ByteBuffer.wrap(data, 2, 4).getFloat();
        float y = ByteBuffer.wrap(data, 6, 4).getFloat();
        boolean isDead = data[10] == 1;
        int kills = ((data[11] & 0xFF) << 8) | (data[12] & 0xFF);
        int deaths = ((data[13] & 0xFF) << 8) | (data[14] & 0xFF);
        byte team = data[15];
        int index = 16;
        String username = "";
        while (index < packet.getLength() && data[index] != 0) {
            username += (char) data[index++];
        }

        if (users.containsKey(id)) {
            User u = users.get(id);
            u.isDead = isDead;
            u.setPosX(x);
            u.setPosY(y);
            u.kills = kills;
            u.deaths = deaths;
            u.team = team;
        } else {
            User u = new User(null, username, id, x, y);
            u.kills = kills;
            u.deaths = deaths;
            u.team = team;
            users.put(id, u);
        }
    }

    public Map<Byte, User> getUsers() {
        return users;
    }

    public void sendPosition(String username, float x, float y) {
        byte[] xArray = ByteBuffer.allocate(4).putFloat(x).array();
        byte[] yArray = ByteBuffer.allocate(4).putFloat(y).array();
        byte[] nameArray = username.getBytes();
        byte[] finalData = new byte[2 + xArray.length + yArray.length + nameArray.length];
        finalData[0] = 1; //Code for updating positions
        int index = 2;
        for (int i = 0; i < xArray.length; i++)
            finalData[index++] = xArray[i];
        for (int i = 0; i < yArray.length; i++)
            finalData[index++] = yArray[i];
        for (int i = 0; i < nameArray.length; i++)
            finalData[index++] = nameArray[i];

        DatagramPacket packet = new DatagramPacket(finalData, finalData.length);
        try {
            udpSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

