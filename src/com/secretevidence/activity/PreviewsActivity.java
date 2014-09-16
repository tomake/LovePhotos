package com.secretevidence.activity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.secretevidence.R;
import com.secretevidence.action.AudioPlayer;

/**
 * 实现文件预览功能   <主要模块>
 * @author MAKE&BE
 *
 */
public class PreviewsActivity extends Activity implements OnClickListener{

	private AudioPlayer ap;				// 音乐播放器
	private ListView listview;			// 用于显示文件列表
	private View view_line;				// 分隔条
	private View v_divide;				// 分隔条
	private ImageView iv_titleImg;
	private ArrayList<String> totalFile= new ArrayList<String>();  		// 存储文件路径
	private static String AUDIO_DIRECTORY="/SecretEvidence/Audio/"; 	// 录音文件存储目录
	private static String VIDEO_DIRECTORY="/SecretEvidence/Video/";		// 视频文件存储目录
	private static String PICTURE_DIRECTORY="/SecretEvidence/Picture/"; // 相册文件存储目录
	private String CURRENT_TAG="current";	// Intent 中的键值对 标记  用于获取数据
	private String FILE_TAG="files";		// Intent 中的键值对 标记  用于获取数据
	private String  INDEX_TAG="index";		// Intent 中的键值对 标记  用于获取数据
	private String EXTRA_KEY="showType";	// Intent 中的键值对 标记  用于获取数据
	private String SHOW_PICTURE="picture";	// 预览模式：照片
	private String SHOW_AUDIO="Audio";		// 预览模式：录音
	private String SHOW_VIDEO="Video";		// 预览模式：视频
	private String viewType;			 	// 标记预览类型
	private int DEVICE_WIDTH;				// 相册预览中图片宽度
	private int DEVICE_HEIGHT;				// 相册预览中图片高度
	
	private MyAdapter adapter;

	/***
	 * 程序入口
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_record_files);

		getDeviceSize();
		viewType=getIntent().getExtras().getString(EXTRA_KEY);
		initViewsId();
		setTitle();
		getFiles();
		setListViewDivider();
		adapter=new MyAdapter(this);
		this.listview.setBackgroundColor(getResources().getColor(R.color.blue));
		this.listview.setAdapter(adapter);
		this.listview.setOnItemClickListener(new ListItemListener());
		this.listview.setOnItemLongClickListener(new ItemLongClickListener());
	}
	
	
	@Override 
	public void onPause(){
		super.onPause();
		
		stopAudio();
		
	}

	@Override  
	public  void onResume(){
		super.onResume();
		 
	}
	
	/**
	 * 初始化控件ID
	 */
	public void initViewsId(){
		this.listview=(ListView)findViewById(R.id.listview);
		this.view_line=(View)findViewById(R.id.view_line);
		this.v_divide=(View)findViewById(R.id.v_divide);
		this.iv_titleImg=(ImageView)findViewById(R.id.iv_titleImg);
	}

	/**
	 * 实现控件点击事件
	 */
	@Override
	public void onClick(View v){
		switch(v.getId()){
		case R.id.iv_1:
		case R.id.iv_2:
		case R.id.iv_3:
		case R.id.iv_4:
			recycleMemory();
			Intent intent =new Intent(PreviewsActivity.this,PhotoViewActivity.class);
			intent.putExtra(CURRENT_TAG, totalFile.get((Integer) v.getTag()));
			intent.putExtra(INDEX_TAG, (Integer)v.getTag());
			intent.putStringArrayListExtra(FILE_TAG, totalFile);
			startActivity(intent);
			this.finish();
			break;
		default :break;
		}
	}

	@Override 
	public void onDestroy(){
		super.onDestroy();
		stopAudio();
	}


	/**
	 * 设置当前预览标题
	 */
	public void setTitle(){
		iv_titleImg.setVisibility(View.VISIBLE);
		if(this.viewType.equals(SHOW_AUDIO)){
			iv_titleImg.setImageResource(R.drawable.audio_lib);
		}
		else if(this.viewType.equals(SHOW_PICTURE)){
			iv_titleImg.setImageResource(R.drawable.pic_lib);
			v_divide.setVisibility(View.VISIBLE);
		}
		else if(this.viewType.equals(SHOW_VIDEO)){
			iv_titleImg.setImageResource(R.drawable.video_lib);
		}
	}

