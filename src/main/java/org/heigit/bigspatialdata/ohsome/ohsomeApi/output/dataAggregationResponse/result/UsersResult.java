package org.heigit.bigspatialdata.ohsome.ohsomeApi.output.dataAggregationResponse.result;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents the result JSON object for the /users resource containing the from timestamp together
 * with the corresponding value.
 */
public class UsersResult {

  @ApiModelProperty(notes = "Timestamp in the format YYYY-MM-DDThh:mm:ssZ", required = true)
  private String fromTimestamp;
  @ApiModelProperty(notes = "Timestamp in the format YYYY-MM-DDThh:mm:ssZ", required = true)
  private String toTimestamp;
  @ApiModelProperty(notes = "Value corresponding to the filter parameters", required = true)
  private double value;

  public UsersResult(String fromTimestamp, String toTimestamp, double value) {
    this.fromTimestamp = fromTimestamp;
    this.toTimestamp = toTimestamp;
    this.value = value;
  }

  public String getToTimestamp() {
    return toTimestamp;
  }

  public String getFromTimestamp() {
    return fromTimestamp;
  }

  public double getValue() {
    return value;
  }
}
