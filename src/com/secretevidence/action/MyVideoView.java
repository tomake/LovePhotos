package com.secretevidence.action;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * 控制视频预览大小
 * @author MAKE&BE
 *
 */
public class MyVideoView extends VideoView {

	public MyVideoView(Context context) {
		super(context); 
		// TODO Auto-generated constructor stub  
	}

	public MyVideoView(Context context, AttributeSet attrs) {
		super(context, attrs); 
		// TODO Auto-generated constructor stub1 
	} 

	public MyVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle); 
		// TODO Auto-generated constructor stub9  

	}


	// 重写此方法   才可以正常全屏 
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
	{ 
		int width = getDefaultSize(0, widthMeasureSpec); 
		int height = getDefaultSize(0, heightMeasureSpec);
		setMeasuredDimension(width , height);
	}

}