	/**
	 * 获取预览文件路径
	 */
	public  void getFiles(){
		String path=this.getSavedDirectory();
		if(this.viewType.equals(SHOW_AUDIO))
			path+=AUDIO_DIRECTORY;
		else if(this.viewType.equals(SHOW_PICTURE))
			path+=PICTURE_DIRECTORY;
		else if(this.viewType.equals(SHOW_VIDEO))
			path+=VIDEO_DIRECTORY;

		File files=new File(path);
		if(files.exists()){
			File[] file=files.listFiles();
			for(File f:file){
				totalFile.add(f.getAbsolutePath());
				System.out.println(f.getAbsolutePath()+"\n");
			}
		}
		else {
			System.out.println("\nfile  not exist  \n"+path);
		}

	}

	/**
	 * 设置列表间隔
	 */
	public void setListViewDivider(){
		if(this.viewType.equals(SHOW_PICTURE)){
			this.listview.setDivider(null);
			this.listview.setDividerHeight(15);
			this.view_line.setVisibility(View.GONE);
			this.listview.setHeaderDividersEnabled(true);
		}

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
	 * 获取手机设备尺寸
	 */
	public void getDeviceSize(){
		DisplayMetrics dm=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		DEVICE_WIDTH=dm.widthPixels;
		DEVICE_HEIGHT=dm.heightPixels;
	}

	
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
	        //开始读入图片，此时把options.inJustDecodeBounds 设回true了  ,表示不为bitmap分配内存
	        newOpts.inJustDecodeBounds = true;  
	        Bitmap bitmap = BitmapFactory.decodeFile(path,newOpts);//此时返回bm为空  
	          
	        newOpts.inJustDecodeBounds = false;  
	        int w = newOpts.outWidth;  
	        int h = newOpts.outHeight;  
	        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为  
	        float hh = 800f;//这里设置高度为800f  
	        float ww = 800f;//这里设置宽度为480f  
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

	ArrayList<ImageView> viewList=new ArrayList<ImageView>();
	ArrayList<Bitmap> bitmapList=new ArrayList<Bitmap>();
	
	
	private void recycleMemory(){
		for(int i=0;i<bitmapList.size();i++){
			
			Bitmap bm= bitmapList.get(i);
			if(!bm.isRecycled()){
				bm.recycle();
				System.out.println(i+" 图片bitmap删除\n");
			}
			bitmapList.remove(i);
		}
	}
		
		
	
	private class MyAsyncTask extends AsyncTask<String , Bitmap , Bitmap>{
		
		private ImageView iv;
		public MyAsyncTask(ImageView iv){
			this.iv=iv;
		}
		
		@Override 
		public Bitmap doInBackground(String... params){
			
			Bitmap bm=getLocalBitmap(params[0]);
			bitmapList.add(bm);
			this.publishProgress(bm);
			return bm;
		}
		
		@Override 
		public void onPreExecute(){
			super.onPreExecute();
			
		}
		
		@Override 
		public void onPostExecute(Bitmap bm){
			super.onPostExecute(null);
		}
		
		
		@Override 
		public void onProgressUpdate(Bitmap... bm){
			 iv.setImageBitmap(bm[0]);
		}
		 
		
	}

	/**
	 *文件列表适配器，用于显示文件
	 */
	class MyAdapter extends BaseAdapter{
		
		
		private int count=0;
		LayoutInflater mInflater;
		public MyAdapter(Context context){
			mInflater=LayoutInflater.from(context);
		}

		@Override 
		public int getCount(){
			if(viewType.equals(SHOW_PICTURE))
				if(totalFile.size()%4==0)
					return totalFile.size()/4;
				else return totalFile.size()/4+1;

			return totalFile.size();

		}

		@Override
		public Object getItem(int position){
			return null;
		}

		@Override
		public long getItemId(int position ){
			return position;
		}

		MyAsyncTask ma ;
		@Override 
		public View getView(int position,View convertView , ViewGroup parent){
			if(convertView==null){
				if(viewType.equals(SHOW_PICTURE)){
					convertView=mInflater.inflate(R.layout.photo_view, null);
					}
					
				else {
					convertView=mInflater.inflate(R.layout.folder_list, null);
				}

			}

			if(viewType.equals(SHOW_PICTURE)){  // 照片文件列表

				final ImageView iv_1=(ImageView)convertView.findViewById(R.id.iv_1);
				ImageView iv_2=(ImageView)convertView.findViewById(R.id.iv_2);
				ImageView iv_3=(ImageView)convertView.findViewById(R.id.iv_3);
				ImageView iv_4=(ImageView)convertView.findViewById(R.id.iv_4);


				if(count<totalFile.size()){
					ma=new MyAsyncTask(iv_1);
					ma.execute(totalFile.get(count));
					viewList.add(iv_1);
					iv_1.setTag(count);
					count++;
					iv_1.setOnClickListener(PreviewsActivity.this);
				}

				if(count<totalFile.size()){
					ma=new MyAsyncTask(iv_2);
					ma.execute(totalFile.get(count));
					
					viewList.add(iv_1);
					iv_2.setTag(count);
					count++;
					iv_2.setOnClickListener(PreviewsActivity.this);
				}
				if(count<totalFile.size()){
					ma=new MyAsyncTask(iv_3);
					ma.execute(totalFile.get(count));
					viewList.add(iv_1);
					iv_3.setTag(count);
					count++;
					iv_3.setOnClickListener(PreviewsActivity.this);
				}
				if(count<totalFile.size()){
					ma=new MyAsyncTask(iv_4);
					ma.execute(totalFile.get(count));
					viewList.add(iv_1);
					iv_4.setTag(count);
					count++;
					iv_4.setOnClickListener(PreviewsActivity.this);

				}

				LayoutParams p=iv_1.getLayoutParams();
				p.width=DEVICE_WIDTH/4;
				p.height=p.width;

				iv_1.setLayoutParams(p);
				iv_2.setLayoutParams(p);
				iv_3.setLayoutParams(p);
				iv_4.setLayoutParams(p);

			}
			else {    // 视频或音频文件列表
				TextView tv_folder=(TextView)convertView.findViewById(R.id.tv_folder);
				ImageView iv_folder=(ImageView)convertView.findViewById(R.id.iv_folder);

				String[] fileName=totalFile.get(position).split("/");

				tv_folder.setText(fileName[fileName.length-1]);
				tv_folder.setTextSize(20);
				if(viewType.equals(SHOW_VIDEO))
					iv_folder.setImageResource(R.drawable.videos);
				else iv_folder.setImageResource(R.drawable.music);

			}

			return convertView ;
		}
		
		

	}


	/**
	 * 文件列表点击事件
	 */
	class ListItemListener implements OnItemClickListener{


		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position,
				long arg3) {
			// TODO Auto-generated method stub

			if(viewType.equals(SHOW_AUDIO)){
				playAudio(totalFile.get(position));
			}
			else if(viewType.equals(SHOW_VIDEO)){
				playVideo(totalFile.get(position));
			}
			else if(viewType.equals(SHOW_PICTURE)){
				// showPicture(totalFile.get(position));
			}

		}

		
		
		
		
		/**
		 * 播放录音文件
		 * @param path
		 */
		public void playAudio(String path){
			if(ap==null){
				ap=new AudioPlayer();
				try {
					ap.playAudio(path);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					showToast("播放异常！");
				}
			}
			else {
				ap.stopAudio();
				try {
					ap.playAudio(path);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					showToast("播放异常！");
				}

			}
		}

		

		/**
		 * 播放视频文件
		 * @param path
		 */
		public void playVideo(String path ){
			Intent intent=new Intent(PreviewsActivity.this,VideoPlayerActivity.class);
			intent.putExtra("path", path);
			startActivity(intent);
		}

	}
	
	

