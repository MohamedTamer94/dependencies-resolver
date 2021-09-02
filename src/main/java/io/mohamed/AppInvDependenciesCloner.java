package io.mohamed;

import io.mohamed.model.Dependency;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

public class AppInvDependenciesCloner {
  public static final File depsDirectory = new File(Util.getLocalFilesDir(), "appinventor-deps");

  public boolean dependencyExists(Dependency dependency) {
    if (isDependenciesDownloaded()) {
      File[] dependenciesFiles = depsDirectory.listFiles();
      if (dependenciesFiles != null) {
        for (File dependencyFile : dependenciesFiles) {
          // this check, however, would resolve any two dependencies with same artifact name the same
          // TODO: make this implementation more exact
          if (FilenameUtils.removeExtension(dependencyFile.getName()).equals(dependency.getArtifactId())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public void download() throws GitAPIException {
    System.out.println("Cloning AppInventor libraries..");
    Git.cloneRepository()
        .setURI("https://github.com/mit-cml/extension-deps")
        .setDirectory(depsDirectory)
        .setBranch("master")
        .setProgressMonitor(
            new ProgressMonitor() {
              @Override
              public void start(int totalTasks) {
                // ignore
              }

              @Override
              public void beginTask(String title, int totalWork) {
                // ignore
              }

              @Override
              public void update(int completed) {
                // ignore
              }

              @Override
              public void endTask() {}

              @Override
              public boolean isCancelled() {
                return false;
              }
            })
        .call();
    System.out.println("Successfully cloned libraries..");
  }

  public boolean isDependenciesDownloaded() {
    try {
      if (!depsDirectory.exists()) {
        return false;
      }
      File[] deps = depsDirectory.listFiles();
      if (deps == null || deps.length == 0) {
        return false;
      }
      Repository repository = Git.open(depsDirectory).getRepository();
      for (Ref ref : repository.getRefDatabase().getRefs()) {
        if (ref.getObjectId() == null) continue;
        return true;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
}
