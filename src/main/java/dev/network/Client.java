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

    public byte getMyId() {
        if (myId==0){
            askId();
            return 0;
        }
        return myId;
    }

    private DatagramSocket udpSocket;

    private Map<Byte, User> users = new ConcurrentHashMap<Byte, User>();

    private Collection<UserBullet> bulletQueue = new ConcurrentLinkedQueue<UserBullet>();

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
                        }

                    } catch (IOException e) {

                    }
                }
            }
        }.start();
    }

    private void restartGame(DatagramPacket packet) {
        Game.getInstance().getPlayer().isDead = false;
    }

    private void receiveId(DatagramPacket packet) {
        myId = packet.getData()[1];
    }

    public void askId(){
        try {
            udpSocket.send(new DatagramPacket(new byte[]{4}, 1, udpSocket.getInetAddress(), udpSocket.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void positionBullet(DatagramPacket packet) {
        byte[] data = packet.getData();
        float x = ByteBuffer.wrap(data,2,4).getFloat();
        float y = ByteBuffer.wrap(data, 6, 4).getFloat();
        bulletQueue.add(new UserBullet(x, y).addUserId(data[1]));
    }

    public void sendBullet(Bullet b){
        byte[] data = new byte[2+4+4+4+4];
        data[0] = 3;
        byte[] posX = ByteBuffer.allocate(4).putFloat(b.getPosX()).array();
        byte[] posY = ByteBuffer.allocate(4).putFloat(b.getPosY()).array();
        byte[] vecX = ByteBuffer.allocate(4).putFloat(b.getVecX()).array();
        byte[] vecY = ByteBuffer.allocate(4).putFloat(b.getVecY()).array();
        for (int i = 0; i < 4; i++) {
            data[i+2] = posX[i];
            data[i+6] = posY[i];
            data[i+10] = vecX[i];
            data[i+14] = vecY[i];
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
        float x = ByteBuffer.wrap(data,2,4).getFloat();
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
        boolean isDead = data[11] == 1;
        int index = 12;
        String username = "";
        while (index < data.length) {
            username += (char) data[index++];
        }

        if (users.containsKey(id)) {
            User u = users.get(id);
            u.isDead=isDead;
            u.setPosX(x);
            u.setPosY(y);
        } else {
            users.put(id, new User(null, username, id, x, y));
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
        int index = 2 ;
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

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

}

