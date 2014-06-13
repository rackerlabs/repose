The distributed datastore allows storing of information on the entire repose cluster.
Repose hosts a hash-ring object store across all nodes in the cluster. The node
data is stored on is determined by the hash of the key for that data.

It communicates via ports specified in the dist-datastore configuration file.

For a more in depth explanation of the dist-datastore,
see repose/documentation/raw/dist-datastore.txt