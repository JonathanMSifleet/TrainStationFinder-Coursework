package com.jsifleet.hackathon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	Button getStations;
	TextView stationOutput;
	WebService webService = new WebService();

	Double deviceLat = 0.0;
	Double deviceLng = 0.0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		getStations = (Button) this.findViewById(R.id.getStations);
		stationOutput = (TextView) this.findViewById(R.id.stationOutput);

		this.getLocation();

	}

	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.getStations:
				String[] FSPermissions = {
						Manifest.permission.READ_EXTERNAL_STORAGE,
						Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.INTERNET
				};

				if (checkGotPermission(FSPermissions)) {
					if (isExternalStorageWritable()) {
						JSONArray JSONStations = webService.getStationsFromURL(deviceLat, deviceLng);
						writeStationsToFS("stations.json", JSONStations);
						ArrayList<Station> listOfStations = this.saveJSONToArrayList(JSONStations);
						this.displayStations(listOfStations);
					} else {
						Log.e("Message", "Cannot write to fs");
					}
				}
				break;
		}

	}

	public void displayStations(ArrayList<Station> stations) {
		stationOutput.setText("");
		for (Station curStation : stations) {
			stationOutput.append("Name: " + curStation.getStationName() + "\n");
			stationOutput.append("Lat: " + curStation.getLat() + "\n");
			stationOutput.append("Long: " + curStation.getLng() + "\n");
		}
	}

	public ArrayList<Station> saveJSONToArrayList(JSONArray stations) {
		ArrayList<Station> listOfStations = new ArrayList<>();
		try {
			for (int i = 0; i < stations.length(); i++) {
				Station tempStation = new Station();

				JSONObject jo = stations.getJSONObject(i);

				tempStation.setStationName(jo.getString("StationName"));
				tempStation.setLat(jo.getDouble("Latitude"));
				tempStation.setLng(jo.getDouble("Longitude"));

				listOfStations.add(tempStation);
			}
		} catch (JSONException e) {
			Log.e("Error", "Something has gone wrong");
			e.printStackTrace();
		}

		return listOfStations;
	}

	public void getLocation() {

		String[] locationPermissions = {
				Manifest.permission.INTERNET,
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_COARSE_LOCATION
		};

		if (checkGotPermission(locationPermissions)) {

			LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					deviceLat = location.getLatitude();
					deviceLng = location.getLongitude();
					//Log.e("Location:", deviceLat.toString() + ", " + deviceLng.toString());
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {

				}

				@Override
				public void onProviderEnabled(String provider) {

				}

				@Override
				public void onProviderDisabled(String provider) {

				}
			});
		}

	}

	public boolean checkGotPermission(String[] requiredPermissions) {

		boolean ok = true;
		for (int i = 0; i < requiredPermissions.length; i++) {
			int result = ActivityCompat.checkSelfPermission(this, requiredPermissions[i]);
			if (result != PackageManager.PERMISSION_GRANTED) {
				ok = false;
			}
		}

		if (!ok) {
			ActivityCompat.requestPermissions(this, requiredPermissions, 1);
			// last permission must be > 0
		} else {
			return true;
		}
		return false;
	}

	public boolean isExternalStorageWritable() {
		// checks if external storage is available for read and write
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public void writeStationsToFS(String filename, JSONArray jsonArray) {

		File folder = new File(Environment.getExternalStorageDirectory(), "/Hackathon");

		if (!folder.exists()) {
			folder.mkdirs();
		}

		File file = new File(Environment.getExternalStorageDirectory(), "Hackathon/" + filename);
		FileOutputStream fos;

		byte[] data = new String(jsonArray.toString()).getBytes();

		try {
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();
			Log.e("Message", "JSON written");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e("Message", "JSON not written");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			Log.e("Message", "JSON not written");
		}
	}
}