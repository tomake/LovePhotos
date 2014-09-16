package com.secretevidence.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.secretevidence.R;

/**
 * ��ʾ�ļ��б�
 * @author MAKE&BE
 *
 */
public class RecordFilesActivity extends Activity {

	private String PHOTOS_FOLDER="��Ƭ";   // �ļ����
	private String AUDIOS_FOLDER="¼��";   // �ļ����
	private String VIDEOS_FOLDER="��Ƶ";   // �ļ����
	private String SHOW_PICTURE="picture";    // Ԥ���ļ�����
	private String SHOW_AUDIO="Audio";	      // Ԥ���ļ�����
	private String SHOW_VIDEO="Video";		  // Ԥ���ļ�����
	private String EXTRA_KEY="showType";	  // Intent �еļ�ֵ�� ���  ���ڻ�ȡ����
	private ListView listview;				  // �ļ�Ԥ���б�

	/**
	 * �������
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_record_files);

		initViewsId();
		this.listview.setBackgroundColor(getResources().getColor(R.color.light_blue));
		this.listview.setAdapter(new MyAdapter(this));
		this.listview.setOnItemClickListener(new ListItemListener());
	}

	/**
	 * ��ʼ���ؼ�ID
	 */
	public void initViewsId(){
		this.listview=(ListView)findViewById(R.id.listview);
	}


	/**
	 *�ļ��б���������������ʾ�ļ�
	 */
	class MyAdapter extends BaseAdapter{

		LayoutInflater mInflater;
		public MyAdapter(Context context){
			mInflater=LayoutInflater.from(context);
		}

		@Override 
		public int getCount(){
			return 3;

		}


		@Override
		public Object getItem(int position){
			return null;
		}

		@Override
		public long getItemId(int position ){
			return position;
		}

 

		@Override 
		public View getView(int position,View convertView , ViewGroup parent){
			if(convertView==null)
				convertView=mInflater.inflate(R.layout.preview_type_list, null);

			ImageView iv=(ImageView) convertView.findViewById(R.id.iv_list);
			if(position==0)
				iv.setImageResource(R.drawable.audio_lib);
			else if(position==1)
				iv.setImageResource(R.drawable.pic_lib);
			else if(position==2)
				iv.setImageResource(R.drawable.video_lib);
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
			Intent intent=new Intent(RecordFilesActivity.this,PreviewsActivity.class);

			switch(position){
			case 0:
				intent.putExtra(EXTRA_KEY, SHOW_AUDIO);
				startActivity(intent);
				break;
			case 1:
				Intent intent1 = new Intent(RecordFilesActivity.this , PhotoGalleryActivity.class);
				startActivity(intent1);
				break;
			case 2:
				intent.putExtra(EXTRA_KEY, SHOW_VIDEO);
				startActivity(intent);
				break;
			default :
				break;
			}

		}
	}

}
