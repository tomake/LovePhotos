package com.secretevidence.action;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;
/**
 * ʵ������¼�ƹ���
 * @author MAKE&BE
 *
 */
public class AudioRecorder {
 
	private MediaRecorder mRecorder;  	// ��ý��¼����

	public AudioRecorder() {

	}

	/**
	 * ��ʼ¼��
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
	 * ֹͣ¼��
	 */
	
	public void stopRecord(){
		if(mRecorder==null)
			return ;
		mRecorder.stop();
		mRecorder.release();
		mRecorder=null;
		
	}
}
