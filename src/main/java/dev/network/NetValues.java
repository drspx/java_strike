package dev.network;

public final class NetValues {

    //How long it takes in ms when no packages are received from a client, to when they should be removed
    public static final int TIMEOUT = 4 * 1000;

    // Packet type for fog of war state broadcast (server -> clients)
    public static final int PACKET_FOG_OF_WAR = 13;

}
