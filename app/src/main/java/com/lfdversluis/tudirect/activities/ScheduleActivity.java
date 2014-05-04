package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.lfdversluis.tudirect.R;
import com.lfdversluis.tudirect.adapters.ScheduleAdapter;
import com.lfdversluis.tudirect.objects.ScheduleObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ScheduleActivity extends Activity {

    int pixelWidth;
    Button searchButton;
    EditText searchField;
    HttpClient httpclient;
    HttpPost httppost;
    HttpResponse response;
    ProgressDialog dialog = null;
    ArrayList<ScheduleObject> scheduleObjects;
    ScheduleAdapter customAdapter;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    schedule.setAdapter(customAdapter);
                    dialog.dismiss();
                    break;
            }
        }
    };
    ListView schedule;
    private String courseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.schedule_activity);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        schedule = (ListView) findViewById(R.id.schedule);
        schedule.setTextFilterEnabled(true);

        scheduleObjects = new ArrayList<ScheduleObject>(50);

        customAdapter = new ScheduleAdapter(this, scheduleObjects);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        pixelWidth = displayMetrics.widthPixels;

        searchButton = (Button) findViewById(R.id.searchButton);
        searchField = (EditText) findViewById(R.id.searchField);
        searchButton.setWidth((int) (pixelWidth * 0.4));
        searchField.setWidth((int) (pixelWidth * 0.6));
        searchField.setHint("Course Id");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            dialog = ProgressDialog.show(ScheduleActivity.this, "Loading",
                    "Retrieving Schedule...", true);
            courseId = extras.getString("courseId");
            new Thread(new Runnable() {
                public void run() {
                    retrieveCourseSchedule();
                }
            }).start();
        }

        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
                dialog = ProgressDialog.show(ScheduleActivity.this, "Loading",
                        "Retrieving Schedule...", true);
                new Thread(new Runnable() {
                    public void run() {
                        retrieveCourseSchedule();
                    }
                }).start();
            }
        });
    }

    public void retrieveCourseSchedule() {
        if (searchField.getText().length() > 0)
            courseId = searchField.getText().toString().trim().toUpperCase();

        scheduleObjects.clear();

        String url = "https://api.tudelft.nl/v0/vakroosters/" + courseId;
        try {
            HttpParams httpPar = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
            HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
            httpclient = new DefaultHttpClient(httpPar);
            httppost = new HttpPost(url);
            //Execute HTTP Post Request
            response = httpclient.execute(httppost);
            final int responseCode = response.getStatusLine().getStatusCode();

            //Positive result, we made the connection
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String aux = "";
                while ((aux = reader.readLine()) != null) {
                    sb.append(aux);
                }
                String json = sb.toString();

                if (!json.equals("null") && json.length() > 0) {
                    JSONTokener tokener = new JSONTokener(json);
                    JSONArray list = ((JSONObject) tokener.nextValue()).getJSONObject("rooster").getJSONObject("evenementLijst").getJSONArray("evenement");

                    for (int i = 0; i < list.length(); i++) {
                        String coursename = list.getJSONObject(i).getString("beschrijvingNL");
                        String begin = "Begin: " + list.getJSONObject(i).getString("startDatumTijd").substring(0, 10) + " " + list.getJSONObject(i).getString("startDatumTijd").substring(11, 19);
                        String end = "End:    " + list.getJSONObject(i).getString("eindeDatumTijd").substring(0, 10) + " " + list.getJSONObject(i).getString("eindeDatumTijd").substring(11, 19);
                        ;
                        String place = list.getJSONObject(i).getJSONObject("ruimte").getString("naamNL");
                        scheduleObjects.add(new ScheduleObject(coursename, begin, end, place));
                    }
                    handler.sendEmptyMessage(1);
                } else {
                    error("The Course Id you have entered appears to be invalid, check the course and try again.");
                }
            }
        } catch (Exception e) {
            error("An error occurred while trying to retrieve the course schedule, check your internet and try again.");
            e.printStackTrace();
        }
    }

    public void error(final String s) {
        runOnUiThread(new Runnable() {
            public void run() {
                dialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(ScheduleActivity.this);
                builder.setTitle("An error has occured.");
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
            ScheduleActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}