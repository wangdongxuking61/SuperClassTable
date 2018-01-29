package com.jr.uhf;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;
import com.jr.uhf.util.Util;

public class WriteActivity extends Activity {
	/* UI 控件 */
	private TextView textTitle;
	private CheckBox checkTag;
	private Button buttonReadTag;
	private EditText editTag;
	private Spinner spinnerMembank;
	private EditText editAccess;
	private EditText editAddr;
	private EditText editLen;
	private EditText editWriteData;
	private Button buttonReadData;

	private String[] membanks;
	private List<String> listMembank = new ArrayList<String>();
	private int membank; // 数据区
	private InputStream is ;					//蓝牙输入流
	private OutputStream os ;					//蓝牙输出流
	private NewSendCommendManager manager; // 超高频指令管理
	private byte[] writeData; // 写入的数据

	private final String TAG = "WriteActivity";

	private Handler mHandler = new Handler() {
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
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_write_tag);
		textTitle = (TextView) findViewById(R.id.textView_title);
		textTitle.setText(R.string.write_data);
		// 设置消息监听
		ConnectedThread.setHandler(mHandler);
		membanks = getResources().getStringArray(R.array.membanks);
		for (String membankItem : membanks) {
			listMembank.add(membankItem);
		}
		// 初始UI
		initUI();
		is = ConnectedThread.getSocketInoutStream();
		os = ConnectedThread.getSocketOutoutStream();
		manager = new NewSendCommendManager(is, os);
		//监听按钮
		 listner() ;
		 //初始化声音池
		 Util.initSoundPool(this);
	}

	private void initUI() {
		checkTag = (CheckBox) findViewById(R.id.checkBox_write_select);
		buttonReadTag = (Button) findViewById(R.id.button_write_read_tag);
		editTag = (EditText) findViewById(R.id.editText_write_tag);
		spinnerMembank = (Spinner) findViewById(R.id.spinner_menbank_write);
		editAccess = (EditText) findViewById(R.id.editText_write_read_access);
		editAddr = (EditText) findViewById(R.id.editText_write_read_addr);
		editLen = (EditText) findViewById(R.id.editText_write_read_len);
		editWriteData = (EditText) findViewById(R.id.editText_write_data);
		buttonReadData = (Button) findViewById(R.id.button_write_data);

		// 设置下拉
		spinnerMembank.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, listMembank));
	}

	// 监听按钮
	private void listner() {
		// 读标签
		 buttonReadTag.setOnClickListener(new OnReadTagListner());
		// 写数据
		 buttonReadData.setOnClickListener(new onWriteDataListener());
		// 数据存储区选择
		spinnerMembank.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				String membankStr = listMembank.get(position);
				// Log.e(TAG, membankStr);
				if ("RESEVER".equals(membankStr)) {
					membank = NewSendCommendManager.RESEVER_MENBANK;
				} else if ("EPC".equals(membankStr)) {
					membank = NewSendCommendManager.EPC_MEMBANK;
				} else if ("TID".equals(membankStr)) {
					membank = NewSendCommendManager.TID_MEMBANK;
				} else if ("USER".equals(membankStr)) {
					membank = NewSendCommendManager.USER_MENBANK;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapter) {
				// TODO Auto-generated method stub

			}
		});
	}

	private byte[] accessPassword; //
	private int addr;
	private int len;
	
	private List<InventoryInfo> listTag ;
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
	
	// 写数据数据
	private class onWriteDataListener implements OnClickListener {
		boolean writeFlag = false;

		@Override
		public void onClick(View arg0) {
			String passwordStr = editAccess.getText().toString();
			String addrStr = editAddr.getText().toString();
			String dataStr = editWriteData.getText().toString();
			if(passwordStr == null){
				Toast.makeText(getApplicationContext(), "密码不能为null", 0).show();
				return ;
			}
			if(addrStr == null){
				Toast.makeText(getApplicationContext(), "起始地址不能为null", 0).show();
				return ;
			}
			accessPassword = Tools.HexString2Bytes(passwordStr);
			addr = Integer.valueOf(addrStr);
			// 校验密码长度是否正确
			if (accessPassword.length != 4) {
				Toast.makeText(getApplicationContext(), "密码长度不对", 0).show();
				return;
			}
			if (dataStr == null || "".equals(dataStr)) {
				Toast.makeText(getApplicationContext(), "写入数据不能为空", 0).show();
				return;
			}
			int dataStrLen = dataStr.length();
			// data数据是否为4的整数倍（1word），若不足则补0
			int syCount = dataStrLen % 4;
			if (syCount != 0) {
				for (int i = 0; i < syCount; i++) {
					dataStr = dataStr + "0";
				}
			}
			writeData = Tools.HexString2Bytes(dataStr);
			// 蓝牙连接断开
			if (!BluetoothActivity.connFlag) {
				return;
			}
			//
			try {
				writeFlag = writeData(membank, addr, accessPassword, writeData,
						writeData.length);// 写入数据
			} catch (Exception e) {
				// 出现异常
			}
			
			if(writeFlag){
				Toast.makeText(getApplicationContext(), "写数据成功", 0).show();
				Util.play(1, 0);//播放提示音
			}
		}

	}

	/*
	 * 写标签数据
	 */
	private boolean writeData(int membank, int address, byte[] password,
			byte[] data, int dataLen) {
		boolean writeFlag = false;
		/**
		 * 超高频读写数据很容易读写失败，因此以读写三次来降低读写失败的次数
		 */
		for (int i = 0; i < 3; i++) {
			writeFlag = manager.writeTo6C(password, membank, address,
					dataLen / 2, data);
			// 读数据成功，跳出循环
			if (writeFlag) {
				Log.e(TAG, "write success!!");
				break;
			}
		}
		return writeFlag;
	}

}
