package sabria.novolley;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import sabria.novolley.library.core.BeautyHttpLib;
import sabria.novolley.library.inter.BeautyCallBack;
import sabria.novolley.library.util.BeautyNetUtil;

/**
 * 测试
 *
 * @author 伟
 *         请求头设置：重写getHeaders方法
 */
public class Test extends Activity {

    public final String TestNetTag = "Test";
    private BeautyHttpLib lib;
    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.testxml);

        Button post = (Button) findViewById(R.id.post);
        text = (TextView) findViewById(R.id.text);


        post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDataFromService();
            }
        });


    }

    private void getDataFromService() {

        lib = new BeautyHttpLib();

        lib.get("https://api.github.com/", TestNetTag, new BeautyCallBack() {

            @Override
            public void onPreStart() {
                super.onPreStart();
            }

            @Override
            public void onSufFinish() {
                super.onSufFinish();
            }

            @Override
            public void onSuccess(byte[] data) {
                //这里只是请求成功的返回，然后不管返回的是什么JSON
                text.setText(new String(data));
            }

            @Override
            public void onFail(int statusCode, String messsage) {
                //statuscode不是200的各种情况
                text.setText(messsage);
            }

        });
    }

    /**
     * 在onDestory中消除此页面的所有请求
     */
    @Override
    protected void onDestroy() {
        BeautyNetUtil.exitNetQueue(TestNetTag);
        super.onDestroy();
    }

}
