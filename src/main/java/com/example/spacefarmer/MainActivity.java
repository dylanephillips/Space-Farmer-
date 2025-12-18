package com.example.spacefarmer;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.autofill.Validators;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Program: MainActivity.java
 * Description: The main activity and game engine for the Space Farmer application. Manages game state,
 * physics (movement/collision), level configuration, tutorial flow, entity management, and rendering updates.
 * Called By: StartActivity, PlanetSelectActivity, LevelSelectActivity, GameOverActivity.
 * Will Call: Various UI and game logic methods.
 */

public class MainActivity extends AppCompatActivity {

    // Helper class to hold state for each alien.
    private static class AlienState {
        TextView alienView; // Reference to the alien's TextView on screen.
        int currentHits; // How many times the alien has been hit.
        long nextMoveTime; // The timestamp for the alien's next move.
        long nextShootTime; // The timestamp for the alien's next shot.

        AlienState(TextView alienView, long currentTime, int moveDelay, int shootDelay) {
            this.alienView = alienView;
            this.currentHits = 0;
            // Set a random initial delay for movement and shooting to desynchronize aliens.
            this.nextMoveTime = currentTime + new Random().nextInt(moveDelay);
            this.nextShootTime = currentTime + new Random().nextInt(shootDelay);
        }
    }

    /**
     * Class: BulletState
     * Description: A simple data class to hold the velocity components (stepX, stepY) for a projectile.
     *              This is significantly more performant than storing them as a String and parsing
     *              them on every frame, as it avoids repeated string manipulation and object creation.
     * Called By: shootBullet() (when creating a new state), updateBullets() (when reading the state).
     */
    private static class BulletState {
        float stepX; // The horizontal velocity component.
        float stepY; // The vertical velocity component.

        BulletState(float stepX, float stepY) {
            this.stepX = stepX;
            this.stepY = stepY;
        }
    }

    // UI Elements
    private RelativeLayout mainLayout; // The root layout containing all game elements.
    private ImageView bgImage; // The background image for the current planet.
    private View playAreaDivider; // The line separating the game area from the controls.
    private TextView scoreText, healthText, farmer; // UI text displays and the farmer character.
    private Button startButton; // The button to start the level.
    private ImageButton leftBtn, rightBtn, upBtn, downBtn, throwBtn; // Control buttons.

    // Boss UI
    private RelativeLayout bossHealthLayout; // The container for the boss's health bar.
    private View bossHealthBarVisual; // The visual representation of the boss's health.

    // Game State
    private int score = 0;
    private int health = 3;
    private boolean gameStarted = false; // Flag to check if the main game loop should run.
    private int currentLevel = 1;
    private boolean isGameOver = false; // Flag to prevent multiple game over events.
    private boolean isLevelComplete = false; // Flag to prevent multiple level complete events.
    private UpgradeManager upgradeManager; // Manages permanent player upgrades.

    // Level Settings (Configured on a per-level basis)
    private int plotsToPlant;
    private int numberOfAliens;
    private int alienHealthPerAlien;
    private int shovelCount;
    private int alienMoveDelay;
    private float alienSpeedFactor;
    private int numberOfObstacles;
    private String obstacleType = "";
    private float movementFactor = 1.0f; // Multiplier for player speed (affected by power-ups/ice).

    // Boss State
    private TextView boss;
    private int maxBossHealth = 1;
    private int currentBossHealth = 1;
    private float bossInitialX = 0f;
    private long nextBossShootTime = 0;
    private long nextAlienSpawnTime = 0;

    // Power-Ups
    private final ArrayList<TextView> powerUpPool = new ArrayList<>(); // Object pool for power-ups.
    private final ArrayList<TextView> activePowerUps = new ArrayList<>(); // Active power-ups on screen.
    private static final int MAX_POWERUPS = 5;
    private boolean isShieldActive = false; // Flag for the shield power-up.
    private long shieldEndTime = 0; // Timestamp when the shield expires.
    private long speedBoostEndTime = 0;
    private float defaultMovementFactor = 1.0f;

    // Game Loop & Object Pooling
    private final Handler gameLoopHandler = new Handler(Looper.getMainLooper()); // The single, unified game loop handler.
    private Runnable gameLoopRunnable; // The runnable that executes the game loop.
    private static final int GAME_LOOP_DELAY = 33; // Delay for ~30 FPS.
    private final ArrayList<TextView> bulletPool = new ArrayList<>(); // Pool of reusable bullet objects.
    private final ArrayList<TextView> activeBullets = new ArrayList<>(); // Bullets currently on screen.
    private static final int MAX_BULLETS = 20;
    private final ArrayList<TextView> shovelPool = new ArrayList<>(); // Pool of reusable shovel objects.
    private final ArrayList<TextView> activeShovels = new ArrayList<>();  // Shovels currently on screen.
    private static final int MAX_SHOVELS = 5;

    // Entities
    private final ArrayList<TextView> plots = new ArrayList<>();
    private final ArrayList<TextView> aliens = new ArrayList<>();
    private final ArrayList<AlienState> alienStates = new ArrayList<>();
    private final ArrayList<TextView> obstacles = new ArrayList<>();

    // Helpers
    private final Random random = new Random();
    private int screenWidth, screenHeight, topBoundary, bottomBoundary;
    private static final int SAFE_PADDING = 120; // Padding to prevent objects from spawning too close.

    // Tutorial State
    private boolean isInTutorial = false;
    private int tutorialStep = 0;
    private TextView tutorialOverlayText;
    private Button tutorialNextButton;

    // Constants & Sounds
    private static final int TUTORIAL_LEVEL = 99;
    private static final int BOSS_LEVEL = 16;
    private MediaPlayer shootSound, plantSound, gameOverSound, hitSound, victorySound, shovelSound;

    // Continuous Movement
    private final Handler moveHandler = new Handler(Looper.getMainLooper()); // Handler for smooth player movement.
    private Runnable moveRunnable; // The runnable that moves the player.
    private boolean isMoving = false; // Flag to check if a movement button is being held down.
    private final int MOVE_DELAY_MS = 50;
    private int farmerSpeed = 30; // The base speed of the farmer.

    /**
     * Function: onCreate
     * Description: Standard Android Activity entry point. Initializes the activity, binds UI elements, loads resources,
     * sets up listeners, and determines if the game should start in normal or tutorial mode.
     * Expected Inputs: savedInstanceState (Bundle) - The activity's previously saved state.
     * Expected Outputs/Results: An initialized and running game screen, ready for player interaction.
     * Called By: Android OS.
     * Will Call: bindUI, loadSounds, getScreenDimensions, setupContinuousMovement, updateUI, getOnBackPressedDispatcher.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bgImage = findViewById(R.id.backgroundImage);

        // Initialize all UI elements and sound effects.
        bindUI();
        loadSounds();
        // Calculate screen dimensions and play area boundaries.
        getScreenDimensions();

        // Get the level number and tutorial mode flag from the calling activity.
        currentLevel = getIntent().getIntExtra("LEVEL_NUMBER", 1);
        boolean tutorialMode = getIntent().getBooleanExtra("TUTORIAL_MODE", false);


        // Set up the listener for the start button.
        startButton.setOnClickListener(v -> {
            startButton.setVisibility(View.GONE);
            startLevel(currentLevel);
        });

        // Set up listeners for the D-Pad buttons for continuous movement.
        setupContinuousMovement(leftBtn, -1, 0);
        setupContinuousMovement(rightBtn, 1, 0);
        setupContinuousMovement(upBtn, 0, -1);
        setupContinuousMovement(downBtn, 0, 1);

        // Set up the listener for the throw shovel button.
        throwBtn.setOnClickListener(v -> throwShovel());

        // Update the UI with initial values.
        updateUI();

        // Handle the back button press to return to the start screen.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent i = new Intent(MainActivity.this, StartActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    /**
     * Function: setupContinuousMovement
     * Description: Attaches an OnTouchListener to a D-Pad button to enable smooth, continuous movement while the button is held down.
     * Expected Inputs: button (ImageButton) - The UI button to attach the listener to.
     * dxMultiplier (int) - The direction of movement on the X-axis (-1 for left, 1 for right).
     * dyMultiplier (int) - The direction of movement on the Y-axis (-1 for up, 1 for down).
     * Expected Outputs/Results: The provided button will trigger the startMoving and stopMoving methods on press and release.
     * Called By: onCreate.
     * Will Call: startMoving, stopMoving.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupContinuousMovement(ImageButton button, int dxMultiplier, int dyMultiplier) {
        button.setOnTouchListener((v, event) -> {
            // Only allow movement if the game has started Validators.or during specific tutorial steps.
            if (!gameStarted && !(isInTutorial && tutorialStep >= 3 && tutorialStep <= 7)) {
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // When the button is pressed.
                    if (!isMoving) {
                        startMoving(dxMultiplier, dyMultiplier); // Start the movement loop.
                    }
                    v.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP: // When the button is released.
                case MotionEvent.ACTION_CANCEL:
                    stopMoving(); // Stop the movement loop.
                    v.setPressed(false);
                    v.performClick(); // Ensure accessibility services can interact.
                    return true;
            }
            return false;
        });
    }

    /**
     * Function: startMoving
     * Description: Begins the continuous movement loop by posting a recurring Runnable to the moveHandler.
     * Expected Inputs: dxMultiplier (int), dyMultiplier (int) - The directional multipliers for movement.
     * Expected Outputs/Results: The moveRunnable begins executing, repeatedly calling moveFarmer.
     * Called By: setupContinuousMovement.
     * Will Call: moveFarmer.
     */
    private void startMoving(int dxMultiplier, int dyMultiplier) {
        isMoving = true;
        moveRunnable = new Runnable() {
            @Override
            public void run() {
                // Calculate the movement delta.
                float dx = dxMultiplier * farmerSpeed;
                float dy = dyMultiplier * farmerSpeed;
                moveFarmer(dx, dy); // Move the farmer.

                // In the tutorial, show the next button after the player has moved.
                if (isInTutorial && tutorialStep == 3) {
                    tutorialNextButton.setVisibility(View.VISIBLE);
                }

                // If the button is still held down, post the next movement step.
                if (isMoving) {
                    moveHandler.postDelayed(this, MOVE_DELAY_MS);
                }
            }
        };
        moveHandler.post(moveRunnable); // Start the loop.
    }

