package com.example.spacefarmer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Program: EndingActivity.java
 * Description: Displays the final congratulations screen after the player defeats the final boss.
 *              Provides a single option to return to the planet selection screen.
 * Called By: MainActivity (when the boss level is completed).
 * Will Call: PlanetSelectActivity.
 */
public class EndingActivity extends AppCompatActivity {
    /**
     * Function: onCreate
     * Description: Standard Android Activity entry point. Initializes the view and sets up the
     *              click listener for the button to return to the planet selection menu.
     * Expected Inputs: savedInstanceState (Bundle) - The activity's previously saved state.
     * Expected Outputs/Results: An initialized screen with a functional "Return to Planet Select" button.
     * Called By: Android OS.
     * Will Call: setContentView, findViewById, startActivity, finish.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call the superclass's implementation of onCreate
        super.onCreate(savedInstanceState);
        // Set the user interface layout for this activity.
        setContentView(R.layout.activity_ending);

        // creates a reference to the button object tp interact
        Button returnButton = findViewById(R.id.returnToPlanetsBtn);

        // set a listener that  will execute a block of code when the user clicks the button.
        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(EndingActivity.this, PlanetSelectActivity.class);
            startActivity(intent);
            finish();
        });
    }
}