	/**
	 *  文件列表长按删除事件 
	 */
	
	class ItemLongClickListener implements  OnItemLongClickListener{
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View v, int position,
				long arg3) {
			final int  index=position;
			if(viewType.equals(SHOW_PICTURE)){
				 
			}
			else if(viewType.equals(SHOW_AUDIO)||viewType.equals(SHOW_VIDEO)){
				new AlertDialog.Builder(PreviewsActivity.this)
				.setTitle("提示")
				.setMessage("删除文件？")
				.setPositiveButton("确认",  new  DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						deleteMediaFile(index);
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
			
			return false ;
		}
		
		public  void  deletePhoto(String  path){
			
		}
		
		public void deleteMediaFile(int index){
			File f=new File(totalFile.get(index));
			if(f.exists()){
				f.delete();
				System.out.println("\n 文件 "+totalFile.get(index)+"  已删除  ？" + !f.exists());
				totalFile.remove(index);
				adapter.notifyDataSetChanged();
			}
		}
	}
	
	
	

	/**
	 * 停止播放录音文件
	 */
	public void stopAudio(){
		if(ap!=null){
			ap.stopAudio();
		}
	}
	
	

	
	/**
	 * 显示各种提示信息
	 * @param msg 提示信息
	 */
	public void showToast(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}


}
