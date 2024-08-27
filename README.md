<p align="center">
    <a href="https://docs.ozone-his.com/"><img src="https://www.ozone-his.com/wp-content/uploads/2021/11/Ozone-Logo.png" alt="Ozone" width="30%"/></a>
</p>

<h3 align="center">Health Information System</h3>

<p align="center">
    Welcome to Ozone's open-source repositories!
    <br/>Engage with the Ozone community and access useful resources below:
</p>

<h3 align="center">
    <a href="https://docs.ozone-his.com/">Docs</a>&nbsp;&nbsp;&nbsp;&nbsp;•&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://talk.openmrs.org/c/software/ozone-his/70">Forum</a>&nbsp;&nbsp;&nbsp;&nbsp;•&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://openmrs.slack.com/archives/C02PYQD5D0A">Chat Room</a>
</h3>

# EIP OpenELIS OpenMRS

### Introduction
Apache camel routes that integrate [OpenELIS](https://github.com/I-TECH-UW/OpenELIS-Global-2) and OpenMRS.

```
The integration is incomplete and work is paused until all the prerequisites are implemented on OpenELIS Global.
```
List of TODOs for OpenELIS Global can be tracked on this public [Notion](https://www.notion.so/mekom/e13ade2febe545689496c46a51115619?v=490643690d374eb4a105c11edf7efbb6) board.

---

### Technical Overview

The project is built on top of [OpenMRS-EIP core](https://github.com/openmrs/openmrs-eip), so the assumption is that
you have an existing OpenMRS EIP based application and wish to add to it odoo integration logic. The project contains
camel routes that track inserts, updates and delete operations in specific tables in an OpenMRS database to take
appropriate action in an odoo system.

If you don't have an existing OpenMRS EIP based application, you will need to first create one as
[documented here](https://github.com/openmrs/openmrs-eip/tree/master/docs/custom), then add the camel routes
provided in this project and application properties.
---

### Integrations

| Integration           | Sync               | Status                  |
|-----------------------|--------------------|-------------------------|
| Patient               | OpenMRS ⮕ OpenELIS | ✅                       |
| Lab Order             | OpenMRS ⮕ OpenELIS | 🚧 Panels not supported |
| Lab Order Result      | OpenELIS ⮕ OpenMRS | ✅                       |
| Modify Lab Order      | OpenMRS ⮕ OpenELIS | ❌                       |
| Discontinue Lab Order | OpenMRS ⮕ OpenELIS | ❌                       |

---

### Steps to run

#### OpenELIS

- Disable TLS from FHIR store
  - Remove ` ./volume/tomcat/hapi_server.xml:/opt/bitnami/tomcat/conf/server.xml` from OpenELIS [docker-compose.yml](https://github.com/I-TECH-UW/OpenELIS-Global-2/blob/develop/docker-compose.yml#L93)
- Update the following configs in `common.properties`
    ```
    org.openelisglobal.fhirstore.uri=http://fhir.openelis.org:8080/fhir/

    org.openelisglobal.remote.source.uri=http://fhir.openelis.org:8080/fhir/
    org.openelisglobal.remote.source.updateStatus=true
    org.openelisglobal.remote.source.identifier=Practitioner/671ee2f8-ced1-411f-aadf-d12fe1e6f2ed
    ```
- Visit `Admin` tab in OpenELIS https://host/api/OpenELIS-Global/MasterListsPage
- Under `Admin`->`Order Entry Configuration` mark `external orders` config as `true`
- Under `Admin`->`Test Management`->`Modify tests` select any test and add `LOINC` code and save. Note `LOINC` codes can be found under `Dictionary` tab on a running OpenMRS instance. Example http://localhost/openmrs/dictionary/concept.htm?conceptId=54 

- **[Optional]** Enable SSO

  - Add to `build.docker-compose.yml`:
  ```
  keycloak:
    container_name: keycloak
    image: quay.io/keycloak/keycloak:25.0.1
    ports:
      - 8089:8080
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: http://localhost:8089
      KC_HOSTNAME_STRICT: false
      KC_HOSTNAME_STRICT_HTTPS: false
    networks:
      - default 
    ```
  - Configure OpenELIS in `./volume/properties/common.properties`:
  ```
    org.itech.login.saml=true
    org.itech.login.saml.metadatalocation=http://keycloak:8080/realms/OpenELIS/protocol/saml/descriptor
    ```
  - Run `docker compose -f build.docker-compose.yml up -d --build`
  - Configure keycloak:

      - Login with admin credentials
      - Create Realm -> OpenELIS
      - Create client ->
        ```
        type: SAML
        name: OpenELIS-Global_saml
        Client ID: OpenELIS-Global_saml
        Valid redirect URIs: https://localhost/api/OpenELIS-Global/login/saml2/sso/keycloak
        ```
      - Edit Keys `client signature required: off`
      - Create User: `admin`
      - Set password under Credentials tab
      - Restart OpenELIS `docker restart openelisglobal-webapp`

Wait for it to start up and then access the frontend at https://localhost/login. There should be an SSO Login button, login with admin user (users must exist in OpenELIS and Keycloak currently, `admin` user exists by default in OEG)
  

#### Ozone: `eip-openelis-openmrs`
Make the following changes in your Ozone Distro.

- Create a file `docker-compose-openelis.yml` under `ozone/run/docker/`
  - Add the following configurations
    ```
    services:
    
      # OpenMRS - OpenELIS integration service
      eip-openelis-openmrs:
        depends_on:
          openmrs:
            condition: service_healthy
          mysql:
            condition: service_started
        environment:
          - OPENELIS_FHIR_SERVER_URL=http://192.168.29.135:8081 #OpenELIS FHIR server instance
          - OPENELIS_FHIR_USERNAME=admin
          - OPENELIS_FHIR_PASSWORD=adminADMIN!
          - OPENMRS_SERVER_URL=http://openmrs:8080/openmrs #OpenMRS instance
          - OPENMRS_SERVER_USER=${OPENMRS_USER}
          - OPENMRS_SERVER_PASSWORD=${OPENMRS_PASSWORD}
          - OPENMRS_RESULTS_ENCOUNTER_TYPE_UUID=${RESULTS_ENCOUNTER_TYPE_UUID}
          - OPENMRS_IDENTIFIER_TYPE_UUID=${OPENMRS_IDENTIFIER_TYPE_UUID}
          - OPENMRS_CONCEPT_COMPLEX_UUID=${CONCEPT_COMPLEX_UUID}
          - BAHMNI_TEST_ORDER_TYPE_UUID=${BAHMNI_TEST_ORDER_TYPE_UUID}
          - EIP_PROFILE=prod
          - EIP_WATCHED_TABLES=patient,person_name,person_address,patient_identifier,orders,test_order
          - EIP_DB_NAME_OPENELIS=${EIP_DB_NAME_OPENELIS}
          - EIP_DB_USER_OPENELIS=${EIP_DB_USER_OPENELIS}
          - EIP_DB_PASSWORD_OPENELIS=${EIP_DB_PASSWORD_OPENELIS}
          - MYSQL_ADMIN_USER=root
          - MYSQL_ADMIN_USER_PASSWORD=${MYSQL_ROOT_PASSWORD}
          - OPENMRS_DB_HOST=${OPENMRS_DB_HOST}
          - OPENMRS_DB_PORT=${OPENMRS_DB_PORT}
          - OPENMRS_DB_NAME=${OPENMRS_DB_NAME}
          - OPENMRS_DB_USER=${OPENMRS_DB_USER}
          - OPENMRS_DB_PASSWORD=${OPENMRS_DB_PASSWORD}
          - OPENMRS_USER=${OPENMRS_USER}
          - OPENMRS_PASSWORD=${OPENMRS_PASSWORD}
          - EIP_FHIR_RESOURCES=Patient,ServiceRequest
          - EIP_FHIR_SERVER_URL=http://openmrs:8080/openmrs/ws/fhir2/R4
          - EIP_FHIR_USERNAME=${OPENMRS_USER}
          - EIP_FHIR_PASSWORD=${OPENMRS_PASSWORD}
        image: mekomsolutions/eip-client:2.1.0
        networks:
          ozone:
            aliases:
              - eip-client-openelis
        restart: unless-stopped
        volumes:
          - "${DISTRO_PATH}/binaries/eip-openelis-openmrs:/eip-client/routes"
          - eip-home-openelis:/eip-home
    
      mysql:
        environment:
          EIP_DB_NAME_OPENELIS: ${EIP_DB_NAME_OPENELIS}
          EIP_DB_USER_OPENELIS: ${EIP_DB_USER_OPENELIS}
          EIP_DB_PASSWORD_OPENELIS: ${EIP_DB_PASSWORD_OPENELIS}
        volumes:
          - "${SQL_SCRIPTS_PATH}/mysql/eip-openelis-openmrs:/docker-entrypoint-initdb.d/db/eip-openelis-openmrs"
    
    volumes:
      eip-home-openelis: ~
    ```
- Create a directory `eip-openelis-openmrs` under `ozone/distro/data/mysql` with a file `create_eip_openelis_openmrs_db.sh`
  
  - Add the following commands
    ```shell
    #!/bin/bash
    
    set -eu
    
    function create_user_and_database() {
    mysql --password=$MYSQL_ROOT_PASSWORD --user=root <<MYSQL_SCRIPT
        CREATE DATABASE $1;
        CREATE USER '$2'@'localhost' IDENTIFIED BY '$3';
        CREATE USER '$2'@'%' IDENTIFIED BY '$3';
        GRANT ALL PRIVILEGES ON $1.* TO '$2'@'localhost';
        GRANT ALL PRIVILEGES ON $1.* TO '$2'@'%';
        FLUSH PRIVILEGES;
    MYSQL_SCRIPT
    }
    
    create_eip_client_user_and_database() {
        local dbName="${1:-}"
        local dbUser="${2:-}"
        local dbUserPassword="${3:-}"
        if [ "${dbName:-}" ] && [ "${dbUser:-}" ] && [ "${dbUserPassword:-}" ]; then
            create_user_and_database "$dbName" "$dbUser" "$dbUserPassword";
        fi
    }
    
    echo "Creating '${EIP_DB_USER_OPENELIS}' user and '${EIP_DB_NAME_OPENELIS}' database..."
    create_eip_client_user_and_database ${EIP_DB_NAME_OPENELIS:-} ${EIP_DB_USER_OPENELIS:-} ${EIP_DB_PASSWORD_OPENELIS:-};
    ```
  - Edit `ozone/run/docker/scripts/docker-compose-files.txt` remove `docker-compose-senaite.yml` and add `docker-compose-openelis.yml`
  - Create a directory `eip-openelis-openmrs` under `ozone/distro/binaries/` and paste the `eip-openelis-openmrs` JAR Example: `eip-openelis-openmrs-1.0.0-SNAPSHOT.jar`
  


- After setting up OpenELIS and Ozone distro, start the Ozone environment using `./start.sh` command.
- Once the Ozone distro is up and running create a Patient and start a Visit, add a Lab Order Eg. (Red Blood Cell) and save.
- This Lab Order should be visible under `Order`->`Incoming Orders`->`Search`

