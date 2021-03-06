**목차**

- [1. data source 로 prometheus 를 연동할 때 `Bad Gateway` 에러 발생](#1-data-source-로-prometheus-를-연동할-때-bad-gateway-에러-발생)
  - [1.1. 현상](#11-현상)
  - [1.2. 분석](#12-분석)
  - [1.3. 해결](#13-해결)
    - [1.3.1. grafana 파드 안에 alias 설정하여 prometheus 서버에 ping 및 data source 설정 테스트](#131-grafana-파드-안에-alias-설정하여-prometheus-서버에-ping-및-data-source-설정-테스트)
    - [1.3.2. prometheus, grafana 서버의 https 의 ca 공개키 추가 혹은 https 대신 http 사용](#132-prometheus-grafana-서버의-https-의-ca-공개키-추가-혹은-https-대신-http-사용)

---

# 1. data source 로 prometheus 를 연동할 때 `Bad Gateway` 에러 발생

## 1.1. 현상

![](/.uploads/2021-08-15-01-55-30.png)

- 위와 같이 data source 로 특정 prometheus 의 URL 을 입력하고 `Save & test` 했는데 `Bad Gateway` 이슈가 발생했다.

## 1.2. 분석

grafana 파드 안에서 prometheus 의 domain 으로 curl 테스트

- 502 Bad Gateway 에러가 발생했다. grafana 서버가 prometheus 서버와 통신이 잘 되는지 `curl` 로 테스트가 필요하다.
- prometheus 와 grafana 를 helm 으로 k8s 위에 nginx-ingress 를 사용해 구축했다.

``` bash
$ k exec -ti grafana-7f8458bcb-bxlcf -- bash
$ ping prometheus.ailab.com
PING prometheus.ailab.com (35.186.238.101): 56 data bytesping: permission denied (are you root?)
```

- 일단, grafana 컨테이너 안에서 권한 문제로 ping 이 제대로 수행되지 않았다.
- 그러나, prometheus.ailab.com 의 resolve 된 IP 가 전혀 모르는 IP 이다.
- 즉, prometheus 서버 URL 에 대한 resolve 를 하지 못하고 누군가 이 도메인으로 올린 공인 IP 로 해석을 했다.
- 그러므로 Alias 설정이 필요하다.

## 1.3. 해결

### 1.3.1. grafana 파드 안에 alias 설정하여 prometheus 서버에 ping 및 data source 설정 테스트

- grafana 파드 안에 유저는 `/etc/hosts` 를 수정할 수 있는 권한이 없다. 그러므로 yaml 에 `hostAlias` 를 수정하자
- grafana 를 helm 으로 배포했으므로 grafana 파드 안에 hostAlias 를 설정하기 위해서는 override-values.yaml 을 업데이트하여 `helm upgrade` 를 하면 된다.

override-values.yaml 작성

``` bash
cat << EOF >> override-values.yaml
hostAliases:
- ip: "10.231.238.232"
  hostnames:
  - "prometheus.ailab.com"  
EOF
```

release 배포

``` bash
helm upgrade --install \
grafana \
grafana/grafana \
-n monitoring --create-namespace \
-f override-values.yaml
```

### 1.3.2. prometheus, grafana 서버의 https 의 ca 공개키 추가 혹은 https 대신 http 사용

또한 prometheus 서버에 https 사용 한다면 CA 공개키가 grafana 파드에 있어야 한다.
self-signed 인증서를 썻다면 cert-manager 를 활용해 인증서를 만들고 prometheus 와 grafana 를 배포한 뒤, grafana 파드에 ca 공개키를 추가해주면 된다.
하지만, 여기서 나는 prometheus 와 grafana 를 http 로 배포하여 해결하였다.