Steps for running python tests locally:

One time setup:
1. Install Python
2. curl -O https://pypi.python.org/packages/source/v/virtualenv/virtualenv-1.9.1.tar.gz 
3. tar xvfz virtualenv-1.9.1.tar.gz
4. cd virtualenv-1.9.1 
5. sudo python setup.py install  

In your python test directory:
1. cd to your test directory (ex. cd test/qe/api-validator-multimatch)
2. virtualenv .
3. source bin/activate
4. pip install -r pip-requirements.txt
5. mkdir -p usr/share/repose/filters 
6. copy your valve jar to usr/share/repose
7. copy your filter ears to usr/share/repose/filters

Run your tests!
1. python test_multimatch.py --print-log (for debug output)
