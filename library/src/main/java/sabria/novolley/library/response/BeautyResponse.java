package sabria.novolley.library.response;


import sabria.novolley.library.inter.BeautyCacheInterface;
import sabria.novolley.library.util.BeautyExecption;

/**
 * 通过一轮逻辑代码处理后，将NetworkResponse变成BeautyResponse
 * @author 伟
 *
 */
public class BeautyResponse {

	/**
	 * 缓存实体
	 */
	public final BeautyCacheInterface.Entry cacheEntry;
	
	public final BeautyExecption error;
	
	public boolean intermediate=false;
	/**
	 * 返回的数据
	 */
	public byte[] result;
	
	
	
	public boolean isSuccess() {
        return error == null;
    }
	
	private BeautyResponse(byte[] result, BeautyCacheInterface.Entry cacheEntry) {
        this.result = result;
        this.cacheEntry = cacheEntry;
        this.error = null;
    }

    private BeautyResponse(BeautyExecption error) {
        this.result = null;
        this.cacheEntry = null;
        this.error = error;
    }
    
    /**
     * 返回一个成功的HttpRespond
     * 在request中会被使用
     * @param result
     *            Http响应的类型
     * @param cacheEntry
     *            缓存对象
     */
    public static  BeautyResponse success(byte[] result, BeautyCacheInterface.Entry cacheEntry) {
        return new BeautyResponse(result, cacheEntry);
    }

    /**
     * 返回一个失败的HttpRespond
     * @param error
     *            失败原因
     */
    public static  BeautyResponse error(BeautyExecption error) {
        return new BeautyResponse(error);
    }
    

}
