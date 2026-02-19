/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 # Copyright (c) 2020, CMCC Technologies Co., Ltd.
 #
 # Licensed under the Apache License, Version 2.0 (the "License")
 # you may not use this file except in compliance with the License.
 # You may obtain a copy of the License at
 #
 #       http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing, software
 # distributed under the License is distributed on an "AS IS" BASIS,
 # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 # See the License for the specific language governing permissions and
 # limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.nssmf.service.impl;


import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.onap.so.adapters.nssmf.extclients.aai.AaiServiceProvider;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.so.adapters.nssmf.config.NssmfAdapterConfig;
import org.onap.so.adapters.nssmf.consts.NssmfAdapterConsts;
import org.onap.so.adapters.nssmf.entity.NssmfInfo;
import org.onap.so.adapters.nssmf.entity.TokenResponse;
import org.onap.so.adapters.nssmf.enums.ActionType;
import org.onap.so.adapters.nssmf.enums.HttpMethod;
import org.onap.so.adapters.nssmf.exceptions.ApplicationException;
import org.onap.so.adapters.nssmf.util.RestUtil;
import org.onap.so.beans.nsmf.ActDeActNssi;
import org.onap.so.beans.nsmf.AllocateAnNssi;
import org.onap.so.beans.nsmf.AllocateCnNssi;
import org.onap.so.beans.nsmf.AllocateTnNssi;
import org.onap.so.beans.nsmf.AnSliceProfile;
import org.onap.so.beans.nsmf.ConnectionLink;
import org.onap.so.beans.nsmf.CnSliceProfile;
import org.onap.so.beans.nsmf.DeAllocateNssi;
import org.onap.so.beans.nsmf.EsrInfo;
import org.onap.so.beans.nsmf.JobStatusResponse;
import org.onap.so.beans.nsmf.PerfReq;
import org.onap.so.beans.nsmf.PerfReqEmbb;
import org.onap.so.beans.nsmf.QuerySubnetCapability;
import org.onap.so.beans.nsmf.NetworkType;
import org.onap.so.beans.nsmf.NsiInfo;
import org.onap.so.beans.nsmf.NssiResponse;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.onap.so.beans.nsmf.NssmfRequest;
import org.onap.so.beans.nsmf.ResponseDescriptor;
import org.onap.so.beans.nsmf.ServiceInfo;
import org.onap.so.beans.nsmf.TnSliceProfile;
import org.onap.so.beans.nsmf.TransportSliceNetwork;
import org.onap.so.beans.nsmf.UeMobilityLevel;
import org.onap.so.db.request.beans.ResourceOperationStatus;
import org.onap.so.db.request.data.repository.ResourceOperationStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.onap.so.adapters.nssmf.util.NssmfAdapterUtil.marshal;
import static org.onap.so.adapters.nssmf.util.NssmfAdapterUtil.unMarshal;
import static org.onap.so.beans.nsmf.NetworkType.ACCESS;
import static org.onap.so.beans.nsmf.NetworkType.CORE;
import static org.onap.so.beans.nsmf.NetworkType.TRANSPORT;
import static org.onap.so.beans.nsmf.ResourceSharingLevel.NON_SHARED;

@RunWith(SpringRunner.class)
public class NssmfManagerServiceImplTest {

    private RestUtil restUtil;

    @Mock
    private AaiServiceProvider aaiSvcProv;

    private NssmfManagerServiceImpl nssiManagerService;

    @Mock
    private HttpResponse tokenResponse;

    @Mock
    private HttpEntity tokenEntity;

    @Mock
    private HttpResponse commonResponse;

    @Mock
    private HttpEntity commonEntity;

    @Mock
    private StatusLine statusLine;

    @Mock
    private HttpClient httpClient;

    private InputStream postStream;

    private InputStream tokenStream;

    @Mock
    private NssmfAdapterConfig adapterConfig;

