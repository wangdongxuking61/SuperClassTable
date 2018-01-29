package com.jr.uhf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

/**
 * 串口设置界面
 * 
 * @author Administrator
 * 
 */
public class SerialportSettingActivity extends Activity {

	/* UI控件 */
	private TextView textTitle ;
	private Spinner spinnerCom;
	private Spinner spinnerBaudrate;
	private EditText editCom;
	private EditText editConnectFlag;
	private EditText editOutputPower;
	private EditText editWorkFreq;
	private Button buttonConnectDevice;
	private Button buttonInto ;

	private String[] comDevices; // 串口文件列表
	private List<String> comPath = new ArrayList<String>();//串口路径列表
	private String[] baudrateStrs; // 波特率数组
	private List<String> baudrateList = new ArrayList<String>(); // 波特率列表

	private SerialPortFinder mSerialportFinder;   //加载设备串口
	private SerialPort comSerialport ;	//串口
	public static InputStream is ;
	public static OutputStream os ;
	private NewSendCommendManager manager ;
	
	private String serialPath ;  //串口路径
	private int baudrate ;
	private final String TAG = "SerialportSettingActivity";
	
	private boolean connFlag = false ; //连接状态
	
	/** 测试数据 **/
	private com.BRMicro.SerialPort switchSerialport ;  //H901串口转换

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// 获取设备串口列表
		setContentView(R.layout.activity_serialport_setting);
		textTitle = (TextView) findViewById(R.id.textView_title);
		textTitle.setText(R.string.serialport_setting);
		//获取串口路径
		this.getSerialportDervicePath();
		// 初始UI
		this.initUI();
//		//测试
//		test();
		//监听
		listener();
	}
	
	//在H901上调试调试方法
//	private void test(){
//		try {
//			comSerialport = new SerialPort(new File("/dev/ttyMT1"), 115200, 0);
//			//切换到13
//			switchSerialport = new com.BRMicro.SerialPort();
//			switchSerialport.switch2channel(13);
//			//打开电源
////			switchSerialport.rfidPoweron();
//		} catch (Exception e) {
////			e.printStackTrace();
//			Toast.makeText(getApplicationContext(), "串口打开失败", 0).show();
//			return ;
//			
//		} 
//
//		
//		if(comSerialport == null){
//			return;
//		}
//		is = comSerialport.getInputStream();
//		os = comSerialport.getOutputStream();
//		
//		manager = new NewSendCommendManager(is, os);
//		
//	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(switchSerialport != null){
			switchSerialport.rfidPoweron();
			
		}
		super.onDestroy();
	}

	// 获取串口列表
	private void getSerialportDervicePath() {
		mSerialportFinder = new SerialPortFinder();
		comDevices = mSerialportFinder.getAllDevices();
		if (comDevices != null && comDevices.length > 0) {
//			comPath = new ArrayList<String>();
			for (int i = 0; i < comDevices.length; i++) {
				comPath.add("/dev/" + comDevices[i].split(" ")[0]);
				Log.e("", comPath.get(i));
			}
		} else {
			Toast.makeText(getApplicationContext(), "获取串口列表失败", 0).show();
			return;
		}
	}

	// 初始UI
	private void initUI() {
		spinnerCom = (Spinner) findViewById(R.id.spinner_com_id);
		spinnerBaudrate = (Spinner) findViewById(R.id.spinner_baudrate);
		editCom = (EditText) findViewById(R.id.editText_com_id);
		editConnectFlag = (EditText) findViewById(R.id.editText_connected_flag);
		editOutputPower = (EditText) findViewById(R.id.editText_output_power);
		editWorkFreq = (EditText) findViewById(R.id.editText_work_freq);
		buttonConnectDevice = (Button) findViewById(R.id.button_connect);
		buttonInto = (Button) findViewById(R.id.button_into_option_reader);
		
		setButtonFlag(buttonInto, connFlag);

		spinnerCom.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, comPath));
		
		baudrateStrs = getResources().getStringArray(R.array.baudrate);
		for(String baudrate : baudrateStrs){
			baudrateList.add(baudrate);
		}
		spinnerBaudrate.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, baudrateList));
	}
	
	/**
	 * 监听
	 */
	private void listener(){
		//串口选择
		spinnerCom.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				serialPath = comPath.get(arg2);
				editCom.setText(serialPath);
				Log.e(TAG, "serialPath = " + serialPath);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		//波特率选择
		spinnerBaudrate.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				baudrate = Integer.valueOf(baudrateList.get(arg2));
				Log.e(TAG, "baudrate = " + baudrateList.get(arg2));
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		//连接设备按钮监听
		buttonConnectDevice.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//1.连接串口
				//2.读取基本配置
				//3.改变基本状态
//				byte[] version = manager.getFirmware();
//				if(version != null){
//					Toast.makeText(getApplicationContext(), "串口打开成功", 0).show();
//					editConnectFlag.setText("已连接");
//					setButtonFlag(buttonConnectDevice, false);
//					setButtonFlag(buttonInto, true);
//				}
//				Log.e("", manager.getOuputPower() + "");
				//初始化串口成功
				if(initSerialport()){
					// 读取版本号，测试模块是否连接
					byte[] version = manager.getFirmware();
					if(version == null){
					Toast.makeText(getApplicationContext(), "设备打开失败", 0).show();
					return ;
				}
					editConnectFlag.setText("已连接");
					setButtonFlag(buttonConnectDevice, false);
					setButtonFlag(buttonInto, true);
				}else{
					Toast.makeText(getApplicationContext(), "串口初始化失败", 0).show();
				}
				
			}
		});
		//
		buttonInto.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent toUHF = new Intent(SerialportSettingActivity.this, SerialportUHFactivity.class);
				toUHF.putExtra("mode", "serialport");
				startActivity(toUHF);
			}
		});
	}
	//连接串口
	private boolean initSerialport(){
		try {
			comSerialport = new SerialPort(new File(serialPath), baudrate, 0);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "串口打开失败", 0).show();
			return false;
			
		} 
		
		//切换到13
//		switchSerialport = new com.BRMicro.SerialPort();
//		switchSerialport.switch2channel(13);
		
		is = comSerialport.getInputStream();
		os = comSerialport.getOutputStream();
		
		manager = new NewSendCommendManager(is, os);
		return true;
	}
	//设置按钮不可用
	private void setButtonFlag(Button button, boolean flag){
		button.setClickable(flag);
		if(flag){
			button.setTextColor(getResources().getColor(R.color.black));
		}else{
			button.setTextColor(getResources().getColor(R.color.gray));
		}
	}
	
	//读取基本状态信息
	private void readBasicInfo(){
		int outputPower = manager.getOuputPower();
		if(outputPower > 0){
			connFlag = true;
			//设备连接成功
			editOutputPower.setText(outputPower + "Bdm");
			editConnectFlag.setText(R.string.connected);
			Toast.makeText(getApplicationContext(), "设备连接成功", 0).show();
		}else{
			connFlag = false;
			//设备连接成功
			editOutputPower.setText("");
			editConnectFlag.setText(R.string.disconnect);
			Toast.makeText(getApplicationContext(), "未能连接设备", 0).show();
		}
		setButtonFlag(buttonInto, connFlag);
	}
	
}
