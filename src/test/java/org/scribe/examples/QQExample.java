package org.scribe.examples;

import java.util.Scanner;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.QQApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class QQExample {
	private static final String NETWORK_NAME = "QQ";
	private static final String PROTECTED_RESOURCE_URL = "https://graph.qq.com/oauth2.0/me";
	private static final String PROTECTED_RESOURCE_URL_2 = "https://graph.qq.com/user/get_user_info";
	private static final Token EMPTY_TOKEN = null;

	public static void main(String[] args) {
		// Replace these with your own api key and secret
		String apiKey = "100443832";
		String apiSecret = "82be7260c81c68408e71e46d3da7ed75";
		OAuthService service = new ServiceBuilder().provider(QQApi.class)
				.apiKey(apiKey).apiSecret(apiSecret).callback("http://1.ifeeds.duapp.com/oauth/qq/callback").build();
		Scanner in = new Scanner(System.in);

		System.out.println("=== " + NETWORK_NAME + "'s OAuth Workflow ===");
		System.out.println();

		// Obtain the Authorization URL
		System.out.println("Fetching the Authorization URL...");
		String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);
		System.out.println("Got the Authorization URL!");
		System.out.println("Now go and authorize Scribe here:");
		System.out.println(authorizationUrl);
		System.out.println("And paste the authorization code here");
		System.out.print(">>");
		Verifier verifier = new Verifier(in.nextLine());
		System.out.println();

		// Trade the Request Token and Verifier for the Access Token
		System.out.println("Trading the Request Token for an Access Token...");
		Token accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);
		System.out.println("Got the Access Token!");
		System.out.println("(if your curious it looks like this: "
				+ accessToken + " )");
		System.out.println();

		// Now let's go and ask for a protected resource!
		System.out.println("Now we're going to access a protected resource...");
		OAuthRequest request = new OAuthRequest(Verb.GET,
				PROTECTED_RESOURCE_URL);
		service.signRequest(accessToken, request);
		Response response = request.send();
		System.out.println("Got it! Lets see what we found...");
		System.out.println();
		System.out.println(response.getCode());
		System.out.println(response.getBody());
		
		OAuthRequest request2 = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL_2);
		service.signRequest(accessToken, request2);
		request2.addQuerystringParameter("oauth_consumer_key", apiKey);
		request2.addQuerystringParameter("openid", "6E2B92E99F8EAF71D016BCA14EB7ED9A");
		Response response2 = request2.send();
		System.out.println("Got it! Lets see what we found...");
		System.out.println();
		System.out.println(response2.getCode());
		System.out.println(response2.getBody());

		System.out.println();
		System.out
				.println("Thats it man! Go and build something awesome with Scribe! :)");

	}
}