    @Mock
    private ResourceOperationStatusRepository repository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        restUtil = Mockito.spy(new RestUtil(aaiSvcProv, httpClient));

        nssiManagerService = new NssmfManagerServiceImpl(this.restUtil, this.repository, this.adapterConfig);
    }

    private void createCommonMock(int statusCode, NssmfInfo nssmf) throws Exception {
        doReturn("7512eb3feb5249eca5ddd742fedddd39").when(restUtil).getToken(any(NssmfInfo.class));
        doReturn(new ServiceInstance()).when(restUtil).getServiceInstance(any());

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        doReturn(nssmf).when(restUtil).getNssmfHost(any(EsrInfo.class));

        when(tokenResponse.getEntity()).thenReturn(tokenEntity);
        when(tokenResponse.getStatusLine()).thenReturn(statusLine);
        when(tokenEntity.getContent()).thenReturn(tokenStream);

        when(commonResponse.getEntity()).thenReturn(commonEntity);
        when(commonResponse.getStatusLine()).thenReturn(statusLine);
        when(commonEntity.getContent()).thenReturn(postStream);

        when(adapterConfig.getInfraAuth()).thenReturn("SW5mcmFQb3J0YWxDbGllbnQ6cGFzc3dvcmQxJA==");

        Answer<HttpResponse> answer = invocation -> {
            Object[] arguments = invocation.getArguments();
            if (arguments != null && arguments.length == 1 && arguments[0] != null) {

                HttpRequestBase base = (HttpRequestBase) arguments[0];
                if (base.getURI().toString().endsWith("/oauth/token")) {
                    return tokenResponse;
                } else {
                    return commonResponse;
                }
            }
            return commonResponse;
        };

        doAnswer(answer).when(httpClient).execute(any(HttpRequestBase.class));

    }

    @Test
    public void allocateCnNssiTest() throws Exception {
        allocateNssi(CORE);

    }

    @Test
    public void allocateTnNssiTest() throws Exception {
        allocateNssi(TRANSPORT);
    }

    @Test
    public void allocateAnNssiTest() throws Exception {
        allocateNssi(ACCESS);
    }


    public void allocateNssi(NetworkType domainType) throws Exception {
        NssmfInfo nssmf = new NssmfInfo();
        nssmf.setUserName("nssmf-user");
        nssmf.setPassword("nssmf-pass");
        nssmf.setPort("8080");
        nssmf.setIpAddress("127.0.0.1");
        nssmf.setUrl("http://127.0.0.1:8080");

        NssiResponse nssiRes = new NssiResponse();
        nssiRes.setJobId("4b45d919816ccaa2b762df5120f72067");
        nssiRes.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");

        TokenResponse token = new TokenResponse();
        token.setAccessToken("7512eb3feb5249eca5ddd742fedddd39");
        token.setExpires(1800);

        postStream = new ByteArrayInputStream(marshal(nssiRes).getBytes(UTF_8));
        tokenStream = new ByteArrayInputStream(marshal(token).getBytes(UTF_8));

        createCommonMock(200, nssmf);
        NssmfAdapterNBIRequest nbiRequest = null;

        switch (domainType) {
            case CORE:
                nbiRequest = createCnAllocateNssi();
                break;
            case TRANSPORT:
                nbiRequest = createTnAllocateNssi();
                break;
            case ACCESS:
                nbiRequest = createAnAllocateNssi();
                break;
        }

        assertNotNull(nbiRequest);
        System.out.println(marshal(nbiRequest));
        ResponseEntity res = nssiManagerService.allocateNssi(nbiRequest);
        assertNotNull(res);
        assertNotNull(res.getBody());
        NssiResponse allRes = unMarshal(res.getBody().toString(), NssiResponse.class);
        if (!domainType.equals(ACCESS)) {
            assertEquals(allRes.getJobId(), "4b45d919816ccaa2b762df5120f72067");
            assertEquals(allRes.getNssiId(), "NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        }

        System.out.println(res);
    }



    private NssmfAdapterNBIRequest createCnAllocateNssi() {
        CnSliceProfile sP = new CnSliceProfile();
        List<String> sns = new LinkedList<>();
        sns.add("001-100001");
        List<String> plmn = new LinkedList<>();
        plmn.add("460-00");
        plmn.add("460-01");
        PerfReqEmbb embb = new PerfReqEmbb();
        embb.setActivityFactor(50);
        List<PerfReqEmbb> embbList = new LinkedList<>();
        embbList.add(embb);
        PerfReq perfReq = new PerfReq();
        perfReq.setPerfReqEmbbList(embbList);
        List<String> taList = new LinkedList<>();
        taList.add("1");
        taList.add("2");
        taList.add("3");
        sP.setSnssaiList(sns);
        sP.setSliceProfileId("ab9af40f13f721b5f13539d87484098");
        sP.setPLMNIdList(plmn);
        sP.setPerfReq(perfReq);
        sP.setMaxNumberOfUEs(200);
        sP.setCoverageAreaTAList(taList);
        sP.setLatency(6);
        sP.setResourceSharingLevel(NON_SHARED);
        NsiInfo nsiInfo = new NsiInfo();
        nsiInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        nsiInfo.setNsiName("eMBB-001");
        AllocateCnNssi cnNssi = new AllocateCnNssi();
        cnNssi.setNssiId("NSST-C-001-HDBNJ-NSSMF-01-A-ZX");
        cnNssi.setNssiName("eMBB-001");
        cnNssi.setScriptName("CN1");
        cnNssi.setSliceProfile(sP);
        cnNssi.setNsiInfo(nsiInfo);

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(CORE);
        nbiRequest.setAllocateCnNssi(cnNssi);
        return nbiRequest;
    }

    private NssmfAdapterNBIRequest createAnAllocateNssi() {

        AnSliceProfile sP = new AnSliceProfile();
        List<String> sns = new LinkedList<>();
        sns.add("001-100001");
        List<String> plmn = new LinkedList<>();
        plmn.add("460-00");
        plmn.add("460-01");
        PerfReqEmbb embb = new PerfReqEmbb();
        embb.setActivityFactor(50);
        List<PerfReqEmbb> embbList = new LinkedList<>();
        embbList.add(embb);
        PerfReq perfReq = new PerfReq();
        perfReq.setPerfReqEmbbList(embbList);
        List<Integer> taList = new LinkedList<>();
        taList.add(1);
        taList.add(2);
        taList.add(3);
        sP.setSNSSAIList(sns);
        sP.setSliceProfileId("ab9af40f13f721b5f13539d87484098");
        sP.setPLMNIdList(plmn);
        sP.setPerfReq(perfReq);
        sP.setMaxNumberOfUEs(200);
        sP.setCoverageAreaTAList(taList);
        sP.setLatency(6);
        sP.setResourceSharingLevel(NON_SHARED);
        sP.setUeMobilityLevel(UeMobilityLevel.STATIONARY);
        sP.setResourceSharingLevel(NON_SHARED);
        NsiInfo nsiInfo = new NsiInfo();
        nsiInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        nsiInfo.setNsiName("eMBB-001");
        AllocateAnNssi anNssi = new AllocateAnNssi();
        anNssi.setNssiId("NSST-C-001-HDBNJ-NSSMF-01-A-ZX");
        anNssi.setNssiName("eMBB-001");
        anNssi.setScriptName("CN1");
        anNssi.setSliceProfile(sP);
        anNssi.setNsiInfo(nsiInfo);

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(ACCESS);
        nbiRequest.setAllocateAnNssi(anNssi);
        return nbiRequest;
    }


    private NssmfAdapterNBIRequest createTnAllocateNssi() {
        TnSliceProfile sP = new TnSliceProfile();
        List<String> sns = new LinkedList<>();
        sns.add("01-1EB5BA40");
        List<String> plmn = new LinkedList<>();
        plmn.add("460-00");
        PerfReqEmbb embb = new PerfReqEmbb();
        embb.setActivityFactor(50);
        List<PerfReqEmbb> embbList = new LinkedList<>();
        embbList.add(embb);

        sP.setSNSSAIList(sns);
        sP.setSliceProfileId("fec94836-87a0-41dc-a199-0ad9aa3890d1");
        sP.setPLMNIdList(plmn);
        sP.setLatency(10);
        sP.setMaxBandwidth(1000);
        sP.setJitter(10);

        List<TransportSliceNetwork> networks = new LinkedList<>();
        TransportSliceNetwork network = new TransportSliceNetwork();
        List<ConnectionLink> connectionLinks = new LinkedList<>();
        ConnectionLink connectionLink = new ConnectionLink();
        connectionLink.setTransportEndpointA("a47c76e3-c010-4eaf-adbb-0ba264118cab");
        connectionLink.setTransportEndpointB("c0c83e33-80d2-43da-b6cb-17b930420d74");
        connectionLinks.add(connectionLink);
        network.setConnectionLinks(connectionLinks);
        networks.add(network);

        NsiInfo nsiInfo = new NsiInfo();
        nsiInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        nsiInfo.setNsiName("eMBB-001");
        AllocateTnNssi tnNssi = new AllocateTnNssi();
        tnNssi.setTransportSliceNetworks(networks);
        tnNssi.setScriptName("TN");
        tnNssi.setSliceProfile(sP);
        tnNssi.setNsiInfo(nsiInfo);

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(TRANSPORT);
        nbiRequest.setAllocateTnNssi(tnNssi);
        return nbiRequest;
    }

    @Test
    public void deAllocateCnNssi() throws Exception {
        deAllocateNssi(CORE);
    }

    @Test
    public void deAllocateAnNssi() throws Exception {
        deAllocateNssi(ACCESS);
    }

    @Test
    public void deAllocateTnNssi() throws Exception {
        deAllocateNssi(TRANSPORT);
    }

    public void deAllocateNssi(NetworkType domainType) throws Exception {
        DeAllocateNssi deAllocateNssi = new DeAllocateNssi();
        deAllocateNssi.setTerminateNssiOption(0);
        List<String> snssai = new LinkedList<>();
        snssai.add("001-100001");
        deAllocateNssi.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        deAllocateNssi.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        deAllocateNssi.setScriptName("CN1");
        deAllocateNssi.setSnssaiList(snssai);

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(domainType);
        nbiRequest.setDeAllocateNssi(deAllocateNssi);

        NssmfInfo nssmf = new NssmfInfo();
        nssmf.setUserName("nssmf-user");
        nssmf.setPassword("nssmf-pass");
        nssmf.setPort("8080");
        nssmf.setIpAddress("127.0.0.1");

        NssiResponse nssiRes = new NssiResponse();
        nssiRes.setJobId("4b45d919816ccaa2b762df5120f72067");

        TokenResponse token = new TokenResponse();
        token.setAccessToken("7512eb3feb5249eca5ddd742fedddd39");
        token.setExpires(1800);

        postStream = new ByteArrayInputStream(marshal(nssiRes).getBytes(UTF_8));
        tokenStream = new ByteArrayInputStream(marshal(token).getBytes(UTF_8));

        createCommonMock(202, nssmf);
        ResponseEntity res = nssiManagerService.deAllocateNssi(nbiRequest, "ab9af40f13f721b5f13539d87484098");
        assertNotNull(res);
        assertNotNull(res.getBody());
        NssiResponse allRes = unMarshal(res.getBody().toString(), NssiResponse.class);
        if (!domainType.equals(ACCESS)) {
            assertEquals(allRes.getJobId(), "4b45d919816ccaa2b762df5120f72067");
        }
    }

    @Test
    public void activateAnNssi() throws Exception {
        activateNssi(ACCESS);
    }


    @Test
    public void activateCnNssi() throws Exception {
        activateNssi(CORE);
    }


    @Test
    public void activateTnNssi() throws Exception {
        activateNssi(TRANSPORT);
    }


    private void activateNssi(NetworkType domainType) throws Exception {
        NssmfInfo nssmf = new NssmfInfo();
        nssmf.setUserName("nssmf-user");
        nssmf.setPassword("nssmf-pass");
        nssmf.setPort("8080");
        nssmf.setIpAddress("127.0.0.1");

        NssiResponse nssiRes = new NssiResponse();
        nssiRes.setJobId("4b45d919816ccaa2b762df5120f72067");

        TokenResponse token = new TokenResponse();
        token.setAccessToken("7512eb3feb5249eca5ddd742fedddd39");
        token.setExpires(1800);

        postStream = new ByteArrayInputStream(marshal(nssiRes).getBytes(UTF_8));
        tokenStream = new ByteArrayInputStream(marshal(token).getBytes(UTF_8));

        ActDeActNssi act = new ActDeActNssi();
        act.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        act.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(domainType);
        nbiRequest.setActDeActNssi(act);

        createCommonMock(200, nssmf);
        ResponseEntity res = nssiManagerService.activateNssi(nbiRequest, "001-100001");
        assertNotNull(res);
        assertNotNull(res.getBody());
        NssiResponse allRes = unMarshal(res.getBody().toString(), NssiResponse.class);
        if (!domainType.equals(ACCESS)) {
            assertEquals(allRes.getJobId(), "4b45d919816ccaa2b762df5120f72067");
        }
    }

    @Test
    public void deActivateNssi() throws Exception {
        NssmfInfo nssmf = new NssmfInfo();
        nssmf.setUserName("nssmf-user");
        nssmf.setPassword("nssmf-pass");
        nssmf.setPort("8080");
        nssmf.setIpAddress("127.0.0.1");

        NssiResponse nssiRes = new NssiResponse();
        nssiRes.setJobId("4b45d919816ccaa2b762df5120f72067");

        TokenResponse token = new TokenResponse();
        token.setAccessToken("7512eb3feb5249eca5ddd742fedddd39");
        token.setExpires(1800);

        postStream = new ByteArrayInputStream(marshal(nssiRes).getBytes(UTF_8));
        tokenStream = new ByteArrayInputStream(marshal(token).getBytes(UTF_8));

        ActDeActNssi act = new ActDeActNssi();
        act.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        act.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(CORE);
        nbiRequest.setActDeActNssi(act);

        createCommonMock(200, nssmf);
        ResponseEntity res = nssiManagerService.deActivateNssi(nbiRequest, "001-100001");
        assertNotNull(res);
        assertNotNull(res.getBody());
        NssiResponse allRes = unMarshal(res.getBody().toString(), NssiResponse.class);
        assertEquals(allRes.getJobId(), "4b45d919816ccaa2b762df5120f72067");
    }

    @Test
    public void modifyAnNssi() throws Exception {
        modifyNssi(ACCESS);
    }


    @Test
    public void modifyCnNssi() throws Exception {
        modifyNssi(CORE);
    }

    public void modifyNssi(NetworkType domainType) throws Exception {
        NssmfInfo nssmf = new NssmfInfo();
        nssmf.setUserName("nssmf-user");
        nssmf.setPassword("nssmf-pass");
        nssmf.setPort("8080");
        nssmf.setIpAddress("127.0.0.1");
        nssmf.setUrl("http://127.0.0.1:8080");

        NssiResponse nssiRes = new NssiResponse();
        nssiRes.setJobId("4b45d919816ccaa2b762df5120f72067");
        nssiRes.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");

        TokenResponse token = new TokenResponse();
        token.setAccessToken("7512eb3feb5249eca5ddd742fedddd39");
        token.setExpires(1800);

        postStream = new ByteArrayInputStream(marshal(nssiRes).getBytes(UTF_8));
        tokenStream = new ByteArrayInputStream(marshal(token).getBytes(UTF_8));

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(domainType);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceUuid("8ee5926d-720b-4bb2-86f9-d20e921c143b");
        serviceInfo.setServiceInvariantUuid("e75698d9-925a-4cdd-a6c0-edacbe6a0b51");
        serviceInfo.setGlobalSubscriberId("5GCustomer");
        serviceInfo.setServiceType("5G");
        serviceInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        serviceInfo.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");

        AllocateCnNssi cnNssi = new AllocateCnNssi();
        cnNssi.setNssiId("NSST-C-001-HDBNJ-NSSMF-01-A-ZX");

        AllocateAnNssi anNssi = new AllocateAnNssi();
        anNssi.setNssiId("NSST-C-001-HDBNJ-NSSMF-01-A-ZX");

        nbiRequest.setServiceInfo(serviceInfo);
        nbiRequest.setAllocateCnNssi(cnNssi);
        nbiRequest.setAllocateAnNssi(anNssi);

        createCommonMock(200, nssmf);
        ResponseEntity res = nssiManagerService.allocateNssi(nbiRequest);
        assertNotNull(res);
        assertNotNull(res.getBody());
        NssiResponse allRes = unMarshal(res.getBody().toString(), NssiResponse.class);
        if (!domainType.equals(ACCESS)) {
            assertEquals(allRes.getJobId(), "4b45d919816ccaa2b762df5120f72067");
        }
        assertNotNull(allRes);
    }

    @Test
    public void testNssmfRequest() throws ApplicationException {
        NssmfRequest nssmfRequest = new NssmfRequest();
        String sst = marshal(nssmfRequest);
        System.out.println(sst);
    }

    @Test
    public void queryCnAllocateJobStatus() throws Exception {
        queryJobStatus(CORE, ActionType.ALLOCATE.toString());
    }

    @Test
    public void queryCnActivateJobStatus() throws Exception {
        queryJobStatus(CORE, ActionType.ACTIVATE.toString());
    }

    @Test
    public void queryCnDeActivateJobStatus() throws Exception {
        queryJobStatus(CORE, ActionType.DEACTIVATE.toString());
    }

    @Test
    public void queryAnJobStatus() throws Exception {
        queryJobStatus(ACCESS, ActionType.ALLOCATE.toString());
    }

    public void queryJobStatus(NetworkType domainType, String action) throws Exception {
        NssmfInfo nssmf = new NssmfInfo();
        nssmf.setUserName("nssmf-user");
        nssmf.setPassword("nssmf-pass");
        nssmf.setPort("8080");
        nssmf.setIpAddress("127.0.0.1");

        JobStatusResponse jobStatusResponse = new JobStatusResponse();
        ResponseDescriptor descriptor = new ResponseDescriptor();
        descriptor.setResponseId("7512eb3feb5249eca5ddd742fedddd39");
        descriptor.setProgress(100);
        descriptor.setStatusDescription("Initiating VNF Instance");
        descriptor.setStatus("FINISHED");
        jobStatusResponse.setResponseDescriptor(descriptor);

        TokenResponse token = new TokenResponse();
        token.setAccessToken("7512eb3feb5249eca5ddd742fedddd39");
        token.setExpires(1800);

        postStream = new ByteArrayInputStream(marshal(jobStatusResponse).getBytes(UTF_8));
        tokenStream = new ByteArrayInputStream(marshal(token).getBytes(UTF_8));

        ResourceOperationStatus operationStatus = new ResourceOperationStatus();
        operationStatus.setOperationId("4b45d919816ccaa2b762df5120f72067");
        operationStatus.setResourceTemplateUUID("8ee5926d-720b-4bb2-86f9-d20e921c143b");
        operationStatus.setServiceId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        operationStatus.setOperType(action);

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(domainType);
        nbiRequest.setResponseId("7512eb3feb5249eca5ddd742fedddd39");
        List<ResourceOperationStatus> optional = new ArrayList<>();
        optional.add(operationStatus);

        doAnswer(invocation -> optional).when(repository).findByServiceIdAndOperationId(any(), any());

        createCommonMock(200, nssmf);

        ResponseEntity res = nssiManagerService.queryJobStatus(nbiRequest, "4b45d919816ccaa2b762df5120f72067");
        assertNotNull(res);
        assertNotNull(res.getBody());
        JobStatusResponse allRes = unMarshal(res.getBody().toString(), JobStatusResponse.class);
        assertEquals(allRes.getResponseDescriptor().getProgress(), 100);
        assertEquals(allRes.getResponseDescriptor().getStatus(), "FINISHED");
        if (!domainType.equals(ACCESS)) {
            assertEquals(allRes.getResponseDescriptor().getResponseId(), "7512eb3feb5249eca5ddd742fedddd39");
        }

    }

    @Test
    public void queryNSSISelectionCapability() throws Exception {

        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(CORE);
        ResponseEntity res = nssiManagerService.queryNSSISelectionCapability(nbiRequest);
        assertNotNull(res);
        assertNotNull(res.getBody());
        Map allRes = unMarshal(res.getBody().toString(), Map.class);
        assertEquals(allRes.get("selection"), "NSMF");

        System.out.println(res);

        nbiRequest.getEsrInfo().setVendor(NssmfAdapterConsts.ONAP_INTERNAL_TAG);
        res = nssiManagerService.queryNSSISelectionCapability(nbiRequest);
        assertNotNull(res);
        assertNotNull(res.getBody());
        allRes = unMarshal(res.getBody().toString(), Map.class);
        assertEquals(allRes.get("selection"), "NSSMF");

        System.out.println(res);

        nbiRequest.getEsrInfo().setNetworkType(NetworkType.ACCESS);
        res = nssiManagerService.queryNSSISelectionCapability(nbiRequest);
        assertNotNull(res);
        assertNotNull(res.getBody());
        allRes = unMarshal(res.getBody().toString(), Map.class);
        assertEquals(allRes.get("selection"), "NSSMF");

        System.out.println(res);
    }

    private NssmfAdapterNBIRequest createNbiRequest(NetworkType networkType) {
        NssmfAdapterNBIRequest nbiRequest = new NssmfAdapterNBIRequest();
        EsrInfo esrInfo = new EsrInfo();
        switch (networkType) {
            case CORE:
                esrInfo.setVendor("huawei");
                esrInfo.setNetworkType(CORE);
                break;
            case TRANSPORT:
                esrInfo.setVendor("ONAP_internal");
                esrInfo.setNetworkType(TRANSPORT);
                break;
            case ACCESS:
                esrInfo.setVendor("huawei");
                esrInfo.setNetworkType(ACCESS);
                break;
        }
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceUuid("8ee5926d-720b-4bb2-86f9-d20e921c143b");
        serviceInfo.setServiceInvariantUuid("e75698d9-925a-4cdd-a6c0-edacbe6a0b51");
        serviceInfo.setGlobalSubscriberId("5GCustomer");
        serviceInfo.setServiceType("5G");
        serviceInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        nbiRequest.setEsrInfo(esrInfo);
        nbiRequest.setServiceInfo(serviceInfo);
        return nbiRequest;
    }

    @Test
    public void querySubnetCapability() {
        NssmfAdapterNBIRequest nbiRequest = createNbiRequest(CORE);

        QuerySubnetCapability subnetCapabilityQuery = new QuerySubnetCapability();
        List<String> subnetTypes = Arrays.asList("CN");
        subnetCapabilityQuery.setSubnetTypes(subnetTypes);
        nbiRequest.setSubnetCapabilityQuery(subnetCapabilityQuery);
        ResponseEntity res = nssiManagerService.querySubnetCapability(nbiRequest);
        assertNotNull(res);
        assertNotNull(res.getBody());
    }
}
