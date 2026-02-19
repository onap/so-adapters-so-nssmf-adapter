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

package org.onap.so.adapters.nssmf.service;

import org.onap.so.adapters.nssmf.annotation.ServiceLogger;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@ServiceLogger
public interface NssmfManagerService {
    ResponseEntity<String> allocateNssi(NssmfAdapterNBIRequest allocateRequest);

    ResponseEntity<String> deAllocateNssi(NssmfAdapterNBIRequest allocateRequest, String sliceProfileId);

    ResponseEntity<String> activateNssi(NssmfAdapterNBIRequest deActRequest, String snssai);

    ResponseEntity<String> deActivateNssi(NssmfAdapterNBIRequest nssiDeActivate, String snssai);

    ResponseEntity<String> queryJobStatus(NssmfAdapterNBIRequest jobReq, String jobId);

    ResponseEntity<String> queryNSSISelectionCapability(NssmfAdapterNBIRequest nbiRequest);

    ResponseEntity<String> querySubnetCapability(NssmfAdapterNBIRequest nbiRequest);

}
