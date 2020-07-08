#! /bin/bash

base_url=http://localhost:9080/api/hello/

curl -v -w "\n" -H'Authorization: Bearer '$(cat token.jwt)\' $base_url$1
