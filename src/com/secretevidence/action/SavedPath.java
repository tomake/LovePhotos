package com.secretevidence.action;

import java.io.File;
import java.util.Calendar;

/**
 * 获取录制文件存储绝对路径
 * @author MAKE&BE
 *
 */

public class SavedPath {
	
	private static String AUDIO_SAVE_DIRECTORY="/SecretEvidence/Audio/";  // 录音文件存储路径
	private static String AUDIO_EXTENSION_NAME=".amr";					  // 录音文件后缀
	private static String VIDEO_SAVED_DIRECTORY="/SecretEvidence/Video/"; // 视频文件存储路径
	private static String VIDEO_EXTENSION_NAME=".mp4";					  // 视频文件后缀
	private static String PICTURE_SAVED_DIRECTORY="/SecretEvidence/Picture/"; // 照相文件存储路径
	private static String PICTURE_EXTENSION_NAME=".jpg";				  // 照片文件后缀
    
	public SavedPath(){
		 
	}
	
	/**
	 * 获取录音文件绝对路径
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
	 * 获取视频文件绝对路径
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
	 * 获取照片文件绝对路径
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
	 * 获取当前时间  用作文件名  避免文件名冲突
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
