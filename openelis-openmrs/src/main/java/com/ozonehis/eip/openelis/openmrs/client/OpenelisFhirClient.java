/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.client;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
public class OpenelisFhirClient {
    @Value("${eip.openelis.fhir.username}")
    private String openelisUsername;

    @Value("${eip.openelis.fhir.password}")
    private String openelisPassword;

    @Value("${eip.openelis.fhir.serverUrl}")
    private String openelisFhirBaseUrl;

    public String authHeader() {
        String auth = getOpenelisUsername() + ":" + getOpenelisPassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    //    @Bean
    //    CamelContextConfiguration contextConfiguration() {
    //        return new CamelContextConfiguration() {
    //
    //            @Override
    //            public void beforeApplicationStart(CamelContext camelContext) {
    //                FhirConfiguration fhirConfiguration = new FhirConfiguration();
    //                FhirContext ctx = FhirContext.forR4();
    //                fhirConfiguration.setServerUrl(openelisFhirBaseUrl);
    //                IGenericClient client = ctx.newRestfulGenericClient(openelisFhirBaseUrl);
    //
    //
    //                if (openelisUsername != null && !openelisUsername.isBlank() && openelisPassword != null &&
    // !openelisPassword.isBlank()) {
    //                        client.registerInterceptor(new BasicAuthInterceptor(openelisUsername, openelisPassword));
    //                }
    //
    //                fhirConfiguration.setClient(client);
    //                fhirConfiguration.setFhirContext(ctx);
    //                fhirConfiguration.setSummary("DATA");
    //
    //                camelContext.getComponent("openelis-fhir",
    // FhirComponent.class).setConfiguration(fhirConfiguration);
    //            }
    //
    //            @Override
    //            public void afterApplicationStart(CamelContext camelContext) {
    //
    //            }
    //        };
    //    }
}
