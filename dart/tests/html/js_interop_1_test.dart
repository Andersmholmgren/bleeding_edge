// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file

#library('JsInterop1Test');
#import('../../lib/unittest/unittest.dart');
#import('../../lib/unittest/html_config.dart');
#import('dart:html');
#import('dart:json');

injectSource(code) {
  final script = new ScriptElement();
  script.type = 'text/javascript';
  script.innerHTML = code;
  document.body.nodes.add(script);
}

main() {
  useHtmlConfiguration();
  var callback;

  test('js-to-dart-post-message', () {
    callback = expectAsync1((e) {
      Expect.equals('hello', e.data);
      window.on.message.remove(callback);
    });
    window.on.message.add(callback);
    injectSource("window.postMessage('hello', '*');");
  });
}
