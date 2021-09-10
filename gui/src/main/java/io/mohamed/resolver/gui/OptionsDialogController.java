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

public class OptionsDialogController implements Initializable {

  public ListView<String> repositoriesList;
  public Button addRepository;
  public Button removeRepository;
  public CheckBox mergeLibrariesCheckbox;
  public CheckBox verboseCheckbox;
  public CheckBox jarOnlyCheckbox;
  public CheckBox filterAppinventorDependenciesCheckbox;
  private ArrayList<String> repositories;
  private boolean mergeLibraries;
  private boolean jarOnly;
  private boolean verbose;
  private boolean filterAppinventorDependencies;

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
    Setting filterAppinventorDependenciesSetting =
        SettingsManager.getSettingForKey(
            SettingsConstants.FILTER_APPINVENTOR_DEPENDENCIES_SETTINGS_KEY);
    Setting verboseSetting =
        SettingsManager.getSettingForKey(SettingsConstants.VERBOSE_SETTINGS_KEY);
    mergeLibraries = Boolean.parseBoolean(mergeLibrariesSetting.getValue().toString());
    jarOnly = Boolean.parseBoolean(jarOnlySetting.getValue().toString());
    filterAppinventorDependencies =
        Boolean.parseBoolean(filterAppinventorDependenciesSetting.getValue().toString());
    verbose = Boolean.parseBoolean(verboseSetting.getValue().toString());
    repositoriesList.setItems(FXCollections.observableArrayList(repositories));
    repositoriesList.setEditable(true);
    mergeLibrariesCheckbox.setSelected(mergeLibraries);
    jarOnlyCheckbox.setSelected(jarOnly);
    filterAppinventorDependenciesCheckbox.setSelected(filterAppinventorDependencies);
    verboseCheckbox.setSelected(verbose);
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
    filterAppinventorDependenciesCheckbox.setOnAction(
        (event -> {
          filterAppinventorDependencies = filterAppinventorDependenciesCheckbox.isSelected();
          filterAppinventorDependenciesSetting.setValue(filterAppinventorDependencies);
          SettingsManager.updateSettingValue(
              filterAppinventorDependenciesSetting, filterAppinventorDependencies);
        }));
    verboseCheckbox.setOnAction(
        (event -> {
          verbose = verboseCheckbox.isSelected();
          verboseSetting.setValue(verbose);
          SettingsManager.updateSettingValue(verboseSetting, verbose);
        }));
  }
}
