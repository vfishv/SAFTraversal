# SAFTraversal

A study into storage traversal times for Storage Access Framework, as Google is forcing this API on all devs with Android Q. Operations that once completed in under a second will now take upwards of 25 seconds or longer. User retention, experience and battery life will be affected by forcing developers to use an API that moves times from under a second to over 25 seconds.

Here is the Android bug report. https://issuetracker.google.com/issues/130261278

The following are some reports created by this application. It seems that the more folders you have, the slower SAF is.

Huawei Y5
* Without opening files
* Number of folders = 1239
* Number of files = 2272
* Files API Time = 522ms
* SAF API Time = 19904ms
* SAF is therefore a slowdown of 38.1x

Huawei Y5
* With opening files
* Number of folders = 1239
* Number of files = 2272
* Files API Time = 713ms
* SAF API Time = 32776ms
* SAF is therefore a slowdown of 46.0x


Samsung S8+
* Without opening files
* Number of folders = 901
* Number of files = 4250
* Files API Time = 181ms
* SAF API Time = 8264ms
* SAF is therefore a slowdown of 45.7x


Samsung S8+
* With opening files
* Number of folders = 901
* Number of files = 4250
* Files API Time = 325ms
* SAF API Time = 25463ms
* SAF is therefore a slowdown of 78.3x


Note: Put the phone in airplane mode while running the tests. Not that that accuracy matters too much. SAF is clearly much slower.
