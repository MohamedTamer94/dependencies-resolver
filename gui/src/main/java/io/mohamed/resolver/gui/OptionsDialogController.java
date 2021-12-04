/*
 *  Copyright (c) 2021 Mohamed Tamer
 *   Permission is hereby granted, free of charge, to any person obtaining
 *   a copy of this software and associated documentation files (the
 *   "Software"), to deal in the Software without restriction, including
 *   without limitation the rights to use, copy, modify, merge, publish,
 *   distribute, sublicense, and/or sell copies of the Software, and to
 *   permit persons to whom the Software is furnished to do so, subject to
 *   the following conditions:
 *
 *   The above copyright notice and this permission notice shall be
 *   included in all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *   LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *   OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *   WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.mohamed.resolver.gui;

import io.mohamed.resolver.gui.settings.Setting;
import io.mohamed.resolver.gui.settings.SettingsConstants;
import io.mohamed.resolver.gui.settings.SettingsManager;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldListCell;

/**
 * The controller for the settings dialog
 *
 * @author Mohamed Tamer
 */
public class OptionsDialogController implements Initializable {

  // the listview for the user-defined custom repository urls
  public ListView<String> repositoriesList;
  // the add repository button
  public Button addRepository;
  // the remove repository button
  public Button removeRepository;
  // the merge libraries checkbox
  public CheckBox mergeLibrariesCheckbox;
  public CheckBox verboseCheckbox;
  // the verbose checkbox
  public CheckBox jarOnlyCheckbox;
  // the jetify libraries checkbox
  public CheckBox jetifyCheckbox;
  // the defined repository urls
  private ArrayList<String> repositories;
  // the merge libraries value
  private boolean mergeLibraries;
  // the jarOnly value
  private boolean jarOnly;
  // the verbose value
  private boolean verbose;
  // the jetify libraries value
  private boolean jetifyLibraries;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    SettingsManager.load();
    Setting repositoryUrlSetting =
        SettingsManager.getSettingForKey(SettingsConstants.REPOS_SETTING_KEY);
    if (repositoryUrlSetting.getValue() instanceof ArrayList) {
      repositories = (ArrayList<String>) repositoryUrlSetting.getValue();
    }
    Setting mergeLibrariesSetting =
        SettingsManager.getSettingForKey(SettingsConstants.MERGE_LIBRARIES_SETTINGS_KEY);
    Setting jarOnlySetting =
        SettingsManager.getSettingForKey(SettingsConstants.JAR_ONLY_SETTINGS_KEY);
    Setting verboseSetting =
        SettingsManager.getSettingForKey(SettingsConstants.VERBOSE_SETTINGS_KEY);
    Setting jetifySetting = SettingsManager.getSettingForKey(SettingsConstants.JETIFY_SETTINGS_KEY);
    mergeLibraries = Boolean.parseBoolean(mergeLibrariesSetting.getValue().toString());
    jarOnly = Boolean.parseBoolean(jarOnlySetting.getValue().toString());
    jetifyLibraries = Boolean.parseBoolean(jetifySetting.getValue().toString());
    verbose = Boolean.parseBoolean(verboseSetting.getValue().toString());
    repositoriesList.setItems(FXCollections.observableArrayList(repositories));
    repositoriesList.setEditable(true);
    mergeLibrariesCheckbox.setSelected(mergeLibraries);
    jarOnlyCheckbox.setSelected(jarOnly);
    verboseCheckbox.setSelected(verbose);
    jetifyCheckbox.setSelected(jetifyLibraries);
    repositoriesList.setCellFactory(TextFieldListCell.forListView());
    addRepository.setOnMouseClicked(
        (event -> {
          TextInputDialog textInputDialog = new TextInputDialog();
          textInputDialog.setContentText("Please enter the maven repository url..");
          textInputDialog.setHeaderText("Enter Maven Repository");
          Optional<String> result = textInputDialog.showAndWait();
          result.ifPresent(
              s -> {
                repositories.add(s);
                repositoryUrlSetting.setValue(repositories);
                repositoriesList.setItems(FXCollections.observableArrayList(repositories));
                SettingsManager.updateSettingValue(repositoryUrlSetting, repositories.toArray());
              });
        }));
    repositoriesList.setOnEditCommit(
        (event -> {
          repositories.remove(event.getIndex());
          repositories.add(event.getNewValue());
          repositoryUrlSetting.setValue(repositories);
          repositoriesList.setItems(FXCollections.observableArrayList(repositories));
          SettingsManager.updateSettingValue(repositoryUrlSetting, repositories.toArray());
        }));
    repositoriesList.setOnMouseClicked(
        (event -> {
          ObservableList<String> selectedItems =
              repositoriesList.getSelectionModel().getSelectedItems();
          if (!selectedItems.isEmpty()) {
            removeRepository.setDisable(false);
            removeRepository.setOnMouseClicked(
                (event1) -> {
                  repositories.remove(repositoriesList.getSelectionModel().getSelectedIndex());
                  repositoryUrlSetting.setValue(repositories);
                  repositoriesList.setItems(FXCollections.observableArrayList(repositories));
                  SettingsManager.updateSettingValue(repositoryUrlSetting, repositories.toArray());
                });
          } else {
            removeRepository.setDisable(true);
          }
        }));
    mergeLibrariesCheckbox.setOnAction(
        (event -> {
          mergeLibraries = mergeLibrariesCheckbox.isSelected();
          mergeLibrariesSetting.setValue(mergeLibraries);
          SettingsManager.updateSettingValue(mergeLibrariesSetting, mergeLibraries);
        }));
    jarOnlyCheckbox.setOnAction(
        (event -> {
          jarOnly = jarOnlyCheckbox.isSelected();
          jarOnlySetting.setValue(jarOnly);
          SettingsManager.updateSettingValue(jarOnlySetting, jarOnly);
        }));
    verboseCheckbox.setOnAction(
        (event -> {
          verbose = verboseCheckbox.isSelected();
          verboseSetting.setValue(verbose);
          SettingsManager.updateSettingValue(verboseSetting, verbose);
        }));
    jetifyCheckbox.setOnAction(
        (event -> {
          jetifyLibraries = jetifyCheckbox.isSelected();
          jetifySetting.setValue(jetifyLibraries);
          SettingsManager.updateSettingValue(jetifySetting, jetifyLibraries);
        }));
  }
}
