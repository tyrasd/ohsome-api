package org.heigit.bigspatialdata.ohsome.ohsomeApi.controller.executor;

import org.heigit.bigspatialdata.ohsome.ohsomeApi.Application;
import org.heigit.bigspatialdata.ohsome.ohsomeApi.output.dataAggregationResponse.Attribution;
import org.heigit.bigspatialdata.ohsome.ohsomeApi.output.metadataResponse.ExtractRegion;
import org.heigit.bigspatialdata.ohsome.ohsomeApi.output.metadataResponse.MetadataResponse;
import org.heigit.bigspatialdata.ohsome.ohsomeApi.output.metadataResponse.TemporalExtent;

/** Includes the execute method for requests mapped to /metadata. */
public class MetadataRequestExecutor {

  /**
   * Returns the metadata of the underlying extract-file.
   * 
   * @return {@link org.heigit.bigspatialdata.ohsome.ohsomeApi.output.metadataResponse.MetadataResponse
   *         MetadataResponse}
   */
  public static MetadataResponse executeGetMetadata() {

    return new MetadataResponse(
        new Attribution(Application.getAttributionUrl(), Application.getAttributionShort()),
        Application.apiVersion, new ExtractRegion(Application.getDataPolyJson(),
            new TemporalExtent(Application.getFromTstamp(), Application.getToTstamp())));
  }

}
