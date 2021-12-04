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

import io.mohamed.resolver.core.resolver.DependencyDownloader;
import io.mohamed.resolver.core.resolver.DependencyResolver;
import io.mohamed.resolver.core.util.Util;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.callback.FilesDownloadedCallback;
import io.mohamed.resolver.core.callback.ResolveCallback;
import io.mohamed.resolver.core.model.Dependency;
import io.mohamed.resolver.gui.settings.SettingsConstants;
import io.mohamed.resolver.gui.settings.SettingsManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * The application which manages the interface and settings for the dependencies resolver GUI
 *
 * @author Mohamed Tamer
 */
public class DependenciesResolverApplication extends Application implements Initializable {

  // the top-level menu bar control
  public MenuBar menu;
  // the gradle dependency text field
  public TextField gradleDependencyTbx;
  // the group ID text field
  public TextField groupIdTbox;
  // the artifact id text field
  public TextField artifactIdTbox;
  // the version text field
  public TextField versionTbox;
  // the choose file button
  public Button chooseFile;
  // the logs scrollPane
  public ScrollPane logs;
  // the resolve Button
  public Button resolveBtn;
  // the copy logs button
  public Button copyLogsBtn;
  // merge libraries setting value
  private boolean mergeLibraries;
  // the user-defined custom repositories setting value
  private ArrayList<String> repositories;
  // the jarOnly setting value
  private boolean jarOnly;
  // the verbose setting value
  private boolean verbose;
  // the jetify libraries setting value
  private boolean jetifyLibraries;
  // the logs pane
  private Pane logsPane;
  // the primary stage for the application
  private Stage primaryStage;
  // the logs as a string
  private String logsStr = "";

