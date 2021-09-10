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

package io.mohamed.resolver.core.callback;

/**
 * An interface implemented by all the dependencies resolver project modules to listen for the
 * callbacks invoked for resolving and downloading dependencies
 *
 * @author Mohamed Tamer
 */
public interface DependencyResolverCallback {

  /**
   * Called when a dependency .pom file is being downloaded
   *
   * @param url the dependency .pom file url
   */
  void dependencyPomDownloading(String url);

  /**
   * Called when a dependency .pom file have been downloaded
   *
   * @param url the dependency .pom file url
   */
  void dependencyPomDownloaded(String url);

  /**
   * Called when a dependency .pom file is being parsed
   *
   * @param url the dependency .pom file url
   */
  void dependencyPomParsing(String url);

  /**
   * Called when a dependency .pom file has been parsed
   *
   * @param url the dependency .pom file url
   */
  void dependencyPomParsed(String url);

  /**
   * Called when the dependency file is being downloaded
   *
   * @param url the dependency file url
   */
  void dependencyFileDownloading(String url);

  /**
   * Called when the dependency file has been downloaded
   *
   * @param url the dependency file url
   */
  void dependencyFileDownloaded(String url);

  /**
   * Called when merging is in progress
   *
   * @param stage the stage which the merger is currently doing
   */
  void merging(MergeStage stage);

  /** Called when merging finishes successfully */
  void mergeSuccess();

  /** Called when merging fails */
  void mergeFailed();

  /**
   * Called when a verbose message is logged
   *
   * @param message the message string
   */
  void verbose(String message);

  /**
   * Called when an error message is logged
   *
   * @param message the message string
   */
  void error(String message);

  /**
   * Called when an info message is logged
   *
   * @param message the message string
   */
  void info(String message);

  /** An enum to represent the stage library merging is at. */
  enum MergeStage {
    /** Merging has started */
    START,
    /** Merging android manifest files has started */
    MERGE_MANIFEST,
    /** Merging android manifest has finished successfully */
    MERGE_MANIFEST_SUCCESS,
    /** Merging android manifest has failed */
    MERGE_MANIFEST_FAILED,
    /** Merging class files has started */
    MERGE_CLASS_FILES,
    /** Merging class files was successful */
    MERGE_CLASS_FILES_SUCCESS,
    /** Merging android resources has started */
    MERGE_RESOURCES,
    /** Merging android resources was successful */
    MERGE_RESOURCES_SUCCESS
  }
}
