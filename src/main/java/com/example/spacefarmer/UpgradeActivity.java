package com.example.spacefarmer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Program: UpgradeActivity.java
 * Description: Displays the "Farm Store" screen, allowing the player to spend their accumulated
 *              score on permanent upgrades for health, shovels, and speed.
 * Called By: NextLevelActivity.
 * Will Call: MainActivity.
 */
public class UpgradeActivity extends AppCompatActivity {

    private UpgradeManager upgradeManager;
    private int nextLevel;

    private TextView scoreDisplay;
    private TextView healthInfo, shovelInfo, speedInfo;
    private Button upgradeHealthBtn, upgradeShovelBtn, upgradeSpeedBtn, continueBtn;


    /**
    *Function: onCreate
    * Description: Initializes the activity, retrieves the next level number, binds UI elements,
    *       updates the display with current costs and levels, and sets up click listeners.
    * Expected Inputs: savedInstanceState (Bundle), Intent Extra: "LEVEL_NUMBER" (int).
    * Expected Outputs/Results: A functional store screen where the player can purchase upgrades.
    * Called By: Android OS.
    * Will Call: bindUI, updateAllUI, purchaseUpgrade, startActivity, finish.
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        // Initialize the manager that handles all saved data
        upgradeManager = new UpgradeManager(this);
        // Get the next level number passed from the previous activity.
        nextLevel = getIntent().getIntExtra("LEVEL_NUMBER", 1) + 1;

        bindUI();
        updateAllUI();

        // --- Set up click listeners for all buttons ---
        // Each upgrade button calls the purchase logic with its specific key.
        upgradeHealthBtn.setOnClickListener(v -> purchaseUpgrade(UpgradeManager.getHealthKey()));
        upgradeShovelBtn.setOnClickListener(v -> purchaseUpgrade(UpgradeManager.getShovelKey()));
        upgradeSpeedBtn.setOnClickListener(v -> purchaseUpgrade(UpgradeManager.getSpeedKey()));

        // The continue button starts the next level.
        continueBtn.setOnClickListener(v -> {
            Intent i = new Intent(UpgradeActivity.this, MainActivity.class);
            i.putExtra("LEVEL_NUMBER", nextLevel);
            startActivity(i);
            finish();
        });
    }

    /**
     * Function: bindUI
     * Description: Links class member variables to their corresponding View objects defined in the XML layout.
     * Expected Inputs: None.
     * Expected Outputs/Results: All UI-related class fields are populated.* Called By: onCreate.
     * Will Call:findViewById.
     */
    private void bindUI() {
        scoreDisplay = findViewById(R.id.scoreDisplay);
        healthInfo = findViewById(R.id.healthInfo);
        shovelInfo = findViewById(R.id.shovelInfo);
        speedInfo = findViewById(R.id.speedInfo);
        upgradeHealthBtn = findViewById(R.id.upgradeHealthBtn);
        upgradeShovelBtn = findViewById(R.id.upgradeShovelBtn);
        upgradeSpeedBtn = findViewById(R.id.upgradeSpeedBtn);
        continueBtn = findViewById(R.id.continueBtn);
    }

    /**
     * Function:updateAllUI
     * Description: Refreshes all text views on the screen to display the most current score and upgrade information.
     * Expected Inputs: None.
     * Expected Outputs/Results: The UI text is updated.
     * Called By: onCreate, purchaseUpgrade.
     *Will Call: upgradeManager.getTotalScore, updateUpgradeUI.
     */
    private void updateAllUI() {
        // Display the player's total accumulated score.
        scoreDisplay.setText("Score: " + upgradeManager.getTotalScore());
        // Update the UI for each individual upgrade.
        updateUpgradeUI(healthInfo, UpgradeManager.getHealthKey());
        updateUpgradeUI(shovelInfo, UpgradeManager.getShovelKey());
        updateUpgradeUI(speedInfo, UpgradeManager.getSpeedKey());
    }

    /**
     * Function: updateUpgradeUI
     * Description: Updates the text for a single upgrade, showing its current level and the cost for the next level.
     * Expected Inputs: infoView (TextView) - The view to update.
     *                  key (String) - The preference key for the upgrade.
     * Expected Outputs/Results: The TextView is updated with formatted text (e.g., "Current: +1 | Cost: 5000").
     * Called By: updateAllUI.
     * Will Call: getCost.
     */
    private void updateUpgradeUI(TextView infoView, String key) {
        // Get the current level of the upgrade from saved data.
        int level = upgradeManager.getUpgradeLevel(key);
        int cost;

        // Set the new base costs for each upgrade type
        if (key.equals(UpgradeManager.getHealthKey())) {
            cost = 500;
        } else if (key.equals(UpgradeManager.getShovelKey())) {
            cost = 400;
        } else { // Speed
            cost = 500;
        }

        // The cost increases with each level purchased.
        cost += (level * (cost / 2));

        // Update the TextView with the current level and the calculated cost for the next level.
        infoView.setText("Current: +" + level + " | Cost: " + cost);
    }

    /**
     * Function: purchaseUpgrade
     * Description: Handles the logic for purchasing an upgrade. It checks if the player has enough score,
     *              spends the score, and updates the upgrade level if the purchase is successful.
     * Expected Inputs: key (String) - The preference key for the upgrade being purchased.
     * Expected Outputs/Results: The player's score and upgrade level are updated. A Toast message is shown.
     * Called By: OnClick listeners for the upgrade buttons.
     * Will Call: getCost, upgradeManager.getTotalScore, upgradeManager.spendScore, upgradeManager.incrementUpgradeLevel, Toast.makeText.
     */
    private void purchaseUpgrade(String key) {
        // Calculate the cost for the next level of the selected upgrade.
        int cost = getCost(key);
        // Get the player's current total score.
        int currentScore = upgradeManager.getTotalScore();

        // --- This is the core purchase logic ---
        // Check if the player can afford the upgrade.
        if (currentScore >= cost) {
            // If they can, subtract the cost from their total score.
            upgradeManager.spendScore(cost);
            // Increment the saved level for that specific upgrade.
            upgradeManager.incrementUpgradeLevel(key);
            // Show a success message.
            Toast.makeText(this, "Upgrade Purchased!", Toast.LENGTH_SHORT).show();
            updateAllUI();
        } else {
            // If they can't afford it, show an error message.
            Toast.makeText(this, "Not enough score!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Function: getCost
     * Description: Calculates the cost for the next level of a given upgrade based on its base cost and current level.
     * Expected Inputs: key (String) - The preference key for the upgrade.
     * Expected Outputs/Results: int - The calculated cost.
     * Called By: updateUpgradeUI, purchaseUpgrade.
     * Will Call: upgradeManager.getUpgradeLevel.
     */
    private int getCost(String key) {
        // Get the current level of the upgrade to calculate the next level's cost.
        int level = upgradeManager.getUpgradeLevel(key);
        int cost;
        // Assign a different base cost for each type of upgrade.
        if (key.equals(UpgradeManager.getHealthKey())) {
            cost = 500;
        } else if (key.equals(UpgradeManager.getShovelKey())) {
            cost = 400;
        } else { // Speed
            cost = 500;
        }
        // The cost scales up based on the current level, making subsequent upgrades more expensive.
        cost += (level * (cost / 2));
        return cost;
    }
}