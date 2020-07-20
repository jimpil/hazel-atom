# hazel-atom

## What
A distributed atom-like type for Clojure, on top of `Hazelcast 4` (see `com.hazelcast.cp.IAtomicReference`),
implementing `IAtom`, `IAtom2`, `IDeref`, `IReference`, `IMeta` and `Closeable`.

## Where
[![Clojars Project](https://clojars.org/hazel-atom/latest-version.svg)](https://clojars.org/hazel-atom)


## How

A single namespace exists (`hazel_atom.core`), and a single constructor-fn is exposed within it (`hz-atom`). 
It will take 1 (`IAtomicReference` object), 2 (init-value) or 3 (metadata-map) arguments.

```clj
(-> (Hazelcast/newHazelcastInstance) ;; get the instance on this node
    .getCPSubsystem                  ;; get the new CP subsystem
    (.getAtomicReference "whatever") ;; get the reference of interest
    (hz-atom {:a 1 :b 2}))           ;; create the distributed atom

=> #object[hazel_atom.core.HazelcastAtom 0x61557a7f {:status :ready, :val {:a 1, :b 2}}]
```
The returned value can be used like a regular Clojure atom, with the added bonus that changes to it will be reflected
across the entire Hazelcast cluster.


## License

Copyright © 2020 Dimitrios Piliouras

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
