**요약**

- helm 을 사용해 gitlab-runner 를 설치하고 앞서 helm 으로 설치한 gitlab 에 연동하자.
- 특히나 self-signed 인증서를 사용한 경우로, helm chart 를 수정해줬다.
- 그 후 기본적인 .gitlab-ci.yml 을 작성하여 runner 의 k8s executor 를 활용해 CI 를 수행해보자.

**목차**

- [1. 전제](#1-전제)
- [2. 환경](#2-환경)
- [3. helm 으로 gitlab runner 설치](#3-helm-으로-gitlab-runner-설치)
  - [3.1. runner 를 연동할 gitlab repo. 생성 및 token 확보](#31-runner-를-연동할-gitlab-repo-생성-및-token-확보)
  - [3.2. gitlab 인스턴스 접근 위한 custom 인증서 명시](#32-gitlab-인스턴스-접근-위한-custom-인증서-명시)
  - [3.3. gitlab 인스턴스 접근 위한 self-signed 인증서 옵션](#33-gitlab-인스턴스-접근-위한-self-signed-인증서-옵션)
  - [3.4. gitlab helm repo 추가](#34-gitlab-helm-repo-추가)
  - [3.5. gitlab-runner 헬름 차트 fetch 및 압축 해제](#35-gitlab-runner-헬름-차트-fetch-및-압축-해제)
  - [3.6. chart 수정](#36-chart-수정)
  - [3.7. values.yaml 수정](#37-valuesyaml-수정)
  - [3.8. 수정한 chart 기반 gitlab-runner 인스턴스 생성](#38-수정한-chart-기반-gitlab-runner-인스턴스-생성)
  - [3.9. 워크로드 상태 확인](#39-워크로드-상태-확인)
- [4. runner 를 사용해 k8s executor 로 CI pipeline 만들기](#4-runner-를-사용해-k8s-executor-로-ci-pipeline-만들기)
- [5. 기타](#5-기타)
  - [5.1. Ubuntu 기반 gitlab-runner Docker 이미지로 전환할 때 설정 방법](#51-ubuntu-기반-gitlab-runner-docker-이미지로-전환할-때-설정-방법)
  - [5.2. etc](#52-etc)
- [6. 트러블슈팅](#6-트러블슈팅)
  - [6.1. runner 등록 실패](#61-runner-등록-실패)
    - [6.1.1. 현상](#611-현상)
    - [6.1.2. 분석](#612-분석)
    - [6.1.3. 해결 1 : gitlab cert 에 대한 인증서 secret 별도 생성 (x)](#613-해결-1--gitlab-cert-에-대한-인증서-secret-별도-생성-x)
    - [6.1.4. 해결 2 : CA cert 에 대한 인증서 secret 별도 생성 (x)](#614-해결-2--ca-cert-에-대한-인증서-secret-별도-생성-x)
    - [6.1.5. 해결 3 : 기존 인증서 secret 사용 (x)](#615-해결-3--기존-인증서-secret-사용-x)
    - [6.1.6. 해결 4-1 : ubuntu 이미지 사용, 기존 인증서 secret 사용, ubuntu 권한 999 사용, toml 설정 (x)](#616-해결-4-1--ubuntu-이미지-사용-기존-인증서-secret-사용-ubuntu-권한-999-사용-toml-설정-x)
    - [6.1.7. 해결 4-2 : ubuntu 이미지 사용, 기존 인증서 secret 사용, ubuntu 권한 999 사용, toml 제외 (x)](#617-해결-4-2--ubuntu-이미지-사용-기존-인증서-secret-사용-ubuntu-권한-999-사용-toml-제외-x)
    - [6.1.8. 해결 4-3 : ubuntu 이미지 사용, 기존 인증서 secret 사용, ubuntu 권한 0 사용, toml 사용, 인증서 수동 이동 (ㅁ)](#618-해결-4-3--ubuntu-이미지-사용-기존-인증서-secret-사용-ubuntu-권한-0-사용-toml-사용-인증서-수동-이동-ㅁ)
    - [6.1.9. 해결 4-4 : 4-3 과 같게하고 인증서만 /etc/gitlab-runner/certs 에 복사 (ㅁ)](#619-해결-4-4--4-3-과-같게하고-인증서만-etcgitlab-runnercerts-에-복사-ㅁ)
    - [6.1.10. 해결 4-5 : 4-4 와 같게하고 인증서만 /etc/gitlab-runner/certs 에 복사하도록 chart 를 받아서 deployment 의 volumeMounts 경로를 변경 설정 (o)](#6110-해결-4-5--4-4-와-같게하고-인증서만-etcgitlab-runnercerts-에-복사하도록-chart-를-받아서-deployment-의-volumemounts-경로를-변경-설정-o)
    - [6.1.11. 해결 5-1 : helm 설치 후 컨테이너 안에서 등록 명령어 변경 후 수동 실행 (x)](#6111-해결-5-1--helm-설치-후-컨테이너-안에서-등록-명령어-변경-후-수동-실행-x)
    - [6.1.12. 해결 5-2 : 컨테이너로 runner 등록하기 (ㅁ)](#6112-해결-5-2--컨테이너로-runner-등록하기-ㅁ)
    - [6.1.13. 해결 5-3 : 컨테이너로 runner 등록하기 + 이미지를 helm 에서 사용하는 alpine 으로 변경 (ㅁ)](#6113-해결-5-3--컨테이너로-runner-등록하기--이미지를-helm-에서-사용하는-alpine-으로-변경-ㅁ)
    - [6.1.14. 해결 5-4 : 컨테이너로 runner 등록하기 + 이미지를 helm 에서 사용하는 alpine 으로 변경 + 권한을 변경 (runAsUser: 100, runAsGroup: 65533, fsGroup: 65533) (ㅁ)](#6114-해결-5-4--컨테이너로-runner-등록하기--이미지를-helm-에서-사용하는-alpine-으로-변경--권한을-변경-runasuser-100-runasgroup-65533-fsgroup-65533-ㅁ)
    - [6.1.15. 해결 6 : gitlab-runner helm 을 hostNetwork 로 돌리기 (x)](#6115-해결-6--gitlab-runner-helm-을-hostnetwork-로-돌리기-x)
  - [CI pod 안에서 docker 사용 불가 이슈](#ci-pod-안에서-docker-사용-불가-이슈)
    - [현상](#현상)
    - [분석](#분석)
    - [해결 1 (o) : config.toml 수정하여 k8s executor 에 privileged 및 cert 관련 볼륨 마운트](#해결-1-o--configtoml-수정하여-k8s-executor-에-privileged-및-cert-관련-볼륨-마운트)

**참고**

- [GitLab Docs : GitLab Runner Helm Chart](https://docs.gitlab.com/runner/install/kubernetes.html)
- [GitLab Docs : Providing a custom certificate for accessing GitLab](https://docs.gitlab.com/runner/install/kubernetes.html#providing-a-custom-certificate-for-accessing-gitlab)
- [GitLab Docs : Self-signed certificates or custom Certification Authorities](https://docs.gitlab.com/runner/configuration/tls-self-signed.html)

---

# 1. 전제

- 클러스터로부터 gitlab 서버 api 접근 가능
- k8s v1.4+ Beta API 사용 가능
- kubectl 사용 가능
- helm v3 client 설치

# 2. 환경

아래 환경 구성에 대한 내용은 생략한다.

- mater 10.0.0.163, worker1~3 10.0.0.164~166
- k8s v1.21.1
- cni: weavenet
- lb nginx ingress v0.47.0 (helm chart v4.0.11)
- metalldb v0.9.6
- gitlab v13.12.4 (helm chart v4.12.4)
- gitlab-runner v13.12.0 (helm chart v0.29.0)
- nfs-provisioner v4.0.2 (helm chart 4.0.11)

# 3. helm 으로 gitlab runner 설치

## 3.1. runner 를 연동할 gitlab repo. 생성 및 token 확보

- helm 으로 gitlab runner 를 설치할 때, 해당 runner 를 등록할 git repo. 를 token 으로 명시해야 한다. 그러므로 우선, 특정한 git repo. 를 만들어야 한다.
- gitlab 접근 > New project 클릭 > create black project 클릭 > project name 등 입력 > create project 클릭

![](/.uploads/2021-06-21-13-06-59.png)

- repo. 가 생성되면 해당 repo. 로 접근 > 좌측 메뉴에서 Settings 클릭 > CI/CD 클릭 > Runners 에 대해 Expand
- 여기에 있는 URL 과 token 정보를 기록해두자. helm 을 통해 runner 를 구축할 때, values.yaml 에 명시해줘야 한다.

![](/.uploads/2021-06-21-13-11-17.png)

## 3.2. gitlab 인스턴스 접근 위한 custom 인증서 명시

secret 을 통한 custom 인증서 명시

- `gitlab runner` helm chart 에는 gitlab 인스턴스에 접근하기 위해 gitlab 인증서를 k8s Secret 안에 넣고 /home/gitlab-runner/.gitlab-runner/certs 디렉토리 안에서 사용할 수 있다.
- 이 secret 의 각 key 들의 이름은 해당 디렉토리의 각 파일 이름들로 쓰여져야 한다.
- key/file 이름은 다음 형식을 지켜서 생성해야 한다. `<gitlab-hostname>.crt` (e.g. `gitlab.your-domain.com.crt`)
- 만약, self-signed 인증서라면, 아래 [gitlab 인스턴스에 접근하기 위해 self-signed 인증서에 대한 옵션](#32-gitlab-인스턴스에-접근하기-위해-self-signed-인증서에-대한-옵션) 를 참고하자.
- 사용되는 hostname 은 반드시 인증서가 등록된 호스트 이름이어야 한다.
- 만약, 인증서만 있을 때 secret 은 아래와 같이 만들면 된다.

``` bash
k -n <NAMESPACE> create secret generic <SECRET_NAME> \
  --from-file=<TARGET_FILENAME>=<CERTIFICATE_FILENAME>
```

- `<TARGET_FILENAME>` : 인증서 파일의 이름. (e.g. `<gitlab-hostname>.crt`, `gitlab.your-domain.com.crt`)
- `<CERTIFICATE_FILENAME>` : custom 인증서 이름.
- 그 후에, `values.yaml` 의 `certsSecretName` 인자에 대한 인수로 secret name 을 넣으면 된다.

``` yaml
certsSecretName: <SECRET_NAME>
```

## 3.3. gitlab 인스턴스 접근 위한 self-signed 인증서 옵션

system 인증서를 읽음 (Default)

- 기본적으로 gitlab runner 는 system 인증서 저장소를 읽고 이 저장소에 저장되어 있는 CA 를 사용해 gitlab 서버를 인증한다.

custom 인증서 파일을 명시

- gitlab 에 특정 인증서를 명시하며 runner 를 등록하기 위해 명령어 `gitlab-runner register --tlas-ca-file=/path` 를 써서 `tls-ca-file` 옵션을 사용할 수 있다. 혹은 `config.toml` 의 `[[runners]]` 부분에 명시할 수 있다. 이 파일은 runner 가 gitlab 서버에 접근하려할 때마다 사용한다.

주의 사항

- 만약, gitlab 서버 인증서를 자체 CA 로 서명했다면, 서명된 gitlab 서버 인증서가 아니라 CA 인증서를 사용하라.
- 인증서를 업데이트 했다면, runner 를 재시작해라.

## 3.4. gitlab helm repo 추가

``` bash
$ helm repo add gitlab https://charts.gitlab.io
```

## 3.5. gitlab-runner 헬름 차트 fetch 및 압축 해제

- 이 글에서는 self-signed 인증서를 통해 helm 으로 구축된 gitlab 에 runner 를 붙일 것이다. 그러므로 앞서 봤듯이 self-signed 인증서를 사용하기위한 추가 작업들이 필요하다. 기존 chart 설정으로는 한계가 있어 일부를 커스터마이징해야한다. 먼저, gitlab/gitlab-runner 헬름 차트를 fetch 해와 압축 해제한다.

``` bash
$ helm fetch gitlab/gitlab-runner
$ tar -xf gitlab-runner-0.29.0.tgz
```

## 3.6. chart 수정

- fetch 해온 chart 에서 deployment.yaml 안에 certs 의 volumeMounts 경로 변경하자.
- toml 등 여러 설정을 해봤지만 runner 컨테이너 구동 시에 인증서 경로를 제대로 잡지 못한다. 그러므로 /etc/gitlab-runner/certs 밑으로 인증서를 마운트해주는 작업을 해줘야 한다.

``` yaml
$ vi gitlab-runner/templates/deployment.yaml

# 73, 117 라인 바꾸기 
mountPath: /etc/gitlab-runner/certs/  #/home/gitlab-runner/.gitlab-runner/certs/
```

## 3.7. values.yaml 수정

- chart 외에도 values.yaml 을 수정하자.

`runner-override-values.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.mylab.com"
runnerRegistrationToken: "ngzTY4ehT3U63q5nhS_3"
hostAliases:
- ip: "10.0.0.216"
  hostnames:
  - "mylab.com"
  - "gitlab.mylab.com"
  - "minio.mylab.com"
  - "registry.mylab.com"
certsSecretName: "mylab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  # runAsUser: 999
  # fsGroup: 999
  runAsUser: 0
  fsGroup: 0
runners:
  config: |
    [[runners]]
      name = "myrunners3"
      url = "https://gitlab.mylab.com"
      token = "ngzTY4ehT3U63q5nhS_3"
      executor = "kubernetes"
rbac:
  create: true
      # tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      # tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      # tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

- `image` : default 는 alpine 인데 이를 사용하면 에러가 난다. 현재로는 latest 태그를 사용하여 ubuntu 기반 runner 를 사용해야 에러가 나지 않고 gitlab 에 잘 연동 된다.
- `gitlabUrl` : 필수 옵션, runner 를 사용할 gitlab 서버의 URL (e.g. https://gitlab.example.com)
- `runnerRegistrationToken` : 필수 옵션, gitlab 에 새로운 runners 를 추가하기 위한 registration token. runner 를 연동할 gitlab repo. 로부터 받을 수 있다.
- `hostAliases` : 파드 안에 /etc/hosts 밑에 gitlab 도메인을 resolve 하기 위해 추가한다.
- `certsSecretName` : 사용할 인증서의 secret 이름.
- `securityContext` : ubuntu 이미지를 사용할 때, root 로 사용해야 에러가 나지 않는다. 인증서 경로 인식과 연관이 있는 것 같다.
- `runners` : config.toml 에 대한 설정이다. 인증서 경로를 입력해줘도 반영이 되지 않는 것 같다. 아마, gitlab 에 연동된 후에 참조하는 것으로 보인다.
- `rbac.create` : gitlab runner 가 job 을 위한 k8s executor 로 파드를 만들고 삭제하고 조회할 때 사용할 serviceAccount 를 생성하라고 명시한다.

## 3.8. 수정한 chart 기반 gitlab-runner 인스턴스 생성

``` bash
$ helm upgrade --install gitlab-runner ./gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values.yaml
```

## 3.9. 워크로드 상태 확인

``` bash
$ k get po -n cicd
NAME                                          READY   STATUS      RESTARTS   AGE
gitlab-gitaly-0                               1/1     Running     0          25h
gitlab-gitlab-exporter-6f645c49c-jb4r9        1/1     Running     0          25h
gitlab-gitlab-shell-58cd975b57-drlxq          1/1     Running     0          25h
gitlab-gitlab-shell-58cd975b57-pnqjb          1/1     Running     0          25h
gitlab-migrations-4-v8m22                     0/1     Completed   0          5h16m
gitlab-minio-5557bb8cfd-vbp7r                 1/1     Running     0          25h
gitlab-minio-create-buckets-4-c6gpc           0/1     Completed   0          5h16m
gitlab-postgresql-0                           2/2     Running     0          25h
gitlab-prometheus-server-6444c7bd76-gs7k6     2/2     Running     0          25h
gitlab-redis-master-0                         2/2     Running     0          25h
gitlab-registry-6b454b5668-2w24z              1/1     Running     0          25h
gitlab-registry-6b454b5668-hlcc9              1/1     Running     0          25h
gitlab-runner-gitlab-runner-cc77f9467-qzwph   1/1     Running     0          28s
gitlab-sidekiq-all-in-1-v1-85fffc9595-rrl8z   1/1     Running     0          25h
gitlab-task-runner-556dc767c7-dccsl           1/1     Running     0          25h
gitlab-webservice-default-bb66f6b6d-bdztv     2/2     Running     0          25h
gitlab-webservice-default-bb66f6b6d-phjtk     2/2     Running     0          25h

$ k logs -n cicd gitlab-runner-gitlab-runner-cc77f9467-qzwph
Registration attempt 1 of 30
Updating CA certificates...
Runtime platform                                    arch=amd64 os=linux pid=13 revision=7a6612da version=13.12.0
Running in system-mode.

Registering runner... succeeded                     runner=ngzTY4eh
Merging configuration from template file "/configmaps/config.template.toml"
Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!
Runtime platform                                    arch=amd64 os=linux pid=1 revision=7a6612da version=13.12.0
Starting multi-runner from /etc/gitlab-runner/config.toml...  builds=0
Running in system-mode.

Configuration loaded                                builds=0
listen_address not defined, metrics & debug endpoints disabled  builds=0
[session_server].listen_address not defined, session endpoints disabled  builds=0
```

![](/.uploads/2021-06-19-18-05-38.png)

- 위와 같이 연동이 정상처리 됐다.

# 4. runner 를 사용해 k8s executor 로 CI pipeline 만들기

- 아래 파일을 작성하여 커밋하면 자동으로 파이프라인이 돌아간다.

`.gitlab-ci.yml`

``` yaml
###
default:
  image: busybox:latest
###

build-job:
  stage: build
  script:
  - echo "Hello, $GITLAB_USER_LOGIN!"

test-job1:
  stage: test
  script:
  - echo "This job tests something"

test-job2:
  stage: test
  script:
  - echo "This job tests something, but takes more time than test-job1."
  - echo "After the echo commands complete, it runs the sleep command for 20 seconds"
  - echo "which simulates a test that runs 20 seconds longer than test-job1"
  - sleep 20

deploy-prod:
  stage: deploy
  script:
  - echo "This job deploys something from the $CI_COMMIT_BRANCH branch."
```

- 파이프라인이 의도된 대로 모두 정상 수행됐다면 결과는 아래와 같다.

![](/.uploads/2021-06-21-13-19-32.png)

# 5. 기타

## 5.1. Ubuntu 기반 gitlab-runner Docker 이미지로 전환할 때 설정 방법

- 기본적으로 GitLab Runner Helm 차트는 musl libc 를 사용하는 Alpine 버전의 gitlab / gitlab-runner 이미지를 사용한다. 경우에 따라 glibc 를 사용하는 Ubuntu 기반 이미지로 전환 할 수 있다.
이렇게하려면 다음 값으로 values.yaml 파일을 업데이트해라.

``` yaml
# Specify the Ubuntu image. Remember to set the version. You can also use the `ubuntu` or `latest` tags.
image: gitlab/gitlab-runner:v13.0.0

# Update the security context values to the user ID in the ubuntu image
securityContext:
  fsGroup: 999
  runAsUser: 999
```

## 5.2. etc

- gitlab runner helm 은 executor 로 배포된다.
- gitlab ci/cd 로 부터 전달 받은 job 에 따라 이 executor 는 특정한 namespace 에 새로운 pod 를 프로비저닝한다.

# 6. 트러블슈팅

## 6.1. runner 등록 실패

### 6.1.1. 현상

- runner 파드가 gitlab 인스턴스에 등록되지 못함.

### 6.1.2. 분석

- runner 파드를 조회하면 아래와 같은 오류 발생

``` bash
Registration attempt 3 of 30
Runtime platform                                    arch=amd64 os=linux pid=32 revision=7a6612da version=13.12.0
WARNING: Running in user-mode.
WARNING: The user-mode requires you to manually start builds processing:
WARNING: $ gitlab-runner run
WARNING: Use sudo for system-mode:
WARNING: $ sudo gitlab-runner...

ERROR: Registering runner... failed                 runner=ngzTY4eh status=couldn't execute POST against https://gitlab.mylab.com/api/v4/runners: Post https://gitlab.mylab.com/api/v4/runners: EOF
PANIC: Failed to register the runner. You may be having network problems.
```

- [공식 문서](https://docs.gitlab.com/runner/install/kubernetes.html#providing-a-custom-certificate-for-accessing-gitlab) 를 참고했다.

### 6.1.3. 해결 1 : gitlab cert 에 대한 인증서 secret 별도 생성 (x)

### 6.1.4. 해결 2 : CA cert 에 대한 인증서 secret 별도 생성 (x)

### 6.1.5. 해결 3 : 기존 인증서 secret 사용 (x)

### 6.1.6. 해결 4-1 : ubuntu 이미지 사용, 기존 인증서 secret 사용, ubuntu 권한 999 사용, toml 설정 (x)

- EOF 에러, toml 정상 적용 안되는 듯, 수동 등록은 되나 UI 에 not yet connected)

`runner-override-values2.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.mylab.com"
runnerRegistrationToken: "ngzTY4ehT3U63q5nhS_3"
hostAliases:
- ip: "10.0.0.216"
  hostnames:
  - "mylab.com"
  - "gitlab.mylab.com"
  - "minio.mylab.com"
  - "registry.mylab.com"
certsSecretName: "mylab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  runAsUser: 999
  fsGroup: 999
  #runAsUser: 0
  #fsGroup: 0
runners:
  config: |
    [[runners]]
      name = "myrunners"
      url = "https://gitlab.mylab.com"
      token = "ngzTY4ehT3U63q5nhS_3"
      executor = "kubernetes"
      tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

``` bash
$ helm upgrade --install gitlab-runner gitlab/gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values2.yaml
```

``` bash
$ k exec -ti gitlab-runner-gitlab-runner-6768b5b64b-ts5wv -n cicd -- bash

> gitlab-runner register \
--name my-runner \
--url https://gitlab.mylab.com \
--registration-token ngzTY4ehT3U63q5nhS_3 \
--tls-ca-file=/home/gitlab-runner/.gitlab-runner/certs/ca.crt \
--tls-cert-file=/home/gitlab-runner/.gitlab-runner/certs/tls.crt \
--tls-key-file=/home/gitlab-runner/.gitlab-runner/certs/tls.key

Runtime platform                                    arch=amd64 os=linux pid=185 revision=7a6612da version=13.12.0
WARNING: Running in user-mode.
WARNING: The user-mode requires you to manually start builds processing:
WARNING: $ gitlab-runner run
WARNING: Use sudo for system-mode:
WARNING: $ sudo gitlab-runner...

Enter the GitLab instance URL (for example, https://gitlab.com/):
[https://gitlab.mylab.com]:
Enter the registration token:
[ngzTY4ehT3U63q5nhS_3]:
Enter a description for the runner:
[my-runner]:
Enter tags for the runner (comma-separated):

Registering runner... succeeded                     runner=ngzTY4eh
Enter an executor: docker, docker+machine, docker-ssh+machine, custom, docker-ssh, parallels, shell, ssh, virtualbox, kubernetes:
[kubernetes]:
Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!
```

### 6.1.7. 해결 4-2 : ubuntu 이미지 사용, 기존 인증서 secret 사용, ubuntu 권한 999 사용, toml 제외 (x)

- cert 에러, 수동 등록은 되나 UI 에 not yet connected

`runner-override-values2.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.mylab.com"
runnerRegistrationToken: "ngzTY4ehT3U63q5nhS_3"
hostAliases:
- ip: "10.0.0.216"
  hostnames:
  - "mylab.com"
  - "gitlab.mylab.com"
  - "minio.mylab.com"
  - "registry.mylab.com"
certsSecretName: "mylab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  runAsUser: 999
  fsGroup: 999
  #runAsUser: 0
  #fsGroup: 0
# runners:
#   config: |
#     [[runners]]
#       name = "myrunners"
#       url = "https://gitlab.mylab.com"
#       token = "ngzTY4ehT3U63q5nhS_3"
#       executor = "kubernetes"
#       tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
#       tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
#       tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

``` bash
$ helm upgrade --install gitlab-runner gitlab/gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values2.yaml
```

``` bash
$ k logs -n cicd gitlab-runner-gitlab-runner-7dc9448469-wg2rt

Registration attempt 4 of 30
Runtime platform                                    arch=amd64 os=linux pid=39 revision=7a6612da version=13.12.0
WARNING: Running in user-mode.
WARNING: The user-mode requires you to manually start builds processing:
WARNING: $ gitlab-runner run
WARNING: Use sudo for system-mode:
WARNING: $ sudo gitlab-runner...

ERROR: Registering runner... failed                 runner=ngzTY4eh status=couldnt execute POST against https://gitlab.mylab.com/api/v4/runners: Post https://gitlab.mylab.com/api/v4/runners: x509: certificate signed by unknown authority
PANIC: Failed to register the runner. You may be having network problems.

$ k exec -ti gitlab-runner-gitlab-runner-7dc9448469-wg2rt -n cicd --  bash

> gitlab-runner register \
  --name my-runner \
  --url https://gitlab.mylab.com \
  --registration-token ngzTY4ehT3U63q5nhS_3 \
  --tls-ca-file=/home/gitlab-runner/.gitlab-runner/certs/ca.crt \
  --tls-cert-file=/home/gitlab-runner/.gitlab-runner/certs/tls.crt \
  --tls-key-file=/home/gitlab-runner/.gitlab-runner/certs/tls.key
Runtime platform                                    arch=amd64 os=linux pid=241 revision=7a6612da version=13.12.0
WARNING: Running in user-mode.
WARNING: The user-mode requires you to manually start builds processing:
WARNING: $ gitlab-runner run
WARNING: Use sudo for system-mode:
WARNING: $ sudo gitlab-runner...

Enter the GitLab instance URL (for example, https://gitlab.com/):
[https://gitlab.mylab.com]:
Enter the registration token:
[ngzTY4ehT3U63q5nhS_3]:
Enter a description for the runner:
[my-runner]:
Enter tags for the runner (comma-separated):

Registering runner... succeeded                     runner=ngzTY4eh
Enter an executor: custom, docker-ssh, parallels, ssh, virtualbox, docker, shell, docker+machine, docker-ssh+machine, kubernetes:
[kubernetes]:
Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!
```

### 6.1.8. 해결 4-3 : ubuntu 이미지 사용, 기존 인증서 secret 사용, ubuntu 권한 0 사용, toml 사용, 인증서 수동 이동 (ㅁ)

`runner-override-values2.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.mylab.com"
runnerRegistrationToken: "ngzTY4ehT3U63q5nhS_3"
hostAliases:
- ip: "10.0.0.216"
  hostnames:
  - "mylab.com"
  - "gitlab.mylab.com"
  - "minio.mylab.com"
  - "registry.mylab.com"
certsSecretName: "mylab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  # runAsUser: 999
  # fsGroup: 999
  runAsUser: 0
  fsGroup: 0
runners:
  config: |
    [[runners]]
      name = "myrunners2"
      url = "https://gitlab.mylab.com"
      token = "ngzTY4ehT3U63q5nhS_3"
      executor = "kubernetes"
      tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

``` bash
$ helm uninstall -n cicd gitlab-runner
$ k get po -n cicd -w
$ helm upgrade --install gitlab-runner gitlab/gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values2.yaml
```

``` bash
$ k get po -n cicd
$ k logs -n cicd gitlab-runner-gitlab-runner-86c4ff8588-tbssw
Registration attempt 1 of 30
Runtime platform                                    arch=amd64 os=linux pid=13 revision=7a6612da version=13.12.0
Running in system-mode.

ERROR: Registering runner... failed                 runner=ngzTY4eh status=couldn't execute POST against https://gitlab.mylab.com/api/v4/runners: Post https://gitlab.mylab.com/api/v4/runners: x509: certificate signed by unknown authority
PANIC: Failed to register the runner. You may be having network problems.

```

``` bash
# gitlab-runner register \
>   --name my-runner2 \
>   --url https://gitlab.mylab.com \
>   --registration-token ngzTY4ehT3U63q5nhS_3 \
>   --tls-ca-file=/home/gitlab-runner/.gitlab-runner/certs/ca.crt \
>   --tls-cert-file=/home/gitlab-runner/.gitlab-runner/certs/tls.crt \
>   --tls-key-file=/home/gitlab-runner/.gitlab-runner/certs/tls.key
Runtime platform                                    arch=amd64 os=linux pid=235 revision=7a6612da version=13.12.0
Running in system-mode.

Enter the GitLab instance URL (for example, https://gitlab.com/):
[https://gitlab.mylab.com]:
Enter the registration token:
[ngzTY4ehT3U63q5nhS_3]:
Enter a description for the runner:
[my-runner2]:
Enter tags for the runner (comma-separated):

Registering runner... succeeded                     runner=ngzTY4eh
Enter an executor: shell, ssh, docker-ssh+machine, parallels, virtualbox, docker+machine, kubernetes, custom, docker, docker-ssh:
[kubernetes]:
Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!
```

![](/.uploads/2021-06-19-14-46-31.png)

- cert 에러, 수동 등록은 되나 UI 에 not yet connected, cert 파일과 toml 파일을 /etc/gitlab-runner 에 옮기니 임시로 해결됐다.
- 인증서를 /etc/gitlab-runner/certs 밑에 두고 /etc/gitlab-runner/config.toml 을 업데이트하니 실행 잘 됨. 단 아래와 같이 session_server listen 설정이 잘못 됐다는데 이건 toml 이 뭔가 configmap 으로 override 된 것으로 보여 설정이 필요해보임

``` bash
Registration attempt 18 of 30
Updating CA certificates...
Runtime platform                                    arch=amd64 os=linux pid=886 revision=7a6612da version=13.12.0
Running in system-mode.

Registering runner... succeeded                     runner=ngzTY4eh
Merging configuration from template file "/configmaps/config.template.toml"
Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!
Runtime platform                                    arch=amd64 os=linux pid=1 revision=7a6612da version=13.12.0
Starting multi-runner from /etc/gitlab-runner/config.toml...  builds=0
Running in system-mode.

Configuration loaded                                builds=0
listen_address not defined, metrics & debug endpoints disabled  builds=0
[session_server].listen_address not defined, session endpoints disabled  builds=0
ERROR: Checking for jobs... forbidden               runner=ngzTY4eh
ERROR: Checking for jobs... forbidden               runner=ngzTY4eh
ERROR: Checking for jobs... forbidden               runner=ngzTY4eh
ERROR: Runner https://gitlab.mylab.comngzTY4ehT3U63q5nhS_3 is not healthy and will be disabled!
```

### 6.1.9. 해결 4-4 : 4-3 과 같게하고 인증서만 /etc/gitlab-runner/certs 에 복사 (ㅁ)

- 4-3 에서 인증서 경로만 /etc/gitlab-runner/certs 밑으로 두면 로그에 이상도 없고 정상 연동 된다.

`runner-override-values2.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.mylab.com"
runnerRegistrationToken: "ngzTY4ehT3U63q5nhS_3"
hostAliases:
- ip: "10.0.0.216"
  hostnames:
  - "mylab.com"
  - "gitlab.mylab.com"
  - "minio.mylab.com"
  - "registry.mylab.com"
certsSecretName: "mylab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  # runAsUser: 999
  # fsGroup: 999
  runAsUser: 0
  fsGroup: 0
runners:
  config: |
    [[runners]]
      name = "myrunners3"
      url = "https://gitlab.mylab.com"
      token = "ngzTY4ehT3U63q5nhS_3"
      executor = "kubernetes"
      tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

``` bash
$ helm uninstall -n cicd gitlab-runner
$ k get po -n cicd -w
$ helm upgrade --install gitlab-runner gitlab/gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values2.yaml
```

``` bash
$ k get po -n cicd
$ k logs -n cicd gitlab-runner-gitlab-runner-865cfdc7ff-4vv9p
Registration attempt 1 of 30
Runtime platform                                    arch=amd64 os=linux pid=13 revision=7a6612da version=13.12.0
Running in system-mode.

ERROR: Registering runner... failed                 runner=ngzTY4eh status=couldn't execute POST against https://gitlab.mylab.com/api/v4/runners: Post https://gitlab.mylab.com/api/v4/runners: x509: certificate signed by unknown authority
PANIC: Failed to register the runner. You may be having network problems.
Registration attempt 2 of 30
Runtime platform                                    arch=amd64 os=linux pid=22 revision=7a6612da version=13.12.0
Running in system-mode.
```

``` bash

$ k exec -ti -n cicd gitlab-runner-gitlab-runner-865cfdc7ff-4vv9p -- bash

# cp /home/gitlab-runner/.gitlab-runner/certs/* /etc/gitlab-runner/certs/

$ k logs -n cicd gitlab-runner-gitlab-runner-865cfdc7ff-4vv9p
Registration attempt 13 of 30
Updating CA certificates...
Runtime platform                                    arch=amd64 os=linux pid=169 revision=7a6612da version=13.12.0
Running in system-mode.

Registering runner... succeeded                     runner=ngzTY4eh
Merging configuration from template file "/configmaps/config.template.toml"
Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!
Runtime platform                                    arch=amd64 os=linux pid=1 revision=7a6612da version=13.12.0
Starting multi-runner from /etc/gitlab-runner/config.toml...  builds=0
Running in system-mode.

Configuration loaded                                builds=0
listen_address not defined, metrics & debug endpoints disabled  builds=0
[session_server].listen_address not defined, session endpoints disabled  builds=0
```

### 6.1.10. 해결 4-5 : 4-4 와 같게하고 인증서만 /etc/gitlab-runner/certs 에 복사하도록 chart 를 받아서 deployment 의 volumeMounts 경로를 변경 설정 (o)

gitlab/gitlab-runner 헬름 차트를 fetch 해와 압축 해제

``` bash
$ helm fetch gitlab/gitlab-runner
$ tar -xf gitlab-runner-0.29.0.tgz
```

fetch 해온 chart 에서 deployment.yaml 안에 certs 의 volumeMounts 경로 변경

``` yaml
$ vi gitlab-runner/templates/deployment.yaml

# 73, 117 라인 바꾸기 
mountPath: /etc/gitlab-runner/certs/  #/home/gitlab-runner/.gitlab-runner/certs/
```

수정한 chart 기반 gitlab-runner 인스턴스 생성

`runner-override-values2.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.mylab.com"
runnerRegistrationToken: "ngzTY4ehT3U63q5nhS_3"
hostAliases:
- ip: "10.0.0.216"
  hostnames:
  - "mylab.com"
  - "gitlab.mylab.com"
  - "minio.mylab.com"
  - "registry.mylab.com"
certsSecretName: "mylab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  # runAsUser: 999
  # fsGroup: 999
  runAsUser: 0
  fsGroup: 0
runners:
  config: |
    [[runners]]
      name = "myrunners3"
      url = "https://gitlab.mylab.com"
      token = "ngzTY4ehT3U63q5nhS_3"
      executor = "kubernetes"
      # tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      # tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      # tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

``` bash
$ helm uninstall -n cicd gitlab-runner
$ k get po -n cicd -w
$ helm upgrade --install gitlab-runner ./gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values2.yaml
```

``` bash
$ k get po -n cicd
$ k logs -n cicd gitlab-runner-gitlab-runner-cc77f9467-qq9vs
Registration attempt 1 of 30
Updating CA certificates...
Runtime platform                                    arch=amd64 os=linux pid=13 revision=7a6612da version=13.12.0
Running in system-mode.

Registering runner... succeeded                     runner=ngzTY4eh
Merging configuration from template file "/configmaps/config.template.toml"
Runner registered successfully. Feel free to start it, but if it's running already the config should be automatically reloaded!
Runtime platform                                    arch=amd64 os=linux pid=1 revision=7a6612da version=13.12.0
Starting multi-runner from /etc/gitlab-runner/config.toml...  builds=0
Running in system-mode.

Configuration loaded                                builds=0
listen_address not defined, metrics & debug endpoints disabled  builds=0
[session_server].listen_address not defined, session endpoints disabled  builds=0
```

### 6.1.11. 해결 5-1 : helm 설치 후 컨테이너 안에서 등록 명령어 변경 후 수동 실행 (x)

등록 명령어

- 결국 아래 값이 디폴트였으며 직접 수행해도 동일한 오류남.

``` bash
gitlab-runner register \
--name my-runner \
--url https://gitlab.mylab.com \
--registration-token ngzTY4ehT3U63q5nhS_3 \
--tls-ca-file=/home/gitlab-runner/.gitlab-runner/certs/ca.crt \
--tls-cert-file=/home/gitlab-runner/.gitlab-runner/certs/tls.crt \
--tls-key-file=/home/gitlab-runner/.gitlab-runner/certs/tls.key
```

### 6.1.12. 해결 5-2 : 컨테이너로 runner 등록하기 (ㅁ)

- [참고](https://docs.gitlab.com/runner/install/docker.html)
- gitlab/gitlab-runner:latest
- 컨테이너 실행

``` bash
$ docker run -d --name gitlab-runner-con --restart always \
     -v /home/myai/my-gitlab-helm/gitlab-runner-crt:/etc/gitlab-runner \
     -v /var/run/docker.sock:/var/run/docker.sock \
     --net=host \
     gitlab/gitlab-runner:latest

$ docker exec -ti gitlab-runner-con bash

apt update
apt install -y net-tools iputils-ping vim netcat

cat <<EOF >> /etc/hosts
# gitlab
10.0.0.216 mylab.com gitlab.mylab.com minio.mylab.com registry.mylab.com
EOF

gitlab-runner register \
--name my-runner \
--url https://gitlab.mylab.com \
--registration-token ngzTY4ehT3U63q5nhS_3 \
--tls-ca-file=/etc/gitlab-runner/ca.crt \
--tls-cert-file=/etc/gitlab-runner/tls.crt \
--tls-key-file=/etc/gitlab-runner/tls.key
```

### 6.1.13. 해결 5-3 : 컨테이너로 runner 등록하기 + 이미지를 helm 에서 사용하는 alpine 으로 변경 (ㅁ)

- gitlab/gitlab-runner:alpine-v13.12.0
- 컨테이너 실행

``` bash
$ docker run -d --name gitlab-runner-con --restart always \
     -v /home/myai/my-gitlab-helm/gitlab-runner-crt:/etc/gitlab-runner \
     -v /var/run/docker.sock:/var/run/docker.sock \
     --net=host \
     gitlab/gitlab-runner:alpine-v13.12.0

$ docker exec -ti gitlab-runner-con bash

$ apt update
$ apt install -y net-tools iputils-ping vim netcat

$ cat /etc/hosts
...
# gitlab
10.0.0.216 mylab.com gitlab.mylab.com minio.mylab.com registry.mylab.com


gitlab-runner register \
--name my-con-runner \
--url https://gitlab.mylab.com \
--registration-token ngzTY4ehT3U63q5nhS_3 \
--tls-ca-file=/etc/gitlab-runner/ca.crt \
--tls-cert-file=/etc/gitlab-runner/tls.crt \
--tls-key-file=/etc/gitlab-runner/tls.key \
--executor docker
```

- 컨테이너 실행 시에 config.toml

``` toml
concurrent = 1
check_interval = 0

[session_server]
  session_timeout = 1800

[[runners]]
  name = "my-runner"
  url = "https://gitlab.mylab.com"
  token = "2HDZBi1jVt7M8aL_4xTp"
  tls-ca-file = "/etc/gitlab-runner/ca.crt"
  tls-cert-file = "/etc/gitlab-runner/tls.crt"
  tls-key-file = "/etc/gitlab-runner/tls.key"
  executor = "kubernetes"
  [runners.custom_build_dir]
  [runners.cache]
    [runners.cache.s3]
    [runners.cache.gcs]
    [runners.cache.azure]
  [runners.kubernetes]
    host = ""
    bearer_token_overwrite_allowed = false
    image = ""
    namespace = ""
    namespace_overwrite_allowed = ""
    privileged = false
    service_account_overwrite_allowed = ""
    pod_annotations_overwrite_allowed = ""
    [runners.kubernetes.affinity]
    [runners.kubernetes.pod_security_context]
    [runners.kubernetes.volumes]
    [runners.kubernetes.dns_config]
```

### 6.1.14. 해결 5-4 : 컨테이너로 runner 등록하기 + 이미지를 helm 에서 사용하는 alpine 으로 변경 + 권한을 변경 (runAsUser: 100, runAsGroup: 65533, fsGroup: 65533) (ㅁ)

- gitlab/gitlab-runner:alpine-v13.12.0
- 컨테이너 실행

``` bash
$ docker run -d --name gitlab-runner-con --restart always \
     -v /home/myai/my-gitlab-helm/gitlab-runner-crt:/etc/gitlab-runner \
     -v /var/run/docker.sock:/var/run/docker.sock \
     --net=host \
     gitlab/gitlab-runner:alpine-v13.12.0

$ docker exec -ti gitlab-runner-con bash

$ apt update
$ apt install -y net-tools iputils-ping vim netcat

$ cat /etc/hosts
...
# gitlab
10.0.0.216 mylab.com gitlab.mylab.com minio.mylab.com registry.mylab.com


gitlab-runner register \
--name my-con-runner \
--url https://gitlab.mylab.com \
--registration-token ngzTY4ehT3U63q5nhS_3 \
--tls-ca-file=/etc/gitlab-runner/ca.crt \
--tls-cert-file=/etc/gitlab-runner/tls.crt \
--tls-key-file=/etc/gitlab-runner/tls.key \
--executor docker
```

- 컨테이너 실행 시에 config.toml

``` toml
concurrent = 1
check_interval = 0

[session_server]
  session_timeout = 1800

[[runners]]
  name = "my-runner"
  url = "https://gitlab.mylab.com"
  token = "2HDZBi1jVt7M8aL_4xTp"
  tls-ca-file = "/etc/gitlab-runner/ca.crt"
  tls-cert-file = "/etc/gitlab-runner/tls.crt"
  tls-key-file = "/etc/gitlab-runner/tls.key"
  executor = "kubernetes"
  [runners.custom_build_dir]
  [runners.cache]
    [runners.cache.s3]
    [runners.cache.gcs]
    [runners.cache.azure]
  [runners.kubernetes]
    host = ""
    bearer_token_overwrite_allowed = false
    image = ""
    namespace = ""
    namespace_overwrite_allowed = ""
    privileged = false
    service_account_overwrite_allowed = ""
    pod_annotations_overwrite_allowed = ""
    [runners.kubernetes.affinity]
    [runners.kubernetes.pod_security_context]
    [runners.kubernetes.volumes]
    [runners.kubernetes.dns_config]
```

### 6.1.15. 해결 6 : gitlab-runner helm 을 hostNetwork 로 돌리기 (x)

``` bash
$ helm upgrade --install gitlab-runner gitlab/gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values2.yaml

$ k edit deploy -n cicd gitlab-runner-gitlab-runner
spec.template.spec.hostNetwork: true

$ k exec -ti gitlab-runner-gitlab-runner-5cff684f67-ds2zf -n cicd -- bash

> gitlab-runner register \
--name my-runner \
--url https://gitlab.mylab.com \
--registration-token ngzTY4ehT3U63q5nhS_3 \
--tls-ca-file=/home/gitlab-runner/.gitlab-runner/certs/ca.crt \
--tls-cert-file=/home/gitlab-runner/.gitlab-runner/certs/tls.crt \
--tls-key-file=/home/gitlab-runner/.gitlab-runner/certs/tls.key

Registration attempt 5 of 30
Runtime platform                                    arch=amd64 os=linux pid=67 revision=7a6612da version=13.12.0
WARNING: Running in user-mode.
WARNING: The user-mode requires you to manually start builds processing:
WARNING: $ gitlab-runner run
WARNING: Use sudo for system-mode:
WARNING: $ sudo gitlab-runner...

ERROR: Registering runner... failed                 runner=ngzTY4eh status=couldn't execute POST against https://gitlab.mylab.com/api/v4/runners: Post https://gitlab.mylab.com/api/v4/runners: x509: certificate signed by unknown authority
PANIC: Failed to register the runner. You may be having network problems.
```

## CI pod 안에서 docker 사용 불가 이슈

### 현상

- gitlab runner k8s executor 를 통해 .gitlab-ci.yml 을 실행할 때 파드 즉, 컨테이너로 실행되기 때문에 docker 명령어를 사용할 수 없다.

### 분석

- dind, dood 등의 방법을 통해 docker 명령어를 사용할 수 있을 것으로 보인다.

### 해결 1 (o) : config.toml 수정하여 k8s executor 에 privileged 및 cert 관련 볼륨 마운트

- 참고 : [[blog] runner 의 config.toml 에 kubernetes 관련 설정 참고](https://adamrushuk.github.io/running-kind-in-gitlab-ci-on-kubernetes/)

- `runner-override-values2.yaml` 작성

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.mylab.com"
runnerRegistrationToken: "ngzTY4ehT3U63q5nhS_3"
hostAliases:
- ip: "10.0.0.216"
  hostnames:
  - "mylab.com"
  - "gitlab.mylab.com"
  - "minio.mylab.com"
  - "registry.mylab.com"
certsSecretName: "mylab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  # runAsUser: 999
  # fsGroup: 999
  runAsUser: 0
  fsGroup: 0
runners:
  config: |
    [[runners]]
      name = "myrunners3"
      url = "https://gitlab.mylab.com"
      token = "ngzTY4ehT3U63q5nhS_3"
      executor = "kubernetes"
      [runners.kubernetes]
        privileged = true
      [[runners.kubernetes.volumes.empty_dir]]
        name = "docker-certs"
        mount_path = "/certs/client"
        medium = "Memory"
      [[runners.kubernetes.volumes.empty_dir]]
        name = "dind-storage"
        mount_path = "/var/lib/docker"
      [[runners.kubernetes.volumes.host_path]]
        name = "hostpath-modules"
        mount_path = "/lib/modules"
        read_only = true
        host_path = "/lib/modules"
      [[runners.kubernetes.volumes.host_path]]
        name = "hostpath-cgroup"
        mount_path = "/sys/fs/cgroup"
        host_path = "/sys/fs/cgroup"
rbac:
  create: true
      # tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      # tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      # tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

- 수정한 `runner-override-values2.yaml` 기반 gitlab-runner 인스턴스 업그레이드

``` bash
$ helm upgrade --install gitlab-runner ./gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values2.yaml
```

- runner 가 해당 gitlab repo. 에 잘 적용됐는지 확인해보자.
- `.gitlab-ci.yml` 예시

``` yam
stages:
- build
- test

default:
  image:
    name: docker:19.03.13

variables:
  DOCKER_HOST: tcp://docker:2376
  # TLS -> 2376 port
  # TLS -> DOCKER_TLS_CERTDIR: "/certs"
  # No TLS -> 2375 port
  # No TLS -> DOCKER_TLS_CERTDIR: ""
  DOCKER_TLS_CERTDIR: "/certs"
  DOCKER_TLS_VERIFY: 1
  DOCKER_CERT_PATH: "$DOCKER_TLS_CERTDIR/client"

# before_script:
#   - docker info

build:
  stage: mybuild
  services:
  - name: docker:19.03.13-dind
  script:
  - apk update
  - apk upgrade
  - apk add git
  - git clone https://github.com/pytorch/serve.git
  - docker info
  - cd serve/docker
  - ls -la .
  - ./build_image.sh

test:
  stage: mytest
  script:
  - docker run --rm -it -p 8080:8080 -p 8081:8081 -p 8082:8082 -p 7070:7070 -p 7071:7071 pytorch/torchserve:latest
  - curl http://localhost:8080/ping
```

- 그 결과 k8s executor 파드 안에서 docker 명령어가 가능해졌다.