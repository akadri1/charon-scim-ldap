/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.charon3.utils;

import java.util.HashMap;
import java.util.Map;

import org.wso2.charon3.core.encoder.JSONDecoder;
import org.wso2.charon3.core.encoder.JSONEncoder;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.FormatNotSupportedException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.protocol.endpoints.AbstractResourceManager;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.utils.ldapmanager.LdapManager;

/**
 * This illustrates what are the core tasks an implementation should take care of,
 * according to their specific implementation, and how the extension points and supportutils
 * implementation provided by charon can be initialized/utilized here.
 */
public class DefaultCharonManager {

    private static volatile DefaultCharonManager defaultCharonManager;
    private static Map<String, String> endpointURLs = new HashMap<String, String>();
    private static UserManager userManager = new LdapManager();
    //private static UserManager userManager = new InMemoryUserManager();
    private static JSONDecoder jsonDecoder = new JSONDecoder();
    private static JSONEncoder jsonEncoder = new JSONEncoder();

    private static final String USERS_URL = "http://localhost:8080/scim/v2/Users";
    private static final String GROUPS_URL = "http://localhost:8080/scim/v2/Groups";
    private static final String SERVICE_URL = "http://localhost:8080/scim/v2/ServiceProviderConfig";
    private static final String RESOURCE_URL = "http://localhost:8080/scim/v2/ResourceType";
    /**
     * Perform initialization.
     */
    private void init() throws CharonException {
        //Define endpoint urls to be used in Location Header
        endpointURLs.put(SCIMConstants.USER_ENDPOINT, USERS_URL);
        endpointURLs.put(SCIMConstants.GROUP_ENDPOINT, GROUPS_URL);
        endpointURLs.put(SCIMConstants.SERVICE_PROVIDER_CONFIG_ENDPOINT, SERVICE_URL);
        endpointURLs.put(SCIMConstants.RESOURCE_TYPE_ENDPOINT, RESOURCE_URL);
        //register endpoint URLs in AbstractResourceEndpoint since they are called with in the API
        registerEndpointURLs();
    }

    private DefaultCharonManager() throws CharonException {
        init();
    }

    /**
     * Should return the static instance of CharonManager implementation.
     * Read the config and initialize extensions as specified in the config.
     *
     * @return
     */
    public static DefaultCharonManager getInstance() throws CharonException {
        if (defaultCharonManager == null) {
            synchronized (DefaultCharonManager.class) {
                if (defaultCharonManager == null) {
                    defaultCharonManager = new DefaultCharonManager();
                    return defaultCharonManager;
                } else {
                    return defaultCharonManager;
                }
            }
        } else {
            return defaultCharonManager;
        }
    }

    /**
     * Obtain the encoder for the given format.
     *
     * @return
     */
    public JSONDecoder getDecoder() throws FormatNotSupportedException {
        return jsonDecoder;
    }

    /**
     * Obtain the decoder for the given format.
     *
     * @return
     */
    public JSONEncoder getEncoder() throws FormatNotSupportedException {
        return jsonEncoder;
    }

    public UserManager getUserManager() throws CharonException {
        return userManager;
    }

    private void registerEndpointURLs() {
        if (endpointURLs != null && endpointURLs.size() != 0) {
            AbstractResourceManager.setEndpointURLMap(endpointURLs);
        }
    }
}

