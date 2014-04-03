// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'package:shelf/shelf.dart' as shelf;
import 'package:shelf/shelf_io.dart' as io;

void main() {
  var publicRoute = const shelf.Stack()
      .addHandler(_echoRequest);

  var bankingRoute = const shelf.Stack()
//      .addMiddleware(authenticator);
      .addHandler(_echoRequest);
      
  
  var router = shelf.router()
      .addRoute(publicRoute, path: 'public')
      .addRoute(bankingRoute, path: 'banking')
      .handler;
  
  var handler = const shelf.Stack()
      .addMiddleware(shelf.logRequests())
      .addHandler(router);

  io.serve(handler, 'localhost', 8080).then((server) {
    print('Serving at http://${server.address.host}:${server.port}');
  });
}

shelf.Response _echoRequest(shelf.Request request) {
  return new shelf.Response.ok('Request for "${request.pathInfo}"');
}
