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
 * 实现视频播放功能    <主要模块>
 * @author MAKE&BE
 *
 */
public class VideoPlayerActivity extends Activity {

	private VideoView vv;		// 视频播放界面
	private MediaController mc; // 视频播放控制器
	
	/**
	 * 程序入口
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
	 * 界面销毁，释放视频播放器等相关资源
	 */
	public void onDestroy(){
		super.onDestroy();
		if(vv!=null){
			vv.stopPlayback();
			vv=null;
		}
	}


}
