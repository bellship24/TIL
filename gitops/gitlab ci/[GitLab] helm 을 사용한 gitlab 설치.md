**요약**

- on-prem 환경에서 전제 사항을 고려하여 gitlab 을 helm 으로 구축해보자.

**목차**

- [1. 사전 작업](#1-사전-작업)
  - [1.1. 기본 전제](#11-기본-전제)
  - [1.2. kubectl 설치](#12-kubectl-설치)
  - [1.3. helm 설치](#13-helm-설치)
- [2. configuration 옵션들](#2-configuration-옵션들)
- [3. helm 으로 gitlab 배포](#3-helm-으로-gitlab-배포)
  - [3.1. gitlab helm repo 추가](#31-gitlab-helm-repo-추가)
  - [3.2. self-signed 인증서 생성](#32-self-signed-인증서-생성)
  - [3.3. override-values.yaml 작성](#33-override-valuesyaml-작성)
  - [3.4. helm 으로 gitlab 설치](#34-helm-으로-gitlab-설치)
  - [3.5. 설치 확인](#35-설치-확인)
  - [3.6. 초기 접속 확인](#36-초기-접속-확인)
  - [3.7. 초기 로그인 방법](#37-초기-로그인-방법)
- [4. helm 으로 gitlab 업그레이드 방법](#4-helm-으로-gitlab-업그레이드-방법)
- [5. 기타](#5-기타)
  - [5.1. 미니오 계정 정보 조회 방법](#51-미니오-계정-정보-조회-방법)
- [6. 트러블슈팅](#6-트러블슈팅)
  - [6.1. gitlab runner 가 gitlab 도메인네임을 resolve 하지 못하는 에러](#61-gitlab-runner-가-gitlab-도메인네임을-resolve-하지-못하는-에러)
  - [6.2. gitlab runner 의 `x509: certificate signed by unknown authority` 이슈](#62-gitlab-runner-의-x509-certificate-signed-by-unknown-authority-이슈)
    - [6.2.1. 현상 : gitlab-runner 파드 생성 불가](#621-현상--gitlab-runner-파드-생성-불가)
    - [6.2.2. 분석 : gitlab helm chart 의 self-signed cert 생성 버그](#622-분석--gitlab-helm-chart-의-self-signed-cert-생성-버그)
    - [6.2.3. 해결 1 (ㅁ) : `auto-generated self-signed wildcard certificate` 를 사용하고 `gitlab-runner` 를 disable 하기](#623-해결-1-ㅁ--auto-generated-self-signed-wildcard-certificate-를-사용하고-gitlab-runner-를-disable-하기)
    - [6.2.4. 해결 2 (o) : 자체적으로 `cert-manager` 를 통해 `self-signed wildcard certificate` 를 생성하고 `gitlab-runner` 를 disable 하기](#624-해결-2-o--자체적으로-cert-manager-를-통해-self-signed-wildcard-certificate-를-생성하고-gitlab-runner-를-disable-하기)
    - [6.2.5. 해결 3 (△) : cert-manager 와 Let’s Encrypt 사용하기](#625-해결-3---cert-manager-와-lets-encrypt-사용하기)
  - [6.3. linux 에서 git clone 불가 이슈](#63-linux-에서-git-clone-불가-이슈)
    - [6.3.1. 현상 : gitlab 서버에 https 접근 불가](#631-현상--gitlab-서버에-https-접근-불가)
    - [6.3.2. 분석 : 자체서명 인증서에 대한 ca.crt 누락](#632-분석--자체서명-인증서에-대한-cacrt-누락)
    - [6.3.3. 해결 1 [o] : ca.crt 배포](#633-해결-1-o--cacrt-배포)
  - [6.4. macOS 에서 git clone 불가 이슈](#64-macos-에서-git-clone-불가-이슈)
    - [6.4.1. 현상 : gitlab repo. git clone 시에 불가](#641-현상--gitlab-repo-git-clone-시에-불가)
    - [6.4.2. 분석 : 자체서명 인증서에 대한 ca.cert 누락](#642-분석--자체서명-인증서에-대한-cacert-누락)
    - [6.4.3. 해결 1 [ㅁ]: gitlab 의 cert 를 검증하지 않기](#643-해결-1-ㅁ-gitlab-의-cert-를-검증하지-않기)
    - [6.4.4. 해결 2 [o] : ca.crt 배포](#644-해결-2-o--cacrt-배포)

**참조**

- [[GitLab Docs] Installing GitLab using Helm](https://docs.gitlab.com/charts/installation/)
- [[GitLab Docs] Deployment Guide](https://docs.gitlab.com/charts/installation/deployment.html)
- [[GitLab Docs] gitlab helm install 의 --set 옵션들](https://docs.gitlab.com/charts/installation/command-line-options.html)
- [[GitLab Docs] gitlab 관련 인증서](https://docs.gitlab.com/charts/installation/tls.html)
- [[blog] 온프레미스에 gitlab 을 공식 helm chart 로 설치하기](https://blog.naver.com/PostView.nhn?blogId=kgg1959&logNo=222343163014&parentCategoryNo=&categoryNo=220&viewDate=&isShowPopularPosts=false&from=postView)
- [[GitLab Docs] kubernetes 기반 gitlab 설치 트러블슈팅](https://docs.gitlab.com/charts/troubleshooting/)

# 1. 사전 작업

## 1.1. 기본 전제

- k8s 클러스터 v1.13+
- k8s 클러스터 스팩 8vCPU, 30GB RAM 이상 권고
- kubectl 1.13+
- helm v3.2.0+
- storageClass
- on-prem 의 경우, metalLB 같은 로드밸런서
- 로드밸런서 용 nginx-ingress controller
- cert-manager 설치 및 self-signed 인증서 생성
- gitlab v13.12.4, helm gitlab chart v4.12.4 로 최신버전 사용

## 1.2. kubectl 설치

``` bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg

echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update

### 최신 버전 설치
sudo apt install -y  kubectl

### 특정 버전 설치
sudo apt install -y kubectl=1.20.7-00

### 버전 hold
sudo apt-mark hold kubectl

### 이후에 kubeconfig 를 알맞게 설정할 것
```

## 1.3. helm 설치

``` bash
$ curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash

% Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 11213  100 11213    0     0  18381      0 --:--:-- --:--:-- --:--:-- 18381
Downloading https://get.helm.sh/helm-v3.4.2-linux-amd64.tar.gz
Verifying checksum... Done.
Preparing to install helm into /usr/local/bin
helm installed into /usr/local/bin/helm

$ helm version
version.BuildInfo{Version:"v3.6.0", GitCommit:"7f2df6467771a75f5646b7f12afb408590ed1755", GitTreeState:"clean", GoVersion:"go1.16.3"}
```

# 2. configuration 옵션들

- helm install 로 gitlab 을 배포하기 전에 몇 가지 옵션에 대한 의사결정이 필요하다.
- 옵션은 helm 의 `--set option.name=value` 형태를 사용해 명시할 수 있다. 이 옵션의 목록은 gitlab Docs 의 [Installation command line options](https://docs.gitlab.com/charts/installation/command-line-options.html)를 참고하자.
- 아래 사항들은 helm install 을 사용할 때 옵션으로 사용된다. 자세한 내용은 [여기](https://docs.gitlab.com/charts/installation/deployment.html#selecting-configuration-options)를 참고하자.

Networking 과 DNS

- 기본적으로 여기서 사용하는 chart 는 GitLab 서비스를 name-based expose 하기 위해 k8s svc 타입 중에 LoadBalancer 타입을 Ingress 오브젝트와 함께 사용한다.
- 이 chart 에서 gitlab, registry 및 minio(MinIO를 사용하는 경우) 를 적절한 IP 로 resolve 하려면 [record](https://goldsony.tistory.com/148) 를 포함할 domain 을 지정해야한다.
- 이에 관해, 이 chart 에서 gitlab 의 domain 네임을 설정하는 옵션이 아래와 같다.

``` bash
--set global.hosts.domain=example.com
```

Dynamic IPs with external-dns 혹은 Static IP

- DNS record 를 수동으로 구성하려는 경우, 그 record 들은 모두 static IP 를 가리켜야 한다. 예를 들어, example.com 을 레코드(혹은 앞서 본 --set global.hosts.domain=example.com 설정)로 사용하고 고정 IP가 10.10.10.10 인 경우, gitlab.example.com, registry.example.com 및 minio.example.com(MinIO를 사용하는 경우) 은 모두 10.10.10.10으로 resolve 되어야 한다. 이에 대한 helm install 옵션은 아래와 같다.

``` bash
--set global.hosts.externalIP=10.10.10.10
```

Persistence Volume

- 기본적으로 이 chart 는 dynamic provisioner 가 persistent volume 을 생성 할 것이라는 예상과 함께 volume claim 을 생성한다. storageClass 를 사용자 정의하거나 수동으로 볼륨을 생성 및 할당하려면 반드시 [gitlab docs 의 Storage Guide](https://docs.gitlab.com/charts/installation/storage.html)를 참고하자.
- 주의 : 초기 설치 후 storage 설정을 변경하려면 k8s 의 여러 object 들을 수동으로 편집해야 하므로 추가적인 스토리지 마이그레이션 작업을 방지하려면 GitLab 의 프로덕션 인스턴스를 설치하기 전에 미리 계획하고 진행하는 것이 좋다.

TLS certificates

- TLS 인증서가 필요한 https 를 사용하여 GitLab 을 실행해야 한다. 기본적으로 차트는 TLS 인증서를 얻기 위해 cert-manager 를 설치하고 구성한다. 자체 와일드 카드 인증서가 있거나 이미 cert-manager 가 설치되어 있거나 TLS 인증서를 얻는 다른 방법이 있는 경우, [여기](https://docs.gitlab.com/charts/installation/tls.html)를 참고해서 추가적인 TLS 설정을 해주면 된다.
- 기본적으로 인증서 등록을 위해 이메일 정보를 넣어줘야 한다.

``` bash
--set certmanager-issuer.email=me@example.com
```

- 만약, self-signed 인증서를 사용한다면, 이 게시글의 뒤에서 다루겠지만, gitlab helm chart 에서 제공하는 cert-manager 실행 시에 대체로 버그가 발생할 수 있으므로 자체 cert-manager 를 사용하는 것이 좋다.

PostgreSQL

- 기본적으로 이 차트는 테스트용으로만 in-cluster PostgreSQL 데이터베이스를 제공한다.
- 운영 환경에 적합한 데이터베이스를 설정하는 방법은 여기 [[gitlab docs] External PostgreSQL database](https://docs.gitlab.com/charts/advanced/external-db/index.html)를 참고하자.
- 예를 들어, 외부 postgre 서버가 있다면, 아래와 같이 gitlab 을 helm install 시에 in-cluster postgresql 을 설치하지 않게 하고 외부 postgre 서버에 접근할 수 있도록 인자들을 추가하면 된다.

``` bash
--set postgresql.install=false
--set global.psql.host=production.postgress.hostname.local
--set global.psql.password.secret=kubernetes_secret_name
--set global.psql.password.key=key_that_contains_postgres_password
```

Redis

- redis 도 postgre 서버 처럼 운영 시에 외부 redis 서버를 연동할 수 있다. 자세한 내용은 여기 [[gitlab docs] Configure Redis settings](https://docs.gitlab.com/charts/charts/globals.html#configure-redis-settings) 를 참고하자.
- 그 외에 설정은 여기 [[gitlab docs] Configure Redis settings](https://docs.gitlab.com/charts/charts/globals.html#configure-redis-settings) 를 참고하자.

MinIO

- 기본적으로, 이 공식 gitlab helm chart 는 object storage API 를 제공하는 in-cluster MinIO deployment 도 제공한다.
- 이 object storage 는 gitlab 이 kubernetes 에서 high-available 한 persistent data 를 저장하기 위해 사용한다.
- 하지만, 운영환경에서 minio 를 사용하는 것을 권고하지는 않는다. 대신에 hosted object storage 로써 Google Cloud Storage 나 AWS S3 를 권고한다.
- 이런 설정을 하기 위해서는 여기 [[gitlab docs] External object storage](https://docs.gitlab.com/charts/advanced/external-object-storage/index.html) 를 참고하자.

Prometheus

Outgoing email

Incoming email

RBAC

CPU and RAM Resource Requirements

- 이 helm chart 의 GitLab 구성 요소 (PostgreSQL, Redis 또는 MinIO 제외) 에 대한 리소스 request 및 replica 수는 기본적으로 소규모 프로덕션 배포에 적합하도록 설정되어 있다. 이는 최소 `8vCPU` 및 `30GB RAM` 이 있는 클러스터에 적합하도록 고안됐다. 운영 환경이 아닌 곳에 배포하려는 경우, 더 작은 클러스터에 맞도록 기본값을 줄일 수 있다.

# 3. helm 으로 gitlab 배포

모든 configuration option 을 결정했다면, 의존성을 지정하고 아래와 같이 Helm 을 실행하자.

## 3.1. gitlab helm repo 추가

``` bash
helm repo add gitlab https://charts.gitlab.io/
helm repo update  
```

gitlab helm chart 를 fetch

``` bash
helm fetch gitlab/gitlab --untar
cd gitlab
```

## 3.2. self-signed 인증서 생성

- 공인 IP 와 도메인네임이 없을 때는 self-signed CA 를 쓰면 된다.
- 앞서 cert-manager 설치를 전제하고 있다. 이미 설치된 cert-manager 를 사용하여 self-signed 인증서를 만들자.

`gitlab-self-signed.yaml`

``` yaml
cat <<EOF > gitlab-self-signed.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: cicd
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
  name: gitlab-wildcard-tls
  namespace: cicd
spec:
  isCA: true
  commonName: '*.mylab.com'
  secretName: lab-gitlab
  issuerRef:
    name: selfsigned-issuer
    kind: ClusterIssuer
    group: cert-manager.io
EOF
```

- 작성한 매니페스트로 리소스를 만들자. commonName, secretName 을 잘 정해주자.
- namespace 등을 잘 성절해서 사용하자

``` bash
$ k apply -f gitlab-self-signed.yaml
namespace/cicd created
clusterissuer.cert-manager.io/selfsigned-issuer created
certificate.cert-manager.io/gitlab-wildcard-tls created
```

## 3.3. override-values.yaml 작성

`override-values.yaml`

``` yaml
global:
  edition: ce
  hosts:
    domain: lab.com
    https: true
    externalIP: 10.0.0.216
  ingress:
    congifureCertmanager: false
    class: "nginx"
    enabled: true
    tls:
      secretName: lab-gitlab
      enabled: true
certmanager:
  install: false
certmanager-issuer:
  email: myname@gmail.com
nginx-ingress:
  enabled: false
gitlab-runner:
  install: false
```

- `global.edition=ce` : ce 버전을 사용한다.
- `global.hosts.domain` : gitlab, minio, registry 의 도메인을 정한다.
- `global.hosts.externalIP` : nginx-ingress 를 통해 접근할 externalIP 이다.
- `global.ingress.configureCertmanager` : false 로 설정해서 certmanager 를 통한 ingress 의 인증서 생성을 하지 않게 한다.
- `global.ingress.class` : nginx 로 설정해서 기존에 설치한 nginx ingress 를 사용하게 한다.
- `global.ingress.enabled` : ingress 를 활성화한다.
- `global.ingress.tls.secretName` : ingress 에서 사용할 인증서의 secret 이름이다. 이를 지정해줘야 자체 구축한 인증서를 사용할 수 있다.
- `global.ingress.tls.enable` : ingress 에 tls 를 활성화한다.
- `certmanager.install` : false 로 설정해서 certmanager 를 설치하지 않게 한다.
- `certmanager-issuer.email` : 인증서 발급 이메일.
- `nginx-ingress.enabled` : nginx-ingress 를 자체 구축한 것으로 쓸 것이기 때문에 같이 설치하지 않게 설정한다.
- `gitlab-runner.install` : false 로 설정해서 gitlab-runner 를 설치하지 않는다. 자체 서명을 사용하는 경우, 이 helm 에서 gitlab-runner 를 설치하는 것을 권고하지 않는다고 한다.

## 3.4. helm 으로 gitlab 설치

``` bash
helm upgrade --install gitlab gitlab/gitlab \
  -n cicd --create-namespace \
  --timeout 600s \
  --version 4.12.4 \
  -f override-values.yaml
```

- 설치가 완료되기까지 약 15분 까지 걸릴 수 있다.

## 3.5. 설치 확인

- 설치가 완료되면 아래와 같은 상태이다.
- 10.0.0.216 은 자체적으로 설치했던 lb-nginx-ingress 의 ExternalIP 이다.

``` bash
$ k get all -n cicd
NAME                                              READY   STATUS      RESTARTS   AGE
pod/gitlab-gitaly-0                               1/1     Running     0          73m
pod/gitlab-gitlab-exporter-6f645c49c-jb4r9        1/1     Running     0          73m
pod/gitlab-gitlab-shell-58cd975b57-drlxq          1/1     Running     0          73m
pod/gitlab-gitlab-shell-58cd975b57-pnqjb          1/1     Running     0          73m
pod/gitlab-migrations-2-7vwr5                     0/1     Completed   0          45m
pod/gitlab-minio-5557bb8cfd-vbp7r                 1/1     Running     0          73m
pod/gitlab-minio-create-buckets-2-hgrmn           0/1     Completed   0          45m
pod/gitlab-postgresql-0                           2/2     Running     0          73m
pod/gitlab-prometheus-server-6444c7bd76-gs7k6     2/2     Running     0          73m
pod/gitlab-redis-master-0                         2/2     Running     0          73m
pod/gitlab-registry-6b454b5668-2w24z              1/1     Running     0          73m
pod/gitlab-registry-6b454b5668-hlcc9              1/1     Running     0          73m
pod/gitlab-sidekiq-all-in-1-v1-85fffc9595-rrl8z   1/1     Running     0          73m
pod/gitlab-task-runner-556dc767c7-dccsl           1/1     Running     0          73m
pod/gitlab-webservice-default-bb66f6b6d-bdztv     2/2     Running     0          73m
pod/gitlab-webservice-default-bb66f6b6d-phjtk     2/2     Running     0          73m

NAME                                 TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)             AGE
service/gitlab-gitaly                ClusterIP   None             <none>        8075/TCP            73m
service/gitlab-gitlab-exporter       ClusterIP   10.107.205.187   <none>        9168/TCP            73m
service/gitlab-gitlab-shell          ClusterIP   10.109.176.181   <none>        22/TCP              73m
service/gitlab-minio-svc             ClusterIP   10.105.5.86      <none>        9000/TCP            73m
service/gitlab-postgresql            ClusterIP   10.100.173.37    <none>        5432/TCP            73m
service/gitlab-postgresql-headless   ClusterIP   None             <none>        5432/TCP            73m
service/gitlab-postgresql-metrics    ClusterIP   10.111.196.4     <none>        9187/TCP            73m
service/gitlab-prometheus-server     ClusterIP   10.111.209.226   <none>        80/TCP              73m
service/gitlab-redis-headless        ClusterIP   None             <none>        6379/TCP            73m
service/gitlab-redis-master          ClusterIP   10.98.192.27     <none>        6379/TCP            73m
service/gitlab-redis-metrics         ClusterIP   10.100.8.78      <none>        9121/TCP            73m
service/gitlab-registry              ClusterIP   10.108.238.232   <none>        5000/TCP            73m
service/gitlab-webservice-default    ClusterIP   10.96.79.150     <none>        8080/TCP,8181/TCP   73m

NAME                                         READY   UP-TO-DATE   AVlabLE   AGE
deployment.apps/gitlab-gitlab-exporter       1/1     1            1           73m
deployment.apps/gitlab-gitlab-shell          2/2     2            2           73m
deployment.apps/gitlab-minio                 1/1     1            1           73m
deployment.apps/gitlab-prometheus-server     1/1     1            1           73m
deployment.apps/gitlab-registry              2/2     2            2           73m
deployment.apps/gitlab-sidekiq-all-in-1-v1   1/1     1            1           73m
deployment.apps/gitlab-task-runner           1/1     1            1           73m
deployment.apps/gitlab-webservice-default    2/2     2            2           73m

NAME                                                    DESIRED   CURRENT   READY   AGE
replicaset.apps/gitlab-gitlab-exporter-6f645c49c        1         1         1       73m
replicaset.apps/gitlab-gitlab-shell-58cd975b57          2         2         2       73m
replicaset.apps/gitlab-minio-5557bb8cfd                 1         1         1       73m
replicaset.apps/gitlab-prometheus-server-6444c7bd76     1         1         1       73m
replicaset.apps/gitlab-registry-6b454b5668              2         2         2       73m
replicaset.apps/gitlab-sidekiq-all-in-1-v1-85fffc9595   1         1         1       73m
replicaset.apps/gitlab-task-runner-556dc767c7           1         1         1       73m
replicaset.apps/gitlab-webservice-default-bb66f6b6d     2         2         2       73m

NAME                                   READY   AGE
statefulset.apps/gitlab-gitaly         1/1     73m
statefulset.apps/gitlab-postgresql     1/1     73m
statefulset.apps/gitlab-redis-master   1/1     73m

NAME                                                             REFERENCE                               TARGETS          MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/gitlab-gitlab-shell          Deployment/gitlab-gitlab-shell          <unknown>/100m   2         10        2          73m
horizontalpodautoscaler.autoscaling/gitlab-registry              Deployment/gitlab-registry              <unknown>/75%    2         10        2          73m
horizontalpodautoscaler.autoscaling/gitlab-sidekiq-all-in-1-v1   Deployment/gitlab-sidekiq-all-in-1-v1   <unknown>/350m   1         10        1          73m
horizontalpodautoscaler.autoscaling/gitlab-webservice-default    Deployment/gitlab-webservice-default    <unknown>/1      2         10        2          73m

NAME                                      COMPLETIONS   DURATION   AGE
job.batch/gitlab-issuer-2                 0/1           45m        45m
job.batch/gitlab-migrations-2             1/1           65s        45m
job.batch/gitlab-minio-create-buckets-2   1/1           2s         45m

$ k get ingress -n cicd
NAME                        CLASS    HOSTS                ADDRESS          PORTS     AGE
gitlab-minio                <none>   minio.lab.com      10.0.0.216   80, 443   74m
gitlab-registry             <none>   registry.lab.com   10.0.0.216   80, 443   74m
gitlab-webservice-default   <none>   gitlab.lab.com     10.0.0.216   80, 443   74m
```

## 3.6. 초기 접속 확인

gitlab 접근

![](/.uploads/2021-07-20-19-43-02.png)

minio 접근

![](/.uploads/2021-06-15-01-31-05.png)

registry 접근

![](/.uploads/2021-06-15-02-31-53.png)

- registry ingress에 접속하면 화면에는 아무것도 보이지 않지만, 네트워크 디버거를 사용하면 200OK응답을 받은 것을 확인할 수 있다. registry ingress 는 gitlab CI/CD 를 사용하기 위해 ingress 를 만든 것으로 생각된다.  

## 3.7. 초기 로그인 방법

- 설치 중에 지정된 도메인을 방문하여 GitLab 인스턴스에 액세스 할 수 있다. [전역 호스트 설정](https://docs.gitlab.com/charts/charts/globals.html#configure-host-settings)이 변경되지 않는 한 기본 도메인은 `gitlab.example.com` 이다. 초기에 루트 비밀번호에 대한 secret 을 수동으로 생성 한 경우, 이를 사용하여 `root` 사용자로 로그인 할 수 있다. 그렇지 않다면, GitLab 은 `root` 사용자를 대해 임의의 비밀번호를 자동으로 생성했을 것이다. 이는 다음 명령으로 추출 할 수 있다.
- 아래 명령을 사용한 경우, 네임스페이스와  `<name>` 을 릴리스 이름으로 대체해라.

``` bash
kubectl get secret <name>-gitlab-initial-root-password -ojsonpath='{.data.password}' | base64 --decode ; echo

### e.g.
$ kubectl get secret gitlab-gitlab-initial-root-password -ojsonpath='{.data.password}' -n cicd | base64 --decode ; echo
euFMde4s7ebztOtGEHPrfF7aEkdJW3sUKZv9P7YPicCMhWJfFCIJp6f9SRXhop22
```

- 로그인 후에 우측 상단 계정 아이콘 클릭 / Edit profile 클릭 / 좌측 메뉴바에서 Password 클릭 / 패스워드 변경 후 저장

# 4. helm 으로 gitlab 업그레이드 방법

- 업그레이드 방법은 [여기](https://docs.gitlab.com/charts/installation/upgrade.html)를 참고하자.

# 5. 기타

## 5.1. 미니오 계정 정보 조회 방법

``` bash
### access_key
$ kubectl get secret -n [네임스페이스] [secret 리소스 이름] -ojsonpath='{.data.accesskey}' | base64 -d ; echo 

### secret_key
$ kubectl get secret -n [네임스페이스] [secret 리소스 이름] -ojsonpath='{.data.secretkey}' | base64 -d ; echo
```

# 6. 트러블슈팅

## 6.1. gitlab runner 가 gitlab 도메인네임을 resolve 하지 못하는 에러

현상

- gitlab runner 파드의 CrashLoopBackOff 현상

분석

- 해당 파드의 로그를 보면 도메인네임을 해석하지 못함.

``` bash
$ k logs gitlab-gitlab-runner-59bff68f95-mq8q4

PANIC: Failed to register the runner. You may be having network problems.
Registration attempt 10 of 30
Runtime platform                                    arch=amd64 os=linux pid=116 revision=7a6612da version=13.12.0
WARNING: Running in user-mode.
WARNING: The user-mode requires you to manually start builds processing:
WARNING: $ gitlab-runner run
WARNING: Use sudo for system-mode:
WARNING: $ sudo gitlab-runner...

ERROR: Registering runner... failed                 runner=cudaElSZ status=couldn't execute POST against https://gitlab.my.gitlab.com/api/v4/runners: Post https://gitla
b.my.gitlab.com/api/v4/runners: dial tcp: lookup gitlab.my.gitlab.com on 10.96.0.10:53: no such host
PANIC: Failed to register the runner. You may be having network problems.
```

해결

- 임시방편으로 hostAliases 를 통해 생성될 파드들의 /etc/hosts 를 업데이트

``` bash
$ k edit gitlab-gitlab-runner
```

``` yaml
    ...
    spec:
      hostAliases:
      - ip: "10.0.0.216"
        hostnames:
        - "gitlab.my.gitlab.com"
        - "minio.my.gitlab.com"
        - "registry.my.gitlab.com"
      containers:
    ...
```

## 6.2. gitlab runner 의 `x509: certificate signed by unknown authority` 이슈

### 6.2.1. 현상 : gitlab-runner 파드 생성 불가

- gitlab helm 을 통해 gitlab 을 기본적으로 구축하면 gitlab runner 파드의 crashloopback 현상이 발생한다.

### 6.2.2. 분석 : gitlab helm chart 의 self-signed cert 생성 버그

- 로그 조회 시에 `x509: certificate signed by unknown authority` 이슈 발생. 즉, 인증서 설정에 오류가 있는 것으로 보인다.

  ``` bash
  $ k logs gitlab-gitlab-runner-945cd8dcd-mvnjf

  Registration attempt 3 of 30
  Runtime platform                                    arch=amd64 os=linux pid=32 revision=7a6612da version=13.12.0
  WARNING: Running in user-mode.
  WARNING: The user-mode requires you to manually start builds processing:
  WARNING: $ gitlab-runner run
  WARNING: Use sudo for system-mode:
  WARNING: $ sudo gitlab-runner...

  ERROR: Registering runner... failed                 runner=cudaElSZ status=couldn't execute POST against https://gitlab.my.gitlab.com/api/v4/runners: Post https://gitlab.my.gitlab.com/api/v4/runners: x509: certificate signed by unknown authority
  PANIC: Failed to register the runner. You may be having network problems.
  ```

### 6.2.3. 해결 1 (ㅁ) : `auto-generated self-signed wildcard certificate` 를 사용하고 `gitlab-runner` 를 disable 하기

![](/.uploads/2021-06-16-22-18-56.png)

- 여기 [문서](https://docs.gitlab.com/charts/installation/tls.html)를 참고했다.
- 이 문서에 따르면, 공식 gitlab helm chart 가 auto-generated self-signed wildcard certificate 를 제공할 수 있다고 한다. 이 기능은 보안상 SSL 기능을 써야되지만 Let's Encrypt 는 사용할 수 없는 환경에서 유용하다.
- 이 기능은 `shared-secrets` job 으로 수행된다. 이 job 은 외부에서 액세스 할 수 있는 모든 서비스에서 사용할 `CA 인증서`, `와일드카드 인증서` 및 `인증서 체인`을 생성한다. 즉, `RELEASE-wildcard-tls`, `RELEASE-wildcard-tls-ca` 및 `RELEASE-wildcard-tls-chain` 을 생선한다. `RELEASE-wildcard-tls-ca` 에는 배포 된 gitlab 인스턴스에 액세스할 사용자 및 시스템에 배포 할 수 있는 public CA 인증서가 포함되어 있다. `RELEASE-wildcard-tls-chain` 에는 `gitlab-runner.certsSecretName = RELEASE-wildcard-tls-chain` 을 통해 GitLab Runner 에 직접 사용할 수 있는 CA 인증서와 와일드 카드 인증서가 모두 포함되어 있다.
- **그러나, 이런 self-signed certificates 를 썼을 때 gitlab-runner chart 는 제대로 동작하지 않을 수 있다.** 그러므로 이럴 때는 아래와 같이 gitlab-runner 를 disable 하기를 권고한다.

``` bash
helm install gitlab gitlab/gitlab \
--set certmanager.install=false \
--set global.ingress.configureCertmanager=false \
--set gitlab-runner.install=false
```

- 그러나, 아래에 해결 2 를 보면 알 수 있듯이, gitlab helm chart 로 self-signed 를 자동 생성할 때, 간혹 인증서에 대한 secret 이 생성되지 않는다고 한다. 이를 버그로 보고 있다고 하니, 되도록이면 cert-manager 를 자체적으로 사용하여 자체 인증서를 만들어 적용해도 좋을 것 같다.

### 6.2.4. 해결 2 (o) : 자체적으로 `cert-manager` 를 통해 `self-signed wildcard certificate` 를 생성하고 `gitlab-runner` 를 disable 하기

- 더불어, [어느 블로그](http://blog.naver.com/PostView.nhn?blogId=kgg1959&logNo=222343163014&parentCategoryNo=150&categoryNo=&viewDate=&isShowPopularPosts=true&from=search)를 봤을 때, 이 helm chart 를 통해 gitlab 을 설치할 때, self-signed 를 사용하면 메시지에는 정상 생성이 출력되지만 실제로 인증서에 대한 secret 이 생성 될 때가 있고 안 될 때가 있다고 한다. 그러면서 이 블로거는 서칭해봤는데 gitlab helm 의 내부적인 오류로 판단한다고 했다. 만약, 이렇게 self-signed 인증서가 생성되지 않으면, 내부적으로 nginx-ingress 의 인증서를 사용하게 된다. 이렇게 되면, gitlab 을 통한 git clone 을 할 때, 인증서 등록을 하지 못하고 같이 배포되는 gitlab-runner 도 인증서 문제로 등록이 안 된다.
- 즉, 여러모로 gitlab helm 사용 시에, auto-generated self-signed wildcard certificates 는 사용하지 않는 것이 좋을 것 같다.
- 만약, 자체적인 cert-manager 를 구축하여 issuer 와 certificate 를 만들어 사용한다면, 해당 secretName 을 `global.ingress.tls.secretName` 에 넣어 helm install/upgrade 하자.
- 이 방법은 사실 본문의 진행 방식이다.

### 6.2.5. 해결 3 (△) : cert-manager 와 Let’s Encrypt 사용하기

- Let ’s Encrypt 는 무료이며 자동화 된 개방형 CA(인증 기관, Certificate Authority) 이다. 다양한 도구를 사용하여 인증서를 자동으로 요청할 수 있다. 그리고 이 gitlab 차트는 `cert-manager` 와 통합 사용할 수 있다.
- 이미 cert-manager 를 사용하고 있는 경우, `global.ingress.annotations` 을 사용하여 cert-manager 배포에 적합한 주석을 구성 할 수 있다. 클러스터에 아직 cert-manager 가 설치되어 있지 않은 경우에도 이 gitlab 차트의 종속성으로 설치 및 구성 할 수 있다.
- internal cert-manager 와 issuer 를 쓰는 경우, cert-manager 설치는 `certmanager.install` 설정으로 할 수 있고 설치한 internal cert-manager 를 사용하기 위해서는 `global.ingress.configureCertmanager` 설정을 사용하면 된다. 그런데, 이 두 가지 모두 기본적으로 true 이므로 기본적으로 발급자 이메일 만 제공하면된다.

  ``` bash
  helm repo update
  helm dep update
  helm install gitlab gitlab/gitlab \
    --set certmanager-issuer.email=you@example.com
  ```

- 하지만, Let's Encrypt 를 사용하는 경우, 이에 대한 인증 방법을 위해 dns01 이나 http01 방법을 사용해야한다. 즉, 폐쇄망에서는 사용이 어려우므로 이 방법은 해결 가능한 방법이지만 제외한다.

## 6.3. linux 에서 git clone 불가 이슈

### 6.3.1. 현상 : gitlab 서버에 https 접근 불가

- linux 환경에서 helm 으로 구축한 gitlab 의 어느 repo. 를 clone 하면 오류가 발생하며 실패한다.
- git clone 시 오류 log

``` bash
$ git clone https://gitlab.mylab.com/root/mycicd.git
Cloning into 'mycicd'...
fatal: unable to access 'https://gitlab.mylab.com/root/mycicd.git/': server certificate verification failed. CAfile: /etc/ssl/certs/ca-certificates.crt CRLfile: none
```

### 6.3.2. 분석 : 자체서명 인증서에 대한 ca.crt 누락

- clone 을 시도한 linux 환경은 git client 이다. https 로 gitlab 을 구축하였으므로 이 client 는 git clone 을 할 때 제일 먼저 gitlab 서버에서 인증서를 줄 것이다. 그리고 client 는 이 인증서를 ca.crt 로 해석하려 할 것이다. 그러나, gitlab 이 self-signed 인증서를 사용했다면, 이 서버의 인증서에 대한 ca.crt 가 client 의 CA chain 에 없을 것이다.

### 6.3.3. 해결 1 [o] : ca.crt 배포

- 그러므로 에러 로그에 나온대로 client 의 알맞은 경로에다가 ca.crt 를 넣어주면 해결된다.

``` bash
$ sudo bash -c " cat <<EOF >> /etc/ssl/certs/ca-certificates.crt
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

## 6.4. macOS 에서 git clone 불가 이슈

### 6.4.1. 현상 : gitlab repo. git clone 시에 불가

- macOS 환경에서 helm 으로 구축한 gitlab 의 어느 repo. 를 clone 하면 오류가 발생하며 실패한다.
- git clone 시 오류 log

``` bash
git clone https://gitlab.mylab.com/root/mycicd.git
'mycicd'에 복제합니다...
fatal: unable to access 'https://gitlab.mylab.com/root/mycicd.git/': SSL certificate problem: self signed certificate
```

### 6.4.2. 분석 : 자체서명 인증서에 대한 ca.cert 누락

- clone 을 시도한 macOS 환경은 git client 이다. https 로 gitlab 을 구축하였으므로 이 client 는 git clone 을 할 때 제일 먼저 gitlab 서버에서 인증서를 줄 것이다. 그리고 client 는 이 인증서를 ca.crt 로 해석하려 할 것이다. 그러나, gitlab 이 self-signed 인증서를 사용했다면, 이 서버의 인증서에 대한 ca.crt 가 client 의 CA chain 에 없을 것이다.

### 6.4.3. 해결 1 [ㅁ]: gitlab 의 cert 를 검증하지 않기

- 앞서 분석에서 말했듯이 gitlab cert 에 대한 CA cert 가 없어 증명이 안된 것이기 때문에, 그냥 cert 를 증명하지 않으려 하면 된다. 그 방법은 아래와 같이 `GIT_SSL_NO_VERIFY` 인자로 사용하면 된다.

``` bash
GIT_SSL_NO_VERIFY=true git clone https://gitlab.mylab.com/root/mycicd.git
'mycicd'에 복제합니다...
remote: Enumerating objects: 9, done.
remote: Counting objects: 100% (9/9), done.
remote: Compressing objects: 100% (9/9), done.
remote: Total 12 (delta 2), reused 0 (delta 0), pack-reused 3
오브젝트를 받는 중: 100% (12/12), 완료.
델타를 알아내는 중: 100% (2/2), 완료.
```

### 6.4.4. 해결 2 [o] : ca.crt 배포

- [[참고] curl 에 신뢰하는 인증기관 인증서(CA Cert) 추가하기](https://www.lesstif.com/gitbook/curl-ca-cert-15892500.html)
- 먼저, curl 을 실행시 -v 옵션으로 CA 목록을 어디에서 가져오는지 위치를 확인한다.

``` bash
$ curl -v https://gitlab.mylab.com
*   Trying 10.0.0.216...
* TCP_NODELAY set
* Connected to gitlab.mylab.com (10.0.0.216) port 443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* successfully set certificate verify locations:
*   CAfile: /etc/ssl/cert.pem
  CApath: none
* TLSv1.2 (OUT), TLS handshake, Client hello (1):
* TLSv1.2 (IN), TLS handshake, Server hello (2):
* TLSv1.2 (IN), TLS handshake, Certificate (11):
* TLSv1.2 (OUT), TLS alert, unknown CA (560):
* SSL certificate problem: self signed certificate
* Closing connection 0
curl: (60) SSL certificate problem: self signed certificate
More details here: https://curl.haxx.se/docs/sslcerts.html

curl failed to verify the legitimacy of the server and therefore could not
establish a secure connection to it. To learn more about this situation and
how to fix it, please visit the web page mentioned above.
```

- 위에 로그를 보면 `CAfile: /etc/ssl/cert.pem` 를 통해 인증서를 가져오는 것을 알 수 있다.
- ca.crt 를 pem 확장자로 변경하자.

``` bash
$ openssl x509 -inform PEM -in gitlab.mylab.com.ca.crt > gitlab.mylab.com.ca.pem
```

- 그리고 생성한 pem 을 앞서 본 /etc/ssl/cert.pem 파일 맨 아래에 추가하자.

`/etc/ssl/cert.pem`

``` bash
...
### gitlab.mylab.com ca certificate PEM for gitops cicd toy project added by jongbae 21.06.21

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
```

- 이제, git clone 을 아래와 같이 다시 수행해보면 정상 clone 된다.

``` bash
$ git clone -v https://gitlab.mylab.com/root/mycicd.git
'mycicd'에 복제합니다...
POST git-upload-pack (175 bytes)
POST git-upload-pack (202 bytes)
remote: Enumerating objects: 9, done.
remote: Counting objects: 100% (9/9), done.
remote: Compressing objects: 100% (9/9), done.
remote: Total 12 (delta 2), reused 0 (delta 0), pack-reused 3
오브젝트를 받는 중: 100% (12/12), 완료.
델타를 알아내는 중: 100% (2/2), 완료.
```