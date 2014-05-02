package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.lfdversluis.tudirect.R;

public class MenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);
    }

    public void startCourseActivity(View v) {
        startActivity(new Intent(MenuActivity.this, CourseActivity.class));
    }

    public void startScheduleActivity(View v) {
        startActivity(new Intent(MenuActivity.this, ScheduleActivity.class));
    }

    public void startGradesActivity(View v) {
        startActivity(new Intent(MenuActivity.this, GradesActivity.class));
    }

    public void startValidGradesActivity(View v) {
        startActivity(new Intent(MenuActivity.this, ValidGradesActivity.class));
    }

    public void startStudyProgressActivity(View v) {
        startActivity(new Intent(MenuActivity.this, StudyProgressActivity.class));
    }

    public void startBuildingActivity(View v) {
        startActivity(new Intent(MenuActivity.this, BuildingActivity.class));
    }

    public void startWorkSpaceActivity(View v) {
        startActivity(new Intent(MenuActivity.this, WorkspaceActivity.class));
    }

    public void startCreditsActivity(View v) {
        startActivity(new Intent(MenuActivity.this, CreditsActivity.class));
    }
}
