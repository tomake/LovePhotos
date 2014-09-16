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
 *实现拍照功能   <主要模块>
 * @author MAKE&BE
 *
 */

public class PhotoActivity extends Activity implements OnClickListener,Callback{

	private SQLiteManager sm;		// 数据库管理
	private MediaPlayer mBeeper;	// 多媒体播放器
	private PowerManager pm;		// 电源管理
	private WakeLock mWakeLock;		// 设置待机模式
	private Camera mCamera;			// 相机
	private SurfaceHolder mSurfaceHolder; // 拍照预览
	private SurfaceView sv_photo;		  // 预览界面
	private ImageButton ib_back;		  // 返回按钮
	private ImageButton ib_preview;		  // 文件预览按钮
	private ImageButton ib_takePhoto;	  // 拍照按钮
	private ImageButton ib_audio;		  // 录音模式
	private ImageButton ib_video;		  // 视频模式
	private boolean isPreview=false;	  // 是否处于预览模式
	private boolean isCurrent=true;
	private String MEDIA_TYPE="picture";					// 媒体类型
	private String NO_SD_CARD_TIPS="SD卡不存在，软件无法正常使用！";	// 提示信息
	private String STORAGE_NOT_ENOUGH="存储空间不足！请清理空间后尝试。";// 提示信息
	private String DATABASE_ERROR="数据库操作失败";				// 提示信息
	private String DATABASE_SUCCESS="数据库操作成功！";			// 提示信息
	private String sql="create table if not exists RecordFile ( id integer primary key autoincrement ,"
			+"filePath text , fileType text )";				// 数据库建表语句
	private NotificationManager nm;							// 提示栏管理器
	private int STORAGE_LIMITED=-1;				// 存储空间不足标记
	private Thread storageThread;

