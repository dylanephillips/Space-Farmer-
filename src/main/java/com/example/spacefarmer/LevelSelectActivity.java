package com.example.spacefarmer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Program: LevelSelectActivity.java
 * Description: Displays a grid of individual levels for a specific planet. It dynamically creates
 *              buttons for each level and visually locks the ones the player has not yet reached.
 * Called By: PlanetSelectActivity.
 * Will Call: MainActivity.
 */
public class LevelSelectActivity extends AppCompatActivity {

    private UpgradeManager upgradeManager;
    private int highestLevelUnlocked;

    /**
     * Function: onCreate
     * Description: Initializes the activity. It determines which planet was selected, then dynamically
     *              populates a GridLayout with buttons for each level of that planet, enabling or
     *              disabling them based on the player's saved progress.
     * Expected Inputs: savedInstanceState (Bundle) - The activity's previously saved state.
     *                  Intent Extras: "PLANET" (String) - The name of the selected planet.
     * Expected Outputs/Results: A screen showing a grid of level buttons, with locked levels disabled.
     * Called By: Android OS.
     * Will Call: setContentView, findViewById, upgradeManager.getHighestLevelUnlocked, levelGrid.addView,
     *            startActivity, finish.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        // Initialize the manager for saved game progress.
        upgradeManager = new UpgradeManager(this);
        // Load the highest level the player has unlocked from SharedPreferences.
        highestLevelUnlocked = upgradeManager.getHighestLevelUnlocked();

        // --- Bind UI elements from the XML layout ---
        TextView planetTitle = findViewById(R.id.planetTitle);
        GridLayout levelGrid = findViewById(R.id.levelGrid);
        Button backButton = findViewById(R.id.backToPlanetSelectBtn);

        // --- Determine which planet's levels to show ---
        // Get the selected planet name passed from PlanetSelectActivity.
        String planet = getIntent().getStringExtra("PLANET");
        int startLevel = 1;
        int levelCount = 5;

        // Configure the title and starting level number based on the planet.
        if ("PURPLE".equals(planet)) {
            planetTitle.setText("Purple Planet");
            startLevel = 6;
        } else if ("ICE".equals(planet)) {
            planetTitle.setText("Ice Planet");
            startLevel = 11;
        } else {
            planetTitle.setText("Green Planet");
            startLevel = 1;
        }

        // Dynamically create and add level buttons to the grid
        for (int i = 0; i < levelCount; i++) {
            // Calculate the actual level number for this button.
            int levelNumber = startLevel + i;
            // Create a new Button programmatically.
            Button levelButton = new Button(this);
            levelButton.setText(String.valueOf(levelNumber));
            levelButton.setTextSize(20f);
            levelButton.setTextColor(Color.WHITE);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.setMargins(16, 16, 16, 16);
            params.width = 150;
            params.height = 150;
            levelButton.setLayoutParams(params);

            // This is the core logic: Check if the level is locked or unlocked ---
            if (levelNumber <= highestLevelUnlocked) {
                // Level is unlocked
                levelButton.setBackgroundColor(ContextCompat.getColor(this, R.color.black));

                // Set a click listener to start the game at this level number.
                levelButton.setOnClickListener(v -> {
                    Intent intent = new Intent(LevelSelectActivity.this, MainActivity.class);
                    intent.putExtra("LEVEL_NUMBER", levelNumber);
                    startActivity(intent);
                });
            } else {
                // Level is locked
                levelButton.setText("🔒");
                levelButton.setBackgroundColor(Color.DKGRAY);
                levelButton.setOnClickListener(v -> {
                    Toast.makeText(this, "Complete level " + (levelNumber - 1) + " to unlock!", Toast.LENGTH_SHORT).show();
                });
            }
            // Add the newly created and configured button to the GridLayout.
            levelGrid.addView(levelButton);
        }

        backButton.setOnClickListener(v -> finish());
    }
}