// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library shelf.handlers.router;

import 'package:stack_trace/stack_trace.dart';

import 'package:path/path.dart' as p;

import '../middleware.dart';
import '../util.dart';
import '../handler.dart';
import '../request.dart';
import '../stack.dart';


/// A [Handler] that routes to other handlers based on the incoming request
/// (path and method)
///
Router router() => new Router();


//typedef bool Matcher(Request request);


class Router {
  final List<_Route> _routes = <_Route>[];
  final Handler _fallbackHandler;
  
  Router([Handler fallbackHandler]) 
      : this._fallbackHandler = fallbackHandler; // TODO: default to a 404 handler
  
  Router addRoute(Handler handler, {String path, String method}) {
    _routes.add(new _Route(handler, path, method));
    return this;
  }
  
  Handler get handler => _handleRequest;
  
  _handleRequest(Request request) {
    final route = _routes.firstWhere((r) => r.canHandle(request));
    return route != null ? route.handle(request) : _fallbackHandler(request);
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

