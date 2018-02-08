package org.heigit.bigspatialdata.ohsome.oshdbRestApi.output.dataAggregationResponse;

import org.heigit.bigspatialdata.ohsome.oshdbRestApi.output.dataAggregationResponse.metadata.Metadata;
import org.heigit.bigspatialdata.ohsome.oshdbRestApi.output.dataAggregationResponse.result.RatioResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.annotations.ApiModelProperty;

/**
 * Represents the whole JSON response object for the data aggregation response using the /ratio
 * resource. It contains the license and copyright, optional
 * {@link org.heigit.bigspatialdata.ohsome.oshdbRestApi.output.dataAggregationResponse.metadata.Metadata
 * Metadata} as well as the results section showing
 * {@link org.heigit.bigspatialdata.ohsome.oshdbRestApi.output.dataAggregationResponse.result.RatioResult
 * RatioResult} objects.
 */
@JsonInclude(Include.NON_NULL)
public class RatioResponseContent {

  @ApiModelProperty(notes = "The license of the used data.", required = true, position = 0)
  private String license;
  @ApiModelProperty(notes = "The copyright of the used data.", required = true, position = 1)
  private String copyright;
  @ApiModelProperty(notes = "The metadata describing the output.", position = 2)
  private Metadata metadata;
  @ApiModelProperty(notes = "The result for /count/ratio requests.")
  private RatioResult[] ratioResult;

  public RatioResponseContent(String license, String copyright, Metadata metadata,
      RatioResult[] ratioResult) {
    this.license = license;
    this.copyright = copyright;
    this.metadata = metadata;
    this.ratioResult = ratioResult;
  }

  public String getLicense() {
    return license;
  }

  public String getCopyright() {
    return copyright;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public RatioResult[] getRatioResult() {
    return ratioResult;
  }
}
