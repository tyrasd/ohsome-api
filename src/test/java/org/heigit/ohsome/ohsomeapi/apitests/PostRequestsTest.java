package org.heigit.ohsome.ohsomeapi.apitests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.heigit.ohsome.ohsomeapi.Application;
import org.heigit.ohsome.ohsomeapi.controller.TestProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class PostRequestsTest {

  private static final String port = TestProperties.PORT2;
  private static final String server = TestProperties.SERVER;

  /** Starts this application context. */
  @BeforeClass
  public static void applicationMainStartup() {
    assumeTrue(TestProperties.PORT2 != null && (TestProperties.INTEGRATION == null
        || !TestProperties.INTEGRATION.equalsIgnoreCase("no")));
    List<String> params = new LinkedList<>();
    params.add("--port=" + port);
    params.addAll(Arrays.asList(TestProperties.DB_FILE_PATH_PROPERTY.split(" ")));
    // this instance gets reused by all of the following @Test methods
    Application.main(params.toArray(new String[0]));
  }

  /** Stops this application context. */
  @AfterClass
  public static void applicationMainShutdown() {
    if (null != Application.getApplicationContext()) {
      SpringApplication.exit(Application.getApplicationContext(), () -> 0);
    }
  }

  @Test
  public void elementsCountGroupByBoundaryGeoJsonTest() throws IOException {
    TestRestTemplate restTemplate = new TestRestTemplate();
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("bpolys",
        "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},"
            + "\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[8.68494,49.41951],"
            + "[8.67902,49.41460],[8.69009,49.41527],[8.68494,49.41951]]]}},{\"type\":\"Feature\","
            + "\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[8.68812,"
            + "49.40466],[8.68091,49.40058],[8.69121,49.40069],[8.68812,49.40466]]]}}]}");
    map.add("types", "way");
    map.add("time", "2016-01-01");
    map.add("keys", "building");
    map.add("format", "geojson");
    ResponseEntity<JsonNode> response = restTemplate
        .postForEntity(server + port + "/elements/count/groupBy/boundary", map, JsonNode.class);
    String expectedJsonString = "{\"attribution\":{\"url\":\"https://ohsome.org/copyrights\",\"text\":\"© OpenStreetMap contributors\"},\"apiVersion\":\"1.3.0-SNAPSHOT\",\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"groupByBoundaryId\":\"feature1\",\"value\":371.0,\"timestamp\":\"2016-01-01T00:00:00Z\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[8.68494,49.41951],[8.67902,49.4146],[8.69009,49.41527],[8.68494,49.41951]]]},\"id\":\"feature1@2016-01-01T00:00:00Z\"},{\"type\":\"Feature\",\"properties\":{\"groupByBoundaryId\":\"feature2\",\"value\":203.0,\"timestamp\":\"2016-01-01T00:00:00Z\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[8.68812,49.40466],[8.68091,49.40058],[8.69121,49.40069],[8.68812,49.40466]]]},\"id\":\"feature2@2016-01-01T00:00:00Z\"}]}";
    ObjectNode expectedJson = (ObjectNode) new ObjectMapper().readTree(expectedJsonString);
    assertEquals(expectedJson, response.getBody());
  }
}
