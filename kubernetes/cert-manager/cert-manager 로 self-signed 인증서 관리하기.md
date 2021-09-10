**목차**

- [1. 요약](#1-요약)
- [2. SelfSigned](#2-selfsigned)
  - [2.1. SelfSigned Issuer 배포하는 방법](#21-selfsigned-issuer-배포하는-방법)
  - [2.2. CA Issuer Bootstrap](#22-ca-issuer-bootstrap)
  - [2.3. 주의 사항](#23-주의-사항)
- [3. 생성한 인증서를 파일로 decode 하는 방법](#3-생성한-인증서를-파일로-decode-하는-방법)

**참고**

- [[cert-manager docs] SelfSigned](https://cert-manager.io/docs/configuration/selfsigned/)

---

# 1. 요약

> cert-manager 에서 self-signed 인증서의 용도, 생성 방법, 사용 방법을 검토 및 검증해보자.

# 2. SelfSigned

`SelfSigned` issuer 는 CA(Certificate Authority, 인증 기관) 자체를 나타내지는 않지만 대신 주어진 개인키를 사용하여 인증서를 "자체 서명" 할 것임을 나타낸다. 즉, 인증서의 개인키는 인증서 자체에 서명하는 데 사용된다.
이 `Issuer` 타입은 커스텀 PKI(Public Key Infrastructure) 에 대한 루트 인증서를 부트스트랩하거나 간단한 임시 인증서를 만드는 데 유용하다.

SelfSigned issuer 에는 보안 문제 등 주의 사항이 있다. 일반적으로 SelfSigned issuer 보다는 CA issuer 를 사용하는 것이 좋다. 즉, SelfSigned issuer 는 초기에 CA issuer 를 부트스트랩하는 데 매우 유용하다.

## 2.1. SelfSigned Issuer 배포하는 방법

SelfSigned issuer 는 k8s 의 다른 리소스에 종속되지 않으므로 구성이 간단하다. issuer spec 에 `SelfSigned: {}` 만 있으면 된다.

``` yaml
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: selfsigned-issuer
  namespace: sandbox
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-cluster-issuer
spec:
  selfSigned: {}
```

배포 확인

``` bash
$ kubectl get issuers  -n sandbox -o wide selfsigned-issuer
NAME                READY   STATUS                AGE
selfsigned-issuer   True                          2m

$ kubectl get clusterissuers -o wide selfsigned-cluster-issuer
NAME                        READY   STATUS   AGE
selfsigned-cluster-issuer   True             3m
```

## 2.2. CA Issuer Bootstrap

앞서 말했듯이 selfsigned issuer 의 권고되는 사용 방법 중 하나는 private 한 PKI 에서 cert-manager CA issuer 를 포함하여 custom root 인증서를 bootstaping 하는 것이다.

아래 yaml 은 SelfSigned issuer 를 생성하고 root 인증서를 발급하며 이 인증서를 CA issuer 로 사용하는 예제이다.

``` yaml
apiVersion: v1
kind: Namespace
metadata:
  name: sandbox
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: my-selfsigned-ca
  namespace: sandbox
spec:
  isCA: true
  commonName: my-selfsigned-ca
  secretName: root-secret
  privateKey:
    algorithm: ECDSA
    size: 256
  issuerRef:
    name: selfsigned-issuer
    kind: ClusterIssuer
    group: cert-manager.io
---
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: my-ca-issuer
  namespace: sandbox
spec:
  ca:
    secretName: root-secret
```

- commonName: CA 이름
- secretName: 인증서가 저장될 Secret 이름

## 2.3. 주의 사항

신뢰

- SelfSigned 인증서를 사용하는 클라이언트는 사전에 인증서가 없으면 인증서를 신뢰할 수 없다. 따라서 대부분의 클라이언트는 메시지 가로채기(man-in-the-middle) 공격이 발생할 경우 보안에 영향을 미치는 "TOFU"(첫 사용에 대한 신뢰)를 강제로 사용할 수 밖에 없다.

인증서 유효성

- 자체 서명된 인증서의 부작용 중 하나는 해당 주체 DN(Distinguished Name) 과 발급자 DN 이 동일하다는 것이다.
- 그러나 자체 서명된 인증서에는 기본적으로 설정된 subject DN 이 없다. 인증서의 subject DN 을 수동으로 설정하지 않으면 issuer DN 이 비어 있고 인증서가 기술적으로 유효하지 않게 된다.
- 빈 Issuer DN 을 가진 인증서를 사용하는 경우 앱이 중단될 위험이 있다. 이를 방지하려면 SelfSigned 인증서의 subject 를 설정해야 한다. 이는 SelfSigned 발급자가 발행할 cert-manager `Certificate` 객체에 `spec.subject` 를 설정하여 수행할 수 있다.
- 버전 1.3 부터 ​​cert-manager 는 Issuer DN 이 비어 있는 SelfSigned Issuer 가 인증서를 생성하고 있음을 감지하면 `BadConfig` 유형의 Kubernetes 경고 이벤트를 내보낸다.

# 3. 생성한 인증서를 파일로 decode 하는 방법

``` bash
k get secret ainswer-wildcard-tls -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
k get secret ainswer-wildcard-tls -o jsonpath='{.data.tls\.crt}' | base64 -d > tls.crt
k get secret ainswer-wildcard-tls -o jsonpath='{.data.tls\.key}' | base64 -d > tls.key
```