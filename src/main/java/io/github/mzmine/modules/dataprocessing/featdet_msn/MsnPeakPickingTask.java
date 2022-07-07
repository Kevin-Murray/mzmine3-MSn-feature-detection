/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.dataprocessing.featdet_msn;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;

public class MsnPeakPickingTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final ParameterSet parameterSet;
  private final ModularFeatureList newFeatureList;
  private int processedScans, totalScans;

  public MsnPeakPickingTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    scanSelection = parameters.getParameter(MsnPeakPickerParameters.scanSelection).getValue();
    mzTolerance = parameters.getParameter(MsnPeakPickerParameters.mzDifference).getValue();
    rtTolerance = parameters.getParameter(MsnPeakPickerParameters.rtTolerance).getValue();
    newFeatureList = new ModularFeatureList(dataFile.getName() + " MSn features",
        getMemoryMapStorage(), dataFile);
    this.parameterSet = parameters;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0f;
    }
    return (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Building MSn feature list based on MSn from " + dataFile;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    int[] totalMSLevel = dataFile.getMSLevels();

    // MS Level not set in Scan Selection field
    if (scanSelection.getMsLevel() == null) {
      setStatus(TaskStatus.ERROR);
      final String msg = "MS Level not set in Scan Selection Field.";
      setErrorMessage(msg);
      return;
    }

    // MS Level cannot be 1 in Scan Selection Field.
    if (scanSelection.getMsLevel() == 1) {
      setStatus(TaskStatus.ERROR);
      final String msg = "MS Level cannot be 1. Please use LC-MS feature detection instead.";
      setErrorMessage(msg);
      return;
    }

    // No scans at MS Level in data file.
    if (!ArrayUtils.contains(totalMSLevel, scanSelection.getMsLevel())) {
      setStatus(TaskStatus.ERROR);
      final String msg = "No MS" + scanSelection.getMsLevel() + " scans in " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    final ScanDataAccess scans = EfficientDataAccess.of(dataFile, ScanDataType.CENTROID, scanSelection);

    totalScans = scans.getNumberOfScans();

    // No scans in selection range.
    if (totalScans == 0) {
      setStatus(TaskStatus.ERROR);
      final String msg = "No scans detected in selection range for " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    /*
      Process each MS2 scan to find MSn scans through fragmentationScan tracing. If a MSn scan
      found, build simple modular feature for MS2 precursor in range.
     */
    while(scans.hasNextScan()){

      // Canceled?
      if (isCanceled()) {
        return;
      }

      scans.nextScan();
      Scan scan = scans.getCurrentScan();
      assert scan != null;

      double precursorMZ;

      if (scan.getMSLevel() == 2) {
        precursorMZ = scan.getPrecursorMz();
      } else if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
        precursorMZ = msn.getMS2PrecursorMz();
      } else {

        /*
          Conflicts can be encountered with Thermo RAW files if using old versions of MSConvert or
          ThermoRawFileParser. MSn spectra are not detected correctly.
         */
        logger.warning(
            () -> String.format("Scan#%d: Cannot find MS2 precursor scan", scan.getScanNumber()));
        continue;
      }

      if (precursorMZ != 0) {

        float scanRT = scan.getRetentionTime();

        Range<Float> rtRange = rtTolerance.getToleranceRange(scanRT);
        Range<Double> mzRange = mzTolerance.getToleranceRange(precursorMZ);

        // Build simple feature for precursor in ranges.
        ModularFeature newFeature = FeatureUtils
            .buildSimpleModularFeature(newFeatureList, dataFile, rtRange, mzRange);

        // Add feature to feature list.
        if (newFeature != null) {

          ModularFeatureListRow newFeatureListRow = new ModularFeatureListRow(newFeatureList,
              scan.getScanNumber(), newFeature);

          newFeatureList.addRow(newFeatureListRow);
        }
      }

      processedScans++;
    }

    // No MSn features detected in range.
    if (newFeatureList.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      final String msg =
          "No MSn precursor features detected in selected range for " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    // Explicitly get scans for full scan (msLevel = 1).
    List<Scan> scan;
    if(scanSelection.getScanRTRange() == null){
      scan = dataFile.getScanNumbers(1);
    } else {
      scan = Arrays.asList(dataFile.getScanNumbers(1, scanSelection.getScanRTRange()));
    }
    newFeatureList.setSelectedScans(dataFile, scan);

    dataFile.getAppliedMethods().forEach(m -> newFeatureList.getAppliedMethods().add(m));
    newFeatureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(MsnFeatureDetectionModule.class, parameterSet,
            getModuleCallDate()));

    // Add new feature list to the project.
    project.addFeatureList(newFeatureList);

    logger.info(
        "Finished MSn feature builder on " + dataFile + ", " + processedScans + " scans processed");

    setStatus(TaskStatus.FINISHED);
  }
}
