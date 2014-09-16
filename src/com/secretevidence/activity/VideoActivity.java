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
 * ʵ����Ƶ¼�ƹ���   <��Ҫģ��>
 * @author MAKE&BE
 *
 */
public class VideoActivity extends Activity implements OnClickListener,SurfaceHolder.Callback{

	private PowerManager.WakeLock mWakeLock;
	private MediaRecorder mRecorder;		// �����
	private Camera mCamera;					// ����ͷ
	private SurfaceHolder holder;			// Ԥ�����
	private ImageButton ib_back;
	private ToggleButton tb_videoRecord;    // ¼�ƿ���
	private ImageButton ib_preview;  
	private ImageButton ib_audio;
	private ImageButton ib_photo;			// ����/¼��/���� ģʽ�л�
	private SurfaceView sv_video;			// ¼��Ԥ��
	private TextView tv_videoTime;			// ��ʾ¼��ʱ��
	private boolean isRecording =false;     // ¼��״̬ 
	private String INIT_TIME="00:00:00";	// ��ʼ����ʱ��
	private String TIMER_EXCEPTION="��ʱ���쳣"; // ��ʾ��Ϣ
	private String IO_EXCEPTION="�ļ��洢�쳣";	// ��ʾ��Ϣ
	private String RECORDER_INIT_EXCEPTION="�������ʼ���쳣";  // ��ʾ��Ϣ
	private String MEDIA_SAVED="��Ƶ�ѱ���"; 	// ��ʾ��Ϣ
	private String NO_SD_CARD_TIPS="SD�������ڣ�����޷�����ʹ�ã�"; // ��ʾ��Ϣ
	private String STORAGE_NOT_ENOUGH="�洢�ռ䲻�㣡������ռ���ԡ�";// ��ʾ��Ϣ
	private final int STATE_RECORDING=1;    // ���¼��״̬
	private int STORAGE_LIMITED=-1;				// �洢�ռ䲻����
	private boolean isCurrent=true;

	private SQLiteManager sm;
	private String MEDIA_TYPE="video";
	private String DATABASE_ERROR="���ݿ����ʧ��";
	private String DATABASE_SUCCESS="���ݿ�����ɹ���";
	private static String  currentSavedPath="pathInit";
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";

	private int totalTime=0;                // ¼����ʱ��
	@SuppressLint("HandlerLeak")
	private Handler handler=new Handler(){  // ���ռ�ʱ��Ϣ����ʾ��ǰ�ܼ�ʱ ������SD�洢����������Ϣ����������Ӧ
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
	 * �������
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
	 * ��������ʱ�ͷ������Դ
	 */
	@Override
	public  void onDestroy(){
		super.onDestroy();
		setScreenAwakeRelease();
		closeDatabase();
		stopRecord();
	}

	/**
	 * ��дonPause����
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
	 * ��дonResume����
	 */
	@Override 
	public void onResume(){
		super.onResume();
		setScreenAwake();
	}

	/**
	 * ��ʼ���ؼ�ID
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
	 * �󶨿ؼ�����¼�
	 */
	private void setOnClickListener(){
		this.ib_back.setOnClickListener(this);
		this.ib_preview.setOnClickListener(this);
		this.tb_videoRecord.setOnClickListener(this);
		this.ib_photo.setOnClickListener(this);
		this.ib_audio.setOnClickListener(this);

	}

	/**
	 * ʵ�ֿؼ������¼�
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
	 * ��ʱ������
	 */
	public void startTimer(){
		// ����ʱ���߳�
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
	 * ʵʱ���SD��������С��10Mʱ��ʾ�ڴ治�㲢�˳�
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
						Thread.sleep(10000);   // 10s�����¼��洢�ռ�
						System.out.println("Video ���SD�� , ��ǰʣ��ռ� "+SDCardStorage.getAvailableStorage()+" M");
					}
					catch(Exception e){
						showToast("SD������쳣");
					}
				}
			}

		}).start();
	}
	
	
	/**
	 * ��ʼ�������
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
	 * ��ʼ¼��
	 */
	public void startRecord(){
		if(mCamera!=null&&mRecorder!=null){
			mRecorder.start();
			System.out.println("\n start "+this.currentSavedPath+"\n");
		}
	}


	/**
	 * ֹͣ¼�ƣ����ͷ�¼�������Դ
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
	 * ��дsurfaceCreated����
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
	 * ��дsurfaceChanged����
	 */
	public void surfaceChanged(SurfaceHolder holder,int format,int w,int h){

		
	}

	/**
	 * ��дsurfaceDestroyed����
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder){
		//stopRecord();
	}

	/**
	 * ��ʽ��¼��ʱ��
	 * @return  ����¼��ʱ��
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
	 *  ��д���ذ�ť�¼� �� �û��������ؼ�ʱ��ʾ   �˳�/��̨����
	 */
	@Override
	public boolean onKeyDown(int  keyCode , KeyEvent event){

		if(keyCode==KeyEvent.KEYCODE_BACK&&event.getNumber()==0){
			//super.onBackPressed();
			new AlertDialog.Builder(VideoActivity.this)
			.setTitle("ѡ�����")
			.setMessage("ȷ���˳���")
			.setPositiveButton("�˳�",new DialogInterface.OnClickListener() {//ȷ����ť
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
			.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {//ȷ����ť
				@Override
				public void onClick(DialogInterface dialog, int which) {
					 
				}
			})
			.show();
		}
		return false ;
	}
	
	/**
	 * ������Ļ����
	 */
	public void setScreenAwake(){
		if(mWakeLock==null){
			PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
			mWakeLock=pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "awake");
			mWakeLock.acquire();
		}
	}

	/**
	 * �ָ���Ļ����ģʽ
	 */
	public void setScreenAwakeRelease(){
		if(mWakeLock!=null){
			mWakeLock.release();
			mWakeLock=null;
		}
	}

	/**
	 * ���ڵ���������ʾ��Ϣ
	 * @param msg ��Ϣ����
	 */
	public void showToast(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}
	
	
	/**
	 * ��ȡ�ļ��洢��Ŀ¼
	 * @return  ���ش洢·��
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
	 * ���SD��ʣ������  С��10Mʱ��ʾ���˳�
	 */
	public void checkStorage(){
		if(SDCardStorage.getAvailableStorage()<10){
			new AlertDialog.Builder(this)
			.setTitle("����")
			.setMessage(STORAGE_NOT_ENOUGH)
			.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
				
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
						file.createNewFile() ;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					showToast("�쳣�����ݿ��ļ�����ʧ��");
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
			}
			catch (Exception e){
				e.printStackTrace();	
				showToast(this.DATABASE_ERROR);
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
