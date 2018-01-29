package com.jr.uhf;

import java.util.ArrayList;
import java.util.List;

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.InventoryTagActivity.ButtonreadtagListner;
import com.jr.uhf.command.NewSendCommendManager;

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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingActivity extends Activity {
	private Spinner outputSpinner;
	private Spinner workareaSpinner;
	private Button buttonSetOutput;
	private Button buttonSetWork;
	private TextView textTitle;
	
	private EditText editOutput ;
	private Button buttonReadOutput ;

	private NewSendCommendManager cmdManager;

	private String[] outputStrings = {"27dBm", "26dBm", "25dBm", "24dBm", "23dBm",
			"22dBm", "21dBm", "20dBm", "19dBm", "18dBm", "17dBm", "16dBm", "15dBm",
			"14dBm" , "13dBm"};
	private String[] workAreaStrings;
	private List<String> listOutput = new ArrayList<String>();
	private List<String> listWorkarea = new ArrayList<String>();
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

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_para_activity);
		textTitle = (TextView) findViewById(R.id.textView_title);
		textTitle.setText("模块参数设置");
		cmdManager = new NewSendCommendManager(ConnectedThread.getSocketInoutStream(),
				ConnectedThread.getSocketOutoutStream());
		// 初始UI控件
		initView();
		
		//监听
		listener();
	}

	private void initView() {
		outputSpinner = (Spinner) findViewById(R.id.spinner_setting_output);
		workareaSpinner = (Spinner) findViewById(R.id.spinner_set_work_area);
		buttonSetOutput = (Button) findViewById(R.id.button_set_output);
		buttonSetWork = (Button) findViewById(R.id.button_set_work_area);
		editOutput = (EditText) findViewById(R.id.editText_setting_output);
		buttonReadOutput = (Button) findViewById(R.id.button_read_output);
		
		workAreaStrings = getResources().getStringArray(R.array.work_area);
		for (String area : workAreaStrings) {
			listWorkarea.add(area);
		}
		for (String output : outputStrings) {
			listOutput.add(output);
		}
		outputSpinner.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, listOutput));
		workareaSpinner.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, listWorkarea));
	}
	
	private int outputValue = 2700;
	private int area = 0;
	//监听
	private void listener(){
		//功率设置
		buttonSetOutput.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(cmdManager.setOutputPower(outputValue)){
					Toast.makeText(getApplicationContext(), "设置成功", 0).show();
				}else{
					Toast.makeText(getApplicationContext(), "设置失败", 0).show();
				}
				
			}
		});
		//工作地区设置
		buttonSetWork.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(cmdManager.setWorkArea(area) == 0){
					Toast.makeText(getApplicationContext(), "设置成功", 0).show();
				}else{
					Toast.makeText(getApplicationContext(), "设置失败", 0).show();
				}
				
			}
		});
		//读取功率
		buttonReadOutput.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int value = cmdManager.getOuputPower();
				if(value > 0){
					editOutput.setText(value + "dBm");
				}
				
			}
		});
		
		outputSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				outputValue = 2700 - (position*100);
				Log.e(TAG, outputValue + "");
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
				
			}
		});
		
		workareaSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				String mArea = listWorkarea.get(position);
				if("中国1".equals(mArea)){
					area = 1;
				}else if("中国2".equals(mArea)){
					area = 4;
				}else if("美国".equals(mArea)){
					area = 2;
				}else if("欧洲".equals(mArea)){
					area = 3;
				}else if("韩国".equals(mArea)){
					area = 6;
				}
				Log.e(TAG, area + "");
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private String TAG = "SettingActivity";
}
