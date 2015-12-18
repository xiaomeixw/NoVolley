package sabria.novolley.library.inter;


import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.response.BeautyResponse;
import sabria.novolley.library.util.BeautyExecption;

/**
 * 分发结果接口
 * @author 伟
 *
 */
public interface BeautyDeliveryInter {

	public void postResponse(BeautyRequest request, BeautyResponse response);

	public void postError(BeautyRequest request, BeautyExecption error);

	public void postResponse(BeautyRequest request, BeautyResponse response, Runnable runnable);

}
