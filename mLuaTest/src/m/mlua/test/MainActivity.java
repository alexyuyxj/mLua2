package m.mlua.test;

import m.mlua.MLua;
import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MLua mLua = new MLua("mian.lua");
		mLua.setLoadFromAssets(this);
		mLua.start(getApplication());
	}
	
}
