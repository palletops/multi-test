[Repository](https://github.com/palletops/multi-test) &#xb7;
[Issues](https://github.com/palletops/multi-test/issues) &#xb7;
[API docs](http://palletops.github.com/multi-test/api/0.1) &#xb7;
[Annotated source](http://palletops.github.com/multi-test/source/0.1/uberdoc.html) &#xb7;
[Release Notes](https://github.com/palletops/multi-test/blob/develop/ReleaseNotes.md)

# multi-test

[![Build Status](https://travis-ci.org/palletops/multi-test.png?branch=develop)](https://travis-ci.org/palletops/multi-test)

A test runner for clojure.test tests.

Tests can be run with a prescribed parallelism.  Tests can be run
multiple times with different bindings.

By default the test functions return a sequence of results.

## Dependency

Add the library coordinates to leiningen's `project.clj` file under the `:dependencies`
key of the `:dev` profile.

```clj
:dependencies [[com.palletops/multi-test "0.1.1"]]
```


## Usage

All functions are in the `com.palletops.multi-test` namespace.

### Running tests

The `test-ns` function runs tests in a given namespace.

```clj
(require '[com.palletops.multi-test :as multi-test])
(multi-test/test-ns 'my.ns-test)
```

The `test-all` function runs tests across all namespaces.

```clj
(require '[com.palletops.multi-test :as multi-test])
(multi-test/test-all)
```

The `test-vars` function runs the tests defined in a sequence of vars
passed as an argument.

```clj
(require '[com.palletops.multi-test :as multi-test])
(multi-test/test-vars [#'my.ns-test/my-test])
```

#### Filtering Tests to Run

Both `test-ns` and `test-all` take a `:run?` key in their option map,
which is used to pass a predicate used to filter the vars to be run.

The `var-matches-selectors?` predicate can be used to filter test vars
that have metadata keys that match one of a set of keywords.

#### Running Tests in Parallel

The `:threads` option key can be passed an integer, specifying the
number of threads to run tests on.

#### Running Tests with multiple Bindings

The `:bindings` option key can be passed a sequence of bindings maps.
All the tests will be run once for each of the specified bindings.

### Test Results

The default `:reporter` will cause all the test functions to run and
return a sequence of results.  Output is as for `clojure.test`.

You can specify different reporting behaviour by passing a function to
`:reporter`, as specified by `clojure.test/report`, with the addition
of being invoked with a `{:type :inititialize}` argument before any
tests are run, and a `{:type :results}` argument after all the tests
are run.

The `silent-report` reporter can be used when no printed output it
desired.

The `summary`, `print-summary` and `print-bad` functions are available
to help process the returned result sequence.

### Replacing clojure.test's runner

You can use the `test-ns-hook` function defined in a test namespace to
cause `clojure.test` to run tests with multi-test.  For example, to
run tests with two different bindings, using two threads, you could
use:

```clj
(defn test-ns-hook []
  (test-ns (ns-name *ns*)
    {:bindings [{#'*x* 1}{#'*x* 2}]
     :threads 2}))
```

## License

Copyright © 2014 Hugo Duncan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
