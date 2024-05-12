package com.example.snakegame;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.SoundPool;

class SpeedBooster extends Consumable {

    public SpeedBooster(Context context, Point spawnRange, int size, SoundManager soundManager) {
        super(context, spawnRange, size, 0,soundManager);

    }

    @Override
    void loadBitmap() {
        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.speed_apple);
    }

    @Override
    public void playSound() {}

    public void applyEffect(SnakeGame game){
        game.increaseSpeed();
    }
}
