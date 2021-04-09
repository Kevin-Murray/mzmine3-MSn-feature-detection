/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

/**
 * Window to show all MS/MS scans of a feature list row
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class MultiSpectraVisualizerPane extends BorderPane {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private List<RawDataFile> rawFiles;
  private FeatureListRow row;
  private RawDataFile activeRaw;

  private static final long serialVersionUID = 1L;
  private GridPane pnGrid;
  private Label lbRaw;

  /**
   * Shows best fragmentation scan raw data file first
   *
   * @param row
   */
  public MultiSpectraVisualizerPane(FeatureListRow row) {
    this(row, row.getBestFragmentation().getDataFile());
  }

  public MultiSpectraVisualizerPane(FeatureListRow row, RawDataFile raw) {
    getStyleClass().add("region-match-chart-bg");

    // setExtendedState(JFrame.MAXIMIZED_BOTH);
    setMinSize(800, 600);

    pnGrid = new GridPane();
    var colCon = new ColumnConstraints();
    colCon.setFillWidth(true);
    pnGrid.getColumnConstraints().add(colCon);
    // any number of rows
    // pnGrid.setLayout(new GridLayout(0, 1, 0, 25));
    // pnGrid.setAutoscrolls(true);
    pnGrid.setVgap(25);

    ScrollPane scrollPane = new ScrollPane(pnGrid);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    setCenter(scrollPane);

    FlowPane pnMenu = new FlowPane();
    pnMenu.setAlignment(Pos.TOP_LEFT);
    pnMenu.setVgap(0);
    setTop(pnMenu);

    Button nextRaw = new Button("next");
    nextRaw.setOnAction(e -> nextRaw());
    Button prevRaw = new Button("prev");
    prevRaw.setOnAction(e -> prevRaw());
    pnMenu.getChildren().addAll(prevRaw, nextRaw);

    lbRaw = new Label();
    pnMenu.getChildren().add(lbRaw);

    Label lbRawTotalWithFragmentation = new Label();
    pnMenu.getChildren().add(lbRawTotalWithFragmentation);

    int n = 0;
    for (Feature f : row.getFeatures()) {
      if (f.getMostIntenseFragmentScan() != null) {
        n++;
      }
    }
    lbRawTotalWithFragmentation.setText("(total raw:" + n + ")");

    // add charts
    setData(row, raw);

    // setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setVisible(true);
  }

  /**
   * next raw file with peak and MSMS
   */
  private void nextRaw() {
    logger.log(Level.INFO, "All MS/MS scans window: next raw file");
    int n = indexOfRaw(activeRaw);
    while (n + 1 < rawFiles.size()) {
      n++;
      setRawFileAndShow(rawFiles.get(n));
    }
  }

  /**
   * Previous raw file with peak and MSMS
   */
  private void prevRaw() {
    logger.log(Level.INFO, "All MS/MS scans window: previous raw file");
    int n = indexOfRaw(activeRaw) - 1;
    while (n - 1 >= 0) {
      n--;
      setRawFileAndShow(rawFiles.get(n));
    }
  }

  /**
   * Set data and create charts
   *
   * @param row
   * @param raw
   */
  public void setData(FeatureListRow row, RawDataFile raw) {
    rawFiles = row.getRawDataFiles();
    this.row = row;
    setRawFileAndShow(raw);
  }

  /**
   * Set the raw data file and create all chromatograms and MS2 spectra
   *
   * @param raw
   * @return true if row has peak with MS2 spectrum in RawDataFile raw
   */
  public boolean setRawFileAndShow(RawDataFile raw) {
    Feature peak = row.getFeature(raw);
    // no peak / no ms2 - return false
    if (peak == null || peak.getAllMS2FragmentScans() == null
        || peak.getAllMS2FragmentScans().size() == 0)
      return false;

    this.activeRaw = raw;
    // clear
    pnGrid.getChildren().clear();

    ObservableList<Scan> numbers = peak.getAllMS2FragmentScans();
    int i = 0;
    for (Scan scan : numbers) {
      BorderPane pn = addSpectra(scan);
      pn.minWidthProperty().bind(widthProperty().subtract(30));
      pnGrid.add(pn, 0, i++);
    }

    int n = indexOfRaw(raw);
    lbRaw.setText(n + ": " + raw.getName());
    logger.finest("All MS/MS scans window: Added " + numbers.size() + " spectra of raw file " + n
        + ": " + raw.getName());
    // show
    // pnGrid.revalidate();
    // pnGrid.repaint();
    return true;
  }

  private int indexOfRaw(RawDataFile raw) {
    return Arrays.asList(rawFiles).indexOf(raw);
  }

  private BorderPane addSpectra(Scan scan) {
    BorderPane panel = new BorderPane();
    // Split pane for eic plot (top) and spectrum (bottom)
    SplitPane bottomPane = new SplitPane();
    bottomPane.setOrientation(Orientation.VERTICAL);

    // Create EIC plot
    // labels for TIC visualizer
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    Feature peak = row.getFeature(activeRaw);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(activeRaw.getDataRTRange(1), 1);

    // mz range
    Range<Double> mzRange = null;
    mzRange = peak.getRawDataPointsMZRange();

    // labels
    labelsMap.put(peak, peak.toString());

    // get EIC window
    TICVisualizerTab window = new TICVisualizerTab(new RawDataFile[] {activeRaw}, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        null,
        // new Feature[] {peak}, // selected features
        labelsMap); // labels

    // get EIC Plot
    TICPlot ticPlot = window.getTICPlot();
    // ticPlot.setPreferredSize(new Dimension(600, 200));
    ticPlot.getChart().getLegend().setVisible(false);

    // add a retention time Marker to the EIC
    ValueMarker marker = new ValueMarker(scan.getRetentionTime());
    marker.setPaint(Color.RED);
    marker.setStroke(new BasicStroke(3.0f));

    XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();
    plot.addDomainMarker(marker);
    bottomPane.getItems().add(ticPlot);
    // bottomPane.setResizeWeight(0.5);
    // bottomPane.setEnabled(true);
    // bottomPane.setDividerSize(5);
    // bottomPane.setDividerLocation(200);

    SplitPane spectrumPane = new SplitPane();
    spectrumPane.setOrientation(Orientation.HORIZONTAL);

    // get MS/MS spectra window
    SpectraVisualizerTab spectraTab = new SpectraVisualizerTab(activeRaw);
    spectraTab.loadRawData(scan);

    // get MS/MS spectra plot
    SpectraPlot spectrumPlot = spectraTab.getSpectrumPlot();
    spectrumPlot.getChart().getLegend().setVisible(false);
    // spectrumPlot.setPreferredSize(new Dimension(600, 400));
    spectrumPane.getItems().add(spectrumPlot);
    spectrumPane.getItems().add(spectraTab.getToolBar());
    // spectrumPane.setResizeWeight(1);
    // spectrumPane.setEnabled(false);
    // spectrumPane.setDividerSize(0);
    bottomPane.getItems().add(spectrumPane);
    panel.setCenter(bottomPane);
    // panel.setBorder(BorderFactory.createLineBorder(Color.black));
    return panel;
  }
}
