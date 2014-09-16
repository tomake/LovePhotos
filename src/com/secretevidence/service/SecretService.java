package com.secretevidence.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.secretevidence.action.AudioRecorder;
import com.secretevidence.action.SDCardStorage;
import com.secretevidence.action.SavedPath;
import com.secretevidence.activity.VideoActivity;
import com.secretevidence.database.SQLiteManager;

/**
 * 
 * ��̨����ʵ��    <��Ҫģ��>
 * @author MAKE&BE
 *
 */
public class SecretService extends Service {  // 

	private int originRingMode;   		  // Ĭ���龰ģʽ
	private AudioManager am;			  // ��Ƶ¼����
	private BroadcastReceiver br;		  // �㲥
	private PowerManager pm;			  // ��Դ������
	private WakeLock mWakeLock;			  // 
	private SensorManager mSensorManager; // ������������
	private Vibrator mVibrator;    		  // ����
	private int count =0;				  // ҡ������
	private int SHAKE_SIGNAL=1;           // ҡ���ź�
	private int SHAKE_MSG=-1;				// ҡһҡ��Ϣ
	private int PHOTO_MSG=0;				// ҡһҡ��Ϣ
	private int AUDIO_MSG=1;				// ҡһҡ��Ϣ
	private long REQUIRE_MIN_INTERVAL=500; 	// �������´���������С�¼���� 500ms
	private long REQUIRE_MAX_INTERVAL=3000; // �������´�����������¼���� 3000ms
	private long lastShakeTime=0;		  		// �������һ��ҡ���ֻ���ʱ��
	private long currentShakeTime=0;	 		// �����ֻ�ҡ���ĵ�ǰʱ��
	private boolean isAudioMode=false ;			// ¼��ģʽ
	private boolean isPictureMode=false;  		// ����ģʽ


	private SQLiteManager sm;					// ���ݿ����
	private static String MEDIA_TYPE="video";	// ý������
	private String DATABASE_ERROR="���ݿ����ʧ��";	// ��ʾ��Ϣ
	private String DATABASE_SUCCESS="���ݿ�����ɹ���";// ��ʾ��Ϣ
	private static String  currentSavedPath="pathInit"; // ��ǰ�洢·��
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";
	private int STORAGE_LIMITED=-1;				// �洢�ռ䲻����
	private boolean isCurrent=true;
	private Thread storageThread;

	/**
	 * ��Ӧ�û������¼�
	 */
	@SuppressLint("HandlerLeak")
	private Handler handler =new Handler(){  // ��Ӧ�û������������Ӧģʽ  ��  ����SD�洢����������Ϣ����������Ӧ
		@Override 
		public void handleMessage(Message msg){
			if(msg.what==SHAKE_SIGNAL){
				if(SHAKE_MSG==PHOTO_MSG)
				{
					isPictureMode=true;
					isAudioMode=false;
					SHAKE_MSG=-1;
					releaseAudioRecorder();
					initCamera();
					System.out.println("\n\n Picture Mode \n\n");
				}
				//				else if(SHAKE_MSG==AUDIO_MSG){
				//					isAudioMode=true;
				//					isPictureMode=false ;
				//					SHAKE_MSG=-1;
				//
				//					releaseCamera();
				//					System.out.println("\n\n Audio Mode \n\n");
				//				}
				//				System.out.println("\n\n�������ֻ���"+count+++"��ҡ��  \n\n");
			}

			if(msg.what==STORAGE_LIMITED){
				releaseSensor();
				unregisterBroadcast();
				releaseCpuAwake();
				releaseCamera();
				releaseAudioRecorder();
				closeDatabase();
				android.os.Process.killProcess(android.os.Process.myPid()); 
			}


		}
	};

