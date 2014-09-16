package com.secretevidence.database;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * 数据库管理类
 * @author MAKE&BE
 *
 */
public class SQLiteManager {
	
	private String databaseName;
	private String table;
	private SQLiteDatabase SQLdb;
	
	
	
	
	public SQLiteManager(){
		
	}
	public SQLiteManager (String databaseName ){
		this.databaseName=databaseName;
	}
	
	
	public void createOrOpenDatabase(String sql){
		
		 
		try{
			this.SQLdb=SQLiteDatabase.openDatabase(databaseName, null
					, SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.CREATE_IF_NECESSARY);
			this.SQLdb.execSQL(sql);
			
			System.out.println("\n\n创建表"+this.table+"成功\n\n");
			
		}
		
		catch(Exception e){
			e.printStackTrace();
			System.out.println("\n\n创建表"+this.table+"失败\n\n");
		}
	}
	
	
	public void closeDatabase(){
		try{
			this.SQLdb.close();
			System.out.println("\n\n关闭数据库成功\n\n");
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\n关闭数据库失败\n\n");
			
		}
		
	}
	
	public void insertData(String sql)throws SQLException{
		 
			this.SQLdb.execSQL(sql);
			System.out.println("\n\n插入成功\n\n");
	}
	
	public boolean updateData(String sql){
		
		try{
			this.SQLdb.execSQL(sql);
			System.out.println("\n\n更新成功\n\n");
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\n更新失败\n\n");
			return false ;
		}
		return true;
	}
	
	public boolean deleteData(String sql){
		try{
			this.SQLdb.execSQL(sql);
			System.out.println("\n\n删除成功\n\n");
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\n删除失败\n\n");
			return false ;
		}
		return true;
	}
	
	public  ArrayList<String> queryData(String sql,String[] selectionArgs){
		 
		ArrayList<String> path=new ArrayList<String>();
		try{
			System.out.println("sql "+sql+"\n\n type "+selectionArgs[0]);
			Cursor cur=this.SQLdb.rawQuery(sql, selectionArgs);
			//this.SQLdb.execSQL(sql);
			 
			while(cur.moveToNext()){
				path.add(cur.getString(1));   // 获取文件路径
				System.out.println("\n\n文件 ： "+cur.getString(1)+"\n\n");
			}
			System.out.println("\n\n查找成功\n\n");
			
			return path;
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\n查找失败\n\n");
			return null;
		}
	}
	
	
}
