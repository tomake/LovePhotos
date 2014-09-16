package com.secretevidence.activity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import com.secretevidence.R;
import com.secretevidence.action.SDCardStorage;
import com.secretevidence.action.SavedPath;
import com.secretevidence.database.SQLiteManager;
import com.secretevidence.service.SecretService;

/**
 *ʵ�����չ���   <��Ҫģ��>
 * @author MAKE&BE
 *
 */

public class PhotoActivity extends Activity implements OnClickListener,Callback{

	private SQLiteManager sm;		// ���ݿ����
	private MediaPlayer mBeeper;	// ��ý�岥����
	private PowerManager pm;		// ��Դ����
	private WakeLock mWakeLock;		// ���ô���ģʽ
	private Camera mCamera;			// ���
	private SurfaceHolder mSurfaceHolder; // ����Ԥ��
	private SurfaceView sv_photo;		  // Ԥ������
	private ImageButton ib_back;		  // ���ذ�ť
	private ImageButton ib_preview;		  // �ļ�Ԥ����ť
	private ImageButton ib_takePhoto;	  // ���հ�ť
	private ImageButton ib_audio;		  // ¼��ģʽ
	private ImageButton ib_video;		  // ��Ƶģʽ
	private boolean isPreview=false;	  // �Ƿ���Ԥ��ģʽ
	private boolean isCurrent=true;
	private String MEDIA_TYPE="picture";					// ý������
	private String NO_SD_CARD_TIPS="SD�������ڣ�����޷�����ʹ�ã�";	// ��ʾ��Ϣ
	private String STORAGE_NOT_ENOUGH="�洢�ռ䲻�㣡������ռ���ԡ�";// ��ʾ��Ϣ
	private String DATABASE_ERROR="���ݿ����ʧ��";				// ��ʾ��Ϣ
	private String DATABASE_SUCCESS="���ݿ�����ɹ���";			// ��ʾ��Ϣ
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";				// ���ݿ⽨�����
	private NotificationManager nm;							// ��ʾ��������
	private int STORAGE_LIMITED=-1;				// �洢�ռ䲻����
	private Thread storageThread;

