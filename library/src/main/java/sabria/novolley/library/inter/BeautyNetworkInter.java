package sabria.novolley.library.inter;


import sabria.novolley.library.core.BeautyRequest;
import sabria.novolley.library.response.NetworkResponse;
import sabria.novolley.library.util.BeautyExecption;

/**
 * 执行request
 * @author 伟
 *
 */
public interface BeautyNetworkInter {

	/**
	 * 执行request并获取response
	 * 这里所谓执行request包含2个方面的组成：
	 * 			1.HttpStack具体实现类发送请求构建出系统的httpResponse
	 * 			2.然后将系统框架的HttpResponse转成含有信息的NetworkResponse
	 * @param request
	 * @return
	 */
	public NetworkResponse executeRequestAndTransToHttpResponse(BeautyRequest request) throws BeautyExecption;

}
