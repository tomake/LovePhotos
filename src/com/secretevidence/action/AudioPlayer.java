package com.secretevidence.action;

import java.io.File;
import java.io.FileInputStream;

import android.media.MediaPlayer;

/**
 * 实现音乐播放功能
 * @author MAKE&BE
 *
 */
public class AudioPlayer {

	private MediaPlayer mp;   // 多媒体播放器
	
	public AudioPlayer(){
		
	}
	
	/**
	 *  播放音乐
	 * @param path
	 * @throws Exception
	 */
	public void playAudio(String path) throws Exception{
		if(mp==null){
			mp=new MediaPlayer();
			File file=new File(path);
			@SuppressWarnings("resource")
			FileInputStream fis=new FileInputStream(file);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
		}
		
		else {
			
			mp.stop();
			mp.reset();
			File file=new File(path);
			@SuppressWarnings("resource")
			FileInputStream fis=new FileInputStream(file);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
		}
	}
	
	/**
	 * 停止播放，释放相关资源
	 */
	public void stopAudio(){
		if(mp!=null)
		{
			mp.stop();
			mp.release();
			mp=null;
		}
	}
	
	/**
	 * 暂停播放
	 */
	public void pauseAudio(){
		if(mp!=null){
			mp.pause();
		}
	}
}
