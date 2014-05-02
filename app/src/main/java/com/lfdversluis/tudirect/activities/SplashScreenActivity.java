package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.lfdversluis.tudirect.R;

public class SplashScreenActivity extends Activity {

    private static final int SPLASH_DURATION = 1000; // Milliseconds
    // used to know if the back button was pressed in the splash screen activity and avoid opening the next activity
    private boolean mIsBackButtonPressed;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar as it's a splash screen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.splashscreen_activity);

        Handler handler = new Handler();

        // run a thread after the direction specified and query meanwhile
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (!mIsBackButtonPressed) {
                    // Redirect to Menu
                    startActivity(new Intent(SplashScreenActivity.this, MenuActivity.class));

                    //This screen is done for, we don't need to be able to return back to here.
                    finish();
                }
            }
        }, SPLASH_DURATION); // time in milliseconds (1 second = 1000 milliseconds) until the run() method will be called
    }

    @Override
    public void onBackPressed() {
        // set the flag to true so the menu activity won't start up
        mIsBackButtonPressed = true;
        super.onBackPressed();
    }
}