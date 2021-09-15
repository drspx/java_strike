package dev.network;

import dev.Game;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.network.NetValues.TIMEOUT;

public class Server {

    private DatagramSocket udpSocket;
    private List<User> clients;
    private byte idIncrementer;
    private List<UserBullet> userBullets = new CopyOnWriteArrayList<>();

    public Server(int udpPort) {
        idIncrementer = (byte) (10 + (Math.random() * 20.0));
        clients = new ArrayList<User>();
        try {
            udpSocket = new DatagramSocket(udpPort);
            System.out.println("Server is bound: " + udpSocket.isBound());
            System.out.println(udpSocket.getInetAddress() + " " + udpPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void newUser(DatagramPacket packet) {
        byte[] data = packet.getData();
        float x = ByteBuffer.wrap(data, 2, 4).getFloat();
        float y = ByteBuffer.wrap(data, 6, 4).getFloat();
        int index = 10;
        String username = "";
        while (index < data.length && data[index] != 0) {
            username += (char) data[index++];
        }

        User u = new User(packet.getAddress(), username, idIncrementer++, x, y);
        u.setPort(packet.getPort());
        clients.add(u);
    }

    private void updatePosition(DatagramPacket packet) { // position of players
        byte[] data = packet.getData();

        float x = ByteBuffer.wrap(data, 2, 4).getFloat();
        float y = ByteBuffer.wrap(data, 6, 4).getFloat();

        int index = 12;
        String username = "";
        while (index < data.length) {
            username += (char) data[index++];
        }

        for (User u : clients) {
            if (u.getAddress().equals(packet.getAddress())) {

                u.setPosX(x);
                u.setPosY(y);
                u.confirmAlive();
                broadcastPosition(u, x, y, username);
                break;
            }
        }


    }

    public void broadCastBullets() {
        List<UserBullet> copy = new CopyOnWriteArrayList<UserBullet>(userBullets);
        for (int i = 0; i < copy.size(); i++) {
            byte[] data = new byte[2 + 4 + 4];
            byte[] posX = ByteBuffer.allocate(4).putFloat(copy.get(i).getPosX()).array();
            byte[] posY = ByteBuffer.allocate(4).putFloat(copy.get(i).getPosY()).array();
            for (int j = 0; j < 4; j++) {
                data[2 + j] = posX[j];
                data[6 + j] = posY[j];
            }
            for (int j = 0; j < clients.size(); j++) {
                data[0] = 3;
                data[1] = copy.get(i).getId();
                DatagramPacket packet = new DatagramPacket(data, data.length,
                        clients.get(j).getAddress(), clients.get(j).getPort());
                try {
                    udpSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void broadcastPosition(User u, float x, float y, String name) {
        byte[] data = new byte[1 + 1 + 4 + 4 + 2 + name.getBytes().length];

        data[0] = 1; //Position update
        data[1] = u.getId();
        byte[] xArr = ByteBuffer.allocate(4).putFloat(x).array();
        byte[] yArr = ByteBuffer.allocate(4).putFloat(y).array();
        for (int i = 0; i < 4; i++) {
            data[2 + i] = xArr[i];
            data[6 + i] = yArr[i];
        }
        data[11] = (byte) (u.isDead ? 1 : 0);
        int index = 12;
        for (int i = 0; i < name.getBytes().length; i++) {
            data[index++] = name.getBytes()[i];
        }

        for (User user : clients) {
            DatagramPacket packet = new DatagramPacket(data, data.length, user.getAddress(), user.getPort());
            try {
                udpSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanUpClients() {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).lastConfirmationOfLife() - System.currentTimeMillis() > TIMEOUT) {
                clients.remove(i--);
            }
        }
    }

    public void start() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                long updateTimeoutLast = System.currentTimeMillis();
                long updateTimeoutWait = 1000;

                byte[] packetData = new byte[256];
                while (udpSocket.isBound()) {
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length);
                    try {
                        udpSocket.receive(packet);
                        boolean clientAlreadyExist = false;
                        for (User client : clients) {
                            if (client.getAddress().equals(packet.getAddress())) {
                                clientAlreadyExist = true;
                                break;
                            }
                        }

                        switch (packet.getData()[0]) {
                            case 1:
                                updatePosition(packet);
                                break;
                            case 2:
                                updateBullets(packet);
                                break;
                            case 3:
                                receiveBullet(packet);
                                break;
                            case 4:
                                sendId(packet);
                                break;

                        }
                        if (!clientAlreadyExist) newUser(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (System.currentTimeMillis() - updateTimeoutLast > updateTimeoutWait) {
                        cleanUpClients();
                        updateTimeoutLast = System.currentTimeMillis();
                    }
                }
            }
        });
        t.start();

    }

    private void sendId(DatagramPacket packet) {
        byte[] data = packet.getData();
        for (User u : clients) {
            if (u.getAddress().equals(packet.getAddress())) {
                data[1] = u.getId();
                break;
            }
        }
        try {
            udpSocket.send(new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveBullet(DatagramPacket packet) {
        byte[] data = packet.getData();
        float posX = ByteBuffer.wrap(data, 2, 4).getFloat();
        float posY = ByteBuffer.wrap(data, 6, 4).getFloat();
        float vecX = ByteBuffer.wrap(data, 10, 4).getFloat();
        float vecY = ByteBuffer.wrap(data, 14, 4).getFloat();
        System.out.println("receive bullet:" + posX + ":" + posY + "-" + vecX + ":" + vecY);

        for (User u : clients) {
            if (u.getAddress().equals(packet.getAddress())) {
                userBullets.add(new UserBullet(posX, posY).addVectors(vecX, vecY).addUserId(u.getId()));
                break;
            }
        }
        broadCastBullets();
    }

    private void updateBullets(DatagramPacket packet) {
        byte[] data = packet.getData();
        float x = ByteBuffer.wrap(data, 2, 4).getFloat();
        float y = ByteBuffer.wrap(data, 6, 4).getFloat();
        System.out.println("received bullet " + x + " " + y);
        //broadCastBullets();

    }


    public List<UserBullet> getUserBullets() {
        return userBullets;
    }

    public String getUserNameFromId(byte id) {
        String username = "";
        for (int i = 0; i < this.clients.size(); i++) {
            if (clients.get(i).getId() == id) {
                username = clients.get(i).getUsername();
                break;
            }
        }
        return username;
    }

    private void cleanUp() {
        for (int i = 0; i < clients.size(); i++) {
            if (System.currentTimeMillis() - clients.get(i).lastConfirmationOfLife() > TIMEOUT) {
                clients.remove(i);
                break;
            }
        }
    }

    public List<User> getUsers() {
        return clients;
    }


    public void tick() {
        cleanUp();
    }

    public void restartGame() {
        byte[] data = new byte[1 + 1];

        data[0] = 5;
        for (int i = 0; i < clients.size(); i++) {

            clients.get(i).isDead = false;


            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, clients.get(i).getAddress(), clients.get(i).getPort());
            try {
                udpSocket.send(datagramPacket);
                System.out.println("Restart message sent" + datagramPacket.getAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Game.getInstance().getDisplay().getKeyboard().restart=false;

    }
}

