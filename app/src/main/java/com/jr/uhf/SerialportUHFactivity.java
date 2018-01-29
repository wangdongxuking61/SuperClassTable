package com.jr.uhf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;
import com.jr.uhf.entity.EPC;
import com.jr.uhf.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
/**
 * 串口操作UHF
 * @author Administrator
 *
 */
public class SerialportUHFactivity extends Activity {
	/* UI控件 */
	private TextView textTitle ;
	private Button buttonReadTag ;
	private Button buttonClear ;
	private ListView listViewEPC ;
	private Button buttonParaSetting ;
	private Button buttonMoreOption ;
	private EditText editTagCount ;
	private Button buttonLockKill ;
	
//	private NewSendCommendManager cmdManager = new NewSendCommendManager(SerialportSettingActivity.is, 
//			SerialportSettingActivity.os);
	boolean isStop = true;
	boolean isRunning = true;
	List<EPC> listEPC = new ArrayList<EPC>(); //EPC列表
	
	
	private final byte HEAD = (byte)0xAA;
	private final byte END = (byte) 0x8E;
	
	private String mode = "";
	private final String TAG = "SerialportUHFactivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_serialport_uhf);
		textTitle = (TextView) findViewById(R.id.textView_title);
//		textTitle.setText("串口UHF操作");
		mode = getIntent().getStringExtra("mode");
		if("usb".equals(mode)){
			textTitle.setText("USB-UHF操作");
		}else if("serialport".endsWith(mode)){
			textTitle.setText("串口UHF操作");
		}
		
		
		//初始UI
		initUI();
		//监听按钮
		listener();
		
		Util.initSoundPool(this);
		
	}
	//初始UI
	private void initUI(){
		buttonReadTag = (Button) findViewById(R.id.button_start_read);
		buttonClear = (Button) findViewById(R.id.button_clear);
		listViewEPC = (ListView) findViewById(R.id.listView_data);
		buttonParaSetting  = (Button) findViewById(R.id.button_para_setting);
		buttonMoreOption = (Button) findViewById(R.id.button_more_option);
		editTagCount = (EditText) findViewById(R.id.editText_tag_count);
		buttonLockKill = (Button) findViewById(R.id.button_lock_kill);
		
	}
	
	private void listener(){
		//盘存标签
		buttonReadTag.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				if(isStop){
//					isStop = false ;
//					buttonReadTag.setText(R.string.stop_read);
//				}else{
//					isStop = true ;
//					buttonReadTag.setText(R.string.inventory);
//					
//				}
				//循环
				if(isSend){
//					isRuning = false;
					isSend = false;
					isRecv = false;
					buttonReadTag.setText("读标签");
				}else{
//					isRuning = true;.
					isSend = true;
					isRecv = true;
					buttonReadTag.setText("停止");
				}
				
			}
		});
		//参数设置
		buttonParaSetting.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingIntent = new Intent(SerialportUHFactivity.this, SerialportUHFsettingActivity.class);
				settingIntent.putExtra("mode", mode);
				startActivity(settingIntent);
			}
		});
		//锁定销毁操作
		buttonLockKill.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent settingIntent = new Intent(SerialportUHFactivity.this, SerialportUHFlockKillActivity.class);
				settingIntent.putExtra("mode", mode);
				startActivity(settingIntent);
				
			}
		});
		//更多操作,读写操作
		buttonMoreOption.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent moreOptionIntent = new Intent(SerialportUHFactivity.this, SerialportUHFreadWriteActivity.class);
				moreOptionIntent.putExtra("mode", mode);
				startActivity(moreOptionIntent);
			}
		});
		//清空
		buttonClear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editTagCount.setText("");
				listEPC.removeAll(listEPC);
				listViewEPC.setAdapter(null);	
				
			}
		});
	}


//	List<EPC> listEPC = new ArrayList<EPC>(); //EPC列表
//	// 循环读标签
//	class LoopReadThread implements Runnable {
//
//		@Override
//		public void run() {
//			List<InventoryInfo> mList = new ArrayList<InventoryInfo>();
//			while(isRunning){
//				while(!isStop){
//					mList = cmdManager.inventoryRealTime();
//					if(mList != null && !mList.isEmpty()){
//						for(InventoryInfo info : mList){
////							String epcStr = Tools.Bytes2HexString(epc, epc.length);
//							addToList(listEPC, info);
//						}
//					}
//		}
//			}
//
//		}
//
//	}
	
	@Override
	protected void onResume() {
		isRunning = true;
//		new Thread(new LoopReadThread()).start();
		new RecvThread().start();
		new SendCmdThread().start();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		isRunning = false;
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		isRunning = false;
		super.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			finish();
		}
		isRunning = false ;
		isSend = false ;
		isRecv = false ;
		return super.onKeyDown(keyCode, event);
	}
	
