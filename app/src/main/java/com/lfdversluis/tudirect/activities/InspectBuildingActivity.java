package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.KeyEvent;
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
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class InspectBuildingActivity extends Activity {

    String buildingId;
    HttpClient httpclient;
    HttpPost httppost;
    StringBuffer buffer;
    HttpResponse response;
    ProgressDialog dialog = null;
    LinearLayout buildingInfoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity);

        buildingInfoLayout = (LinearLayout) findViewById(R.id.buildingList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            buildingId = extras.getString("buildingId");
            dialog = ProgressDialog.show(InspectBuildingActivity.this, "",
                    "Retrieving building information...", true);
            new Thread(new Runnable() {
                public void run() {
                    retrieveBuildingInfo();
                }
            }).start();
        } else {
            error("There was no location code provided. Go back to the building page and retry. If the problems persist, please submit a bug report.");
        }
    }

    public void retrieveBuildingInfo() {
        String url = "https://api.tudelft.nl/v0/gebouwen/" + buildingId;

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
                String aux;
                while ((aux = reader.readLine()) != null) {
                    sb.append(aux);
                }
                String json = sb.toString();

                if (!json.equals("null") && json.length() > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buildingInfoLayout.removeAllViews();
                        }
                    });
                    JSONTokener tokener = new JSONTokener(json);
                    JSONObject building = ((JSONObject) tokener.nextValue()).getJSONObject("getLocatieByLocatieCodeResponse").getJSONObject("locatie");

                    addTitle(building.getString("naamEN"));

                    addTextView("location Code", building.getString("locatieCode"));
                    JSONObject adress = building.getJSONObject("fysiekAdres").getJSONObject("binnenlandsAdres");
                    String adres = "Street: " + adress.getString("straat") + "\r\n"
                            + "House number: " + adress.getString("huisnummer") + "\r\n"
                            + "Zip code: " + adress.getString("postcode") + "\r\n"
                            + "Town: " + adress.getString("plaats");
                    addTextView("Address", adres);

                    String gps;
                    if (!building.isNull("gpscoordinaten")) {
                        gps = "Latitude: " + building.getJSONObject("gpscoordinaten").getString("@lat") + "\r\nLongitude: " + building.getJSONObject("gpscoordinaten").getString("@lon");
                    } else {
                        gps = "No gps coordinates known for this building.";
                    }
                    addTextView("GPS coordinates", gps);

                    dialog.dismiss();
                } else {
                    error("The building number did not result in an answer from the server, please try again later.");
                }
            }
        } catch (Exception e) {
            error("An error occurred while trying to retrieve the course, check your internet and try again.");
            e.printStackTrace();
        }
    }

    public void addTitle(final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutParams lparams = new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                TextView titleTextView = new TextView(InspectBuildingActivity.this);
                titleTextView.setBackgroundColor(0xFF00A6FF);
                titleTextView.setTextColor(0xFFFFFFFF);
                titleTextView.setTextSize(25);
                titleTextView.setLayoutParams(lparams);
                titleTextView.setText(title);
                titleTextView.setPadding(5, 0, 5, 0);
                InspectBuildingActivity.this.buildingInfoLayout.addView(titleTextView);

                TextView blackLine = new TextView(InspectBuildingActivity.this);
                blackLine.setBackgroundColor(0xFF000000);
                blackLine.setLayoutParams(lparams);
                blackLine.setHeight(3);
                InspectBuildingActivity.this.buildingInfoLayout.addView(blackLine);

            }
        });
    }

    public void addTextView(final String header, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutParams lparams = new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                TextView headerTextView = new TextView(InspectBuildingActivity.this);
                headerTextView.setBackgroundColor(0xFF00A6FF);
                headerTextView.setTextColor(0xFFFFFFFF);
                headerTextView.setTextSize(16);
                headerTextView.setLayoutParams(lparams);
                headerTextView.setText(header);
                headerTextView.setPadding(5, 0, 5, 0);
                InspectBuildingActivity.this.buildingInfoLayout.addView(headerTextView);

                TextView bodyTextView = new TextView(InspectBuildingActivity.this);
                bodyTextView.setLayoutParams(lparams);
                bodyTextView.setPadding(5, 0, 5, 10);
                bodyTextView.setText(text);
                Linkify.addLinks(bodyTextView, Linkify.EMAIL_ADDRESSES);
                InspectBuildingActivity.this.buildingInfoLayout.addView(bodyTextView);
            }
        });
    }

    public void error(final String s) {
        runOnUiThread(new Runnable() {
            public void run() {
                dialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(InspectBuildingActivity.this);
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
            InspectBuildingActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}