package org.heigit.bigspatialdata.ohsome.ohsomeapi.executor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.Application;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.exception.BadRequestException;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.exception.ExceptionMessages;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.inputprocessing.InputProcessingUtils;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.inputprocessing.InputProcessor;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.inputprocessing.ProcessingData;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.oshdb.DbConnData;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.oshdb.ExtractMetadata;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.Description;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.dataaggregationresponse.Attribution;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.dataaggregationresponse.DefaultAggregationResponse;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.dataaggregationresponse.Metadata;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.dataaggregationresponse.Response;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.dataaggregationresponse.groupbyresponse.GroupByResponse;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.dataaggregationresponse.groupbyresponse.GroupByResult;
import org.heigit.bigspatialdata.ohsome.ohsomeapi.output.dataaggregationresponse.users.UsersResult;
import org.heigit.bigspatialdata.oshdb.api.generic.OSHDBCombinedIndex;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableFunction;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapAggregator;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygonal;

/** Includes the execute methods for requests mapped to /users. */
public class UsersRequestExecutor {

  private static final String URL = ExtractMetadata.attributionUrl;
  private static final String TEXT = ExtractMetadata.attributionShort;
  public static final DecimalFormat df = ExecutionUtils.defineDecimalFormat("#.##");

  private UsersRequestExecutor() {
    throw new IllegalStateException("Utility class");
  }

  /** Performs a count calculation. */
  public static Response executeCount(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, boolean isDensity) throws Exception {
    long startTime = System.currentTimeMillis();
    SortedMap<OSHDBTimestamp, Integer> result;
    MapReducer<OSMContribution> mapRed = null;
    InputProcessor inputProcessor = new InputProcessor(servletRequest, false, isDensity);
    mapRed = inputProcessor.processParameters();
    ProcessingData processingData = inputProcessor.getProcessingData();
    RequestParameters requestParameters = processingData.getRequestParameters();
    result = mapRed.aggregateByTimestamp().map(OSMContribution::getContributorUserId).countUniq();
    ExecutionUtils exeUtils = new ExecutionUtils(processingData);
    Geometry geom = inputProcessor.getGeometry();
    UsersResult[] results =
        exeUtils.fillUsersResult(result, requestParameters.isDensity(), inputProcessor, df, geom);
    Metadata metadata = null;
    if (processingData.isShowMetadata()) {
      long duration = System.currentTimeMillis() - startTime;
      metadata = new Metadata(duration, Description.usersCount(isDensity),
          inputProcessor.getRequestUrlIfGetRequest(servletRequest));
    }
    if ("csv".equalsIgnoreCase(requestParameters.getFormat())) {
      exeUtils.writeCsvResponse(results, servletResponse,
          exeUtils.createCsvTopComments(URL, TEXT, Application.API_VERSION, metadata));
      return null;
    }
    return DefaultAggregationResponse.of(new Attribution(URL, TEXT), Application.API_VERSION,
        metadata, results);
  }

  /** Performs a count calculation grouped by the OSM type. */
  public static Response executeCountGroupByType(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, boolean isDensity) throws Exception {
    long startTime = System.currentTimeMillis();
    SortedMap<OSHDBCombinedIndex<OSHDBTimestamp, OSMType>, Integer> result = null;
    MapReducer<OSMContribution> mapRed = null;
    InputProcessor inputProcessor = new InputProcessor(servletRequest, false, isDensity);
    mapRed = inputProcessor.processParameters();
    ProcessingData processingData = inputProcessor.getProcessingData();
    RequestParameters requestParameters = processingData.getRequestParameters();
    result = mapRed.aggregateByTimestamp()
        .aggregateBy((SerializableFunction<OSMContribution, OSMType>) f -> {
          return f.getEntityAfter().getType();
        }, processingData.getOsmTypes()).map(OSMContribution::getContributorUserId).countUniq();
    SortedMap<OSMType, SortedMap<OSHDBTimestamp, Integer>> groupByResult;
    groupByResult = ExecutionUtils.nest(result);
    GroupByResult[] resultSet = new GroupByResult[groupByResult.size()];
    int count = 0;
    ExecutionUtils exeUtils = new ExecutionUtils(processingData);
    Geometry geom = inputProcessor.getGeometry();
    for (Entry<OSMType, SortedMap<OSHDBTimestamp, Integer>> entry : groupByResult.entrySet()) {
      UsersResult[] results = exeUtils.fillUsersResult(entry.getValue(),
          requestParameters.isDensity(), inputProcessor, df, geom);
      resultSet[count] = new GroupByResult(entry.getKey().toString(), results);
      count++;
    }
    Metadata metadata = null;
    if (processingData.isShowMetadata()) {
      long duration = System.currentTimeMillis() - startTime;
      metadata = new Metadata(duration, Description.usersCountGroupByType(isDensity),
          inputProcessor.getRequestUrlIfGetRequest(servletRequest));
    }
    if ("csv".equalsIgnoreCase(requestParameters.getFormat())) {
      exeUtils.writeCsvResponse(resultSet, servletResponse,
          exeUtils.createCsvTopComments(URL, TEXT, Application.API_VERSION, metadata));
      return null;
    }
    return new GroupByResponse(new Attribution(URL, TEXT), Application.API_VERSION, metadata,
        resultSet);
  }

