package sabria.novolley.library.queue;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import sabria.novolley.library.config.BeautyHttpConfig;
import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.implement.BeautyResponseDelivery;
import sabria.novolley.library.inter.BeautyCacheInterface;
import sabria.novolley.library.inter.BeautyDeliveryInter;
import sabria.novolley.library.inter.BeautyNetworkInter;
import sabria.novolley.library.thread.BeautyCacheThread;
import sabria.novolley.library.thread.BeautyNetworkThread;
import sabria.novolley.library.util.BeautyLog;


/**
 * 请求队列
 * 
 * @author xiongwei
 * 
 */
public class BeautyRequestQueue {

	// ***********************************
	// 参数
	// ***********************************

	// AtomicInteger 一种线程安全的加减自增自减操作接口
	private AtomicInteger mRandomSequenceNum = new AtomicInteger();
	/**
	 * 等待的请求以及当前的请求
	 */
	// HashMap储存键值对|put()方法|无序 <---> HashSet仅仅存储key|add()|无序
	private final Map<String, Queue<BeautyRequest>> mWaittingRequests = new HashMap<String, Queue<BeautyRequest>>();
	private final Set<BeautyRequest> mCurrentRequests = new HashSet<BeautyRequest>();

	/**
	 * 两个存储Thread的线程容器
	 */
	// BlockingQueue线程容器,PriorityBlockingQueue是其子类,存储对象必须实现Comparable接口。队列通过接口的compare方法确定对象的priority,如果compare方法返回负数，那么在队列里面的优先级就比较高
	// take()获取并移除此队列的头部
	private final PriorityBlockingQueue<BeautyRequest> mNetworkQueue = new PriorityBlockingQueue<BeautyRequest>();
	private final PriorityBlockingQueue<BeautyRequest> mCacheQueue = new PriorityBlockingQueue<BeautyRequest>();

	/**
	 * 执行网络请求的线程 执行缓存线程
	 */
	private BeautyNetworkThread[] mDispatchers;
	private BeautyCacheThread mCacheDispatcher;

	/**
	 * 两个不断传递的接口 缓存方案对象接口 最终会将缓存存储到Disk上 persisting responses to disk 网络方案对象接口
	 * 执行网络请求performing HTTP requests
	 */
	private final BeautyCacheInterface mCache;
	private final BeautyNetworkInter mNetwork;

	/**
	 * 结果的传输接口最终根据http请求的结果进行分发 posting responses and errors
	 */
	private final BeautyDeliveryInter mDelivery;

	// ***********************************
	// 初始化构造函数
	// ***********************************
	public BeautyRequestQueue(BeautyCacheInterface mCache, BeautyNetworkInter mNetwork) {
		this(mCache, mNetwork, BeautyHttpConfig.DEFAULT_NETWORK_THREAD_POOL_SIZE);
	}

	public BeautyRequestQueue(BeautyCacheInterface mCache, BeautyNetworkInter mNetwork, int threadPoolSize) {
		//最后一个参数就是分发线程run起来
		this(mCache, mNetwork, threadPoolSize, new BeautyResponseDelivery(new Handler(Looper.getMainLooper())));
	}

	/**
	 * 等待调用start()方法,然后开始创建Thread
	 * 
	 * @param cache
	 *            将responses存储到disk缓存中
	 * @param network
	 *            执行HTTP请求
	 * @param threadPoolSize
	 *            network threads网络线程数量
	 * @param delivery
	 *            分发结果
	 */
	public BeautyRequestQueue(BeautyCacheInterface cache, BeautyNetworkInter network, int threadPoolSize, BeautyDeliveryInter delivery) {
		mCache = cache;
		mNetwork = network;
		mDispatchers = new BeautyNetworkThread[threadPoolSize];
		mDelivery = delivery;
	}

	// ***********************************
	// start()方法开启队列执行逻辑
	// ***********************************
	public void start() {
		// 第一步：停止所有,保证队列是最新的
		stopRunningFirst();

		// 第二步,创建Thread:(cup+1)个网络network-Thread+1个缓冲cache-Thread
		mCacheDispatcher = new BeautyCacheThread(mNetworkQueue,mCacheQueue,mNetwork,mCache,mDelivery);
		mCacheDispatcher.start();//thread开始跑

		for (int i = 0; i < mDispatchers.length; i++) {
			//创建Network-Thread
			BeautyNetworkThread beautyNetworkThread = new BeautyNetworkThread(mNetworkQueue,mNetwork,mCache,mDelivery);
			mDispatchers[i] = beautyNetworkThread;
			beautyNetworkThread.start();
		}

	}

