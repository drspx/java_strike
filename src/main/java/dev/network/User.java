package dev.network;

import dev.Entity;
import dev.Player;

import java.net.InetAddress;
import java.net.SocketAddress;

public class User extends Entity {
    public boolean isDead = false;
    //Server side
    private InetAddress address;
    private int port;

    private byte id;
    private String username;

    //private object, not sent over network, used to timeout, in case someone logs out.
    private long lastAlive;


    public User(InetAddress address, String username, byte id, float x, float y)
    {
        super(x, y, Player.width, Player.height);
        this.address = address;
        this.username = username;
        this.id = id;
        lastAlive = System.currentTimeMillis();
    }

    public void confirmAlive()
    {
        lastAlive = System.currentTimeMillis();
    }

    public long lastConfirmationOfLife()
    {
        return lastAlive;
    }

    public String getUsername() {
        return username;
    }

    public byte getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }


}
