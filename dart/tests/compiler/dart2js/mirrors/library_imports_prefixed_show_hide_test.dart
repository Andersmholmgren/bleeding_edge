// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import "dart:mirrors";

import "package:async_helper/async_helper.dart";

import "mirrors_test_helper.dart";
import "../../../lib/mirrors/library_imports_prefixed_show_hide_test.dart";

main() {
  asyncTest(() => analyze("library_imports_prefixed_show_hide_test.dart").
      then((MirrorSystem mirrors) {
    test(mirrors);
  }));
}