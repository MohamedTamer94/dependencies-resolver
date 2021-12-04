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

package io.mohamed.resolver.core.util;

import io.mohamed.resolver.core.model.Dependency;
import java.util.ArrayList;

public class AI2Dependency {

  public static final ArrayList<Dependency> appinventorDependencies = new ArrayList<>();

  static {
    appinventorDependencies.add(new Dependency("ch.acra", "acra", "4.4.0", "jar"));
    appinventorDependencies.add(new Dependency("com.caverock", "androidsvg", "1.4"));
    appinventorDependencies.add(new Dependency("androidx.annotation", "annotation", "1.0"));
    appinventorDependencies.add(new Dependency("androidx.appcompat", "appcompat", "1.1.0", "aar"));
    appinventorDependencies.add(
        new Dependency("androidx.asynclayoutinflater", "asynclayoutinflater", "1.0.0", "aar"));
    appinventorDependencies.add(new Dependency("androidx.cardview", "cardview", "1.0.0", "aar"));
    appinventorDependencies.add(
        new Dependency("androidx.constraintlayout", "constraintlayout", "1.1.3", "aar"));
    appinventorDependencies.add(
        new Dependency("androidx.constraintlayout", "constraintlayout", "1.1.3", "aar"));
    appinventorDependencies.add(
        new Dependency("androidx.collection", "collection", "1.0.0", "jar"));
    appinventorDependencies.add(
        new Dependency("androidx.constraintlayout", "constraintlayout-solver", "1.1.3", "jar"));
    appinventorDependencies.add(
        new Dependency("androidx.coordinaterlayout", "coordinatorlayout", "1.0.0", "aar"));
    appinventorDependencies.add(new Dependency("androidx.core", "core", "1.2.0", "aar"));
    appinventorDependencies.add(new Dependency("androidx.core", "core-runtime", "2.0.0", "aar"));
    appinventorDependencies.add(new Dependency("androidx.core", "core-common", "2.0.0", "jar"));
    appinventorDependencies.add(
        new Dependency("androidx.cursoradapter", "cursoradapter", "1.0.0", "aar"));
    appinventorDependencies.add(
        new Dependency("androidx.customview", "customview", "1.0.0", "aar"));
    appinventorDependencies.add(
        new Dependency("androidx.documentfile", "documentfile", "1.0.0", "aar"));
    appinventorDependencies.add(
        new Dependency("androidx.drawerlayout", "drawerlayout", "1.0.0", "aar"));
    appinventorDependencies.add(
        new Dependency("com.firebase", "firebase-client-android", "2.5.0", "jar"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.fragment:fragment:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'org.apache.httpcomponents:httpcore:4.3.2'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'org.apache.httpcomponents:httpmime:4.3.4'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'commons-codec:commons-codec:1.7'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'commons-fileupload:commons-fileupload:1.2.2'"));
    appinventorDependencies.add(Dependency.valueOf("implementation 'commons-io:commons-io:2.0.1'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'org.apache.commons:commons-lang3:3.10'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'org.apache.commons:commons-pool2:2.0'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'com.google.apis:google-api-services-fusiontables:v2-rev28-1.25.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'com.google.code.gson:gson:2.1'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'com.google.guava:guava:14.0.1'"));
    appinventorDependencies.add(Dependency.valueOf("implementation 'redis.clients:jedis:3.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'com.google.api-client:google-api-client:1.10.3-beta'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'com.google.api-client:google-api-client-android2:1.10.3-beta'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'com.google.http-client:google-http-client:1.10.3-beta'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'com.google.http-client:google-http-client-android2:1.10.3-beta'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'com.google.http-client:google-http-client-android3:1.10.3-beta'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'com.google.oauth-client:google-oauth-client:1.10.1-beta'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'org.osmdroid:osmdroid-android:5.6.5'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'net.cattaka:physicaloid:1.0.2'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'org.twitter4j:twitter4j-core:3.0.5'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'org.twitter4j:twitter4j-media-support:3.0.5'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.interpolator:interpolator:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.legacy:legacy-support-core-ui:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.legacy:legacy-support-core-utils:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.lifecycle:lifecycle-livedata:2.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.lifecycle:lifecycle-livedata-core:2.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.lifecycle:lifecycle-runtime:2.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.lifecycle:lifecycle-viewmodel:2.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.loader:loader:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.0.0'"));
    appinventorDependencies.add(Dependency.valueOf("implementation 'androidx.print:print:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.recyclerview:recyclerview:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.slidingpanelayout:slidingpanelayout:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.vectordrawable:vectordrawable:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'androidx.vectordrawable:vectordrawable-animated:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf(
            "implementation 'androidx.versionedparcelable:versionedparcelable:1.0.0'"));
    appinventorDependencies.add(
        Dependency.valueOf("implementation 'androidx.viewpager:viewpager:1.0.0'"));
  }

  public boolean dependencyExists(Dependency dependency) {
    for (Dependency appinventorDependency : appinventorDependencies) {
      if (appinventorDependency.compare(dependency, true)) {
        return true;
      }
    }
    return false;
  }
}
