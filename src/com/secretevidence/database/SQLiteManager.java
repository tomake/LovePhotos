package com.secretevidence.database;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * ���ݿ������
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
			
			System.out.println("\n\n������"+this.table+"�ɹ�\n\n");
			
		}
		
		catch(Exception e){
			e.printStackTrace();
			System.out.println("\n\n������"+this.table+"ʧ��\n\n");
		}
	}
	
	
	public void closeDatabase(){
		try{
			this.SQLdb.close();
			System.out.println("\n\n�ر����ݿ�ɹ�\n\n");
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\n�ر����ݿ�ʧ��\n\n");
			
		}
		
	}
	
	public void insertData(String sql)throws SQLException{
		 
			this.SQLdb.execSQL(sql);
			System.out.println("\n\n����ɹ�\n\n");
	}
	
	public boolean updateData(String sql){
		
		try{
			this.SQLdb.execSQL(sql);
			System.out.println("\n\n���³ɹ�\n\n");
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\n����ʧ��\n\n");
			return false ;
		}
		return true;
	}
	
	public boolean deleteData(String sql){
		try{
			this.SQLdb.execSQL(sql);
			System.out.println("\n\nɾ���ɹ�\n\n");
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\nɾ��ʧ��\n\n");
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
				path.add(cur.getString(1));   // ��ȡ�ļ�·��
				System.out.println("\n\n�ļ� �� "+cur.getString(1)+"\n\n");
			}
			System.out.println("\n\n���ҳɹ�\n\n");
			
			return path;
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println("\n\n����ʧ��\n\n");
			return null;
		}
	}
	
	
}
