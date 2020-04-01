package com.jsifleet.hackathon;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.LongSparseArray;
import androidx.core.app.ActivityCompat;

import com.mapbox.mapboxsdk.plugins.annotation.Annotation;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {

	private MapView mapView;
	private MapboxMap map;
	SymbolManager sm;

	Button getStations;
	ScrollView stationOutput;
	LinearLayout stationLayout;
	TextView stationTextView;
	WebService webService = new WebService();

	Double deviceLat = 0.0;
	Double deviceLng = 0.0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		this.getLocation();

		final String mapboxToken = "pk.eyJ1Ijoiam9uYXRoYW53YXNoZXJlIiwiYSI6ImNrOGg1dmFmZzAxamMzZXBuYTgzeTVkOWYifQ.YoryZ31-wW4WCy_5orU6MA";

		Mapbox.getInstance(this, mapboxToken);
		setContentView(R.layout.activity_main);

		getStations = (Button) this.findViewById(R.id.getStations);
		stationOutput = (ScrollView) this.findViewById(R.id.stationOutput);
		stationLayout = (LinearLayout) this.findViewById(R.id.stationLayout);
		stationTextView = (TextView) this.findViewById(R.id.stationTextView);

		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);
	}

	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.getStations:
				handleGetStations();
				break;
		}

	}

	public void handleGetStations() {
		String[] FSPermissions = {
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.INTERNET
		};

		if (checkGotPermission(FSPermissions)) {
			JSONArray JSONStations = webService.getStationsFromURL(deviceLat, deviceLng);
			ArrayList<Station> listOfStations = this.saveJSONToArrayList(JSONStations);
			this.displayStationsText(listOfStations);
			this.deleteAnnotations();
			this.addSymbol(deviceLat, deviceLng);
			this.displayStationsMapBox(listOfStations);
		} else {
			Log.e("Message", "Do not have permissions");
		}
	}

	public void deleteAnnotations() {

		LongSparseArray<Symbol> annotations = sm.getAnnotations();
		ArrayList<Symbol> tempAnnotations = new ArrayList<>();

		for (int i = 0; i < annotations.size(); i++) {
			tempAnnotations.add(annotations.valueAt(i));
		}

		sm.delete(tempAnnotations);
	}

	public void displayStationsText(ArrayList<Station> stations) {
		stationTextView.setText("");
		for (Station curStation : stations) {
			stationTextView.append("Name: " + curStation.getStationName() + "\n");
			//stationTextView.append("Lat: " + curStation.getLat() + "\n");
			//stationTextView.append("Long: " + curStation.getLng() + "\n");
			double tempDistance = curStation.getDistance();
			tempDistance = Math.floor(tempDistance * 100) / 100;
			stationTextView.append("Distance: " + tempDistance + " miles");
			stationTextView.append("\n\n");
		}
	}

	public void displayStationsMapBox(ArrayList<Station> stations) {
		for (Station curStation : stations) {
			this.addSymbol(curStation.getLat(), curStation.getLng());
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
				tempStation.setDistance(calcDistanceHaversine(deviceLat, deviceLng, tempStation.getLat(), tempStation.getLng()));

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

	public double calcDistanceHaversine(double deviceLat, double deviceLng, double lat2, double lng2) {
		final double R = 6372.8; // kilometers

		double tempDeviceLat = deviceLat;
		double tempDeviceLng = deviceLng;

		double dLat = Math.toRadians(lat2 - tempDeviceLat);
		double dLon = Math.toRadians(lng2 - tempDeviceLng);

		tempDeviceLat = Math.toRadians(tempDeviceLat);
		lat2 = Math.toRadians(lat2);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(tempDeviceLat) * Math.cos(lat2);
		double c = 2 * Math.asin(Math.sqrt(a));

		return convertToMiles(R * c);
	}

	public double convertToMiles(double distance) {
		return distance * 0.62137;
	}

	@Override
	public void onMapReady(@NonNull MapboxMap mapboxMap) {
		map = mapboxMap;
		//required:
		mapboxMap.setStyle(Style.OUTDOORS, this);

		mapboxMap.setCameraPosition(
				new CameraPosition.Builder()
						.target(new LatLng(deviceLat, deviceLng))
						.zoom(12.0)
						.build()
		);
	}

	@Override
	public void onStyleLoaded(@NonNull Style style) {
		// symbol manager is responsible for adding map markers:
		sm = new SymbolManager(mapView, map, style);

		addSymbol(deviceLat, deviceLng);

	}

	public void addSymbol(double lat, double lng) {
		// create an individual map marker:
		SymbolOptions symbolOptions = new SymbolOptions()
				.withLatLng(new LatLng(lat, lng))
				.withIconImage("suitcase-15")
				.withIconColor("black")
				.withIconSize(1.0f);

		// display item:
		Symbol symbol = sm.create(symbolOptions);
	}

	@Override
	public void onStart() {
		super.onStart();
		mapView.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		mapView.onStop();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

}
