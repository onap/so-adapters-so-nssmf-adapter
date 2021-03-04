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
package org.onap.so.adapters.nssmf.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class RestResponseTest {

    @Test
    public void testRestResponse() {
        RestResponse restResponse = new RestResponse();
        Map<String, String> header = new HashMap<>();
        header.put("X-RequestID", "05a63ae2-249a-49b9-bc52-0c70a3bca487");
        header.put("X-InvocationID", "bb7f7fc5-52ba-4937-a7af-55036694fe19");
        header.put("testlong", "55036694");
        header.put("testint", "55");
        restResponse.setRespHeaderMap(header);

        String headerStr = restResponse.getRespHeaderStr("X-RequestID");
        assertEquals(headerStr, "05a63ae2-249a-49b9-bc52-0c70a3bca487");

        long headerLong = restResponse.getRespHeaderLong("testlong");
        long headerLongNull = restResponse.getRespHeaderLong("testlong1");
        assertNotNull(headerLong);
        assertEquals(headerLongNull, -1L);

        int headerInt = restResponse.getRespHeaderInt("testint");
        int headerIntNull = restResponse.getRespHeaderInt("testint1");
        assertNotNull(headerInt);
        assertEquals(headerIntNull, -1);

        Map headerMap = restResponse.getRespHeaderMap();
        assertNotNull(headerMap);
    }

}
