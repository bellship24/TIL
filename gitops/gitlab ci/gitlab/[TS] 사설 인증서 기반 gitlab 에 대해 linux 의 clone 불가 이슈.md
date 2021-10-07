## 3.2. linux 에서 git clone 불가 이슈

### 3.2.1. 현상 : gitlab 서버에 https 접근 불가

- linux 환경에서 helm 으로 구축한 gitlab 의 어느 repo. 를 clone 하면 오류가 발생하며 실패한다.
- git clone 시 오류 log

``` bash
$ git clone https://gitlab.mylab.com/root/mycicd.git
Cloning into 'mycicd'...
fatal: unable to access 'https://gitlab.mylab.com/root/mycicd.git/': server certificate verification failed. CAfile: /etc/ssl/certs/ca-certificates.crt CRLfile: none
```

### 3.2.2. 분석 : 자체서명 인증서에 대한 ca.crt 누락

- clone 을 시도한 linux 환경은 git client 이다. https 로 gitlab 을 구축하였으므로 이 client 는 git clone 을 할 때 제일 먼저 gitlab 서버에서 인증서를 줄 것이다. 그리고 client 는 이 인증서를 ca.crt 로 해석하려 할 것이다. 그러나, gitlab 이 self-signed 인증서를 사용했다면, 이 서버의 인증서에 대한 ca.crt 가 client 의 CA chain 에 없을 것이다.

### 3.2.3. 해결 1 [o] : ca.crt 배포

- 그러므로 에러 로그에 나온대로 client 의 알맞은 경로에다가 ca.crt 를 넣어주면 해결된다.

``` bash
$ sudo bash -c " cat <<EOF >> /etc/ssl/certs/ca-certificates.crt
## added by cicd
-----BEGIN CERTIFICATE-----
MIIDAzCCAeugAwIBAgIRAL2lb2jQjm0SSIDuO9oTtJswDQYJKoZIhvcNAQELBQAw
GzEZMBcGA1UEAxMQZ2l0bGFiLmFpbGFiLmNvbTAeFw0yMTA2MTgwNDMzMzdaFw0y
MTA5MTYwNDMzMzdaMBsxGTAXBgNVBAMTEGdpdGxhYi5haWxhYi5jb20wggEiMA0G
CSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC+ryBfWGb7iiP0S29y6egKAhcfO+PB
Qs095qQUL+FDCNDr5PWiz8Oj/eRc0re+EQA4mup1inS2Qi7lM072P9aF3yJrlNyD
gp4UpZcJKNPh9rjLCUybyHfL5edG0X0eHnwtJD3Cm6xBvNfKDMBQbhAgS+OB9tlg
ONHYrjgJmB8G99869QOE9ARnm9k3jJnUMPKeZczjXsepWayiUX5hT4AR/6LoEEpg
VY0AMdCwByHFSo58Px/psQI0uMT1KN+3wrsQTkatVrUKQZm1uHrQGfM9+nl1e6eG
ZcVZga4YD5iwVwufTjTCeLMy7dgZiyTQWmbFBQPPChgr78eTNKkxlmkdAgMBAAGj
QjBAMA4GA1UdDwEB/wQEAwICpDAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5
M6Txx0XuivazDhEvHHgfoe1nDjANBgkqhkiG9w0BAQsFAAOCAQEAuRqlPxphzHzG
m5ltu+ZzZtjkB0NcOpe64tsqlJj+P8/4SrqaXthh+4avs31C9hfjAowm94lNdMTC
+QauQwXPT1hErpuXN3rYvTAND/BgD71StXn1JBg4K2WXTdefefRbuMVHnZPynIQD
1B5q14UPeazIsyhwqj8A5o/4rb9bqDimIZQX5eU5HJeyeGRXY2fFTRbcFfrOgs33
uRvj48sQuWaIyRQmxrM4kXbWHMXcy+0p/HhEufh6BjkpQgblz6bxgbDXxfU50PzC
MmYqr6Q7IqAnw9iwR+61IOvvcv0g0LU5XqPKQeAxkz2nsh0VVGG1xl3PXpakrvrV
vHN/TdLLbw==
-----END CERTIFICATE-----
EOF
"
```

- 인증서를 옮긴 후에, clone 을 해보면 잘 된다.

``` bash
$ git clone https://gitlab.mylab.com/root/mycicd.git
Cloning into 'mycicd'...
remote: Enumerating objects: 9, done.
remote: Counting objects: 100% (9/9), done.
remote: Compressing objects: 100% (9/9), done.
remote: Total 12 (delta 2), reused 0 (delta 0), pack-reused 3
Unpacking objects: 100% (12/12), done.
```