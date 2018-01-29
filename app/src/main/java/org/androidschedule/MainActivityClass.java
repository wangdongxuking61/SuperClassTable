package org.androidschedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.jr.bluetooth.ConnectedThread;
import com.jr.uhf.BluetoothActivity;
import com.jr.uhf.InventoryTagActivity;
import com.jr.uhf.MyActivityManager;
import com.jr.uhf.R;
import com.jr.uhf.command.CommandThread;
import com.jr.uhf.command.InventoryInfo;
import com.jr.uhf.command.NewSendCommendManager;
import com.jr.uhf.command.Tools;
import com.jr.uhf.entity.EPC;
import com.jr.uhf.util.Util;

import org.editschedule.SetActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import temp.DataBase;
import temp.ShareMethod;
import temp.MyApplication;
import temp.MyDialog;

public class MainActivityClass extends Activity {

    //识别标签设置start
    /* UI控件 */
//    private TextView textTitle;
    private Button buttonClear;
    private Button buttonreadTag;

    private TextView classText;
    private TextView existText;
    private TextView lackText;
//    private RadioButton rbLoop;
//    private EditText editCountTag;
//    private RadioButton rbSingle;
//    private RadioGroup radioGroup;
//    private ListView listViewTag;

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
    //识别标签设置end

    public ListView list[] = new ListView[7];
    private TabHost tabs = null;
    private TextView exitButton = null;
    private TextView setButton = null;
    public static DataBase db;
    public Cursor[] cursor = new Cursor[7];
    public SimpleCursorAdapter adapter;
    private SharedPreferences pre;

    //定义手势检测器实例
    private GestureDetector detector = null;
    //定义手势动作两点之间的最小距离
    private final int FLIP_DISTANCE = 200;
    private TextView buttonBluetooth;

    HashMap<String, String> epc2class = new HashMap<String, String>();
    ArrayList<String> allTadayClass = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        epc2class.put("E20020756015018117305F11", "语文");
        epc2class.put("E2000016990F01351160A6EE", "数学");
        epc2class.put("E20020756015017817305F1D", "英语");
        epc2class.put("E20020756015017917305F19", "音乐");
        epc2class.put("E20020756015018017305F15", "美术");
        epc2class.put("E20020756015018217305F0D", "自然");

        //识别标签设置start
        setContentView(R.layout.activity_main);
//        setContentView(R.layout.inventorytag_activity);

//        textTitle = (TextView) findViewById(R.id.textView_title);
//        textTitle.setText(R.string.inventory_tag);
//        textTitle.append("--已连接");
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

        // 课程表
        //将该activity加入到MyApplication对象实例容器中
        MyApplication.getInstance().addActivity(this);

        db = new DataBase(MainActivityClass.this);
        pre = getSharedPreferences("firstStart", Context.MODE_PRIVATE);

        //判断程序是否第一次运行，如果是创建数据库表
        if (pre.getBoolean("firstStart", true)) {
            SingleInstance.createTable();
            (pre.edit()).putBoolean("firstStart", false).commit();
//			finish();
        }

        set_exitButton();
        set_setButton();
        set_list_tabs();
        set_list_view();


