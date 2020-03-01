package com.grantech.purchase.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * @author ManJav
 *
 */
public class Verify {

	static public final String MARKET_CAFEBAZAAR = "cafebazaar";
	static public final String MARKET_MYKET = "myket";
	static public final String MARKET_ZARINPAL = "zarinpal";

	private int amount;
	private String token;
	private String market;
	private String product;
	private String packageName;
	private Properties props = ConfigUtils.getInstance().load(ConfigUtils.DEFAULT);

	public Verify(String packageName, String market, int amount, String product, String token) {
		this.token = token;
		this.amount = amount;
		this.market = market;
		this.product = product;
		this.packageName = packageName;
		this.perform();
	}

	private void perform() {
		if (!market.equals(MARKET_CAFEBAZAAR) && !market.equals(MARKET_MYKET) && !market.equals(MARKET_ZARINPAL)) {
			sendSuccessResult(1, 0, "", System.currentTimeMillis());
			return;
		}

		Properties result = new Properties();
		HttpUtils.Data data;
		data = verify();

		// send purchase data to client
		// if consumptionState is zero, its means the product consumed.
		if (data.statusCode == HttpStatus.SC_OK) {
			if (this.market.equals(MARKET_ZARINPAL)) {
				result.put("response", data.json.getInt("Status"));
				if (data.json.getInt("Status") == 100) {
					this.token = data.json.getString("RefID");
					sendSuccessResult(0, 0, "", System.currentTimeMillis());
				} else {
					send(result);
				}
				return;
			}

			sendSuccessResult(data.json.getInt("consumptionState"), data.json.getInt("purchaseState"),
					data.json.getString("developerPayload"), data.json.getLong("purchaseTime"));
		}

		// when product id or purchase token is wrong
		if (data.statusCode == HttpStatus.SC_NOT_FOUND) {
			result.put("response", data.statusCode);
			send(result);
			return;
		}

		// when access token expired
		if (data.statusCode == HttpStatus.SC_UNAUTHORIZED) {
			if (refreshAccessToken()) {
				perform();
			} else {
				result.put("response", data.statusCode);
				send(result);
				System.out.print("refresh access token faild.");
			}
			return;
		}

		// unknown error
		System.out.print("Unknown Error.");
	}

	private void send(Properties result) {
	}

	private void sendSuccessResult(int consumptionState, int purchaseState, String developerPayload, long purchaseTime) {
	}

	/**
	 * This method only called in initial setup
	 * 
	 * @return json string contains:<br/>
	 *         <b>"access_token"</b>: access token needs per verification.<br/>
	 *         <b>"token_type"</b>: "Bearer"<br/>
	 *         <b>"expires_in"</b>: after expires_in seconds, access_token
	 *         expired.<br/>
	 *         <b>"refresh_token"</b>: we need refresh token for get new access
	 *         token when expired.<br/>
	 *         <b>"scope"</b>: "androidpublisher"
	 */
	String requestAccessToken() {
		List<NameValuePair> argus = new ArrayList<>();
		argus.add(new BasicNameValuePair("grant_type", "authorization_code"));
		argus.add(new BasicNameValuePair("code", props.getProperty("cafebazaarAccessTokenCode")));
		argus.add(new BasicNameValuePair("client_id", props.getProperty("cafebazaarAccessTokenClientID")));
		argus.add(new BasicNameValuePair("client_secret", props.getProperty("cafebazaarAccessTokenClientSecret")));
		argus.add(new BasicNameValuePair("redirect_uri", "http://www.gerantech.com/tanks/test.php?a=b"));
		HttpUtils.Data data = HttpUtils.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus, true);
		System.out.print("request_AccessToken: " + data.statusCode + " " + data.text);
		return (data.text);
	}

	/**
	 * This method called when access token expired.<br/>
	 * Web request get json string contains:<br/>
	 * <b>"access_token"</b>: access token needs per verification.<br/>
	 * <b>"token_type"</b>: "Bearer"<br/>
	 * <b>"expires_in"</b>: after expires_in seconds, access_token expired.<br/>
	 * <b>"scope"</b>: "androidpublisher"
	 * 
	 * @return boolean value <br/>
	 *         if access dtoken refreshed return true else false
	 */
	Boolean refreshAccessToken() {
		List<NameValuePair> argus = new ArrayList<>();
		argus.add(new BasicNameValuePair("grant_type", "refresh_token"));
		argus.add(new BasicNameValuePair("client_id", props.getProperty("cafebazaarAccessTokenClientID")));
		argus.add(new BasicNameValuePair("client_secret", props.getProperty("cafebazaarAccessTokenClientSecret")));
		argus.add(new BasicNameValuePair("refresh_token", props.getProperty("cafebazaarAccessTokenRefreshToken")));
		HttpUtils.Data data = HttpUtils.post("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", argus, true);
		System.out.print("refresh_token: " + data.statusCode + " " + data.text);
		if (data.statusCode != HttpStatus.SC_OK || !data.json.containsKey("access_token"))
			return false;
		props.setProperty("cafebazaarAccessToken", data.json.getString("access_token"));
		ConfigUtils.getInstance().save(ConfigUtils.DEFAULT);
		return true;
	}

	/**
	 * Server side purchase verification method.<br/>
	 * Web request get json string if succeed, contains:<br/>
	 * <b>"consumptionState"</b>: if consumptionState is zero, thats mean product
	 * consumed.<br/>
	 * <b>"purchaseState"</b>: type of purchase.<br/>
	 * <b>"kind"</b>: "androidpublisher#inappPurchase"<br/>
	 * <b>"developerPayload"</b>: the payload that developer when started purchase
	 * flow send to this.market server.<br/>
	 * <b>"purchaseTime"</b>: purchase time in miliseconds<br/>
	 * 
	 * @return Data <br/>
	 */
	HttpUtils.Data verify() {
		// set headers
		Map<String, String> headers = new HashMap<>();
		if (this.market.equals(MARKET_MYKET)) {
			headers.put("X-Access-Token", props.getProperty("myketAccessToken"));
		} else if (this.market.equals(MARKET_ZARINPAL)) {
			headers.put("User-Agent", "Zarinpal REST");
			headers.put("Content-Type", "application/json");
		}

		// set url
		String url = null;
		if (this.market.equals(MARKET_ZARINPAL)) {
			url = "https://www.zarinpal.com/pg/rest/WebGate/PaymentVerification.json";
			List<NameValuePair> argus = new ArrayList<>();
			argus.add(new BasicNameValuePair("MerchantID", props.getProperty("zarinpalMerchantID")));
			argus.add(new BasicNameValuePair("Authority", token));
			argus.add(new BasicNameValuePair("Amount", amount + ""));
			HttpUtils.Data data = HttpUtils.post(url, argus, true, true);
			System.out.print("verify " + data.statusCode + " " + data.text);
			return data;
		}

		if (this.market.equals(MARKET_MYKET))
			url = "https://developer.myket.ir/api/applications/" + packageName + "/purchases/products/" + product
					+ "/tokens/" + token;
		else if (this.market.equals(MARKET_CAFEBAZAAR))
			url = "https://pardakht.cafebazaar.ir/devapi/v2/api/validate/" + packageName + "/inapp/" + product
					+ "/purchases/" + token + "/?access_token=" + props.getProperty("cafebazaarAccessToken");

		// system.out.print("purchase url:", url);
		HttpUtils.Data data = HttpUtils.get(url, headers, true);
		System.out.print("verify " + data.statusCode + " " + data.text);
		return data;
	}
}