  /** Performs a count calculation grouped by the tag. */
  public static Response executeCountGroupByTag(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, boolean isDensity) throws Exception {
    long startTime = System.currentTimeMillis();
    InputProcessor inputProcessor = new InputProcessor(servletRequest, false, isDensity);
    String[] groupByKey = inputProcessor.splitParamOnComma(
        inputProcessor.createEmptyArrayIfNull(servletRequest.getParameterValues("groupByKey")));
    if (groupByKey.length != 1) {
      throw new BadRequestException(ExceptionMessages.GROUP_BY_KEY_PARAM);
    }
    MapReducer<OSMContribution> mapRed = null;
    mapRed = inputProcessor.processParameters();
    ProcessingData processingData = inputProcessor.getProcessingData();
    RequestParameters requestParameters = processingData.getRequestParameters();
    ExecutionUtils exeUtils = new ExecutionUtils(processingData);
    String[] groupByValues = inputProcessor.splitParamOnComma(
        inputProcessor.createEmptyArrayIfNull(servletRequest.getParameterValues("groupByValues")));
    TagTranslator tt = DbConnData.tagTranslator;
    Integer[] valuesInt = new Integer[groupByValues.length];
    ArrayList<Pair<Integer, Integer>> zeroFill = new ArrayList<>();
    int keysInt = tt.getOSHDBTagKeyOf(groupByKey[0]).toInt();
    if (groupByValues.length != 0) {
      for (int j = 0; j < groupByValues.length; j++) {
        valuesInt[j] = tt.getOSHDBTagOf(groupByKey[0], groupByValues[j]).getValue();
        zeroFill.add(new ImmutablePair<Integer, Integer>(keysInt, valuesInt[j]));
      }
    }
    SortedMap<OSHDBCombinedIndex<OSHDBTimestamp, Pair<Integer, Integer>>, Integer> result = null;
    result = mapRed.flatMap(f -> {
      List<Pair<Pair<Integer, Integer>, OSMContribution>> res = new LinkedList<>();
      int[] tags = exeUtils.extractContributionTags(f);
      for (int i = 0; i < tags.length; i += 2) {
        int tagKeyId = tags[i];
        int tagValueId = tags[i + 1];
        if (tagKeyId == keysInt) {
          if (valuesInt.length == 0) {
            res.add(
                new ImmutablePair<>(new ImmutablePair<Integer, Integer>(tagKeyId, tagValueId), f));
          }
          for (int value : valuesInt) {
            if (tagValueId == value) {
              res.add(new ImmutablePair<>(new ImmutablePair<Integer, Integer>(tagKeyId, tagValueId),
                  f));
            }
          }
        }
      }
      if (res.isEmpty()) {
        res.add(new ImmutablePair<>(new ImmutablePair<Integer, Integer>(-1, -1), f));
      }
      res.add(new ImmutablePair<>(new ImmutablePair<Integer, Integer>(-2, -2), f));
      return res;
    }).aggregateByTimestamp().aggregateBy(Pair::getKey, zeroFill).map(Pair::getValue)
        .map(OSMContribution::getContributorUserId).countUniq();
    Geometry geom = inputProcessor.getGeometry();
    SortedMap<Pair<Integer, Integer>, SortedMap<OSHDBTimestamp, Integer>> groupByResult;
    groupByResult = ExecutionUtils.nest(result);
    GroupByResult[] resultSet = new GroupByResult[groupByResult.size()];
    String groupByName = "";
    int count = 0;
    for (Entry<Pair<Integer, Integer>, SortedMap<OSHDBTimestamp, Integer>> entry : groupByResult
        .entrySet()) {
      UsersResult[] results = exeUtils.fillUsersResult(entry.getValue(),
          requestParameters.isDensity(), inputProcessor, df, geom);
      if (entry.getKey().getKey() == -2 && entry.getKey().getValue() == -2) {
        groupByName = "total";
      } else if (entry.getKey().getKey() == -1 && entry.getKey().getValue() == -1) {
        groupByName = "remainder";
      } else {
        groupByName = tt.getOSMTagOf(keysInt, entry.getKey().getValue()).toString();
      }
      resultSet[count] = new GroupByResult(groupByName, results);
      count++;
    }
    Metadata metadata = null;
    if (processingData.isShowMetadata()) {
      long duration = System.currentTimeMillis() - startTime;
      metadata = new Metadata(duration, Description.usersCountGroupByTag(isDensity),
          inputProcessor.getRequestUrlIfGetRequest(servletRequest));
    }
    if ("csv".equalsIgnoreCase(requestParameters.getFormat())) {
      exeUtils.writeCsvResponse(resultSet, servletResponse,
          exeUtils.createCsvTopComments(URL, TEXT, Application.API_VERSION, metadata));
      return null;
    }
    return new GroupByResponse(new Attribution(URL, TEXT), Application.API_VERSION, metadata,
        resultSet);
  }

