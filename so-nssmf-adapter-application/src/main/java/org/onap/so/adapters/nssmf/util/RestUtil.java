/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2020 Huawei Technologies Co., Ltd. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.nssmf.util;

import jakarta.ws.rs.core.UriBuilder;
import java.net.SocketTimeoutException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.onap.aai.domain.yang.EsrSystemInfo;
import org.onap.aai.domain.yang.EsrSystemInfoList;
import org.onap.aai.domain.yang.EsrThirdpartySdnc;
import org.onap.aai.domain.yang.EsrThirdpartySdncList;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.so.adapters.nssmf.exceptions.ApplicationException;
import org.onap.so.adapters.nssmf.extclients.aai.AaiServiceProvider;
import org.onap.so.adapters.nssmf.entity.TokenRequest;
import org.onap.so.adapters.nssmf.entity.TokenResponse;
import org.onap.so.adapters.nssmf.enums.HttpMethod;
import org.onap.so.adapters.nssmf.entity.NssmfInfo;
import org.onap.so.adapters.nssmf.entity.RestResponse;
import org.onap.so.beans.nsmf.EsrInfo;
import org.onap.so.beans.nsmf.ServiceInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import static org.onap.so.adapters.nssmf.enums.HttpMethod.POST;
import static org.onap.so.adapters.nssmf.util.NssmfAdapterUtil.BAD_REQUEST;
import static org.onap.so.adapters.nssmf.util.NssmfAdapterUtil.marshal;
import static org.onap.so.adapters.nssmf.util.NssmfAdapterUtil.unMarshal;
import static org.onap.logging.filter.base.ErrorCode.AvailabilityError;
import static org.onap.so.logger.LoggingAnchor.FOUR;
import static org.onap.so.logger.MessageEnum.RA_NS_EXC;

@Component
@RequiredArgsConstructor
public class RestUtil {

    private static final Logger logger = LoggerFactory.getLogger(RestUtil.class);
    private static final int DEFAULT_TIME_OUT = 60000;
    private static final String NSSMI_ADAPTER = "NSSMI Adapter";
    private static final String TOKEN_URL = "/api/rest/securityManagement/v1" + "/oauth/token";
    private final AaiServiceProvider aaiSvcProv;
    @Qualifier("nssmfHttpClient")
    private final HttpClient httpClient;

    public void createServiceInstance(ServiceInstance serviceInstance, ServiceInfo serviceInfo) {
        aaiSvcProv.invokeCreateServiceInstance(serviceInstance, serviceInfo.getGlobalSubscriberId(),
                serviceInfo.getSubscriptionServiceType(), serviceInfo.getNssiId());
    }

    public ServiceInstance getServiceInstance(ServiceInfo serviceInfo) {
        return aaiSvcProv.invokeGetServiceInstance(serviceInfo.getGlobalSubscriberId(),
                serviceInfo.getSubscriptionServiceType(), serviceInfo.getNssiId());
    }

    public void deleteServiceInstance(ServiceInfo serviceInfo) {
        aaiSvcProv.invokeDeleteServiceInstance(serviceInfo.getGlobalSubscriberId(),
                serviceInfo.getSubscriptionServiceType(), serviceInfo.getNssiId());
    }

    public NssmfInfo getNssmfHost(EsrInfo esrInfo) throws ApplicationException {
        EsrThirdpartySdncList sdncList = aaiSvcProv.invokeGetThirdPartySdncList();
        if (sdncList != null && sdncList.getEsrThirdpartySdnc() != null) {
            for (EsrThirdpartySdnc sdncEsr : sdncList.getEsrThirdpartySdnc()) {

                EsrSystemInfoList sysInfoList =
                        aaiSvcProv.invokeGetThirdPartySdncEsrSystemInfo(sdncEsr.getThirdpartySdncId());

                if (sysInfoList != null && sysInfoList.getEsrSystemInfo() != null) {
                    for (EsrSystemInfo esr : sysInfoList.getEsrSystemInfo()) {
                        if (esr != null && esr.getType().equals(esrInfo.getNetworkType().getNetworkType())
                                && esr.getVendor().equals(esrInfo.getVendor())) {
                            logger.info("Found an entry with vendor name " + esrInfo.getVendor() + " and network type "
                                    + esrInfo.getNetworkType() + " in ESR.");
                            NssmfInfo nssmfInfo = new NssmfInfo();
                            nssmfInfo.setIpAddress(esr.getIpAddress());
                            nssmfInfo.setPort(esr.getPort());
                            nssmfInfo.setCacert(esr.getSslCacert());
                            nssmfInfo.setUserName(esr.getUserName());
                            nssmfInfo.setPassword(esr.getPassword());
                            String endPoint = UriBuilder.fromPath("").host(esr.getIpAddress())
                                    .port(Integer.valueOf(esr.getPort())).scheme("https").build().toString();
                            nssmfInfo.setUrl(endPoint);
                            return nssmfInfo;
                        }
                    }
                }

            }
        }

        throw new ApplicationException(BAD_REQUEST, "ESR information is improper");
    }


