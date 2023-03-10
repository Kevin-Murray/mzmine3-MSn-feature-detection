/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.TableViewUtils;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

/**
 * Window to show all spectral libraries matches from selected scan or feature list match
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de) & SteffenHeu (s_heuc03@uni-muenster.de)
 */
public class SpectraIdentificationResultsWindowFX extends SimpleTab {

  private static final Logger logger = Logger.getLogger(
      SpectraIdentificationResultsWindowFX.class.getName());

  private final Font headerFont = new Font("Dialog Bold", 16);
  private final ObservableList<SpectralDBAnnotation> totalMatches;
  private final ObservableList<SpectralDBAnnotation> visibleMatches;

  private final Map<SpectralDBAnnotation, SpectralMatchPanelFX> matchPanels;
  private final VBox mainBox;
  // couple y zoom (if one is changed - change the other in a mirror plot)
  private boolean isCouplingZoomY;
  private final Label noMatchesFound;
  private final BorderPane pnMain;
  private int currentIndex = 0;
  private int showBestN = 15;
  private Label shownMatchesLbl;

  public SpectraIdentificationResultsWindowFX() {
    super("Spectral matches", false, false);

    totalMatches = FXCollections.observableList(Collections.synchronizedList(new ArrayList<>()));
    visibleMatches = FXCollections.observableArrayList();
    // any number of rows

    noMatchesFound = new Label("I'm working on it");
    noMatchesFound.setFont(headerFont);
    // yellow
    noMatchesFound.setTextFill(Color.web("0xFFCC00"));

    HBox btnmenu = createButtonMenu();
    mainBox = new VBox(noMatchesFound, btnmenu);

//    ScrollPane scrollPane = new ScrollPane();
//    scrollPane.setFitToHeight(true);
//    scrollPane.setFitToWidth(true);
//    scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
//    scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);

    pnMain = new BorderPane(createTable());
    pnMain.setTop(mainBox);
    pnMain.setMinWidth(700);
    pnMain.setMinHeight(500);
    setContent(pnMain);

    matchPanels = new HashMap<>();
    setCoupleZoomY(true);
  }

  private void createTopMenu() {
//    MenuBar menuBar = new MenuBar();
//    // menuBar.add(new WindowsMenu());
//
//    Menu menu = new Menu("Menu");
//
//    menuBar.getMenus().add(menu);
//    pnMain.setTop(menuBar);
  }

  @NotNull
  private HBox createButtonMenu() {
    // set font size of chart
    Button btnSetup = new Button("Setup");
    btnSetup.setOnAction(e -> {
      MZmineCore.runLater(() -> {
        if (MZmineCore.getConfiguration()
                .getModuleParameters(SpectraIdentificationResultsModule.class).showSetupDialog(true)
            == ExitCode.OK) {
          showExportButtonsChanged();
        }
      });
    });

    CheckBox cbCoupleZoomY = new CheckBox("Couple y-zoom");
    cbCoupleZoomY.setSelected(true);
    cbCoupleZoomY.setOnAction(e -> setCoupleZoomY(cbCoupleZoomY.isSelected()));

    var prev = new Button("<<");
    prev.setOnAction(event -> showPrevious());
    var next = new Button(">>");
    next.setOnAction(event -> showNext());

    var showN = new TextField("15");
    showN.textProperty().addListener((o, ov, nv) -> {
      try {
        if (!nv.isBlank()) {
          showBestN = Integer.parseInt(nv);
        }
      } catch (Exception ex) {
      }
    });

    shownMatchesLbl = new Label("");

    var hBox = new HBox(btnSetup, cbCoupleZoomY, prev, showN, next, shownMatchesLbl);
    hBox.setAlignment(Pos.CENTER_LEFT);
    hBox.setSpacing(5);
    return hBox;
  }