	/**
	 * ʵʱ���SD��������С��10Mʱ��ʾ�ڴ治�㲢�˳�
	 */
	public void startStotageDetecte(){
		storageThread=new Thread(new Runnable(){
			@Override 
			public void run(){
				while(isCurrent){
					if(SDCardStorage.getAvailableStorage()<10)
					{
						isCurrent=false ;
						Message msg=new Message();
						msg.what=STORAGE_LIMITED;
						handler.sendMessage(msg);
					}
					try{
						Thread.sleep(20000);   // 20s�����¼��洢�ռ�
						System.out.println("Service ���SD�� , ��ǰʣ��ռ� "+SDCardStorage.getAvailableStorage()+" M");
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}

		});
		storageThread.start();
	}




	/**
	 * ���ô�����������
	 */
	private SensorEventListener mSensorEventListener=new SensorEventListener(){
		@Override 
		public void onSensorChanged(SensorEvent event){
			float[] values=event.values;
			float x=values[0];  // x�᷽����������ٶȣ�����Ϊ��
			float y=values[1];  // y�᷽����������ٶȣ���ǰΪ��
			float z=values[2];  // z�᷽����������ٶȣ�����Ϊ��

			int mid=19;
			if(x>mid||x<-mid||y>mid||y<-mid||z>mid||z<-mid){

				mVibrator.vibrate(200);
				if(!isPictureMode){
					isPictureMode=true;
					Message msg=new Message();
					msg.what=SHAKE_SIGNAL;
					SHAKE_MSG=PHOTO_MSG;
					handler.sendMessage(msg);
				}

				//				if(isPictureMode){
				//					releaseCamera();
				//				}
				//				if(isAudioMode){
				//					releaseAudioRecorder();
				//				}
				//				mVibrator.vibrate(200);

				//				//mVibrator.vibrate(new long[]{100,125,200,125},-1);
				//				lastShakeTime=currentShakeTime;
				//				currentShakeTime=System.currentTimeMillis();
				//				long timeInterval=currentShakeTime-lastShakeTime;
				//				if(timeInterval>REQUIRE_MIN_INTERVAL&&timeInterval<REQUIRE_MAX_INTERVAL){
				//					Message msg=new Message();
				//					msg.what=SHAKE_SIGNAL;
				//					SHAKE_MSG=PHOTO_MSG;
				//					handler.sendMessage(msg);
				//				}
				//				else if(timeInterval>REQUIRE_MAX_INTERVAL){
				//					Message msg=new Message();
				//					msg.what=SHAKE_SIGNAL;
				//					SHAKE_MSG=AUDIO_MSG;
				//					handler.sendMessage(msg);
				//				}



			}
		}

		@Override 
		public void onAccuracyChanged(Sensor sensor , int accuracy){

		}
	};


	/**
	 * ��ʼ���㲥
	 */
	public  void initBroadcast(){

		IntentFilter intentf=new IntentFilter();;
		br=new BroadcastReceiver(){
			@Override  
			public void onReceive(Context context,Intent intent){
				String action=intent.getAction();
				if(Intent.ACTION_SCREEN_ON.equals(action)){
					if(isPictureMode){

					}
					else if(isAudioMode){
						releaseAudioRecorder();
					}
					System.out.println("\n\nscreen  on \n\n");
				}
				else if(Intent.ACTION_SCREEN_OFF.equals(action)){

					if(isPictureMode){
						MEDIA_TYPE="picture";
						takePicture();
					}
					else if(isAudioMode){
						MEDIA_TYPE="audio";
						startAudioRecorder();
						System.out.println("\n\n\n\n");
					}

					System.out.println("\n\nscreen off \n\n");
				}
				else if(Intent.ACTION_USER_PRESENT.equals(action)){

					System.out.println("\n\nscreen unlock \n\n");
				}

			}
		};
		intentf.addAction(Intent.ACTION_SCREEN_OFF);
		intentf.addAction(Intent.ACTION_SCREEN_ON);
		intentf.addAction(Intent.ACTION_USER_PRESENT);
		this.registerReceiver(br, intentf);
		System.out.println("\n\n BroadcastReceiver init \n\n");
	}


	/**
	 * ��д����
	 */
	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	/**
	 * ��д����
	 */
	@SuppressLint("ServiceCast")
	@Override 
	public void onCreate(){
		super.onCreate();

		initSensor();
		initBroadcast();
		initDatabase();
		setCpuAwake();
		startStotageDetecte();
		System.out.println("\n\nService.onCreate()����");

	}

	/**
	 * ��д����
	 */
	@Override 
	public int onStartCommand(Intent intent, int flags, int startId){
		//startBackgroundTask(intent,startId);
		System.out.println("\n\nService.onStartCommand() ��ʼ");
		return Service.START_STICKY;
	}

	/**
	 * ��д����
	 */
	@Override 
	public void onDestroy(){
		super.onDestroy();
		isCurrent=false;
		releaseSensor();
		unregisterBroadcast();
		releaseCpuAwake();
		releaseCamera();
		releaseAudioRecorder();
		closeDatabase();
		System.out.println("\n\nService.onDestroy()����");

	}

	/**
	 * ��ʼ��������
	 */
	public void initSensor(){
		mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
		mVibrator=(Vibrator)getApplication().getSystemService(Service.VIBRATOR_SERVICE);

		//  ע�������    ��һ��������Listener���ڶ������������ô��������ͣ�����������ֵ��ȡ��������Ϣ��Ƶ��
		mSensorManager.registerListener(mSensorEventListener, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		System.out.println("\n\n initSensor  \n\n");
	}

	/**
	 * �ͷŴ����������Դ
	 */
	public void releaseSensor(){
		if(mSensorManager!=null)
			mSensorManager.unregisterListener(mSensorEventListener);
		System.out.println("\n\n releaseSensor  \n\n");
	}


	/**
	 * ע���㲥
	 */
	public void unregisterBroadcast(){
		if(br!=null)
			this.unregisterReceiver(br);
		System.out.println("\n\n BroadcastReceiver unregister \n\n");
	}

	/**
	 * ���ô���ģʽ
	 */
	public void setCpuAwake(){
		if(mWakeLock==null){
			pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
			mWakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cpuAwake");
			mWakeLock.acquire();
			System.out.println("\n\nSecretService mWakeLock acquire \n\n");
		}
	}

	/**
	 * �ָ�����ģʽ�����ͷ������Դ
	 */
	public void releaseCpuAwake(){
		if(mWakeLock!=null){
			mWakeLock.release();
			System.out.println("\n\nSecretService mWakeLock release \n\n");
		}
	}


	/**
	 * �����龰ģʽΪ����  ����������վ����������Խ��������Ч��
	 */
	public void setRingMode(){
		if(am==null){
			am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
			originRingMode=am.getRingerMode();
			am.setRingerMode(AudioManager.RINGER_MODE_SILENT);

			System.out.println("\n\n RingMode is Setted \n\n");
		}
	}

	/**
	 * �ָ��龰ģʽ���ͷ������Դ
	 */
	public void restoreRingMode(){
		if(am!=null){
			am.setRingerMode(originRingMode);
			am=null;
			System.out.println("\n\n RingMode is restore\n\n");
		}
	}

	/**
	 * ���չ���ʵ��
	 */
	private Camera mCamera;
	private AudioRecorder ar;
	private MediaRecorder mr;

	// ��д����
	private ShutterCallback sc=new ShutterCallback(){
		@Override  
		public void onShutter(){

		}
	};

	// ��д����
	private PictureCallback pc=new PictureCallback(){
		@Override 
		public void onPictureTaken(byte[] data , Camera camera){
			Bitmap bm=BitmapFactory.decodeByteArray(data, 0, data.length);
			currentSavedPath =SavedPath.getPictureSavedPath(getSavedDirectory());
			File file=new File(currentSavedPath);
			insertData(currentSavedPath);
			System.out.println("\n\n take picture start "+currentSavedPath+"\n");

			try {
				BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(file));
				Matrix m=new Matrix();
				m.setRotate(90,bm.getWidth()/2,bm.getHeight()/2);
				Bitmap bm1= Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight(),m,true);
				bm1.compress(Bitmap.CompressFormat.JPEG, 30, bos);
				bos.flush();
				bos.close();
				mCamera.startPreview();
			} catch ( Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("\n\npicturecallback ex\n\n");
			}

		}
	};

	// ��д����
	private PictureCallback raw=new PictureCallback(){
		@Override 
		public void onPictureTaken(byte[] data , Camera camera){

		}
	};

	/**
	 * ��ʼ�����
	 */
	@SuppressLint("NewApi")
	public void initCamera(){
		if(mCamera==null){
			System.out.println("\n\n init Camera \n\n");
			mCamera=Camera.open();
			Camera.Parameters p=mCamera.getParameters();
			p.setPictureFormat(PixelFormat.JPEG);
			p.setJpegQuality(100);
			p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			try{
				mCamera.setParameters(p);
			}
			catch(Exception ee){
				ee.printStackTrace();
			}
			mCamera.setDisplayOrientation(90);
			mCamera.startPreview();
			mCamera.autoFocus(new AutoFocusCallback(){
				@Override 
				public void onAutoFocus(boolean success , Camera camera){

				}
			});

			System.out.println("\n\n init Camera successfully \n\n");

		}
	}

	/**
	 * �ͷ������Դ
	 */
	public void releaseCamera(){
		if(mCamera!=null){
			mCamera.stopPreview();
			mCamera.release();
			mCamera=null;
			System.out.println("\n\n Camera Release \n\n");
		}
	}

	/**
	 * ����
	 */
	public void takePicture(){
		if(mCamera!=null){
			mCamera.takePicture(sc, raw, pc);
			mVibrator.vibrate(50);
			System.out.println("\n\n take picture end "+currentSavedPath+"\n");
		}
	}


	/**
	 * ��ʼ¼��
	 */
	public void startAudioRecorder(){
		if(ar==null){
			currentSavedPath=SavedPath.getAudioSavedPath(getSavedDirectory());
			ar=new AudioRecorder();
			try {
				ar.startRecord(currentSavedPath);
				System.out.println("\n\n start recorde  "+currentSavedPath+"\n");
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("\n*********\n*********AudioRecord illegalState\n*******\n**********");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("\n*********\n*********AudioRecord IO Exception\n*******\n**********");
			}

		}
	}

	/**
	 * ����¼�����ͷ������Դ
	 */
	public void releaseAudioRecorder(){

		if(ar!=null){
			ar.stopRecord();
			ar=null;
			insertData(currentSavedPath);
			System.out.println("\n\n stop recorde \n\n");
		}
	}



	/**
	 * ��ȡ���洢Ŀ¼
	 * @return
	 */
	@SuppressLint("NewApi")
	public String getSavedDirectory(){   
		String path;
		if(android.os.Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED)){
			path=Environment.getExternalStorageDirectory()+"";
		}
		else {
			path= getExternalCacheDir()+"";
		}

		return path;
	}

