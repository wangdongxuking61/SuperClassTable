package com.jr.uhf;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;
import com.jr.uhf.util.Util;

public class SerialportUHFreadWriteActivity extends Activity implements
		OnClickListener {
	private EditText editAccess;
	private Spinner spinnerMembank;
	private EditText editAddr;
	private EditText editLen;
	private Button buttonRead;
	private Button buttonWrite;
	private EditText editRead;
	private EditText editWrite;
	private Button buttonReadTag; // 读标签按钮
	private EditText editTag;
	private TextView textTitle ;

	private String[] membanks;
	private List<String> listMembank = new ArrayList<String>();
	private int membank = 0; // 数据区
	private int addr; // 起始地址
	private int len; // 数据长度word
	private byte[] accessPassword; // 访问密码
	// UHF指令
//	private NewSendCommendManager manager = new NewSendCommendManager(
//			SerialportSettingActivity.is, SerialportSettingActivity.os);
	private NewSendCommendManager cmdManager = null;
	String mode ;

	private String TAG = "SerialportUHFMoreActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_uhf_more_option);
		textTitle = (TextView) findViewById(R.id.textView_title);
		mode = getIntent().getStringExtra("mode");
		if("usb".equals(mode)){
			textTitle.setText("USB-读/写操作");
			cmdManager = new NewSendCommendManager(
					UsbSettingActivity.getInputStream(), UsbSettingActivity.getOutputStream());
		}else if("serialport".endsWith(mode)){
			textTitle.setText("串口-读/写操作");
			cmdManager = new NewSendCommendManager(
					SerialportSettingActivity.is, SerialportSettingActivity.os);
		}
		// 初始UI
		initView();
		// 监听
		listener();
		// 初始化声音池
		Util.initSoundPool(this);
	}

	private void initView() {
		editAccess = (EditText) findViewById(R.id.editText_uhf_more_access);
		spinnerMembank = (Spinner) findViewById(R.id.Spinner_uhf_more_access);
		editAddr = (EditText) findViewById(R.id.editText_uhf_more_option_read_addr);
		editLen = (EditText) findViewById(R.id.editText_more_option_read_len);
		buttonRead = (Button) findViewById(R.id.button_more_option_read_data);
		buttonWrite = (Button) findViewById(R.id.button_more_option_write_data);
		editRead = (EditText) findViewById(R.id.editText_more_option_read_data);
		editWrite = (EditText) findViewById(R.id.editText_more_option_write_data);
		buttonReadTag = (Button) findViewById(R.id.button_read_tag_serialport_more);
		editTag = (EditText) findViewById(R.id.editText_tag_serialport_more);
		membanks = getResources().getStringArray(R.array.membanks);
		for (String membankItem : membanks) {
			listMembank.add(membankItem);
		}
		// 设置下拉
		spinnerMembank.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, listMembank));
	}

	private void listener() {
		// 读标签
		buttonReadTag.setOnClickListener(new OnReadTagListner());
		// 读标签数据
		buttonRead.setOnClickListener(this);
		// 写数据
		buttonWrite.setOnClickListener(this);

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
				// Log.e(TAG, membankStr + membank);
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapter) {

			}
		});
	}

	private List<InventoryInfo> listTag;

	// 读标签
	private class OnReadTagListner implements OnClickListener {
		@Override
		public void onClick(View arg0) {
			// 输入输出流不为null
			listTag = cmdManager.inventoryRealTime();
			if (listTag != null && !listTag.isEmpty()) {
				Util.play(1, 0);// 提示音
				String epcStr = Tools.Bytes2HexString(listTag.get(0).getEpc(),
						listTag.get(0).getEpc().length);
				editTag.setText(epcStr);
				Log.i(TAG, "epcStr = " + epcStr);
			}

		}
	}

	@Override
	protected void onDestroy() {
		listTag = null;
		super.onDestroy();
	}
	/*
	 * 读标签
	 */
	private byte[] readData(int membank, int address, int length,
			byte[] password) {
		String epcstr = editTag.getText().toString().trim();
		if(epcstr== null || "".equals(epcstr)){
			Toast.makeText(getApplicationContext(), "请先读取标签",
					Toast.LENGTH_SHORT).show();
			return null;
		}
		if(listTag.isEmpty() || listTag == null){
			Toast.makeText(getApplicationContext(), "请先读取标签",
					Toast.LENGTH_SHORT).show();
			return null;
		}
		//选择EPC
		cmdManager.selectEPC(listTag.get(0).getEpc());
		byte[] data = null;
		/**
		 * 超高频读写数据很容易读写失败，因此以读写三次来降低读写失败的次数
		 */
		for (int i = 0; i < 3; i++) {
			data = cmdManager.readFrom6C(membank, address, length, password);
			// 读数据成功，跳出循环
			if (data != null) {
				Util.play(1, 0);// 提示音
				Log.e(TAG, "data = " + Tools.Bytes2HexString(data, data.length));
				break;
			}
		}
		return data;
	}

	/*
	 * 写标签数据
	 */
	private boolean writeData(int membank, int address, byte[] password,
			byte[] data, int dataLen) {
		
		String epcstr = editTag.getText().toString().trim();
		if(epcstr== null || "".equals(epcstr)){
			Toast.makeText(getApplicationContext(), "请先读取标签",
					Toast.LENGTH_SHORT).show();
			return false;
		}
		boolean writeFlag = false;
		if(listTag.isEmpty() || listTag == null){
			Toast.makeText(getApplicationContext(), "请先读取标签",
					Toast.LENGTH_SHORT).show();
			return writeFlag;
		}
		//选择EPC
		cmdManager.selectEPC(listTag.get(0).getEpc());
		/**
		 * 超高频读写数据很容易读写失败，因此以读写三次来降低读写失败的次数
		 */
		for (int i = 0; i < 3; i++) {
			writeFlag = cmdManager.writeTo6C(password, membank, address,
					dataLen / 2, data);
			// 读数据成功，跳出循环
			if (writeFlag) {
				Util.play(1, 0);// 提示音
				Log.e(TAG, "write success!!");
				break;
			}
		}
		return writeFlag;
	}

	@Override
	public void onClick(View v) {
		String addrStr = editAddr.getText().toString();
		String lenStr = editLen.getText().toString();
		String accessStr = editAccess.getText().toString();
		String writeDataStr = editWrite.getText().toString();
		if (null == addrStr || "".equals(addrStr)) {
			Toast.makeText(getApplicationContext(), "起始地址不能为空",
					Toast.LENGTH_SHORT).show();
			return;
		}
		addr = Integer.valueOf(addrStr);
		if (null == accessStr || "".equals(accessStr)
				|| accessStr.length() != 8) {
			Toast.makeText(getApplicationContext(), "访问密码不能为空，且为4个字节",
					Toast.LENGTH_SHORT).show();
			return;
		}
		accessPassword = Tools.HexString2Bytes(accessStr);
		switch (v.getId()) {
		case R.id.button_more_option_read_data:// 读数据
			if (null == lenStr || "".equals(lenStr)) {
				Toast.makeText(getApplicationContext(), "数据长度不能为空",
						Toast.LENGTH_SHORT).show();
				return;
			}
			len = Integer.valueOf(lenStr);
			byte[] readData = readData(membank, addr, len, accessPassword);
			if (readData != null && readData.length > 1) {
				editRead.setText(Tools.Bytes2HexString(readData, readData.length));
				Toast.makeText(getApplicationContext(), "读数据成功",
						Toast.LENGTH_SHORT).show();
			} else if (readData != null){
				Toast.makeText(getApplicationContext(), "读数据失败,错误码" + readData[0],
						Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getApplicationContext(), "读数据失败" ,
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.button_more_option_write_data:// 写数据
			if (null == writeDataStr || "".equals(writeDataStr)) {
				Toast.makeText(getApplicationContext(), "写入数据不能为空",
						Toast.LENGTH_SHORT).show();
				return;
			}
			// 计算写入数据，若长度不为4的整数倍则用0补齐
			int writeLen = writeDataStr.length();
			int temp = writeLen % 4;
			if (temp != 0) {
				for (int i = 0; i < temp; i++) {
					writeDataStr += "0";
				}
			}
			byte[] writeBytes = Tools.HexString2Bytes(writeDataStr);
			//写数据
			boolean writeFlag = writeData(membank, addr, accessPassword,
					writeBytes, writeBytes.length);
			if (writeFlag) {
				Toast.makeText(getApplicationContext(), "写数据成功",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "写数据失败",
						Toast.LENGTH_SHORT).show();
			}
			break;

		default:
			break;
		}

	}

}
