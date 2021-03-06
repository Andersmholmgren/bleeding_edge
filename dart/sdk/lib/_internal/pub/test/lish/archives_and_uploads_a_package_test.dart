// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:convert';

import 'package:scheduled_test/scheduled_test.dart';
import 'package:scheduled_test/scheduled_server.dart';

import '../../lib/src/exit_codes.dart' as exit_codes;
import '../descriptor.dart' as d;
import '../test_pub.dart';
import 'utils.dart';

main() {
  initConfig();
  setUp(d.validPackage.create);

  integration('archives and uploads a package', () {
    var server = new ScheduledServer();
    d.credentialsFile(server, 'access token').create();
    var pub = startPublish(server);

    confirmPublish(pub);
    handleUploadForm(server);
    handleUpload(server);

    server.handle('GET', '/create', (request) {
      request.response.write(JSON.encode({
        'success': {'message': 'Package test_pkg 1.0.0 uploaded!'}
      }));
      request.response.close();
    });

    pub.stdout.expect(startsWith('Uploading...'));
    pub.stdout.expect('Package test_pkg 1.0.0 uploaded!');
    pub.shouldExit(exit_codes.SUCCESS);
  });

  // TODO(nweiz): Once a multipart/form-data parser in Dart exists, we should
  // test that "pub lish" chooses the correct files to publish.
}
