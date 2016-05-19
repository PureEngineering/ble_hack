package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.v4.graphics.ColorUtils;
import android.util.Log;

import android.view.Window;

import android.widget.TextView;
import android.widget.Toast;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.view.ViewGroup.*;
import java.text.DecimalFormat;


public class BluetoothLeService extends Activity implements BluetoothAdapter.LeScanCallback {
	private static final String TAG = "BLE";



	ScrollView mScrollView;
	PowerManager powerManager;

	TextView bluetoothStatusTextView;
	TextView blank1TextView;
	TextView rssiTextView;
	TextView addressTextView;

	String sensor_address;
	int sample;
	int max_rssi = -255;

	int backgroundColor;

	DecimalFormat twoDForm = new DecimalFormat("#.##");


	private BluetoothAdapter mBluetoothAdapter;

	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

	private Handler handler = new Handler();
	public void updateView()
	{
		handler.post(new Runnable() {

			public void run() {
				updateUI();
			}
		});
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_COARSE_LOCATION: {
									 if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
										 Log.d(TAG, "coarse location permission granted");
									 } else {
										 final AlertDialog.Builder builder = new AlertDialog.Builder(this);
										 builder.setTitle("Functionality limited");
										 builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
										 builder.setPositiveButton(android.R.string.ok, null);
										 builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

											 @Override
											 public void onDismiss(DialogInterface dialog) {
											 }

										 });
										 builder.show();
									 }
									 return;
			}
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			// Android M Permission check?
			if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			{
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);

				builder.setTitle("This app needs location access");
				builder.setMessage("Please grant location access so this app can detect beacons.");
				builder.setPositiveButton(android.R.string.ok, null);

				builder.setOnDismissListener(new DialogInterface.OnDismissListener()
						{
							//@Override?
							public void onDismiss(DialogInterface dialog)
				{
					requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
				}
						}
						);

				builder.show();
			}
		}


		powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


		Log.i(TAG, TAG + "Activity started");


		/*
		 * Bluetooth in Android 4.3 is accessed via the BluetoothManager, rather than
		 * the old static BluetoothAdapter.getInstance()
		 */
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();


		mScrollView = new ScrollView(getApplicationContext());

		mScrollView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
				| View.SYSTEM_UI_FLAG_IMMERSIVE);

		mScrollView.setBackgroundColor(Color.BLACK);


		TableLayout mainLayout = new TableLayout(getApplicationContext());

		TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		mainLayout.setLayoutParams(params);

		//mainLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.FILL_PARENT));

		bluetoothStatusTextView = new TextView(this);
		bluetoothStatusTextView.setText("No Data");
		mainLayout.addView(bluetoothStatusTextView);

		blank1TextView = new TextView(this);
		blank1TextView.setTextSize(15);
		blank1TextView.setText("  ");
		blank1TextView.setGravity(Gravity.CENTER_HORIZONTAL);
		mainLayout.addView(blank1TextView);

		rssiTextView = new TextView(this);
		rssiTextView.setTextSize(15);
		rssiTextView.setText("  ");
		rssiTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		mainLayout.addView(rssiTextView);


		addressTextView = new TextView(this);
		addressTextView.setTextSize(25);
		addressTextView.setText("No Data");
		addressTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		addressTextView.setPadding(5,20,5,20);
		mainLayout.addView(addressTextView);


		mScrollView.addView(mainLayout);


		setContentView(mScrollView);



	}

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * We need to enforce that Bluetooth is first enabled, and take the
		 * user to settings to enable it if they have not done so.
		 */
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			//Bluetooth is disabled
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
			finish();
			return;
		}

		/*
		 * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
		 * from installing on these devices, but this will allow test devices or other
		 * sideloads to report whether or not the feature exists.
		 */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		//Begin scanning for LE devices
		startScan();
	}

	@Override
	protected void onPause() {
		super.onPause();
		//Cancel any scans in progress
		mHandler.removeCallbacks(mStopRunnable);
		mHandler.removeCallbacks(mStartRunnable);
		mBluetoothAdapter.stopLeScan(this);
	}

	private Runnable mStopRunnable = new Runnable() {
		@Override
		public void run() {
			stopScan();
		}
	};
	private Runnable mStartRunnable = new Runnable() {
		@Override
		public void run() {
			startScan();
		}
	};

	private void startScan() {
		Log.i(TAG, TAG + " startScan");
		//Scan for devices advertising the thermometer service
		mBluetoothAdapter.startLeScan( this);

		mHandler.postDelayed(mStopRunnable, 2500);
		max_rssi = -255;
	}

	private void stopScan() {
		mBluetoothAdapter.stopLeScan(this);
		mHandler.postDelayed(mStartRunnable, 100);
	}

	void updateUI()
	{


		addressTextView.setText(sensor_address);

		rssiTextView.setText(String.valueOf(max_rssi));

		backgroundColor = max_rssi +128;

		blank1TextView.setText(String.valueOf(backgroundColor));

		if(backgroundColor < 45) {

			backgroundColor = 0;
		}
		else
		{

			if(backgroundColor >95)
			{
				backgroundColor = 95;
			}
		}


		mScrollView.setBackgroundColor(ColorUtils.XYZToColor(backgroundColor,backgroundColor,backgroundColor));

	}
	/* BluetoothAdapter.LeScanCallback */

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{

		int start_index = 8;


		Log.i(TAG, "BLE Device: " + device.getName() +":" + device.getAddress() + " @ " + rssi + " scanRecord: " + bytesToHex(scanRecord));

		if(rssi > max_rssi )
		{
			max_rssi = rssi;
			sensor_address = device.getAddress() +' '+ device.getName();
			sample=  (scanRecord[start_index++]&0xff|((scanRecord[start_index++]&0xff)<<8));
			updateUI();
		}


	}

	/*
	 * We have a Handler to process scan results on the main thread,
	 * add them to our list adapter, and update the view
	 */
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

		}
	};


}
