package com.secretevidence.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

import com.secretevidence.R;
import com.secretevidence.action.MyVideoView;

/**
 * ʵ����Ƶ���Ź���    <��Ҫģ��>
 * @author MAKE&BE
 *
 */
public class VideoPlayerActivity extends Activity {

	private VideoView vv;		// ��Ƶ���Ž���
	private MediaController mc; // ��Ƶ���ſ�����
	
	/**
	 * �������
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
				, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		

		DisplayMetrics dm= new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		if(dm.widthPixels/dm.heightPixels>0){
			setContentView(R.layout.activity_video_player);
		}
		else if(dm.widthPixels/dm.heightPixels==0){
			setContentView(R.layout.activity_video_player_portrait);
		}

		String path=getIntent().getExtras().getString("path");

		MyVideoView mvv=(MyVideoView)findViewById(R.id.mvv);
		mvv.setVideoPath(path);
		mc=new MediaController(this);
		mvv.setMediaController(mc);
		mc.setMediaPlayer(mvv);
		mvv.start();
	}

	/**
	 * �������٣��ͷ���Ƶ�������������Դ
	 */
	public void onDestroy(){
		super.onDestroy();
		if(vv!=null){
			vv.stopPlayback();
			vv=null;
		}
	}


}