    /**
     * Function: stopMoving
     * Description: Stops the continuous movement loop by removing the Runnable from the moveHandler's queue.
     * Expected Inputs: None.
     * Expected Outputs/Results: The moveRunnable stops executing.
     * Called By: setupContinuousMovement.
     * Will Call: None.
     */
    private void stopMoving() {
        isMoving = false;
        moveHandler.removeCallbacks(moveRunnable); // Stop the loop.
    }

    /**
     * Function: bindUI
     * Description: Links class member variables to their corresponding View objects defined in the XML layout.
     * Expected Inputs: None.
     * Expected Outputs/Results: All UI-related class fields are populated with references to their views.
     * Called By: onCreate.
     * Will Call: findViewById.
     */
    private void bindUI() {
        mainLayout = findViewById(R.id.mainLayout);
        playAreaDivider = findViewById(R.id.playAreaDivider);
        scoreText = findViewById(R.id.scoreText);
        healthText = findViewById(R.id.healthText);
        farmer = findViewById(R.id.farmer);
        startButton = findViewById(R.id.startButton);
        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);
        upBtn = findViewById(R.id.upBtn);
        downBtn = findViewById(R.id.downBtn);
        throwBtn = findViewById(R.id.throwBtn);
        tutorialOverlayText = findViewById(R.id.tutorialOverlayText);
        tutorialNextButton = findViewById(R.id.tutorialNextButton);
        bossHealthLayout = findViewById(R.id.bossHealthLayout);
        bossHealthBarVisual = findViewById(R.id.bossHealthBar);
        if (bossHealthLayout != null) bossHealthLayout.setVisibility(View.GONE);
    }

    /**
     * Function: loadSounds
     * Description: Initializes all MediaPlayer objects for the various sound effects used in the game.
     * Expected Inputs: None.
     * Expected Outputs/Results: All sound-related MediaPlayer fields are initialized.
     * Called By: onCreate.
     * Will Call: MediaPlayer.create.
     */
    private void loadSounds() {
        try {
            shootSound = MediaPlayer.create(this, R.raw.shoot);
            plantSound = MediaPlayer.create(this, R.raw.plant);
            gameOverSound = MediaPlayer.create(this, R.raw.gameover);
            hitSound = MediaPlayer.create(this, R.raw.hit);
            victorySound = MediaPlayer.create(this, R.raw.victory);
            shovelSound = MediaPlayer.create(this, R.raw.shovel);
        } catch (Exception ignored) {}
    }

    /**
     * Function: getScreenDimensions
     * Description: Gets the screen's physical dimensions and uses a ViewTreeObserver to reliably calculate the
     * top and bottom boundaries of the gameplay area after the layout has been fully drawn.
     * Expected Inputs: None.
     * Expected Outputs/Results: Populates screenWidth, screenHeight, topBoundary, and bottomBoundary variables.
     * Called By: onCreate.
     * Will Call: getWindowManager, Display.getSize, mainLayout.getViewTreeObserver.
     */
    private void getScreenDimensions() {Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // Use a ViewTreeObserver to wait for the layout pass to complete.
        ViewTreeObserver viewTreeObserver = mainLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Remove the listener to prevent it from being called multiple times.
                    mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Now that the layout is drawn, we can safely get the boundaries.
                    topBoundary = healthText.getBottom() + 20; // Top of play area is below the health text.
                    bottomBoundary = (int) playAreaDivider.getY(); // Bottom of play area is above the D-Pad.

                    // If in tutorial mode, start the tutorial now that boundaries are set.
                    boolean tutorialMode = getIntent().getBooleanExtra("TUTORIAL_MODE", false);
                    if (tutorialMode) {
                        startTutorial();
                    }
                }
            });
        }
    }

    /**
     * Function: configureLevel
     * Description: Sets all game parameters for a specific level, including number of plots, aliens, health,
     * speed, and applies any permanent upgrades purchased by the player.
     * Expected Inputs: level (int) - The level number to configure.
     * Expected Outputs/Results: All level-specific class variables are set to their correct values for the upcoming level.
     * Called By: startLevel.
     * Will Call: upgradeManager.getUpgradeLevel.
     */
    private void configureLevel(int level) {
        if (upgradeManager == null) {
            upgradeManager = new UpgradeManager(this);
        }

        // Load permanent upgrades from SharedPreferences.
        int healthUpgrade = upgradeManager.getUpgradeLevel(UpgradeManager.getHealthKey());
        int shovelUpgrade = upgradeManager.getUpgradeLevel(UpgradeManager.getShovelKey());
        int speedUpgrade = upgradeManager.getUpgradeLevel(UpgradeManager.getSpeedKey());

        // Define base stats.
        int baseHealth = 3;
        int baseShovelCount = 0;
        float baseSpeed = 30;

        // Apply upgrades to base stats.
        health = baseHealth + healthUpgrade;
        int startingShovels = baseShovelCount + shovelUpgrade;
        farmerSpeed = (int) (baseSpeed + (speedUpgrade * 5));

        // Set default values for the level.
        plotsToPlant = 5;
        numberOfAliens = 1;
        alienHealthPerAlien = 1;
        shovelCount = startingShovels;
        alienSpeedFactor = 1.0f;
        alienMoveDelay = 2000;
        obstacleType = "";
        numberOfObstacles = 0;
        movementFactor = 1.0f;
        maxBossHealth = 1;
        currentBossHealth = 1;

        // Use a switch statement to define settings for each unique level.
        switch (level) {
            case TUTORIAL_LEVEL:
                bgImage.setImageResource(R.drawable.green_planet_bg);
                plotsToPlant = 2;
                numberOfAliens = 0;
                shovelCount = 2 + shovelUpgrade;
                alienSpeedFactor = 0.5f;
                alienMoveDelay = 4000;
                break;
            case 1:
                bgImage.setImageResource(R.drawable.green_planet_bg);
                plotsToPlant = 5;
                numberOfAliens = 1;
                alienSpeedFactor = 0.6f;
                alienMoveDelay = 2700;
                break;
            case 2:
                bgImage.setImageResource(R.drawable.green_planet_bg);
                plotsToPlant = 5;
                numberOfAliens = 1;
                alienSpeedFactor = 0.7f;
                alienMoveDelay = 2600;
                break;
            case 3:
                bgImage.setImageResource(R.drawable.green_planet_bg);
                plotsToPlant = 6;
                numberOfAliens = 2;
                alienSpeedFactor = 0.8f;
                alienMoveDelay = 2500;
                break;
            case 4:
                bgImage.setImageResource(R.drawable.green_planet_bg);
                plotsToPlant = 6;
                numberOfAliens = 2;
                alienSpeedFactor = 0.8f;
                alienMoveDelay = 2520;
                break;
            case 5:
                bgImage.setImageResource(R.drawable.green_planet_bg);
                plotsToPlant = 7;
                numberOfAliens = 2;
                alienSpeedFactor = 0.8f;
                alienMoveDelay = 2500;
                break;
            case 6:
                bgImage.setImageResource(R.drawable.purple_planet_bg);
                plotsToPlant = 8;
                numberOfAliens = 2;
                shovelCount = 3 + shovelUpgrade;
                alienSpeedFactor = 1.3f;
                alienMoveDelay = 1900;
                obstacleType = "🪨";
                numberOfObstacles = 2;
                break;
            case 7:
                bgImage.setImageResource(R.drawable.purple_planet_bg);
                plotsToPlant = 8;
                numberOfAliens = 2;
                shovelCount = 3 + shovelUpgrade;
                alienSpeedFactor = 1.3f;
                alienMoveDelay = 1800;
                obstacleType = "🪨";
                numberOfObstacles = 3;
                break;
            case 8:
                bgImage.setImageResource(R.drawable.purple_planet_bg);
                plotsToPlant = 8;
                numberOfAliens = 2;
                shovelCount = 3 + shovelUpgrade;
                alienSpeedFactor = 1.3f;
                alienMoveDelay = 1750;
                obstacleType = "🪨";
                numberOfObstacles = 3;
                break;
            case 9:
                bgImage.setImageResource(R.drawable.purple_planet_bg);
                plotsToPlant = 9;
                numberOfAliens = 3;
                shovelCount = 3 + shovelUpgrade;
                alienSpeedFactor = 1.4f;
                alienMoveDelay = 1700;
                obstacleType = "🪨";
                numberOfObstacles = 4;
                break;
            case 10:
                bgImage.setImageResource(R.drawable.purple_planet_bg);
                plotsToPlant = 9;
                numberOfAliens = 3;
                alienHealthPerAlien = 2;
                shovelCount = 5 + shovelUpgrade;
                alienSpeedFactor = 1.4f;
                alienMoveDelay = 1650;
                obstacleType = "🪨";
                numberOfObstacles = 3;
                break;
            case 11:
                bgImage.setImageResource(R.drawable.ice_planet_bg);
                plotsToPlant = 9;
                numberOfAliens = 3;
                alienHealthPerAlien = 2;
                shovelCount = 7 + shovelUpgrade;
                alienSpeedFactor = 1.1f;
                alienMoveDelay = 1500;
                obstacleType = "🧊";
                numberOfObstacles = 3;
                movementFactor = 0.8f;
                break;
            case 12:
                bgImage.setImageResource(R.drawable.ice_planet_bg);
                plotsToPlant = 9;
                numberOfAliens = 4;
                alienHealthPerAlien = 2;
                shovelCount = 10 + shovelUpgrade;
                alienSpeedFactor = 1.1f;
                alienMoveDelay = 1450;
                obstacleType = "🧊";
                numberOfObstacles = 3;
                movementFactor = 0.8f;
                break;
            case 13:
                bgImage.setImageResource(R.drawable.ice_planet_bg);
                plotsToPlant = 9;
                numberOfAliens = 4;
                alienHealthPerAlien = 2;
                shovelCount = 10 + shovelUpgrade;
                alienSpeedFactor = 1.1f;
                alienMoveDelay = 1370;
                obstacleType = "🧊";
                numberOfObstacles = 3;
                movementFactor = 0.8f;
                break;
            case 14:
                bgImage.setImageResource(R.drawable.ice_planet_bg);
                plotsToPlant = 9;
                numberOfAliens = 4;
                alienHealthPerAlien = 2;
                shovelCount = 12 + shovelUpgrade;
                alienSpeedFactor = 1.0f;
                alienMoveDelay = 1300;
                obstacleType = "🧊";
                numberOfObstacles = 3;
                movementFactor = 0.8f;
                break;
            case 15:
                bgImage.setImageResource(R.drawable.ice_planet_bg);
                plotsToPlant = 9;
                numberOfAliens = 5;
                alienHealthPerAlien = 2;
                shovelCount = 12 + shovelUpgrade;
                alienSpeedFactor = 1.0f;
                alienMoveDelay = 1200;
                obstacleType = "🧊";
                numberOfObstacles = 3;
                movementFactor = 0.8f;
                break;
            case BOSS_LEVEL:
                bgImage.setImageResource(R.drawable.ice_planet_bg);
                plotsToPlant = 0;
                numberOfAliens = 0;
                shovelCount = 20 + shovelUpgrade;
                movementFactor = 1.0f;
                maxBossHealth = 15;
                currentBossHealth = maxBossHealth;
                break;
        }
    }

    /**
     * Function: startLevel
     * Description: Initializes a new level by resetting game state, configuring the level parameters,
     * initializing all object pools, and starting the main game loop.
     * Expected Inputs: level (int) - The level number to start.
     * Expected Outputs/Results: A new game session is started for the specified level.
     * Called By: onCreate, startTutorial.
     * Will Call: configureLevel, clearAllEntities, initBulletPool, initShovelPool, initPowerUpPool, updateUI, spawnBoss, generatePlots, spawnAliens, startGameLoop.
     */
    private void startLevel(int level) {
        configureLevel(level); // Set up the level's parameters.

        // Reset all game state flags.
        isGameOver = false;
        isLevelComplete = false;
        gameStarted = true;
        score = 0;

        // Reset temporary power-ups.
        isShieldActive = false;
        shieldEndTime = 0;
        speedBoostEndTime = 0;
        defaultMovementFactor = movementFactor;

        // Clear all entities from the previous session.
        clearAllEntities();
        // Initialize the object pools for this level.
        initBulletPool();
        initShovelPool();
        initPowerUpPool();
        updateUI();

        // Spawn the boss or the regular level entities.
        if (currentLevel == BOSS_LEVEL) {
            spawnBoss();
        } else {
            generatePlots();
            if (numberOfObstacles > 0) {
                generateObstacles();
            }
            spawnAliens(numberOfAliens);
        }
        // Start the main game loop.
        startGameLoop();
    }

    /**
     * Function: initBulletPool
     * Description: Pre-allocates a set number of TextViews to be used as bullets, improving performance by avoiding object creation during gameplay.
     * Expected Inputs: None.
     * Expected Outputs/Results: The bulletPool is populated with invisible, reusable TextViews.
     * Called By: startLevel.
     * Will Call: mainLayout.removeView, mainLayout.addView.
     */
    private void initBulletPool() {
        // Remove any old bullets from the layout.
        for (TextView bullet : bulletPool) {
            mainLayout.removeView(bullet);
        }
        bulletPool.clear();
        activeBullets.clear();
        // Create the new pool of bullet objects.
        for (int i = 0; i < MAX_BULLETS; i++) {
            TextView bullet = new TextView(this);
            bullet.setText("⚡");
            bullet.setTextSize(22f);
            bullet.setVisibility(View.GONE); // Start invisible.
            mainLayout.addView(bullet); // Add to the layout.
            bulletPool.add(bullet); // Add to the pool list.
        }
    }

    /**
     * Function: initShovelPool
     * Description: Pre-allocates a set number of TextViews to be used as shovels.
     * Expected Inputs: None.
     * Expected Outputs/Results: The shovelPool is populated with invisible, reusable TextViews.
     * Called By: startLevel.
     * Will Call: mainLayout.removeView, mainLayout.addView.
     */
    private void initShovelPool() {
        // Remove any old shovels from the layout.
        for (TextView shovel : shovelPool) {
            mainLayout.removeView(shovel);
        }
        shovelPool.clear();
        activeShovels.clear();
        // Create the new pool of shovel objects.
        for (int i = 0; i < MAX_SHOVELS; i++) {
            TextView shovel = new TextView(this);
            shovel.setText("🪓");
            shovel.setTextSize(26f);
            shovel.setVisibility(View.GONE); // Start invisible.
            mainLayout.addView(shovel); // Add to the layout.
            shovelPool.add(shovel); // Add to the pool list.
        }
    }

    /**
     * Function: initPowerUpPool
     * Description: Pre-allocates a set number of TextViews to be used as power-up icons.
     * Expected Inputs: None.
     * Expected Outputs/Results: The powerUpPool is populated with invisible, reusable TextViews.
     * Called By: startLevel.
     * Will Call: mainLayout.removeView, mainLayout.addView.
     */
    private void initPowerUpPool() {
        // Remove any old power-ups from the layout.
        for (TextView powerUp : powerUpPool) {
            mainLayout.removeView(powerUp);
        }
        powerUpPool.clear();
        activePowerUps.clear();
        // Create the new pool of power-ups objects.
        for (int i = 0; i < MAX_POWERUPS; i++) {
            TextView powerUp = new TextView(this);
            powerUp.setTextSize(30f);
            powerUp.setVisibility(View.GONE); // Start invisible.
            mainLayout.addView(powerUp); // Add to the layout.
            powerUpPool.add(powerUp); // Add to the pool list.
        }
    }

    /**
     * Function: spawnPowerUp
     * Description: Spawns a random power-up (Shield or Speed Boost) at a specified location.
     * Expected Inputs: x (float), y (float) - The coordinates where the power-up should appear.
     * Expected Outputs/Results: A power-up icon becomes visible on screen and is added to the activePowerUps list.
     * Called By: handleAlienDeath.
     * Will Call: None.
     */
    private void spawnPowerUp(float x, float y) {
        if (powerUpPool.isEmpty()) return; // Don't spawn if the pool is empty.
        TextView powerUpView = powerUpPool.remove(powerUpPool.size() - 1); // Get a power-up from the pool.
        // Randomly choose between a shield and a speed boost.
        if (random.nextBoolean()) {
            powerUpView.setText("🛡️");
            powerUpView.setTag("SHIELD");
        } else {
            powerUpView.setText("💨");
            powerUpView.setTag("SPEED_BOOST");
        }
        powerUpView.setX(x);
        powerUpView.setY(y);
        powerUpView.setVisibility(View.VISIBLE); // Make it visible.
        activePowerUps.add(powerUpView); // Add it to the active list.
    }

    /**
     * Function: startGameLoop
     * Description: Starts the main game loop, which repeatedly calls the updateGame method.
     * Expected Inputs: None.
     * Expected Outputs/Results: The game loop begins running at a fixed rate.
     * Called By: startLevel.
     * Will Call: gameLoopHandler.post.
     */
    private void startGameLoop() {
        gameLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!gameStarted) return; // Stop the loop if the game is over.
                updateGame(); // Call the main update method.
                // Schedule the next frame.
                gameLoopHandler.postDelayed(this, GAME_LOOP_DELAY);
            }
        };
        gameLoopHandler.post(gameLoopRunnable); // Start the loop.
    }

    /**
     * Function: updateGame
     * Description: The central update method for the entire game. Called on every frame by the game loop.
     * Expected Inputs: None.
     * Expected Outputs/Results: All active game entities are updated for the current frame.
     * Called By: gameLoopRunnable.
     * Will Call: updatePowerUps, checkPowerUpExpirations, updateBullets, updateShovels, updateAliensAndBoss, respawnAliens.
     */
    private void updateGame() {
        long currentTime = System.currentTimeMillis(); // Get the time for this frame.
        // Update all game subsystems.
        updatePowerUps();
        checkPowerUpExpirations(currentTime);
        updateBullets();
        updateShovels();
        updateAliensAndBoss(currentTime);
        respawnAliens(currentTime);
    }

    /**
     * Function: updatePowerUps
     * Description: Checks for collisions between the farmer and any active power-ups on the screen.
     * Expected Inputs: None.
     * Expected Outputs/Results: If a collision occurs, the power-up is activated and removed from the screen.
     * Called By: updateGame.
     * Will Call: collision, activatePowerUp.
     */
    private void updatePowerUps() {
        // Use an iterator to safely remove items while looping.
        Iterator<TextView> iterator = activePowerUps.iterator();
        while (iterator.hasNext()) {
            TextView powerUp = iterator.next();
            // Check for collision between the farmer and the power-up.
            if (collision(farmer, powerUp)) {
                activatePowerUp((String) powerUp.getTag());
                powerUp.setVisibility(View.GONE); // Hide the power-up.
                powerUpPool.add(powerUp); // Return it to the object pool.
                iterator.remove(); // Remove it from the active list.
            }
        }
    }

    /**
     * Function: activatePowerUp
     * Description: Applies the effect of a collected power-up, such as activating a shield or a speed boost.
     * Expected Inputs: type (String) - The tag of the power-up to activate ("SHIELD" or "SPEED_BOOST").
     * Expected Outputs/Results: The player's state is modified (e.g., shield becomes active) for a limited duration.
     * Called By: updatePowerUps.
     * Will Call: Toast.makeText.
     */
    private void activatePowerUp(String type) {
        long currentTime = System.currentTimeMillis();
        switch (type) {
            case "SHIELD":
                isShieldActive = true; // Set the shield flag.
                shieldEndTime = currentTime + 5000; // Set expiration time to 5 seconds from now.
                farmer.setText("🛡️👨‍🌾"); // Change farmer appearance to show shield.
                Toast.makeText(this, "Shield Active!", Toast.LENGTH_SHORT).show();
                break;
            case "SPEED_BOOST":
                movementFactor = defaultMovementFactor * 1.5f; // Increase movement speed by 50%.
                speedBoostEndTime = currentTime + 5000; // Set expiration time to 5 seconds from now.
                Toast.makeText(this, "Speed Boost!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Function: checkPowerUpExpirations
     * Description: Checks if any active power-ups have expired and removes their effects.
     * Expected Inputs: currentTime (long) - The current system time.
     * Expected Outputs/Results: Player state is returned to normal if a power-up's duration has elapsed.
     * Called By: updateGame.
     * Will Call: None.
     */
    private void checkPowerUpExpirations(long currentTime) {
        // Check if the shield is active and its time has run out.
        if (isShieldActive && currentTime > shieldEndTime) {
            isShieldActive = false; // Deactivate the shield.
            shieldEndTime = 0;
            farmer.setText("👨‍🌾"); // Revert farmer appearance.
        }
        // Check if the speed boost is active and its time has run out.
        if (movementFactor > defaultMovementFactor && currentTime > speedBoostEndTime) {
            movementFactor = defaultMovementFactor; // Revert to default speed.
            speedBoostEndTime = 0;
        }
    }

    /**
     * Function: updateBullets
     * Description: Updates the position of all active bullets and checks for collisions.
     * Expected Inputs: None.
     * Expected Outputs/Results: Bullets move across the screen and are recycled on collision or when off-screen.
     * Called By: updateGame.
     * Will Call: recycleBullet, collision, gameOver.
     */
    private void updateBullets() {
        // Use an iterator to safely remove bullets while looping.
        Iterator<TextView> iterator = activeBullets.iterator();
        while (iterator.hasNext()) {
            TextView bullet = iterator.next();

            // Get the bullet's velocity from its state.
            BulletState state = (BulletState) bullet.getTag();
            if (state == null) continue;

            // Update the bullet's position.
            bullet.setX(bullet.getX() + state.stepX);
            bullet.setY(bullet.getY() + state.stepY);

            // Recycle the bullet if it goes off-screen.
            if (bullet.getY() > screenHeight || bullet.getY() < 0 || bullet.getX() < 0 || bullet.getX() > screenWidth) {
                recycleBullet(bullet, iterator);
                continue;
            }

            // Recycle the bullet if it hits an obstacle.
            for (TextView obstacle : obstacles) {
                if (collision(bullet, obstacle)) {
                    recycleBullet(bullet, iterator);
                    break;
                }
            }
            if (bullet.getVisibility() == View.GONE) continue; // Skip if already recycled.

            // Check for collision with the farmer.
            if (collision(bullet, farmer)) {
                if (isShieldActive) { // If shield is active, block the hit.
                    recycleBullet(bullet, iterator);
                } else { // Otherwise, take damage.
                    recycleBullet(bullet, iterator);
                    health--;
                    if (hitSound != null) hitSound.start();
                    updateUI();
                    if (health <= 0) gameOver(); // Check for game over.
                }
            }
        }
    }

    /**
     * Function: updateShovels
     * Description: Updates the position of all active shovels and checks for collisions.
     * Expected Inputs: None.
     * Expected Outputs/Results: Shovels move across the screen and are recycled on collision or when off-screen.
     * Called By: updateGame.
     * Will Call: recycleShovel, collision, handleAlienDeath, completeLevel.
     */
    private void updateShovels() {
        // Use an iterator to safely remove shovels while looping.
        Iterator<TextView> iterator = activeShovels.iterator();
        while (iterator.hasNext()) {
            TextView shovel = iterator.next();
            // Move the shovel upwards.
            shovel.setY(shovel.getY() - 20);

            // Recycle the shovel if it goes off-screen.
            if (shovel.getY() < 0) {
                recycleShovel(shovel, iterator);
                continue;
            }

            // Check for collision with the boss.
            if (currentLevel == BOSS_LEVEL && boss != null && currentBossHealth > 0 && collision(shovel, boss)) {
                recycleShovel(shovel, iterator); // Recycle the shovel on hit.
                currentBossHealth--; // Decrement boss health.
                updateBossHealthUI(); // Update the health bar.
                if (currentBossHealth <= 0) { // If boss is defeated.
                    score += 10000; // Add bonus score.
                    updateUI();
                    completeLevel(); // Complete the level (and the game).
                }
                continue;
            }

            // If not the boss level, check for collision with regular aliens.
            if (currentLevel != BOSS_LEVEL) {
                for (TextView alien : aliens) {
                    if (collision(shovel, alien)) {
                        AlienState state = (AlienState) alien.getTag();
                        state.currentHits++; // Increment the alien's hit count.
                        // If the alien has taken enough hits, handle its death.
                        if (state.currentHits >= alienHealthPerAlien) {
                            handleAlienDeath(state);
                        } else {
                            // If the alien is damaged but not defeated, change its appearance.
                            if (alien.getText().equals("👾")) {
                                alien.setText("🔴");
                            }
                        }
                        recycleShovel(shovel, iterator); // Recycle the shovel.
                        break; // A shovel can only hit one alien.
                    }
                }
            }
        }
    }

    /**
     * Function: updateAliensAndBoss
     * Description: Updates the position and attack patterns for all active aliens and the boss.
     * Expected Inputs: currentTime (long) - The current system time.
     * Expected Outputs/Results: Aliens and the boss move and shoot based on their individual timers and AI logic.
     * Called By: updateGame.
     * Will Call: ObjectAnimator.start, shootBullet, collision, gameOver.
     */
    private void updateAliensAndBoss(long currentTime) {
        // Update all regular aliens.
        for (AlienState state : alienStates) {
            // Check if it's time for the alien to move.
            if (currentTime >= state.nextMoveTime) {
                long animationDuration = 2500;
                int alienWidth = 100;
                int maxAlienY = bottomBoundary - state.alienView.getHeight() - 10;
                int minY = topBoundary;

                // Calculate a new random position within the play area.
                float newX = random.nextInt(screenWidth - alienWidth);
                float newY = minY + random.nextInt(Math.max(minY, maxAlienY - minY));

                // Animate the alien's movement smoothly to the new position.
                ObjectAnimator animX = ObjectAnimator.ofFloat(state.alienView, "x", state.alienView.getX(), newX);
                ObjectAnimator animY = ObjectAnimator.ofFloat(state.alienView, "y", state.alienView.getY(), newY);
                animX.setDuration(animationDuration);
                animY.setDuration(animationDuration);
                animX.setInterpolator(new LinearInterpolator());
                animY.setInterpolator(new LinearInterpolator());
                animX.start();
                animY.start();
                // Set the time for the next move.
                long delayBetweenMoves = (long) (alienMoveDelay / alienSpeedFactor);
                state.nextMoveTime = currentTime + delayBetweenMoves;
            }
            // Check if it's time for the alien to shoot.
            if (currentTime >= state.nextShootTime) {
                shootBullet(state.alienView, farmer.getX() + farmer.getWidth() / 2f); // Shoot a bullet towards the farmer.
                state.nextShootTime = currentTime + 2500 + random.nextInt(1500); // Set the time for the next shot.
            }
        }

        // If the boss exists and is alive, update it.
        if (boss != null && currentBossHealth > 0) {
            // The boss gets faster as its health decreases.
            float speedMultiplier = 1.0f + (1.0f - ((float)currentBossHealth / maxBossHealth)) * 1.5f;
            // Calculate a target X that tracks the farmer.
            float targetX = farmer.getX() - (boss.getWidth() / 2f) + (farmer.getWidth() / 2f);
            float currentX = boss.getX();
            // Smoothly move the boss towards the target X.
            float newX = currentX + ((targetX - currentX) * 0.02f * speedMultiplier);
            boss.setX(newX);

            // Check for collision between the farmer and the boss.
            if (collision(farmer, boss)) {
                if (!isShieldActive) {
                    health--;
                    if (hitSound != null) hitSound.start();
                    updateUI();
                    if (health <= 0) gameOver();
                }
            }

            // Check if it's time for the boss to shoot.
            if (currentTime >= nextBossShootTime) {
                float farmerCenterX = farmer.getX() + farmer.getWidth() / 2f;
                // Shoot a 3-bullet spread pattern.
                shootBullet(boss, farmerCenterX - 150);
                shootBullet(boss, farmerCenterX);
                shootBullet(boss, farmerCenterX + 150);
                // The boss shoots faster as its health decreases.
                long nextBurstDelay = 1800 + random.nextInt(1000);
                nextBurstDelay *= ((float)currentBossHealth / maxBossHealth);
                nextBurstDelay = Math.max(800, nextBurstDelay);
                nextBossShootTime = currentTime + nextBurstDelay;
            }
        }
    }

    /**
     * Function: recycleBullet
     * Description: Returns an inactive bullet TextView to the object pool so it can be reused.
     * Expected Inputs: bullet (TextView) - The bullet to recycle.
     * iterator (Iterator) - The list iterator, to safely remove the bullet while iterating.
     * Expected Outputs/Results: The bullet is hidden and returned to the bulletPool.
     * Called By: updateBullets.
     * Will Call: None.
     */
    private void recycleBullet(TextView bullet, Iterator<TextView> iterator) {
        bullet.setVisibility(View.GONE); // Make it invisible.
        bullet.setTag(null);  // Clear its state.
        if (iterator != null) {
            iterator.remove(); // Safely remove from the active list.
        } else {
            activeBullets.remove(bullet);
        }
        bulletPool.add(bullet); // Add it back to the pool.
    }

    /**
     * Function: recycleShovel
     * Description: Returns an inactive shovel TextView to the object pool.
     * Expected Inputs: shovel (TextView), iterator (Iterator).
     * Expected Outputs/Results: The shovel is hidden and returned to the shovelPool.
     * Called By: updateShovels.
     * Will Call: None.
     */
    private void recycleShovel(TextView shovel, Iterator<TextView> iterator) {
        shovel.setVisibility(View.GONE);
        if (iterator != null) {
            iterator.remove();
        } else {
            activeShovels.remove(shovel);
        }
        shovelPool.add(shovel);
    }

    /**
     * Function: isAreaOccupied
     * Description: Checks if a proposed rectangular area overlaps with any existing entities, with padding.
     * Expected Inputs: Proposed X, Y, width, height (floats), and a list of entities to check against.
     * Expected Outputs/Results: boolean - true if there is an overlap, false otherwise.
     * Called By: generateObstacles, generatePlots.
     * Will Call: None.
     */
    private boolean isAreaOccupied(float proposedX, float proposedY, float proposedWidth, float proposedHeight, ArrayList<TextView> existingEntities) {
        // Loop through all entities to check against.
        for (TextView existing : existingEntities) {
            float eX = existing.getX();
            float eY = existing.getY();
            float eWidth = existing.getWidth();
            float eHeight = existing.getHeight();
            // Check for overlap with a safe padding area around each entity.
            if (proposedX < eX + eWidth + SAFE_PADDING &&
                    proposedX + proposedWidth + SAFE_PADDING > eX &&
                    proposedY < eY + eHeight + SAFE_PADDING &&
                    proposedY + proposedHeight + SAFE_PADDING > eY) {
                return true; // The area is occupied.
            }
        }
        return false; // The area is clear.
    }

    /**
     * Function: spawnAliens
     * Description: A utility function to spawn a specified number of new aliens.
     * Expected Inputs: count (int) - The number of aliens to create.
     * Expected Outputs/Results: The specified number of aliens are added to the game world.
     * Called By: startLevel, showNextTutorialStep.
     * Will Call: spawnAlien.
     */
    private void spawnAliens(int count) {
        for (int i = 0; i < count; i++) {
            spawnAlien();
        }
    }

    /**
     * Function: respawnAliens
     * Description: Checks if an alien needs to be respawned (if the current count is below the level's quota) and does so after a delay.
     * Expected Inputs: currentTime (long) - The current system time.
     * Expected Outputs/Results: A new alien may be spawned if conditions are met.
     * Called By: updateGame.
     * Will Call: spawnAlien.
     */
    private void respawnAliens(long currentTime) {
        // Only respawn in non-boss levels and if there are fewer aliens than required.
        if (currentLevel != BOSS_LEVEL && aliens.size() < numberOfAliens) {
            // If the respawn timer isn't set, set it now.
            if (nextAlienSpawnTime == 0) {
                nextAlienSpawnTime = currentTime + 5000; // 5-second delay.
            }
            // If the current time has passed the respawn time.
            if (currentTime >= nextAlienSpawnTime) {
                spawnAlien(); // Spawn a new alien.
                nextAlienSpawnTime = 0; // Reset the timer.
            }
        }
    }

    /**
     * Function: spawnAlien
     * Description: Creates a single new alien entity at a random position within the play area.
     * Expected Inputs: None.
     * Expected Outputs/Results: A new alien is added to the screen and the internal entity lists.
     * Called By: spawnAliens, respawnAliens.
     * Will Call: mainLayout.addView.
     */
    private void spawnAlien() {
        TextView alien = new TextView(this);
        // Set the alien's appearance based on its health.
        if (alienHealthPerAlien > 1) {
            alien.setText("👾"); // Stronger alien.
        } else {
            alien.setText("👽"); // Weaker alien.
        }
        alien.setTextSize(40f);
        // Calculate a random spawn position.
        int alienWidth = 100;
        int maxAlienY = bottomBoundary - farmer.getHeight() - 10;
        int minY = topBoundary;
        alien.setX(random.nextInt(screenWidth - alienWidth));
        alien.setY(minY + random.nextInt(Math.max(minY, maxAlienY - minY)));
        // Create a new state object for the alien.
        AlienState alienState = new AlienState(alien, System.currentTimeMillis(), alienMoveDelay, 2500 + random.nextInt(1500));
        alien.setTag(alienState); // Store the state in the alien's tag.
        mainLayout.addView(alien); // Add the alien to the screen.
        aliens.add(alien); // Add to the list of active aliens.
        alienStates.add(alienState); // Add to the list of alien states.
    }

    /**
     * Function: generateObstacles
     * Description: Creates a set number of randomly placed obstacles (rocks or ice) within the play area, avoiding overlaps.
     * Expected Inputs: None.
     * Expected Outputs/Results: Obstacles are added to the screen.
     * Called By: startLevel.
     * Will Call: isAreaOccupied.
     */
    private void generateObstacles() {
        // Clear any old obstacles.
        for (TextView obstacle : obstacles) mainLayout.removeView(obstacle);
        obstacles.clear();
        // Define the vertical range for spawning.
        int minY = topBoundary + 100;
        int maxY = bottomBoundary - 100;
        int obstacleVerticalRange = maxY - minY;
        if (obstacleVerticalRange <= 50) return; // Don't spawn if the area is too small.
        // Create a temporary list of all existing entities to check for overlaps.
        ArrayList<TextView> existingViews = new ArrayList<>(aliens);
        existingViews.addAll(plots);
        existingViews.add(farmer);
        float obstacleW = 120f;
        float obstacleH = 120f;
        // Create the specified number of obstacles.
        for (int i = 0; i < numberOfObstacles; i++) {
            TextView obstacle = new TextView(this);
            obstacle.setText(obstacleType);
            obstacle.setTextSize(45f);
            float newX, newY;
            int attempts = 0; // To prevent an infinite loop.
            // Try to find a clear spot to spawn the obstacle.
            do {
                newX = random.nextInt(screenWidth - (int) obstacleW);
                newY = minY + random.nextInt(obstacleVerticalRange);
                attempts++;
                if (attempts > 500) return; // Give up if a spot can't be found.
            } while (isAreaOccupied(newX, newY, obstacleW, obstacleH, obstacles) || isAreaOccupied(newX, newY, obstacleW, obstacleH, existingViews));
            obstacle.setX(newX);
            obstacle.setY(newY);
            mainLayout.addView(obstacle); // Add the obstacle to the screen.
            obstacles.add(obstacle); // Add to the list of obstacles.
        }
    }

    /**
     * Function: shootBullet
     * Description: Creates a homing bullet that moves towards a target X-coordinate. This is used by both aliens and the boss.
     * Expected Inputs: shooter (TextView) - The entity firing the bullet.
     *                  targetX (float) - The X-coordinate the bullet should travel towards.
     * Expected Outputs/Results: A bullet from the object pool is activated, given a velocity, and made visible on screen.
     * Called By: updateAliensAndBoss.
     * Will Call: None.
     */
    private void shootBullet(TextView shooter, float targetX) {
        if (bulletPool.isEmpty()) return; // Don't shoot if the pool is empty.
        TextView bullet = bulletPool.remove(bulletPool.size() - 1); // Get a bullet from the pool.
        // Position the bullet at the shooter's location.
        bullet.setX(shooter.getX() + shooter.getWidth() / 2f);
        bullet.setY(shooter.getY() + shooter.getHeight());
        if (shootSound != null) shootSound.start();
        // Calculate the direction towards the farmer.
        float targetY = farmer.getY() + farmer.getHeight() / 2f;
        float deltaX = targetX - bullet.getX();
        float deltaY = targetY - bullet.getY();
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        float speed = 12f;
        // Calculate the velocity components.
        float stepX = (deltaX / distance) * speed;
        float stepY = (deltaY / distance) * speed;
        bullet.setTag(new BulletState(stepX, stepY)); // Store the velocity in the bullet's tag.
        bullet.setVisibility(View.VISIBLE); // Make the bullet visible.
        activeBullets.add(bullet); // Add it to the active list.
    }

    /**
     * Function: moveFarmer
     * Description: Moves the farmer's character, enforcing screen and gameplay boundaries.
     * Expected Inputs: dx (float) - The change in X position.
     *                  dy (float) - The change in Y position.
     * Expected Outputs/Results: The farmer's coordinates are updated. Collisions with obstacles or the boss will revert the move.
     * Called By: startMoving (from the continuous movement runnable).
     * Will Call: collision, checkPlotPlanting.
     */
    private void moveFarmer(float dx, float dy) {
        if (!gameStarted) return;
        // Apply the movement factor (for speed boosts or ice).
        dx *= movementFactor;
        dy *= movementFactor;
        float currentX = farmer.getX();
        float currentY = farmer.getY();

        // Calculate the new position, clamped within the screen boundaries.
        float newX = Math.max(0, Math.min(currentX + dx, screenWidth - farmer.getWidth()));
        float newY = Math.max(topBoundary, Math.min(currentY + dy, bottomBoundary - farmer.getHeight()));

        // Set the new position.
        if (isInTutorial && tutorialStep == 5) {
            farmer.setX(newX);
            farmer.setY(newY);
            return;
        }

        // Set the new position.
        farmer.setX(newX);
        farmer.setY(newY);

        // Check for collision with obstacles and revert the move if necessary.
        for (TextView obstacle : obstacles) {
            if (collision(farmer, obstacle)) {
                farmer.setX(currentX);
                farmer.setY(currentY);
                return;
            }
        }
        if (currentLevel == BOSS_LEVEL && boss != null && collision(farmer, boss)) {
            farmer.setX(currentX);
            farmer.setY(currentY);
            return;
        }
        // Check for collision with plots to plant/harvest.
        checkPlotPlanting();
    }

    /**
     * Function: throwShovel
     * Description: Activates a shovel from the object pool and makes it visible, ready to be moved by the game loop.
     * Expected Inputs: None.
     * Expected Outputs/Results: A shovel appears on screen and the player's shovel count is decremented.
     * Called By: The "Throw" button's OnClickListener.
     * Will Call: updateUI.
     */
    private void throwShovel() {
        // Can't throw if the game hasn't started, the shovel pool is empty, or the player is out of shovels.
        if (!gameStarted || shovelPool.isEmpty()) return;
        if (shovelCount <= 0) {
            Toast.makeText(this, "No shovels left!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Decrement shovel count and update the UI display.
        shovelCount--;
        updateUI();
        if (shovelSound != null) shovelSound.start();
        // Get a shovel object from the pool.
        TextView shovel = shovelPool.remove(shovelPool.size() - 1);
        // Position the shovel at the farmer's location.
        shovel.setX(farmer.getX() + farmer.getWidth() / 2f);
        shovel.setY(farmer.getY() - 30);
        shovel.setVisibility(View.VISIBLE);  // Make it visible.
        activeShovels.add(shovel); // Add it to the list of active shovels to be updated.
    }

    /**
     * Function: spawnBoss
     * Description: Creates the final boss entity and displays its health bar.
     * Expected Inputs: None.
     * Expected Outputs/Results: The boss TextView is created and added to the layout. The boss health bar becomes visible.
     * Called By: startLevel.
     * Will Call: mainLayout.addView, updateBossHealthUI.
     */
    private void spawnBoss() {
        // Create the TextView for the boss.
        boss = new TextView(this);
        boss.setText("😈");
        boss.setTextSize(90f);
        int bossWidth = 150;
        // Position the boss at the top-center of the screen.
        bossInitialX = screenWidth / 2f - bossWidth / 2f;
        boss.setX(bossInitialX);
        boss.setY(50f);
        boss.setTag(maxBossHealth); // Store its max health in the tag.
        mainLayout.addView(boss); // Add the boss to the layout.
        // Make the health bar visible.
        if (bossHealthLayout != null) {
            bossHealthLayout.setVisibility(View.VISIBLE);
            updateBossHealthUI();
        }
        // Set the timer for the boss's first attack.
        nextBossShootTime = System.currentTimeMillis() + 2000;
    }

    /**
     * Function: updateBossHealthUI
     * Description: Updates the width of the boss's health bar based on its current health percentage.
     * Expected Inputs: None.
     * Expected Outputs/Results: The visual width of the bossHealthBarVisual view is updated.
     * Called By: updateShovels (when the boss is hit).
     * Will Call: None.
     */
    private void updateBossHealthUI() {
        if (bossHealthBarVisual != null && maxBossHealth > 0) {
            // Calculate the ratio of current health to max health.
            float healthRatio = (float) currentBossHealth / maxBossHealth;
            View container = (View) bossHealthBarVisual.getParent();
            if (container != null) {
                // Post the UI update to the main thread to ensure it's safe.
                container.post(() -> {
                    int containerWidth = container.getWidth();
                    // Calculate the new width of the health bar based on the ratio.
                    int newWidth = (int) (containerWidth * healthRatio);
                    ViewGroup.LayoutParams params = bossHealthBarVisual.getLayoutParams();
                    params.width = newWidth;
                    bossHealthBarVisual.setLayoutParams(params);
                });
            }
        }
    }

    /**
     * Function: startTutorial
     * Description: Initiates the game's tutorial mode.
     * Expected Inputs: None.
     * Expected Outputs/Results: The tutorial UI is displayed and the tutorial level is started.
     * Called By: onCreate (if TUTORIAL_MODE extra is true).
     * Will Call: startLevel, showNextTutorialStep.
     */
    private void startTutorial() {
        isInTutorial = true;
        tutorialStep = 0;
        // Show the tutorial overlay UI.
        startButton.setVisibility(View.GONE);
        tutorialNextButton.setVisibility(View.VISIBLE);
        tutorialOverlayText.setVisibility(View.VISIBLE);
        // Start the special tutorial level.
        startLevel(TUTORIAL_LEVEL);
        tutorialOverlayText.bringToFront();
        tutorialNextButton.bringToFront();
        // Display the first step of the tutorial.
        showNextTutorialStep();
    }

    /**
     * Function: showNextTutorialStep
     * Description: Manages the flow of the tutorial, advancing from one step to the next and updating on-screen instructions.
     * Expected Inputs: None.
     * Expected Outputs/Results: The tutorial overlay text and button visibility are updated for the current step.
     * Called By: startTutorial, tutorialNextButton OnClickListener.
     * Will Call: endTutorial, spawnAliens.
     */
    private void showNextTutorialStep() {
        tutorialStep++;
        // Hide all game controls by default for each step.
        leftBtn.setVisibility(View.GONE);
        rightBtn.setVisibility(View.GONE);
        upBtn.setVisibility(View.GONE);
        downBtn.setVisibility(View.GONE);
        throwBtn.setVisibility(View.GONE);
        farmer.setVisibility(View.GONE);
        tutorialNextButton.setText("NEXT >");
        tutorialNextButton.setOnClickListener(v -> showNextTutorialStep());

        // Use a switch statement to define the content and UI for each tutorial step.
        switch (tutorialStep) {
            case 1:
                tutorialOverlayText.setText("Welcome, Space Farmer! Your mission is to plant all the plots (🟫) while avoiding alien attacks.");
                scoreText.setBackgroundColor(0xAA00FF00); // Highlight the score text.
                healthText.setBackgroundColor(0xAAFF0000); // Highlight the health text.
                break;
            case 2:
                scoreText.setBackgroundColor(0x00000000);
                healthText.setBackgroundColor(0x00000000);
                tutorialOverlayText.setText("This is your HEALTH (❤️) and your SHOVEL COUNT (🪓). Losing all health ends the mission. Shovels are for defense!");
                healthText.setBackgroundColor(0xAAFF0000);
                scoreText.setBackgroundColor(0xAA00FF00);
                break;
            case 3:
                healthText.setBackgroundColor(0x00000000);
                scoreText.setBackgroundColor(0x00000000);
                tutorialOverlayText.setText("Use the directional buttons to move your farmer (👨‍🌾) around the screen. Try moving now!");
                farmer.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                upBtn.setVisibility(View.VISIBLE);
                downBtn.setVisibility(View.VISIBLE);
                tutorialNextButton.setVisibility(View.GONE);
                break;
            case 4:
                farmer.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                upBtn.setVisibility(View.VISIBLE);
                downBtn.setVisibility(View.VISIBLE);
                tutorialOverlayText.setText("Move over the brown plots (🟫) to plant seeds (🌱). Plant all of them to complete the level. Go ahead and plant both!");
                tutorialNextButton.setVisibility(View.GONE);
                break;
            case 5:
                farmer.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                upBtn.setVisibility(View.VISIBLE);
                downBtn.setVisibility(View.VISIBLE);
                tutorialOverlayText.setText("The seeds are planted (🌱)! Now, move back over the planted crops to harvest them for bonus score (💰)! Watch your score increase!");
                moveFarmer(0, 0);
                break;
            case 6:
                farmer.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                upBtn.setVisibility(View.VISIBLE);
                downBtn.setVisibility(View.VISIBLE);
                tutorialOverlayText.setText("On your mission, you will encounter obstacles (like boulders 🪨 or icebergs 🧊) that block your movement and alien bullets! Plan your route carefully.");
                break;
            case 7:
                farmer.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                upBtn.setVisibility(View.VISIBLE);
                downBtn.setVisibility(View.VISIBLE);
                throwBtn.setVisibility(View.VISIBLE);
                tutorialOverlayText.setText(getString(R.string.tutorial_powerups));
                break;
            case 8:
                // In this step, the player must defeat an alien.
                farmer.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                upBtn.setVisibility(View.VISIBLE);
                downBtn.setVisibility(View.VISIBLE);
                throwBtn.setVisibility(View.VISIBLE);
                tutorialOverlayText.setText("Use the Throw Button (🪓) to launch a shovel at an alien. You have " + shovelCount + " shovels. One alien will now appear!");
                spawnAliens(1); // Spawn a single alien for the player to fight.
                tutorialNextButton.setOnClickListener(null); // The step advances when the alien is defeated, not by button click.
                tutorialNextButton.setVisibility(View.GONE);
                break;
            default:
                // If there are no more steps, end the tutorial.
                endTutorial();
                break;
        }
    }

    /**
     * Function: endTutorial
     * Description: Cleans up all tutorial-related UI and state, then returns the player to the Planet Select screen.
     * Expected Inputs: None.
     * Expected Outputs/Results: The MainActivity is finished and PlanetSelectActivity is launched.
     * Called By: showNextTutorialStep.
     * Will Call: clearAllEntities, Toast.makeText, startActivity, finish.
     */
    private void endTutorial() {
        isInTutorial = false;
        tutorialStep = 0;
        // Hide tutorial UI and show normal game controls.
        tutorialOverlayText.setVisibility(View.GONE);
        tutorialNextButton.setVisibility(View.GONE);
        leftBtn.setVisibility(View.VISIBLE);
        rightBtn.setVisibility(View.VISIBLE);
        upBtn.setVisibility(View.VISIBLE);
        downBtn.setVisibility(View.VISIBLE);
        throwBtn.setVisibility(View.VISIBLE);
        scoreText.setBackgroundColor(0x00000000);
        healthText.setBackgroundColor(0x00000000);
        // Clear any remaining tutorial entities.
        clearAllEntities();
        Toast.makeText(this, "Tutorial Complete! Returning to Planet Select.", Toast.LENGTH_LONG).show();
        // Go back to the planet selection screen.
        Intent i = new Intent(this, PlanetSelectActivity.class);
        startActivity(i);
        finish(); // Finish this activity to prevent returning to it.
    }

    /**
     * Function: generatePlots
     * Description: Creates the required number of farm plots at random, non-overlapping positions.
     * Expected Inputs: None.
     * Expected Outputs/Results: Brown plot (🟫) TextViews are added to the layout.
     * Called By: startLevel.
     * Will Call: isAreaOccupied, mainLayout.addView.
     */
    private void generatePlots() {
        // Boss level has no plots.
        if (currentLevel == BOSS_LEVEL) {
            return;
        }
        // Clear any plots from a previous game.
        for (TextView plot : plots) mainLayout.removeView(plot);
        plots.clear();
        // Define the vertical area where plots can spawn.
        int minY;
        int maxY;
        if (isInTutorial) {
            minY = screenHeight / 4;
            maxY = bottomBoundary - 100;
        } else {
            minY = topBoundary;
            maxY = bottomBoundary - 100;
        }
        int plotVerticalRange = maxY - minY;
        if (plotVerticalRange <= 50) {
            minY = 100;
            plotVerticalRange = 300;
        }
        ArrayList<TextView> existingViews = new ArrayList<>(aliens); // List to check for overlaps.
        existingViews.addAll(obstacles);
        TextView farmerProxy = new TextView(this);
        farmerProxy.setX(farmer.getX());
        farmerProxy.setY(farmer.getY());
        existingViews.add(farmerProxy);
        float plotW = 80f;
        float plotH = 80f;
        float newX, newY;
        int attempts = 0;
        // Loop to create the required number of plots.
        for (int i = 0; i < plotsToPlant; i++) {
            TextView plot = new TextView(this);
            plot.setText("🟫");
            plot.setTextSize(28f);
            // Try to find a clear spot to spawn the plot.
            do {
                newX = random.nextInt(screenWidth - (int)plotW);
                newY = minY + random.nextInt(plotVerticalRange);
                attempts++;
                if (attempts > 500) return; // Give up after 500 tries.
            } while (isAreaOccupied(newX, newY, plotW, plotH, plots) || isAreaOccupied(newX, newY, plotW, plotH, existingViews));
            plot.setX(newX);
            plot.setY(newY);
            mainLayout.addView(plot); // Add the plot to the screen.
            plots.add(plot); // Add to the list of plots.
        }
    }

    /**
     * Function: checkPlotPlanting
     * Description: Checks for collision between the farmer and plots to handle planting and harvesting, updating the score.
     * Expected Inputs: None.
     * Expected Outputs/Results: Plot visuals are updated (to 🌱 or 💰), score is incremented, and level completion is checked.
     * Called By: moveFarmer.
     * Will Call: collision, updateUI, completeLevel.
     */
    private void checkPlotPlanting() {
        if (currentLevel == BOSS_LEVEL) return;
        int plantedCount = 0;
        // Loop through all plots.
        for (TextView plot : plots) {
            String tag = (String) plot.getTag();
            // Count how many plots are already planted or harvested.
            if ("planted".equals(tag) || "harvested".equals(tag)) {
                plantedCount++;
            }
            // If the farmer collides with a planted plot, harvest it.
            if ("planted".equals(tag) && collision(farmer, plot)) {
                plot.setText("💰");
                plot.setTag("harvested");
                score += 50;
                if (plantSound != null) plantSound.start();
                updateUI();
            } else if (tag == null && collision(farmer, plot)) { // If it's an empty plot, plant it.
                plot.setText("🌱");
                plot.setTag("planted");
                score++;
                if (plantSound != null) plantSound.start();
                updateUI();
            }
        }
        // If all plots have been planted, the level is complete.
        if (plantedCount >= plotsToPlant) {
            if (isInTutorial) {
                if (tutorialStep == 4 || tutorialStep == 5) { // Handle tutorial advancement separately.
                    tutorialNextButton.setVisibility(View.VISIBLE);
                    tutorialNextButton.setText("NEXT >");
                }
                return;
            }
            completeLevel();
        }
    }

    /**
     * Function: handleAlienDeath
     * Description: Manages the consequences of an alien being defeated, including its removal and a chance to spawn a power-up.
     * Expected Inputs: state (AlienState) - The state object of the defeated alien.
     * Expected Outputs/Results: The alien's view is removed, and a respawn timer or tutorial advancement may be triggered.
     * Called By: updateShovels.
     * Will Call: spawnPowerUp, endTutorial.
     */
    private void handleAlienDeath(AlienState state) {
        // Remove the alien from the screen and internal lists.
        mainLayout.removeView(state.alienView);
        aliens.remove(state.alienView);
        alienStates.remove(state);

        //  There is a 15% chance to spawn a power-up where the alien died.
        if (random.nextFloat() < 0.15f) {
            spawnPowerUp(state.alienView.getX(), state.alienView.getY());
        }

        // If this was the alien in the tutorial, advance the tutorial.
        if (isInTutorial && tutorialStep == 8) {
            gameLoopHandler.removeCallbacksAndMessages(null); // Pause the game.
            tutorialOverlayText.setText("SUCCESS! You eliminated the threat. You are ready for your mission.");
            tutorialNextButton.setText("END TUTORIAL");
            tutorialNextButton.setOnClickListener(v -> endTutorial());
            tutorialNextButton.setVisibility(View.VISIBLE);
            return;
        }
        // If an alien died in a normal level, set a timer to respawn a new one.
        if (aliens.size() < numberOfAliens) {
            nextAlienSpawnTime = System.currentTimeMillis() + 5000;
        }
    }

    /**
     * Function: collision
     * Description: Performs Axis-Aligned Bounding Box (AABB) collision detection between two Views.
     * Expected Inputs: a (View), b (View) - The two views to check for overlap.
     * Expected Outputs/Results: boolean - true if the views overlap, false otherwise.
     * Called By: updateBullets, updateShovels, moveFarmer, checkPlotPlanting.
     * Will Call: None.
     */
    private boolean collision(View a, View b) {
        if (a == null || b == null) return false;
        // Invisible views can't collide.
        if (a.getVisibility() != View.VISIBLE || b.getVisibility() != View.VISIBLE) return false;

        // Use a smaller hitbox radius for the farmer for a better feel.
        if (a.equals(farmer) || b.equals(farmer)) {
            float paddingX = farmer.getWidth() * 0.2f; // 20% padding on each side.
            float paddingY = farmer.getHeight() * 0.2f;
            return a.getX() < b.getX() + b.getWidth() - paddingX &&
                    a.getX() + a.getWidth() > b.getX() + paddingX &&
                    a.getY() < b.getY() + b.getHeight() - paddingY &&
                    a.getY() + a.getHeight() > b.getY() + paddingY;
        }
        // Standard AABB collision for all other objects.
        return a.getX() < b.getX() + b.getWidth() &&
                a.getX() + a.getWidth() > b.getX() &&
                a.getY() < b.getY() + b.getHeight() &&
                a.getY() + a.getHeight() > b.getY();
    }

    /**
     * Function: gameOver
     * Description: Ends the current game session and transitions to the GameOverActivity.
     * Expected Inputs: None.
     * Expected Outputs/Results: The game loop stops, and the GameOverActivity is launched with necessary level info.
     * Called By: updateBullets.
     * Will Call: startActivity, finish.
     */
    private void gameOver() {
        if (isGameOver) return; // Prevent this from running multiple times.
        isGameOver = true;
        gameStarted = false; // Stop the game loop.
        // Stop all Handlers.
        gameLoopHandler.removeCallbacksAndMessages(null);
        moveHandler.removeCallbacksAndMessages(null);
        if (gameOverSound != null) gameOverSound.start();
        // Delay the transition slightly to let the sound play.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent i = new Intent(this, GameOverActivity.class);
            // Pass the current level info so the player can retry.
            i.putExtra("TUTORIAL_MODE", isInTutorial);
            i.putExtra("LEVEL_NUMBER", currentLevel);
            startActivity(i);
            finish();
        }, 800);
    }

    /**
     * Function: completeLevel
     * Description: Ends the current level successfully and transitions to the NextLevelActivity or EndingActivity.
     * Expected Inputs: None.
     * Expected Outputs/Results: The game loop stops, and the appropriate next activity is launched.
     * Called By: checkPlotPlanting, updateShovels.
     * Will Call: clearAllEntities, startActivity, finish.
     */
    private void completeLevel() {
        if (isLevelComplete || isGameOver) return; // Prevent this from running multiple times.
        isLevelComplete = true;
        gameStarted = false; // Stop the game loop.
        gameLoopHandler.removeCallbacksAndMessages(null);
        moveHandler.removeCallbacksAndMessages(null);
        clearAllEntities(); // Clean up the screen.
        if (victorySound != null) victorySound.start();

        // Fade out the main layout for a smooth transition.
        mainLayout.animate().alpha(0f).setDuration(800).withEndAction(() -> {
            Intent i;
            // If the boss was defeated, go to the new EndingActivity
            if (currentLevel == BOSS_LEVEL) {
                // DEBUGGING: Show a message to confirm this block is being executed.
                Toast.makeText(this, "Boss Defeated! Showing Ending.", Toast.LENGTH_LONG).show();
                i = new Intent(this, EndingActivity.class);
            } else {
                // Otherwise, go to the regular next level screen
                i = new Intent(this, NextLevelActivity.class);
                i.putExtra("LEVEL_NUMBER", currentLevel);
                i.putExtra("SCORE", score);
            }
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }).start();
    }

    /**
     * Function: clearAllEntities
     * Description: Removes all dynamic game objects from the screen and clears their respective lists.
     * Expected Inputs: None.
     * Expected Outputs/Results: All plots, aliens, projectiles, and power-ups are removed from the layout and lists.
     * Called By: startLevel, completeLevel, endTutorial.
     * Will Call: recycleBullet, recycleShovel, mainLayout.removeView.
     */
    private void clearAllEntities() {
        // Remove all views from the layout and clear the tracking lists.
        for (TextView view : plots) mainLayout.removeView(view);
        for (TextView view : new ArrayList<>(activeBullets)) recycleBullet(view, null);
        for (TextView view : new ArrayList<>(activeShovels)) recycleShovel(view, null);
        for (TextView view : aliens) mainLayout.removeView(view);
        for (TextView view : obstacles) mainLayout.removeView(view);
        plots.clear();
        aliens.clear();
        alienStates.clear();
        obstacles.clear();

        // Also remove the boss and its health bar if they exist.
        if (boss != null) {
            mainLayout.removeView(boss);
            boss = null;
        }
        if (bossHealthLayout != null) {
            bossHealthLayout.setVisibility(View.GONE);
        }
        // Reset the farmer's position for the next level.
        farmer.setX(screenWidth / 2f - farmer.getWidth() / 2f);
        farmer.setY(bottomBoundary - farmer.getHeight() - 40);
    }

    /**
     * Function: updateUI
     * Description: Refreshes the on-screen score and health displays.
     * Expected Inputs: None.
     * Expected Outputs/Results: The text in scoreText and healthText is updated.
     * Called By: startLevel, throwShovel, updateBullets, checkPlotPlanting.
     * Will Call: None.
     */
    private void updateUI() {
        // Update the text to reflect the current game state.
        scoreText.setText("Score: " + score + "  Shovels: " + shovelCount);
        healthText.setText("Health: " + health);
    }

    /**
     * Function: onDestroy
     * Description: Standard Android Activity lifecycle method. Cleans up all resources to prevent memory leaks.
     * Expected Inputs: None.
     * Expected Outputs/Results: All MediaPlayer resources are released and all Handlers are stopped.
     * Called By: Android OS.
     * Will Call: MediaPlayer.release.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all MediaPlayer resources.
        MediaPlayer[] sounds = {shootSound, plantSound, gameOverSound, hitSound, victorySound, shovelSound};
        for (MediaPlayer m : sounds) {
            if (m != null) {
                m.release();
            }
        }
        // Stop all handlers to prevent memory leaks.
        gameLoopHandler.removeCallbacksAndMessages(null);
        moveHandler.removeCallbacksAndMessages(null);
    }
}