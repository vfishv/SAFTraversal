# SAFTraversal

A study into storage traversal times for Storage Access Framework, as Google is forcing this API on all devs with Android Q. Operations that once completed in under a second will now take upwards of 25 seconds or longer. User retention, experience and battery life will be affected by forcing developers to use an API that moves times from under a second to over 25 seconds.

The following are some reports created by this application. It seems that the more folders you have, the slower SAF is.

Huawei Y5
    Without opening files
    Number of folders = 1239
    Number of files = 2272
    Files API Time = 436ms
    SAF API Time = 32741ms
    SAF is therefore a slowdown of 75.1x

Huawei Y5
    With opening files
    Number of folders = 1239
    Number of files = 2272
    Files API Time = 669ms
    SAF API Time = 45681ms
    SAF is therefore a slowdown of 68.3x


Samsung S8+
    Without opening files
    Number of folders = 894
    Number of files = 4247
    Files API Time = 205ms
    SAF API Time = 28305ms
    SAF is therefore a slowdown of 138.1x


Samsung S8+
    With opening files
    Number of folders = 894
    Number of files = 4247
    Files API Time = 527ms
    SAF API Time = 71625ms
    SAF is therefore a slowdown of 135.9x





Note: Put the phone in airplane mode while running the tests. Not that that accuracy matters too much. SAF is clearly much slower.
