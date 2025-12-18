package com.example.spacefarmer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Program: UpgradeManager.java
 * Description: A manager for handling all data persistence using SharedPreferences. It is responsible
 *              for saving and loading the player's total score, permanent upgrade levels, and the
 *              highest level they have unlocked.
 * Called By: StartActivity, NextLevelActivity, UpgradeActivity, MainActivity.
 * Will Call: None.
 */
public class UpgradeManager {

    // The name of the SharedPreferences file where all game data will be stored.
    private static final String PREFS_NAME = "SpaceFarmerPrefs";
    // The key for storing the player's total accumulated score.
    private static final String KEY_TOTAL_SCORE = "totalScore";
    // The key for storing the purchased level of the Health upgrade.
    private static final String KEY_HEALTH_LEVEL = "healthLevel";
    // The key for storing the purchased level of the Shovel upgrade.
    private static final String KEY_SHOVEL_LEVEL = "shovelLevel";
    // The key for storing the purchased level of the Speed upgrade.
    private static final String KEY_SPEED_LEVEL = "speedLevel";
    // The key for storing the highest level number the player has successfully reached.
    private static final String KEY_HIGHEST_LEVEL_UNLOCKED = "highestLevelUnlocked";

    // The SharedPreferences object used to read and write data.
    private final SharedPreferences sharedPreferences;

    /**
     * Function: UpgradeManager (Constructor)
     * Description: Initializes the manager by getting a reference to the app's SharedPreferences file.
     * Expected Inputs: context (Context) - The application context.
     * Expected Outputs/Results: The sharedPreferences field is initialized.
     * Called By: Any activity that needs to access saved data.
     * Will Call: context.getSharedPreferences.
     */
    public UpgradeManager(Context context) {
        // Get a handle to the SharedPreferences file named PREFS_NAME.
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Function: addScore
     * Description: Adds a given amount to the player's saved total score.
     * Expected Inputs: scoreToAdd (int) - The amount of score to add.
     * Expected Outputs/Results: The KEY_TOTAL_SCORE value in SharedPreferences is updated.
     * Called By: NextLevelActivity.
     * Will Call: getTotalScore.
     */
    public void addScore(int scoreToAdd) {
        // First, read the current score from storage.
        int currentScore = getTotalScore();
        // Use the editor to write the new score.
        // .apply() saves the change asynchronously in the background
        sharedPreferences.edit().putInt(KEY_TOTAL_SCORE, currentScore + scoreToAdd).apply();
    }

    /**
     * Function: spendScore
     * Description: Subtracts a given amount from the player's saved total score.
     * Expected Inputs: scoreToSpend (int) - The amount of score to subtract.
     * Expected Outputs/Results: The KEY_TOTAL_SCORE value is updated.
     * Called By: UpgradeActivity.
     * Will Call: getTotalScore.
     */
    public void spendScore(int scoreToSpend) {
        int currentScore = getTotalScore();
        // Use Math.max to prevent the score from becoming negative.
        sharedPreferences.edit().putInt(KEY_TOTAL_SCORE, Math.max(0, currentScore - scoreToSpend)).apply();
    }

    /**
     * Function: getTotalScore
     * Description: Retrieves the player's total saved score.
     * Expected Inputs: None.
     * Expected Outputs/Results: int - The player's total score.
     * Called By: addScore, spendScore, UpgradeActivity.
     * Will Call: None.
     */
    public int getTotalScore() {
        return sharedPreferences.getInt(KEY_TOTAL_SCORE, 0);
    }

    /**
     * Function: getUpgradeLevel
     * Description: Retrieves the current purchased level for a specific upgrade.
     * Expected Inputs: key (String) - The preference key for the upgrade (e.g., "healthLevel").
     * Expected Outputs/Results: int - The current level of the upgrade.
     * Called By: UpgradeActivity, MainActivity.
     * Will Call: None.
     */
    public int getUpgradeLevel(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    /**
     * Function: incrementUpgradeLevel
     * Description: Increments the level of a specific upgrade by one.
     * Expected Inputs: key (String) - The preference key for the upgrade.
     * Expected Outputs/Results: The stored level for the given key is increased by 1.
     * Called By: UpgradeActivity.
     * Will Call: getUpgradeLevel.
     */
    public void incrementUpgradeLevel(String key) {
        int currentLevel = getUpgradeLevel(key);
        sharedPreferences.edit().putInt(key, currentLevel + 1).apply();
    }

    /**
     * Function: getHighestLevelUnlocked
     * Description: Retrieves the highest level the player has unlocked so far.
     * Expected Inputs: None.
     * Expected Outputs/Results: int - The highest unlocked level number.
     * Called By: PlanetSelectActivity.
     * Will Call: None.
     */
    public int getHighestLevelUnlocked() {
        // The player always has access to level 1 by default.
        return sharedPreferences.getInt(KEY_HIGHEST_LEVEL_UNLOCKED, 1);
    }

    /**
     * Function: unlockLevel
     * Description: Unlocks a new level if it is higher than the current highest unlocked level.
     * Expected Inputs: level (int) - The level number to unlock.
     * Expected Outputs/Results: The KEY_HIGHEST_LEVEL_UNLOCKED value is updated if the new level is higher.
     * Called By: NextLevelActivity.
     * Will Call: getHighestLevelUnlocked.
     */
    public void unlockLevel(int level) {
        int currentHighest = getHighestLevelUnlocked();
        // This check prevents the saved progress from being accidentally overwritten with a lower level number.
        if (level > currentHighest) {
            sharedPreferences.edit().putInt(KEY_HIGHEST_LEVEL_UNLOCKED, level).apply();
        }
    }

    /**
     * Function: resetUpgrades
     * Description: Completely wipes all data from SharedPreferences, resetting all progress.
     * Expected Inputs: None.
     * Expected Outputs/Results: All saved data is cleared.
     * Called By: StartActivity.
     * Will Call: None.
     */
    public void resetUpgrades() {
        sharedPreferences.edit().clear().apply();
    }

    /**
     * Function: getHealthKey
     * Description: Returns the static preference key for the health upgrade.
     * Expected Inputs: None.
     * Expected Outputs/Results: String - The health upgrade key.
     * Called By: UpgradeActivity, MainActivity.
     * Will Call: None.
     */
    public static String getHealthKey() {
        return KEY_HEALTH_LEVEL;
    }

    /**
     * Function: getShovelKey
     * Description: Returns the static preference key for the shovel upgrade.
     * Expected Inputs: None.
     * Expected Outputs/Results: String - The shovel upgrade key.
     * Called By: UpgradeActivity, MainActivity.
     * Will Call: None.
     */
    public static String getShovelKey() {
        return KEY_SHOVEL_LEVEL;
    }

    /**
     * Function: getSpeedKey
     * Description: Returns the static preference key for the speed upgrade.
     * Expected Inputs: None.
     * Expected Outputs/Results: String - The speed upgrade key.
     * Called By: UpgradeActivity, MainActivity.
     * Will Call: None.
     */
    public static String getSpeedKey() {
        return KEY_SPEED_LEVEL;
    }
}