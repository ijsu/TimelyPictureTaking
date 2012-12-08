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
	private SurfaceView mySurfaceView;//SurfaceView����
	private SurfaceHolder mySurfaceHolder;//SurfaceHolder����
	private String serverIp, serverPort, timeDelay, timeInterval, pictureSize;
	
	// Hardware
	private Camera myCamera;//Camera����
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
        mySurfaceView = (SurfaceView) findViewById(R.id.surfaceView);//���oSurfaceView���
        mySurfaceHolder = mySurfaceView.getHolder();//���oSurfaceHolder
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
//	    myCriteria.setPowerRequirement(Criteria.POWER_HIGH); //���H��ĳ���]�w�����AGPS��CameraPreview���B�@
		// �g���ն���LocationListener�A�BListener����Ū����m���O�Alocation�~�ॿ�`��s�F�_�h�A�g�n�׮ɶ������|��s
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
						"�L�k�s���۾�", Toast.LENGTH_SHORT).show();
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
//		myParameters.setPreviewSize(w, h);//�ù��j�p
//		myParameters.set("orientation", "portrait");
//		myParameters.set("orientation", "landscape");
//		myParameters.set("rotation",270);
		myCamera.setParameters(myParameters);
//		myCamera.startPreview();//�ߧY����Preview   	
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
		myViewInfo.setText("����ǰe\n");
		
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
			myViewInfo.setText("�w��Ӷǰe�G" + alarmCounted + "��\n");		
			Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
			ImageView myImageView = (ImageView) findViewById(R.id.imageView);
			myImageView.setImageBitmap(bm);//�N�Ϥ���ܨ�U�誺ImageView��
			
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
        //�᭱�y�A�������O�O�Ѽơ]�Ҥl�̬O�u�{�𮧮ɶ��^�A�i��(publishProgress�Ψ�)�A��^�� ����  
          
        @Override  
        protected void onPreExecute() {  
            //�Ĥ@�Ӱ����k  
            super.onPreExecute();  
        }  
          
        @Override  
        protected String doInBackground(byte[]... params) {  
            //�ĤG�Ӱ����k,onPreExecute()���槹�����
//        	LocationManager myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//    		Criteria criteria = new Criteria();	//��T���Ѫ̿���з�
//    		String bestProvider = myLocationManager.getBestProvider(criteria, true);	//��ܺ�ǫ׳̰������Ѫ�
//    		Location  myLocation = myLocationManager.getLastKnownLocation(bestProvider);
//		    Location myLocation = myLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//		    Location myLocation = myLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    		String bestProvider = myLocationManager.getBestProvider(new Criteria(), true);	//��ܺ�ǫ׳̰������Ѫ�
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
				myViewInfo.append("�ɶ�:" + String.valueOf(new Date(myLocation.getTime())) + "\n");
				myViewInfo.append("�g��:" +  String.valueOf(myLocation.getLongitude()) + "\n");
				myViewInfo.append("�n��:" +  String.valueOf(myLocation.getLatitude()) + "\n");
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
			        
				myViewInfo.append("�L�k���o��m��T\n");
			}
			myViewInfo.append("�v���j�p:" + String.valueOf(params[0].length) + "\n");
			return "�ǰe���\";
        }  
  
        @Override  
        protected void onProgressUpdate(Integer... progress) {  
            //�o�Ө�ƦbdoInBackground�ե�publishProgress��Ĳ�o�A���M�եήɥu���@�ӰѼ�  
            //���O�o�̨��쪺�O�@�ӼƲ�,�ҥH�n��progesss[0]�Ө���  
            //��n�ӰѼƴN��progress[n]�Ө���  
            //tv.setText(progress[0]+"%");  
            super.onProgressUpdate(progress);  
        }  
  
        @Override  
        protected void onPostExecute(String result) {  
            //doInBackground��^��Ĳ�o�A���y�ܻ��A�N�OdoInBackground���槹��Ĳ�o  
            //�o�̪�result�N�O�W��doInBackground����᪺��^�ȡA�ҥH�o�̬O"���槹��"  
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

		myViewInfo.setText("�ثe�ѼƳ]�w: \n" +
				"�s�u�D����}��: " + serverIp + ":" + serverPort +"\n" +
				"����ɶ�: " + timeDelay + "\n" +
				"���j�ɶ�: " + timeInterval + "\n" +
				"�ǰe�v���ѪR��: " + pictureSize);
	}

	public void onLocationChanged(Location arg0) {
		// �g���ն��U�F�H�U���O�Alocation�~�ॿ�`��s�F�_�h�A�g�n�׮ɶ������|��s
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
