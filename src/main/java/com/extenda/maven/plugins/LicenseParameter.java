/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.extenda.maven.plugins;

import org.apache.maven.model.License;

public class LicenseParameter extends License {

    /** Serial version UID. */
    private static final long serialVersionUID = 2593674950399343088L;

    /**
     * License parameter value.
     * 
     * @parameter
     * @required
     */
    private String id;

    /**
     * Generated Jan 9, 2013.
     * 
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Generated Jan 9, 2013.
     * 
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
}
