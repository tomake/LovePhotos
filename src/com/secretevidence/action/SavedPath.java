package com.secretevidence.action;

import java.io.File;
import java.util.Calendar;

/**
 * ��ȡ¼���ļ��洢����·��
 * @author MAKE&BE
 *
 */

public class SavedPath {
	
	private static String AUDIO_SAVE_DIRECTORY="/SecretEvidence/Audio/";  // ¼���ļ��洢·��
	private static String AUDIO_EXTENSION_NAME=".amr";					  // ¼���ļ���׺
	private static String VIDEO_SAVED_DIRECTORY="/SecretEvidence/Video/"; // ��Ƶ�ļ��洢·��
	private static String VIDEO_EXTENSION_NAME=".mp4";					  // ��Ƶ�ļ���׺
	private static String PICTURE_SAVED_DIRECTORY="/SecretEvidence/Picture/"; // �����ļ��洢·��
	private static String PICTURE_EXTENSION_NAME=".jpg";				  // ��Ƭ�ļ���׺
    
	public SavedPath(){
		 
	}
	
	/**
	 * ��ȡ¼���ļ�����·��
	 * @param directory
	 * @return
	 */
	public static String getAudioSavedPath(String directory){
		String path= directory+ AUDIO_SAVE_DIRECTORY;
		File folder=new File(path);
		if(!folder.exists()){
			 folder.mkdirs() ;
		}

		return path+ getCurrentTime()+ AUDIO_EXTENSION_NAME;
	}
	
	
	/**
	 * ��ȡ��Ƶ�ļ�����·��
	 * @param directory
	 * @return
	 */
	public static String getVideoSavedPath(String directory){
		String path= directory+ VIDEO_SAVED_DIRECTORY;
		File folder=new File(path);
		if(!folder.exists()){
			 folder.mkdirs() ;
		}

		return path+ getCurrentTime()+ VIDEO_EXTENSION_NAME;
		
	}

	/**
	 * ��ȡ��Ƭ�ļ�����·��
	 * @param directory
	 * @return
	 */
	public static String getPictureSavedPath(String directory){
		String path= directory+ PICTURE_SAVED_DIRECTORY;
		File folder=new File(path);
		if(!folder.exists()){
			 folder.mkdirs() ;
		}
		return path+ getCurrentTime()+ PICTURE_EXTENSION_NAME;
	}
	
	/**
	 * ��ȡ��ǰʱ��  �����ļ���  �����ļ�����ͻ
	 * @return
	 */
	public static String getCurrentTime(){
		Calendar c=Calendar.getInstance();
		 
		String time=c.get(Calendar.YEAR)+"_"+formatNum(c.get(Calendar.MONTH)+1) +"_"
				+formatNum(c.get(Calendar.DAY_OF_MONTH))+"_"
				+formatNum(c.get(Calendar.HOUR_OF_DAY))
				+formatNum(c.get(Calendar.MINUTE))
				+formatNum(c.get(Calendar.SECOND));

		return time;
	}
	
	public static String formatNum(int num){
		
		if(num>=10)
			return num+"";
		else 
			return "0"+num;
	}
}
