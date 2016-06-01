package com.foofv.crawler.similarity.coordinate_request;

import com.foofv.crawler.CrawlerConf;
import com.foofv.crawler.util.http.HttpAsyncClientUtil;
import com.foofv.crawler.util.http.HttpAsyncClientUtil$;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import scala.runtime.AbstractFunction2;
import scala.runtime.BoxedUnit;

import java.io.IOException;
import java.net.URI;

/**
 * Created by msfenn on 16/09/15.
 */

//TODO: may change it to asynchronous mode later
public class HttpSynchronousRequest {

    private HttpRequestBase requestBase = new HttpGet();
    private HttpClientContext context = HttpClientContext.adapt(new BasicHttpContext());
    private HttpResponseHandler responseHandler = new HttpResponseHandler();

    private static CrawlerConf crawlerConf = new CrawlerConf();
    private static HttpAsyncClientUtil httpClient = HttpAsyncClientUtil$.MODULE$.getInstance(crawlerConf);

    class HttpResponseHandler extends AbstractFunction2<HttpClientContext, HttpResponse, BoxedUnit> {

        private final Boolean LOCK = false;
        private String response;

        @Override
        public BoxedUnit apply(HttpClientContext httpContext, HttpResponse httpResponse) {

            try {
                HttpEntity httpEntity = httpResponse.getEntity();
                synchronized (LOCK) {
                    response = EntityUtils.toString(httpEntity);
                    LOCK.notify();
                }
            } catch (IOException e) {
                response = null;
                e.printStackTrace();
            }

            return null;
        }

        public String getResponse() {

            synchronized (LOCK) {
                while (response == null)
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
            String result = response;
            response = null;

            return result;
        }
    }

    public String getHttpRequest(String url) {

        requestBase.setURI(URI.create(url));
        httpClient.execute(requestBase, context, responseHandler);
        String response = responseHandler.getResponse();

        return response;
    }

    public static void main(String[] args) {

        HttpSynchronousRequest request = new HttpSynchronousRequest();
        System.out.println(request.getHttpRequest("http://api.map.baidu.com/geoconv/v1/?coords=12947453.59,4846467.98&from=6&to=5&ak=AOkR59MT4atImiOo3BGee0lL"));
        System.out.println(request.getHttpRequest("http://api.map.baidu.com/geocoder/v2/?address=浦东大道&output=json&ak=AOkR59MT4atImiOo3BGee0lL"));
    }
}
