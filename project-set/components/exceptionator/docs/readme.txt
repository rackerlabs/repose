*** Here's a real short description of Exceptionator's intended use...

First off, I'm used HTTP header extensions to communicate with the Exceptionator.  These headers will be used
to trigger one of several module-specific exceptions in the filter.

X-Runtime-Exception-Message := contains message used for the thrown exception
The type 'IrishMythicalHeroException' will be thrown using this message.
IrishMythicalHeroException extends RuntimeException.
The resulting http status returned from a request should contain a 502.