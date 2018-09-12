package dk.medcom.video.api.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class TestRunner extends Runner {

	private Class testClass;


	public TestRunner(Class testClass) {
		super();
		this.testClass = testClass;
	}

	@Override
	public Description getDescription() {
		return Description
				.createTestDescription(testClass, "My runner description");
	}

	@Override
	public void run(RunNotifier notifier) {
		String projectDirectory = System.getenv("MAVEN_PROJECTBASEDIR");
		String outputDirectory = projectDirectory+"/target";
		
		// Laver et sted, hvor junitreport kan ligge
/*		TemporaryFolder testFolder = new TemporaryFolder();
		String temporaryFolderUri = null;
		try {
			testFolder.create();
			temporaryFolderUri = testFolder.getRoot().getCanonicalPath();
		} catch (IOException e) {

			throw new RuntimeException(e);
		}
*/
		Network n = setup();

		GenericContainer newman = new GenericContainer<>("postman/newman_ubuntu1404:4.1.0")
				.withNetwork(n)
				.withFileSystemBind(outputDirectory, "/testresult", BindMode.READ_WRITE)
				//.withFileSystemBind("/home/eva/ffproject/medcom-video-api/medcom-video-api-test/src/test/resources/output", "/testresult", BindMode.READ_WRITE)
				.withClasspathResourceMapping("docker/collections/medcom-video-api.postman_collection.json", "/etc/postman/test_collection.json", BindMode.READ_ONLY)
				.withCommand("run /etc/postman/test_collection.json -r junit --reporter-junit-export /testresult/TEST-dk.medcom.video.api.test.IntegrationTest.xml --global-var host=videoapi:8080");
		newman.start();

		/*while (!new File(temporaryFolderUri+"junit-result.xml").exists()) {
			System.out.println("Waiting....");
		}
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(temporaryFolderUri+"junit-result.xml"));
			String report = new String(encoded);
			System.out.println(report);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}*/
	}


	public Network setup() {
		Network n = Network.newNetwork();

		MySQLContainer mysql = (MySQLContainer) new MySQLContainer("mysql:5.5").withDatabaseName("videodb").withUsername("videouser").withPassword("secret1234").withNetwork(n).withNetworkAliases("mysql");
		mysql.start();

		MockServerContainer userService = new MockServerContainer().withNetwork(n).withNetworkAliases("userservice");
		userService.start();
		MockServerClient mockServerClient = userService.getClient();
		mockServerClient.when(HttpRequest.request().withMethod("GET"), Times.unlimited()).respond(getResponse());

		GenericContainer videoApi = new GenericContainer<>("kvalitetsit/medcom-video-api-web:latest")
				.withNetwork(n)
				.withNetworkAliases("videoapi")
				.withEnv("CONTEXT", "/api")
				.withEnv("jdbc_url", "jdbc:mysql://mysql:3306/videodb")
				.withEnv("jdbc_user", "videouser")
				.withEnv("jdbc_pass", "secret1234")
				.withEnv("userservice_url", "http://userservice")
				.withEnv("userservice_token_attribute_organisation", "organisation_id")
				.withEnv("userservice_token_attribute_username", "username")
				.withEnv("userservice.token.attribute.email", "email")
				.withExposedPorts(8080)
				.waitingFor(Wait.forHttp("/api/info").forStatusCode(200));
		videoApi.start();

		return n;
	}


	private static HttpResponse getResponse() {  
		return new HttpResponse().withBody("{\"organisation_id\":\"klak\",\"email\":\"eva@klak.dk\"}").withHeaders(new Header("Content-Type", "application/json")).withStatusCode(200);
	}
}
