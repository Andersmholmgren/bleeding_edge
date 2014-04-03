library shelf.collection.map;

// TODO(kevmoo): use UnmodifiableMapView from SDK once 1.4 ships
import 'package:collection/wrappers.dart';
import 'dart:collection';

// TODO: this should move somewhere else like the collection library
// TODO: This doesn't support removing entries from the map. Could support
// by adding a marker NULL object.
class NestedMap<K, V> extends DelegatingMap<K, V> {
  final Map<K, V> _localMap;
  
  NestedMap(Map<K, V> base, [Map<K, V> localMap]) 
    : this._localMap = localMap != null ? localMap : {}, 
      super(base);

  @override
  V operator [](K key) {
    final value = _localMap[key];
    return value != null ? value : super[key];
  }

  @override
  void operator []=(K key, V value) {
    _localMap[key] = value;
  }

  void addAll(Map<K, V> other) {
    _localMap.addAll(other);
  }

  void clear() {
    _localMap.clear();
    super.clear();
  }

  bool containsKey(Object key) => 
      _localMap.containsKey(key) || super.containsKey(key);

  bool containsValue(Object value) => 
      _localMap.containsValue(value) || super.containsValue(value);

  void forEach(void f(K key, V value)) {
    Maps.forEach(this, f);
  }

  bool get isEmpty => _localMap.isEmpty && super.isEmpty;

  bool get isNotEmpty => !isEmpty;

  // TODO: ouch
  Iterable<K> get keys => 
      []..addAll(_localMap.keys)
      ..addAll(super.keys.where((k) => !_localMap.keys.contains(k)));

  int get length => keys.length;

  V putIfAbsent(K key, V ifAbsent()) => 
      super.containsKey(key) ? null : _localMap.putIfAbsent(key, ifAbsent);

  // TODO: there is a bug here as it could be in the base map but we don't want 
  // to touch it. Need to support a NULL marker or similar
  V remove(Object key) => _localMap.remove(key);

  Iterable<V> get values => keys.map((k) => this[k]);

}