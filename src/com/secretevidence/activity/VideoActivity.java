package com.secretevidence.activity;

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.secretevidence.R;
import com.secretevidence.action.SDCardStorage;
import com.secretevidence.action.SavedPath;
import com.secretevidence.database.SQLiteManager;

/**
 * 实现视频录制功能   <主要模块>
 * @author MAKE&BE
 *
 */
public class VideoActivity extends Activity implements OnClickListener,SurfaceHolder.Callback{

	private PowerManager.WakeLock mWakeLock;
	private MediaRecorder mRecorder;		// 摄像机
	private Camera mCamera;					// 摄像头
	private SurfaceHolder holder;			// 预览相关
	private ImageButton ib_back;
	private ToggleButton tb_videoRecord;    // 录制开关
	private ImageButton ib_preview;  
	private ImageButton ib_audio;
	private ImageButton ib_photo;			// 拍照/录音/摄像 模式切换
	private SurfaceView sv_video;			// 录像预览
	private TextView tv_videoTime;			// 显示录制时间
	private boolean isRecording =false;     // 录制状态 
	private String INIT_TIME="00:00:00";	// 初始化计时器
	private String TIMER_EXCEPTION="计时器异常"; // 提示信息
	private String IO_EXCEPTION="文件存储异常";	// 提示信息
	private String RECORDER_INIT_EXCEPTION="摄像机初始化异常";  // 提示信息
	private String MEDIA_SAVED="视频已保存"; 	// 提示信息
	private String NO_SD_CARD_TIPS="SD卡不存在，软件无法正常使用！"; // 提示信息
	private String STORAGE_NOT_ENOUGH="存储空间不足！请清理空间后尝试。";// 提示信息
	private final int STATE_RECORDING=1;    // 标记录制状态
	private int STORAGE_LIMITED=-1;				// 存储空间不足标记
	private boolean isCurrent=true;

	private SQLiteManager sm;
	private String MEDIA_TYPE="video";
	private String DATABASE_ERROR="数据库操作失败";
	private String DATABASE_SUCCESS="数据库操作成功！";
	private static String  currentSavedPath="pathInit";
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";

	private int totalTime=0;                // 录制总时间
	@SuppressLint("HandlerLeak")
	private Handler handler=new Handler(){  // 接收计时信息，显示当前总计时 、接收SD存储容量不足信息，并做出响应
		@Override
		public void handleMessage(Message msg){
			if(msg.what==STATE_RECORDING){
				tv_videoTime.setText(getRecordingTime());
			}

			if(msg.what==STORAGE_LIMITED){
				stopRecord();
				checkStorage();
			}
			
		}
	};

	/**
	 * 程序入口
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_video);
		System.out.println("\n***************\n**********\nVideo Activity start \n\n*********\n************\n***********\n");
		initViewsId();
		setOnClickListener();

		holder=this.sv_video.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		this.tv_videoTime.setText(INIT_TIME);
		startTimer();
		startStotageDetecte();
		setScreenAwake();
		initDatabase();
	}

	/**
	 * 界面销毁时释放相关资源
	 */
	@Override
	public  void onDestroy(){
		super.onDestroy();
		setScreenAwakeRelease();
		closeDatabase();
		stopRecord();
	}

	/**
	 * 重写onPause方法
	 */
	@Override 
	public void onPause(){
		super.onPause();
		setScreenAwakeRelease();
		isCurrent=false ;
	}

	 

	@Override
	public void onStart(){
		super.onStart();
		if(!isCurrent){
			isCurrent=true;
			startStotageDetecte();
		}
		 
	}
	
	/**
	 * 重写onResume方法
	 */
	@Override 
	public void onResume(){
		super.onResume();
		setScreenAwake();
	}

	/**
	 * 初始化控件ID
	 */
	private void initViewsId(){
		this.ib_back=(ImageButton)findViewById(R.id.ib_back);
		this.ib_preview=(ImageButton)findViewById(R.id.ib_preview);
		this.tb_videoRecord=(ToggleButton)findViewById(R.id.tb_videoRecord);
		this.ib_photo=(ImageButton)findViewById(R.id.ib_photo);
		this.ib_audio=(ImageButton)findViewById(R.id.ib_audio);
		this.tv_videoTime=(TextView)findViewById(R.id.tv_videoTime);
		this.sv_video=(SurfaceView)findViewById(R.id.sv_video);
	}

