package com.jr.uhf;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;
import com.jr.uhf.util.Util;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ReadActivity extends Activity {
	
	/* UI 控件 */
	private TextView textTitle ; 				 //标题
	private CheckBox checkTag ;					//是否勾选标签
	private Button buttonReadTag;				//读标签按钮
	private EditText editTag ;					//标签号输入框
	private Spinner spinnerMembank;				//存储区
	private EditText editAccess;				//密码
	private EditText editAddr;					//起始地址
	private EditText editLen;					//读取数据长度
	private EditText editReadData; 				//数据显示
	private Button buttonReadData;				//读数据按钮	
	
	private InputStream is ;					//蓝牙输入流
	private OutputStream os ;					//蓝牙输出流
	private NewSendCommendManager manager ;		//超高频指令句柄
	
	private int membank = 0;
	private int addr = 0;						//起始地址
	private int len = 1;						//读取数据长度
	private byte[] accessPassword ;				//访问密码
	
	
	
	private String[] membanks ;
	private List<String> listMembank = new ArrayList<String>();
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case BluetoothActivity.CONNECT_INTERRUPT:// 连接中断
				BluetoothActivity.connFlag = false;
				Toast.makeText(getApplicationContext(), "连接中断",
						Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}			
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read_tag);
		textTitle = (TextView) findViewById(R.id.textView_title);
		textTitle.setText(R.string.readData);
		textTitle.append("--已连接");
		//设置消息监听
		ConnectedThread.setHandler(mHandler);
		membanks = getResources().getStringArray(R.array.membanks);
		for(String membankItem : membanks){
			listMembank.add(membankItem);
		}
		//初始UI
		initUI();
		is = ConnectedThread.getSocketInoutStream();
		os = ConnectedThread.getSocketOutoutStream();
		manager = new NewSendCommendManager(is, os);
		//监听按钮
		listner();
		//初始化声音池
		Util.initSoundPool(this);
		
	}

	//初始UI
	private void initUI(){
		checkTag = (CheckBox) findViewById(R.id.checkBox_select);
		buttonReadTag = (Button) findViewById(R.id.button_read_tag);
		editTag = (EditText) findViewById(R.id.editText_tag);
		spinnerMembank = (Spinner) findViewById(R.id.spinner_menbank);
		editAccess = (EditText) findViewById(R.id.editText_read_access);
		editAddr = (EditText) findViewById(R.id.editText_read_addr);
		editLen = (EditText) findViewById(R.id.editText_read_len);
		editReadData = (EditText) findViewById(R.id.editText_read_data);
		buttonReadData = (Button)findViewById(R.id.button_read_data);
		//设置下拉
		spinnerMembank.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line,
				listMembank));
	}
	//监听按钮
	private void listner(){
		//读标签
		buttonReadTag.setOnClickListener(new OnReadTagListner());
		//读数据
		buttonReadData.setOnClickListener(new onReadDataListener());
		//数据存储区选择
		spinnerMembank.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				String membankStr = listMembank.get(position);
//				Log.e(TAG, membankStr);
				if("RESEVER".equals(membankStr)){
					membank = NewSendCommendManager.RESEVER_MENBANK;
				}else if("EPC".equals(membankStr)){
					membank = NewSendCommendManager.EPC_MEMBANK;
				}else if("TID".equals(membankStr)){
					membank = NewSendCommendManager.TID_MEMBANK;
				}else if("USER".equals(membankStr)){
					membank = NewSendCommendManager.USER_MENBANK;
				}
//				Log.e(TAG, membankStr + membank);
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapter) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private List<InventoryInfo> listTag = new ArrayList<InventoryInfo>(); //盘存标签
	private static final String TAG = "ReadActivity";
	//读标签
	private class OnReadTagListner implements OnClickListener{
		@Override
		public void onClick(View arg0) {

			//蓝牙连接断开
			if(!BluetoothActivity.connFlag){
				return;
			}
			//输入输出流不为null
			if(is != null && os != null){
				listTag = manager.inventoryRealTime();
				if(listTag != null && !listTag.isEmpty()){
					String epcStr = Tools.Bytes2HexString(listTag.get(0).getEpc(), listTag.get(0).getEpc().length);
					editTag.setText(epcStr);
					Log.e(TAG, "epcStr = " + epcStr);
				}
			}
			
		}
	}
	//读数据
	private class onReadDataListener implements OnClickListener{
		byte[] readdata ;
		@Override
		public void onClick(View arg0) {
			String passwordStr = editAccess.getText().toString();
			String addrStr = editAddr.getText().toString();
			String lenStr = editLen.getText().toString();
			if(passwordStr == null){
				Toast.makeText(getApplicationContext(), "密码不能为null", 0).show();
				return ;
			}
			if(addrStr == null){
				Toast.makeText(getApplicationContext(), "起始地址不能为null", 0).show();
				return ;
			}
			if(lenStr == null){
				Toast.makeText(getApplicationContext(), "长度不能为null", 0).show();
				return ;
			}
			accessPassword = Tools.HexString2Bytes(passwordStr);
			addr = Integer.valueOf(addrStr);
			len = Integer.valueOf(lenStr);
			if(len == 0){
				Toast.makeText(getApplicationContext(), "长度不能为0", 0).show();
				return;
			}
			//校验密码长度是否正确
			if(accessPassword.length != 4){
				Toast.makeText(getApplicationContext(), "密码长度不对", 0).show();
				return;
			}
			//蓝牙连接断开
			if(!BluetoothActivity.connFlag){
				return;
			}
			//
			try{
				readdata = ReadActivity.this.readData(membank, addr, len, accessPassword);
			}catch(Exception e){
				//出现异常
			}
			
			if(readdata != null && readdata.length > 1){
				Util.play(1, 0);
				Toast.makeText(getApplicationContext(), "读数据成功", 0).show();
				editReadData.setText(Tools.Bytes2HexString(readdata, readdata.length));
			}else{
				Toast.makeText(getApplicationContext(), "读数据失败，错误码"+ readdata[0], 0).show();
			}
		}
		
	}
	
	/*
	 * 读标签
	 */
	private byte[] readData(int membank, int address, int length , byte[] password ){
		byte[] data = null;
		/**
		 * 超高频读写数据很容易读写失败，因此以读写三次来降低读写失败的次数
		 */
		for(int i = 0; i < 3; i++){
			data = manager.readFrom6C(membank, address, length, password);
			//读数据成功，跳出循环
			if(data != null){
				Log.e(TAG, "data = " + Tools.Bytes2HexString(data, data.length));
				break;
			}
		}
		return data;
	}
	
	
}
