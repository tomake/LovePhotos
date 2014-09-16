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
 * 后台功能实现    <主要模块>
 * @author MAKE&BE
 *
 */
public class SecretService extends Service {  // 

	private int originRingMode;   		  // 默认情景模式
	private AudioManager am;			  // 音频录制器
	private BroadcastReceiver br;		  // 广播
	private PowerManager pm;			  // 电源管理器
	private WakeLock mWakeLock;			  // 
	private SensorManager mSensorManager; // 传感器控制器
	private Vibrator mVibrator;    		  // 震动器
	private int count =0;				  // 摇动次数
	private int SHAKE_SIGNAL=1;           // 摇动信号
	private int SHAKE_MSG=-1;				// 摇一摇信息
	private int PHOTO_MSG=0;				// 摇一摇信息
	private int AUDIO_MSG=1;				// 摇一摇信息
	private long REQUIRE_MIN_INTERVAL=500; 	// 设置振动事触发件的最小事件间隔 500ms
	private long REQUIRE_MAX_INTERVAL=3000; // 设置振动事触发件的最大事件间隔 3000ms
	private long lastShakeTime=0;		  		// 设置最后一次摇动手机的时间
	private long currentShakeTime=0;	 		// 设置手机摇动的当前时间
	private boolean isAudioMode=false ;			// 录音模式
	private boolean isPictureMode=false;  		// 拍照模式


	private SQLiteManager sm;					// 数据库管理
	private static String MEDIA_TYPE="video";	// 媒体类型
	private String DATABASE_ERROR="数据库操作失败";	// 提示信息
	private String DATABASE_SUCCESS="数据库操作成功！";// 提示信息
	private static String  currentSavedPath="pathInit"; // 当前存储路径
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";
	private int STORAGE_LIMITED=-1;				// 存储空间不足标记
	private boolean isCurrent=true;
	private Thread storageThread;

	/**
	 * 响应用户控制事件
	 */
	@SuppressLint("HandlerLeak")
	private Handler handler =new Handler(){  // 响应用户操作，进入对应模式  、  接收SD存储容量不足信息，并做出响应
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
				//				System.out.println("\n\n监听到手机第"+count+++"次摇动  \n\n");
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
	 * 实时检测SD卡容量，小于10M时提示内存不足并退出
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
						Thread.sleep(20000);   // 20s后重新检测存储空间
						System.out.println("Service 检测SD卡 , 当前剩余空间 "+SDCardStorage.getAvailableStorage()+" M");
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
	 * 设置传感器监听器
	 */
	private SensorEventListener mSensorEventListener=new SensorEventListener(){
		@Override 
		public void onSensorChanged(SensorEvent event){
			float[] values=event.values;
			float x=values[0];  // x轴方向的重力加速度，向右为正
			float y=values[1];  // y轴方向的重力加速度，向前为正
			float z=values[2];  // z轴方向的重力加速度，向上为正

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
	 * 初始化广播
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
	 * 重写方法
	 */
	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	/**
	 * 重写方法
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
		System.out.println("\n\nService.onCreate()创建");

	}

	/**
	 * 重写方法
	 */
	@Override 
	public int onStartCommand(Intent intent, int flags, int startId){
		//startBackgroundTask(intent,startId);
		System.out.println("\n\nService.onStartCommand() 开始");
		return Service.START_STICKY;
	}

	/**
	 * 重写方法
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
		System.out.println("\n\nService.onDestroy()销毁");

	}

	/**
	 * 初始化传感器
	 */
	public void initSensor(){
		mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
		mVibrator=(Vibrator)getApplication().getSystemService(Service.VIBRATOR_SERVICE);

		//  注册监听器    第一个参数是Listener，第二个参数是所得传感器类型，第三个参数值获取传感器信息的频率
		mSensorManager.registerListener(mSensorEventListener, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		System.out.println("\n\n initSensor  \n\n");
	}

	/**
	 * 释放传感器相关资源
	 */
	public void releaseSensor(){
		if(mSensorManager!=null)
			mSensorManager.unregisterListener(mSensorEventListener);
		System.out.println("\n\n releaseSensor  \n\n");
	}


	/**
	 * 注销广播
	 */
	public void unregisterBroadcast(){
		if(br!=null)
			this.unregisterReceiver(br);
		System.out.println("\n\n BroadcastReceiver unregister \n\n");
	}

	/**
	 * 设置待机模式
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
	 * 恢复待机模式，并释放相关资源
	 */
	public void releaseCpuAwake(){
		if(mWakeLock!=null){
			mWakeLock.release();
			System.out.println("\n\nSecretService mWakeLock release \n\n");
		}
	}


	/**
	 * 设置情景模式为静音  （想控制拍照静音，但测试结果表明无效）
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
	 * 恢复情景模式并释放相关资源
	 */
	public void restoreRingMode(){
		if(am!=null){
			am.setRingerMode(originRingMode);
			am=null;
			System.out.println("\n\n RingMode is restore\n\n");
		}
	}

	/**
	 * 拍照功能实现
	 */
	private Camera mCamera;
	private AudioRecorder ar;
	private MediaRecorder mr;

	// 重写方法
	private ShutterCallback sc=new ShutterCallback(){
		@Override  
		public void onShutter(){

		}
	};

	// 重写方法
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

	// 重写方法
	private PictureCallback raw=new PictureCallback(){
		@Override 
		public void onPictureTaken(byte[] data , Camera camera){

		}
	};

	/**
	 * 初始化相机
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
	 * 释放相机资源
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
	 * 拍照
	 */
	public void takePicture(){
		if(mCamera!=null){
			mCamera.takePicture(sc, raw, pc);
			mVibrator.vibrate(50);
			System.out.println("\n\n take picture end "+currentSavedPath+"\n");
		}
	}


	/**
	 * 开始录音
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
	 * 结束录音并释放相关资源
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
	 * 获取父存储目录
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
	 * 初始化数据库
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
					System.out.println("异常！数据库文件创建失败");
				}
			}
			System.out.println("\n  init database \n");
			sm=new SQLiteManager(databaseName);
			sm.createOrOpenDatabase(sql);
		}
	}

	/**
	 * 插入数据
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
	 * 关闭数据库
	 */
	public void closeDatabase(){
		if(sm!=null){
			sm.closeDatabase();

		}
	}



}
