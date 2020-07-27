package org.heigit.ors.weightaugmentation;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.heigit.ors.exceptions.ParameterValueException;
import org.heigit.ors.geojson.GeometryJSON;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UserWeightParserTest {

  private static final GeometryFactory factory = new GeometryFactory();
  private UserWeightParser userWeightParser;
  private List<AugmentedWeight> weightAugmentations;
  private List<Geometry> geometries1;
  private List<Double> weights1;
  private List<Geometry> geometries2;
  private List<Double> weights2;
  String normalInputJson;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    userWeightParser = new UserWeightParser();
    normalInputJson = "{\"type\": \"FeatureCollection\", \"features\": [{\"type\": \"Feature\", \"properties\": {\"weight\": \"5.0\"}, \"geometry\": {\"type\": \"Polygon\", \"coordinates\": [[[8.691, 49.415], [8.691, 49.413], [8.699, 49.413], [8.691, 49.415]]] } }, { \"type\": \"Feature\", \"properties\": { \"weight\": 0.1 }, \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[[8.682, 49.413], [8.689, 49.413], [8.689, 49.419], [8.682, 49.419], [8.682, 49.413]], [[8.684, 49.418], [8.684, 49.414], [8.687, 49.414], [8.687, 49.418], [8.684, 49.418]]]}}]}";

    geometries1 = new ArrayList<>();
    geometries1.add(GeometryJSON.parse(new JSONObject("{\"type\": \"Polygon\", \"coordinates\": [[[8.691, 49.415], [8.691, 49.413], [8.699, 49.413], [8.691, 49.415]]]}")));
    geometries1.add(GeometryJSON.parse(new JSONObject("{\"type\": \"Polygon\", \"coordinates\": [[[8.682, 49.413], [8.689, 49.413], [8.689, 49.419], [8.682, 49.419], [8.682, 49.413]], [[8.684, 49.418], [8.684, 49.414], [8.687, 49.414], [8.687, 49.418], [8.684, 49.418]]]}")));
    weights1 = new ArrayList<>(Arrays.asList(5.0, 0.1));
    geometries2 = new ArrayList<>();
    geometries2.add(GeometryJSON.parse(new JSONObject("{\"type\": \"Polygon\", \"coordinates\": [[[8.680, 49.416], [8.664, 49.399], [8.692, 49.401], [8.680, 49.416]]]}")));
    weights2 = new ArrayList<>(Arrays.asList(2.3));
  }

  @Test
  public void testParse() throws ParameterValueException {
    weightAugmentations = userWeightParser.parse(normalInputJson);
    List<AugmentedWeight> expectedWeightAugmentations = new ArrayList<>();
    expectedWeightAugmentations.add(new AugmentedWeight(geometries1.get(0), weights1.get(0)));
    expectedWeightAugmentations.add(new AugmentedWeight(geometries1.get(1), weights1.get(1)));
    Assert.assertEquals(expectedWeightAugmentations, weightAugmentations);
  }

  @Test
  public void testAddWeightAugmentations() throws Exception {
    weightAugmentations = userWeightParser.parse(normalInputJson);
    int sizeBefore = weightAugmentations.size();
    String geomJson = "{\"type\": \"Polygon\",\"coordinates\": [[[8.681,49.420],[8.685,49.420],[8.684,49.423],[8.681,49.420]]]}";
    Geometry geom = GeometryJSON.parse(new JSONObject(geomJson));
    userWeightParser.addWeightAugmentations(weightAugmentations, geom, 1.3);
    AugmentedWeight expectedResult = new AugmentedWeight(geom, 1.3);
    AugmentedWeight actualResult = weightAugmentations.get(weightAugmentations.size() - 1);
    Assert.assertEquals(expectedResult, actualResult);
    Assert.assertEquals(sizeBefore + 1, weightAugmentations.size());
  }

  @Test
  public void testParseGeometry() throws ParameterValueException {
    String inputJson = "{\"type\": \"Polygon\", \"coordinates\": [[[8.680, 49.416], [8.664, 49.399], [8.692, 49.401], [8.680, 49.416]]]}";
    thrown.expect(ParameterValueException.class);
    thrown.expectMessage("Parameter 'user_weights' has incorrect value or format.");
    userWeightParser.parse(inputJson);
  }

  @Test
  public void testParseFeature() throws ParameterValueException {
    String inputJson = "{\"type\": \"Feature\", \"properties\": {\"weight\": 2.3}, \"geometry\": {\"type\": \"Polygon\", \"coordinates\": [[[8.680, 49.416], [8.664, 49.399], [8.692, 49.401], [8.680, 49.416]]]}}";
    weightAugmentations = userWeightParser.parse(inputJson);
    List<AugmentedWeight> expectedAugmentations = new ArrayList<>();
    expectedAugmentations.add(new AugmentedWeight(geometries2.get(0), weights2.get(0)));
    Assert.assertEquals(expectedAugmentations, weightAugmentations);
  }

  @Test
  public void testParseFeatureWithoutWeight() throws ParameterValueException {
    String inputJson = "{\"type\": \"Feature\", \"properties\": {}, \"geometry\": {\"type\": \"Polygon\", \"coordinates\": [[[8.680, 49.416], [8.664, 49.399], [8.692, 49.401], [8.680, 49.416]]]}}";
    thrown.expect(JSONException.class);
    thrown.expectMessage("JSONObject[\"weight\"] not found.");
    userWeightParser.parse(inputJson);
  }

  @Test
  public void testParseFeatureCollection() throws ParameterValueException {
    String inputJson = "{\"type\": \"FeatureCollection\", \"features\": [{\"type\": \"Feature\", \"properties\": {\"weight\": 2.3}, \"geometry\": {\"type\": \"Polygon\", \"coordinates\": [[[8.680, 49.416], [8.664, 49.399], [8.692, 49.401], [8.680, 49.416]]]}}]}";
    weightAugmentations = userWeightParser.parse(inputJson);
    List<AugmentedWeight> expectedAugmentations = new ArrayList<>();
    expectedAugmentations.add(new AugmentedWeight(geometries2.get(0), weights2.get(0)));
    Assert.assertEquals(expectedAugmentations, weightAugmentations);
  }

  @Test
  public void testParseFeatureCollectionWithoutWeight() throws ParameterValueException {
    String inputJson = "{\"type\": \"FeatureCollection\", \"features\": [{\"type\": \"Feature\", \"properties\": {}, \"geometry\": {\"type\": \"Polygon\", \"coordinates\": [[[8.680, 49.416], [8.664, 49.399], [8.692, 49.401], [8.680, 49.416]]]}}]}";
    thrown.expect(JSONException.class);
    thrown.expectMessage("JSONObject[\"weight\"] not found.");
    userWeightParser.parse(inputJson);
  }
}