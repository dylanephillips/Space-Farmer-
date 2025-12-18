package com.example.spacefarmer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Program: PlanetSelectActivity.java
 * Description: Displays the main planet selection screen. This activity is responsible for showing which
 *              planets are locked or unlocked based on the player's saved progress.
 * Called By: StartActivity, NextLevelActivity, GameOverActivity.
 * Will Call: LevelSelectActivity, StartActivity.
 */
public class PlanetSelectActivity extends AppCompatActivity {

    // --- UI Elements ---
    private ImageButton greenPlanetBtn, purplePlanetBtn, icePlanetBtn;
    private ImageView purplePlanetLock, icePlanetLock;
    private Button backButton;

    // --- Sound & Data ---
    private MediaPlayer backgroundMusic, clickSound;
    private UpgradeManager upgradeManager;

    /**
     * Function: onCreate
     * Description: Initializes the activity, binds UI components, starts background music, and sets up
     *              the initial state of the planet buttons (locked/unlocked).
     * Expected Inputs: savedInstanceState (Bundle) - The activity's previously saved state.
     * Expected Outputs/Results: An initialized screen with animated planet buttons and correct lock states.
     * Called By: Android OS.
     * Will Call: setContentView, findViewById, MediaPlayer.create, animatePlanet, checkUnlocks.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planet_select);

        upgradeManager = new UpgradeManager(this);

        // --- UI ELEMENTS ---
        greenPlanetBtn = findViewById(R.id.greenPlanetBtn);
        purplePlanetBtn = findViewById(R.id.purplePlanetBtn);
        icePlanetBtn = findViewById(R.id.icePlanetBtn);
        purplePlanetLock = findViewById(R.id.purplePlanetLock);
        icePlanetLock = findViewById(R.id.icePlanetLock);
        backButton = findViewById(R.id.backButton);

        // --- BACKGROUND MUSIC ---
        backgroundMusic = MediaPlayer.create(this, R.raw.menu_music);
        if (backgroundMusic != null) {
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.7f, 0.7f);
            backgroundMusic.start();
        }

        // --- CLICK SOUND ---
        clickSound = MediaPlayer.create(this, R.raw.click_sound);

        // --- ANIMATIONS & LISTENERS ---
        animatePlanet(greenPlanetBtn);
        animatePlanet(purplePlanetBtn);
        animatePlanet(icePlanetBtn);

        checkUnlocks();

        backButton.setOnClickListener(v -> {
            playClick();
            Intent intent = new Intent(PlanetSelectActivity.this, StartActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    /**
     * Function: checkUnlocks
     * Description: Checks the player's highest unlocked level from the UpgradeManager and updates the
     *              UI accordingly, showing or hiding the lock icons and setting the correct click listeners.
     * Expected Inputs: None.
     * Expected Outputs/Results: Planet buttons are either enabled (leading to LevelSelectActivity) or
     *                           disabled (showing a Toast message).
     * Called By: onCreate, onResume.
     * Will Call: upgradeManager.getHighestLevelUnlocked, openLevelSelect, playClick, Toast.makeText.
     */
    private void checkUnlocks() {
        int highestLevelUnlocked = upgradeManager.getHighestLevelUnlocked();

        // Green Planet (starts at level 1) is always unlocked
        greenPlanetBtn.setOnClickListener(v -> openLevelSelect("GREEN"));

        // Purple Planet (starts at level 6)
        if (highestLevelUnlocked >= 6) {
            purplePlanetLock.setVisibility(View.GONE);
            purplePlanetBtn.setOnClickListener(v -> openLevelSelect("PURPLE"));
        } else {
            purplePlanetLock.setVisibility(View.VISIBLE);
            purplePlanetBtn.setOnClickListener(v -> {
                playClick();
                Toast.makeText(this, "Complete Level 5 to unlock!", Toast.LENGTH_SHORT).show();
            });
        }

        // Ice Planet (starts at level 11)
        if (highestLevelUnlocked >= 11) {
            icePlanetLock.setVisibility(View.GONE);
            icePlanetBtn.setOnClickListener(v -> openLevelSelect("ICE"));
        } else {
            icePlanetLock.setVisibility(View.VISIBLE);
            icePlanetBtn.setOnClickListener(v -> {
                playClick();
                Toast.makeText(this, "Complete Level 10 to unlock!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Function: openLevelSelect
     * Description: A helper method to reduce code duplication. Launches the LevelSelectActivity for a specific planet.
     * Expected Inputs: planetName (String) - The name of the planet to display ("GREEN", "PURPLE", or "ICE").
     * Expected Outputs/Results: The LevelSelectActivity is launched with the correct planet identifier.
     * Called By: checkUnlocks.
     * Will Call: playClick, startActivity.
     */
    private void openLevelSelect(String planetName) {
        playClick();
        Intent intent = new Intent(this, LevelSelectActivity.class);
        intent.putExtra("PLANET", planetName);
        startActivity(intent);
    }

    /**
     * Function: playClick
     * Description: Plays the standard UI click sound effect.
     * Expected Inputs: None.
     * Expected Outputs/Results: A sound is played.
     * Called By: All button click handlers.
     * Will Call: MediaPlayer.start.
     */
    private void playClick() {
        if (clickSound != null) {
            clickSound.start();
        }
    }

    /**
     * Function: animatePlanet
     * Description: Creates a continuous, gentle floating animation for a given planet button.
     * Expected Inputs: planet (ImageButton) - The button to animate.
     * Expected Outputs/Results: The button begins a perpetual scaling animation.
     * Called By: onCreate.
     * Will Call: ObjectAnimator.ofFloat.
     */
    private void animatePlanet(ImageButton planet) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(planet, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(planet, "scaleY", 1f, 1.1f, 1f);
        scaleX.setDuration(1800);
        scaleY.setDuration(1800);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new LinearInterpolator());
        scaleY.setInterpolator(new LinearInterpolator());
        scaleX.start();
        scaleY.start();
    }

    // --- Lifecycle Management for Music ---

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check unlocks every time the screen is shown to reflect progress after returning from a level.
        checkUnlocks();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.release();
            backgroundMusic = null;
        }
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
    }
}