  /** Performs a count calculation grouped by the key. */
  public static Response executeCountGroupByKey(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, boolean isDensity) throws Exception {
    long startTime = System.currentTimeMillis();
    InputProcessor inputProcessor = new InputProcessor(servletRequest, false, isDensity);
    String[] groupByKeys = inputProcessor.splitParamOnComma(
        inputProcessor.createEmptyArrayIfNull(servletRequest.getParameterValues("groupByKeys")));
    if (groupByKeys == null || groupByKeys.length == 0) {
      throw new BadRequestException(ExceptionMessages.GROUP_BY_KEYS_PARAM);
    }
    MapReducer<OSMContribution> mapRed = null;
    mapRed = inputProcessor.processParameters();
    ProcessingData processingData = inputProcessor.getProcessingData();
    RequestParameters requestParameters = processingData.getRequestParameters();
    ExecutionUtils exeUtils = new ExecutionUtils(processingData);
    TagTranslator tt = DbConnData.tagTranslator;
    Integer[] keysInt = new Integer[groupByKeys.length];
    for (int i = 0; i < groupByKeys.length; i++) {
      keysInt[i] = tt.getOSHDBTagKeyOf(groupByKeys[i]).toInt();
    }
    SortedMap<OSHDBCombinedIndex<OSHDBTimestamp, Integer>, Integer> result = null;
    result = mapRed.flatMap(f -> {
      List<Pair<Integer, OSMContribution>> res = new LinkedList<>();
      int[] tags = exeUtils.extractContributionTags(f);
      for (int i = 0; i < tags.length; i += 2) {
        int tagKeyId = tags[i];
        for (int key : keysInt) {
          if (tagKeyId == key) {
            res.add(new ImmutablePair<>(tagKeyId, f));
          }
        }
      }
      if (res.isEmpty()) {
        res.add(new ImmutablePair<>(-1, f));
      }
      res.add(new ImmutablePair<>(-2, f));
      return res;
    }).aggregateByTimestamp().aggregateBy(Pair::getKey, Arrays.asList(keysInt)).map(Pair::getValue)
        .map(OSMContribution::getContributorUserId).countUniq();
    Geometry geom = inputProcessor.getGeometry();
    SortedMap<Integer, SortedMap<OSHDBTimestamp, Integer>> groupByResult;
    groupByResult = ExecutionUtils.nest(result);
    GroupByResult[] resultSet = new GroupByResult[groupByResult.size()];
    String groupByName = "";
    int count = 0;
    for (Entry<Integer, SortedMap<OSHDBTimestamp, Integer>> entry : groupByResult.entrySet()) {
      UsersResult[] results = exeUtils.fillUsersResult(entry.getValue(),
          requestParameters.isDensity(), inputProcessor, df, geom);
      if (entry.getKey() == -2) {
        groupByName = "total";
      } else if (entry.getKey() == -1) {
        groupByName = "remainder";
      } else {
        groupByName = tt.getOSMTagKeyOf(entry.getKey().intValue()).toString();
      }
      resultSet[count] = new GroupByResult(groupByName, results);
      count++;
    }
    Metadata metadata = null;
    if (processingData.isShowMetadata()) {
      long duration = System.currentTimeMillis() - startTime;
      metadata = new Metadata(duration, Description.usersCountGroupByKey(isDensity),
          inputProcessor.getRequestUrlIfGetRequest(servletRequest));
    }
    if ("csv".equalsIgnoreCase(requestParameters.getFormat())) {
      exeUtils.writeCsvResponse(resultSet, servletResponse,
          exeUtils.createCsvTopComments(URL, TEXT, Application.API_VERSION, metadata));
      return null;
    }
    return new GroupByResponse(new Attribution(URL, TEXT), Application.API_VERSION, metadata,
        resultSet);
  }

