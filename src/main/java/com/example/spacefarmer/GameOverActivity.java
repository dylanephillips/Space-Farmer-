package com.example.spacefarmer;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Program: GameOverActivity.java
 * Description: Activity displayed when the player's health reaches zero. It provides options to
 *              retry the level or return to the main menu.
 * Called By: MainActivity (upon calling its `gameOver()` method).
 * Will Call: MainActivity, PlanetSelectActivity.
 */
public class GameOverActivity extends AppCompatActivity {
    /**
     * Function: onCreate
     * Description: Initializes the activity view, retrieves the level number and tutorial status from the
     *              intent, and sets up click listeners for the 'Retry' and 'Main Menu' buttons.
     * Expected Inputs: savedInstanceState (Bundle) - The activity's previously saved state.
     *                  Intent Extras: "LEVEL_NUMBER" (int), "TUTORIAL_MODE" (boolean).
     * Expected Outputs/Results: An initialized screen with functional buttons that correctly restart
     *                           the level or tutorial.
     * Called By: Android OS.
     * Will Call: setContentView, getIntent, findViewById, setOnClickListener, startActivity, finish.
     */
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game_over);

    // Retrieve info from MainActivity
    final int levelToRetry = getIntent().getIntExtra("LEVEL_NUMBER", 1);
    final boolean wasInTutorial = getIntent().getBooleanExtra("TUTORIAL_MODE", false);

    // Bind UI elements
    Button retryButton = findViewById(R.id.retryButton);
    Button mainMenuButton = findViewById(R.id.mainMenuButton);

    /**
     * Logic: Retry Button Click Handler
     * Objective: Restart the failed level or tutorial by sending the correct info back to MainActivity.
     */
    retryButton.setOnClickListener(v -> {
        Intent intent = new Intent(GameOverActivity.this, MainActivity.class);

        // If the player died in the tutorial, set the flag to restart the tutorial.
        if (wasInTutorial) {
            intent.putExtra("TUTORIAL_MODE", true);
        } else {
            // Otherwise, just restart the level they were on.
            intent.putExtra("LEVEL_NUMBER", levelToRetry);
        }
        startActivity(intent);
        finish();
    });

    /**
     * Logic: Main Menu Button Click Handler
     * Objective: Navigate the user back to the Planet Selection screen.
     */
    mainMenuButton.setOnClickListener(v -> {
        Intent intent = new Intent(GameOverActivity.this, PlanetSelectActivity.class);
        startActivity(intent);
        finish();
    });
}
}