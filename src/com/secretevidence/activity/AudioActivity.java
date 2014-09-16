package com.secretevidence.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.secretevidence.R;
import com.secretevidence.action.AudioRecorder;
import com.secretevidence.action.SDCardStorage;
import com.secretevidence.action.SavedPath;
import com.secretevidence.database.SQLiteManager;
import com.secretevidence.service.SecretService;

/**
 * 实现录音功能   <主要模块>
 * @author MAKE&BE
 *
 */
public class AudioActivity extends Activity implements OnClickListener {
	
	private NotificationManager nm;
	
	private TextView tv_audioTime;				// 显示录制时间
	private ImageButton ib_back;
	private ImageButton ib_preview;		
	private ToggleButton tb_audioRecord;		// 录音开关
	private ImageButton ib_photo;				// 拍照/录音/摄像  模式切换 
	private ImageButton ib_video;
	private AudioRecorder mAudioRecorder;		// 录音机
	private String INIT_TIME="00:00:00";		// 初始化录制时间 
	private String TIMER_EXCEPTION="计时器异常";	// 提示信息
	private String IO_EXCEPTION="文件存储异常";		// 提示信息
	private String INIT_RECORDER_EXCEPTION="初始化录音机异常"; // 提示信息
	private String NO_SD_CARD_TIPS="SD卡不存在，软件无法正常使用！"; // 提示信息
	private String STORAGE_NOT_ENOUGH="存储空间不足！请清理空间后尝试。";// 提示信息
	private String AUDIO_SAVED="录音文件已保存";		// 提示信息
	private static String  currentSavedPath="pathInit";  // 文件存储路径
	
	private SQLiteManager sm;						// 数据库管理
	private String MEDIA_TYPE="audio";				// 媒体类型
	private String DATABASE_ERROR="数据库操作失败";		// 提示信息
	private String DATABASE_SUCCESS="数据库操作成功！";	// 提示信息
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";

	private int STORAGE_LIMITED=-1;				// 存储空间不足标记
	private int STATE_RECORDING=1;				// 计时器变化标记
	private int totalTime=0;					// 总计时
	private boolean isRecording=false;			// 录音状态
	private boolean isCurrent=true;
	
