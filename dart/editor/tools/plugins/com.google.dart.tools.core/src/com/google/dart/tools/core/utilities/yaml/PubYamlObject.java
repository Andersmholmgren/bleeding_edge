/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.utilities.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An object for reading from and writing to pubspec.yaml
 * 
 * @coverage dart.tools.core.utilities
 */
public class PubYamlObject {

  public String name;
  public String version;
  public String author;
  public List<String> authors;
  public String description;
  public String homepage;
  public String documentation;
  public Map<String, Object> environment;
  public Map<String, Object> dependencies;
  public Map<String, Object> dev_dependencies;
  public ArrayList<Object> transformers;

  public PubYamlObject() {
  }
}
