package sabria.novolley.library.core;


import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.squareup.okhttp.OkHttpClient;

import sabria.novolley.library.implement.BeautyImplCache;
import sabria.novolley.library.implement.BeautyImplNetwork;
import sabria.novolley.library.implement.HttpClientStack;
import sabria.novolley.library.implement.HurlStack;
import sabria.novolley.library.implement.NewOkHttpStack;
import sabria.novolley.library.implement.OkHttpStack;
import sabria.novolley.library.inter.BeautyCacheInterface;
import sabria.novolley.library.inter.BeautyHttpStackInter;
import sabria.novolley.library.inter.BeautyNetworkInter;
import sabria.novolley.library.queue.BeautyRequestQueue;
import sabria.novolley.library.util.IOUtil;

/**
 * Application 中执行单例对象获取 每个activity中执行add 然后 onDestore中执行cancel
 * 具体回调的暴露是给BeautyHttpLib来完成
 * @author 伟
 */
public class BeautyHttpInit {
	
	private Context mContext;
	private BeautyCacheInterface mCache;
	private BeautyNetworkInter mNetwork;
	private BeautyRequestQueue mInstanceQueue;
	private OkHttpStack okHttpStack;
	
	/**
	 * 单例模式
	 */
	private static BeautyHttpInit Instance;

	//这里之所以没有将getInstance 和 init()放在一个方法里,就是因为init()只用构建一次
	//如果每次getInstance里面都init(),那会非常消耗性能
	public static BeautyHttpInit getInstance() {
		if (Instance == null) {
			Instance = new BeautyHttpInit();
		}
		return Instance;
	}

	public BeautyHttpInit() {
	}
	
	
	/**
	 * 初始化 
	 * 只在application中执行一次:OkVolley.getInstance().init(this)
	 */
	public BeautyHttpInit init(Context context){
		return init(context,getDefaultHttpStack());
	}
	//因为init方法只执行一遍,所以这里执行newQueue方法
	public BeautyHttpInit init(Context context,BeautyHttpStackInter httpStack){
		this.mContext=context;
		this.mInstanceQueue=newRequestQueue(context,httpStack);
		return this;
	}

	
	/**
	 * 初始化就只创建一个Queue,下次直接判断是否为null,然后拿去被创建过的那个
	 * @param context
	 * @return 
	 */
	private BeautyRequestQueue newRequestQueue(Context context,BeautyHttpStackInter httpStack) {
		
		 final long startTime = System.currentTimeMillis();  //開始時間
		
		//初始化BeautyImplNetwork
		if(mNetwork==null){
			mNetwork=new BeautyImplNetwork(httpStack);
		}
		//初始化BeautyImplCache
		if(mCache==null){
			mCache=new BeautyImplCache(IOUtil.initCachePath(mContext));
		}
		
		//初始化请求队列并开启,等待request持续不断的加入
		BeautyRequestQueue requestQueue = new BeautyRequestQueue(mCache, mNetwork);
		requestQueue.start();
		
		
		long consumingTime = System.currentTimeMillis() - startTime; //消耗時間：纳秒
		System.out.println("初始化application:"+consumingTime+"毫秒");
		
		return requestQueue;
	}
	
	/**
	 * 其他任何地方都只做拿的动作
	 * 			OkVolley.getInstance().getRequestQueue().add(request);
	 * @return
	 */
	public BeautyRequestQueue getRequestQueue(){
		if(mInstanceQueue==null){
			mInstanceQueue=newRequestQueue(mContext, getDefaultHttpStack());
		}
		return mInstanceQueue;
	}
	

	/**
	 * 默认的是获取OKHTTP的Stack
	 * android5.0已经默认用OKHTTP替换HTTPClient和HTTPURL
	 * 
	 * API<9使用HttpClientStack
	 * API>9使用HurlStack
	 * 建议使用OKHttpStack
	 * @return
	 */
	private BeautyHttpStackInter getDefaultHttpStack() {
		
			//2.3版本以下版本使用HttpClientStack
		    if (Build.VERSION.SDK_INT < 9) {
		    	 return new HttpClientStack(AndroidHttpClient.newInstance("volley/0"));
		    //2.3-4.0版本使用HttpURLConnection 
	        } else if(Build.VERSION.SDK_INT>=9&&Build.VERSION.SDK_INT<14){
	        	return new HurlStack();
	        //大于4.0版本使用OKHttp
	        } else if(Build.VERSION.SDK_INT>=14){
	        	System.out.println("使用OKHttp");
	        	//return new HurlStack();
	        	return new NewOkHttpStack(new OkHttpClient());
	        } else {
	        	return new HurlStack();
	        }
		    
		  /*if(okHttpStack==null){
			 okHttpStack=new OkHttpStack(new OkHttpClient());
		  }*/
		  //return okHttpStack;
	}
	
	
	
	

}
