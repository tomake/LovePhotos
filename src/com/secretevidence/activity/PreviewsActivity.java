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
 * ʵ���ļ�Ԥ������   <��Ҫģ��>
 * @author MAKE&BE
 *
 */
public class PreviewsActivity extends Activity implements OnClickListener{

	private AudioPlayer ap;				// ���ֲ�����
	private ListView listview;			// ������ʾ�ļ��б�
	private View view_line;				// �ָ���
	private View v_divide;				// �ָ���
	private ImageView iv_titleImg;
	private ArrayList<String> totalFile= new ArrayList<String>();  		// �洢�ļ�·��
	private static String AUDIO_DIRECTORY="/SecretEvidence/Audio/"; 	// ¼���ļ��洢Ŀ¼
	private static String VIDEO_DIRECTORY="/SecretEvidence/Video/";		// ��Ƶ�ļ��洢Ŀ¼
	private static String PICTURE_DIRECTORY="/SecretEvidence/Picture/"; // ����ļ��洢Ŀ¼
	private String CURRENT_TAG="current";	// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String FILE_TAG="files";		// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String  INDEX_TAG="index";		// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String EXTRA_KEY="showType";	// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String SHOW_PICTURE="picture";	// Ԥ��ģʽ����Ƭ
	private String SHOW_AUDIO="Audio";		// Ԥ��ģʽ��¼��
	private String SHOW_VIDEO="Video";		// Ԥ��ģʽ����Ƶ
	private String viewType;			 	// ���Ԥ������
	private int DEVICE_WIDTH;				// ���Ԥ����ͼƬ���
	private int DEVICE_HEIGHT;				// ���Ԥ����ͼƬ�߶�
	
	private MyAdapter adapter;

	/***
	 * �������
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
	 * ��ʼ���ؼ�ID
	 */
	public void initViewsId(){
		this.listview=(ListView)findViewById(R.id.listview);
		this.view_line=(View)findViewById(R.id.view_line);
		this.v_divide=(View)findViewById(R.id.v_divide);
		this.iv_titleImg=(ImageView)findViewById(R.id.iv_titleImg);
	}

	/**
	 * ʵ�ֿؼ�����¼�
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
	 * ���õ�ǰԤ������
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
	 * ��ȡԤ���ļ�·��
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
	 * �����б���
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
	 * ��ȡ�ֻ��豸�ߴ�
	 */
	public void getDeviceSize(){
		DisplayMetrics dm=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		DEVICE_WIDTH=dm.widthPixels;
		DEVICE_HEIGHT=dm.heightPixels;
	}

	
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
	        //��ʼ����ͼƬ����ʱ��options.inJustDecodeBounds ���true��  ,��ʾ��Ϊbitmap�����ڴ�
	        newOpts.inJustDecodeBounds = true;  
	        Bitmap bitmap = BitmapFactory.decodeFile(path,newOpts);//��ʱ����bmΪ��  
	          
	        newOpts.inJustDecodeBounds = false;  
	        int w = newOpts.outWidth;  
	        int h = newOpts.outHeight;  
	        //���������ֻ��Ƚ϶���800*480�ֱ��ʣ����ԸߺͿ���������Ϊ  
	        float hh = 800f;//�������ø߶�Ϊ800f  
	        float ww = 800f;//�������ÿ��Ϊ480f  
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

	ArrayList<ImageView> viewList=new ArrayList<ImageView>();
	ArrayList<Bitmap> bitmapList=new ArrayList<Bitmap>();
	
	
	private void recycleMemory(){
		for(int i=0;i<bitmapList.size();i++){
			
			Bitmap bm= bitmapList.get(i);
			if(!bm.isRecycled()){
				bm.recycle();
				System.out.println(i+" ͼƬbitmapɾ��\n");
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
	 *�ļ��б���������������ʾ�ļ�
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

			if(viewType.equals(SHOW_PICTURE)){  // ��Ƭ�ļ��б�

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
			else {    // ��Ƶ����Ƶ�ļ��б�
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
	 * �ļ��б����¼�
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
		 * ����¼���ļ�
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
					showToast("�����쳣��");
				}
			}
			else {
				ap.stopAudio();
				try {
					ap.playAudio(path);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					showToast("�����쳣��");
				}

			}
		}

		

		/**
		 * ������Ƶ�ļ�
		 * @param path
		 */
		public void playVideo(String path ){
			Intent intent=new Intent(PreviewsActivity.this,VideoPlayerActivity.class);
			intent.putExtra("path", path);
			startActivity(intent);
		}

	}
	
	

	/**
	 *  �ļ��б���ɾ���¼� 
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
				.setTitle("��ʾ")
				.setMessage("ɾ���ļ���")
				.setPositiveButton("ȷ��",  new  DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						deleteMediaFile(index);
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
			
			return false ;
		}
		
		public  void  deletePhoto(String  path){
			
		}
		
		public void deleteMediaFile(int index){
			File f=new File(totalFile.get(index));
			if(f.exists()){
				f.delete();
				System.out.println("\n �ļ� "+totalFile.get(index)+"  ��ɾ��  ��" + !f.exists());
				totalFile.remove(index);
				adapter.notifyDataSetChanged();
			}
		}
	}
	
	
	

	/**
	 * ֹͣ����¼���ļ�
	 */
	public void stopAudio(){
		if(ap!=null){
			ap.stopAudio();
		}
	}
	
	

	
	/**
	 * ��ʾ������ʾ��Ϣ
	 * @param msg ��ʾ��Ϣ
	 */
	public void showToast(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}


}
