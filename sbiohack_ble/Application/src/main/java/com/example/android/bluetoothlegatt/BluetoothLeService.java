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

import android.speech.tts.TextToSpeech;
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

import java.security.Policy;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Objects;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.View;

import android.provider.Settings;



public class BluetoothLeService extends Activity implements BluetoothAdapter.LeScanCallback, TextToSpeech.OnInitListener{
	private static final String TAG = "BLE";


	private TextToSpeech mTts;
	private boolean speechReady = false;

	ScrollView mScrollView;
	PowerManager powerManager;

	TextView bluetoothStatusTextView;
	TextView blank1TextView;
	TextView rssiTextView;
	TextView addressTextView;
	TextView volumeTextView;

	String sensor_address;
	int sample;
	int max_rssi = -255;
	int last_rssi = -255;

	int backgroundColor;

	int averageRSSI = 0;

	DecimalFormat twoDForm = new DecimalFormat("#.##");

	private boolean saidHello = false;

	private boolean someoneHome = false;

	int lastSetVolume0;
	int lastSetVolume1;

	int last_rssi0 = -255;
	int last_rssi1 = -255;

	musicLoverConnection localMusicLoverConnection;

	private BluetoothAdapter mBluetoothAdapter;

	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

	public static Camera cam = null;// has to be static, otherwise onDestroy() destroys it

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

		mTts = new TextToSpeech(this, this);

		Log.i(TAG, TAG + "Activity started");


		/*
		 * Bluetooth in Android 4.3 is accessed via the BluetoothManager, rather than
		 * the old static BluetoothAdapter.getInstance()
		 */
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();

