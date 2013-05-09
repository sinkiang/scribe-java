package org.scribe.spring;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.FactoryBean;

public class OAuthServiceFactoryBean implements FactoryBean<OAuthService> {
	private Class<? extends Api> apiClass;
	private String apiKey;
	private String apiSecret;
	private String callback;

	public OAuthService getObject() throws Exception {
		return new ServiceBuilder().provider(apiClass).apiKey(apiKey)
				.apiSecret(apiSecret).callback(callback).build();
	}

	public Class<?> getObjectType() {
		return OAuthService.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setApiClass(Class<? extends Api> apiClass) {
		this.apiClass = apiClass;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

}
