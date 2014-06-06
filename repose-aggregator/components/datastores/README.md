Repose uses "datastores" to store various data.

By default, the local datastore is used. A distributed datastore can be turned on, which
allows consisten data storage across multiple nodes in a cluster.

Datastores implement a specific API which defines behavior of datastores, outlined below:

    get(key) - access data associated with a specific key value

    put(key, value) - stores a piece of data and associates it with a key value
        put(key, value, time, unit) can be used to specify a duration of time in specific units
        this gives a specific lifetime to the data

    patch(key, value) - used to update a specific value in the datastore
        patch(key, value, time, unit) can be used to update a specific value in the datastore
        as well as the time values associated with it

    remove(key) - removes the key value pair associated with the provided key

    removeAll() - removes all elements in the datastore

    getName() - gets the name of the datastore


