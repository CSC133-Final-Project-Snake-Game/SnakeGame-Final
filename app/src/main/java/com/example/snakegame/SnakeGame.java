package com.example.snakegame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import android.graphics.Rect;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;



class SnakeGame extends SurfaceView implements Runnable{

    // Objects for the game loop/thread
    private Thread mThread = null;
    // Control pausing between updates
    private long mNextFrameTime;
    // Is the game currently playing and or paused?
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;
    private volatile boolean mpauseMenu = false;
    private boolean isNewGame = true;
    private Rect pauseButton;
    private Rect Line;
    private Rect pauseMenu;
    private Rect pauseMenuResume;
    private Rect pauseMenuQuit;

    // for playing sound effects
    private SoundManager soundManager;

    // Movement speed for the snake
    private float normalSpeed = 1.0f;
    private float boostedSpeed = 2.0f;
    private float speedMultiplier = normalSpeed;
    private boolean isSpeedBoosted = false;
    private long speedBoostEndTime = 0;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh;

    // How many points does the player have
    private int mScore;

    // Objects for drawing
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Bitmap mBackground;
    private Bitmap mStart;
    private Typeface mCustomFont;

    // A snake ssss
    private Snake mSnake;
    // And an apple
    private Apple mApple;
    private List<Consumable> consumables = new ArrayList<>();
    private List<Buff> activeBuffs = new ArrayList<>();
    private int blockSize;

    // Variables for the colored rectangles
    private Rect greenColorRect;
    private Rect blueColorRect;
    private Rect yellowColorRect;
    private Rect redColorRect;

    // gameover
    private boolean gameOver = false;


    // This is the constructor method that gets called
    // from SnakeActivity
    public SnakeGame(Context context, Point size) {
        super(context);
        // Work out how many pixels each block is
        // How many blocks of the same size will fit into the height
        initializeScreen(size);
        //Initialize the SoundPool
        soundManager = new SoundManager(context);
        // Initialize the drawing objects
        initializeDrawObjects();
        // Call the constructors of our two game objects
        callConstructorObjects(context);
        //initialize for pause button
        initializePauseButton();
        // initialize for pause menu
        initializePauseMenu();
        // initialize for pause menu buttons
        initializeStartMenuImage(context, size);
        //initialize for the background image
        initializeBackGroundImage(context,size);
        //initialize text font
        initializeTextFont(context);
        //initialize color choice rectangles
        initializeColorRect(size);
    }

    // Called to start a new game
    public void newGame() {

        // reset the snake
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);

        // reset movement speed back to default 1.0f
        speedMultiplier = normalSpeed;
        isSpeedBoosted = false;
        clearBuffs();

        consumables.clear();
        // Get the apple ready for dinner
        mApple.spawn();
        consumables.add(mApple);

        // Reset the mScore
        mScore = 0;

        // Setup mNextFrameTime so an update can triggered
        mNextFrameTime = System.currentTimeMillis();