  /** Performs a count calculation grouped by the boundary. */
  public static <P extends Geometry & Polygonal> Response executeCountGroupByBoundary(
      HttpServletRequest servletRequest, HttpServletResponse servletResponse, boolean isDensity)
      throws Exception {
    long startTime = System.currentTimeMillis();
    SortedMap<OSHDBCombinedIndex<OSHDBTimestamp, Integer>, Integer> result = null;
    MapReducer<OSMContribution> mapRed = null;
    InputProcessor inputProcessor = new InputProcessor(servletRequest, false, isDensity);
    inputProcessor.getProcessingData().setIsGroupByBoundary(true);
    mapRed = inputProcessor.processParameters();
    ProcessingData processingData = inputProcessor.getProcessingData();
    RequestParameters requestParameters = processingData.getRequestParameters();
    ArrayList<Geometry> arrGeoms = processingData.getBoundaryList();
    @SuppressWarnings("unchecked") // intentionally as check for P on Polygonal is already performed
    Map<Integer, P> geoms = IntStream.range(0, arrGeoms.size()).boxed()
        .collect(Collectors.toMap(idx -> idx, idx -> (P) arrGeoms.get(idx)));
    InputProcessingUtils utils = inputProcessor.getUtils();
    MapAggregator<OSHDBCombinedIndex<OSHDBTimestamp, Integer>, OSMContribution> mapAgg
        = mapRed.aggregateByTimestamp().aggregateByGeometry(geoms);
    if (processingData.containsSimpleFeatureTypes()) {
      mapAgg = inputProcessor.filterOnSimpleFeatures(mapAgg);
    }
    result = mapAgg.map(OSMContribution::getContributorUserId).countUniq();
    SortedMap<Integer, SortedMap<OSHDBTimestamp, Integer>> groupByResult;
    groupByResult = ExecutionUtils.nest(result);
    GroupByResult[] resultSet = new GroupByResult[groupByResult.size()];
    int count = 0;
    ExecutionUtils exeUtils = new ExecutionUtils(processingData);
    Object[] boundaryIds = utils.getBoundaryIds();
    for (Entry<Integer, SortedMap<OSHDBTimestamp, Integer>> entry : groupByResult.entrySet()) {
      UsersResult[] results = exeUtils.fillUsersResult(entry.getValue(),
          requestParameters.isDensity(), inputProcessor, df, arrGeoms.get(count));
      resultSet[count] = new GroupByResult(boundaryIds[count], results);
      count++;
    }
    Metadata metadata = null;
    if (processingData.isShowMetadata()) {
      long duration = System.currentTimeMillis() - startTime;
      metadata = new Metadata(duration, Description.usersCountGroupByBoundary(isDensity),
          inputProcessor.getRequestUrlIfGetRequest(servletRequest));
    }
    if ("geojson".equalsIgnoreCase(requestParameters.getFormat())) {
      return GroupByResponse.of(new Attribution(URL, TEXT), Application.API_VERSION, metadata,
          "FeatureCollection",
          exeUtils.createGeoJsonFeatures(resultSet, processingData.getGeoJsonGeoms()));
    } else if ("csv".equalsIgnoreCase(requestParameters.getFormat())) {
      exeUtils.writeCsvResponse(resultSet, servletResponse,
          exeUtils.createCsvTopComments(URL, TEXT, Application.API_VERSION, metadata));
      return null;
    }
    return new GroupByResponse(new Attribution(URL, TEXT), Application.API_VERSION, metadata,
        resultSet);
  }
}
