package sabria.novolley;

import android.app.Application;

import sabria.novolley.library.core.BeautyHttpInit;

public class BeautyApplication extends Application{
	
	@Override
	public void onCreate() {
		super.onCreate();
		initBeautyNet();
	}

	private void initBeautyNet() {
		BeautyHttpInit.getInstance().init(getApplicationContext());
	}

}
