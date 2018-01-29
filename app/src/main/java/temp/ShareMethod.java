package temp;

import java.util.Calendar;
import java.util.Date;

public class ShareMethod {

    //获取当天是星期几，这里星期、一......分别为数字0、1......
    //    周几:日 一  二 三  四 五 六
    //    get: 1  2  3  4  5  6  7
    //weekDay: 6  0  1  2  3  4  5
    public static int getWeekDay() {
        Calendar calendar = Calendar.getInstance();
        Date date = new Date(System.currentTimeMillis());
        calendar.setTime(date);
        int weekDay = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        if (weekDay == -1) weekDay = 6;
        return weekDay;
    }

    //获取当前的时间,并以字符串"xx:xx"的形式返回
    public static String getTime() {
        Calendar c = Calendar.getInstance();
        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        //获取完整的时间，在只有一位的数字前面加0
        StringBuffer s_hour = new StringBuffer();
        StringBuffer s_minute = new StringBuffer();
        s_hour.append(hourOfDay);
        s_minute.append(minute);
        if (hourOfDay < 10) {
            s_hour.insert(0, "0");
        }
        if (minute < 10) {
            s_minute.insert(0, "0");
        }
        return s_hour.toString() + ":" + s_minute.toString();
    }

}
