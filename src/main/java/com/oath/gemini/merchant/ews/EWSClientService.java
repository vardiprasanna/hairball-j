package com.oath.gemini.merchant.ews;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oath.gemini.merchant.ClosableHttpClient;
import com.oath.gemini.merchant.HttpStatus;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import lombok.Getter;

/**
 * @author tong on 10/1/2017
 */
@Getter
public class EWSClientService {
    EWSAccessTokenData tokens;

    public EWSClientService(EWSAccessTokenData tokens) {
        this.tokens = tokens;
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
     * Start an async report job
     */
    public <T> EWSResponseData<T> job(Class<T> responseType, Object requestBody, EWSEndpointEnum path, Object... params) throws Exception {
        return invoke(responseType, requestBody, HttpMethod.POST, path, params);
    }

    /**
     * A main Gemini service function
     */
    @SuppressWarnings("unchecked")
    private <T> EWSResponseData<T> invoke(Class<T> responseType, Object requestBody, HttpMethod method, EWSEndpointEnum path,
            Object... params) throws Exception {

        EWSResponseData<T> response = null;

        try (ClosableHttpClient httpClient = new ClosableHttpClient()) {
            Request request = httpClient.newRequest(method, path.toString(), requestBody, null, params);

            // Send a request
            request.header(HttpHeader.ACCEPT, MediaType.APPLICATION_JSON);
            request.header(HttpHeader.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            request.header(HttpHeader.AUTHORIZATION, "Bearer " + tokens.getAccessToken());
            Map<String, Object> res = httpClient.send(Map.class);

            // Check an error first
            if (res != null && res.get("HttpStatus") != null) {
                HttpStatus httpStatus = (HttpStatus) res.get("HttpStatus");

                // if (!httpStatus.isOk()) {
                // response = new EWSResponseData<>();
                // response.setErrors(httpStatus.getMessage());
                // response.setStatus(httpStatus.getStatus());
                // return response;
                // }
                if (!httpStatus.isOk()) {
                    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);git 
                    JsonNode errorMsg = mapper.readTree(httpStatus.getMessage());
                    response = new EWSResponseData<>();

                    if (errorMsg.has("response")) {
                        JsonNode responseNode = errorMsg.get("response");
                        T[] ewsObjects = null;

                        if (responseNode.isArray()) {
                            ewsObjects = mapper.convertValue(responseNode, new TypeReference<T[]>() {
                            });
                        } else {
                            T obj = mapper.convertValue(responseNode, responseType);
                            if (obj != null) {
                                ewsObjects = (T[]) (Array.newInstance(responseType, 1));
                                ewsObjects[0] = obj;
                            }
                        }
                        response.setObjects(ewsObjects);
                    }

                    if (errorMsg.has("errors")) {
                        JsonNode errorNode = errorMsg.get("errors");
                        if (errorNode.has(0) && errorNode.get(0).has("message")) {
                            response.setBrief(errorNode.get(0).get("message").asText());
                        }
                    }
                    if (errorMsg.has("timestamp")) {
                        response.setTimestamp(errorMsg.get("timestamp").toString());
                    }
                    response.setErrors(httpStatus.getMessage());
                    response.setStatus(httpStatus.getStatus());
                    return response;
                }
            }

            // Convert a raw response to a list of T objects
            if (res != null && res.get("response") != null) {
                ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
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
