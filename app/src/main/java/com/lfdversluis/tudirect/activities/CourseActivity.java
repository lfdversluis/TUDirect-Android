package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.lfdversluis.tudirect.R;

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

public class CourseActivity extends Activity {

    int pixelWidth;
    Button searchButton;
    EditText searchField;
    HttpClient httpclient;
    HttpPost httppost;
    HttpResponse response;
    ProgressDialog dialog = null;
    LinearLayout courseInformationLayout;
    private String courseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.course_activity);

        courseInformationLayout = (LinearLayout) findViewById(R.id.courseInformationLayout);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        pixelWidth = displayMetrics.widthPixels;

        searchButton = (Button) findViewById(R.id.searchButton);
        searchField = (EditText) findViewById(R.id.searchField);
        searchButton.setWidth((int) (pixelWidth * 0.4));
        searchField.setWidth((int) (pixelWidth * 0.6));
        searchField.setHint("Course ID");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            dialog = ProgressDialog.show(CourseActivity.this, "Loading",
                    "Retrieving Course...", true);
            courseId = extras.getString("courseId");
            new Thread(new Runnable() {
                public void run() {
                    retrieveCourse();
                }
            }).start();
        }

        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);

                dialog = ProgressDialog.show(CourseActivity.this, "Loading",
                        "Retrieving Course...", true);
                new Thread(new Runnable() {
                    public void run() {
                        retrieveCourse();
                    }
                }).start();
            }
        });
    }

    public void retrieveCourse() {
        if (searchField.getText().length() > 0)
            courseId = searchField.getText().toString().trim().toUpperCase();

        String url = "https://api.tudelft.nl/v0/vakken/" + courseId;
        try {
            HttpParams httpPar = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
            HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
            httpclient = new DefaultHttpClient(httpPar);
            httppost = new HttpPost(url);

            //Execute HTTP Post Request
            response = httpclient.execute(httppost);
            final int responseCode = response.getStatusLine().getStatusCode();

            // Server response is OK.
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "iso-8859-1"));
                StringBuilder sb = new StringBuilder();
                String aux = "";
                while ((aux = reader.readLine()) != null) {
                    sb.append(aux);
                }
                String json = sb.toString();

                if (!json.equals("null") && json.length() > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            courseInformationLayout.removeAllViews();
                        }
                    });
                    JSONTokener tokener = new JSONTokener(json);
                    JSONObject course = ((JSONObject) tokener.nextValue()).getJSONObject("vak");

                    addTitle(course.getString("kortenaamEN"));
                    addTextView("ECTS", course.getString("ects"));

                    //Parse it all, since EN tag does not guarantee English and some tags are not there for English.
                    JSONArray jar = course.getJSONObject("extraUnsupportedInfo").getJSONArray("vakUnsupportedInfoVelden");
                    for (int i = 0; i < jar.length(); i++) {
                        addTextView(jar.getJSONObject(i).getString("@label"), jar.getJSONObject(i).getString("inhoud"));
                    }

                    if (course.getJSONObject("extraUnsupportedInfo").getJSONArray("vakMedewerkers").getJSONObject(1).get("medewerker") instanceof JSONObject) {
                        addTextView("Teachers",
                                course.getJSONObject("extraUnsupportedInfo").getJSONArray("vakMedewerkers").getJSONObject(1).getJSONObject("medewerker").getString("naam") +
                                        " - " + course.getJSONObject("extraUnsupportedInfo").getJSONArray("vakMedewerkers").getJSONObject(1).getJSONObject("medewerker").getString("email")
                        );
                    } else {
                        JSONArray teachers = course.getJSONObject("extraUnsupportedInfo").getJSONArray("vakMedewerkers").getJSONObject(1).getJSONArray("medewerker");
                        StringBuilder stringb = new StringBuilder();
                        for (int i = 0; i < teachers.length(); i++) {
                            stringb.append(teachers.getJSONObject(i).get("naam") + " - " + teachers.getJSONObject(i).getString("email") + "\r\n");
                        }
                        addTextView("Teachers", stringb.toString());
                    }
                    dialog.dismiss();
                } else {
                    error("The Course ID you have entered appears to be invalid, check the course and try again.\n(Due to curriculum changes the course ID might have changed)");
                }
            }
        } catch (Exception e) {
            error("An error occurred while trying to retrieve the course, check your internet and try again.");
            e.printStackTrace();
        }
    }

    public void error(final String s) {
        dialog.dismiss();
        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(CourseActivity.this);
                builder.setTitle("An error has occurred.");
                builder.setMessage(s)
                        .setCancelable(false)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    public void addTitle(final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

                TextView titleTextView = new TextView(CourseActivity.this);
                titleTextView.setBackgroundColor(0xFF00A6FF);
                titleTextView.setTextColor(0xFFFFFFFF);
                titleTextView.setTextSize(25);
                titleTextView.setLayoutParams(lparams);
                titleTextView.setText(title);
                titleTextView.setPadding(5, 0, 5, 0);
                CourseActivity.this.courseInformationLayout.addView(titleTextView);

                TextView blackLine = new TextView(CourseActivity.this);
                blackLine.setBackgroundColor(0xFF000000);
                blackLine.setLayoutParams(lparams);
                blackLine.setHeight(3);
                CourseActivity.this.courseInformationLayout.addView(blackLine);

            }
        });
    }

    public void addTextView(final String header, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

                TextView headerTextView = new TextView(CourseActivity.this);
                headerTextView.setBackgroundColor(0xFF00A6FF);
                headerTextView.setTextColor(0xFFFFFFFF);
                headerTextView.setTextSize(16);
                headerTextView.setText(header);
                headerTextView.setPadding(5, 0, 5, 0);
                CourseActivity.this.courseInformationLayout.addView(headerTextView, lparams);

                TextView bodyTextView = new TextView(CourseActivity.this);
                bodyTextView.setPadding(5, 0, 5, 10);
                bodyTextView.setText(text);
                Linkify.addLinks(bodyTextView, Linkify.EMAIL_ADDRESSES);
                CourseActivity.this.courseInformationLayout.addView(bodyTextView, lparams);
            }
        });
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}