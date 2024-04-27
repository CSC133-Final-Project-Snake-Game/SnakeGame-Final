package com.example.snakegame;

public class Buff {
    enum Type {
        SPEED_BOOST
    }

    Type type;
    long startTime;
    long duration; // in milliseconds

    Buff(Type type, long duration) {
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public boolean isActive() {
        return System.currentTimeMillis() < (startTime + duration);
    }

    public void refreshDuration(long newDuration) {
        this.startTime = System.currentTimeMillis();
        this.duration = newDuration;
    }
}

