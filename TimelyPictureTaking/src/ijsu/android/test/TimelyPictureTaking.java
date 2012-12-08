package ijsu.android.test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// onResume() to open camera and set camera parameters
// onPause() to release camera
// insure application is connected to camera (opened) during activity running state
// insure user should not pause application to invoke onPause() (e.g. screen rotation ...)
// camera previewing is initiated by startAlarm button and terminated by stopAlarm button
// camera releases and re-opens after taking picture scheduled by alarm manager

public class TimelyPictureTaking extends Activity implements LocationListener {

	private static final String ALARM_REFRESH_ACTION = "ijsu.android.test.ALARM_REFRESH_ACTION";
	private static final int ALARM_CODE = 20;
	private BroadcastReceiver alarmReceiver;
	private PendingIntent pendingIntent;

	private AlarmManager alarmManager;

	private int alarmCounted;

	// UI references
	private TextView myViewInfo;
	private SurfaceView mySurfaceView;//SurfaceView物件
	private SurfaceHolder mySurfaceHolder;//SurfaceHolder物件
	private String serverIp, serverPort, timeDelay, timeInterval, pictureSize;
	
	// Hardware
	private Camera myCamera;//Camera物件
	private Button startbtn, stopbtn, settingbtn, exitbtn;
	private LocationManager myLocationManager;
	private String myLocationProvider; 
	private boolean preViewing = false;

	// The handler that manage the UI updates
	private Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ALARM_CODE:
				myCamera.takePicture(null, null, myjpegCallback);				
				break;
			default:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// window settings
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN ,  
		              WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// We retrieve UI references
		setContentView(R.layout.timely_picture_taking);
        mySurfaceView = (SurfaceView) findViewById(R.id.surfaceView);//取得SurfaceView控制項
        mySurfaceHolder = mySurfaceView.getHolder();//取得SurfaceHolder
        mySurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
		myViewInfo = (TextView) findViewById(R.id.viewInfo);
        startbtn = (Button) findViewById(R.id.startButton);
        stopbtn = (Button) findViewById(R.id.pauseButton);
        settingbtn = (Button) findViewById(R.id.settingButton);
        exitbtn = (Button) findViewById(R.id.exitButton);
}

	@Override
	protected void onStart() {
		super.onStart();
		
		// We get the AlarmManager
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		// We prepare the pendingIntent for the AlarmManager
		Intent intent = new Intent(ALARM_REFRESH_ACTION);
		pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		        
		// We create and register a broadcast receiver for alarms
		alarmReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// We increment received alarms
				alarmCounted++;
				// We notify to handler the arrive of alarm
				Message msg = myHandler.obtainMessage(ALARM_CODE, intent); 
				myHandler.sendMessage(msg);
			}
		};
		
		// We register dataReceiver to listen ALARM_REFRESH_ACTION
		IntentFilter filter = new IntentFilter(ALARM_REFRESH_ACTION);
		registerReceiver(alarmReceiver, filter);
		
	    // Get the location manager
	    myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