  public static void main(String[] args) {
    DependenciesResolverApplication.launch(args);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // load settings
    SettingsManager.load();
    loadSettings();
    // listen for settings changes
    SettingsManager.setOnSettingsChangeListener(this::loadSettings);
    // set the logs scrollPane content
    logsPane = new VBox();
    logs.setContent(logsPane);
    // add the top level menu bar
    Menu homeMenu = new Menu("Home");
    Menu toolsMenu = new Menu("Tools");
    Menu helpMenu = new Menu("Help");
    MenuItem aboutItem = new MenuItem("About");
    aboutItem.setOnAction(
        (event -> {
          Alert alert =
              new Alert(
                  AlertType.NONE,
                  Util.getVersion() + "\nA java library to resolve dependencies for a specific maven artifact.\nCreated and maintained by: Mohamed Tamer",
                  ButtonType.OK);
          alert.setTitle("Dependencies Resolver");
          alert.show();
        }));
    helpMenu.getItems().add(aboutItem);
    MenuItem optionsItem = new MenuItem("Options");
    MenuItem exitItem = new MenuItem("Exit");
    exitItem.setOnAction((event -> System.exit(0)));
    MenuItem clearCacheItem = new MenuItem("Clear Cache");
    clearCacheItem.setOnAction(
        (event -> {
          try {
            Alert clearCacheAlert =
                new Alert(
                    AlertType.WARNING,
                    "Clearing the Cache would result in deleting ALL cached libraries and resetting the settings to its default values.",
                    ButtonType.YES,
                    ButtonType.CANCEL);
            clearCacheAlert.setTitle("Clear Cache");
            clearCacheAlert.setHeaderText("Are you sure you want to delete the app's cache?");
            Optional<ButtonType> result = clearCacheAlert.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.YES)) {
              Util.clearCache();
              SettingsManager.resetSettings();
              appendLog("Clear Cache Successful..");
            }
          } catch (IOException | BackingStoreException e) {
            e.printStackTrace();
            appendLog("Failed to clear cache..", true);
          }
        }));
    optionsItem.setOnAction(this::showOptionsDialog);
    homeMenu.getItems().add(optionsItem);
    homeMenu.getItems().add(new SeparatorMenuItem());
    homeMenu.getItems().add(exitItem);
    toolsMenu.getItems().add(clearCacheItem);
    menu.getMenus().add(homeMenu);
    menu.getMenus().add(toolsMenu);
    menu.getMenus().add(helpMenu);

    // the choose file button
    final File[] selectedFile = new File[1];
    chooseFile.setOnMouseClicked(
        (event) -> {
          DirectoryChooser fileChooser = new DirectoryChooser();
          fileChooser.setTitle("Choose output directory");
          selectedFile[0] = fileChooser.showDialog(primaryStage);
          if (selectedFile[0] != null) {
            chooseFile.setText(selectedFile[0].getAbsolutePath());
          }
        });
    copyLogsBtn.setOnMouseClicked((event) -> {
      final Clipboard clipboard = Clipboard.getSystemClipboard();
      final ClipboardContent content = new ClipboardContent();
      content.putString(logsStr);
      clipboard.setContent(content);
    });
    resolveBtn.setOnMouseClicked(
        (event -> {
          // clear previous logs
          clearLogs();
          // make sure the user has input a dependency
          String gradleDependency = gradleDependencyTbx.getText();
          String groupID = groupIdTbox.getText();
          String artifactId = artifactIdTbox.getText();
          String version = versionTbox.getText();
          boolean gradleDependencyProvided = !gradleDependency.isEmpty();
          boolean artifactInfoProvided =
              !groupID.isEmpty() && !artifactId.isEmpty();
          if (!gradleDependencyProvided && !artifactInfoProvided) {
            Alert noDependencyProvidedAlert =
                new Alert(
                    AlertType.NONE,
                    "The dependency's group ID or artifact ID is missing.",
                    ButtonType.OK);
            noDependencyProvidedAlert.setTitle("Missing Artifact Information");
            noDependencyProvidedAlert.setHeaderText("Missing Artifact Information");
            noDependencyProvidedAlert.show();
            return;
          }
          // make sure the user has selected an output directory
          if (selectedFile[0] == null) {
            Alert noDependencyProvidedAlert =
                new Alert(AlertType.NONE, "No Output file was provided", ButtonType.OK);
            noDependencyProvidedAlert.setTitle("No Output File");
            noDependencyProvidedAlert.setHeaderText("No Output File");
            noDependencyProvidedAlert.show();
            return;
          }
          // parse the user defined dependnecy
          Dependency dependency;
          if (artifactInfoProvided) {
            dependency = new Dependency(groupID, artifactId, version);
          } else {
            try {
              dependency = Dependency.valueOf(gradleDependency);
            } catch (IllegalArgumentException e) {
              Alert failedToConvertDependency = new Alert(AlertType.WARNING);
              failedToConvertDependency.setTitle("Failed to parse dependency");
              failedToConvertDependency.setContentText(
                  "The dependency " + gradleDependency + " isn't well formatted.");
              failedToConvertDependency.show();
              return;
            }
          }
          // start fetching and downloading dependencies
          appendLog("Fetching Dependencies..");
          DependencyResolverCallback dependencyResolverCallback =
              new DependencyResolverCallback() {
                @Override
                public void dependencyPomDownloading(String url) {
                  appendLog("Downloading " + url);
                }

                @Override
                public void dependencyPomDownloaded(String url) {
                  appendLog("Downloaded " + url);
                }

                @Override
                public void dependencyPomParsing(String url) {
                  appendLog("Parsing " + url);
                }

                @Override
                public void dependencyPomParsed(String url) {
                  appendLog("Parsed " + url);
                }

                @Override
                public void dependencyFileDownloading(String url) {
                  appendLog("Downloading " + url);
                }

                @Override
                public void dependencyFileDownloaded(String url) {
                  appendLog("Downloaded " + url);
                }

                @Override
                public void merging(MergeStage stage) {
                  switch (stage) {
                    case MERGE_MANIFEST:
                      appendLog("Merging Android Manifests..");
                      break;
                    case MERGE_MANIFEST_FAILED:
                      appendLog("Failed to Merge Android Manifests..", true);
                      break;
                    case MERGE_CLASS_FILES:
                      appendLog("Merging Class Files..");
                      break;
                    case MERGE_MANIFEST_SUCCESS:
                      appendLog("Successfully Merged Android Manifests..");
                      break;
                    case START:
                      appendLog("Merging Libraries..");
                      break;
                    case MERGE_RESOURCES:
                      appendLog("Merging Resources..");
                      break;
                    case MERGE_RESOURCES_SUCCESS:
                      appendLog("Successfully Merged Resources..");
                    case MERGE_CLASS_FILES_SUCCESS:
                      appendLog("Successfully merged class files..");
                  }
                }

                @Override
                public void mergeSuccess() {
                  appendLog("Successfully Merged Libraries..");
                }

                @Override
                public void mergeFailed() {
                  appendLog("Failed to merge libraries..", true);
                }

                @Override
                public void verbose(String message) {
                  if (verbose) {
                    appendLog(message);
                  }
                }

                @Override
                public void error(String message) {
                  appendLog(message, true);
                }

                @Override
                public void info(String message) {
                  appendLog(message, false);
                }
              };
          Dependency finalDependency = dependency;
          ResolveCallback callback =
              (artifactFound, pomUrl, mavenRepo, dependencyList, dependency1) -> {
                if (artifactFound) {
                  appendLog("Successfully Resolved " + dependencyList.size() + " dependencies..");
                  FilesDownloadedCallback filesDownloadedCallback =
                      fileList -> {
                        appendLog("Successfully downloaded " + fileList.size() + " dependencies..");
                        File dependenciesDir = new File(selectedFile[0].getAbsolutePath());
                        if (!dependenciesDir.exists()) {
                          if (!dependenciesDir.mkdir()) {
                            System.err.println("Failed to create dependencies directory.");
                            return;
                          }
                        }
                        // copy all downloaded files to the output directory
                        appendLog("Copying libraries to output directory..");
                        for (File file : fileList) {
                          if (file == null) {
                            continue;
                          }
                          appendLog(
                              "Copying library: "
                                  + file.getAbsolutePath()
                                  + " to output directory "
                                  + dependenciesDir.getAbsolutePath());
                          File destFile =
                              new File(dependenciesDir.getAbsolutePath(), file.getName());
                          try {
                            Files.copy(
                                file.toPath(),
                                destFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                          } catch (Exception e) {
                            appendLog(e.toString(), true);
                          }
                        }
                        appendLog("Success!");
                        Platform.runLater(
                            () -> {
                              logs.setVvalue(logs.getVmax());
                              Alert alert =
                                  new Alert(
                                      AlertType.INFORMATION,
                                      "Files were saves successfully to "
                                          + selectedFile[0].getAbsolutePath(),
                                      ButtonType.OK);
                              alert.setTitle("Success!");
                              alert.setHeaderText("Success!");
                              alert.show();
                            });
                      };
                  appendLog("Downloading Dependencies");
                  new DependencyDownloader.Builder()
                      .setCallback(filesDownloadedCallback)
                      .setDependencyResolverCallback(dependencyResolverCallback)
                      .setDependencies(dependencyList)
                      .setJarOnly(jarOnly)
                      .setMainDependency(finalDependency)
                      .setMerge(mergeLibraries)
                      .setJetifyLibraries(jetifyLibraries)
                      .setRepositories(repositories)
                      .resolve();
                }
              };
          new DependencyResolver.Builder()
              .setDependency(dependency)
              .setCallback(callback)
              .setRepositories(repositories)
              .setDependencyResolverCallback(dependencyResolverCallback)
              .resolve();
        }));
    // print any uncaught exceptions to the user
    Thread.setDefaultUncaughtExceptionHandler(
        (t, e) -> {
          e.printStackTrace();
          appendLog(e.toString(), true);
        });
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    this.primaryStage = primaryStage;
    Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
    primaryStage.setTitle("Dependencies Resolver");
    Scene scene = new Scene(root, 520, 590);
    primaryStage.setScene(scene);
    primaryStage.show();
    primaryStage.setMinWidth(520);
    primaryStage.setMinHeight(620);
  }

  /** Clears any logs printed in the logs pane */
  private void clearLogs() {
    if (!logsPane.getChildren().isEmpty()) {
      logsPane.getChildren().removeAll(logsPane.getChildren());
    }
  }

  /** Loads settings defined by the user */
  private void loadSettings() {
    mergeLibraries =
        Boolean.parseBoolean(
            SettingsManager.getSettingForKey(SettingsConstants.MERGE_LIBRARIES_SETTINGS_KEY)
                .getValue()
                .toString());
    jarOnly =
        Boolean.parseBoolean(
            SettingsManager.getSettingForKey(SettingsConstants.JAR_ONLY_SETTINGS_KEY)
                .getValue()
                .toString());
    repositories =
        (ArrayList<String>)
            SettingsManager.getSettingForKey(SettingsConstants.REPOS_SETTING_KEY).getValue();
    verbose =
        Boolean.parseBoolean(
            SettingsManager.getSettingForKey(SettingsConstants.VERBOSE_SETTINGS_KEY)
                .getValue()
                .toString());
    jetifyLibraries =
        Boolean.parseBoolean(
            SettingsManager.getSettingForKey(SettingsConstants.JETIFY_SETTINGS_KEY)
                .getValue()
                .toString());
  }

  /**
   * Appends a log message to the logs pane
   *
   * @param msg the log message
   * @param error style the message as an error or not
   */
  public void appendLog(String msg, boolean error) {
    Platform.runLater(
        () -> {
          Label log = new Label();
          log.setText(msg);
          logsStr += msg + "\n";
          if (error) {
            log.setTextFill(Color.RED);
            log.setFont(Font.font(null, FontWeight.BOLD, 12));
          }
          log.setPadding(new Insets(5, 10, 5, 10));
          logsPane.getChildren().add(log);
        });
  }

  /**
   * Logs an info message
   *
   * @param message the message
   */
  public void appendLog(String message) {
    appendLog(message, false);
  }

  /**
   * Displays the settings dialog
   *
   * @param actionEvent the menu click event
   */
  private void showOptionsDialog(ActionEvent actionEvent) {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/optionsDialog.fxml"));
    try {
      Parent parent = fxmlLoader.load();
      Scene dialogScene = new Scene(parent, 600, 400);
      Stage stage = new Stage();
      stage.setTitle("Options");
      stage.initModality(Modality.APPLICATION_MODAL);
      stage.setScene(dialogScene);
      stage.showAndWait();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
