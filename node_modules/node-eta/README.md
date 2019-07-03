# ETA

Estimated time of arrival.

# API

## ctor(count)

Constructs ETA object for `count` of iterations.

```js
var Eta = require('node-eta');
var eta = new Eta(10);
```

## start()

Starts time measurement.

## iterate()

Notifies estimator that one more iteration has finished.

## getLengthInSeconds()

Returns number of seconds passed from start.

## getEstimatedLengthInSeconds()

Returns total length in seconds (estimated). 

## getIterationsPerSecond()

Returns current number of iterations per second.

## getPercentage()

Returns completion percentage.

## getEtaInSeconds()

Returns current value of ETA in seconds.

## getEtaFormatted()

Returns current value formatted for easy reading by human.

# License

BSD
