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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.StringUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Goal to build a SPDX file with license information for all the project's
 * dependencies that are defined in the dependency management section.
 * For further information see (http://www.spdx.org) 
 * <p>
 * This goal is heavily influenced and based on the dependency management
 * report.
 * 
 * @goal spdx
 * 
 * @phase pre-site
 * @threadSafe
 * 
 * @author Peter Norrhall, Extenda AB
 */
public class SpdxMojo extends AbstractMojo {

	/**
	 * The Maven Project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * Artifact Resolver component.
	 * 
	 * @component
	 */
	protected ArtifactResolver resolver;

	/**
	 * Maven Project Builder component.
	 * 
	 * @component
	 */
	private MavenProjectBuilder mavenProjectBuilder;

	/**
	 * Artifact metadata source component.
	 * 
	 * @component
	 */
	protected ArtifactMetadataSource artifactMetadataSource;

	/**
	 * Maven Artifact Factory component.
	 * 
	 * @component
	 */
	private ArtifactFactory artifactFactory;

	/**
	 * Output directory. This is the base output directory where exported files
	 * will be generated.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}/site/rdf/spdx"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * Scopes to include.
	 * 
	 * @parameter default-value="compile,runtime"
	 * @required
	 */
	private String scopes;

	/**
	 * List of full or partial <tt>groupId</tt>s to exclude. For example, to
	 * exclude all sub package of group <tt>se.extenda</tt>, add the following:
	 * 
	 * <pre>
	 * {@code
	 * <excludes>
	 *   <exclude>se.extenda</exclude>
	 * </excludes>
	 * }
	 * </pre>
	 * 
	 * @parameter alias="exclude"
	 */
	private List<String> excludes = new ArrayList<String>();

	/**
	 * List of custom pre defined licenses. Intended to be used when license is
	 * unknown.
	 * 
	 * @parameter alias="license"
	 */
	private List<LicenseParameter> licenses = new ArrayList<LicenseParameter>();

	/**
	 * List of artifacts with customized licenses. Intended to be used when
	 * license is unknown.
	 * 
	 * @parameter alias="licenseMapping"
	 */
	private List<LicenseMapping> licenseMappings = new ArrayList<LicenseMapping>();

	/**
	 * Artifact Factory component.
	 * 
	 * @component
	 */
	protected ArtifactFactory factory;

	/**
	 * Local Repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @required
	 * @readonly
	 */
	protected ArtifactRepository localRepository;

	/**
	 * 4.5 Package Originator (optional)
	 * Either "Person: " <person name> and optional "("email")" or
	 * "Organization:" organization name and optional "("email")"
	 * @parameter alias="originator"
	 */
	protected String originator;
	
	/**
	 * 4.6 Package Download Location
	 * @parameter alias="downloadlocation"
	 */
	protected String downloadLocation;
	
	/**
	 * 4.15 Copyright Text
	 * @parameter alias="copyrighttext"
	 */
	protected String copyrighttext;


	
	@Override
	public void execute() throws MojoExecutionException {

		if (outputDirectory != null)
		  System.out.println(outputDirectory.toString());
		
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		if (project != null)
			  System.out.println(project.toString());
		
		@SuppressWarnings("unchecked")
		List<Dependency> dependencies = project.getDependencies();

		Map<String, String> licenseMap = new TreeMap<String, String>();

		Set<String> licenseScopes = new HashSet<String>();
		for (String scope : scopes.split(",")) {
			licenseScopes.add(scope.trim().toLowerCase());
		}

		// Collect the dependency licenses.
		List<String[]> licenses = new ArrayList<String[]>();
		licenses.add(new String[] {"Dependency", "License type"});
		for (Dependency dependency : dependencies) {
			String scope = dependency.getScope() == null ? "runtime"
					: dependency.getScope().toLowerCase();
			if (licenseScopes.contains(scope)
					&& !excluded(dependency.getGroupId())) {
				String[] licenseRow = getDependencyRow(dependency, licenseMap);
				licenses.add(licenseRow);
			}
		}

		// Collect the external license mappings
		for (LicenseMapping mapping : licenseMappings) {
			if (mapping.isExternal()) {
				String license = getLicenseString(
						mapping.getMappedLicenses(this.licenses), licenseMap);
				licenses.add(new String[] {mapping.getArtifactId(), license});
			}
		}

		// Collect the license definition and URL links.
		List<String[]> licenseLinks = new ArrayList<String[]>();
		licenseLinks.add(new String[] {"License type", "License file"});
		for (Map.Entry<String, String> entry : licenseMap.entrySet()) {
			licenseLinks.add(new String[] {entry.getValue(), entry.getKey()});
		}

		writeSpdxFile(project.getName() + ".rdf", licenses, licenseMap);

	}

	private void writeSpdxFile(
			String filename, 
			List<String[]> licenses,
			Map<String, String> licenceMap) {

		// create an empty Model
		Model model = ModelFactory.createDefaultModel();

		// create the spdx resource
		Resource spdx = model.createResource("");

		StringBuffer result = new StringBuffer();
		result.append(generateHeader());
		result.append(generateCreationInformation());
		result.append(genertePackageInformation(licenceMap));
		result.append(generteOtherLicensingInformation(model, spdx));
		result.append(generateFileInformation());
		result.append(generateReviewerInformation());
		
        try {
            File outputFile = new File(outputDirectory, filename);
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8");
            writer.write(result.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            getLog().error("Failed to write RDF file.", e);
        }
		

	}
	
	private StringBuffer generateHeader() {
		StringBuffer result = new StringBuffer();
		
		String header = "<rdf:RDF\n" +
						"  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
						"  xmlns:j.0=\"http://usefulinc.com/ns/doap#\"\n" +
						"  xmlns=\"http://spdx.org/rdf/terms#\"\n" +
						"  xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n" +
						"<SpdxDocument rdf:about=\"http://www.spdx.org/tools#SPDXANALYSIS\">\n" +
						"  <specVersion>SPDX-1.2</specVersion>\n";
		result.append(header);
		return result;
		
	}


	protected StringBuffer generateCreationInformation() {
		StringBuffer result = new StringBuffer();
		
		result.append("    <creationInfo>\n");
		result.append("      <CreationInfo>\n");
		result.append("        <created>" + generateUTCTime() + "</created>\n");
		// omit <rdfs:comment>...</rdfs:comment>
		// omit <creator> Organization: </rdfs:comment>
		result.append("        <creator> Tool: spdx-maven-spdx-plugin-0.0.1</creator>\n");
		// omit <licenseListVersion> 1.2.0 </licenseListVersion>
		result.append("      </CreationInfo>\n");
		result.append("    </creationInfo>\n");
		return result;
	}

	/*
	 * Generate the current time using UTC format
	 */
	protected String generateUTCTime() {
		TimeZone utc = TimeZone.getTimeZone("UTC");
		Calendar calendar = Calendar.getInstance(utc);

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:MM:ss'Z'");
		df.setTimeZone(utc);
		return df.format(calendar.getTime());
	}

	protected StringBuffer genertePackageInformation(Map<String, String> licenceMap) {
		StringBuffer result = new StringBuffer();
		result.append("  <Package rdf:about=\"\">\n");
		result.append("    <packageFileName>" + project.getName() + "</packageFileName>\n");
		
		if (!StringUtils.isEmpty(originator)) {
			result.append("    <originator>" + originator + "</originator>\n");
		} 
		
		if (!StringUtils.isEmpty(downloadLocation)) {
			result.append("    <downloadLocation>" + downloadLocation + "</downloadLocatio>\n");
		} else {
			result.append("    <downloadLocation>NONE</downloadLocatio>\n");
		}
		
		generatePackageVerificationCode(result);
		
		generateConcludedLicense(result);
		
		generateLicenseReferenceFromFile(result, licenceMap);
		
		generateDeclaredLicense(result, project);
		
		result.append("  </Package>\n");
		return result;
	}


	private void generatePackageVerificationCode(final StringBuffer result) {
		result.append("    </packageVerificationCode>\n");
		result.append("      </PackageVerificationCode>\n");
		result.append("        <packageVerificationCodeValue>\n");
		result.append("          " + getPackageVerificationCodeValue() + "\n");
		result.append("        </packageVerificationCodeValue>\n");
		
		if (true) { // Check if exclude files exists
			result.append("        <packageVerificationExcludeFile>\n");
			result.append("          " + getPackageVerificationExcludeFiles() + "\n");
			result.append("        </packageVerificationExcludeFile>\n");
		}
		
		result.append("      </PackageVerificationCode>\n");
		result.append("    <packageVerificationCode>\n");
	}

	
	/**
	 *  Purpose: This field provides an independently reproducible mechanism identifying
	 *	specific contents of a package based on the actual files (except the SPDX file itself, if it is included
	 *	in the package) that make up each package and that correlates to the data in this SPDX file. This
	 *	identifier enables a recipient to determine if any file in the original package (that the analysis was
	 *	done on) has been changed and permits inclusion of an SPDX file as part of a package.
	 *	4.7.2
	 * 
	 *  Intent: Providing a unique identifier based on the files inside each package, eliminates
	 *	confusion over which version or modification of a specific package the SPDX file refers to. It also
	 *	permits one to embed the SPDX file within the package without altering the identifier.
	 * @return
	 */
	private String getPackageVerificationCodeValue() {
		/*
		verificationcode = 0
		filelist = templist = “”
		for all files in the package {
		if file is an “excludes” file, skip it // exclude SPDX analysis file(s) 
		append templist with “SHA1(file)/n”
		}
		sort templist in ascending order by SHA1 value
		filelist = templist with "/n"s removed. //  ordered sequence of SHA1 values with no separators 
		verificationcode = SHA1(filelist)
		Where SHA1(file) applies a SHA1 algorithm on the contents of file and returns the result in
		lowercase hexadecimal digits.
		Required sort order: '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' (ASCII order)		 
		*/
		
		// TODO Auto-generated method stub
		return null;
	}

	private String getPackageVerificationExcludeFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 *  4.11 Concluded License
	 *	4.11.1 Purpose: This field contains the license the SPDX file creator has concluded as
	 *	governing the package or alternative values, if the governing license cannot be determined. The
	 *	options to populate this field are limited to:
	 *	(a) the SPDX License List short form identifier, if the concluded license is on the SPDX License
	 *	List;
	 *	(b) a reference to the license text denoted by the LicenseRef-[idString], if the concluded license is
	 *	not on the SPDX License List;
	 *	(c) NOASSERTION should be used if:
	 *	(i) the SPDX file creator has attempted to but cannot reach a reasonable objective
	 *	determination of the Concluded License;
	 *	(ii) the SPDX file creator is uncomfortable concluding a license, despite some license
	 *	information being available;
	 *	(iii) the SPDX file creator has made no attempt to determine a Concluded License;
	 *	(iv) the SPDX file creator has intentionally provided no information (no meaning should
	 *	be implied by doing so); or
	 *	(v) there is no licensing information from which to conclude a license for the package.
	 *	With respect to (a) and (b) above, if there is more than one concluded license, all should be
	 *	included. If the package recipient has a choice of multiple licenses, then each of the choices
	 *	should be recited as a "disjunctive" license. If the Concluded License is not the same as the
	 *	Declared License, a written explanation should be provided in the Comments on License field
	 *	(section 4.14). With respect to (c), a written explanation in the Comments on License field
	 *	(section 4.14) is preferred.
	 *	4.11.2 Intent: Here, the intent is for the SPDX file creator to analyze the license information in
	 *	package, and other objective information, e.g., COPYING file, together with the results from any
	 *	scanning tools, to arrive at a reasonably objective conclusion as to what license governs the
	 *	package.
	 *
	 * Geneate Concluded License
	 * @param result - append 
	 */
	private void generateConcludedLicense(final StringBuffer result) {
		result.append("    <licenseConcluded>NOASSERTION</licenseConcluded>\n");
	}


	/**
	 * 4.12 All Licenses Information from Files
	 * 4.12.1 Purpose: This field is to contain a list of all licenses found in the package. The
	 * relationship between licenses (i.e., conjunctive, disjunctive) is not specified in this field – it is simply
	 * a listing of all licenses found. The options to populate this list are limited to:
	 * (a) the SPDX License List short form identifier, if a detected license is on the SPDX License List;
	 * (b) a reference to the license, denoted by LicenseRef-[idString], if the detected license is not on
	 * the SPDX License List;
	 * (c) NONE, if no license information is detected in any of the files; or
	 * (d) NOASSERTION, if the SPDX file creator has not examined the contents of the actual files or if
 	 * the SPDX file creator has intentionally provided no information (no meaning should be implied by
	 * doing so).
	 * 4.12.2 Intent: Here, the intention is to capture all license information detected in the actual files.
	 * @param result
	 * @param licenseMap
	 */
	private void generateLicenseReferenceFromFile(final StringBuffer result, Map<String, String> licenseMap) {
		for (Map.Entry<String, String> entry : licenseMap.entrySet()) {
			result.append("<licenseInfoFromFiles rdf:resouce=\"" + entry.getValue() + "\"/>\n");
//			result.append(entry.getValue()); // The MIT License
//			result.append(entry.getKey()); // http://code.google.com/p/mockito/wiki/License
		}		
	}
	
	/**
	 * 4.13 Declared License
	 * 4.13.1 Purpose: This field lists the licenses that have been declared by the authors of the
	 * package. Any license information that does not originate from the package authors, e.g. license
	 * information from a third party repository, should not be included in this field. The options to populate
	 * this field are limited to:
	 * (a) the SPDX License List short form identifier, if the license is on the SPDX License List;
	 * (b) a reference to the license, denoted by LicenseRef-[idString], if the declared license is
	 * not on the SPDX License List;
	 * (c) NONE, if no license information is detected in any of the files; or
	 * (d) NOASSERTION, if the SPDX file creator has not examined the contents of the
	 * package or if the SPDX file creator has intentionally provided no information (no meaning
	 * should be implied by doing so).
	 * With respect to “a” and “b” above, if license information for more than one license is contained in
	 * the file, all should be reflected in this field. If the license information offers the package recipient a
	 * choice of licenses, then each of the choices should be recited as "disjunctive" licenses.
	 * 4.13.2 Intent: This is simply the license identified in text in one or more files (for example
	 * COPYING file) in the source code package. This field is not intended to capture license information
	 * obtained from an external source, such as the package website. Such information can be included
	 * in 4.11 Concluded License. This field may have multiple declared licenses, if multiple licenses are
	 * declared at the package level.
	 * @param result
	 * @param project
	 */
	private void generateDeclaredLicense(final StringBuffer result, MavenProject project) {
		@SuppressWarnings("unchecked")
		List<License> licenses = project.getLicenses();
		if (licenses == null) {
			result.append("  <licenseDeclared rdf:resouce=\"NONE\"/>\n");
		} else if (licenses.size() == 0) {
			result.append("  <licenseDeclared rdf:resouce=\"NONE\"/>\n");
		} else if (licenses.size() == 1) {
			result.append("  <licenseDeclared rdf:resouce=\"" + licenses.get(0).getName() + "\"/>\n");
		} else {
			result.append("  <licenseDeclared>\n");
			result.append("    <ConjunctiveLicenseSet>\n");
			for (License license : licenses) {
				result.append("      <member rdf:resouce=\"" + license.getName() + "\"/>\n");
			}
			result.append("    </ConjunctiveLicenseSet>\n");
			result.append("  </licenseDeclared>\n");
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	private StringBuffer generateReviewerInformation() {
		StringBuffer result = new StringBuffer();
		
		// Do not add any review information
		
		return result;

	}

	/**
	 * One instance of the File Information is required for each file in the software package. It provides important meta
	 * information about a given file including licenses and copyright. Each instance should include the following fields.
	 * @return
	 */
	private StringBuffer generateFileInformation() {
		StringBuffer result = new StringBuffer();
		
		return result;
	}

	private StringBuffer generteOtherLicensingInformation(Model model, Resource spdx) {
		StringBuffer result = new StringBuffer();
		
		return result;
	}

	private boolean excluded(String groupId) {
		for (String exclude : excludes) {
			if (groupId.startsWith(exclude)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private String[] getDependencyRow(Dependency dependency,
			Map<String, String> licenseMap) {

		Artifact artifact = artifactFactory.createProjectArtifact(
				dependency.getGroupId(), dependency.getArtifactId(),
				dependency.getVersion());

		String licenses = "";
		String artifactName = artifact.getArtifactId();
		try {
			List<ArtifactVersion> versions = artifactMetadataSource
					.retrieveAvailableVersions(artifact, localRepository,
							project.getRemoteArtifactRepositories());

			if (versions.size() > 0) {
				Collections.sort(versions);
				artifact.setVersion(versions.get(versions.size() - 1)
						.toString());
			}
			MavenProject artifactProject = getMavenProjectFromRepository(artifact);
			artifactName = artifactProject.getName();
			licenses = getLicenses(artifactProject, licenseMap);
		} catch (ProjectBuildingException e) {
			getLog().warn(
					"Unable to create Maven project for " + artifact.getId()
					+ " from repository.", e);
		} catch (ArtifactMetadataRetrievalException e) {
			getLog().warn(
					"Unable to retrieve versions for " + artifact.getId()
					+ " from repository.", e);
		}

		return new String[] {artifactName, licenses};
	}

	@SuppressWarnings("unchecked")
	private String getLicenses(MavenProject artifactProject,
			Map<String, String> licenseMap) {
		List<License> licenses = null;

		for (LicenseMapping mapping : licenseMappings) {
			boolean match = false;
			if (mapping.getGroupId() != null) {
				match = mapping.getGroupId().equals(
						artifactProject.getGroupId());
			} else {
				match = mapping.getArtifactId().equals(
						artifactProject.getArtifactId());
			}
			if (match) {
				licenses = mapping.getMappedLicenses(this.licenses);
				break;
			}
		}

		if (licenses == null) {
			// We only get licenses from project if not defined by user in POM.
			licenses = artifactProject.getLicenses();
		}

		return getLicenseString(licenses, licenseMap);
	}

	private String getLicenseString(List<License> licenses,
			Map<String, String> licenseMap) {
		StringBuilder licensesBuffer = new StringBuilder();
		for (License license : licenses) {
			String name;
			// Reuse already detected license names to avoid multiple naming
			// definitions of the same legal license.
			if (licenseMap.containsValue(license.getUrl())) {
				name = licenseMap.get(license.getUrl());
			} else {
				name = license.getName();
				licenseMap.put(license.getUrl(), name);
			}

			if (licensesBuffer.length() > 0) {
				licensesBuffer.append(", ");
			}
			licensesBuffer.append(license.getName());
		}
		return licensesBuffer.toString();
	}

	/**
	 * Get the <code>Maven project</code> from the repository depending the
	 * <code>Artifact</code> given.
	 * 
	 * @param artifact
	 *            an artifact
	 * @return the Maven project for the given artifact
	 * @throws ProjectBuildingException
	 *             if any
	 */
	public MavenProject getMavenProjectFromRepository(Artifact artifact)
			throws ProjectBuildingException {
		Artifact projectArtifact = artifact;

		boolean allowStubModel = false;
		if (!"pom".equals(artifact.getType())) {
			projectArtifact = factory.createProjectArtifact(
					artifact.getGroupId(), artifact.getArtifactId(),
					artifact.getVersion(), artifact.getScope());
			allowStubModel = true;
		}

		// TODO: we should use the MavenMetadataSource instead
		return mavenProjectBuilder.buildFromRepository(projectArtifact,
				project.getRemoteArtifactRepositories(), localRepository,
				allowStubModel);
	}
}
