package com.secretevidence.activity;

/**
 * 预览照片   <主要模块>
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.secretevidence.R;

public class PhotoViewActivity extends Activity implements OnTouchListener ,
android.view.GestureDetector.OnGestureListener{

	private String CURRENT_TAG="current";	// Intent 中的键值对 标记  用于获取数据
	private String FILE_TAG="files";		// Intent 中的键值对 标记  用于获取数据
	private String INDEX_TAG="index";		// Intent 中的键值对 标记  用于获取数据
	private  String currentFilePath;		// 当前文件路径
	private ArrayList<String> filesPath;	// 存储文件路径
	private static int currentIndex=-1;		// 当前预览文件在ArrayList中的下标
	private GestureDetector gestureDetector ;  // 手势管理
	private  ImageView iv_photo;			// 用于显示文件
	DisplayMetrics dm;
	
	private LruCache<String , Bitmap> imageCache ;
	/**
	 * 程序入口
	 */
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo_view);
		
		int  cacheSize =(int) Runtime.getRuntime().maxMemory() / 8 ;
		imageCache = new LruCache<String , Bitmap>(cacheSize){
			@Override 
			public int  sizeOf(String key ,Bitmap bmp){
				return  bmp.getByteCount() ;
			}
		};
		
		
		dm=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		currentIndex=getIntent().getExtras().getInt(INDEX_TAG);
		currentFilePath=getIntent().getExtras().getString(CURRENT_TAG);
		filesPath=getIntent().getExtras().getStringArrayList(FILE_TAG);		
		iv_photo=(ImageView)findViewById(R.id.iv_photo);
		
		// iv_photo.setImageBitmap(getLocalBitmap(currentFilePath));
		iv_photo.setImageBitmap(getBitmap(currentFilePath));
		
		iv_photo.setOnTouchListener(this);
		iv_photo.setLongClickable(true);
		gestureDetector=new GestureDetector(this);
		gestureDetector.setIsLongpressEnabled(true);
		
		System.out.println("position tag : index = "  + currentIndex);




	}
	
	public Bitmap getBitmap(String path){
		Bitmap bmp = imageCache.get(path);
		if(bmp == null){
			bmp = BitmapFactory.decodeFile(path);
			imageCache.put(path, bmp) ;
		}
		return bmp ;
	}

	/**
	 * 获取bitmap 
	 * @param path
	 * @return
	 */

	private Bitmap compressImage(Bitmap image) {  

		ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中  
		int options = 100;  
		while ( baos.toByteArray().length / 1024>100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩         
			baos.reset();//重置baos即清空baos  
			image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中  
			options -= 10;//每次都减少10  
		}  
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中  
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片  
		return bitmap;  
	}  


	/**
	 * 将图片转换成bitmap
	 * @param path
	 * @return
	 */
	public Bitmap getLocalBitmap(String path){
		BitmapFactory.Options newOpts = new BitmapFactory.Options();  
		//开始读入图片，此时把options.inJustDecodeBounds 设回true了  
		newOpts.inJustDecodeBounds = true;  
		Bitmap bitmap = BitmapFactory.decodeFile(path,newOpts);//此时返回bm为空  
		newOpts.inJustDecodeBounds = false;  
		int w = newOpts.outWidth;  
		int h = newOpts.outHeight;  
		float hh =  dm.heightPixels;	 
		float ww = dm.widthPixels;		  
		//缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可  
		int be = 1;//be=1表示不缩放  
		if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放  
			be = (int) (newOpts.outWidth / ww);  
		} else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放  
			be = (int) (newOpts.outHeight / hh);  
		}  
		if (be <= 0)  
			be = 1;  
		newOpts.inSampleSize = be;//设置缩放比例  
		//重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了  
		bitmap = BitmapFactory.decodeFile(path, newOpts);  

		return compressImage(bitmap);//压缩好比例大小后再进行质量压缩  

	}
	 
	
	/**
	 * 显示下一张图片
	 */
	public void showNext(){
		if(currentIndex==filesPath.size()-1){
			Toast.makeText(getApplicationContext(), "已经是最后一张图片了！", Toast.LENGTH_SHORT).show();
			return ;
		}

		// iv_photo.setImageBitmap(getLocalBitmap(filesPath.get(++currentIndex)));
		iv_photo.setImageBitmap(getBitmap(filesPath.get(++currentIndex)));
	}

	/**
	 * 显示上一张图片
	 */
	public void showPrevious(){
		if(currentIndex==0){
			Toast.makeText(getApplicationContext(), "已经是第一张图片了！", Toast.LENGTH_SHORT).show();
			return ;
		}

		// iv_photo.setImageBitmap(getLocalBitmap(filesPath.get(--currentIndex)));
		iv_photo.setImageBitmap(getBitmap(filesPath.get(--currentIndex)));

	}

	/**
	 * 重写事件
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 重写事件
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub

		if(velocityX<0){
			showNext();
		}
		else if(velocityX>0){
			showPrevious();
		}
		return false;
	}

	/**
	 * 重写事件
	 */
	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub


		new AlertDialog.Builder(PhotoViewActivity.this)
		.setTitle("提示")
		.setMessage("删除文件？")
		.setPositiveButton("确认",  new  DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				deleteMediaFile(currentIndex);
			}

		})
		.setNegativeButton("取消", new  DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}

		})
		.show();

	}


	public void deleteMediaFile(int index){
		File f=new File(filesPath.get(index));
		if(f.exists()){
			f.delete();
			System.out.println("\n 文件 "+filesPath.get(index)+"  已删除  ？" + !f.exists());
			filesPath.remove(index);
			if(filesPath.size()==0)
				this.finish();
			else {
				if(index>=filesPath.size()-1){
					currentIndex=filesPath.size()-1;
				}
				else {
					currentIndex=index;
				}
				// this.iv_photo.setImageBitmap(this.getLocalBitmap(filesPath.get(currentIndex)));
				this.iv_photo.setImageBitmap(this.getBitmap(filesPath.get(currentIndex)));
			}
		}
	}

	/**
	 * 重写事件
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 重写事件
	 */
	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	/**
	 * 重写事件
	 */
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 重写事件
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		gestureDetector.onTouchEvent(event);
		return true;
	}



}
