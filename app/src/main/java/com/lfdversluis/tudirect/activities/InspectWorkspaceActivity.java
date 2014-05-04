package com.lfdversluis.tudirect.activities;

import android.app.AlertDialog;
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
import java.util.ArrayList;

public class InspectWorkspaceActivity extends ListActivity {

    String buildingId;
    HttpClient httpclient;
    HttpPost httppost;
    StringBuffer buffer;
    HttpResponse response;
    ProgressDialog dialog = null;
    ArrayList<String> roomIds;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    workspaceList.setAdapter(new ArrayAdapter<String>(InspectWorkspaceActivity.this, R.layout.list_activity, roomIds));
                    dialog.dismiss();
                    workspaceList.onRefreshComplete();
                    break;
            }
        }
    };
    JSONArray rooms;
    PullToRefreshListView workspaceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_refreshlist);
        workspaceList = (PullToRefreshListView) findViewById(R.id.refreshList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            buildingId = extras.getString("buildingId");
            retrieveWorkspaceInfo();

        } else {
            error("There was no location code provided. Go back to the workspace list and retry. If the problems persist, please submit a bug report.");
        }

        workspaceList.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(final PullToRefreshBase<ListView> lv) {
                retrieveWorkspaceInfo();
            }
        });

        workspaceList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(InspectWorkspaceActivity.this, WorkspaceInformationActivity.class);
                try {
                    intent.putExtra("roomName", rooms.getJSONObject(position - 1).getJSONObject("ruimte").getString("naamEN"));
                    intent.putExtra("pcsAvailable", rooms.getJSONObject(position - 1).getJSONObject("computerGebruik").getString("aantalBeschikbaar"));
                    intent.putExtra("pcsInUse", rooms.getJSONObject(position - 1).getJSONObject("computerGebruik").getString("aantalInGebruik"));
                    intent.putExtra("time", rooms.getJSONObject(position - 1).getJSONObject("computerGebruik").getString("momentopnameDatumTijd"));
                    startActivity(intent);
                } catch (JSONException e) {
                    error("There was an error while retrieving the detailed information of the location you selected. Please try again.");
                }
            }
        });
    }

    public void retrieveWorkspaceInfo() {
        dialog = ProgressDialog.show(InspectWorkspaceActivity.this, "",
                "Retrieving building information...", true);
        new Thread(new Runnable() {
            public void run() {
                String url = "https://api.tudelft.nl/v0/gebouwen/" + buildingId + "/ruimtes?computerlokaal=true";
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
                            Object arrayOrObject = ((JSONObject) tokener.nextValue()).getJSONObject("computerRuimteInformatieLijst").get("computerRuimteInformatie");
                            if (arrayOrObject instanceof JSONArray) {

                                rooms = (JSONArray) arrayOrObject;
                                roomIds = new ArrayList<String>(rooms.length());

                                for (int i = 0; i < rooms.length(); i++) {
                                    roomIds.add(rooms.getJSONObject(i).getJSONObject("ruimte").getString("naamEN"));
                                }
                            } else {
                                JSONObject obj = (JSONObject) arrayOrObject;
                                rooms = new JSONArray();
                                roomIds = new ArrayList<String>(1);
                                roomIds.add(obj.getJSONObject("ruimte").getString("naamEN"));
                                rooms.put(obj);
                            }
                            handler.sendEmptyMessage(1);
                            dialog.dismiss();
                        } else {
                            error("The room code did not result in an answer from the server, please try again later.");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    error("An error occurred while trying to retrieve the workspace, check your internet and try again.");
                }
            }
        }).start();
    }

    public void error(final String s) {
        runOnUiThread(new Runnable() {
            public void run() {
                dialog.dismiss();
                workspaceList.onRefreshComplete();
                AlertDialog.Builder builder = new AlertDialog.Builder(InspectWorkspaceActivity.this);
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
            InspectWorkspaceActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}