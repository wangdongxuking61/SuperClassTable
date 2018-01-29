package com.jr.uhf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;
import com.jr.usb.FT311UARTInterface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class UsbSettingActivity extends Activity {
	
	private Spinner spinnerBaud ;
	private Spinner spinnerDataBit ;
	private Spinner spinnerStopBit ;
	private Spinner spinnerParity ;
	private Spinner spinnerFlow ;
	private Button buttonSetting ;
	private Button buttonToUhf ;
	
	private String[] bauds ;
	private String[] dataBits ;
	private String[] stopBits ;
	private String[] paritys ;
	private String[] flows ;
	
	private static FT311UARTInterface usbInterface ;
	
	private static FileInputStream in ;
	private static FileOutputStream os ;
	
	int baudRate = 115200; /* baud rate */
	byte stopBit = 1; /* 1:1stop bits, 2:2 stop bits */
	byte dataBit = 8; /* 8:8bit, 7: 7bit */
	byte parity = 0; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
	byte flowControl = 0; /* 0:none, 1: flow control(CTS,RTS) */
	
	private int usbSt ;  //是否连上USB,0为连上USB模块，2为没有发现USB设备
	
//	private List<String>
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_usb_setting);
		TextView title = (TextView) findViewById(R.id.textView_title);
		title.setText("USB参数设置");
		usbInterface = new FT311UARTInterface(this);
		initView();
		listner();
		buttonToUhf.setTextColor(getResources().getColor(R.color.gray));
		buttonToUhf.setClickable(false);
	}

	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		usbSt = usbInterface.ResumeAccessory();
		if(usbSt == 2){
			Toast.makeText(getApplicationContext(), "未发现USB设备", 0).show();
			buttonSetting.setClickable(false);
			buttonSetting.setTextColor(getResources().getColor(R.color.gray));
		}else{
			buttonSetting.setClickable(true);
			buttonSetting.setTextColor(getResources().getColor(R.color.black));	
		}

		Log.e("usbST", " " + usbSt);
	}
	
	private boolean bConfiged = false ;
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
//		flag = false ;
		usbInterface.DestroyAccessory(bConfiged);
		usbInterface = null ;
	}
	
	
	private void initView() {
		spinnerBaud = (Spinner) findViewById(R.id.spinner_usb_baud);
		spinnerDataBit = (Spinner) findViewById(R.id.spinner_usb_data);
		spinnerStopBit = (Spinner) findViewById(R.id.spinner_usb_stop);
		spinnerParity = (Spinner) findViewById(R.id.spinner_usb_parity);
		spinnerFlow = (Spinner) findViewById(R.id.spinner_usb_flow);
		buttonSetting = (Button) findViewById(R.id.button_usb_setting);
		spinnerBaud = (Spinner) findViewById(R.id.spinner_usb_baud);
		buttonToUhf = (Button) findViewById(R.id.button_to_usbuhf);

		
		
		bauds = getResources().getStringArray(R.array.baudrate);
		dataBits = getResources().getStringArray(R.array.data_bits);
		stopBits = getResources().getStringArray(R.array.stop_bits);
		paritys = getResources().getStringArray(R.array.parity);
		flows = getResources().getStringArray(R.array.flow_control);
		spinnerBaud.setAdapter(new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, bauds));
		spinnerDataBit.setAdapter(new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, dataBits));
		spinnerStopBit.setAdapter(new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, stopBits));
		spinnerParity.setAdapter(new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, paritys));
		spinnerFlow.setAdapter(new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, flows));
		//设置默认配置
		spinnerBaud.setSelection(3);
		spinnerDataBit.setSelection(1);
		
	}
	

	
	private void listner(){
		spinnerBaud.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {

				baudRate = Integer.parseInt(parent.getItemAtPosition(pos).toString());
			}

			public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
			}
		});
		
		spinnerStopBit.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {

				stopBit = (byte) Integer.parseInt(parent.getItemAtPosition(pos).toString());
			}

			public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
			}
		});
		spinnerDataBit.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {

				dataBit = (byte) Integer.parseInt(parent.getItemAtPosition(pos).toString());
			}

			public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
			}
		});
		spinnerParity.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,
					long id) {

				String parityString = new String(parent.getItemAtPosition(pos).toString());
				if (parityString.compareTo("None") == 0) {
					parity = 0;
				}

				if (parityString.compareTo("Odd") == 0) {
					parity = 1;
				}

				if (parityString.compareTo("Even") == 0) {
					parity = 2;
				}

				if (parityString.compareTo("Mark") == 0) {
					parity = 3;
				}

				if (parityString.compareTo("Space") == 0) {
					parity = 4;
				}
			}

			public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
			}
		});
		spinnerFlow.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

				String flowString = new String(parent.getItemAtPosition(pos).toString());
				if (flowString.compareTo("None") == 0) {
					flowControl = 0;
				}

				if (flowString.compareTo("CTS/RTS") == 0) {
					flowControl = 1;
				}
			}

			public void onNothingSelected(AdapterView<?> parent) { // Do nothing. }}
			}
		});
		//设置USB参数
		buttonSetting.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				usbInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
				bConfiged = true ;
				if(usbInterface.inputstream != null && usbInterface.outputstream != null){
					Toast.makeText(getApplicationContext(), "参数设置成功", 0).show();
					buttonToUhf.setTextColor(getResources().getColor(R.color.black));
					buttonToUhf.setClickable(true);
//					NewSendCommendManager manager = new NewSendCommendManager(UsbSettingActivity.getInputStream(), UsbSettingActivity.getOutputStream());
//					
//					manager.inventoryRealTime();
//					new Runn().start();
//					new Runn().start();
					
				}
			}
		});
		
		buttonToUhf.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(UsbSettingActivity.this, SerialportUHFactivity.class);
				intent.putExtra("mode", "usb");
				startActivity(intent);
//				byte[] cmd = {(byte)0xAA, (byte)0x00, (byte)0x22, (byte)0x00, (byte)0x00, (byte)0x22, (byte)0x8e};
//				
//				try {
//					if(usbInterface.outputstream != null)
//					{
//						usbInterface.outputstream.write(cmd);
//						usbInterface.outputstream.flush();
//					}
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
				
				
			}
		});
	}
	
//	boolean flag = true;
//	
//	class Runn extends Thread{
//		
//		public Runn(){
//			this.setPriority(Thread.MAX_PRIORITY);
//		}
//		
//		@Override
//		public void run() {
//			
//			byte[] buffer = new byte[128];
//			try{
//				
//				
////				Thread.sleep(2000);
////				usbInterface.inputstream.available();
//				int count = 5;
//				while(flag){
//					if(usbInterface.inputstream != null){
//						Thread.sleep(50);
//						int size = usbInterface.inputstream.read(buffer, 0,128);
//						if(size > 0){
//							Log.e("", Tools.Bytes2HexString(buffer, size));
//						}
//					}
//					Thread.sleep(200);
//				}
//			}catch(Exception e){
//				e.printStackTrace();
//			}
//			
//		}
//	}
	
	
	public static FileInputStream getInputStream(){
		return usbInterface.inputstream ;
	}
	
	public static FileOutputStream getOutputStream(){
		return usbInterface.outputstream ;
	}
	

}
