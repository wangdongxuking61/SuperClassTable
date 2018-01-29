#超级课表的安卓程序
###说明
- 打开软件：com.jr.uhf.MainActivity是程序入口，跳转到com.jr.uhf.BluetoothActivity，作为主界面
- 先点击"连接识别设备"，蓝牙连接到"Jiuray201711518"
- 点击"进入课程表"，会转到org.androidschedule.MainActivityClass的界面。再点击"搜索课本"，会显示出搜索到的课本和缺失的课本

###java文件与xml文件的对应
- com.jr.uhf.BluetoothActivity -> activity_bluetooth.xml
- org.androidschedule.MainActivityClass -> activity_main.xml
- 课程表中按下"设置"Button，会显示activity_set.xml界面

###致谢
该项目基于https://github.com/mmc-maodun/Multi-function-curriculum-Based-on-Android，非常感谢前人的工作。