    public String getToken(NssmfInfo nssmfInfo) throws ApplicationException {


        TokenRequest req = new TokenRequest();
        req.setGrantType("password");
        req.setUserName(nssmfInfo.getUserName());
        req.setValue(nssmfInfo.getPassword());

        String tokenReq = marshal(req);

        logger.info("Sending token request to NSSMF: " + tokenReq);
        RestResponse tokenRes = send(nssmfInfo.getUrl() + TOKEN_URL, POST, tokenReq, null);

        TokenResponse res = unMarshal(tokenRes.getResponseContent(), TokenResponse.class);

        return res.getAccessToken();
    }


    public RestResponse send(String url, HttpMethod methodType, String content, Header header) {

        HttpUriRequestBase req = null;
        ClassicHttpResponse res = null;

        logger.debug("Beginning to send message {}: {}", methodType, url);

        try {
            int timeout = DEFAULT_TIME_OUT;

            RequestConfig config = RequestConfig.custom().setResponseTimeout(Timeout.ofMilliseconds(timeout))
                    .setConnectTimeout(Timeout.ofMilliseconds(timeout))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout)).build();
            logger.debug("Sending request to NSSMF: " + content);
            req = getHttpReq(url, methodType, header, config, content);
            res = (ClassicHttpResponse) httpClient.execute(req);

            String resContent = null;
            if (res.getEntity() != null) {
                resContent = EntityUtils.toString(res.getEntity(), "UTF-8");
            }

            int statusCode = res.getCode();
            String statusMessage = res.getReasonPhrase();
            logger.info("NSSMF Response: {} {}", statusCode,
                    statusMessage + (resContent == null ? "" : System.lineSeparator() + resContent));

            if (res.getCode() >= 300) {
                String errMsg = "{\n  \"errorCode\": " + res.getCode() + "\n  \"errorDescription\": " + statusMessage
                        + "\n}";
                logError(errMsg);
                return createResponse(statusCode, errMsg);
            }
            if (null != req) {
                req.reset();
            }
            req = null;

            return createResponse(statusCode, resContent);

        } catch (SocketTimeoutException e) {
            String errMsg = "Request to NSSMF timed out";
            logError(errMsg, e);
            return createResponse(408, errMsg);
        } catch (Exception e) {
            String errMsg = "Error processing request to NSSMF";
            logError(errMsg, e);
            return createResponse(500, errMsg);
        } finally {
            if (res != null) {
                try {
                    EntityUtils.consume(res.getEntity());
                } catch (Exception e) {
                    logger.debug("Exception :", e);
                }
            }
            if (req != null) {
                try {
                    req.reset();
                } catch (Exception e) {
                    logger.debug("Exception :", e);
                }
            }
        }
    }

    public RestResponse createResponse(int statusCode, String errMsg) {
        RestResponse restResponse = new RestResponse();
        restResponse.setStatus(statusCode);
        restResponse.setResponseContent(errMsg);
        return restResponse;
    }

    private HttpUriRequestBase getHttpReq(String url, HttpMethod method, Header header, RequestConfig config,
            String content) throws ApplicationException {
        HttpUriRequestBase base;
        switch (method) {
            case POST:
                HttpPost post = new HttpPost(url);
                post.setEntity(new StringEntity(content, APPLICATION_JSON));
                base = post;
                break;

            case GET:
                HttpGet get = new HttpGet(url);
                if (content != null) {
                    get.setEntity(new StringEntity(content, APPLICATION_JSON));
                }
                base = get;
                break;

            case PUT:
                HttpPut put = new HttpPut(url);
                put.setEntity(new StringEntity(content, APPLICATION_JSON));
                base = put;
                break;

            case PATCH:
                base = new HttpPatch(url);
                break;

            case DELETE:
                HttpDelete delete = new HttpDelete(url);
                if (content != null) {
                    delete.setEntity(new StringEntity(content, APPLICATION_JSON));
                }
                base = delete;
                break;
            default:
                throw new ApplicationException(404, "invalid method: " + method);

        }
        base.setConfig(config);
        if (header != null) {
            base.setHeader(header);
        }
        return base;
    }

    public RestResponse sendRequest(String allocateUrl, HttpMethod post, String allocateReq, EsrInfo esrInfo)
            throws ApplicationException {
        NssmfInfo nssmfInfo = getNssmfHost(esrInfo);
        Header header = new BasicHeader("X-Auth-Token", getToken(nssmfInfo));
        String nssmfUrl = nssmfInfo.getUrl() + allocateUrl;
        return send(nssmfUrl, post, allocateReq, header);
    }

    private static void logError(String errMsg, Throwable t) {
        logger.error(FOUR, RA_NS_EXC.toString(), NSSMI_ADAPTER, AvailabilityError.getValue(), errMsg, t);
    }

    private static void logError(String errMsg) {
        logger.error(FOUR, RA_NS_EXC.toString(), NSSMI_ADAPTER, AvailabilityError.toString(), errMsg);
    }
}
