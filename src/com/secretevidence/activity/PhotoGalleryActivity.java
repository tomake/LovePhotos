package com.secretevidence.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.secretevidence.R;
import com.secretevidence.adapter.PictureViewAdapter;

public class PhotoGalleryActivity extends Activity {
	private static String PICTURE_DIRECTORY="/SecretEvidence/Picture/"; // ����ļ��洢Ŀ¼
	private ArrayList<String> totalFile = new ArrayList<String>() ;
	private GridView picture_gallery ;
	private String CURRENT_TAG="current";	// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String FILE_TAG="files";		// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private String  INDEX_TAG="index";		// Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo_gallery);
		setTitle("��ƬԤ��") ;
		initViews();
	}

	private void initViews() {
		getFiles();
		picture_gallery = (GridView)findViewById(R.id.picture_gallery) ;
		picture_gallery.setAdapter(new PictureViewAdapter(getApplicationContext(), totalFile));
		picture_gallery.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent =new Intent(PhotoGalleryActivity.this,PhotoViewActivity.class);
				intent.putExtra(CURRENT_TAG, totalFile.get(position));
				intent.putExtra(INDEX_TAG, position);
				intent.putStringArrayListExtra(FILE_TAG, totalFile);
				startActivity(intent);
			}
		});
	}


	/**
	 * ��ȡԤ���ļ�·��
	 */
	public  ArrayList<String> getFiles(){
		String path=this.getSavedDirectory() + PICTURE_DIRECTORY;
		System.out.println("path = "  + path );
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

		return (ArrayList<String>) totalFile ;
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

}