  @NotNull
  private TableView<SpectralDBAnnotation> createTable() {
    TableView<SpectralDBAnnotation> tableView = new TableView<>(visibleMatches);
    tableView.setPadding(Insets.EMPTY);
    TableColumn<SpectralDBAnnotation, SpectralDBAnnotation> column = new TableColumn<>();
    column.setSortable(false);

    column.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
    column.setCellFactory(param -> new TableCell<>() {
      private final BorderPane pane;

      {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        pane = new BorderPane();
        this.setPadding(Insets.EMPTY);
        this.setStyle("-fx-padding: 0 0 0 0;");
        setBorder(Border.EMPTY);
      }

      @Override
      protected void updateItem(final SpectralDBAnnotation item, final boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setGraphic(null);
        } else {
          pane.setCenter(getChart(item));
          setGraphic(pane);
        }
      }
    });

    tableView.getColumns().add(column);
    TableViewUtils.autoFitLastColumn(tableView);
    return tableView;
  }

  private Node getChart(final SpectralDBAnnotation item) {
    return matchPanels.containsKey(item) ? matchPanels.get(item) : new Label("No chart found");
  }

  public void setCoupleZoomY(boolean selected) {
    isCouplingZoomY = selected;

    synchronized (matchPanels) {
      matchPanels.values().stream().filter(Objects::nonNull)
          .forEach(pn -> pn.setCoupleZoomY(selected));
    }
  }

  /**
   * Add a new match and sort the view. Call from {@link Platform#runLater}.
   *
   * @param match
   */
  public synchronized void addMatches(SpectralDBAnnotation match) {
    if (!totalMatches.contains(match)) {
      // add
      totalMatches.add(match);

      // sort and show
      sortTotalMatches();
    }
  }

  /**
   * add all matches and sort the view. Call from {@link Platform#runLater}.
   *
   * @param matches
   */
  public void addMatches(List<SpectralDBAnnotation> matches) {
    if (matches.isEmpty()) {
      return;
    }
    totalMatches.addAll(matches);
    // sort and show
    sortTotalMatches();
  }

  /**
   * Sort all matches and renew panels
   */
  public void sortTotalMatches() {
    if (totalMatches.isEmpty()) {
      setMatchingFinished();
      return;
    } else {
      mainBox.getChildren().remove(noMatchesFound);
    }

    // reversed sorting (highest cosine first
    synchronized (totalMatches) {
      totalMatches.sort((SpectralDBAnnotation a, SpectralDBAnnotation b) -> Double.compare(
          b.getSimilarity().getScore(), a.getSimilarity().getScore()));
    }
    // renew layout and show
    renewLayout();
  }

  public void setMatchingFinished() {
    if (totalMatches.isEmpty()) {
      noMatchesFound.setText("Sorry no matches found.\n"
                             + "Please visualize NIST spectral search results through NIST MS Search software.");
      noMatchesFound.setTextFill(Color.RED);
    }
  }

  /**
   * Removes panels and puts them in order.
   */
  private void renewLayout() {
    currentIndex = 0;
    handleLayoutChangeIndex();
  }

  private void handleLayoutChangeIndex() {
    MZmineCore.runLater(() -> {
      // add all panel in order
      synchronized (totalMatches) {
        // select first 15 matches
        var best = totalMatches.stream().skip(currentIndex).limit(showBestN).toList();
        // add all
        for (SpectralDBAnnotation match : best) {
          if (!matchPanels.containsKey(match)) {
            // add and skip matches without datapoints
            SpectralMatchPanelFX pn = new SpectralMatchPanelFX(match);
            pn.setCoupleZoomY(isCouplingZoomY);
//          pn.prefWidthProperty().bind(this.widthProperty());
            matchPanels.put(match, pn);
          }
        }
        visibleMatches.setAll(best);
        shownMatchesLbl.setText(
            "(%d-%d of %d)".formatted(currentIndex, currentIndex + visibleMatches.size(),
                totalMatches.size()));
      }
    });
  }

  private void showNext() {
    currentIndex = Math.min(currentIndex + showBestN, totalMatches.size() - 1);
    handleLayoutChangeIndex();
  }

  private void showPrevious() {
    currentIndex = Math.max(currentIndex - showBestN, 0);
    handleLayoutChangeIndex();
  }

  private void showExportButtonsChanged() {
    if (matchPanels == null) {
      return;
    }
    matchPanels.values().forEach(pn -> {
      pn.applySettings(MZmineCore.getConfiguration()
          .getModuleParameters(SpectraIdentificationResultsModule.class));
    });
  }

}
