package dk.medcom.video.api.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dk.medcom.video.api.dto.CreateMeetingDto;
import dk.medcom.video.api.dto.MeetingDto;
import dk.medcom.video.api.dto.UpdateMeetingDto;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.VideoMeetingsApi;
import io.swagger.client.model.CreateMeeting;
import okio.BufferedSink;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

public class IntegrationWithOrganisationServiceTest {
	private static final Logger mysqlLogger = LoggerFactory.getLogger("mysql");
	private static final Logger videoApiLogger = LoggerFactory.getLogger("video-api");
	private static final Logger mockServerLogger = LoggerFactory.getLogger("mock-server");
	private static final Logger newmanLogger = LoggerFactory.getLogger("newman");

	private static boolean commandLine;

	private static Network dockerNetwork;
	private static GenericContainer resourceContainer;
	private static GenericContainer videoApi;
	private static Integer videoApiPort;
	private static GenericContainer testOrganisationFrontend;

	@BeforeClass
	public static void setup() {
		dockerNetwork = Network.newNetwork();

		createOrganisationService(dockerNetwork);

        resourceContainer = new GenericContainer<>(new ImageFromDockerfile()
                .withFileFromClasspath("/collections/medcom-video-api.postman_collection.json", "docker/collections/medcom-video-api.postman_collection.json")
                .withFileFromClasspath("/loop.sh", "loop.sh")
                .withDockerfileFromBuilder( builder -> builder.from("bash")
                        .add("/collections/medcom-video-api.postman_collection.json", "/collections/medcom-video-api.postman_collection.json")
                        .add("/loop.sh", "/loop.sh")
                        .volume("/collections")
                        .volume("/testresult")
                        .cmd("sh", "/loop.sh")
                        .build()));

        resourceContainer.start();
        System.out.println("Created: " + resourceContainer.isCreated());

		// SQL server for Video API.
		MySQLContainer mysql = (MySQLContainer) new MySQLContainer("mysql:5.7")
				.withDatabaseName("videodb")
				.withUsername("videouser")
				.withPassword("secret1234")
				.withNetwork(dockerNetwork)
				.withNetworkAliases("mysql");
		mysql.start();
		attachLogger(mysql, mysqlLogger);

		// Mock server
		MockServerContainer userService = new MockServerContainer()
				.withNetwork(dockerNetwork)
				.withNetworkAliases("userservice");
		userService.start();
		attachLogger(userService, mockServerLogger);
		MockServerClient mockServerClient = new MockServerClient(userService.getContainerIpAddress(), userService.getMappedPort(1080));
		mockServerClient.when(HttpRequest.request().withMethod("GET"), Times.unlimited()).respond(getResponse());

		// VideoAPI
		videoApi = new GenericContainer<>("kvalitetsit/medcom-video-api:latest")
				.withNetwork(dockerNetwork)
				.withNetworkAliases("videoapi")
				.withEnv("CONTEXT", "/api")
				.withEnv("jdbc_url", "jdbc:mysql://mysql:3306/videodb?useSSL=false")
				.withEnv("jdbc_user", "videouser")
				.withEnv("jdbc_pass", "secret1234")
				.withEnv("userservice_url", "http://userservice:1080")
				.withEnv("userservice_token_attribute_organisation", "organisation_id")
				.withEnv("userservice_token_attribute_username", "username")
				.withEnv("userservice.token.attribute.email", "email")
				.withEnv("userservice.token.attribute.userrole", "userrole")
				.withEnv("scheduling.template.default.conferencing.sys.id", "22")
				.withEnv("scheduling.template.default.uri.prefix", "abc")
				.withEnv("scheduling.template.default.uri.domain", "test.dk")
				.withEnv("scheduling.template.default.host.pin.required", "true")
				.withEnv("scheduling.template.default.host.pin.range.low", "1000")
				.withEnv("scheduling.template.default.host.pin.range.high", "9999")
				.withEnv("scheduling.template.default.guest.pin.required", "true")
				.withEnv("scheduling.template.default.guest.pin.range.low", "1000")
				.withEnv("scheduling.template.default.guest.pin.range.high", "9999")
				.withEnv("scheduling.template.default.vmravailable.before", "15")
				.withEnv("scheduling.template.default.max.participants", "10")
				.withEnv("scheduling.template.default.end.meeting.on.end.time", "true")
				.withEnv("scheduling.template.default.uri.number.range.low", "1000")
				.withEnv("scheduling.template.default.uri.number.range.high", "9999")
				.withEnv("scheduling.template.default.ivr.theme", "10")
				.withEnv("scheduling.info.citizen.portal", "https://portal.vconf.dk")
				.withEnv("mapping.role.provisioner", "dk:medcom:role:provisioner")
				.withEnv("mapping.role.admin", "dk:medcom:role:admin")
				.withEnv("mapping.role.user", "dk:medcom:role:user")
				.withEnv("mapping.role.meeting_planner", "dk:medcom:role:meeting_planner")
				.withEnv("LOG_LEVEL", "debug")
				.withEnv("spring.flyway.locations", "classpath:db/migration,filesystem:/app/sql")
				.withClasspathResourceMapping("db/migration/V901__insert _test_data.sql", "/app/sql/V901__insert _test_data.sql", BindMode.READ_ONLY)
				.withEnv("organisation.service.enabled", "true")
				.withEnv("organisation.service.endpoint", "http://organisationfrontend:80/services")
				.withEnv("short.link.base.url", "https://video.link/")
				.withClasspathResourceMapping("docker/logback-test.xml", "/configtemplates/logback.xml", BindMode.READ_ONLY)
				.withExposedPorts(8080)
				.waitingFor(Wait.forHttp("/api/actuator/info").forStatusCode(200));
		videoApi.start();
		videoApiPort = videoApi.getMappedPort(8080);
		attachLogger(videoApi, videoApiLogger);
	}

