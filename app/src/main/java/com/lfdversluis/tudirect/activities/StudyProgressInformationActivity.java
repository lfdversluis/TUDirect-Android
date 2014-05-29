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

public class StudyProgressInformationActivity extends Activity {

    String programName, program, examProgram, examType, satisfied;
    double pointsRequired, pointsNow;
    LinearLayout studyProgressLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity);

        studyProgressLayout = (LinearLayout) findViewById(R.id.buildingList);

        // Retrieve the information passed to this Activity by the InspectWorkspaceActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            programName = extras.getString("programName");
            program = extras.getString("program");
            examProgram = extras.getString("examProgram");
            examType = extras.getString("examType");
            pointsRequired = Double.parseDouble(extras.getString("pointsRequired"));
            pointsNow = Double.parseDouble(extras.getString("pointsNow"));
            satisfied = extras.getString("satisfied");

            //Display information on the screen.
            addTitle(programName);
            addTextView("Program name", program);
            addTextView("Exam program", examProgram);
            addTextView("Exam type", examType);
            addTextView("ECTS points required", pointsRequired + "");
            addTextView("ECTS points obtained", pointsNow + "");
            addTextView("Points left to obtain", pointsRequired - pointsNow + "");
            addTextView("Completed", (satisfied.equals("J") ? "Yes" : "No"));

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
                TextView titleTextView = new TextView(StudyProgressInformationActivity.this);
                titleTextView.setBackgroundColor(0xFF00A6FF);
                titleTextView.setTextColor(0xFFFFFFFF);
                titleTextView.setTextSize(25);
                titleTextView.setLayoutParams(lparams);
                titleTextView.setText(title);
                titleTextView.setPadding(5, 0, 5, 0);
                StudyProgressInformationActivity.this.studyProgressLayout.addView(titleTextView);

                TextView blackLine = new TextView(StudyProgressInformationActivity.this);
                blackLine.setBackgroundColor(0xFF000000);
                blackLine.setLayoutParams(lparams);
                blackLine.setHeight(3);
                StudyProgressInformationActivity.this.studyProgressLayout.addView(blackLine);

            }
        });
    }

    public void addTextView(final String header, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutParams lparams = new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                TextView headerTextView = new TextView(StudyProgressInformationActivity.this);
                headerTextView.setBackgroundColor(0xFF00A6FF);
                headerTextView.setTextColor(0xFFFFFFFF);
                headerTextView.setTextSize(16);
                headerTextView.setLayoutParams(lparams);
                headerTextView.setText(header);
                headerTextView.setPadding(5, 0, 5, 0);
                StudyProgressInformationActivity.this.studyProgressLayout.addView(headerTextView);

                TextView bodyTextView = new TextView(StudyProgressInformationActivity.this);
                bodyTextView.setLayoutParams(lparams);
                bodyTextView.setPadding(5, 0, 5, 10);
                bodyTextView.setText(text);
                StudyProgressInformationActivity.this.studyProgressLayout.addView(bodyTextView);
            }
        });
    }

    public void error(final String s) {
        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudyProgressInformationActivity.this);
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
            StudyProgressInformationActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}