// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library shelf.handlers.router;

import '../handler.dart';
import '../request.dart';
import '../response.dart';


/// A [Handler] that routes to other handlers based on the incoming request
/// (path and method). Note there can be several layers of routers to facilitate
/// modularity.
/// 
/// When the Router routes a request to a handler it adjusts the requests as follows:
/// * the routes path is removed from the start of the requests pathInfo
/// * the routes path is added to the end of the requests scriptName
/// 
/// e.g if original request had path info of `/banking' and scriptName was
/// `/abc` then when routing a request of `/banking/accounts` the new request
/// past to the handler will have a pathInfo of `/accounts` and scriptName of
/// `/abc/banking`
/// 
///
Router router() => new Router();



class Router {
  final List<_Route> _routes = <_Route>[];
  final _Route _fallbackRoute;
  
  Router([Handler fallbackHandler])
      : this._fallbackRoute = new _Route(fallbackHandler != null ?
          fallbackHandler : _send404, null, null);
  
  Router addRoute(Handler handler, {String path, String method}) {
    _routes.add(new _Route(handler, path, method));
    return this;
  }
  
  Handler get handler => _handleRequest;
  
  _handleRequest(Request request) {
    final route = _routes.firstWhere((r) => r.canHandle(request), 
        orElse: () => _fallbackRoute);
    return route.handle(request) ;
  }
}


class _Route {
  final Handler handler;
  final String path; 
  final String method;
  
  _Route(this.handler, this.path, this.method);
  
  bool canHandle(Request request) {
    return 
      (path == null || request.pathInfo.startsWith(path))
      &&
      (method == null || request.method == method);
  }
  
  handle(Request request) {
    final pathInfo = request.pathInfo;
    final scriptName = request.scriptName;
    
    // TODO: more robust handling of paths
    final newPathInfo = path == null ? pathInfo : pathInfo.substring(path.length);
    
    final newScriptName = path == null ? scriptName : scriptName + path; 
    
    final newRequest = request.copyWith(pathInfo: newPathInfo, 
        scriptName: newScriptName);
    
    return handler(newRequest);
  }

}

Response _send404(Request req) {
  return new Response.notFound("Not Found");
}