	private static void attachLogger(GenericContainer container, Logger logger) {
		logger.info("Attaching logger to container: " + container.getContainerInfo().getName());
		Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
		container.followOutput(logConsumer);
	}

	@Test
	public void verifyTestResults() throws InterruptedException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		Thread.sleep(5000);
		TemporaryFolder folder = new TemporaryFolder();
		folder.create();

		GenericContainer newman = new GenericContainer<>("postman/newman_ubuntu1404:4.1.0")
					.withNetwork(dockerNetwork)
					.withVolumesFrom(resourceContainer, BindMode.READ_WRITE)
					.withCommand("run /collections/medcom-video-api.postman_collection.json -r cli,junit --reporter-junit-export /testresult/TEST-dk.medcom.video.api.test.IntegrationTest.xml --global-var host=videoapi --global-var port=8080");

		newman.start();
		attachLogger(newman, newmanLogger);

		long waitTime = 500;
		long loopLimit = 60;

		for(int i = 0; newman.isRunning() && i < loopLimit; i++) {
			System.out.println(i);
			System.out.println("Waiting....");
			Thread.sleep(waitTime);
		}

		resourceContainer.copyFileFromContainer("/testresult/TEST-dk.medcom.video.api.test.IntegrationTest.xml", folder.getRoot().getCanonicalPath() + "/TEST-dk.medcom.video.api.test.IntegrationTest.xml");

		FileInputStream input = new FileInputStream(folder.getRoot().getCanonicalPath() + "/TEST-dk.medcom.video.api.test.IntegrationTest.xml");
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document xmlDocument = builder.parse(input);
		XPath xPath = XPathFactory.newInstance().newXPath();
		String failureExpression = "/testsuites/testsuite/@failures";
		String errorExpression = "/testsuites/testsuite/@errrors";

		int failures = ((Double) xPath.compile(failureExpression).evaluate(xmlDocument, XPathConstants.NUMBER)).intValue();
		int errors = ((Double) xPath.compile(errorExpression).evaluate(xmlDocument, XPathConstants.NUMBER)).intValue();

		if(errors != 0 || failures != 0) {
			StringBuilder stringBuilder = new StringBuilder();
			Files.readAllLines(Paths.get(folder.getRoot().getCanonicalPath() + "/TEST-dk.medcom.video.api.test.IntegrationTest.xml")).forEach(x -> stringBuilder.append(x).append(System.lineSeparator()));
			System.out.println(stringBuilder);
		}

