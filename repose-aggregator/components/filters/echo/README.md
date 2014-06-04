The echo filter is a simple filter that responds to an HTTP Request with a 200 status,
along with an identical header to that in the initial response.

The request header is looped through on its header names, and each header name is looped through on its header values.
Each one of these header values is added to the httpResponse along with its respective header name.