        isNewGame = true;

    }


    // Handles the game loop
    @Override
    public void run() {
        while (mPlaying) {
            if (!mPaused) {
                // Update 10 times a second
                if (updateRequired()) {
                    update();
                    checkBuffs();
//                    checkSpeedBoostTimer();
                }
            }

            draw();
        }
    }

    public void checkSpeedBoostTimer(){
        if (isSpeedBoosted && System.currentTimeMillis() > speedBoostEndTime) {
            speedMultiplier = normalSpeed; // Revert movement speed back to normal
            isSpeedBoosted = false;
        }
    }

    // Check to see if it is time for an update
    public boolean updateRequired() {

        // Run at 10 frames per second
        final long TARGET_FPS = 10;
        // There are 1000 milliseconds in a second
        final long MILLIS_PER_SECOND = 1000;

        final long framePeriod = (long) ((MILLIS_PER_SECOND / TARGET_FPS) / speedMultiplier);

        if (mNextFrameTime <= System.currentTimeMillis()) {
            mNextFrameTime = System.currentTimeMillis() + framePeriod;
            return true;
        }

        return false;
    }

    public void increaseSpeed() {
        Buff existingBoost = null;
        for (Buff buff : activeBuffs) {
            if (buff.type == Buff.Type.SPEED_BOOST) {
                existingBoost = buff;
                break;
            }
        }

        if (existingBoost != null) {
            existingBoost.refreshDuration(5000);
            soundManager.refreshSpeedBoostSound();
        } else {
            // No active speed boost, so add a new one
            Buff speedBoost = new Buff(Buff.Type.SPEED_BOOST, 5000);
            activeBuffs.add(speedBoost);
            soundManager.playSpeedBoostSound();
        }
        speedMultiplier = boostedSpeed;
    }


    public void checkBuffs() {
        boolean speedBoostActive = false;
        Iterator<Buff> iterator = activeBuffs.iterator();
        while (iterator.hasNext()) {
            Buff buff = iterator.next();
            if (!buff.isActive()) {
                if (buff.type == Buff.Type.SPEED_BOOST && speedMultiplier == boostedSpeed) {
                    speedMultiplier = normalSpeed;  // Reset speed to normal
                    soundManager.stopSpeedBoostSound();
                }
                iterator.remove();  // Remove the expired buff
                continue;
            }
            if (buff.type == Buff.Type.SPEED_BOOST) {
                speedBoostActive = true;
            }
        }

        if (!speedBoostActive && isSpeedBoosted) {
            isSpeedBoosted = false;
            speedMultiplier = normalSpeed;
            soundManager.stopSpeedBoostSound();
        }
    }



    // Update all the game objects
    public void update() {

        // Move the snake
        mSnake.move();

        List<Consumable> consumedItems = new ArrayList<>();
        List<Consumable> newItems = new ArrayList<>();
        for (Consumable consumable : consumables) {
            if (mSnake.checkDinner(consumable.getLocation())) {

                // adjust the score according to the value of the consumable
                mScore += consumable.value;

                consumable.playSound();
                consumable.applyEffect(this);
                consumedItems.add(consumable);

                if (consumable.value > 0) {
                    for (int i = 0; i < consumable.value; i++) {
                        mSnake.grow();
                    }
                } else if (consumable.value < 0) {
                    for (int i = 0; i < Math.abs(consumable.value); i++) {
                        mSnake.shrink();
                    }
                }

                if (consumable instanceof Apple) {
                    // Spawns a new apple
                    Apple newApple = new Apple(getContext(), new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, soundManager);
                    newApple.spawn();
                    newItems.add(newApple);

                    // Spawns a bad apple every time a "good" apple is consumed.
                    BadApple badApple = new BadApple(getContext(), new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, soundManager);
                    badApple.spawn();
                    newItems.add(badApple);

                    SpeedBooster speedApple = new SpeedBooster(getContext(), new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, soundManager);
                    speedApple.spawn();
                    newItems.add(speedApple);
                }
            }
        }
        consumables.removeAll(consumedItems);
        consumables.addAll(newItems);

        // Did the snake die?
        if (mSnake.detectDeath()) {
            // Pause the game ready to start again
            soundManager.pauseAllSounds();
            clearBuffs();
            soundManager.playDeathSound();

            mPaused = true;
            isNewGame = true;
        }

    }


    // Do all the drawing
    public void draw() {
        // Get a lock on the mCanvas
        if (mSurfaceHolder.getSurface().isValid()) {
            mCanvas = mSurfaceHolder.lockCanvas();
            //Added the background image
            drawbackground(mCanvas);
            // Set the size, color, and font of the mPaint for the text
            drawSetText(mCanvas);
            //Draw the score and names of students
            drawScoreAndName(mCanvas);
            //Draw the pause button as a white square
            drawPause(mCanvas);
            //Draw the apple and snake
            drawGameObjects(mCanvas);
            // draw pause menu while paused
            drawPauseMenu(mCanvas);
            // Draw some text while paused
            drawPauseMessage(mCanvas);
            // Added the start menu image
            drawStartMenu(mCanvas);

            if (!mPaused){
                // Display active buffs
                mPaint.setColor(Color.YELLOW);
                mPaint.setTextSize(75);
                int yPosition = 75; // Start drawing buffs from this vertical position
                for (Buff buff : activeBuffs) {
                    long timeLeft = (buff.startTime + buff.duration - System.currentTimeMillis()) / 1000;
                    mCanvas.drawText(buff.type.name() + " Active! Time left: " + timeLeft + "s", 800, yPosition, mPaint);
                    yPosition += 50; // Increment position for next buff
                }
            }

            // If game paused, draw the text and rectangles
            if(mPaused) {
                // Draw the text "Snake Color" above the green color rectangle
                mPaint.setColor(Color.WHITE); // Set the color for the text
                mPaint.setTextSize(50); // Set the text size
                String snakeColorText = "Snake Color";
                float textWidth = mPaint.measureText(snakeColorText); // Measure the width of the text
                float x = greenColorRect.centerX() - (textWidth / 2); // Calculate the x coordinate for centering the text
                float y = greenColorRect.top - 20; // Set the y coordinate above the rectangle
                mCanvas.drawText(snakeColorText, x, y, mPaint); // Draw the text

                // Draw the green color rectangle
                mPaint.setColor(Color.GREEN); // Set the color for the  rectangle
                mCanvas.drawRect(greenColorRect, mPaint); // Draw the rectangle

                // Draw the blue color rectangle below the green one
                mPaint.setColor(Color.BLUE);
                mCanvas.drawRect(blueColorRect, mPaint);

                // Draw the yellow color rectangle below the blue one
                mPaint.setColor(Color.YELLOW);
                mCanvas.drawRect(yellowColorRect, mPaint);

                // Draw the red color rectangle below the yellow one
                mPaint.setColor(Color.RED);
                mCanvas.drawRect(redColorRect, mPaint);
            }


            if (!mPaused){
                // Display active buffs
                mPaint.setColor(Color.YELLOW);
                mPaint.setTextSize(75);
                int yPosition = 75; // Start drawing buffs from this vertical position
                for (Buff buff : activeBuffs) {
                    long timeLeft = (buff.startTime + buff.duration - System.currentTimeMillis()) / 1000;
                    mCanvas.drawText(buff.type.name() + " Active! Time left: " + timeLeft + "s", 800, yPosition, mPaint);
                    yPosition += 50; // Increment position for next buff
                }
            }

            // calling gameover
            if (gameOver) {
                drawGameOverText(mCanvas);
            }

            // Unlock the mCanvas and reveal the graphics for this frame
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    // Method to get the Snake object
    public Snake getSnake() {
        return mSnake;
    }

    private void initializeColorRect(Point screenSize) {
        // Calculate the coordinates for the top-right corner
        int rectWidth = 200; // Width of the rectangle
        int rectHeight = 100; // Height of the rectangle
        int padding = 100; // Padding from the screen edges

        int left = screenSize.x - rectWidth - padding; // Left coordinate of the rectangle
        int top = padding; // Top coordinate of the rectangle
        int right = screenSize.x - padding; // Right coordinate of the rectangle
        int bottom = rectHeight + padding; // Bottom coordinate of the rectangle

        // Initialize the green rectangle
        greenColorRect = new Rect(left, top, right, bottom);

        // Initialize the blue rectangle below the green one
        blueColorRect = new Rect(left, bottom + padding, right, bottom + padding + rectHeight);

        // Initialize the yellow rectangle below the blue one
        yellowColorRect = new Rect(left, blueColorRect.bottom + padding, right, blueColorRect.bottom + padding + rectHeight);

        // Initialize the red rectangle below the yellow one
        redColorRect = new Rect(left, yellowColorRect.bottom + padding, right, yellowColorRect.bottom + padding + rectHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();

                // If game paused, allow colored rectangles to be clicked
                if(mpauseMenu && mPaused) {
                    // Example: Assuming there's a rectangle on the screen representing the green color selection area
                    if (greenColorRect.contains(x, y)) {
                        // Change snake color to green
                        mSnake.setSnakeColor(getContext(), Snake.SnakeColor.GREEN);
                        return true;
                    }
                    if (blueColorRect.contains(x, y)) {
                        // Change snake color to green
                        mSnake.setSnakeColor(getContext(), Snake.SnakeColor.BLUE);
                        return true;
                    }
                    if (yellowColorRect.contains(x, y)) {
                        // Change snake color to green
                        mSnake.setSnakeColor(getContext(), Snake.SnakeColor.YELLOW);
                        return true;
                    }
                    if (redColorRect.contains(x, y)) {
                        // Change snake color to green
                        mSnake.setSnakeColor(getContext(), Snake.SnakeColor.RED);
                        return true;
                    }
                }

                //Detect if button is clicked
                if (pauseButton.contains(x, y)) {
                    mPaused = !mPaused;
                    if (mPaused) {
                        soundManager.pauseAllSounds();
                    }else {
                        soundManager.resumeAllSounds();
                    }
                    return true;
                }
                if (mPaused) {
                    mPaused = false;
                    soundManager.pauseAllSounds();
                    if (isNewGame) {
                        newGame();
                        isNewGame = false;
                    }
                    return true;
                }
                if(mPaused && pauseMenuResume.contains(x, y)) {
                    mPaused = !mPaused;
                }

                mSnake.switchHeading(motionEvent);
                break;
        }
        return true;
    }

    // Stop the thread
    public void pause() {
        mPlaying = false;
        try {
            mThread.join();
            clearBuffs();
            soundManager.pauseAllSounds();
        } catch (InterruptedException e) {
            // Error
        }
    }

    // Start the thread
    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
        soundManager.resumeAllSounds();
    }

    private void clearBuffs() {
        for (Buff buff : activeBuffs) {
            if (buff.type == Buff.Type.SPEED_BOOST) {
                soundManager.stopSpeedBoostSound();
            }
        }
        activeBuffs.clear();
    }

    private void drawbackground(Canvas canvas) {
        canvas.drawBitmap(mBackground,0,0,null);
    }

    private void drawStartMenu (Canvas canvas) {
        if (isNewGame) {
            canvas.drawBitmap(mStart, 0, 0, null);
        }
    }

    private void drawSetText(Canvas canvas){
        mPaint.setTypeface(mCustomFont);
        mPaint.setColor(Color.argb(255, 255, 255, 255));
        mPaint.setTextSize(120);
    }

    private void drawScoreAndName(Canvas canvas) {
        mCanvas.drawText("" + mScore, 150, 120, mPaint);
//        mCanvas.drawText("Alexis Dawatan, Wei Chong", 1750, 120, mPaint);
    }

    private void drawPause(Canvas canvas){
        mPaint.setColor(Color.argb(200, 255, 255, 255));
        mCanvas.drawRect(pauseButton, mPaint);
    }

    private void drawGameObjects(Canvas canvas){
        // Draw the apple and the snake
        mSnake.draw(mCanvas, mPaint);

        // Draw all the consumables
        for (Consumable consumable : consumables) {
            consumable.draw(mCanvas, mPaint);
        }
    }

    private void drawPauseMessage(Canvas canvas) {
        if (mPaused) {
            mpauseMenu = !mpauseMenu;

            // Set the size and color of the mPaint for the text
            mPaint.setColor(Color.argb(255, 255, 255, 255));
            mPaint.setTextSize(250);

            // Determine the message based on if game is paused or new game is created.
            String message = isNewGame ? getResources().getString(R.string.tap_to_play) : "Game Paused";

            // Draw the message
            mCanvas.drawText(message, 400, 300, mPaint);
        }
    }

    private void drawPauseMenu (Canvas canvas) {
        if (mpauseMenu && !isNewGame) {
            // draws transparent black pause rectangle
            mPaint.setColor(Color.argb(200, 0, 0, 0));
            mCanvas.drawRect(pauseMenu, mPaint);
            // draws line under 'game paused'
            mPaint.setColor(Color.argb(255, 255, 255, 255));
            mCanvas.drawRect(Line,mPaint);

            // draws resume button
            mPaint.setColor(Color.argb(200, 255, 255, 255));
            mCanvas.drawRect(pauseMenuResume, mPaint);
            mPaint.setColor(Color.BLACK);
            mCanvas.drawText("Resume",500, 700, mPaint);

            // draws quit button
            mPaint.setColor(Color.argb(200, 255, 255, 255));
            mCanvas.drawRect(pauseMenuQuit, mPaint);
            mPaint.setColor(Color.BLACK);
            mCanvas.drawText("Quit",1200, 700, mPaint);

            mpauseMenu = !mpauseMenu;
        }
    }

    //Initialize methods
    private void initializeScreen(Point size){
        // Work out how many pixels each block is
        blockSize = size.x / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        mNumBlocksHigh = size.y / blockSize;
    }

    private void initializeDrawObjects(){
        mSurfaceHolder = getHolder();
        mPaint = new Paint();
    }

    private void initializePauseButton(){
        int pauseButtonWidth = 100;
        int pauseButtonHeight = 100;
        int pauseButtonPadding = 30;
        pauseButton = new Rect(pauseButtonPadding, pauseButtonPadding, pauseButtonWidth + pauseButtonPadding, pauseButtonHeight + pauseButtonPadding);
    }

    private void initializePauseMenu(){ // pause menu and its buttons
        int pauseMenuWidth = 1800;
        int pauseMenuHeight = 900;
        int pauseButtonPadding = 100;
        pauseMenu = new Rect(250, 50, pauseMenuWidth, pauseMenuHeight);

        int pauseLineWidth = 1600;
        int pauseLineHeight = 100;
        int pauseLinePadding = 300;
        Line = new Rect(400, 375, pauseLineWidth, pauseLineHeight + pauseLinePadding);

        int pauseResumeWidth = 850;
        int pauseResumeHeight = 700;
        int pauseResumePadding = 100;
        pauseMenuResume = new Rect(400, 500, pauseResumeWidth + pauseResumePadding, pauseResumeHeight + pauseResumePadding);

        int pauseQuitWidth = 1500;
        int pauseQuitHeight = 700;
        int pauseQuitPadding = 100;
        pauseMenuQuit = new Rect(1000, 500, pauseQuitWidth + pauseQuitPadding, pauseQuitHeight + pauseQuitPadding);
    }

    private void initializeStartMenuImage(Context context, Point size) {
        mStart = BitmapFactory.decodeResource(context.getResources(), R.drawable.start_menu);
        mStart = Bitmap.createScaledBitmap(mStart, size.x, size.y, false);
    }

    private void initializeBackGroundImage(Context context, Point size) {
        try {
            mBackground= BitmapFactory.decodeResource(context.getResources(), R.drawable.grass);
            mBackground = Bitmap.createScaledBitmap(mBackground, size.x, size.y, false);
        } catch (Exception e) {
            // Handle error loading background image
            e.printStackTrace();
        }
    }

    private void initializeTextFont(Context context){
        try {
            mCustomFont = ResourcesCompat.getFont(context, R.font.cookie_crisp);
            mPaint.setTypeface(mCustomFont);
        } catch (Exception e) {
            // Handle error loading custom font
            e.printStackTrace();
        }
    }

    private void callConstructorObjects(Context context){
        mApple = new Apple(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize, soundManager);

        mSnake = new Snake(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize, Snake.SnakeColor.GREEN);
    }

    // adding gameover
    private void gameOver() {
        // Stop the game
        mPlaying = false;
        // Set game over state
        gameOver = true;
        // Pause the game
        pause();
    }
    private void drawGameOverText(Canvas canvas) {
        mPaint.setColor(Color.RED);
        mPaint.setTextSize(100);
        canvas.drawText("Game Over", 200, 500, mPaint);
    }

}