	private Handler handler=new Handler(){		// 接收SD存储容量不足信息，并做出响应
		@Override
		public void handleMessage(Message msg){

			if(msg.what==STORAGE_LIMITED){
				checkStorage();
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
						Thread.sleep(3000);   // 10s后重新检测存储空间
						System.out.println("Photo 检测SD卡 , 当前剩余空间 "+SDCardStorage.getAvailableStorage()+" M");
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
	 * Activity 程序入口
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
	 * 当前界面销毁时，进行关闭数据库等相关操作
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
	 * 初始化控件ID
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
	 * 绑定控件点击事件
	 */
	private void setOnClickListener(){

		this.ib_back.setOnClickListener(this);
		this.ib_preview.setOnClickListener(this);
		this.ib_takePhoto.setOnClickListener(this);
		this.ib_audio.setOnClickListener(this);
		this.ib_video.setOnClickListener(this);
	}

	/**
	 * 设置控件点击事件
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
	 * 拍照
	 */
	private void takePicture(){
		if(mCamera!=null ){
			mCamera.takePicture(shutter, row, jpegCallback);
			System.out.println("当前剩余空间  "+SDCardStorage.getAvailableStorage()+" M");
		}
	}

	/**
	 * 释放相机资源
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
	 * 拍照相关参数
	 */
	private ShutterCallback shutter=new ShutterCallback(){
		public void onShutter(){

		}
	};

	/**
	 * 拍照相关参数
	 */
	private PictureCallback row=new PictureCallback(){
		public void onPictureTaken(byte[] data,Camera camera){

		}
	};

	/**
	 * 拍照相关参数
	 */
	private PictureCallback jpegCallback=new PictureCallback(){
		// 拍照时调用 ，data为数据 
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
				showToast("jpegCallback 异常");
				e.printStackTrace();
			}
		}
	};

	/**
	 * 获取图片的旋转角度
	 * @param path  图片路径
	 * @return  返回图片角度
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
	 * 旋转图片
	 * @param bm  要旋转的图片
	 * @param degree 旋转角度
	 * @return 旋转后的图片
	 */
	public  Bitmap rotateBitmap(Bitmap bm,int degree){
		Bitmap returnBm=null;
		Matrix matrix=new Matrix();
		matrix.postRotate(degree);
		try{
			returnBm=Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),bm.getHeight(),matrix,true);
		}catch(OutOfMemoryError e){
			showToast("内存溢出！");
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
	 * 拍照预览界面
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
		p.setPictureFormat(PixelFormat.JPEG);   // 设置图片格式
		p.setJpegQuality(100);     // 设置图片质量 
		p.setPreviewSize(dm.heightPixels-50, dm.widthPixels);
		p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);  // 设置闪光灯模式
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			showToast("setPreviewDisplay 异常");
			e.printStackTrace();
		}
		try{
			
		
		mCamera.setParameters(p);
		}
		catch(Exception ee){
			ee.printStackTrace();
		}
		mCamera.setDisplayOrientation(90);    // 保证图片预览正常
		mCamera.startPreview();    				// 开始预览
		mCamera.autoFocus(new AutoFocusCallback(){   // 设置自动对焦
			@Override
			public void onAutoFocus(boolean success,Camera camera){

			}
		});
		isPreview=true;
	}

	/**
	 * 预览界面改变时触发
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder,int format , int w,int h){
		mCamera.startPreview();
	}

	/**
	 * 预览界面销毁，释放相机资源
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder ){
		resetCamera();			// 停止预览，并释放Camera资源
	}



	/**
	 *  重写返回按钮事件   用户单击返回键时提示   退出/后台运行
	 */
	@Override
	public boolean onKeyDown(int  keyCode , KeyEvent event){

		if(keyCode==KeyEvent.KEYCODE_BACK&&event.getNumber()==0){
			System.out.println("返回按钮被点击\n\n");
			//super.onBackPressed();
			new AlertDialog.Builder(PhotoActivity.this)
			.setTitle("选择操作")
			.setMessage("确认退出？")
			.setPositiveButton("退出",new DialogInterface.OnClickListener() {//确定按钮
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
			.setNegativeButton("后台运行",new DialogInterface.OnClickListener() {//取消按钮
				@Override
				public void onClick(DialogInterface dialog, int which) {
					resetCamera();
					setCpuAwake();
					displayNotification("蜜精灵在后台运行","点击进入拍照模式","",R.drawable.logo);
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
	 * 提示栏提示信息
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
	 * 实现弹出提示信息
	 * @param msg  提示信息
	 */
	public void showToast(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}


	/**
	 * 播放声音
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
	 * 释放多媒体播放器
	 */
	public void releaseBeep(){
		if(mBeeper!=null){
			mBeeper.stop();
			mBeeper.stop();
			mBeeper=null;
		}
	}


	/**
	 * 设置关闭屏幕时，CPU不进入待机状态，后台服务方可正常运行
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
	 * 恢复系统待机模式
	 */
	public void releaseCpuAwake(){
		if(mWakeLock!=null){
			mWakeLock.release();
			System.out.println("\n\nMainActivity mWakeLock release \n\n");
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
					if(file.createNewFile()){
						//showToast("数据库文件创建成功");
					}
					else 
					{
						showToast("数据库文件创建失败");
					}
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
	 * 将数据插入数据库
	 * @param data  sql插入语句
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
	 * 关闭数据库
	 */
	public void closeDatabase(){
		if(sm!=null){
			sm.closeDatabase();
		}
	}

	/**
	 * 检测SD卡是否存在
	 */
	public void checkSDCard(){
		if(!android.os.Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED)){
			new AlertDialog.Builder(this)
			.setTitle("敬告")
			.setMessage(NO_SD_CARD_TIPS)
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {

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
					releaseBeep();
					closeDatabase();
					PhotoActivity.this.finish();
					android.os.Process.killProcess(android.os.Process.myPid()); 

				}
			}).show();
		}
	}

	/**
	 * 获取存储目录  
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
	 * 停止后台服务
	 */
	public void stopService(){
		Intent intent=new Intent();
		intent.setAction("SECRET_SERVICE");
		stopService(intent);
	}
}
