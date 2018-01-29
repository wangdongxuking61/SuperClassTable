package temp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;

import com.jr.uhf.MainActivity;
import org.androidschedule.MainActivityClass;

public class MyAdapter {

    private Context context;
    private MainActivityClass main;
    private Cursor[] cursor = new Cursor[7];
    private SimpleCursorAdapter[] adapter;

    private SharedPreferences preferences;

    public MyAdapter(Context context) {
        this.context = context;
        main = (MainActivityClass) context;
    }
}
