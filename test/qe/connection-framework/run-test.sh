#!/bin/bash

virtualenv .
source bin/activate
pip install -r pip-requirements.txt

python test_connection_framework.py
