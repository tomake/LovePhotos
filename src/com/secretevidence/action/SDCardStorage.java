package com.secretevidence.action;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;

/**
 * 获取可用存储空间大小
 * @author MAKE&BE
 *
 */
public class SDCardStorage {

	@SuppressWarnings("deprecation")
	public static long getAvailableStorage(){
		File path=Environment.getExternalStorageDirectory();
		StatFs stat=new StatFs(path.getPath());
		
		return stat.getBlockSize()*stat.getAvailableBlocks()/1024/1024;
	}
}
