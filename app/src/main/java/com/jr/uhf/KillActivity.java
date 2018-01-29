package com.jr.uhf;

import java.util.List;

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.InventoryTagActivity.ButtonreadtagListner;
import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class KillActivity extends Activity {

	private TextView textTitle;
	private EditText editTag;
	private Button buttonReadTag;
	private EditText editKillPassword;
	private Button buttonKill;

	private NewSendCommendManager cmdManager;
	private String TAG = "KillActivity";

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
		setContentView(R.layout.activity_kill_tag);
		textTitle = (TextView) findViewById(R.id.textView_title);
		textTitle.setText("销毁标签");
		// 初始UI
		initUI();
		ConnectedThread.setHandler(mHandler);
		cmdManager = new NewSendCommendManager(
				ConnectedThread.getSocketInoutStream(),
				ConnectedThread.getSocketOutoutStream());
		// 监听
		listener();
	}

	private void initUI() {
		editTag = (EditText) findViewById(R.id.editText_kill_tag);
		buttonReadTag = (Button) findViewById(R.id.button_kill_read_tag);
		editKillPassword = (EditText) findViewById(R.id.editText_kill_access_password);
		buttonKill = (Button) findViewById(R.id.button_kill);
	}

	private void listener() {
		buttonReadTag.setOnClickListener(new OnReadTagListner());
		buttonKill.setOnClickListener(new onKillTagListener());
	}

	private byte[] password ;
	//销毁标签
	private class onKillTagListener implements OnClickListener{

		@Override
		public void onClick(View arg0) {
			String accessStr = editKillPassword.getText().toString();
			String epc = editTag.getText().toString();
			if(accessStr == null && accessStr.length() != 8){
				Toast.makeText(getApplicationContext(), "访问密码不能为空,且为8位十六进制", 0).show();
				return;
			}
			if(epc == null && "".equals(epc) && listTag.isEmpty()){
				Toast.makeText(getApplicationContext(), "请先选择标签", 0).show();
				return;
			}
			password = Tools.HexString2Bytes(accessStr);
			
			//先选择EPC
			cmdManager.selectEPC(listTag.get(0).getEpc());
			//销毁
			boolean lockFlag = cmdManager.killTag(password);
			if(lockFlag){
				Toast.makeText(getApplicationContext(), "销毁标签成功", 0).show();
			}else{
				Toast.makeText(getApplicationContext(), "销毁标签失败", 0).show();
			}
			
		}
		
	}
	
	private List<InventoryInfo> listTag;

	// 读标签
	private class OnReadTagListner implements OnClickListener {
		@Override
		public void onClick(View arg0) {

			// 蓝牙连接断开
			if (!BluetoothActivity.connFlag) {
				return;
			}
			// 输入输出流不为null
			// if(is != null && os != null){
			listTag = cmdManager.inventoryRealTime();
			if (listTag != null && !listTag.isEmpty()) {
				String epcStr = Tools.Bytes2HexString(listTag.get(0).getEpc(),
						listTag.get(0).getEpc().length);
				editTag.setText(epcStr);
				Log.e(TAG, "epcStr = " + epcStr);
				// }
			}

		}
	}

}
