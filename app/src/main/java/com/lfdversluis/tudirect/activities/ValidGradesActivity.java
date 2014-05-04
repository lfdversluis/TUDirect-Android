package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.lfdversluis.tudirect.R;
import com.lfdversluis.tudirect.adapters.GradeAdapter;
import com.lfdversluis.tudirect.objects.CourseWrap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class ValidGradesActivity extends Activity {

    ArrayList<CourseWrap> wrap;
    GradeAdapter customAdapter;
    JSONArray list;
    TextView gradeUnweighted, gradeWeighted;
    Dialog dialog = null;
    double points = 0.0;
    double pointsEcts = 0.0;
    double ectsTotal = 0.0;
    int coursesTotal = 0, decimals = 1, unweightedDecimals = 1;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Collections.sort(wrap);
                    gradeList.setAdapter(customAdapter);
                    DecimalFormat df = new DecimalFormat("#.0");
                    gradeWeighted.setText(df.format(pointsEcts / ectsTotal));
                    gradeUnweighted.setText(df.format(points / coursesTotal));
                    gradeList.onRefreshComplete();
                    break;
            }
        }
    };
    String comma = "#.0000";
    private String token;
    private HttpClient httpclient;
    private HttpPost httppost;
    private HttpResponse response;
    private PullToRefreshListView gradeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grade_activity);

        gradeList = (PullToRefreshListView) findViewById(R.id.gradeList);
        gradeList.setAdapter(customAdapter);

        gradeUnweighted = (TextView) findViewById(R.id.averageuGrade);
        gradeWeighted = (TextView) findViewById(R.id.averageGrade);

        SharedPreferences pref = getSharedPreferences("loginToken", MODE_PRIVATE);
        token = pref.getString("token", null);

        // Token is ongeldig, gebruiker moet inloggen
        wrap = new ArrayList<CourseWrap>(40);
        customAdapter = new GradeAdapter(this, wrap);

        gradeList.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(final PullToRefreshBase<ListView> lv) {
                retrieveGrades();
            }
        });

        gradeList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ValidGradesActivity.this, CourseActivity.class);
                intent.putExtra("courseId", wrap.get(position - 1).getCourseId());
                startActivity(intent);
            }
        });
    }

    public void changeAmountOfDecimals(View v) {
        decimals++;
        decimals %= 5;

        if (ectsTotal == 0) return;

        if (decimals == 0) {
            DecimalFormat df = new DecimalFormat("#");

            //Set weighted grade
            gradeWeighted.setText(df.format(pointsEcts / ectsTotal));

            // Set unweighted
            gradeUnweighted.setText(df.format(points / coursesTotal));
        } else {
            // Set weighted
            DecimalFormat df = new DecimalFormat(comma.substring(0, 2 + decimals));
            gradeWeighted.setText(df.format(pointsEcts / ectsTotal));

            // Set unweighted
            gradeUnweighted.setText(df.format(points / coursesTotal));
        }
    }

    public void retrieveGrades() {
        dialog = ProgressDialog.show(ValidGradesActivity.this, "Loading",
                "Retrieving your grades...", true);
        wrap.clear();
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpParams httpPar = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
                    HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
                    httpclient = new DefaultHttpClient(httpPar);
                    httppost = new HttpPost("https://api.tudelft.nl/v0/geldendstudieresultaten?oauth_token=" + token);
                    //Execute HTTP Post Request
                    response = httpclient.execute(httppost);
                    final int responseCode = response.getStatusLine().getStatusCode();

                    //Positive result, we got feedback
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String aux = "";
                        while ((aux = reader.readLine()) != null) {
                            sb.append(aux);
                        }
                        String json = sb.toString();

                        if (json.length() > 0) {
                            JSONTokener tokener = new JSONTokener(json);
                            JSONObject object = (JSONObject) tokener.nextValue();

                            if (!object.isNull("error")) {
                                String error = object.getString("error");
                                if (error.matches("token_expired")) {
                                    startActivity(new Intent(ValidGradesActivity.this, MainActivity.class));
                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                }
                            } else {
                                list = object.getJSONObject("studieresultaatLijst").getJSONArray("studieresultaat");

                                //comma notation formatter
                                DecimalFormat format = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.FRANCE));

                                for (int i = 0; i < list.length(); i++) {
                                    boolean suf = list.getJSONObject(i).getBoolean("voldoende");
                                    String grade = list.getJSONObject(i).getString("resultaat");
                                    String courseid = list.getJSONObject(i).getString("cursusid");
                                    String mutationDate = list.getJSONObject(i).getString("mutatiedatum");
                                    String ects = list.getJSONObject(i).getString("ectspunten");

                                    if (grade.matches("[0-9.,]+")) {
                                        points += format.parse(grade).doubleValue();
                                        ectsTotal += format.parse(list.getJSONObject(i).getString("ectspunten")).doubleValue();
                                        pointsEcts += ((format.parse(list.getJSONObject(i).getString("ectspunten")).doubleValue()) * (format.parse(grade).doubleValue()));
                                        coursesTotal++;
                                    }
                                    wrap.add(new CourseWrap(suf, courseid, grade, mutationDate, ects));
                                }
                                handler.sendEmptyMessage(1);
                            }
                        } else {
                            error("The server responded with no message to read.");
                        }
                    } else {
                        error("The server responded with code (" + responseCode + "), your token might be invalid or the server is temporarily unavailable.");
                    }
                } catch (JSONException j) {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    error("An error occurred while parsing your grades, if the problem persists please submit a bug report.");
                }
            }
        }).start();
    }

    public void error(final String err) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                gradeList.onRefreshComplete();
                AlertDialog.Builder builder = new AlertDialog.Builder(ValidGradesActivity.this);
                builder.setTitle("An error has occurred.");
                builder.setMessage(err)
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, MenuActivity.class));
            ValidGradesActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences pref = getSharedPreferences("loginToken", MODE_PRIVATE);
        token = pref.getString("token", null);
        // Check if a token is set, if not redirect to the Main Activity
        if (token == null || token.length() == 0) {
            startActivity(new Intent(ValidGradesActivity.this, MainActivity.class));
        } else {
            retrieveGrades();
        }
    }
}