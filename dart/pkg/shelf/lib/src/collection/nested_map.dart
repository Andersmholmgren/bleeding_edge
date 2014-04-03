library shelf.collection.map;

// TODO(kevmoo): use UnmodifiableMapView from SDK once 1.4 ships
import 'package:collection/wrappers.dart';

// TODO: this should move somewhere else like the collection library
// TODO: This doesn't support removing entries from the map. Could support
// by adding a marker NULL object.
class NestedMap<K, V> extends DelegatingMap<K, V> {
  final Map<K, V> _localMap;
  
  NestedMap(Map<K, V> base, [Map<K, V> localMap]) 
    : this._localMap = localMap != null ? localMap : {}, 
      super(base);

  V operator [](K key) {
    final value = _localMap[key];
    return value != null ? value : super[key];
  }

  void operator []=(K key, V value) {
    _localMap[key] = value;
  }

}