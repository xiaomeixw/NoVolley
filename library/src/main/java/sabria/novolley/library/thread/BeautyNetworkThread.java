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
import sabria.novolley.library.util.BeautyExecption;
import sabria.novolley.library.util.BeautyLog;


/**
 * network线程：从网络请求队列中循环读取请求并且执行 解读：cpu+1个线程，用于调度处理走网络的请求。启动后会不断从网络请求队列中取请求处理，
 * 队列为空则等待，请求处理结束则将结果传递给 ResponseDelivery 去执行后续处理，并判断结果是否要进行缓存。
 * 
 * 1.调用 mQueue的take（）方法从队列中获取请求，如果没有请求，则一直阻塞在那里等待，直到队列中有新的请求到来。
 * 2.判断请求有没有被取消，如果被取消，则重新获取请求.
 * 3.调用Network对象将请求发送到网络中，并返回一个 NetworkResponse对象.
 * 4.调用请求的pareseNetworkResonse方法，将NetworkResponse对象解析成相对应的Response对象.
 * 5.判断请求是否需要缓存，如果需要缓存，则将其Response中cacheEntry对象放到缓存mCache中.
 * 6.调用 mDelivery将Response对象传到主线程中进行UI更新.
 * 
 * @author 伟
 * 
 */
public class BeautyNetworkThread extends Thread {

	// ***********************************
	// 参数
	// ***********************************
	/**
	 * 两个不断传递的接口 
	 * 缓存方案对象接口 最终会将缓存存储到Disk上 persisting responses to disk 
	 * 网络方案对象接口 执行网络请求performing HTTP requests
	 */
	private final BeautyCacheInterface mCache;
	private final BeautyNetworkInter mNetwork;
	
	/**
	 * 结果的传输接口最终根据http请求的结果进行分发 posting responses and errors
	 */
	private final BeautyDeliveryInter mDelivery;
	
	//BlockingQueue线程容器,从requestQueue中传递过来
	private final BlockingQueue<BeautyRequest> mQueue;
	
	//停止线程使用volatile + interrupt()结合 替代stop()
	private volatile boolean mQuit = false;
	
	
	
	
	// ***********************************
	// 构造函数
	// ***********************************
	public BeautyNetworkThread(PriorityBlockingQueue<BeautyRequest> mNetworkQueue, BeautyNetworkInter mNetwork, BeautyCacheInterface mCache, BeautyDeliveryInter mDelivery) {
	   this.mQueue=mNetworkQueue;
	   this.mNetwork=mNetwork;
	   this.mCache=mCache;
	   this.mDelivery=mDelivery;
	}
	
	
	// ***********************************
	// 线程run
	// ***********************************
	@Override
	public void run() {
		//线程优先级
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		BeautyRequest request;
		//1.while-true循环开始
		while (true) {
			
			//2.从线程池容量中拿出首个request
			try {
				request=mQueue.take();
			} catch (InterruptedException e) {
				//如果需求线程停止,.return 跳出while-true
				if(mQuit){
					return;
				}
				//异常后结束本次循环
				continue;
			}
			
			
			
			try {
				//3.如果请求被取消那么请求结束
				if(request.isCanceled()){
					request.finish();
					continue;
				}
				
				//API>=14这里可以使用TrafficStats.setThreadStatsTag()方法来标记线程内部发生的数据传输情况
				
				//4.如果请求不取消，继续执行
				//此时通过mNetwork来执行request请求,获取NetworkResponse
				NetworkResponse networkResponse = mNetwork.executeRequestAndTransToHttpResponse(request);
				
				//5.如果返回的state=304 同时 请求已经有响应传输 结束请求
				//304:自从上次请求后，请求的网页未修改过。服务器返回此响应时，不会返回网页内容。
				if(networkResponse.notModified && request.hasHadResponseDelivered()){
					request.finish();
					continue;
				}
				
				//6.将networkResponse 在UI线程中 正式解析为 Response
				BeautyResponse response = request.parseNetworkResponse(networkResponse);
				
				//7.是否开启缓存
				if(request.shouldCache() && response.cacheEntry!=null){
					//存储到缓存中
					mCache.put(request.getCacheKey(), response.cacheEntry);
				}
				
				//8.请request标记为已传输，同时传输响应结果到UI界面修改
				request.markDelivered();
				mDelivery.postResponse(request, response);
			} catch(BeautyExecption e){
				 parseAndDeliverNetworkError(request, e);
			}catch (Exception e) {
				 BeautyLog.debug("Unhandled exception %s", e.getMessage());
				//错误分发
				mDelivery.postError(request,new BeautyExecption(e));
			}
			
			
			
		}
		
		
		
		
	}






	/**
	 * 开始队列执行前，停止之前的Thread
	 * requestQueue中调用
	 */
	public void quit() {
		mQuit=true;
		interrupt();
	}
	
	
	/**
	 * 传递异常
	 * @param request
	 * @param error
	 */
	private void parseAndDeliverNetworkError(BeautyRequest request,BeautyExecption error) {
        error = request.parseNetworkError(error);
        mDelivery.postError(request, error);
    }
	

}
