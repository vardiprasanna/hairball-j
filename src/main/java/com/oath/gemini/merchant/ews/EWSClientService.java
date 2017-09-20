package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oath.gemini.merchant.ClosableHttpClient;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;

public class EWSClientService {
    private String refreshToken;

    public EWSClientService(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Invoke HTTP POST to create a Gemini object
     */
    public <T> EWSResponseData<T> create(Class<T> responseType, Object requestBody, EWSEndpointEnum path, Object... params)
            throws Exception {
        return invoke(responseType, requestBody, HttpMethod.POST, path, params);
    }

    /**
     * Invoke HTTP DELETE to delete a Gemini object
     */
    public <T> EWSResponseData<T> delete(Class<T> responseType, EWSEndpointEnum path, Object... params) throws Exception {
        return invoke(responseType, null, HttpMethod.DELETE, path, params);
    }

    /**
     * Invoke HTTP GET to retrieve a requested Gemini object
     */
    public <T> EWSResponseData<T> get(Class<T> responseType, EWSEndpointEnum path, Object... params) throws Exception {
        return invoke(responseType, null, HttpMethod.GET, path, params);
    }

    /**
     * Invoke HTTP PUT to update a Gemini object
     */
    public <T> EWSResponseData<T> update(Class<T> responseType, Object requestBody, EWSEndpointEnum path, Object... params)
            throws Exception {
        return invoke(responseType, requestBody, HttpMethod.PUT, path, params);
    }

    /**
     * A main Gemini service function
     */
    @SuppressWarnings("unchecked")
    private <T> EWSResponseData<T> invoke(Class<T> responseType, Object requestBody, HttpMethod method, EWSEndpointEnum path,
            Object... params) throws Exception {

        EWSAccessTokenData tokens = EWSAuthentication.getAccessTokenFromRefreshToken(refreshToken);
        EWSResponseData<T> response = null;

        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            Request request = httpClient.newRequest(method, path.toString(), requestBody, null, params);

            // Send a request
            request.header(HttpHeader.ACCEPT, "application/json");
            request.header(HttpHeader.CONTENT_TYPE, "application/json");
            request.header(HttpHeader.AUTHORIZATION, "Bearer " + tokens.getAccessToken());
            Map<String, String> res = httpClient.send(Map.class);

            // Convert a raw response to a list of T objects
            if (res != null && res.get("response") != null) {
                ObjectMapper mapper = new ObjectMapper();
                Object rawResponse = res.get("response");
                T[] ewsObjects = null;

                if (rawResponse instanceof List<?>) {
                    List<?> rawObjectList = (List<?>) rawResponse;
                    ewsObjects = (T[]) (Array.newInstance(responseType, rawObjectList.size()));

                    for (int i = 0; i < rawObjectList.size(); i++) {
                        ewsObjects[i] = mapper.convertValue(rawObjectList.get(i), responseType);
                    }
                } else {
                    ewsObjects = (T[]) (Array.newInstance(responseType, 1));
                    ewsObjects[0] = mapper.convertValue(rawResponse, responseType);
                }

                response = mapper.convertValue(res, EWSResponseData.class);
                response.setObjects(ewsObjects);
            }
        }
        return response;
    }
}
