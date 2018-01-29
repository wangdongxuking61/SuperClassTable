package com.jr.uhf;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class HelpAcivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		TextView textTitle = (TextView) findViewById(R.id.textView_title);
		textTitle.setText("帮助");
	}

}
