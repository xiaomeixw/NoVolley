package sabria.novolley.library.util;


import sabria.novolley.library.core.BeautyHttpInit;
import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.queue.BeautyRequestQueue;

/**
 * 加入队列以及取消队列帮助类
 * @author 伟
 *
 */
public class BeautyNetUtil {
	
	
	
	
	/**
	 * 传入tag名,退出请求
	 * @param tag
	 */
	public static void exitNetQueue(final String tag){
		BeautyHttpInit.getInstance().getRequestQueue().cancelAll(new BeautyRequestQueue.RequestFilter() {
			@Override
			public boolean apply(BeautyRequest request) {
				return request.getTag()!=null&&request.getTag().equals(tag);
			}
		});
	}

}