	/**
	 * 绑定控件点击事件
	 */
	private void setOnClickListener(){
		this.ib_back.setOnClickListener(this);
		this.ib_preview.setOnClickListener(this);
		this.tb_videoRecord.setOnClickListener(this);
		this.ib_photo.setOnClickListener(this);
		this.ib_audio.setOnClickListener(this);

	}

	/**
	 * 实现控件监听事件
	 */
	public void onClick(View v){

		switch(v.getId()){
		case R.id.ib_back:
			if(isRecording){
				System.out.println("\n pause : "+currentSavedPath+"\n");
				stopRecord();
				this.insertData(currentSavedPath);
				this.tb_videoRecord.setChecked(false);
				isRecording=false ;
				totalTime=0;
				this.tv_videoTime.setText(INIT_TIME);
			}
			stopRecord();
			Intent intent0 = new Intent(this,MainActivity.class);
			startActivity(intent0);
			this.finish();
			break;
		case R.id.ib_preview:
			if(isRecording){
				System.out.println("\n pause : "+currentSavedPath+"\n");
				stopRecord();
				this.insertData(currentSavedPath);
				this.tb_videoRecord.setChecked(false);
				isRecording=false ;
				totalTime=0;
				this.tv_videoTime.setText(INIT_TIME);
			}
			stopRecord();
			Intent intent1 = new Intent(this,RecordFilesActivity.class);
			startActivity(intent1);
			break;
		case R.id.tb_videoRecord:
			if(this.tb_videoRecord.isChecked()){
				if(mCamera==null&&mRecorder==null)
					initRecorder();
				startRecord();
				isRecording=true;
			}
			else {
				System.out.println("\n stop : "+currentSavedPath+"\n");
				stopRecord();
				this.insertData(currentSavedPath);
				isRecording=false;
				totalTime=0;
				this.tv_videoTime.setText(INIT_TIME);
				showToast(MEDIA_SAVED);
			}
			break;
		case R.id.ib_photo:
			stopRecord();
			if(isRecording){
				this.insertData(currentSavedPath);
				this.tb_videoRecord.setChecked(false);
				isRecording=false;
				totalTime=0;
				this.tv_videoTime.setText(INIT_TIME);
				System.out.println("\n mode change : "+currentSavedPath+"\n");
			}
			Intent intent2 =new Intent(this,PhotoActivity.class);
			startActivity(intent2);
			this.finish();
			break;
		
		case R.id.ib_audio:
			if(isRecording){
				System.out.println("\n pause : "+currentSavedPath+"\n");
				stopRecord();
				this.insertData(currentSavedPath);
				this.tb_videoRecord.setChecked(false);
				isRecording=false ;
				totalTime=0;
				this.tv_videoTime.setText(INIT_TIME);
			}
			stopRecord();
			Intent intent3 = new Intent(this,AudioActivity.class);
			startActivity(intent3);
			this.finish();
			break;
		default:
			break;
		}
	}

