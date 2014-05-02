package com.lfdversluis.tudirect.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
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
import java.util.ArrayList;
import java.util.Collections;

public class GradesActivity extends ListActivity {

    ArrayList<CourseWrap> wrap;
    GradeAdapter customAdapter;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Collections.sort(wrap);
                    gradeList.setAdapter(customAdapter);
                    dialog.dismiss();
                    gradeList.onRefreshComplete();
                    break;
            }
        }
    };
    JSONArray list;
    StringBuffer buffer;
    Dialog dialog = null;
    private String token;
    private HttpClient httpclient;
    private HttpPost httppost;
    private HttpResponse response;
    private PullToRefreshListView gradeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_refreshlist);
        gradeList = (PullToRefreshListView) findViewById(R.id.refreshList);

        SharedPreferences pref = getSharedPreferences("loginToken", MODE_PRIVATE);
        token = pref.getString("token", null);


        // Check if a token is set, if not redirect to the Main Activity
        if (token == null || token.length() == 0) {
            startActivity(new Intent(GradesActivity.this, MainActivity.class));
            finish();
        } else {
            wrap = new ArrayList<CourseWrap>(40);
            customAdapter = new GradeAdapter(this, wrap);

            retrieveGrades();

            gradeList.setOnRefreshListener(new OnRefreshListener<ListView>() {
                @Override
                public void onRefresh(final PullToRefreshBase<ListView> lv) {
                    retrieveGrades();
                }
            });

            gradeList.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(GradesActivity.this, CourseActivity.class);
                    intent.putExtra("courseId", wrap.get(position - 1).getCourseId());
                    startActivity(intent);
                }
            });
        }
    }

    public void retrieveGrades() {
        dialog = ProgressDialog.show(GradesActivity.this, "Loading",
                "Retrieving your grades...", true);
        wrap.clear();
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpParams httpPar = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
                    HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
                    httpclient = new DefaultHttpClient(httpPar);
                    httppost = new HttpPost("https://api.tudelft.nl/v0/studieresultaten?oauth_token=" + token);

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
                            list = ((JSONObject) tokener.nextValue()).getJSONObject("studieresultaatLijst").getJSONArray("studieresultaat");

                            for (int i = 0; i < list.length(); i++) {
                                boolean sufficient = list.getJSONObject(i).getBoolean("voldoende");
                                String grade = list.getJSONObject(i).getString("resultaat");
                                String courseID = list.getJSONObject(i).getString("cursusid");
                                String mutationDate = list.getJSONObject(i).getString("mutatiedatum");
                                // API no longer provides ECTS in gradesactivity, changed with an hidden update
                                String ects = "-";
                                wrap.add(new CourseWrap(sufficient, courseID, grade, mutationDate, ects));
                            }
                            handler.sendEmptyMessage(1);
                        } else {
                            error("The server responded with no message to read.");
                        }
                    } else {
                        error("The server responded with code (" + responseCode + "), your token might be invalid or the server is temporarily unavailable.");
                    }
                } catch (JSONException j) {
                    dialog.dismiss();
                    // Terrible work around for the fact that the token is no longer valid.
                    // The JSON is malformed and does not contain a ':'
                    // causing the parser to throw an exception
                    // and hence we know we should ask for a new Token...
                    // And yes, if the user is logged in and the server keeps sending bad JSON it will be an infinite loop...
                    // Can't do much about an error in the API, right?
                    startActivity(new Intent(GradesActivity.this, MainActivity.class));
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    error("An error was thrown while parsing your grades, if the problem persists please submit a bug report.");
                }
            }
        }).start();
    }

    public void error(final String err) {
        runOnUiThread(new Runnable() {
            public void run() {
                dialog.dismiss();
                gradeList.onRefreshComplete();
                AlertDialog.Builder builder = new AlertDialog.Builder(GradesActivity.this);
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

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}