package com.secretevidence.action;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;
/**
 * 实现声音录制功能
 * @author MAKE&BE
 *
 */
public class AudioRecorder {
 
	private MediaRecorder mRecorder;  	// 多媒体录制器

	public AudioRecorder() {

	}

	/**
	 * 开始录制
	 * @param savedPath
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void startRecord(String savedPath) throws IllegalStateException, IOException{
		if(this.mRecorder!=null)
			return ;
		mRecorder=new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mRecorder.setOutputFile(savedPath);
		File file=new File(savedPath);
		file.createNewFile();
		mRecorder.prepare();
		mRecorder.start();
	}
	
	/**
	 * 停止录制
	 */
	
	public void stopRecord(){
		if(mRecorder==null)
			return ;
		mRecorder.stop();
		mRecorder.release();
		mRecorder=null;
		
	}
}
