package com.example.spacefarmer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Contacts;
import android.widget.Button;
import android.widget.Toast;

import java.nio.file.attribute.FileTime;

/**
 * Program: StartActivity.java
 * Description: The initial launch activity for the Space Farmer game. It serves as the main menu,
 *              presenting the user with options to start the game, begin the tutorial, or reset progress.
 * Called By: Android OS (as the main launcher activity), NextLevelActivity, GameOverActivity.
 * Will Call: PlanetSelectActivity, MainActivity.
 */
public class StartActivity extends AppCompatActivity {

    /**
     * Function: onCreate
     * Description: Initializes the activity view and sets up click listeners for all navigation buttons.
     * Expected Inputs: savedInstanceState (Bundle) - The activity's previously saved state.
     * Expected Outputs/Results: An initialized main menu with functional "Start", "Tutorial", and "Reset" buttons.
     * Called By: Android OS.
     * Will Call: setContentView, findViewById, setOnClickListener, AlertDialog.Builder, upgradeManager.resetUpgrades.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // --- Bind UI elements from the XML layout to variables ---
        Button startGameBtn = findViewById(R.id.startGameBtn);
        Button tutorialBtn = findViewById(R.id.tutorialBtn);
        Button resetBtn = findViewById(R.id.resetBtn); // Find the new button

        // --- "Start Game" Button Logic ---
        startGameBtn.setOnClickListener(v -> {
            Intent i = new Intent(StartActivity.this, PlanetSelectActivity.class);
            startActivity(i);
        });

        // --- "Tutorial" Button Logic ---
        tutorialBtn.setOnClickListener(v -> {
            Intent i = new Intent(StartActivity.this, MainActivity.class);
            i.putExtra("TUTORIAL_MODE", true);
            startActivity(i);
        });

        // Set an OnClickListener for the new Reset Button
        resetBtn.setOnClickListener(v -> {
            // Use an AlertDialog to confirm the choice
            new AlertDialog.Builder(this)
                    .setTitle("Reset Progress")
                    .setMessage("Are you sure you want to reset all your score and upgrades? This cannot be undone.")
                    // Define the "positive" action button
                    .setPositiveButton("Yes, Reset", (dialog, which) -> {
                        // Create an instance of UpgradeManager and call the reset method
                        UpgradeManager upgradeManager = new UpgradeManager(this);
                        // Call the method that clears all relevant SharedPreferences data.
                        upgradeManager.resetUpgrades();
                        // Provide feedback to the user that the action was successful.
                        Toast.makeText(this, "Progress has been reset.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}