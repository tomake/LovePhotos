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
		
		// 设置可用的图片缓存大小
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
	 * 获取bitmap 
	 * @param path
	 * @return
	 *//*

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


	*//**
	 * 将图片转换成bitmap
	 * @param path
	 * @return
	 *//*
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

	}*/
	
	
}