	private Handler handler=new Handler(){		// ����SD�洢����������Ϣ����������Ӧ
		@Override
		public void handleMessage(Message msg){

			if(msg.what==STORAGE_LIMITED){
				checkStorage();
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
						Thread.sleep(3000);   // 10s�����¼��洢�ռ�
						System.out.println("Photo ���SD�� , ��ǰʣ��ռ� "+SDCardStorage.getAvailableStorage()+" M");
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
	 * Activity �������
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo);

		initViewsId();
		setOnClickListener();
		stopService();
		this.mSurfaceHolder=this.sv_photo.getHolder();
		this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		this.mSurfaceHolder.addCallback(PhotoActivity.this);
		

		nm=(NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
		nm.cancel(R.layout.activity_photo);
		nm.cancel(R.layout.activity_audio);
		nm.cancel(R.layout.activity_main);
		checkSDCard();
		checkStorage();
		initDatabase();
		//startStotageDetecte();
		System.out.println("\ninit finish\n");
	}

	@Override 
	public void onPause()	{
		super.onPause();
		isCurrent=false ;
	}

	@Override
	public void onStart(){
		super.onStart();
//		if(!isCurrent){
//			isCurrent=true;
//			startStotageDetecte();
//		}
		 
	}

	/**
	 * ��ǰ��������ʱ�����йر����ݿ����ز���
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		releaseBeep();
		closeDatabase();
		isCurrent=false;
		System.out.println("\n\n PhotoActivity destroy \n\n");
	}

	/**
	 * ��ʼ���ؼ�ID
	 */
	private void  initViewsId(){
		this.sv_photo=(SurfaceView)findViewById(R.id.sv_photo);
		this.ib_back=(ImageButton)findViewById(R.id.ib_back);
		this.ib_preview=(ImageButton)findViewById(R.id.ib_preview);
		this.ib_takePhoto=(ImageButton)findViewById(R.id.ib_takePhoto);
		this.ib_audio=(ImageButton)findViewById(R.id.ib_audio);
		this.ib_video=(ImageButton)findViewById(R.id.ib_video);

	}

	/**
	 * �󶨿ؼ�����¼�
	 */
	private void setOnClickListener(){

		this.ib_back.setOnClickListener(this);
		this.ib_preview.setOnClickListener(this);
		this.ib_takePhoto.setOnClickListener(this);
		this.ib_audio.setOnClickListener(this);
		this.ib_video.setOnClickListener(this);
	}

	/**
	 * ���ÿؼ�����¼�
	 */
	public void onClick(View v){
		switch(v.getId()){
		case R.id.ib_back:
			Intent intent0=new Intent(this,MainActivity.class);
			startActivity(intent0);
			this.finish();
			break;
		case R.id.ib_preview:
			Intent intent1 = new Intent(this,RecordFilesActivity.class);
			startActivity(intent1);
			break;
		case R.id.ib_takePhoto:
			takePicture();
			break;
		case R.id.ib_audio:
			System.out.println("\naudiomode start\n");
			Intent intent2=new Intent(this,AudioActivity.class);
			startActivity(intent2);
			this.finish();
			break;
		case R.id.ib_video:
			Intent intent =new Intent(this,VideoActivity.class);
			startActivity(intent);
			this.finish();
			break;
		}
	}

	/**
	 * ����
	 */
	private void takePicture(){
		if(mCamera!=null ){
			mCamera.takePicture(shutter, row, jpegCallback);
			System.out.println("��ǰʣ��ռ�  "+SDCardStorage.getAvailableStorage()+" M");
		}
	}

	/**
	 * �ͷ������Դ
	 */
	private void resetCamera(){

		if(mCamera!=null ){
			mCamera.stopPreview();
			mCamera.release();
			mCamera=null;
			isPreview=false ;
			System.out.println("\n\n MainActiviy Camera release \n\n");
		}

	}

	/**
	 * ������ز���
	 */
	private ShutterCallback shutter=new ShutterCallback(){
		public void onShutter(){

		}
	};

	/**
	 * ������ز���
	 */
	private PictureCallback row=new PictureCallback(){
		public void onPictureTaken(byte[] data,Camera camera){

		}
	};

	/**
	 * ������ز���
	 */
	private PictureCallback jpegCallback=new PictureCallback(){
		// ����ʱ���� ��dataΪ���� 
		public void onPictureTaken(byte[] data,Camera camera){

			
			Bitmap bm=BitmapFactory.decodeByteArray(data,0, data.length);
			String path=SavedPath.getPictureSavedPath(getSavedDirectory());
			insertData(path);
			File jpg=new File(path);
			try{
				BufferedOutputStream bos=new BufferedOutputStream
						(new FileOutputStream(jpg));
				Matrix m=new Matrix();
				m.setRotate(90, bm.getWidth()/2, bm.getHeight()/2);
				Bitmap bm1=Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
				bm1.compress(Bitmap.CompressFormat.JPEG, 30, bos);
				bos.flush();
				bos.close();
				mCamera.startPreview();
			}catch(Exception e){
				showToast("jpegCallback �쳣");
				e.printStackTrace();
			}
		}
	};

	/**
	 * ��ȡͼƬ����ת�Ƕ�
	 * @param path  ͼƬ·��
	 * @return  ����ͼƬ�Ƕ�
	 */
	public int getBitmapDegree(String path){
		int degree=0;
		try{
			ExifInterface ei=new ExifInterface(path);
			int orientation=ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch(orientation){
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree=90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree=180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree=270;
				break;
			default:
				break;

			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return degree;
	}

	/**
	 * ��תͼƬ
	 * @param bm  Ҫ��ת��ͼƬ
	 * @param degree ��ת�Ƕ�
	 * @return ��ת���ͼƬ
	 */
	public  Bitmap rotateBitmap(Bitmap bm,int degree){
		Bitmap returnBm=null;
		Matrix matrix=new Matrix();
		matrix.postRotate(degree);
		try{
			returnBm=Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),bm.getHeight(),matrix,true);
		}catch(OutOfMemoryError e){
			showToast("�ڴ������");
			e.printStackTrace();
		}

		if(returnBm==null){
			return bm;
		}
		if(bm!=returnBm){
			bm.recycle();
		}
		return returnBm;
	}

	/**
	 * ����Ԥ������
	 */
	@SuppressLint("NewApi")
	@Override  
	public void surfaceCreated(SurfaceHolder holder){
		DisplayMetrics dm=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		if(!isPreview){
			mCamera=Camera.open();
		}

		
		
		
		
		Camera.Parameters p=mCamera.getParameters();
		p.setPictureFormat(PixelFormat.JPEG);   // ����ͼƬ��ʽ
		p.setJpegQuality(100);     // ����ͼƬ���� 
		p.setPreviewSize(dm.heightPixels-50, dm.widthPixels);
		p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);  // ���������ģʽ
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			showToast("setPreviewDisplay �쳣");
			e.printStackTrace();
		}
		try{
			
		
		mCamera.setParameters(p);
		}
		catch(Exception ee){
			ee.printStackTrace();
		}
		mCamera.setDisplayOrientation(90);    // ��֤ͼƬԤ������
		mCamera.startPreview();    				// ��ʼԤ��
		mCamera.autoFocus(new AutoFocusCallback(){   // �����Զ��Խ�
			@Override
			public void onAutoFocus(boolean success,Camera camera){

			}
		});
		isPreview=true;
	}

