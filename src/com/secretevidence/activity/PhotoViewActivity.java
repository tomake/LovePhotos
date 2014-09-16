package com.secretevidence.activity;

/**
 * Ԥ����Ƭ   <��Ҫģ��>
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

	private String CURRENT_TAG="current";	// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String FILE_TAG="files";		// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String INDEX_TAG="index";		// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private  String currentFilePath;		// ��ǰ�ļ�·��
	private ArrayList<String> filesPath;	// �洢�ļ�·��
	private static int currentIndex=-1;		// ��ǰԤ���ļ���ArrayList�е��±�
	private GestureDetector gestureDetector ;  // ���ƹ���
	private  ImageView iv_photo;			// ������ʾ�ļ�
	DisplayMetrics dm;
	
	private LruCache<String , Bitmap> imageCache ;
	/**
	 * �������
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
	 * ��ȡbitmap 
	 * @param path
	 * @return
	 */

	private Bitmap compressImage(Bitmap image) {  

		ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//����ѹ������������100��ʾ��ѹ������ѹ��������ݴ�ŵ�baos��  
		int options = 100;  
		while ( baos.toByteArray().length / 1024>100) {  //ѭ���ж����ѹ����ͼƬ�Ƿ����100kb,���ڼ���ѹ��         
			baos.reset();//����baos�����baos  
			image.compress(Bitmap.CompressFormat.JPEG, options, baos);//����ѹ��options%����ѹ��������ݴ�ŵ�baos��  
			options -= 10;//ÿ�ζ�����10  
		}  
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//��ѹ���������baos��ŵ�ByteArrayInputStream��  
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//��ByteArrayInputStream��������ͼƬ  
		return bitmap;  
	}  


	/**
	 * ��ͼƬת����bitmap
	 * @param path
	 * @return
	 */
	public Bitmap getLocalBitmap(String path){
		BitmapFactory.Options newOpts = new BitmapFactory.Options();  
		//��ʼ����ͼƬ����ʱ��options.inJustDecodeBounds ���true��  
		newOpts.inJustDecodeBounds = true;  
		Bitmap bitmap = BitmapFactory.decodeFile(path,newOpts);//��ʱ����bmΪ��  
		newOpts.inJustDecodeBounds = false;  
		int w = newOpts.outWidth;  
		int h = newOpts.outHeight;  
		float hh =  dm.heightPixels;	 
		float ww = dm.widthPixels;		  
		//���űȡ������ǹ̶��������ţ�ֻ�ø߻��߿�����һ�����ݽ��м��㼴��  
		int be = 1;//be=1��ʾ������  
		if (w > h && w > ww) {//�����ȴ�Ļ����ݿ�ȹ̶���С����  
			be = (int) (newOpts.outWidth / ww);  
		} else if (w < h && h > hh) {//����߶ȸߵĻ����ݿ�ȹ̶���С����  
			be = (int) (newOpts.outHeight / hh);  
		}  
		if (be <= 0)  
			be = 1;  
		newOpts.inSampleSize = be;//�������ű���  
		//���¶���ͼƬ��ע���ʱ�Ѿ���options.inJustDecodeBounds ���false��  
		bitmap = BitmapFactory.decodeFile(path, newOpts);  

		return compressImage(bitmap);//ѹ���ñ�����С���ٽ�������ѹ��  

	}
	 
	
	/**
	 * ��ʾ��һ��ͼƬ
	 */
	public void showNext(){
		if(currentIndex==filesPath.size()-1){
			Toast.makeText(getApplicationContext(), "�Ѿ������һ��ͼƬ�ˣ�", Toast.LENGTH_SHORT).show();
			return ;
		}

		// iv_photo.setImageBitmap(getLocalBitmap(filesPath.get(++currentIndex)));
		iv_photo.setImageBitmap(getBitmap(filesPath.get(++currentIndex)));
	}

	/**
	 * ��ʾ��һ��ͼƬ
	 */
	public void showPrevious(){
		if(currentIndex==0){
			Toast.makeText(getApplicationContext(), "�Ѿ��ǵ�һ��ͼƬ�ˣ�", Toast.LENGTH_SHORT).show();
			return ;
		}

		// iv_photo.setImageBitmap(getLocalBitmap(filesPath.get(--currentIndex)));
		iv_photo.setImageBitmap(getBitmap(filesPath.get(--currentIndex)));

	}

	/**
	 * ��д�¼�
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * ��д�¼�
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
	 * ��д�¼�
	 */
	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub


		new AlertDialog.Builder(PhotoViewActivity.this)
		.setTitle("��ʾ")
		.setMessage("ɾ���ļ���")
		.setPositiveButton("ȷ��",  new  DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				deleteMediaFile(currentIndex);
			}

		})
		.setNegativeButton("ȡ��", new  DialogInterface.OnClickListener(){

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
			System.out.println("\n �ļ� "+filesPath.get(index)+"  ��ɾ��  ��" + !f.exists());
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
	 * ��д�¼�
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * ��д�¼�
	 */
	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	/**
	 * ��д�¼�
	 */
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * ��д�¼�
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		gestureDetector.onTouchEvent(event);
		return true;
	}



}
