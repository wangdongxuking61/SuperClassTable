package com.jr.uhf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.command.CommandThread;
import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;
import com.jr.uhf.entity.EPC;
import com.jr.uhf.util.Util;

import org.androidschedule.MainActivityClass;

public class InventoryTagActivity extends Activity {

    /* UI控件 */
    private EditText editCountTag;
    private Button buttonClear;
    private Button buttonreadTag;
    private RadioGroup radioGroup;
    private RadioButton rbSingle;
    private RadioButton rbLoop;
    private ListView listViewTag;
    private TextView textTitle;

    // 超高频指令管理者
    private NewSendCommendManager manager;

    private MyActivityManager activityManager;
    // 蓝牙连接输入输出流
    private InputStream is;
    private OutputStream os;

    private boolean isSingleRead = false;

    private String TAG = "InventoryTagActivity";

    private CommandThread commthread;
    public static final int READ_TAG = 2001;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            Log.i(TAG, msg.getData().getString("str"));
            switch (msg.what) {
                case BluetoothActivity.CONNECT_INTERRUPT:// 连接中断
                    BluetoothActivity.connFlag = false;
                    Toast.makeText(getApplicationContext(), "连接中断",
                            Toast.LENGTH_SHORT).show();
                    break;
                case InventoryTagActivity.READ_TAG:// 读标签数据

                    break;
                default:
                    break;
            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventorytag_activity);
        textTitle = (TextView) findViewById(R.id.textView_title);
        textTitle.setText(R.string.inventory_tag);
        textTitle.append("--已连接");
        activityManager = (MyActivityManager) getApplication();
        activityManager.addActivity(this);
        //设置消息监听
        ConnectedThread.setHandler(mHandler);
        // 获取UI控件
        this.initUI();
        // 监听
        this.listner();
        // 获取蓝牙输入输出流
        is = ConnectedThread.getSocketInoutStream();
        os = ConnectedThread.getSocketOutoutStream();
        //
        manager = new NewSendCommendManager(is, os);
        // 开启线程
//		new Thread(new LoopReadThread()).start();
        new RecvThread().start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new SendCmdThread().start();
        //初始化声音池
        Util.initSoundPool(this);
    }

    // 获取UI控件
    private void initUI() {
        editCountTag = (EditText) findViewById(R.id.editText_tag_count);
        buttonClear = (Button) findViewById(R.id.button_clear_data);
        buttonreadTag = (Button) findViewById(R.id.button_inventory);
        radioGroup = (RadioGroup) findViewById(R.id.RgInventory);
        rbSingle = (RadioButton) findViewById(R.id.RbInventorySingle);
        rbLoop = (RadioButton) findViewById(R.id.RbInventoryLoop);
        listViewTag = (ListView) findViewById(R.id.listView_tag);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isRuning = false;
            if (commthread != null) {
                commthread.interrupt();
            }
            isRecv = false;
            isSend = false;
            isRuning = false;
            isRunning = false;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 监听UI
    private void listner() {
        rbSingle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    isSingleRead = true;
                    isSend = false;
                    isRecv = false;
                    buttonreadTag.setText("读标签");
                    Log.i(TAG, "isSingle --- >" + isSingleRead);
                }
            }
        });
        rbLoop.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    isSingleRead = false;

                    Log.i(TAG, "isSingle --- >" + isSingleRead);
                }
            }
        });
        // 读标签
        buttonreadTag.setOnClickListener(new ButtonreadtagListner());

        //清空
        buttonClear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                editCountTag.setText("");
                listEPC.removeAll(listEPC);
                listViewTag.setAdapter(null);
            }
        });

    }

    List<InventoryInfo> listTag;//单次读标签返回的数据
    boolean isRuning = false;

    // 读标签
    class ButtonreadtagListner implements OnClickListener {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "buttonreadTag click----");
            //单次
            if (isSingleRead) {
                listTag = manager.inventoryRealTime();
                if (listTag != null && !listTag.isEmpty()) {
                    for (InventoryInfo epc : listTag) {
//						String epcStr = Tools.Bytes2HexString(epc, epc.length);
                        addToList(listEPC, epc);
                    }
                }
            } else {
                //循环
                if (isSend) {
//					isRuning = false;
                    isSend = false;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    isRecv = false;
                    buttonreadTag.setText("读标签");
                } else {
//					isRuning = true;.
                    isSend = true;
                    isRecv = true;
                    buttonreadTag.setText("停止");
                }
            }
        }
    }

    boolean isRunning = true;  //控制发送接收线程
    boolean isSend = false;  //控制发送指令

    //发送盘存指令
    private class SendCmdThread extends Thread {
        @Override
        public void run() {
            //盘存指令
            byte[] cmd = {(byte) 0xAA, (byte) 0x00, (byte) 0x22, (byte) 0x00,
                    (byte) 0x00, (byte) 0x22, (byte) 0x8E};
            while (isRunning) {
                if (isSend) {
                    try {
                        ConnectedThread.getSocketOutoutStream().write(cmd);
                    } catch (IOException e) {
                        isSend = false;
                        isRunning = false;
                        Log.e(TAG, "Socket 连接出错" + e.toString());
                    }
                    try {
                        Thread.sleep(50);
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
    private class RecvThread extends Thread {
        @Override
        public void run() {
            InputStream is = ConnectedThread.getSocketInoutStream();
            int size = 0;
            byte[] buffer = new byte[256];
            byte[] temp = new byte[512];
            int index = 0;  //temp有效数据指向
            int count = 0;  //temp有效数据长度
            while (isRunning) {
                if (isRecv) {
                    try {
                        Thread.sleep(30);
                        size = is.read(buffer);
                        if (size > 0) {
                            count += size;
                            //超出temp长度清空
                            if (count > 512) {
                                count = 0;
                                Arrays.fill(temp, (byte) 0x00);
                            }
                            //先将接收到的数据拷到temp中
                            System.arraycopy(buffer, 0, temp, index, size);
                            index = index + size;
                            if (count > 7) {
//								Log.e(TAG, "temp: " + Tools.Bytes2HexString(temp, count));
                                //判断AA022200
                                if ((temp[0] == (byte) 0xAA) && (temp[1] == (byte) 0x02) && (temp[2] == (byte) 0x22) && (temp[3] == (byte) 0x00)) {
                                    //正确数据位长度等于RSSI（1个字节）+PC（2个字节）+EPC
                                    int len = temp[4] & 0xff;
                                    if (count < len + 7) {//数据区尚未接收完整
                                        continue;
                                    }
                                    if (temp[len + 6] != (byte) 0x8E) {//数据区尚未接收完整
                                        continue;
                                    }
                                    //得到完整数据包
                                    byte[] packageBytes = new byte[len + 7];
                                    System.arraycopy(temp, 0, packageBytes, 0, len + 7);
//									Log.e(TAG, "packageBytes: " + Tools.Bytes2HexString(packageBytes, packageBytes.length));
                                    //校验数据包
                                    byte crc = checkSum(packageBytes);
                                    InventoryInfo info = new InventoryInfo();
                                    if (crc == packageBytes[len + 5]) {
                                        //RSSI
                                        info.setRssi(temp[5]);
                                        //PC
                                        info.setPc(new byte[]{temp[6], temp[7]});
                                        //EPC
                                        byte[] epcBytes = new byte[len - 5];
                                        System.arraycopy(packageBytes, 8, epcBytes, 0, len - 5);
                                        info.setEpc(epcBytes);
//                                        Util.play(1, 0);//播放提示音
                                        addToList(listEPC, info);
                                    }
                                    count = 0;
                                    index = 0;
                                    Arrays.fill(temp, (byte) 0x00);
                                } else {
                                    //包错误清空
                                    count = 0;
                                    index = 0;
                                    Arrays.fill(temp, (byte) 0x00);
                                }
                            }

                        }


                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        isRunning = false;
                        Log.e(TAG, "Socket 连接出错" + e.toString());
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

    boolean isStop = false;
    List<EPC> listEPC = new ArrayList<EPC>(); //EPC列表
    // 循环读标签
//	private class LoopReadThread implements Runnable {
//
//		@Override
//		public void run() {
//			List<InventoryInfo> mList = new ArrayList<InventoryInfo>();
//			while(!isStop){
//				while (!isSingleRead && isRuning) {
//					if (BluetoothActivity.connFlag) {
//						Log.i(TAG, "loopread ... ");
//						mList = manager.inventoryRealTime();
//						if(mList != null && !mList.isEmpty()){
//							for(InventoryInfo info : mList){
////								String epcStr = Tools.Bytes2HexString(epc, epc.length);
//								Util.play(1, 0);//播放提示音
//								addToList(listEPC, info);
//							}
//						}
//						try {
//							Thread.sleep(50);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//
//		}
//
//	}


    List<Map<String, Object>> listMap;

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
                for (EPC epcdata : list) {
                    Map<String, Object> map = new HashMap<String, Object>();

                    map.put("EPC", epcdata.getEpc());
                    map.put("PC", epcdata.getPc() + "");
//					map.put("LEN", epcdata.getEpc().length());
                    map.put("RSSI", epcdata.getRssi() + "dBm");
                    map.put("COUNT", epcdata.getCount());
                    idcount++;
                    listMap.add(map);
                }
                editCountTag.setText("" + listEPC.size());
                listViewTag.setAdapter(new SimpleAdapter(InventoryTagActivity.this,
                        listMap, R.layout.list_epc_item, new String[]{
                        "EPC", "PC", "RSSI", "COUNT"}, new int[]{
                        R.id.textView_item_epc, R.id.textView_item_pc,
                        R.id.textView_item_rssi, R.id.textView_item_count}));
            }
        });
    }
}