	/**
	 * Ԥ������ı�ʱ����
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder,int format , int w,int h){
		mCamera.startPreview();
	}

	/**
	 * Ԥ���������٣��ͷ������Դ
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder ){
		resetCamera();			// ֹͣԤ�������ͷ�Camera��Դ
	}



	/**
	 *  ��д���ذ�ť�¼�   �û��������ؼ�ʱ��ʾ   �˳�/��̨����
	 */
	@Override
	public boolean onKeyDown(int  keyCode , KeyEvent event){

		if(keyCode==KeyEvent.KEYCODE_BACK&&event.getNumber()==0){
			System.out.println("���ذ�ť�����\n\n");
			//super.onBackPressed();
			new AlertDialog.Builder(PhotoActivity.this)
			.setTitle("ѡ�����")
			.setMessage("ȷ���˳���")
			.setPositiveButton("�˳�",new DialogInterface.OnClickListener() {//ȷ����ť
				@Override
				public void onClick(DialogInterface dialog, int which) {
					releaseCpuAwake();
					nm.cancel(R.layout.activity_photo);
					nm.cancel(R.layout.activity_audio);
					nm.cancel(R.layout.activity_main);
					Intent intent=new Intent(PhotoActivity.this,SecretService.class);
					stopService(intent);
					PhotoActivity.this.finish();
					android.os.Process.killProcess(android.os.Process.myPid()); 
				}
			}) 
			.setNegativeButton("��̨����",new DialogInterface.OnClickListener() {//ȡ����ť
				@Override
				public void onClick(DialogInterface dialog, int which) {
					resetCamera();
					setCpuAwake();
					displayNotification("�۾����ں�̨����","�����������ģʽ","",R.drawable.logo);
					Intent intent =new Intent(PhotoActivity.this,SecretService.class);
					startService(intent);
					PhotoActivity.this.finish();
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

		nm.notify(R.layout.activity_photo,n);
	}



	/**
	 * ʵ�ֵ�����ʾ��Ϣ
	 * @param msg  ��ʾ��Ϣ
	 */
	public void showToast(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}


	/**
	 * ��������
	 */
	public  void playBeep(){
		if(mBeeper==null){
			mBeeper=MediaPlayer.create(getApplicationContext(),R.raw.ding);
			mBeeper.start();
			System.out.println("\n\n beep \n\n");
		}
		else 
			mBeeper.start();
	}

	/**
	 * �ͷŶ�ý�岥����
	 */
	public void releaseBeep(){
		if(mBeeper!=null){
			mBeeper.stop();
			mBeeper.stop();
			mBeeper=null;
		}
	}


	/**
	 * ���ùر���Ļʱ��CPU���������״̬����̨���񷽿���������
	 */
	public void setCpuAwake(){
		if(mWakeLock==null){
			pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
			mWakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cpuAwake");
			mWakeLock.acquire();
			System.out.println("\n\nMainActivity mWakeLock acquire \n\n");
		}

	}


	/**
	 * �ָ�ϵͳ����ģʽ
	 */
	public void releaseCpuAwake(){
		if(mWakeLock!=null){
			mWakeLock.release();
			System.out.println("\n\nMainActivity mWakeLock release \n\n");
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
					if(file.createNewFile()){
						//showToast("���ݿ��ļ������ɹ�");
					}
					else 
					{
						showToast("���ݿ��ļ�����ʧ��");
					}
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
	 * �����ݲ������ݿ�
	 * @param data  sql�������
	 */
	public void insertData(String data){
		if(sm!=null){
			String sql="insert into RecordFile(filePath,fileType) values('"+data+"','"+this.MEDIA_TYPE+"')";
			try{
				sm.insertData(sql);
				//showToast(this.DATABASE_SUCCESS);
				System.out.println("\n\n"+data+"\n\n");
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

	/**
	 * ���SD���Ƿ����
	 */
	public void checkSDCard(){
		if(!android.os.Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED)){
			new AlertDialog.Builder(this)
			.setTitle("����")
			.setMessage(NO_SD_CARD_TIPS)
			.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					PhotoActivity.this.finish();
					android.os.Process.killProcess(android.os.Process.myPid()); 
				}
			}).show();
		}

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
					releaseBeep();
					closeDatabase();
					PhotoActivity.this.finish();
					android.os.Process.killProcess(android.os.Process.myPid()); 

				}
			}).show();
		}
	}

	/**
	 * ��ȡ�洢Ŀ¼  
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
	 * ֹͣ��̨����
	 */
	public void stopService(){
		Intent intent=new Intent();
		intent.setAction("SECRET_SERVICE");
		stopService(intent);
	}
}
