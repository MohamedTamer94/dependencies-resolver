package io.mohamed.resolver.gui;

import io.mohamed.resolver.core.DependencyDownloader;
import io.mohamed.resolver.core.DependencyResolver;
import io.mohamed.resolver.core.callback.DependencyResolverCallback;
import io.mohamed.resolver.core.callback.FilesDownloadedCallback;
import io.mohamed.resolver.core.callback.ResolveCallback;
import io.mohamed.resolver.core.model.Dependency;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class DependenciesResolverApplication extends Application {

  private Button chooseFile;
  private TextField gradleDependencyTextBox;
  private TextField artifactIDTextBox;
  private TextField groupIDTextBox;
  private TextField versionTextBox;
  private ScrollPane logsPane;
  private Pane pane;
  private CheckBox mergeLibraries;

  public static void main(String[] args) {
    DependenciesResolverApplication.launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws IOException {
    Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
    primaryStage.setTitle("Dependencies Resolver");
    Scene scene = new Scene(root, 520, 620);
    primaryStage.setScene(scene);
    primaryStage.show();
    primaryStage.setMinWidth(520);
    primaryStage.setMinHeight(620);

    chooseFile = (Button) scene.lookup("#chooseFile");
    Button resolveButton = (Button) scene.lookup("#resolveBtn");
    gradleDependencyTextBox = (TextField) scene.lookup("#gradleDependencyTbx");
    artifactIDTextBox = (TextField) scene.lookup("#artifactIdTbox");
    groupIDTextBox = (TextField) scene.lookup("#groupIdTbox");
    versionTextBox = (TextField) scene.lookup("#versionTbox");
    mergeLibraries = (CheckBox) scene.lookup("#mergeLibraries");
    logsPane = (ScrollPane) scene.lookup("#logs");
    pane = new VBox();
    logsPane.setContent(pane);
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
    resolveButton.setOnMouseClicked(
        (event -> {
          clearLogs();
          String gradleDependency = gradleDependencyTextBox.getText();
          String groupID = groupIDTextBox.getText();
          String artifactId = artifactIDTextBox.getText();
          String version = versionTextBox.getText();
          boolean gradleDependencyProvided = !gradleDependency.isEmpty();
          boolean artifactInfoProvided =
              !groupID.isEmpty() && !artifactId.isEmpty() && !version.isEmpty();
          if (!gradleDependencyProvided && !artifactInfoProvided) {
            Alert noDependencyProvidedAlert =
                new Alert(
                    AlertType.NONE,
                    "Neither a gradle dependency, nor artifactId, groupId, and version were provided!",
                    ButtonType.OK);
            noDependencyProvidedAlert.setTitle("No Dependency Provided");
            noDependencyProvidedAlert.setHeaderText("No Dependency Provided");
            noDependencyProvidedAlert.show();
            return;
          }
          if (selectedFile[0] == null) {
            Alert noDependencyProvidedAlert =
                new Alert(AlertType.NONE, "No Output file was provided", ButtonType.OK);
            noDependencyProvidedAlert.setTitle("No Output File");
            noDependencyProvidedAlert.setHeaderText("No Output File");
            noDependencyProvidedAlert.show();
            return;
          }
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
                  appendLog(message);
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
                logsPane.setVvalue(logsPane.getVmax());
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
                        System.out.println("Copying libraries to output directory..");
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
                          } catch (IOException e) {
                            e.printStackTrace();
                          }
                        }
                        appendLog("Success!");
                        Platform.runLater(
                            () -> {
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
                      .setFilterAppInventorDependencies(false)
                      .setJarOnly(false)
                      .setMainDependency(finalDependency)
                      .setMerge(mergeLibraries.isSelected())
                      .setVerbose(true)
                      .setRepositories(new ArrayList<>())
                      .resolve();
                }
              };
          new DependencyResolver.Builder()
              .setDependency(dependency)
              .setCallback(callback)
              .setRepositories(new ArrayList<>())
              .setDependencyResolverCallback(dependencyResolverCallback)
              .resolve();
        }));
  }

  private void clearLogs() {
    if (!pane.getChildren().isEmpty()) {
      pane.getChildren().removeAll(pane.getChildren());
    }
  }

  private void appendLog(String message) {
    appendLog(message, false);
  }

  private void appendLog(String msg, boolean error) {
    Platform.runLater(
        () -> {
          System.out.println(msg);
          Label log = new Label(msg);
          if (error) {
            log.setTextFill(Color.RED);
            log.setFont(Font.font(null, FontWeight.BOLD, 12));
          }
          log.setPadding(new Insets(5, 10, 5, 10));
          pane.getChildren().add(log);
        });
  }
}
