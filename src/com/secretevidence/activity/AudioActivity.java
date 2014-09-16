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
 * ʵ��¼������   <��Ҫģ��>
 * @author MAKE&BE
 *
 */
public class AudioActivity extends Activity implements OnClickListener {
	
	private NotificationManager nm;
	
	private TextView tv_audioTime;				// ��ʾ¼��ʱ��
	private ImageButton ib_back;
	private ImageButton ib_preview;		
	private ToggleButton tb_audioRecord;		// ¼������
	private ImageButton ib_photo;				// ����/¼��/����  ģʽ�л� 
	private ImageButton ib_video;
	private AudioRecorder mAudioRecorder;		// ¼����
	private String INIT_TIME="00:00:00";		// ��ʼ��¼��ʱ�� 
	private String TIMER_EXCEPTION="��ʱ���쳣";	// ��ʾ��Ϣ
	private String IO_EXCEPTION="�ļ��洢�쳣";		// ��ʾ��Ϣ
	private String INIT_RECORDER_EXCEPTION="��ʼ��¼�����쳣"; // ��ʾ��Ϣ
	private String NO_SD_CARD_TIPS="SD�������ڣ�����޷�����ʹ�ã�"; // ��ʾ��Ϣ
	private String STORAGE_NOT_ENOUGH="�洢�ռ䲻�㣡������ռ���ԡ�";// ��ʾ��Ϣ
	private String AUDIO_SAVED="¼���ļ��ѱ���";		// ��ʾ��Ϣ
	private static String  currentSavedPath="pathInit";  // �ļ��洢·��
	
	private SQLiteManager sm;						// ���ݿ����
	private String MEDIA_TYPE="audio";				// ý������
	private String DATABASE_ERROR="���ݿ����ʧ��";		// ��ʾ��Ϣ
	private String DATABASE_SUCCESS="���ݿ�����ɹ���";	// ��ʾ��Ϣ
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";

	private int STORAGE_LIMITED=-1;				// �洢�ռ䲻����
	private int STATE_RECORDING=1;				// ��ʱ���仯���
	private int totalTime=0;					// �ܼ�ʱ
	private boolean isRecording=false;			// ¼��״̬
	private boolean isCurrent=true;
	
	private Thread storageThread;
	@SuppressLint("HandlerLeak")
	private Handler handler=new Handler(){		// ���ռ�ʱ��Ϣ����ʾ��ǰ�ܼ�ʱ   ��  ����SD�洢����������Ϣ����������Ӧ
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
	 * �������
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
	 * ҳ����ת������˳�ʱ���ر����ݿ�
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		this.closeDatabase();
		isCurrent=false;
	}

	/**
	 * ��ʼ���ؼ�ID
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
	 * �󶨿ؼ�����¼�
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
	 * ʵ�ֿؼ������¼�
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
	 * ������ʱ��
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
						System.out.println("Audio ���SD�� , ��ǰʣ��ռ� "+SDCardStorage.getAvailableStorage()+" M");
					}
					catch(Exception e){
						Looper.prepare();
						showToast("SD������쳣");
						Looper.loop();
					}
				}
			}

		});
		storageThread.start();
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
	 * ֹͣ��̨����
	 */
	 public void stopService(){
		   Intent intent=new Intent();
		   intent.setAction("SECRET_SERVICE");
		   stopService(intent);
	   }

	/**
	 *  ��д���ذ�ť�¼����û��������ؼ�ʱ��ʾ   �˳�/��̨����
	 */
	@Override
	public boolean onKeyDown(int  keyCode , KeyEvent event){

		if(keyCode==KeyEvent.KEYCODE_BACK&&event.getNumber()==0){
			//super.onBackPressed();
			new AlertDialog.Builder(AudioActivity.this)
			.setTitle("ѡ�����")
			.setMessage("ȷ���˳���")
			.setPositiveButton("�˳�",new DialogInterface.OnClickListener() {//ȷ����ť
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
			.setNegativeButton("��̨����",new DialogInterface.OnClickListener() {//ȷ����ť
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mAudioRecorder.stopRecord();
					displayNotification("�۾����ں�̨����","�������¼��ģʽ","",R.drawable.logo);
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
	 * ��ʾ��ʾ����Ϣ
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
	 * ��ʾ��Ϣ
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
			path=this.getExternalCacheDir()+"";
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