		localMusicLoverConnection = new musicLoverConnection();

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);


		mScrollView = new ScrollView(getApplicationContext());

		mScrollView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
				| View.SYSTEM_UI_FLAG_IMMERSIVE);

		mScrollView.setBackgroundColor(Color.BLACK);

		//setAutoOrientationEnabled(getApplicationContext(),false);


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


		volumeTextView = new TextView(this);
		volumeTextView.setTextSize(25);
		volumeTextView.setText("No Data");
		volumeTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		volumeTextView.setPadding(5,20,5,20);
		mainLayout.addView(volumeTextView);

		mScrollView.addView(mainLayout);


		setContentView(mScrollView);





	}

	public static void setAutoOrientationEnabled(Context context, boolean enabled)
	{
		Settings.System.putInt( context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);


	}

	public void onInit(int status) {

		if (status == TextToSpeech.SUCCESS)
		{
			Log.d(TAG, "TextToSpeech init SUCCESS code:" + status);
			mTts.setLanguage(Locale.US);
			mTts.setPitch(1.2F);
			mTts.setSpeechRate(1.2F);
			speechReady = true;
			speakText("Voice Enabled");
		}
		else
		{
			Log.e(TAG, "TextToSpeech init failed code:" + status);
		}
	}

	 void speakText(String speakText)
	{
		if((speechReady == true) )
		{
			mTts.speak(speakText,TextToSpeech.QUEUE_FLUSH,  null,null);
		}
	}

	 void speakTextQueue(String speakText)
	{
		if((speechReady == true) )
		{
			mTts.speak(speakText,TextToSpeech.QUEUE_ADD,  null,null);
		}
	}

	public void flashLightOn(View view)
	{

		//todo
	}

	public void flashLightOff(View view) {
		//todo
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

		mHandler.postDelayed(mStopRunnable, 1000);
		max_rssi = -255;

		last_rssi0 = last_rssi0-3;
		last_rssi1 = last_rssi1-3;


		if(someoneHome == false)
		{
			setVolume(convertRSSItoVolume(last_rssi0),convertRSSItoVolume(last_rssi1));
			Log.i(TAG, "Nobody home, ");
		}

		someoneHome = false;

	}

	private void stopScan() {
		mBluetoothAdapter.stopLeScan(this);
		mHandler.postDelayed(mStartRunnable, 100);
	}




	double getDistance(int rssi, int txPower) {
    /*
     * RSSI = TxPower - 10 * n * lg(d)
     * n = 2 (in free space)
     *
     * d = 10 ^ ((TxPower - RSSI) / (10 * n))
     */

		return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
	}

	static final int MAX_VOLUME = (30);
	int convertRSSItoVolume(int rssi)
	{
		int volume;

		//averageRSSI += 3*rssi;
		//averageRSSI /=4;

		//volume = (int)( getDistance(rssi,0));   //if we want to get louder as we move further away


		volume = rssi +128;
		//volume = (int) (Math.log((double)volume/25.5)*95.0);
		//volume = (int) (Math.sqrt((double)volume))*10;


		Log.i(TAG, "volume calculated to :"+volume + "rssi used "+rssi + "ave "+averageRSSI );
		return volume;  ////10 for testing.
	}

	void updateUI()
	{
		int background_level;


		addressTextView.setText(sensor_address);

		rssiTextView.setText(String.valueOf(max_rssi));

		backgroundColor = Math.max(max_rssi,last_rssi);

		backgroundColor = max_rssi +128;

		if(backgroundColor < 50) {

			backgroundColor = 0;
		}
		else
		{
			backgroundColor = (int) (Math.log((double)backgroundColor/25.5)*95.0);
		}

		blank1TextView.setText(String.valueOf(backgroundColor));
		mScrollView.setBackgroundColor(ColorUtils.XYZToColor(backgroundColor,backgroundColor,backgroundColor));



		last_rssi = max_rssi;

	}
	/* BluetoothAdapter.LeScanCallback */


	public void setVolume(int newVolume0,int newVolume1) {

		volumeTextView.setText("Volume0:" + String.valueOf(newVolume0) + " Volume1:" + String.valueOf(newVolume1));

		//newVolume = (lastSetVolume+ newVolume)/2;  //average the volume, so it doesnt get too loud too fast
		//lastSetVolume = newVolume;

		if(newVolume0 > MAX_VOLUME)
		{
			Log.i(TAG, "volume TOO LOUD, setting to MAX");
			newVolume0 = MAX_VOLUME;
		}
		else if (newVolume0 <0)
		{
			newVolume0 = 0;
		}

		if(newVolume1 > MAX_VOLUME)
		{
			Log.i(TAG, "volume TOO LOUD, setting to MAX");
			newVolume1 = MAX_VOLUME;
		}
		else if (newVolume1 <0)
		{
			newVolume1 = 0;
		}

			//localMusicLoverConnection.playMusic();
			localMusicLoverConnection.setKitchenVolume(newVolume0);
			localMusicLoverConnection.setDenVolume(newVolume1);
			lastSetVolume0= newVolume0;
			lastSetVolume1 = newVolume1;


		//localMusicLoverConnection.setVolume(newVolume);
		//Log.i(TAG, "" + localMusicLoverConnection.getVolume());

		someoneHome = true;

	}

	public void processGoodBLE(BluetoothDevice device, int rssi0, int rssi1) {



		setVolume(convertRSSItoVolume(rssi0),convertRSSItoVolume(rssi1));

		//max_rssi = rssi;
		sensor_address = device.getAddress() + ' ' + device.getName();

		updateUI();

	}

	public static final int THRESHHOLD_RSSI =(-80);
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{

		int start_index = 8;


		//Log.i(TAG, "BLE Device: " + device.getName() +":" + device.getAddress() + " @ " + rssi + " scanRecord: " + bytesToHex(scanRecord));

		if(1==0)
		//if(Objects.equals("EB:02:A3:22:EC:46", device.getAddress()))
		{
			//sashi fitbit charge HR

			Log.i(TAG, "BLE Device: " + device.getName() +":" + device.getAddress() + " @ " + rssi + " scanRecord: " + bytesToHex(scanRecord));

			if (rssi > max_rssi) {

				max_rssi = rssi;
				sensor_address = device.getAddress() + ' ' + device.getName();
				sample = (scanRecord[start_index++] & 0xff | ((scanRecord[start_index++] & 0xff) << 8));
				updateUI();


				if(saidHello == false) {
					speakTextQueue("Hello");
					saidHello = true;
				}
			}
		}

		if(Objects.equals("E8:71:05:C2:68:A6", device.getAddress()))
		{
			//kevin silver colored
			if (rssi > THRESHHOLD_RSSI) {
				//do cool stuff here
				Log.i(TAG, "kevin silver colored tracker BLE Device: " + device.getName() +":" + device.getAddress() + " @ " + rssi + " scanRecord: " + bytesToHex(scanRecord));

					if(rssi > last_rssi0) {
						last_rssi0 = rssi;
						processGoodBLE(device, last_rssi0, last_rssi1);
					}

				someoneHome = true;

			}
		}

		if(Objects.equals("CA:6D:C0:8A:81:CB", device.getAddress()))
		{
			//silver colored tracker
			//do cool stuff here
			if (rssi > THRESHHOLD_RSSI) {
				//do cool stuff here
				Log.i(TAG, "silver colored tracker BLE Device: " + device.getName() +":" + device.getAddress() + " @ " + rssi + " scanRecord: " + bytesToHex(scanRecord));

				if(rssi > last_rssi1)
				{
					last_rssi1 = rssi;
					processGoodBLE(device, last_rssi0, last_rssi1);
				}



				someoneHome = true;
			}
		}


		// stopped working
//		if(Objects.equals("E2:E1:3F:8A:1F:BB", device.getAddress()))
//		{
//			//copper colored tracker
//			if (rssi > THRESHHOLD_RSSI) {
//				//do cool stuff here
//				Log.i(TAG, "copper colored tracker BLE Device: " + device.getName() +":" + device.getAddress() + " @ " + rssi + " scanRecord: " + bytesToHex(scanRecord));
//
//				last_rssi1 = rssi;
//				processGoodBLE(device, last_rssi0, last_rssi1);
//
//			}
//		}




		//if(!Objects.equals("tkr", device.getName()))
		{
			if(device.getName()!= null)
			{
				//Log.i(TAG, "BLE Device: " + device.getName() +":" + device.getAddress() + " @ " + rssi + " scanRecord: " + bytesToHex(scanRecord));

				/*
				if (rssi > max_rssi) {

					max_rssi = rssi;
					sensor_address = device.getAddress() + ' ' + device.getName();
					sample = (scanRecord[start_index++] & 0xff | ((scanRecord[start_index++] & 0xff) << 8));
					updateUI();

				}
				*/

			}
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
