package dev;

public class BombState {
    public static final int STATE_CARRIED = 0;
    public static final int STATE_PLANTING = 1;
    public static final int STATE_PLANTED = 2;
    public static final int STATE_DEFUSING = 3;
    public static final int STATE_EXPLODED = 4;
    public static final int STATE_DEFUSED = 5;

    public static final long PLANT_TIME_MS = 3000;
    public static final long DEFUSE_TIME_MS = 5000;
    public static final long BOMB_TIMER_MS = 40000;

    private int state = STATE_CARRIED;
    private float bombX, bombY;
    private long plantStartTime;
    private long plantedTime;
    private long defuseStartTime;
    private byte carrierPlayerId;
    private byte defuserPlayerId;

    // Bomb site area
    private float siteX, siteY, siteW, siteH;

    public BombState(float siteX, float siteY, float siteW, float siteH) {
        this.siteX = siteX;
        this.siteY = siteY;
        this.siteW = siteW;
        this.siteH = siteH;
    }

    public void reset(byte carrierPlayerId) {
        this.state = STATE_CARRIED;
        this.carrierPlayerId = carrierPlayerId;
        this.bombX = 0;
        this.bombY = 0;
    }

    public boolean isInBombSite(float px, float py) {
        return px >= siteX && px <= siteX + siteW && py >= siteY && py <= siteY + siteH;
    }

    public void startPlanting(float x, float y, byte playerId) {
        if (state == STATE_CARRIED && playerId == carrierPlayerId) {
            state = STATE_PLANTING;
            bombX = x;
            bombY = y;
            plantStartTime = System.currentTimeMillis();
        }
    }

    public void cancelPlanting() {
        if (state == STATE_PLANTING) {
            state = STATE_CARRIED;
        }
    }

    public boolean tickPlanting() {
        if (state == STATE_PLANTING && System.currentTimeMillis() - plantStartTime >= PLANT_TIME_MS) {
            state = STATE_PLANTED;
            plantedTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public void startDefusing(byte playerId) {
        if (state == STATE_PLANTED) {
            state = STATE_DEFUSING;
            defuseStartTime = System.currentTimeMillis();
            defuserPlayerId = playerId;
        }
    }

    public void cancelDefusing() {
        if (state == STATE_DEFUSING) {
            state = STATE_PLANTED;
        }
    }

    public boolean tickDefusing() {
        if (state == STATE_DEFUSING && System.currentTimeMillis() - defuseStartTime >= DEFUSE_TIME_MS) {
            state = STATE_DEFUSED;
            return true;
        }
        return false;
    }

    public boolean tickBombTimer() {
        if (state == STATE_PLANTED || state == STATE_DEFUSING) {
            if (System.currentTimeMillis() - plantedTime >= BOMB_TIMER_MS) {
                state = STATE_EXPLODED;
                return true;
            }
        }
        return false;
    }

    public int getRemainingBombTime() {
        if (state == STATE_PLANTED || state == STATE_DEFUSING) {
            long elapsed = System.currentTimeMillis() - plantedTime;
            return Math.max(0, (int) ((BOMB_TIMER_MS - elapsed) / 1000));
        }
        return -1;
    }

    public int getPlantProgress() {
        if (state == STATE_PLANTING) {
            long elapsed = System.currentTimeMillis() - plantStartTime;
            return (int) Math.min(100, (elapsed * 100) / PLANT_TIME_MS);
        }
        return 0;
    }

    public int getDefuseProgress() {
        if (state == STATE_DEFUSING) {
            long elapsed = System.currentTimeMillis() - defuseStartTime;
            return (int) Math.min(100, (elapsed * 100) / DEFUSE_TIME_MS);
        }
        return 0;
    }

    public int getState() {
        return state;
    }

    public float getBombX() {
        return bombX;
    }

    public float getBombY() {
        return bombY;
    }

    public float getSiteX() {
        return siteX;
    }

    public float getSiteY() {
        return siteY;
    }

    public float getSiteW() {
        return siteW;
    }

    public float getSiteH() {
        return siteH;
    }

    public byte getCarrierPlayerId() {
        return carrierPlayerId;
    }
}
