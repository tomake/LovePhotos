package com.secretevidence.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.secretevidence.R;
import com.secretevidence.service.SecretService;

public class MainActivity extends Activity implements OnClickListener{

	private NotificationManager nm;
	private ImageView  iv_mainAudio;
	private ImageView  iv_mainPic;
	private ImageView  iv_mainVideo;
	private ImageView  iv_mainLib;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		nm=(NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
		nm.cancel(R.layout.activity_photo);
		nm.cancel(R.layout.activity_audio);
		nm.cancel(R.layout.activity_main);
		initViewsId();
		setViewsListener();
		
	}

	public void initViewsId(){

		this.iv_mainAudio=(ImageView)findViewById(R.id.iv_mainAudio);
		this.iv_mainLib=(ImageView)findViewById(R.id.iv_mainLib);
		this.iv_mainPic=(ImageView)findViewById(R.id.iv_mainPic);
		this.iv_mainVideo=(ImageView)findViewById(R.id.iv_mainVideo);
	};
	public void setViewsListener(){
		this.iv_mainAudio.setOnClickListener(this);
		this.iv_mainLib.setOnClickListener(this);
		this.iv_mainPic.setOnClickListener(this);
		this.iv_mainVideo.setOnClickListener(this);
	}

	@Override
	public void onClick(View v){
		Intent intent=new Intent();
		switch(v.getId()){
		case R.id.iv_mainAudio:
			intent.setClass(this, AudioActivity.class);
			startActivity(intent);
			this.finish();
			break;
		case R.id.iv_mainLib:
			intent.setClass(this, RecordFilesActivity.class);
			startActivity(intent);
			break;
		case R.id.iv_mainPic:
			intent.setClass(this, PhotoActivity.class);
			startActivity(intent);
			this.finish();
			break;
		case R.id.iv_mainVideo:
			intent.setClass(this, VideoActivity.class);
			startActivity(intent);
			this.finish();
			break;
		default:break;
		}
	}

	/**
	 *  ��д���ذ�ť�¼�   �û��������ؼ�ʱ��ʾ   �˳�/��̨����
	 */
	@Override
	public boolean onKeyDown(int  keyCode , KeyEvent event){

		if(keyCode==KeyEvent.KEYCODE_BACK&&event.getNumber()==0){
			System.out.println("���ذ�ť�����\n\n");
			//super.onBackPressed();
			new AlertDialog.Builder(MainActivity.this)
			.setTitle("ѡ�����")
			.setMessage("ȷ���˳���")
			.setPositiveButton("�˳�",new DialogInterface.OnClickListener() {//ȷ����ť
				@Override
				public void onClick(DialogInterface dialog, int which) {
					nm.cancel(R.layout.activity_photo);
					nm.cancel(R.layout.activity_audio);
					nm.cancel(R.layout.activity_main);
					Intent intent=new Intent(MainActivity.this,SecretService.class);
					stopService(intent);
					MainActivity.this.finish();
					android.os.Process.killProcess(android.os.Process.myPid()); 
				}
			}) 
			.setNegativeButton("��̨����",new DialogInterface.OnClickListener() {//ȡ����ť
				@Override
				public void onClick(DialogInterface dialog, int which) {
					displayNotification("�۾����ں�̨����","�������������","",R.drawable.logo);
					Intent intent =new Intent(MainActivity.this,SecretService.class);
					startService(intent);
					MainActivity.this.finish();
				}
			})
			.show();
		}
		return false ;
	}


	/**
	 * ��ʾ����ʾ��Ϣ
	 * @param tickertext  
	 * @param title   
	 * @param content
	 * @param drawable
	 */
	private void displayNotification(String tickertext ,String title ,String content,int drawable ){
		@SuppressWarnings("deprecation")
		Notification n=new Notification(drawable ,  tickertext,System.currentTimeMillis());
		PendingIntent pi= PendingIntent.getActivity(this,0,new Intent(this,PhotoActivity.class),0);
		n.setLatestEventInfo(this, title, content, pi);
		//n.vibrate=new long[]{100,50,100,50};

		nm.notify(R.layout.activity_main,n);
	}

	
	
 

}
