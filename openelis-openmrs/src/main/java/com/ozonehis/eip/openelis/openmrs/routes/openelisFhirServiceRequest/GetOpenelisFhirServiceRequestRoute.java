/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes.openelisFhirServiceRequest;

import com.ozonehis.eip.openelis.openmrs.Constants;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.stereotype.Component;

@Component
public class GetOpenelisFhirServiceRequestRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openelis-get-service-request-route")
                .log(LoggingLevel.INFO, "Fetching Service Request in OpenELIS...")
                .routeId("openelis-get-service-request-route")
                .onException(HttpOperationFailedException.class)
                .handled(true)
                .log(LoggingLevel.INFO, "ServiceRequest is gone/deleted error: ${exception.message}")
                .setBody(simple("ServiceRequest is gone/deleted error: ${exception.message}"))
                .end()
                .toD("openelisfhir:read/resourceById?resourceClass=ServiceRequest&stringId=" + "${header." + Constants.HEADER_SERVICE_REQUEST_ID + "}")
                .marshal()
                .fhirJson("R4")
                .convertBodyTo(String.class)
                .end();
        // spotless:on
    }
}
