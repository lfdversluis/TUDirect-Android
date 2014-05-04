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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.lfdversluis.tudirect.R;
import com.lfdversluis.tudirect.adapters.GradeAdapter;

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

public class StudyProgressActivity extends ListActivity {

    GradeAdapter customAdapter;
    JSONArray list;
    Dialog dialog = null;
    ArrayList<String> studies;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    progressList.setAdapter(customAdapter);
                    setListAdapter(new ArrayAdapter<String>(StudyProgressActivity.this, R.layout.list_activity, studies));
                    progressList.onRefreshComplete();
                    break;
            }
        }
    };
    private String token;
    private HttpClient httpclient;
    private HttpPost httppost;
    private HttpResponse response;
    private PullToRefreshListView progressList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_refreshlist);
        progressList = (PullToRefreshListView) findViewById(R.id.refreshList);

        studies = new ArrayList<String>(5);

        progressList.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(final PullToRefreshBase<ListView> lv) {
                retrieveProgress();
            }
        });

        progressList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                position--;
                Intent intent = new Intent(StudyProgressActivity.this, StudyProgressInformationActivity.class);
                try {
                    intent.putExtra("programName", studies.get(position));
                    intent.putExtra("program", list.getJSONObject(position).getString("opleiding_naam_en") + "/" + list.getJSONObject(position).getString("opleiding_naam"));
                    intent.putExtra("examProgram", list.getJSONObject(position).getString("examenprogramma_naam_en") + "/" + list.getJSONObject(position).getString("examenprogramma_naam"));
                    intent.putExtra("examType", list.getJSONObject(position).getString("examentype_omschrijving_en") + "/" + list.getJSONObject(position).getString("examentype_omschrijving"));
                    intent.putExtra("pointsRequired", list.getJSONObject(position).getString("minimum_punten_examenprogramma"));
                    intent.putExtra("pointsNow", list.getJSONObject(position).getString("behaalde_punten_basisprogramma"));
                    intent.putExtra("satisfied", list.getJSONObject(position).getString("voldaan"));
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void retrieveProgress() {
        dialog = ProgressDialog.show(StudyProgressActivity.this, "Loading",
                "Retrieving your studyprogress...", true);
        studies.clear();
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpParams httpPar = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
                    HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
                    httpclient = new DefaultHttpClient(httpPar);
                    httppost = new HttpPost("https://api.tudelft.nl/v0/studievoortgang?oauth_token=" + token);
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
                            studies.clear();
                            JSONTokener tokener = new JSONTokener(json);
                            JSONObject object = (JSONObject) tokener.nextValue();

                            if (!object.isNull("error")) {
                                String error = object.getString("error");
                                if (error.matches("token_expired")) {
                                    startActivity(new Intent(StudyProgressActivity.this, MainActivity.class));
                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                }
                            } else {
                                list = object.getJSONObject("getStudievoortgangByStudentnummerResponse").getJSONArray("studievoortgang");

                                for (int i = 0; i < list.length(); i++) {
                                    studies.add(list.getJSONObject(i).getString("examenprogramma_naam_en") + "/" + list.getJSONObject(i).getString("examenprogramma_naam"));
                                }
                                handler.sendEmptyMessage(1);
                            }
                        }
                    } else {
                        error();
                    }
                } catch (JSONException j) {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    error();
                }
            }
        }).start();
    }

    public void error() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                progressList.onRefreshComplete();
                AlertDialog.Builder builder = new AlertDialog.Builder(StudyProgressActivity.this);
                builder.setTitle("An error has occurred.");
                builder.setMessage("An error has occurred, please check your internet configuration and try again. If the problems persist please contact us.")
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
            StudyProgressActivity.this.finish();
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
            startActivity(new Intent(StudyProgressActivity.this, MainActivity.class));
        } else {
            retrieveProgress();
        }
    }
}
