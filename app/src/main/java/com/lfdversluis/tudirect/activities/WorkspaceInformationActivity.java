package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.lfdversluis.tudirect.R;

public class WorkspaceInformationActivity extends Activity {

    String roomName, time;
    int pcsAvailable, pcsInUse;
    LinearLayout workplaceLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity);

        workplaceLayout = (LinearLayout) findViewById(R.id.buildingList);

        // Retrieve the information passed to this Activity by the InspectWorkspaceActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            roomName = extras.getString("roomName");
            pcsAvailable = Integer.parseInt(extras.getString("pcsAvailable"));
            pcsInUse = Integer.parseInt(extras.getString("pcsInUse"));
            time = extras.getString("time").substring(0, 10) + " " + extras.getString("time").substring(11, 19);

            //Display these on the screen.
            addTitle(roomName);
            addTextView("Computers available", (pcsAvailable - pcsInUse) + "/" + pcsAvailable + " computer are free at this location.\n\n" +
                    "Please note that only users logged in at the computers can be seen. Students occupying places with i.e. laptops are not taken into account.");
            addTextView("Last Check", "The last check was done on " + time);
        }
        // No information was somehow passed to this Activity, thus throw an error.
        else {
            error("No information was available, please try again.");
        }
    }

    public void addTitle(final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutParams lparams = new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                TextView titleTextView = new TextView(WorkspaceInformationActivity.this);
                titleTextView.setBackgroundColor(0xFF00A6FF);
                titleTextView.setTextColor(0xFFFFFFFF);
                titleTextView.setTextSize(25);
                titleTextView.setLayoutParams(lparams);
                titleTextView.setText(title);
                titleTextView.setPadding(5, 0, 5, 0);
                WorkspaceInformationActivity.this.workplaceLayout.addView(titleTextView);

                TextView blackLine = new TextView(WorkspaceInformationActivity.this);
                blackLine.setBackgroundColor(0xFF000000);
                blackLine.setLayoutParams(lparams);
                blackLine.setHeight(3);
                WorkspaceInformationActivity.this.workplaceLayout.addView(blackLine);

            }
        });
    }

    public void addTextView(final String header, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutParams lparams = new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                TextView headerTextView = new TextView(WorkspaceInformationActivity.this);
                headerTextView.setBackgroundColor(0xFF00A6FF);
                headerTextView.setTextColor(0xFFFFFFFF);
                headerTextView.setTextSize(16);
                headerTextView.setLayoutParams(lparams);
                headerTextView.setText(header);
                headerTextView.setPadding(5, 0, 5, 0);
                WorkspaceInformationActivity.this.workplaceLayout.addView(headerTextView);

                TextView bodyTextView = new TextView(WorkspaceInformationActivity.this);
                bodyTextView.setLayoutParams(lparams);
                bodyTextView.setPadding(5, 0, 5, 10);
                bodyTextView.setText(text);
                WorkspaceInformationActivity.this.workplaceLayout.addView(bodyTextView);
            }
        });
    }

    public void error(final String s) {
        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(WorkspaceInformationActivity.this);
                builder.setTitle("An error has occurred.");
                builder.setMessage(s)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, MenuActivity.class));
            WorkspaceInformationActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}