	/**
	 * 开始队列执行前，停止之前的Thread 不使用stop，而使用 interrupt+volatile变量进行线程停止
	 */
	private void stopRunningFirst() {

		// 停止Cache-Thread ：
		if (mCacheDispatcher != null) {
			mCacheDispatcher.quit();
		}

		// 停止Network-Thread
		for (int i = 0; i < mDispatchers.length; i++) {
			if (mDispatchers[i] != null) {
				mDispatchers[i].quit();
			}
		}

	}

	// ***********************************
	// 最后调用add方法将request放入这个队列中
	// ***********************************
	/**
	 * 加入线程队列中
	 * 实例化一个request对象，调用RequestQueue.addRequest(request),该request如果不允许被缓存
	 * ，将会被添加至mNetworkQueue队列中，待多个NetworkDispatcher线程take()取出对象
	 * 如果该request可以被缓存，该request将会被添加至mCacheQueue队列中
	 * ，待mCacheDispatcher线程从mCacheQueue.take()取出对象，
	 * 如果该request在mCache中不存在匹配的缓存时，该request将会被移交添加至mNetworkQueue队列中
	 * ，待网络访问完成后，将关键头信息添加至mCache缓存中去！
	 * 
	 * @param request
	 * @return 返回的就是这个传递过来的request
	 */
	public BeautyRequest addRequest(BeautyRequest request) {
		
		//回调函数这里开始,做progress的提示
		if (request.getCallback() != null) {
			request.getCallback().onPreStart();
		}

		// 1.加入当前的线程队列
		// 1.1request与requestQueue关联起来
		request.setRequestQueue(this);
		// 1.2加入当前的set集合
		synchronized (mCurrentRequests) {
			mCurrentRequests.add(request);
		}

		// 2.给request标示数字tag
		request.setSequence(getSequenceNumber());

		// 3.是否启用缓存thread,如果没有启用cache-thread,就直接跳过cachequeue,直接存储到network容量池
		// 然后等待多个Network线程take()取出对象
		if (!request.shouldCache()) {
			// 加入Network容量池
			mNetworkQueue.add(request);
			return request;
		}

		// 4.启用缓存thread后,会将该request同时也添加到cache容量池
		synchronized (mWaittingRequests) {
			// 根据key判断缓存中是否有该request
			String cacheKey = request.getCacheKey();
			if (mWaittingRequests.containsKey(cacheKey)) {
				// 等待request容器中已经有这个request了，拿出来排队
				Queue<BeautyRequest> stagedRequests = mWaittingRequests.get(cacheKey);
				if (stagedRequests == null) {
					stagedRequests = new LinkedList<BeautyRequest>();
				}
				stagedRequests.add(request);
				// 将request村人等待request-map集合中
				mWaittingRequests.put(cacheKey, stagedRequests);
			} else {
				// 没有相同的请求被处理
				mWaittingRequests.put(cacheKey, null);
				// 将request加入cache容量池
				mCacheQueue.add(request);
			}

		}
		return request;
	}

	// ***********************************
	// 请求完成以及取消请求
	// ***********************************
	//通知请求队列，本次请求已经完成
	public void finish(BeautyRequest request){
		//从当前Requests中移除
		synchronized (mCurrentRequests) {
			mCurrentRequests.remove(request);
		}
		//如果有设置过request缓存
		if(request.shouldCache()){
			synchronized (mWaittingRequests) {
				String cacheKey = request.getCacheKey();
				Queue<BeautyRequest> waitingRequests = mWaittingRequests.remove(cacheKey);
				if(waitingRequests!=null){
					if (BeautyHttpConfig.DEBUG) {
						BeautyLog.debug("Releasing %d waiting requests for cacheKey=%s.", waitingRequests.size(), cacheKey);
					}
					mCacheQueue.addAll(waitingRequests);
				}
			
			}
		}
		
	}
	
	/**
	 * 取消请求
	 */
	public interface RequestFilter {
		public boolean apply(BeautyRequest request);
	}
	public void cancelAll(RequestFilter filter) {
		synchronized (mCurrentRequests) {
			for (BeautyRequest request : mCurrentRequests) {
				//做回调处理如下：如果是同一个tag名,apply回调 return true,那么就取消该请求
				if (filter.apply(request)) {
					request.cancel();
				}
			}
		}
	}
	//tag必须不能为空
	public void cancelAll(final Object tag) {
		if (tag == null) {
			throw new IllegalArgumentException("Cannot cancelAll with a null tag");
		}
		cancelAll(new RequestFilter() {
			@Override
			public boolean apply(BeautyRequest request) {
				return request.getTag() == tag;
			}
		});
	}
	
	

	// ***********************************
	// 帮助函数
	// ***********************************
	public int getSequenceNumber() {
		return mRandomSequenceNum.incrementAndGet();
	}
}
