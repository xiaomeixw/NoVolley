package sabria.novolley.library.implement;

import android.os.Handler;

import java.util.concurrent.Executor;

import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.inter.BeautyDeliveryInter;
import sabria.novolley.library.response.BeautyResponse;
import sabria.novolley.library.util.BeautyExecption;

/**
 * 分发机制：
 *    其实就是开了一个线程对response进行是否请求成功的判断，然后根据判断的结果来进行分发
 * 
 * @author 伟
 * 
 */
public class BeautyResponseDelivery implements BeautyDeliveryInter {

	// 并发处理
	private final Executor mResponsePoster;

	public BeautyResponseDelivery(Executor executor) {
		mResponsePoster = executor;
	}

	public BeautyResponseDelivery(final Handler handler) {
		mResponsePoster = new Executor() {
			@Override
			public void execute(Runnable command) {
				handler.post(command);
			}
		};
	}

	// 必须实现的三个方法
	@Override
	public void postResponse(BeautyRequest request, BeautyResponse response) {
		postResponse(request, response, null);
	}

	@Override
	public void postResponse(BeautyRequest request, BeautyResponse response, Runnable runnable) {
		// 标记为分发过了
		request.markDelivered();
		// 开线程执行run进行相关分发操作
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, runnable));
	}

	@Override
	public void postError(BeautyRequest request, BeautyExecption error) {
		BeautyResponse response = BeautyResponse.error(error);
		mResponsePoster.execute(new ResponseDeliveryRunnable(request, response, null));
	}

	/**
	 * 一个Runnable，将网络请求响应分发到UI线程中
	 */
	private class ResponseDeliveryRunnable implements Runnable {
		private final BeautyRequest mRequest;
		private final BeautyResponse mResponse;
		private final Runnable mRunnable;

		public ResponseDeliveryRunnable(BeautyRequest request, BeautyResponse response, Runnable runnable) {
			mRequest = request;
			mResponse = response;
			mRunnable = runnable;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			// 如果请求被取消,那么结束这个request,并停止分发
			// If this request has canceled, finish it and don't deliver.
			if (mRequest.isCanceled()) {
				// request已经取消，在分发时finish
				mRequest.finish();
				return;
			}

			// 如果请求时成功的,那么分发他
			if (mResponse.isSuccess()) {
				// 让他自己去取.result
				mRequest.deliverSuccessResponse(mResponse);
			} else {
				// 分发告诉request错误
				mRequest.deliverError(mResponse.error);
			}
			//这里是专门为callback-onFinish而准备的
			mRequest.requestFinish();
			//"done" 请求结束了,也告诉request结束咯
			mRequest.finish();
			// 执行参数runnable
			if (mRunnable != null) {
				mRunnable.run();
			}

		}
	}

}
