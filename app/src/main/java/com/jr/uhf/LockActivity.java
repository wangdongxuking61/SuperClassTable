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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;

/**
 * 蓝牙操作锁定标签
 * 
 * @author Administrator
 * 
 */
public class LockActivity extends Activity {
	/* UI 控件 */
	private TextView textTitle; // 标题
	private CheckBox checkTag; // 是否勾选标签
	private Button buttonReadTag; // 读标签按钮
	private EditText editTag; // 标签号输入框
	private Spinner spinnerLockMembank; // 锁定数据区
	private EditText editAccess; // 密码
	private Spinner spinnerLockType; // 锁定类型
	private Button buttonLock; // 锁定

	private String[] lockMembanks;
	private String[] lockTypes;
	private List<String> listLockMembanks = new ArrayList<String>();
	private List<String> listLockType = new ArrayList<String>();

	private int lockMem;  //锁定区
	private int lockType; //锁定类型
	private byte[] accessBytes ;//访问密码
	private NewSendCommendManager cmdManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lock_tag);
		textTitle = (TextView) findViewById(R.id.textView_title);
		textTitle.setText("锁定标签");
		cmdManager = new NewSendCommendManager(
				ConnectedThread.getSocketInoutStream(),
				ConnectedThread.getSocketOutoutStream());
		lockMembanks = getResources().getStringArray(R.array.lock_membank);
		for (String membank : lockMembanks) {
			listLockMembanks.add(membank);
		}
		lockTypes = getResources().getStringArray(R.array.lock_type);
		for (String lockType : lockTypes) {
			listLockType.add(lockType);
		}
		// 初始化UI
		initUI();
		// 监听
		listener();
	}

	private void initUI() {
		checkTag = (CheckBox) findViewById(R.id.checkBox_lock_select);
		buttonReadTag = (Button) findViewById(R.id.button_lock_read_tag);
		editTag = (EditText) findViewById(R.id.editText_lock_tag);
		spinnerLockMembank = (Spinner) findViewById(R.id.spinner_lock_membank);
		editAccess = (EditText) findViewById(R.id.editText_lock_access_password);
		spinnerLockType = (Spinner) findViewById(R.id.spinner_lock_type);
		buttonLock = (Button) findViewById(R.id.button_lock);

		spinnerLockMembank.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, listLockMembanks));
		spinnerLockType.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, listLockType));

	}

	private String TAG = "LockActivity";
	
	private void listener() {
		
		//读标签
		buttonReadTag.setOnClickListener(new OnReadTagListner());
		//锁定区
		spinnerLockMembank.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				
				String mem = listLockMembanks.get(position);
				if("销毁密码".equals(mem)){
					lockMem = NewSendCommendManager.LOCK_MEM_KILL;
				}else if("访问密码".equals(mem)){
					lockMem = NewSendCommendManager.LOCK_MEM_ACCESS;
				}else if("EPC区".equals(mem)){
					lockMem = NewSendCommendManager.LOCK_MEM_EPC;
				}else if("TID区".equals(mem)){
					lockMem = NewSendCommendManager.LOCK_MEM_TID;
				}else if("USER区".equals(mem)){
					lockMem = NewSendCommendManager.LOCK_MEM_USER;
				}
				Log.e(TAG, lockMem + "");
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		//锁定类型
		spinnerLockType.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View view,
					int position, long id) {
				String type = listLockType.get(position);
				if("开放".equals(type)){
					lockType = NewSendCommendManager.LOCK_TYPE_OPEN;
				}else if("永久开放".equals(type)){
					lockType = NewSendCommendManager.LOCK_TYPE_PERMA_OPEN;
				}else if("锁定".equals(type)){
					lockType = NewSendCommendManager.LOCK_TYPE_LOCK;
				}else if("永久锁定".equals(type)){
					lockType = NewSendCommendManager.LOCK_TYPE_PERMA_LOCK;
				}
				Log.e(TAG, lockType + "");
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		//锁定标签
		buttonLock.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String accessStr = editAccess.getText().toString();
				String epc = editTag.getText().toString();
				if(accessStr == null && accessStr.length() != 8){
					Toast.makeText(getApplicationContext(), "访问密码不能为空,且为8位十六进制", 0).show();
					return;
				}
				if(epc == null && "".equals(epc) && listTag.isEmpty()){
					Toast.makeText(getApplicationContext(), "请先选择标签", 0).show();
					return;
				}
				accessBytes = Tools.HexString2Bytes(accessStr);
				
				//先选择EPC
				cmdManager.selectEPC(listTag.get(0).getEpc());
				//锁定
				boolean lockFlag = cmdManager.lock6C(accessBytes, lockMem, lockType);
				if(lockFlag){
					Toast.makeText(getApplicationContext(), "锁定成功", 0).show();
				}else{
					Toast.makeText(getApplicationContext(), "锁定失败", 0).show();
				}
			}
		});
	}
	
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
//			if(is != null && os != null){
				listTag = cmdManager.inventoryRealTime();
				if(listTag != null && !listTag.isEmpty()){
					String epcStr = Tools.Bytes2HexString(listTag.get(0).getEpc(), listTag.get(0).getEpc().length);
					editTag.setText(epcStr);
					Log.e(TAG, "epcStr = " + epcStr);
//				}
			}
			
		}
	}
}