//	    Criteria myCriteria = new Criteria();
//	    myCriteria.setPowerRequirement(Criteria.POWER_HIGH); //有人建議須設定此項，GPS及CameraPreview方能運作
		// 經測試須有LocationListener，且Listener須有讀取位置指令，location才能正常更新；否則，經緯度時間都不會更新
	    myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, this);
}
	
	protected void onResume() {
		super.onResume();
		restorePrefs();

		openCamera();
		String[] pictureSizeXY = pictureSize.split("x");
		Log.v("picture size", "size x:" + pictureSizeXY[0]);
		Log.v("picture size", "size y:" + pictureSizeXY[1]);
		
		setPictureSize(Integer.parseInt(pictureSizeXY[0]), Integer.parseInt(pictureSizeXY[1]));
//		myCamera.startPreview();
	}
	
	protected void onPause(){
		super.onPause();
		savePrefs();
		releaseCamera();
	}

	public void openCamera() {
        if (myCamera == null) {
			try {
	        	myCamera = Camera.open();
				myCamera.setPreviewDisplay(mySurfaceHolder);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(TimelyPictureTaking.this,
						"無法連接相機", Toast.LENGTH_SHORT).show();
				finish();
			}
        }
	}

	public void releaseCamera() {
		if (myCamera != null) {
			myCamera.stopPreview();
			myCamera.release();
			myCamera = null;
		}
	}
	
	public void getCameraInfo() {
        if (myCamera == null) {
        	myCamera = Camera.open();
			try {
				myCamera.setPreviewDisplay(mySurfaceHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        Camera.Parameters myParameters = myCamera.getParameters();
        myViewInfo.append("Supported Picture Sizes: \n");
        for(Camera.Size pictureSize: myParameters.getSupportedPictureSizes ()) {
        	myViewInfo.append(pictureSize.width + "x" + pictureSize.height + "\n" );
        }
        myViewInfo.append("Supported Preview Sizes: \n");
        for(Camera.Size pictureSize: myParameters.getSupportedPreviewSizes ()) {
        	myViewInfo.append(pictureSize.width + "x" + pictureSize.height + "\n" );
        }		
	}
	
    public void setPictureSize(final int w, int h) {
        Camera.Parameters myParameters = myCamera.getParameters();
		myParameters.setPictureFormat(PixelFormat.JPEG);
		myParameters.setPictureSize(w, h);
//		myParameters.setPreviewSize(w, h);//螢幕大小
//		myParameters.set("orientation", "portrait");
//		myParameters.set("orientation", "landscape");
//		myParameters.set("rotation",270);
		myCamera.setParameters(myParameters);
//		myCamera.startPreview();//立即執行Preview   	
	}

	public void startAlarm(View v) {
		// We get value for repeating alarm
		int startTime = Integer.parseInt(timeDelay)*1000;
		long intervals = Long.parseLong(timeInterval)*1000;

		Log.v("Integer Value", "start time:" + Integer.toString(startTime));
		Log.v("Integer Value", "time inteval:" + Long.toString(intervals));
		// We have to register to AlarmManager
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.MILLISECOND, startTime);

		//start preview
		if (!preViewing) myCamera.startPreview();
		preViewing = true;
		
		// We set a repeating alarm
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar
				.getTimeInMillis(), intervals, pendingIntent);

		// disable related buttons
		startbtn.setEnabled(false);
	    stopbtn.setEnabled(true);
	    settingbtn.setEnabled(false);
	    exitbtn.setEnabled(false);
	}

	public void stopAlarm(View v) {
		// We cancel alarms that matches with pending intent
		alarmManager.cancel(pendingIntent);
		myViewInfo.setText("停止傳送\n");
		
		// stop preview
		if (preViewing) myCamera.stopPreview();
		preViewing = false;
		
		// disable stop-button
		startbtn.setEnabled(true);
	    stopbtn.setEnabled(false);
	    settingbtn.setEnabled(true);
	    exitbtn.setEnabled(true);
	}
	
	public void systemSettings(View v) {
		startActivity(new Intent(this, SettingsActivity.class));
	}

	public void systemExit(View v) {
		finish();
	}

	PictureCallback myjpegCallback = new PictureCallback(){
		public void onPictureTaken(byte[] data, Camera camera) {
			
			// run on another thread (asynctask) to transmit GPS and image data
			(new UploadTask()).execute(data);
			
			// display the captured image
			myViewInfo.setText("已拍照傳送：" + alarmCounted + "次\n");		
			Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
			ImageView myImageView = (ImageView) findViewById(R.id.imageView);
			myImageView.setImageBitmap(bm);//將圖片顯示到下方的ImageView中
			
			// for testing
			myCamera.stopPreview();
			myCamera.release();
			myCamera = null;
			openCamera();
			String[] pictureSizeXY = pictureSize.split("x");
			setPictureSize(Integer.parseInt(pictureSizeXY[0]), Integer.parseInt(pictureSizeXY[1]));
			myCamera.startPreview();
			preViewing = true;
			

		}
	};	
	
//    class UploadTask extends AsyncTask<byte[], Integer, String> {  
    class UploadTask extends AsyncTask<byte[], Integer, String> {  
        //後面尖括號內分別是參數（例子裡是線程休息時間），進度(publishProgress用到)，返回值 類型  
          
        @Override  
        protected void onPreExecute() {  
            //第一個執行方法  
            super.onPreExecute();  
        }  
          
        @Override  
        protected String doInBackground(byte[]... params) {  
            //第二個執行方法,onPreExecute()執行完後執行
//        	LocationManager myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//    		Criteria criteria = new Criteria();	//資訊提供者選取標準
//    		String bestProvider = myLocationManager.getBestProvider(criteria, true);	//選擇精準度最高的提供者
//    		Location  myLocation = myLocationManager.getLastKnownLocation(bestProvider);
//		    Location myLocation = myLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//		    Location myLocation = myLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    		String bestProvider = myLocationManager.getBestProvider(new Criteria(), true);	//選擇精準度最高的提供者
    		Location  myLocation = myLocationManager.getLastKnownLocation(bestProvider);

			if (myLocation != null) {
				try {
					Socket socket = new Socket(serverIp.trim(), Integer.parseInt(serverPort));
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					out.writeLong(new Date().getTime());
					out.writeLong(Double.doubleToLongBits(myLocation.getLatitude()));
					out.writeLong(Double.doubleToLongBits(myLocation.getLongitude()));
					out.writeLong(Double.doubleToLongBits(myLocation.getAltitude()));
					out.writeInt(params[0].length);
					out.write(params[0]);
					out.close();
					socket.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				myViewInfo.append("時間:" + String.valueOf(new Date(myLocation.getTime())) + "\n");
				myViewInfo.append("經度:" +  String.valueOf(myLocation.getLongitude()) + "\n");
				myViewInfo.append("緯度:" +  String.valueOf(myLocation.getLatitude()) + "\n");
			} else {
//+++++++++ the following code just for indoor testing. (No GPS)		    
			    try {
					Socket socket = new Socket(serverIp.trim(), Integer.parseInt(serverPort));
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					out.writeLong(new Date().getTime());
					out.writeLong(Double.doubleToLongBits(25.345));
					out.writeLong(Double.doubleToLongBits(123.45));
					out.writeLong(Double.doubleToLongBits(162.5));
					out.writeInt(params[0].length);
					out.write(params[0]);
					out.close();
					socket.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
	//+++++++++ the above code just for indoor testing. (No GPS)
			        
				myViewInfo.append("無法取得位置資訊\n");
			}
			myViewInfo.append("影像大小:" + String.valueOf(params[0].length) + "\n");
			return "傳送成功";
        }  
  
        @Override  
        protected void onProgressUpdate(Integer... progress) {  
            //這個函數在doInBackground調用publishProgress時觸發，雖然調用時只有一個參數  
            //但是這裡取到的是一個數組,所以要用progesss[0]來取值  
            //第n個參數就用progress[n]來取值  
            //tv.setText(progress[0]+"%");  
            super.onProgressUpdate(progress);  
        }  
  
        @Override  
        protected void onPostExecute(String result) {  
            //doInBackground返回時觸發，換句話說，就是doInBackground執行完後觸發  
            //這裡的result就是上面doInBackground執行後的返回值，所以這裡是"執行完畢"  
            //setTitle(result);  
            super.onPostExecute(result);
            
        }  
          
    }

	private void savePrefs() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		settings.edit().putString("serverIp", serverIp)
				.putString("serverPort", serverPort)
				.putString("timeDelay",timeDelay)
				.putString("timeInterval", timeInterval)
				.putString("pictureSize", pictureSize).commit();
	}

    private void restorePrefs() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		serverIp = settings.getString("serverIp","192.168.2.131");
		serverPort = settings.getString("serverPort","5566");
		timeDelay = settings.getString("timeDelay","3");
		timeInterval = settings.getString("timeInterval","5");
		pictureSize = settings.getString("pictureSize", "320x240");

		myViewInfo.setText("目前參數設定: \n" +
				"連線主機位址埠號: " + serverIp + ":" + serverPort +"\n" +
				"延遲時間: " + timeDelay + "\n" +
				"間隔時間: " + timeInterval + "\n" +
				"傳送影像解析度: " + pictureSize);
	}

	public void onLocationChanged(Location arg0) {
		// 經測試須下達以下指令，location才能正常更新；否則，經緯度時間都不會更新
		Location myLocation = myLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}

	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
}
