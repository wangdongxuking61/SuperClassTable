package com.jr.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jr.uhf.BluetoothActivity;
import com.jr.uhf.command.Tools;
/*
已建立连接后启动的线程，需要传进来两个参数
socket用来获取输入流，读取远程蓝牙发送过来的消息
handler用来在收到数据时发送消息
*/
public class ConnectedThread extends Thread {
	
	private static final int RECEIVE_MSG = 7;
	private static final int SEND_MSG=8;
	private boolean isStop;
	private static BluetoothSocket socket;
	private static Handler handler;
	private static InputStream is;
	private static OutputStream os;
	
	private static String TAG = "ConnectedThread";
	
	public ConnectedThread(BluetoothSocket s,Handler h){
		socket=s;
		handler=h;
		isStop=false;
	}
	
	public static void setHandler(Handler h){
		handler = h;
	}
	
	public void run(){
		Log.i(TAG,"connectedThread.run()");
		byte[] buf;
		int size;
		
			size=0;
			buf=new byte[1024];
			try {
				try {
					is=socket.getInputStream();
					os = socket.getOutputStream();
					Log.i(TAG,"连接成功");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				while(!isStop){
//				Log.i(TAG,"连接成功");
				
//				size=is.read(buf);
//				Log.i(TAG,"读取了一次数据");
//			is.available();
			if(size>0){
				//把读取到的数据放进Bundle再放进Message，然后发送出去
//				sendMessageToHandler(buf,size, RECEIVE_MSG);
			}
		}
		} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.i(TAG,"连接已断开");
				byte[] temp = "连接已断开".getBytes();
				sendMessageToHandler(temp,temp.length, BluetoothActivity.CONNECT_INTERRUPT);
				isStop=true;
		}
	}
	
	@Override
	public void interrupt() {
		Log.i(TAG, "interrupt");
		isStop = true;
		if(is != null){
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(os != null){
			try {
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		super.interrupt();
	}
	
	
	
	public static InputStream getSocketInoutStream(){
		return is;
	}
	
	public static OutputStream getSocketOutoutStream(){
		return os;
	}
	
	public static void write(byte[] buf){
		try {
			os=socket.getOutputStream();
			os.write(buf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.i(TAG,buf.length+"---");
		sendMessageToHandler(buf,buf.length, SEND_MSG);
	}
	
	private static void sendMessageToHandler(byte[] buf,int bufLen ,int mode){
//		String msgStr=new String(buf, 0 , bufLen);
		String msgStr = Tools.Bytes2HexString(buf, bufLen);
		Log.i(TAG, msgStr);
		Bundle bundle=new Bundle();
		bundle.putString("str", msgStr);
		Message msg=new Message();
		msg.setData(bundle);
		msg.what=mode;
		handler.sendMessage(msg);
	}
}
