package arkive.admin.comm.web;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.LaxRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class RestTemplateUtil {


	public static String getXmlResponseAsync(String url, Map<String, String> pEgovMap){
		RestTemplate asyncRestTemplate = new RestTemplate();

		CloseableHttpClient httpClient = HttpClientBuilder.create()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();

		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		asyncRestTemplate.setRequestFactory(factory);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url);

		Iterator<String> keyList = pEgovMap.keySet().iterator();

		String mapKey = "";
		while(keyList.hasNext()){
			mapKey = keyList.next();
			uriComponentsBuilder =  uriComponentsBuilder.queryParam(mapKey, pEgovMap.get(mapKey));
		}

		String queryUrl = uriComponentsBuilder.build(false).toString();
		ResponseEntity<String> result = asyncRestTemplate.getForEntity(queryUrl, String.class);

		return result.getBody();
	}

	public static String postXmlResponseAsync(String url, Map<String, String> pEgovMap) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, HttpClientErrorException {
		RestTemplate asyncRestTemplate = new RestTemplate();

		SSLContext sslContexts = SSLContexts.custom().loadTrustMaterial(new TrustAllStrategy()).build();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContexts, NoopHostnameVerifier.INSTANCE);

	    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
	            .register("https", socketFactory)
	            .register("http", PlainConnectionSocketFactory.getSocketFactory())
	            .build();

	    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

	    CloseableHttpClient httpClient = HttpClients.custom()
	            .setConnectionManager(connManager)
	            .setRedirectStrategy(new DefaultRedirectStrategy())
	            .build();
		
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		asyncRestTemplate.setRequestFactory(factory);

		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url);

		Iterator<String> keyList = pEgovMap.keySet().iterator();

		String mapKey = "";
		while(keyList.hasNext()){
			mapKey = keyList.next();
			uriComponentsBuilder =  uriComponentsBuilder.queryParam(mapKey, pEgovMap.get(mapKey));
		}

		String queryUrl = uriComponentsBuilder.build(false).toString();
		ResponseEntity<String> result = asyncRestTemplate.postForEntity(queryUrl, null, String.class);

		return result.getBody();
	}
}
