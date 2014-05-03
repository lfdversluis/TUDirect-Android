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
import java.util.ArrayList;

public class WorkspaceActivity extends ListActivity {

    static String[] workspaces = {"An error occurred, please try again later"};
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    listView.setAdapter(new ArrayAdapter<String>(WorkspaceActivity.this, R.layout.list_activity, workspaces));
                    dialog.dismiss();
                    listView.onRefreshComplete();
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
                Intent intent = new Intent(WorkspaceActivity.this, InspectWorkspaceActivity.class);
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
        dialog = ProgressDialog.show(WorkspaceActivity.this, "",
                "Retrieving Workspace list...", true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpParams httpPar = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
                    HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
                    httpclient = new DefaultHttpClient(httpPar);
                    httppost = new HttpPost("https://api.tudelft.nl/v0/gebouwen?computerlokaal=true");
                    //Execute HTTP Post Request
                    response = httpclient.execute(httppost);
                    final int responseCode = response.getStatusLine().getStatusCode();

                    // Checking status code for OK
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "iso-8859-1"));
                        StringBuilder sb = new StringBuilder();
                        String aux = "";
                        while ((aux = reader.readLine()) != null) {
                            sb.append(aux);
                        }
                        String json = sb.toString();

                        if (json.length() > 0) {
                            JSONTokener tokener = new JSONTokener(json);
                            list = ((JSONObject) tokener.nextValue()).getJSONObject("getLocatiesMetComputerRuimtesResponse").getJSONObject("locatieLijst").getJSONArray("locatie");

                            if (list.length() == 0) {
                                handler.sendEmptyMessage(1);
                                return;
                            }

                            list = removeNulls(list);

                            workspaces = new String[list.length()];
                            for (int i = 0; i < list.length(); i++) {
                                workspaces[i] = list.getJSONObject(i).getString("naamEN");
                            }

                            handler.sendEmptyMessage(1);
                        }
                    } else {
                        error("The server responded with a code " + responseCode + ". This might be due to maintenance, please try again later.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    error("While parsing the workspaces a problem occurred, please try again later.");
                }
            }
        }).start();
    }

    // These function below is entirely here because we need to remove
    // the null objects from the array, else we are going to crash!
    // Workarounds everywhere!
    public JSONArray removeNulls(final JSONArray from) {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>(from.length() - 1);

        for (int i = 0; i < from.length(); i++) {
            if (from.isNull(i)) continue;
            try {
                JSONObject obj = from.getJSONObject(i);
                list.add(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        final JSONArray ja = new JSONArray();
        for (final JSONObject obj : list) {
            ja.put(obj);
        }

        return ja;
    }

    public void error(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                dialog.dismiss();
                listView.onRefreshComplete();
                AlertDialog.Builder builder = new AlertDialog.Builder(WorkspaceActivity.this);
                builder.setTitle("An error has occurred.");
                builder.setMessage(msg)
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

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}