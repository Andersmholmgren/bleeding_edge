library shelf.collection.map.test;

import 'package:shelf/src/collection/nested_map.dart';
import 'package:unittest/unittest.dart';

void main() {
  final Map<String, String> base = { 'foo': 'bar' };
  
  group('when passing local map via constructor then', () {
    test('it works without local map', () {
      final nmap = new NestedMap(base);
      expect(nmap['foo'], equals('bar'));
    });

    test('the local value overrides base', () {
      final nmap = new NestedMap(base, {'foo': 'fum'});
      expect(nmap['foo'], equals('fum'));
    });

    test('non overriden value fetches from base', () {
      final nmap = new NestedMap(base, {'blah': 'fum'});
      expect(nmap['foo'], equals('bar'));
      expect(nmap['blah'], equals('fum'));
    });    
  });

  group('when setting values via setter then', () {
    test('the local value overrides base', () {
      final nmap = new NestedMap(base)..['foo']='fum';
      expect(nmap['foo'], equals('fum'));
    });

    test('non overriden value fetches from base', () {
      final nmap = new NestedMap(base)..['blah']='fum';
      expect(nmap['foo'], equals('bar'));
      expect(nmap['blah'], equals('fum'));
    });    
  });    

}
