package com.lfdversluis.tudirect.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

public class BuildingActivity extends ListActivity {

    static String[] buildings;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    runOnUiThread(new Runnable() {
                        public void run() {
                            dialog.dismiss();
                            listView.onRefreshComplete();
                        }
                    });
                    listView.setAdapter(new ArrayAdapter<String>(BuildingActivity.this, R.layout.list_activity, buildings));
                    break;
            }
        }
    };
    HttpClient httpclient;
    HttpPost httppost;
    HttpResponse response;
    PullToRefreshListView listView;
    JSONArray list;
    Dialog dialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_refreshlist);
        listView = (PullToRefreshListView) findViewById(R.id.refreshList);

        refresh();

        listView.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(final PullToRefreshBase<ListView> lv) {
                refresh();
            }
        });

        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(BuildingActivity.this, InspectBuildingActivity.class);
                try {
                    intent.putExtra("buildingId", list.getJSONObject(position - 1).getString("locatieCode"));
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    public void refresh() {
        //Parse buildings from API and store them in buildings.
        dialog = ProgressDialog.show(BuildingActivity.this, "",
                "Retrieving Building list...", true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpParams httpPar = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
                    HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
                    httpclient = new DefaultHttpClient(httpPar);
                    httppost = new HttpPost("http://api.tudelft.nl/v0/gebouwen");
                    //Execute HTTP Post Request
                    response = httpclient.execute(httppost);
                    final int responseCode = response.getStatusLine().getStatusCode();

                    //Positive result, we made the connection
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "iso-8859-1"));
                        StringBuilder sb = new StringBuilder();
                        String aux;
                        while ((aux = reader.readLine()) != null) {
                            sb.append(aux);
                        }
                        String json = sb.toString();

                        if (json.length() > 0) {
                            JSONTokener tokener = new JSONTokener(json);
                            list = ((JSONObject) tokener.nextValue()).getJSONObject("findAllLocatiesResponse").getJSONObject("locatieLijst").getJSONArray("locatie");

                            buildings = new String[list.length()];
                            for (int i = 0; i < list.length(); i++) {
                                if (list.getJSONObject(i).has("naamEN")) {
                                    buildings[i] = list.getJSONObject(i).getString("naamEN");
                                } else {
                                    buildings[i] = "Building " + list.getJSONObject(i).getString("locatieCode");
                                }
                            }
                            handler.sendEmptyMessage(1);
                        }
                    } else {
                        error("The server responded with code (" + responseCode + "), the server is probably temporarily unavailable.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    error("An error occurred while parsing the building, please try again later. If the problem persists please submit a bug report.");
                }
            }
        }).start();
    }

    public void error(final String err) {
        runOnUiThread(new Runnable() {
            public void run() {
                dialog.dismiss();
                listView.onRefreshComplete();
                AlertDialog.Builder builder = new AlertDialog.Builder(BuildingActivity.this);
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
            BuildingActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}