package io.jenkins.plugins;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.File;
import java.nio.file.Files;
import java.util.logging.Level;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.UploadableArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.RealJenkinsExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SmokeTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmokeTests.class);

    @RegisterExtension
    private final RealJenkinsExtension extension =
            new RealJenkinsExtension().withLogger(SmokeTests.class, Level.FINEST);

    @Test
    void smokeUploadTest() throws Throwable {
        extension.startJenkins();
        extension.runRemotely(SmokeTests::smokeTest);
    }

    /**
     * Smoke test for the plugin
     * @param r the Jenkins rule
     * @throws Exception if the test fails
     */
    private static void smokeTest(JenkinsRule r) throws Exception {
        WireMockServer wireMock = new WireMockServer(8181);
        wireMock.start();

        File testFile = Files.createTempFile("text", ".txt").toFile();

        // PUT to upload artifact
        wireMock.stubFor(put(urlMatching("/my-generic-repo/.*")).willReturn(okJson("{}")));

        // Test upload
        testUpload("my-generic-repo", wireMock.baseUrl(), testFile);
    }

    /**
     * Test uploading a file to an Artifactory server
     * @param url the URL of the Artifactory server
     * @param testFile the file to upload
     * @throws Exception if the file cannot be uploaded
     */
    private static void testUpload(String repo, String url, File testFile) throws Exception {
        try (Artifactory artifactory = ArtifactoryClientBuilder.create()
                .setUrl(url)
                .setUsername("fake")
                .setPassword("fake")
                .build()) {
            UploadableArtifact artifact = artifactory.repository(repo).upload("text.txt", testFile);
            artifact.withSize(Files.size(testFile.toPath()));
            artifact.withListener((bytesRead, totalBytes) -> LOGGER.info("Uploaded {}/{}", bytesRead, totalBytes));
            artifact.doUpload();
        }
    }
}