//	private List<Map<String, Object>> listMap;
	
	
	boolean isRuning = true ;  //控制发送接收线程
	boolean isSend = false ;  //控制发送指令
	
	//发送盘存指令
	private class SendCmdThread extends Thread{
		@Override
		public void run() {
			//盘存指令
			byte[] cmd = { HEAD, (byte) 0x00, (byte) 0x22, (byte) 0x00,
					(byte) 0x00, (byte) 0x22, END };
			while(isRunning){
				OutputStream os = null;
				if("usb".equals(mode)){
					os = UsbSettingActivity.getOutputStream();
				}else if("serialport".endsWith(mode)){
					os = SerialportSettingActivity.os;
				}
				if(isSend){
					try {
						os.write(cmd);
					} catch (IOException e) {
						isSend = false ;
						isRunning = false ;
						Log.e(TAG, "Socket 连接出错" + e.toString());
					}
					try {
						Thread.sleep(60);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			super.run();
		}
	}
	
	boolean isRecv = false;
	//接收线程
	private class RecvThread extends Thread{
		@Override
		public void run() {
			InputStream is = null;
			if("usb".equals(mode)){
				is = UsbSettingActivity.getInputStream();
			}else if("serialport".endsWith(mode)){
				is =  SerialportSettingActivity.is;
			}
			int size = 0;
			byte[] buffer = new byte[256];
			byte[] temp = new byte[512];
			int index = 0;  //temp有效数据指向
			int count = 0;  //temp有效数据长度
			while(isRunning){
				if(isRecv){
					try {
						Thread.sleep(50);
						size = is.read(buffer);
						if(size > 0){
							count += size ;
							//超出temp长度清空
							if(count > 512){
								count = 0;
								Arrays.fill(temp, (byte)0x00);
							}
							//先将接收到的数据拷到temp中
							System.arraycopy(buffer, 0, temp, index, size);
							index = index + size ;
							if(count > 7){
//								Log.e(TAG, "temp: " + Tools.Bytes2HexString(temp, count));
								//判断AA022200
								if((temp[0] == HEAD)&& (temp[1] == (byte)0x02) && (temp[2] == (byte)0x22) && (temp[3] == (byte)0x00)){
									//正确数据位长度等于RSSI（1个字节）+PC（2个字节）+EPC
									int len = temp[4]&0xff;
									if(count < len + 7){//数据区尚未接收完整
										continue;
									}
									if(temp[len + 6] != END){//数据区尚未接收完整
										continue ;
									}
									//得到完整数据包
									byte[] packageBytes = new byte[len + 7];
									System.arraycopy(temp, 0, packageBytes, 0, len + 7);
//									Log.e(TAG, "packageBytes: " + Tools.Bytes2HexString(packageBytes, packageBytes.length));
									//校验数据包
									byte crc = checkSum(packageBytes);
									InventoryInfo info = new InventoryInfo();
									if( crc == packageBytes[len + 5]){
										//RSSI
										info.setRssi(temp[5]);
										//PC
										info.setPc(new byte[]{temp[6],temp[7]});
										//EPC
										byte[] epcBytes = new byte[len - 5];
										System.arraycopy(packageBytes, 8, epcBytes, 0, len - 5);
										info.setEpc(epcBytes);
										Util.play(1, 0);//播放提示音
										addToList(listEPC, info);
									}
									count = 0;
									index = 0;
									Arrays.fill(temp, (byte)0x00);
								}else{
									//包错误清空
									count = 0;
									index = 0;
									Arrays.fill(temp, (byte)0x00);
								}
							}
			                   
						}
						
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						isRunning = false ;
						Log.e(TAG, "连接出错" + e.toString());
					}
				}
			}
			super.run();
		}
	}
	
	//计算校验和
	public byte checkSum(byte[] data) {
		byte crc = 0x00;
		// 从指令类型累加到参数最后一位
		for (int i = 1; i < data.length - 2; i++) {
			crc += data[i];
		}
		return crc;
	}

	
	List<Map<String, Object>> listMap ;
	// 将读取的EPC添加到LISTVIEW
	private void addToList(final List<EPC> list, final InventoryInfo info) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String epc = Tools.Bytes2HexString(info.getEpc(), info.getEpc().length);
				String pc = Tools.Bytes2HexString(info.getPc(), info.getPc().length);
				int rssi = info.getRssi();
				// 第一次读入数据
				if (list.isEmpty()) {
					EPC epcTag = new EPC();
					epcTag.setEpc(epc);
					epcTag.setCount(1);
					epcTag.setPc(pc);
					epcTag.setRssi(rssi);
					list.add(epcTag);
				} else {
					for (int i = 0; i < list.size(); i++) {
						EPC mEPC = list.get(i);
						// list中有此EPC
						if (epc.equals(mEPC.getEpc())) {
							mEPC.setCount(mEPC.getCount() + 1);
							mEPC.setRssi(rssi);
							mEPC.setPc(pc);
							list.set(i, mEPC);
							break;
						} else if (i == (list.size() - 1)) {
							// list中没有此epc
							EPC newEPC = new EPC();
							newEPC.setEpc(epc);
							newEPC.setCount(1);
							newEPC.setPc(pc);
							newEPC.setRssi(rssi);
							list.add(newEPC);
						}
					}
				}
				// 将数据添加到ListView
				listMap = new ArrayList<Map<String, Object>>();
				int idcount = 1;
//				Util.play(1, 0); //播放提示音
				for (EPC epcdata : list) {
					Map<String, Object> map = new HashMap<String, Object>();
					
					map.put("EPC", epcdata.getEpc());
					map.put("PC", epcdata.getPc() +"");
					map.put("RSSI", epcdata.getRssi() + "Dbm");
					map.put("COUNT", epcdata.getCount());
					idcount++;
					listMap.add(map);
				}
				editTagCount.setText("" + listEPC.size());
				listViewEPC.setAdapter(new SimpleAdapter(SerialportUHFactivity.this,
						listMap, R.layout.list_epc_item, new String[] { 
								"EPC", "PC","RSSI","COUNT" }, new int[] {
								R.id.textView_item_epc, R.id.textView_item_pc,
								R.id.textView_item_rssi,R.id.textView_item_count }));
			}
		});
	}
}