        //设置Tab变换时的监听事件
        tabs.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                set_class_data();
            }
        });

        set_class_data();
    }


    public void set_class_data() {
        //从数据库取出旧数据
        int day = tabs.getCurrentTab();

        String classStr = "今日课程：";
        allTadayClass.clear();
        boolean first = true;
        for (int i = 0; i < 12; i++) {
            cursor[day].moveToPosition(i);
            String[] temp = new String[8];
            for (int j = 0; j < 8; j++) {
                temp[j] = cursor[day].getString(j + 1);
            }

            if (!temp[0].equals("")) {
                if (first)
                    first = false;
                else
                    classStr += "，";
                String tmpClass = temp[0].substring(temp[0].indexOf(":") + 2);
                classStr += tmpClass;
                allTadayClass.add(tmpClass);
            }
        }
        classText.setText(classStr);
    }


    // 获取UI控件
    private void initUI() {
//        editCountTag = (EditText) findViewById(R.id.editText_tag_count);
        buttonClear = (Button) findViewById(R.id.button_clear_data);
        buttonreadTag = (Button) findViewById(R.id.button_inventory);

        classText = (TextView) findViewById(R.id.classText);
        existText = (TextView) findViewById(R.id.existText);
        lackText = (TextView) findViewById(R.id.lackText);
//        radioGroup = (RadioGroup) findViewById(R.id.RgInventory);
//        rbSingle = (RadioButton) findViewById(R.id.RbInventorySingle);
//        rbLoop = (RadioButton) findViewById(R.id.RbInventoryLoop);
//        listViewTag = (ListView) findViewById(R.id.listView_tag);
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
//        rbSingle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView,
//                                         boolean isChecked) {
//                if (isChecked) {
//                    isSingleRead = true;
//                    isSend = false;
//                    isRecv = false;
//                    buttonreadTag.setText("读标签");
//                    Log.i(TAG, "isSingle --- >" + isSingleRead);
//                }
//            }
//        });
//        rbLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView,
//                                         boolean isChecked) {
//                if (isChecked) {
//                    isSingleRead = false;
//
//                    Log.i(TAG, "isSingle --- >" + isSingleRead);
//                }
//            }
//        });
        // 读标签
        buttonreadTag.setOnClickListener(new ButtonreadtagListner());

//        //清空
        buttonClear.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
//                editCountTag.setText("");
                listEPC.removeAll(listEPC);
                existText.setText("");
                lackText.setText("");
//                listViewTag.setAdapter(null);
            }
        });
    }

    List<InventoryInfo> listTag;//单次读标签返回的数据
    boolean isRuning = false;

    // 读标签
    class ButtonreadtagListner implements View.OnClickListener {
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
                    buttonreadTag.setText("搜索课本");
                } else {
//					isRuning = true;.
                    isSend = true;
                    isRecv = true;
                    buttonreadTag.setText("停止搜索");
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
                        Thread.sleep(50);
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
                                        Util.play(1, 0);//播放提示音
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


                //检查什么课本带了
                Log.i("xuxu", listMap.toString());
                boolean first = true;
                String existClassStr = "存在课本：";
                for (int i = 0; i < listEPC.size(); i++)
                    if (epc2class.containsKey(listEPC.get(i).getEpc())) {
                        if (first)
                            first = false;
                        else
                            existClassStr += "，";
                        existClassStr += epc2class.get(listEPC.get(i).getEpc());
                    }
                existText.setText(existClassStr);

                //什么课本没带
                ArrayList<String> lackClass = (ArrayList<String>) allTadayClass.clone();
                for (int i = 0; i < listEPC.size(); i++) {
                    if (epc2class.containsKey(listEPC.get(i).getEpc()))
                        lackClass.remove(epc2class.get(listEPC.get(i).getEpc()));
                }
                String lackClassStr = "缺少课本：";
                first = true;
                for (String s : lackClass) {
                    if (first)
                        first = false;
                    else
                        lackClassStr += "，";
                    lackClassStr += s;
                }
                lackText.setText(lackClassStr);


//                listViewTag.setAdapter(new SimpleAdapter(MainActivityClass.this,
//                        listMap, R.layout.list_epc_item, new String[]{
//                        "EPC", "PC", "RSSI", "COUNT"}, new int[]{
//                        R.id.textView_item_epc, R.id.textView_item_pc,
//                        R.id.textView_item_rssi, R.id.textView_item_count}));
            }
        });
    }


    public void set_exitButton() {
        exitButton = (TextView) findViewById(R.id.exitButton);

        //为退出按钮绑定监听器
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建AlertDialog.Builder对象，该对象是AlterDialog的创建器，AlterDialog用来创建弹出对话框
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityClass.this);
                exit(builder);
            }
        });
    }

    public void set_setButton() {
        setButton = (TextView) findViewById(R.id.setButton);

        //声明一个获取系统音频服务的类的对象
        final AudioManager audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        //获取手机之前设置好的铃声模式,该数据将用来传递给activity_set
        final int orgRingerMode = audioManager.getRingerMode();

        //为设置按钮绑定监听器
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivityClass.this, SetActivity.class);
                //将orgRingerMode数据传给activity_set
                intent.putExtra("mode_ringer", orgRingerMode);
                startActivity(intent);
            }
        });
    }

    public void set_list_tabs() {

        list[0] = (ListView) findViewById(R.id.list0);
        list[1] = (ListView) findViewById(R.id.list1);
        list[2] = (ListView) findViewById(R.id.list2);
        list[3] = (ListView) findViewById(R.id.list3);
        list[4] = (ListView) findViewById(R.id.list4);
        list[5] = (ListView) findViewById(R.id.list5);
        list[6] = (ListView) findViewById(R.id.list6);
        tabs = (TabHost) findViewById(R.id.tabhost);

        //创建手势检测器
        detector = new GestureDetector(this, new DetectorGestureListener());

        //在配置任何的TabSpec之前，必须在TabHost上调用该方法
        tabs.setup();

        //为主界面注册七个选项卡
        TabHost.TabSpec spec = null;
        addCard(spec, "tag1", R.id.list0, "一");
        addCard(spec, "tag2", R.id.list1, "二");
        addCard(spec, "tag3", R.id.list2, "三");
        addCard(spec, "tag4", R.id.list3, "四");
        addCard(spec, "tag5", R.id.list4, "五");
        addCard(spec, "tag6", R.id.list5, "六");
        addCard(spec, "tag7", R.id.list6, "日");

        //修改tabHost选项卡中的字体的颜色
        TabWidget tabWidget = tabs.getTabWidget();
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            TextView tv = (TextView) tabWidget.getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(0xffFFFFFF);//0xff004499
            tv.setGravity(Gravity.CENTER);
        }

        //设置打开时默认的选项卡是当天的选项卡，选择是哪天
        tabs.setCurrentTab(ShareMethod.getWeekDay());

        //用适配器为各选项卡添加所要显示的内容
        for (int i = 0; i < 7; i++) {
            cursor[i] = db.select(i);
            list[i].setAdapter(adapter(i));
        }
    }

    public void set_list_view() {
        for (int day = 0; day < 7; day++) {
            //为七个ListView绑定触碰监听器，将其上的触碰事件交给GestureDetector处理
            //此监听器是必须的，不然滑动手势只在ListView下的空白区域有效，而在ListView上无效
            list[day].setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return detector.onTouchEvent(event);
                }
            });

            //为每个ListView的每个item绑定监听器，点击则弹出由AlertDialog创建的列表对话框进行选择
            list[day].setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                                        final int id, long arg3) {
                    final int currentDay = tabs.getCurrentTab();
                    final int n = id;
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityClass.this);
                    builder.setIcon(R.drawable.ic_launcher);
                    builder.setTitle("选择");
                    TextView tv = (TextView) arg1.findViewById(R.id.ltext0);
                    Log.i("Test", (tv.getText().toString().equals("")) + "");
                    //如果课程栏目为空就启动添加对话框
                    if ((tv.getText()).toString().equals("")) {
                        //通过数组资源为对话框中的列表添加选项内容，这里只有一个选项
                        builder.setItems(R.array.edit_options1, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //如果单击了该列表项，则跳转到编辑课程信息的界面
                                if (which == 0) {
                                    new MyDialog(MainActivityClass.this).add(currentDay, n);
                                }
                            }
                        });
                        builder.create().show();
                    }

                    //否则启动修改对话框，或直接删除数据
                    else {
                        builder.setItems(R.array.edit_options2, new DialogInterface.OnClickListener() {

                            @SuppressWarnings("deprecation")
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //如果单击了该列表项，则跳转到编辑课程信息的界面
                                if (which == 0) {

                                    // 更改课程！！！
                                    new MyDialog(MainActivityClass.this).modify(currentDay, n);
                                }
                                if (which == 1) {
                                    cursor[currentDay].moveToPosition(n);
                                    int n1 = Integer.parseInt(cursor[currentDay].getString(7));//课程的总节数
                                    int n2 = Integer.parseInt(cursor[currentDay].getString(8));//选中的为该课程的第几节
                                    switch (n2) {
                                        case 0:
                                            for (int m = 0; m < n1; m++) {
                                                MainActivityClass.db.deleteData(currentDay, n + m + 1);
                                            }
                                            break;

                                        case 1:
                                            MainActivityClass.db.deleteData(currentDay, n);
                                            for (int m = 1; m < n1; m++) {
                                                MainActivityClass.db.deleteData(currentDay, n + m);
                                            }
                                            break;
                                        case 2:
                                            MainActivityClass.db.deleteData(currentDay, n - 1);
                                            MainActivityClass.db.deleteData(currentDay, n);
                                            for (int m = 2; m < n1; m++) {
                                                MainActivityClass.db.deleteData(currentDay, n + m - 1);
                                            }
                                            break;
                                        case 3:
                                            for (int m = n2; m >= 0; m--) {
                                                MainActivityClass.db.deleteData(currentDay, n - m + 1);
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                    cursor[currentDay].requery();
                                    list[currentDay].invalidate();
                                }
                            }
                        });
                        builder.create().show();
                    }
                }
            });
        }
    }


    //左滑&右滑
    //内部类，实现GestureDetector.OnGestureListener接口
    class DetectorGestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        //当用户在触屏上“滑过”时触发此方法
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            int i = tabs.getCurrentTab();
            //第一个触点事件的X坐标值减去第二个触点事件的X坐标值超过FLIP_DISTANCE，也就是手势从右向左滑动
            if (e1.getX() - e2.getX() > FLIP_DISTANCE) {
                if (i < 6)
                    tabs.setCurrentTab(i + 1);
                //	float currentX = e2.getX();
                //	list[i].setRight((int) (inialX - currentX));
//                set_class_data();
                existText.setText("");
                lackText.setText("");
                return true;
            }

            //第二个触点事件的X坐标值减去第一个触点事件的X坐标值超过FLIP_DISTANCE，也就是手势从左向右滑动
            else if (e2.getX() - e1.getX() > FLIP_DISTANCE) {
                if (i > 0)
                    tabs.setCurrentTab(i - 1);
//                set_class_data();
                existText.setText("");
                lackText.setText("");
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

    }

    //覆写Activity中的onTouchEvent方法，将该Activity上的触碰事件交给GestureDetector处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    //设置菜单按钮
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    //当点击菜单中的“退出”键时，弹出提示是否退出的对话框
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //创建AlertDialog.Builder对象，该对象是AlterDialog的创建器，AlterDialog用来创建弹出对话框
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (item.getItemId() == R.id.menu_exit) {
            exit(builder);
            return true;
        }
        if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(MainActivityClass.this, SetActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //子方法:为主界面添加选项卡
    public void addCard(TabHost.TabSpec spec, String tag, int id, String name) {
        spec = tabs.newTabSpec(tag);
        spec.setContent(id);
        spec.setIndicator(name);
        tabs.addTab(spec);
    }

    //子方法：用来弹出是否退出程序的对话框，并执行执行是否退出操作
    public void exit(AlertDialog.Builder builder) {
        //为弹出的对话框设置标题和内容
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("退出程序");
        builder.setMessage("确定要退出课表吗？");
        //设置左边的按钮为“确定”键，并且其绑定监听器，点击后退出
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //退出应用程序，即销毁地所有的activity
                MyApplication.getInstance().exitApp();
            }
        });
        //设置右边的按钮为“取消”键，并且其绑定监听器，点击后仍然停留在当前界面
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        //创建并显示弹出的对话框
        builder.create().show();
    }

    //为每一个list提供数据适配器
    @SuppressWarnings("deprecation")
    public SimpleCursorAdapter adapter(int i) {
        return new SimpleCursorAdapter(this, R.layout.list_v2, cursor[i], new String[]{"_id", "classes", "location",
                "teacher", "zhoushu"}, new int[]{R.id.number, R.id.ltext0, R.id.ltext1, R.id.ltext6, R.id.ltext7});
    }

    //第一次运行时创建数据库表
    static class SingleInstance {
        static SingleInstance si;

        private SingleInstance() {
            for (int i = 0; i < 7; i++) {
                db.createTable(i);
            }
        }

        static SingleInstance createTable() {
            if (si == null)
                return si = new SingleInstance();
            return null;
        }
    }
}