	private Thread storageThread;
	@SuppressLint("HandlerLeak")
	private Handler handler=new Handler(){		// 接收计时信息，显示当前总计时   、  接收SD存储容量不足信息，并做出响应
		@Override
		public void handleMessage(Message msg){
			if(msg.what==STATE_RECORDING){
				tv_audioTime.setText(getRecordingTime());
			}
			
			
			if(msg.what==STORAGE_LIMITED){
				if(mAudioRecorder!=null){
					mAudioRecorder.stopRecord();
				}
				
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
		setContentView(R.layout.activity_audio);

		initViewsId();
		setOnClickListener();
		stopService();
		
		nm=(NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
		nm.cancel(R.layout.activity_photo);
		nm.cancel(R.layout.activity_audio);
		nm.cancel(R.layout.activity_main);
		mAudioRecorder=new AudioRecorder();
		this.tv_audioTime.setText(INIT_TIME);
		startTimer();
		startStotageDetecte();
		initDatabase();
		System.out.println("audioAc start ");
	}

	
	@Override 
	public void onPause()	{
		super.onPause();
		isCurrent=false ;
//		if(isRecording){
//			this.tb_audioRecord.setChecked(false);
//			this.mAudioRecorder.stopRecord();
//			this.tv_audioTime.setText(INIT_TIME);
//			this.insertData(currentSavedPath);
//			System.out.println("\n pause  : "+currentSavedPath+"\n");
//			isRecording=false;
//			totalTime=0;
//		}
		System.out.println("on pause ");
	}

	@Override
	public void onStart(){
		super.onStart();
		if(!isCurrent){
			isCurrent=true;
			startStotageDetecte();
		}
		 
		System.out.println("on start ");
	}
	
	/**
	 * 页面跳转或程序退出时，关闭数据库
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		this.closeDatabase();
		isCurrent=false;
	}

	/**
	 * 初始化控件ID
	 */
	private void initViewsId(){

		this.tb_audioRecord=(ToggleButton)findViewById(R.id.tb_audioRecord);
		this.tv_audioTime=(TextView)findViewById(R.id.tv_audioTime);
		this.ib_back=(ImageButton)findViewById(R.id.ib_back);
		this.ib_preview=(ImageButton)findViewById(R.id.ib_preview);
		this.ib_photo=(ImageButton)findViewById(R.id.ib_photo);
		this.ib_video=(ImageButton)findViewById(R.id.ib_video);
		
	}

	/**
	 * 绑定控件点击事件
	 */
	private void setOnClickListener(){

		this.tb_audioRecord.setOnClickListener(this);
		this.tv_audioTime.setOnClickListener(this);
		this.ib_back.setOnClickListener(this);
		this.ib_preview.setOnClickListener(this);
		this.ib_photo.setOnClickListener(this);
		this.ib_video.setOnClickListener(this);
	}

	/**
	 * 实现控件监听事件
	 */
	@Override
	public void onClick(View v){
		
		switch(v.getId()){
		case R.id.tb_audioRecord:
			
			if(!this.tb_audioRecord.isChecked())
			{
				this.mAudioRecorder.stopRecord();
				this.insertData(currentSavedPath);
				isRecording=false;
				totalTime=0;
				this.tv_audioTime.setText(INIT_TIME);
				showToast(AUDIO_SAVED);
				System.out.println("\n stop  : "+currentSavedPath+"\n");
			}
			else {
				try {
					currentSavedPath=SavedPath.getAudioSavedPath(this.getSavedDirectory());
					System.out.println("\n start  : "+currentSavedPath+"\n");
					this.mAudioRecorder.startRecord(currentSavedPath);
					isRecording=true;
				} catch (IllegalStateException e) {
					showToast(INIT_RECORDER_EXCEPTION);
					e.printStackTrace();
				} catch (IOException e) {
					showToast(IO_EXCEPTION);
					e.printStackTrace();
				}
			}
			break;
		case R.id.ib_back:
			isCurrent=false ;
			if(isRecording){
				showToast(AUDIO_SAVED);
				this.mAudioRecorder.stopRecord();
				this.insertData(currentSavedPath);
				isRecording=false;
			}
			Intent intent0=new Intent(this,MainActivity.class);
			startActivity(intent0);
			this.finish();
			break;
		case R.id.ib_preview:
			isCurrent=false ;
			if(isRecording){
				this.tb_audioRecord.setChecked(false);
				this.mAudioRecorder.stopRecord();
				this.tv_audioTime.setText(INIT_TIME);
				this.insertData(currentSavedPath);
				System.out.println("\n pause  : "+currentSavedPath+"\n");
				isRecording=false;
				totalTime=0;
				showToast(AUDIO_SAVED);
			}
			Intent intent1 = new Intent(this,RecordFilesActivity.class);
			startActivity(intent1);
			break;
		case R.id.ib_photo:
			isCurrent=false ;
			if(isRecording){
				showToast(AUDIO_SAVED);
				this.mAudioRecorder.stopRecord();
				this.insertData(currentSavedPath);
				isRecording=false;
			}
			Intent intent2=new Intent(this,PhotoActivity.class);
			startActivity(intent2);
			this.finish();
			break;
		
		case R.id.ib_video:
			isCurrent=false ;
			if(isRecording){
				showToast(AUDIO_SAVED);
				this.mAudioRecorder.stopRecord();
				this.insertData(currentSavedPath);
				isRecording=false;
			}
			Intent intent3=new Intent(this,VideoActivity.class);
			startActivity(intent3);
			this.finish();
			break;
		default:
			break;
		}
	}


	/**
	 * 开启计时器
	 */
	public void startTimer(){
		new Thread(new Runnable(){
			@Override 
			public void run(){
				while(true){
					if(tb_audioRecord.isChecked())
					{
						Message msg=new Message();
						msg.what=STATE_RECORDING;
						handler.sendMessage(msg);
						totalTime++;
					}
					try{
						Thread.sleep(1000);
					}
					catch(Exception e){
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
						System.out.println("Audio 检测SD卡 , 当前剩余空间 "+SDCardStorage.getAvailableStorage()+" M");
					}
					catch(Exception e){
						Looper.prepare();
						showToast("SD卡检测异常");
						Looper.loop();
					}
				}
			}

		});
		storageThread.start();
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
	 * 停止后台服务
	 */
	 public void stopService(){
		   Intent intent=new Intent();
		   intent.setAction("SECRET_SERVICE");
		   stopService(intent);
	   }

	/**
	 *  重写返回按钮事件，用户单击返回键时提示   退出/后台运行
	 */
	@Override
	public boolean onKeyDown(int  keyCode , KeyEvent event){

		if(keyCode==KeyEvent.KEYCODE_BACK&&event.getNumber()==0){
			//super.onBackPressed();
			new AlertDialog.Builder(AudioActivity.this)
			.setTitle("选择操作")
			.setMessage("确认退出？")
			.setPositiveButton("退出",new DialogInterface.OnClickListener() {//确定按钮
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mAudioRecorder.stopRecord();
					nm.cancel(R.layout.activity_photo);
					nm.cancel(R.layout.activity_audio);
					nm.cancel(R.layout.activity_main);
					Intent intent=new Intent(AudioActivity.this,SecretService.class);
					stopService(intent);
					AudioActivity.this.finish();
				    android.os.Process.killProcess(android.os.Process.myPid()); 
				}
			}) 
			.setNegativeButton("后台运行",new DialogInterface.OnClickListener() {//确定按钮
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mAudioRecorder.stopRecord();
					displayNotification("蜜精灵在后台运行","点击进入录音模式","",R.drawable.logo);
					Intent intent =new Intent(AudioActivity.this,SecretService.class);
					startService(intent);
					AudioActivity.this.finish();
				}
			})
			.show();
		}
		return false ;
	}

	/**
	 * 显示提示栏信息
	 * @param tickertext
	 * @param title
	 * @param content
	 * @param drawable
	 */
	private void displayNotification(String tickertext ,String title ,String content,int drawable ){
		@SuppressWarnings("deprecation")
		Notification n=new Notification(drawable ,  tickertext,System.currentTimeMillis());
		PendingIntent pi= PendingIntent.getActivity(this,0,new Intent(this,AudioActivity.class),0);
		n.setLatestEventInfo(this, title, content, pi);
		//n.vibrate=new long[]{100,50,100,50};
		
		nm.notify(R.layout.activity_audio,n);
	}
	
	/**
	 * 提示信息
	 * @param msg 信息内容
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
			path=this.getExternalCacheDir()+"";
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
					if( mAudioRecorder!=null){
						 mAudioRecorder.stopRecord();
					}
					closeDatabase();
					AudioActivity.this.finish();
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
