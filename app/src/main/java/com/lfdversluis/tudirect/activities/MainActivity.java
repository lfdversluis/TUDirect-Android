package com.lfdversluis.tudirect.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lfdversluis.tudirect.R;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

	final static String clientid = "tudirect";
	final static String clientsecret = "36481f4d-0188-42e9-95a4-b99b3ff35ce9";
	final static String callback = "http://ios-dev.no-ip.org/tuiosapp";
	final static String callaccess = "https://oauth.tudelft.nl/oauth2/authorize?response_type=code&client_id="+clientid+"&redirect_uri="+callback;
	final static String urischeme = "tuiosapp://";
	WebView webview;
	HttpPost httppost;
	HttpResponse response;
	HttpClient httpclient;
	List<NameValuePair> nameValuePairs;
	Dialog dialog = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		webview = (WebView)findViewById(R.id.webview);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.loadUrl(callaccess);

		webview.setWebViewClient(new WebViewClient() {
			@Override
			public void onLoadResource(WebView view, String url)
			{
				if (url.contains(urischeme)) {
					//Close the WebView
					webview.setVisibility(View.GONE);
					//Let the user know he is authenticating
					dialog = ProgressDialog.show(MainActivity.this, "Connecting",
							"Authenticating...", true);

					final String accesscode = url.substring(11, url.length());
					//Start the authenticating process
					new Thread(new Runnable() {
						public void run() {
							try{
								HttpParams httpPar = new BasicHttpParams();
								HttpConnectionParams.setConnectionTimeout(httpPar, 45 * 1000);
								HttpConnectionParams.setSoTimeout(httpPar, 45 * 1000);
								httpclient=new DefaultHttpClient(httpPar);
								//Set url for the token request.
								httppost= new HttpPost("https://oauth.tudelft.nl/oauth2/token");
								//Set the header to the urlencoding: see protocol OAUTH
								httppost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
								nameValuePairs = new ArrayList<NameValuePair>(3);
								// Adding the post parameters grant_type, code and redirect_uro along with their values.
								nameValuePairs.add(new BasicNameValuePair("grant_type","authorization_code"));
								nameValuePairs.add(new BasicNameValuePair("code", accesscode));
								nameValuePairs.add(new BasicNameValuePair("redirect_uri", callback));
								httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

								// Add the second header - see OAUTH2 protocol.
								httppost.setHeader("Authorization: Basic ", new String(Base64.encode((clientid+ ":" +clientsecret).getBytes(), Base64.URL_SAFE|Base64.NO_WRAP),"UTF-8"));
								// Execute HTTP Post Request
								response = httpclient.execute(httppost);

								// Retrieve the response code of the request.
								final int responseCode = response.getStatusLine().getStatusCode();

								// Everything ok, retrieve the token of the user and store it so we will remember him/her.
								if(responseCode == 200){
									dialog.dismiss();
									BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
									String json = reader.readLine();
									if(json != null){
										JSONTokener tokener = new JSONTokener(json);
										final String token = ((JSONObject)tokener.nextValue()).getString("access_token");
                                        Log.e("token", token);
										getSharedPreferences("loginToken",MODE_PRIVATE)
										.edit()
										.putString("token", token)
										.commit();

										// Now that token has been set, time to close this screen and
                                        // go back to where we came from.
                                        MainActivity.this.finish();
									}
								}
								else{
									error();
								}
							}catch(Exception e){
								error();
							}
						}
					}).start();
				}
				else {
					super.onLoadResource(view, url);
				}
			}
		});
	}

	public void error(){
		runOnUiThread(new Runnable(){
			public void run() {
				dialog.dismiss();
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("An error has occurred.");
				builder.setMessage("An error has occurred, please check your internet configuration and try again. If the problems persist please submit a bug report.")
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
}