	/**
	 * 计时器开启
	 */
	public void startTimer(){
		// 开计时器线程
		new Thread(new Runnable(){
			@Override
			public void run(){
				while(true){
					if(tb_videoRecord.isChecked()){
						Message msg=new Message();
						msg.what=STATE_RECORDING;
						handler.sendMessage(msg);
						totalTime++;
					}
					try{
						Thread.sleep(1000);
					}catch (Exception e){
						e.printStackTrace();
						showToast(TIMER_EXCEPTION);
					}
				}

			}

		}).start();
	}

	
	/**
	 * 实时检测SD卡容量，小于10M时提示内存不足并退出
	 */
	public void startStotageDetecte(){
		new Thread(new Runnable(){
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
						Thread.sleep(10000);   // 10s后重新检测存储空间
						System.out.println("Video 检测SD卡 , 当前剩余空间 "+SDCardStorage.getAvailableStorage()+" M");
					}
					catch(Exception e){
						showToast("SD卡检测异常");
					}
				}
			}

		}).start();
	}
	
	
	/**
	 * 初始化摄像机
	 */
	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	public void initRecorder(){
		
		mCamera=Camera.open();
		Camera.Parameters p=mCamera.getParameters();
		p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		mCamera.setParameters(p);
		mCamera.setDisplayOrientation(90);
		mCamera.unlock();
		mRecorder=new MediaRecorder();
		mRecorder.setCamera(mCamera);

		mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		mRecorder.setPreviewDisplay(holder.getSurface());
		currentSavedPath=SavedPath.getVideoSavedPath(this.getSavedDirectory());
		System.out.println("\n"+currentSavedPath+"\n");
		File file=new File(currentSavedPath);
		mRecorder.setOutputFile(currentSavedPath);
		try {
			mRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			showToast(RECORDER_INIT_EXCEPTION);
		} catch (IOException e) {
			e.printStackTrace();
			showToast(IO_EXCEPTION);
		}
	}


	/**
	 * 开始录制
	 */
	public void startRecord(){
		if(mCamera!=null&&mRecorder!=null){
			mRecorder.start();
			System.out.println("\n start "+this.currentSavedPath+"\n");
		}
	}


	/**
	 * 停止录制，并释放录像相关资源
	 */
	public void stopRecord(){

		if(mRecorder!=null){
			if(isRecording)
				mRecorder.stop();
			mRecorder.release();
			mRecorder=null;
			mCamera.lock();
			System.out.println("\n recorder release \n");
		}
		if(mCamera!=null){
			mCamera.release();
			mCamera=null;
			System.out.println("\n stop recording ,camera release \n");
		}
	}

	/**
	 * 重写surfaceCreated方法
	 */
	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	@Override
	public void surfaceCreated(SurfaceHolder holder){
//		if(!isRecording)
//			initRecorder();
		System.out.println("\n\ninit VideoRecorder | is recording?\n\n"+isRecording);
	}

	/**
	 * 重写surfaceChanged方法
	 */
	public void surfaceChanged(SurfaceHolder holder,int format,int w,int h){

		
	}

	/**
	 * 重写surfaceDestroyed方法
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder){
		//stopRecord();
	}

	/**
	 * 格式化录制时间
	 * @return  返回录制时间
	 */
	public String getRecordingTime(){
		int hour=totalTime/3600;
		int min=totalTime/60;
		int sec=totalTime%60;
		return (hour>=10 ? hour+"" : "0"+hour) +":"
		+ (min>=10 ? min+"" : "0"+min)+":"
		+ (sec>=10 ? sec+"" : "0" + sec);

	}

	/**
	 *  重写返回按钮事件 ， 用户单击返回键时提示   退出/后台运行
	 */
	@Override
	public boolean onKeyDown(int  keyCode , KeyEvent event){

		if(keyCode==KeyEvent.KEYCODE_BACK&&event.getNumber()==0){
			//super.onBackPressed();
			new AlertDialog.Builder(VideoActivity.this)
			.setTitle("选择操作")
			.setMessage("确认退出？")
			.setPositiveButton("退出",new DialogInterface.OnClickListener() {//确定按钮
				@Override
				public void onClick(DialogInterface dialog, int which) {
					stopRecord();
					if(isRecording){
						insertData(currentSavedPath);
						System.out.println("\n\n exit :"+currentSavedPath);
					}
					VideoActivity.this.finish();
				}
			}) 
			.setNegativeButton("取消",new DialogInterface.OnClickListener() {//确定按钮
				@Override
				public void onClick(DialogInterface dialog, int which) {
					 
				}
			})
			.show();
		}
		return false ;
	}
	
	/**
	 * 保持屏幕唤醒
	 */
	public void setScreenAwake(){
		if(mWakeLock==null){
			PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
			mWakeLock=pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "awake");
			mWakeLock.acquire();
		}
	}

	/**
	 * 恢复屏幕唤醒模式
	 */
	public void setScreenAwakeRelease(){
		if(mWakeLock!=null){
			mWakeLock.release();
			mWakeLock=null;
		}
	}

	/**
	 * 用于弹出各种提示消息
	 * @param msg 消息内容
	 */
	public void showToast(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}
	
	
	/**
	 * 获取文件存储父目录
	 * @return  返回存储路径
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
	 * 检测SD卡剩余容量  小于10M时提示并退出
	 */
	public void checkStorage(){
		if(SDCardStorage.getAvailableStorage()<10){
			new AlertDialog.Builder(this)
			.setTitle("敬告")
			.setMessage(STORAGE_NOT_ENOUGH)
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					VideoActivity.this.finish();
					android.os.Process.killProcess(android.os.Process.myPid()); 
				}
			}).show();
		}
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
						file.createNewFile() ;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					showToast("异常！数据库文件创建失败");
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
			}
			catch (Exception e){
				e.printStackTrace();	
				showToast(this.DATABASE_ERROR);
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
