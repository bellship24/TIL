**목차**

- [1. 요약](#1-요약)
- [2. 전제 조건](#2-전제-조건)
- [3. helm 으로 cert-manager 설치](#3-helm-으로-cert-manager-설치)
  - [3.1. helm repo 추가](#31-helm-repo-추가)
  - [helm install 실행](#helm-install-실행)
- [기타](#기타)
  - [아웃풋 yaml 출력하기](#아웃풋-yaml-출력하기)
  - [삭제](#삭제)
    - [[Troubleshoot] 삭제 시에 Terminating 상태에 빠지는 경우](#troubleshoot-삭제-시에-terminating-상태에-빠지는-경우)

**참고**

- [[cert-manager docs] Installing with Helm]([https://](https://cert-manager.io/docs/installation/helm/))

---

# 1. 요약

# 2. 전제 조건

- Helm v3+
- Kubernetes 클러스터

# 3. helm 으로 cert-manager 설치

## 3.1. helm repo 추가

jetstack helm repo 추가 및 업데이트

``` bash
helm repo add jetstack https://charts.jetstack.io
helm repo update
helm repo ls
```

- cert-manager 를 공식적으로 지원해주는 차트이다.

## helm install 실행

- cert-manager 는 많은 CRD 리소스를 필요로 한다.
- 이러한 CRD 리소스를 설치하는 방법은 크게 두 가지가 있다.
- 첫 째, `kubectl` 을 사용해 매뉴얼하게 설치하는 방법.
- 둘 째, helm 차트 설치 시에 `installCRDs` 옵션 사용하는 방법.

설치 실행

``` bash
helm upgrade --install \
  cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --version v1.5.3 \
  --set installCRDs=true
  # --set prometheus.enabled=false \  # Example: disabling prometheus using a Helm parameter
  # --set webhook.timeoutSeconds=4s   # Example: changing the wehbook timeout using a Helm parameter
```

# 기타

## 아웃풋 yaml 출력하기

- 실제 바로 설치하지 않고 설치할 때 실제로 사용하는 yaml 을 파일로 저장하려면 아래와 같이 수행하자.

``` bash
$ helm template \
  cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.5.3 \
  # --set prometheus.enabled=false \   # Example: disabling prometheus using a Helm parameter
  # --set installCRDs=true \           # Uncomment to also template CRDs
  > cert-manager.custom.yaml
```

## 삭제

``` bash
### 리소스 삭제
kubectl get Issuers,ClusterIssuers,Certificates,CertificateRequests,Orders,Challenges --all-namespaces

### helm release 삭제
helm --namespace cert-manager delete cert-manager

### namespace 삭제
kubectl delete ns cert-manager
```

### [Troubleshoot] 삭제 시에 Terminating 상태에 빠지는 경우

- 만약, 삭제 시에 cert-manager 리소스와 릴리즈를 삭제하지 않고 네임스페이스만 먼저 삭제했다면 네임스페이스가 Terminating 상태에 빠지게 될 수 있다.
- 이는 APIService 가 계속 남아있지만 webhook 서버가 더이상 돌지않아 접근이 불가능하기 때문이다.
- 이 문제를 해결하기 위해서는 아래 명령어로 apiservice 를 지워주자.

``` bash
kubectl delete apiservice v1beta1.webhook.cert-manager.io
```