		assertEquals(0, failures);
		assertEquals(0, errors);
	}

	@Test(expected = ForbiddenException.class)
	public void testCanNotReadOtherOrganisation()  {
		String result = getClient()
				.path("meetings")
				.path("7cc82183-0d47-439a-a00c-38f7a5a01fc1")
				.request()
				.get(String.class);
	}

	@Test
	public void testCanReadMeeting() {
		var result = getClient()
				.path("meetings")
				.path("7cc82183-0d47-439a-a00c-38f7a5a01fc2")
				.request()
				.get(MeetingDto.class);

		assertNotNull(result);
		assertEquals(12, result.getShortId().length());
//		assertEquals("https://video.link/" + result.getShortId(), result.getShortLink());
		assertEquals("external_id", result.getExternalId());
	}

	@Test
	public void testCanCreateExternalId() throws ApiException {
		var apiClient = new ApiClient()
				.setBasePath(String.format("http://%s:%s/api/", videoApi.getContainerIpAddress(), videoApiPort))
				.setOffsetDateTimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss X"));
		var videoMeetings = new VideoMeetingsApi(apiClient);

		var createMeeting = createMeeting("another_external_id");

		var meeting = videoMeetings.meetingsPost(createMeeting);
		assertNotNull(meeting);
		assertEquals(createMeeting.getExternalId(), meeting.getExternalId());
	}

	@Test
	public void testUniqueOrganisationExternalId() throws ApiException {
		var apiClient = new ApiClient()
				.setBasePath(String.format("http://%s:%s/api/", videoApi.getContainerIpAddress(), videoApiPort))
				.setOffsetDateTimeFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss X"));
		var videoMeetings = new VideoMeetingsApi(apiClient);

		var createMeeting = createMeeting("external_id");

		try {
			videoMeetings.meetingsPostWithHttpInfo(createMeeting);
			fail();
		}
		catch(ApiException e) {
			assertTrue(e.getResponseBody().contains("ExternalId not unique within organisation."));
			assertEquals(400, e.getCode());
		}
	}


	private CreateMeeting createMeeting(String externalId) {
		var createMeeting = new CreateMeeting();
		createMeeting.setDescription("This is a description");
		var now = Calendar.getInstance();
		var inOneHour = createDate(now, 1);
		var inTwoHours = createDate(now, 2);

		createMeeting.setStartTime(OffsetDateTime.ofInstant(inOneHour.toInstant(), ZoneId.systemDefault()));
		createMeeting.setEndTime(OffsetDateTime.ofInstant(inTwoHours.toInstant(), ZoneId.systemDefault()));
		createMeeting.setSubject("This is a subject!");
		createMeeting.setExternalId(externalId);

		return createMeeting;
	}

	@Test
	public void testCanCreateMeetingAndSearchByShortId() {
		var createMeeting = new CreateMeetingDto();
		createMeeting.setDescription("This is a description");
		var now = Calendar.getInstance();
		var inOneHour = createDate(now, 1);
		var inTwoHours = createDate(now, 2);

		createMeeting.setStartTime(inOneHour);
		createMeeting.setEndTime(inTwoHours);
		createMeeting.setSubject("This is a subject!");

		var response = getClient()
				.path("meetings")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(createMeeting, MediaType.APPLICATION_JSON_TYPE), MeetingDto.class);

		assertNotNull(response.getUuid());

		var searchResponse = getClient()
				.path("meetings")
				.queryParam("short-id", response.getShortId()) // short id
				.request()
				.get(MeetingDto.class);

		assertNotNull(searchResponse);
		assertEquals(response.getUuid(), searchResponse.getUuid());
//		assertEquals("https://video.link/" + searchResponse.getShortId(), searchResponse.getShortLink());
	}

	private Date createDate(Calendar calendar, int hoursToAdd) {
		Calendar cal = (Calendar) calendar.clone();
		cal.add(Calendar.HOUR, hoursToAdd);

		return cal.getTime();
	}

	WebTarget getClient() {
		JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		provider.setMapper(objectMapper);
		WebTarget target =  ClientBuilder.newClient(new ClientConfig(provider))
				.target(UriBuilder.fromUri(String.format("http://%s:%s/api/", videoApi.getContainerIpAddress(), videoApiPort)));

		return target;
	}

	private static HttpResponse getResponse() {
		return new HttpResponse().withBody("{\"UserAttributes\": {\"organisation_id\": [\"pool-test-org\"],\"email\":[\"eva@klak.dk\"],\"userrole\":[\"dk:medcom:role:admin\"]}}").withHeaders(new Header("Content-Type", "application/json")).withStatusCode(200);
	}


	private static void createOrganisationService(Network n) {
		MySQLContainer organisationMysql = (MySQLContainer) new MySQLContainer("mysql:5.7")
				.withDatabaseName("organisationdb")
				.withUsername("orguser")
				.withPassword("secret1234")
				.withNetwork(n)
				.withNetworkAliases("organisationdb")
				;

		organisationMysql.start();

		GenericContainer organisationContainer = new GenericContainer("kvalitetsit/medcom-vdx-organisation:latest")
				.withNetwork(n)
				.withNetworkAliases("organisationservice")
				.withEnv("jdbc_url", "jdbc:mysql://organisationdb/organisationdb")
				.withEnv("jdbc_user", "orguser")
				.withEnv("jdbc_pass", "secret1234")
				.withEnv("usercontext_header_name", "X-Test-Auth")
				.withEnv("userattributes_role_key", "UserRoles")
				.withEnv("userattributes_org_key", "organisation")
				.withEnv("userrole_admin_values", "adminrole")
				.withEnv("userrole_user_values", "userrole1,userrole2")
				.withEnv("userrole_monitor_values", "monitorrole")
				.withEnv("userrole_provisioner_values", "provisionerrole")
				.withEnv("spring.flyway.locations", "classpath:db/migration,filesystem:/app/sql")
				.withClasspathResourceMapping("organisation/V901__organisation_test_data.sql", "/app/sql/V901__organisation_test_data.sql", BindMode.READ_ONLY)
				.withExposedPorts(8080)
				;

		organisationContainer.start();
		organisationContainer.withLogConsumer(outputFrame -> System.out.println(outputFrame));
		testOrganisationFrontend = new GenericContainer("kvalitetsit/gooioidwsrest:1.1.14")
				.withNetwork(n)
				.withNetworkAliases("organisationfrontend")
				.withCommand("-config", "/caddy/config.json")
				.withClasspathResourceMapping("organisation/caddy.json", "/caddy/config.json", BindMode.READ_ONLY)
				.withExposedPorts(80)
				.waitingFor(Wait.forLogMessage(".*", 1));

		testOrganisationFrontend.start();

	}
}

