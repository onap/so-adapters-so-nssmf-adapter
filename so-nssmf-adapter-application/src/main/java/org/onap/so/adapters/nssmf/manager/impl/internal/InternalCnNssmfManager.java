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

package org.onap.so.adapters.nssmf.manager.impl.internal;

import org.onap.so.adapters.nssmf.exceptions.ApplicationException;
import org.onap.so.adapters.nssmf.manager.impl.InternalNssmfManager;
import org.onap.so.beans.nsmf.AllocateCnNssi;
import org.onap.so.beans.nsmf.CnSliceProfile;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.onap.so.beans.nsmf.NssmfRequest;
import java.util.HashMap;
import java.util.Map;
import static org.onap.so.adapters.nssmf.util.NssmfAdapterUtil.marshal;

public class InternalCnNssmfManager extends InternalNssmfManager {

    @Override
    protected String doWrapAllocateReqBody(NssmfAdapterNBIRequest nbiRequest) throws ApplicationException {

        NssmfRequest request =
                new NssmfRequest(serviceInfo, nbiRequest.getEsrInfo().getNetworkType(), nbiRequest.getAllocateCnNssi());
        request.setName(nbiRequest.getAllocateCnNssi().getNssiName());
        return marshal(request);
    }

    @Override
    protected String doWrapModifyReqBody(NssmfAdapterNBIRequest nbiRequest) throws ApplicationException {
        AllocateCnNssi allocateCnNssi = nbiRequest.getAllocateCnNssi();
        CnSliceProfile cnSliceProfile = allocateCnNssi.getSliceProfile();
        Map<String, Object> additional = new HashMap<>();
        additional.put("modifyAction", "allocate");
        additional.put("nsiInfo", allocateCnNssi.getNsiInfo());
        additional.put("scriptName", allocateCnNssi.getScriptName());
        additional.put("snssaiList", cnSliceProfile.getSnssaiList());
        additional.put("sliceProfileId", cnSliceProfile.getSliceProfileId());

        NssmfRequest request = new NssmfRequest(serviceInfo, esrInfo.getNetworkType(), additional);
        return marshal(request);
    }

}
