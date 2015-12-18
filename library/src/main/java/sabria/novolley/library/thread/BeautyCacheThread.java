package sabria.novolley.library.thread;

import android.os.Process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.inter.BeautyCacheInterface;
import sabria.novolley.library.inter.BeautyDeliveryInter;
import sabria.novolley.library.inter.BeautyNetworkInter;
import sabria.novolley.library.response.BeautyResponse;
import sabria.novolley.library.response.NetworkResponse;


/**
 * 一个线程，用于调度处理走缓存的请求。启动后会不断从缓存请求队列中取请求处理，
 * 队列为空则等待，请求处理结束则将结果传递给ResponseDelivery 去执行后续处理。
 * 当结果未缓存过、缓存失效或缓存需要刷新的情况下，
 * 该请求都需要重新进入BeautyNetworkThread去调度处理。
 * 
 * 操作步骤：
 * 1.初始化本地缓存
 * 2.开始一个无限的循环，调用 mCacheQueue的take方法，来获得一个请求
 * 3.判断请求是否已经取消，如果已经被取消了，跳出本次循环
 * 4.根据请求的CacheKey去缓存中寻找相对应的记录，如果找不到对应的记录，或者对应的记录过期了，则将其放到NetworkQueue队列中
 * 5.缓存中存在相对应的记录，那么调用每个请求具体的实现方法 parseNetworkResponse函数，根据具体的请求去解析得到对应的响应Response对象
 * 6.获得Response对象之后,判断缓存中的那个是否新鲜，如果不新鲜了，同时也把缓存中的更新下。如果需要进行更新，么就会在发送响应结果回主线程更新的同时，再将请求放到NetworkQueue中，从网络中更新请求对应的数据。如果不需要，则直接将结果调用mDelivery传回主线程进行UI的更新
 * 
 * @author 伟
 *
 */
public class BeautyCacheThread extends Thread {

	// ***********************************
	// 参数
	// ***********************************
	/**
	 * 两个不断传递的接口 
	 * 缓存方案对象接口 最终会将缓存存储到Disk上 persisting responses to disk 
	 * 网络方案对象接口  执行网络请求performing HTTP requests
	 */
	private final BeautyCacheInterface mCache;
	private final BeautyNetworkInter mNetwork;

	/**
	 * 结果的传输接口最终根据http请求的结果进行分发 posting responses and errors
	 */
	private final BeautyDeliveryInter mDelivery;

	// BlockingQueue线程容器,从requestQueue中传递过来
	private final BlockingQueue<BeautyRequest> mNetworkQueue;
	private final BlockingQueue<BeautyRequest> mCacheQueue;

	// 停止线程使用volatile + interrupt()结合 替代stop()
	private volatile boolean mQuit = false;
	
	// ***********************************
	// 构造函数
	// ***********************************
	public BeautyCacheThread(PriorityBlockingQueue<BeautyRequest> mNetworkQueue, PriorityBlockingQueue<BeautyRequest> mcacheQueue,BeautyNetworkInter mNetwork, BeautyCacheInterface mCache, BeautyDeliveryInter mDelivery) {
		this.mNetworkQueue = mNetworkQueue;
		this.mCacheQueue=mcacheQueue;
		this.mNetwork = mNetwork;
		this.mCache = mCache;
		this.mDelivery = mDelivery;
	}
	
	
	// ***********************************
	// 线程run
	// ***********************************
	@Override
	public void run() {
		//设置优先级
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		//1.初始化缓存
		mCache.initialize();
		
		//2.while-true循环
		while (true) {
			//3.从缓存队列中取得一个request
			try {
				final BeautyRequest request = mCacheQueue.take();
				
				//3.如果请求取消，结束本次请求，然后重新获取请求
				if(request.isCanceled()){
					request.finish();
					//break是结束整个循环体，continue是结束单次循环
					//当执行continue时结束这次请求,当while-true依然会持续执行
					continue;
				}
				
				//4.根据key从缓存中拿结果value
				BeautyCacheInterface.Entry entry=mCache.get(request.getCacheKey());
				
				//5.缓存结果不存在，将请求加入网络容量池
				if(entry == null){
					//如果缓存只有key在，而value为null,称为缓存丢失Cache miss
					//那就把request放入网络容量池中
					 //这里说明缓存中没有对应的记录，那么需要去网络中获取，那么就将它放到Network的队列中  
					mNetworkQueue.put(request);
					//结束本次循环
					continue;
				}
				
				//6.缓存是否过期
			    if(entry.isExpired()){
			    	//如果过期，设置缓存实体，同时也放入网络容量池
			    	request.setCacheEntry(entry);
			    	//// 如果缓存中有记录，但是已经过期了或者失效了，也需要去网络获取，放到Network队列中  
			    	mNetworkQueue.put(request);
			    	continue;
			    }
				
				//7.将缓存解析为response对象
			    //// 如果上面的情况都不存在，说明缓存中存在这样记录，那么就调用request的parseNetworkResponse方法，获取一个响应Response  
			    BeautyResponse response=request.parseNetworkResponse(new NetworkResponse(entry.data, entry.responseHeaders));
				
			    //8判断缓存结果是否新鲜
			    if(!entry.refreshNeeded()){
			    	//如果新鲜,就直接传递Response
			    	//缓存记录，不需要更新，那么就直接调用mDelivery，传回给主线程去更新。 
			    	mDelivery.postResponse(request, response);
			    }else{
			    	//存在这样一种情况，缓存记录存在，但是它约定的生存时间已经到了（还未完全过期，叫软过期），可以将其发送到主线程去更新  
			    	//如果不新鲜了,传递响应结果
			    	//但同时放入network去进行新鲜度验证
			    	request.setCacheEntry(entry);
			    	//标记response intermediate
			    	response.intermediate = true;
			    	// 将其传回主线程的同时，将请求放到Network队列中。
			    	mDelivery.postResponse(request, response,new Runnable() {
						
						@Override
						public void run() {
							 try {
								 //放入Network线程
	                              mNetworkQueue.put(request);
	                            } catch (InterruptedException e) {
	                               //nothing can do
	                            }
						}
					});
			    }
			} catch (InterruptedException e) {
				if(mQuit){
					return;
				}
				continue;
			}
			
		}
		
		
	}
	
	
	
	
	/**
	 * 中断thread
	 */
	public void quit() {
		mQuit = true;
		interrupt();
	}

}
