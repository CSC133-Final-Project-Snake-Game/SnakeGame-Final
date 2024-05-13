package com.example.snakegame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import java.io.IOException;
import android.os.Handler;
import android.os.Looper;


public class SoundManager {
    private SoundPool mSP;
    private int mEat_ID;
    private int mCrashID;
    private int mbadID;
    private int mSpeedBoostID;
    private int mSpeedBoostStreamID;
    private Handler mHandler;

    public SoundManager(Context context) {
        mHandler = new Handler(Looper.getMainLooper());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSP = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSP = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }

        loadSounds(context);
    }

    private void loadSounds(Context context){
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the sounds in memory
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_death.ogg");
            mCrashID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("get_bad.ogg");
            mbadID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("speed_boost.MP3");
            mSpeedBoostID = mSP.load(descriptor, 0);


        } catch (IOException e) {
            // Error
        }
    }

    public void pauseAllSounds() {
        mSP.autoPause();
    }

    public void resumeAllSounds() {
        mSP.autoResume();
    }

    public void playEatSound(){
        mSP.play(mEat_ID, 1, 1, 0, 0, 1);
    }

    public void playDeathSound(){
        mSP.play(mCrashID,1,1,0,0,1);
    }

    public void playBadSound(){
        mSP.play(mbadID, 1, 1, 0, 0, 1);
    }

    public void playSpeedBoostSound() {
        if (mSpeedBoostStreamID != 0) {

        } else {
            // Start playing the sound since it's not already playing
            mSpeedBoostStreamID = mSP.play(mSpeedBoostID, 1, 1, 0, 0, 1);
            mHandler.postDelayed(this::loopSpeedBoostSound, 5000); // Assume duration is 5000 ms for one loop
        }
    }

    private void loopSpeedBoostSound() {
        if (mSpeedBoostStreamID != 0) {
            mSP.setLoop(mSpeedBoostStreamID, -1);
        }
    }

    public void stopSpeedBoostSound() {
        if (mSpeedBoostStreamID != 0) {
            mSP.stop(mSpeedBoostStreamID);
            mSP.setLoop(mSpeedBoostStreamID, 0);
            mSpeedBoostStreamID = 0;
        }
        mHandler.removeCallbacks(this::loopSpeedBoostSound);
    }

    public void refreshSpeedBoostSound() {
        if (mSpeedBoostStreamID != 0) {
            // Stop the current playback
            mSP.stop(mSpeedBoostStreamID);
            // Reset the loop status
            mSP.setLoop(mSpeedBoostStreamID, 0);
            // Start the sound again
            mSpeedBoostStreamID = mSP.play(mSpeedBoostID, 1, 1, 0, 0, 1);
            mHandler.postDelayed(this::loopSpeedBoostSound, 5000); // Reset the looping mechanism
        }
    }

}
