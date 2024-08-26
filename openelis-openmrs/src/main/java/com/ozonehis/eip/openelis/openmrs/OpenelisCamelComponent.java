/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.component.fhir.FhirComponent;
import org.apache.camel.component.fhir.FhirConfiguration;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Configuration
@ComponentScan("org.openmrs.eip.fhir")
public class OpenelisCamelComponent {

    @Value("${eip.openelis.fhir.username}")
    private String openelisUsername;

    @Value("${eip.openelis.fhir.password}")
    private String openelisPassword;

    @Value("${eip.openelis.fhir.serverUrl}")
    private String openelisFhirBaseUrl;

    @Bean
    @Qualifier("openelisFhirClient") IGenericClient openelisFhirClient() {
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient(getOpenelisFhirBaseUrl() + "/fhir");
        if (StringUtils.isNotBlank(getOpenelisUsername()) && StringUtils.isNotBlank(getOpenelisPassword())) {
            client.registerInterceptor(new BasicAuthInterceptor(getOpenelisUsername(), getOpenelisPassword()));
        }
        return client;
    }

    @Bean
    CamelContextConfiguration openelisContextConfiguration() {
        return new CamelContextConfiguration() {

            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                FhirConfiguration fhirConfiguration = new FhirConfiguration();
                fhirConfiguration.setServerUrl(getOpenelisFhirBaseUrl());
                fhirConfiguration.setClient(openelisFhirClient());
                fhirConfiguration.setFhirContext(FhirContext.forR4());
                fhirConfiguration.setSummary("DATA");

                FhirComponent openelisFhirComponent = new FhirComponent();
                openelisFhirComponent.setConfiguration(fhirConfiguration);

                camelContext.addComponent("openelisfhir", openelisFhirComponent);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {}
        };
    }
}
