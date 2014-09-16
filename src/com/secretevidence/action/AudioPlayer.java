package com.secretevidence.action;

import java.io.File;
import java.io.FileInputStream;

import android.media.MediaPlayer;

/**
 * ʵ�����ֲ��Ź���
 * @author MAKE&BE
 *
 */
public class AudioPlayer {

	private MediaPlayer mp;   // ��ý�岥����
	
	public AudioPlayer(){
		
	}
	
	/**
	 *  ��������
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
	 * ֹͣ���ţ��ͷ������Դ
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
	 * ��ͣ����
	 */
	public void pauseAudio(){
		if(mp!=null){
			mp.pause();
		}
	}
}
