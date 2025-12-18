package com.example.spacefarmer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Program: NextLevelActivity.java
 * Description: Interstitial activity displayed upon successful completion of a game level. It saves
 *              the player's score, unlocks the next level, and provides navigation options to
 *              the store, the next level, or the main menu.
 * Called By: MainActivity (upon calling its `completeLevel()` method).
 * Will Call: UpgradeActivity, MainActivity, StartActivity.
 */
public class NextLevelActivity extends AppCompatActivity {

    /**
     * Function: onCreate
     * Description: Initializes the activity. It retrieves the completed level number and score,
     *              updates the player's permanent progress, and sets up click listeners for all
     *              navigation buttons.
     * Expected Inputs: savedInstanceState (Bundle) - The activity's previously saved state.
     *                  Intent Extras: "LEVEL_NUMBER" (int), "SCORE" (int).
     * Expected Outputs/Results: An initialized screen with functional navigation buttons.
     * Called By: Android OS.
     * Will Call: setContentView, getIntent, findViewById, upgradeManager.addScore,
     *            upgradeManager.unlockLevel, startActivity, finish.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_level);

        // Find all the UI elements
        TextView levelCompleteText = findViewById(R.id.victoryText);
        Button continueButton = findViewById(R.id.continueButton);
        Button storeButton = findViewById(R.id.storeButton);
        Button mainMenuButton = findViewById(R.id.mainMenuButton);

        // Get info from the completed level
        int completedLevel = getIntent().getIntExtra("LEVEL_NUMBER", 1);
        int scoreFromLevel = getIntent().getIntExtra("SCORE", 0);

        levelCompleteText.setText("Planet " + completedLevel + " Secure!");

        // Add the score from the completed level to the player's total
        UpgradeManager upgradeManager = new UpgradeManager(this);
        upgradeManager.addScore(scoreFromLevel);

        // Unlock the next level
        upgradeManager.unlockLevel(completedLevel + 1);

        // --- Set OnClick Listeners for all three buttons ---

        // Continue button starts the next level directly
        continueButton.setOnClickListener(v -> {
            Intent i = new Intent(NextLevelActivity.this, MainActivity.class);
            i.putExtra("LEVEL_NUMBER", completedLevel + 1);
            startActivity(i);
            finish();
        });

        // Store button takes you to the Upgrade Store
        storeButton.setOnClickListener(v -> {
            Intent i = new Intent(NextLevelActivity.this, UpgradeActivity.class);
            i.putExtra("LEVEL_NUMBER", completedLevel); // Pass the *completed* level
            startActivity(i);
            finish();
        });

        // Main Menu button takes you back to the start screen
        mainMenuButton.setOnClickListener(v -> {
            Intent i = new Intent(NextLevelActivity.this, StartActivity.class);
            startActivity(i);
            finish();
        });
    }
}