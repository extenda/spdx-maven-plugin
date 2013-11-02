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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.License;
import org.codehaus.plexus.util.StringUtils;

public class LicenseMapping {

    /**
     * The artifact ID to map to a license.
     * 
     * @parameter
     */
    private String artifactId;

    /**
     * The group ID to map to a license. Use either a groupId or an artifactId
     * to map a license. Both is not necessary.
     * 
     * @parameter
     * @optional
     */
    private String groupId;

    /**
     * Comma separated list of license IDs for the artifact.
     * 
     * @parameter
     */
    private String licenseId;

    /**
     * Flag to indicate that the license mapping is to an external library not
     * handled by Maven. However, the mapping should still be presented in the
     * license report.
     * 
     * @parameter default-value="false"
     */
    private boolean external;

    /**
     * 
     * @return the licenseId
     */
    public String getLicenseId() {
        return licenseId;
    }

    /**
     * 
     * @param licenseId
     *            the licenseId to set
     */
    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    /**
     * 
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Generated Jan 9, 2013.
     * 
     * @param artifactId
     *            the artifactId to set
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public List<License> getMappedLicenses(List<LicenseParameter> licenses) {
        Set<String> mapping = new HashSet<String>();
        for (String id : StringUtils.split(licenseId)) {
            mapping.add(id.trim());
        }

        List<License> result = new ArrayList<License>();
        for (LicenseParameter license : licenses) {
            if (mapping.contains(license.getId())) {
                result.add(license);
            }
        }

        return result;
    }

    /**
     * @param external
     *            the external to set
     */
    public void setExternal(boolean external) {
        this.external = external;
    }

    /**
     * @return the external
     */
    public boolean isExternal() {
        return external;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }
}
