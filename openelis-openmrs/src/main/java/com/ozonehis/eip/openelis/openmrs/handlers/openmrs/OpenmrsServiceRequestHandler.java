/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsServiceRequestHandler {

    public ServiceRequest getServiceRequestByID(ProducerTemplate producerTemplate, String serviceRequestID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERVICE_REQUEST_ID, serviceRequestID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-get-service-request-route", null, headers, String.class);
        if (response.contains("gone/deleted")) {
            // TODO: Can be moved to route as well
            ServiceRequest deletedServiceRequestResponse = new ServiceRequest();
            deletedServiceRequestResponse.setId((String) headers.get(Constants.HEADER_SERVICE_REQUEST_ID));
            deletedServiceRequestResponse.setStatus(ServiceRequest.ServiceRequestStatus.REVOKED);
            return deletedServiceRequestResponse;
        }
        FhirContext ctx = FhirContext.forR4();
        ServiceRequest serviceRequestResponse = ctx.newJsonParser().parseResource(ServiceRequest.class, response);

        return serviceRequestResponse;
    }
}
