package com.jr.uhf;

import java.util.ArrayList;
import java.util.List;

import com.jr.uhf.command.NewSendCommendManager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SerialportUHFsettingActivity extends Activity {
	private Spinner outputSpinner;
	private Spinner workareaSpinner;
	private Button buttonSetOutput;
	private Button buttonSetWork;
	private TextView textTitle;
	private EditText editOutput;
	private Button buttonReadOutput;

	private NewSendCommendManager cmdManager = new NewSendCommendManager(
			SerialportSettingActivity.is, SerialportSettingActivity.os);

	private String[] outputStrings = { "27dBm","26dBm", "25dBm", "24dBm", "23dBm",
			"22dBm", "21dBm", "20dBm", "19dBm", "18dBm", "17dBm", "16dBm" , "15dBm", "14dBm", "13dBm"};
	private String[] workAreaStrings;
	private List<String> listOutput = new ArrayList<String>();
	private List<String> listWorkarea = new ArrayList<String>();

	private String TAG = "SerialportUHFsettingActivity";
	private String mode ;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_para_activity);
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
		// 初始UI控件
		initView();

		// 监听
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

	// 监听
	private void listener() {
		// 功率设置
		buttonSetOutput.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (cmdManager.setOutputPower(outputValue)) {
					Toast.makeText(getApplicationContext(), "设置成功", 0).show();
				} else {
					Toast.makeText(getApplicationContext(), "设置失败", 0).show();
				}

			}
		});
		// 工作地区设置
		buttonSetWork.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (cmdManager.setWorkArea(area) == 0) {
					Toast.makeText(getApplicationContext(), "设置成功", 0).show();
				} else {
					Toast.makeText(getApplicationContext(), "设置失败", 0).show();
				}

			}
		});
		// 读取功率
		buttonReadOutput.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int value = cmdManager.getOuputPower();
				if (value > 0) {
					editOutput.setText(value + "dBm");
				}

			}
		});

		outputSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				outputValue = 2700 - (position * 100);
				Log.e(TAG, outputValue + "");

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
		outputSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				outputValue = 2700 - position * 100;

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});

		workareaSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
	}
}