	/**
	 * ��ʼ�����ݿ�
	 */
	public void initDatabase(){
		if(sm==null){
			String dir=this.getSavedDirectory()+"/SecretEvidence/database";
			String databaseName=this.getSavedDirectory()+"/SecretEvidence/database/file.db";
			File directory=new File(dir);
			if(!directory.exists()){
				directory.mkdirs() ;
			}
			File file=new File(databaseName);
			if(!file.exists()){
				try {
					file.createNewFile();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("�쳣�����ݿ��ļ�����ʧ��");
				}
			}
			System.out.println("\n  init database \n");
			sm=new SQLiteManager(databaseName);
			sm.createOrOpenDatabase(sql);
		}
	}

	/**
	 * ��������
	 * @param data
	 */
	public void insertData(String data){
		if(sm!=null){
			String sql="insert into RecordFile(filePath,fileType) values('"+data+"','"+this.MEDIA_TYPE+"')";
			try{
				sm.insertData(sql);
				System.out.println(this.DATABASE_SUCCESS);
			}
			catch (Exception e){
				e.printStackTrace();	
				System.out.println(this.DATABASE_ERROR);
			}
		}
	}

	/**
	 * �ر����ݿ�
	 */
	public void closeDatabase(){
		if(sm!=null){
			sm.closeDatabase();

		}
	}



}
