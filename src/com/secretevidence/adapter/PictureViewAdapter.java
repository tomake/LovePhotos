package com.secretevidence.adapter;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.secretevidence.R;

@SuppressLint("NewApi")
public class PictureViewAdapter extends BaseAdapter {
	private LruCache<String , Bitmap> imageCaches ;
	private Context mContext ; 
	private ArrayList<String> sources ;
	
	
	public PictureViewAdapter(Context context , ArrayList<String> sources) {
		this.mContext = context ;
		this.sources = sources ;
		
		// ���ÿ��õ�ͼƬ�����С
		int maxCache =(int) Runtime.getRuntime().maxMemory() / 8 ;
		System.out.println("maxCache = " + maxCache / 1024 + " KB");
		this.imageCaches = new LruCache<String ,Bitmap>(maxCache){
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
			
		} ;
	}
	
	@Override
	public int getCount() {
		 
		return sources.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return sources.get(position);
	}

	@Override
	public long getItemId(int position) {
		 
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView==null){
			convertView = LayoutInflater.from(mContext).inflate(R.layout.picture_gallery, null);
		}
		final String filePath = sources.get(position) ;
		final ImageView iv_image = (ImageView)convertView.findViewById(R.id.iv_image);
		iv_image.setTag(filePath);
		System.out.println("set position tag = " + iv_image.getTag()) ;
		new AsyncTask<String, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(
					String... params) {
				
				Bitmap bmp = imageCaches.get(filePath) ;
				if(bmp == null){
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true ;
					 BitmapFactory.decodeFile(filePath, options) ;
					 options.inSampleSize = getSampleSize(options , 100,100) ;
					 options.inJustDecodeBounds = false  ;
					 bmp = BitmapFactory.decodeFile(filePath,options);
					 imageCaches.put(filePath, bmp) ;
				} 
					return bmp;
			}
			
			@Override 
			protected void onPostExecute(Bitmap bmp){
				iv_image.setImageBitmap(bmp) ;
			}
		}.execute(filePath);
		return convertView;
	}

	private int getSampleSize(Options options, int width, int height) {
		final int oHeight = options.outHeight ;
		final int oWidth = options.outWidth ;
		int sampleSize = 1 ;
		if(oHeight > height || oWidth > width ){
			final int ratioHeight = oHeight / height ;
			final int ratioWidth = oWidth / width ;
			sampleSize = ratioHeight < ratioWidth ? ratioHeight : ratioWidth  ;
		}
		
		
		return sampleSize ;
	}

	
	/**
	 * ��ȡbitmap 
	 * @param path
	 * @return
	 *//*

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


	*//**
	 * ��ͼƬת����bitmap
	 * @param path
	 * @return
	 *//*
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

	}*/
	
	
}
