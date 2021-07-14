**요약**

- k8s executor 에 대해 알아보고 gitlab CI 를 하기 위한 설정을 하자.

**참고**

- [[GitLab Runner Docs] The Kubernetes executor](https://docs.gitlab.com/runner/executors/kubernetes.html)
- [[Blog] How to Install GitLab Runner on Kubernetes](https://adambcomer.com/blog/install-gitlab-runner-kubernetes/)
- [[GitLab Runner Docs] Adding extra host aliases](https://docs.gitlab.com/runner/executors/kubernetes.html#adding-extra-host-aliases)

**목차**

- [1. kubernetes executor 기본 개념](#1-kubernetes-executor-기본-개념)
- [2. Workflow](#2-workflow)
- [3. kubernetes API 에 연결](#3-kubernetes-api-에-연결)
- [4. kubernetes executor 의 interaction diagram](#4-kubernetes-executor-의-interaction-diagram)
- [5. runner 에 k8s 관련 `config.toml` 설정](#5-runner-에-k8s-관련-configtoml-설정)
- [6. excutor ServiceAccount 설정](#6-excutor-serviceaccount-설정)
- [7. k8s executor 에서 build 할 때 docker 사용하기](#7-k8s-executor-에서-build-할-때-docker-사용하기)
  - [7.1. dood 방법](#71-dood-방법)
  - [7.2. dind 방법](#72-dind-방법)
  - [7.3. dood 와 dind 의 리소스 제한 관련 주의 사항 : 리소스 분리](#73-dood-와-dind-의-리소스-제한-관련-주의-사항--리소스-분리)
- [8. 기타](#8-기타)
  - [8.1. (삭제 예정) runner pod logs](#81-삭제-예정-runner-pod-logs)
  - [8.2. services 에 접근하는 방법](#82-services-에-접근하는-방법)
- [9. 트러블슈팅](#9-트러블슈팅)
  - [9.1. ERROR: Job failed (system failure): secrets is forbidden](#91-error-job-failed-system-failure-secrets-is-forbidden)
    - [9.1.1. 현상](#911-현상)
    - [9.1.2. 분석](#912-분석)
    - [9.1.3. 해결 1 : RBAC 활성화 (o)](#913-해결-1--rbac-활성화-o)
  - [9.2. job 실행 중, gitlab 에 https 접근 불가](#92-job-실행-중-gitlab-에-https-접근-불가)
    - [9.2.1. 현상 : pipeline 실행 중단](#921-현상--pipeline-실행-중단)
    - [9.2.2. 분석 : git repo. fetch 불가](#922-분석--git-repo-fetch-불가)
    - [9.2.3. 해결 1 [o] : config.toml 에 runners.kubernetes.host_aliases 설정](#923-해결-1-o--configtoml-에-runnerskuberneteshost_aliases-설정)

---

# 1. kubernetes executor 기본 개념

- kubernetes executor 는 GitLab CI 에서 사용되며, 빌드를 위해 k8s 클러스터를 사용할 수 있게 해준다.
- executor 는 k8s 클러스터의 API 를 호출해 각 GitLab CI job 마다 새로운 파드를 만든다.
- 이 파드 안에는 build container, helper continer, `.gitlab-ci.yml` 또는 `config.toml` 파일 안에 정의된 각 `service` 에 대한 container 총 세 가로 구성되어 있다.
- 이 세 가지 컨테이너의 이름들.
  - build container 는 `build`
  - helper container 는 `helper`
  - services container 는 `svc-X` (`X` 는 `[0-9]+`)

# 2. Workflow

- k8s executor 는 build 를 여러 단계로 나눈다.
  1. Prepare : k8s 클러스터 안에서 파드를 만든다. 파드에는 build 와 services 를 실행할 컨테이너를 만든다.
  2. Pre-build : cache 를 clone, restore 하고 이전 stage 에서 artifact 를 다운로드한다. 이는 해당 파드 안에서 별도 컨테이너로 실행된다.
  3. Build : User build.
  4. Post-build : cache 를 만들고 GitLab 에 artifact 를 업로드한다. 이는 해당 파드 안에서 별도 컨테이너로 실행된다.

# 3. kubernetes API 에 연결

- kubernetes API 에 연결하기 위한 옵션들. (optional 로써 명시되지 않으면 auto-discover 하려고 한다.)
  - `host` : k8s apiserver host URL
  - `cert_file` : k8s apiserver user auth 인증서
  - `key_file` : k8s apiserver user auth 개인키
  - `ca_file` : k8s apiserver user ca 인증서

- 제공되는 user account 는 특정한 namespace 에 있는 Pod 에 create, list, attach 할 수 있는 권한이 있어야 한다.
- **만약, GitLab Runner 를 k8s 클러스터 안에서 실행하고 있다면, 위에 옵션들은 생략해도 된다. runner 가 kubernetes API 를 auto-discover 할 것이기 때문이다.**
- 만약, 클러스터 외부에서 돌린다면, 위에 옵션들을 설정해줘야할 필요가 있고 kubernetes API 에 잘 접근하는 지 꼭 확인하라.

# 4. kubernetes executor 의 interaction diagram

- 아래 다이어그램은 k8s 클러스터 위에서 실행중인 GitLab Runner 와 k8s API 간에 interaction 을 나타낸다. k8s API 는 k8s 위에 있는 runner 가 클러스터 위에 pod 를 만들고자 할 때 사용된다. 이 interaction 은 어떤 k8s 클러스터여도 동일하다.

![](/.uploads/2021-06-20-01-56-28.png)

# 5. runner 에 k8s 관련 `config.toml` 설정

- [[GitLab Docs] The available config.toml settings](https://docs.gitlab.com/runner/executors/kubernetes.html#the-available-configtoml-settings)

# 6. excutor ServiceAccount 설정

- `config.yaml` 에 `KUBERNETES_SERVICE_ACCOUNT` 환경변수 설정하여 SA 를 명시할 수 있다. 혹은 `--kubernetes-service-account` 플래그를 사용하여 설정할 수 있다.

# 7. k8s executor 에서 build 할 때 docker 사용하기

- k8s executor 를 사용하면 gitlab CI/CD 는 pod 를 생성하여 job 을 수행한다. 이 파드의 컨테이너 안에서 작업을 수행하기 위해서는 여러 가지 고려해야할 사항들이 있다. 예를 들면, job 이 일반적인 docker build 일 경우, 컨테이너 안에서 docker build 를 하게끔 설정해줘야 한다.
- 먼저, [여기](https://docs.gitlab.com/runner/executors/kubernetes.html#using-docker-in-your-builds) 를 참고했다.

## 7.1. dood 방법

- docker out of docker 를 통해 빌드를 위한 도커 클라이언트 컨테이너에서 호스트 도커 데몬 서버를 사용할 수 있게 되는데 이는 다른 컨테이너들에 영향을 줄 수 있고 이름이나 포트가 중복되면 안되기 때문에 권고하지 않는 방법이다.
- 사용 방법은 다음과 같다. `runners.kubernetes.volumes.host_path` 옵션을 사용해 호스트의 `/var/run/docker.sock` 를 빌드 컨테이너에 expose 하자.

## 7.2. dind 방법

- `docker-in-docker` 이미지 즉, `docker:dind` 이미지를 실행하는 것은 아쉽게도 previleged 모드에서 실행돼야 한다.
- 기본적으로 파드 안에 컨테이너들은 파드 안에 할당된 볼륨들만 공유 가능하고 localhost 를 사용하여 서로 연결할 수 있는 IP 주소만 공유한다.
- `/var/run/docker.sock` 은 `docker:dind` 컨테이너에 의해 공유되지 않으며 docker 바이너리는 기본적으로 `/var/run/docker.sock` 를 사용하려고 한다. 이를 덮어 쓰고 클라이언트가 TCP 를 사용하여 Docker 데몬에 연결하도록 하려면 다른 컨테이너에 빌드 컨테이너의 환경 변수를 포함해야한다.
  - `DOCKER_HOST=tcp://localhost:2375` for no TLS connection.
  - `DOCKER_HOST=tcp://localhost:2376` for TLS connection.

- docker-in-docker 이미지라고도 알려진 docker : dind를 실행하는 것도 가능하지만 슬프게도 컨테이너가 권한 모드에서 실행되어야합니다. 그러한 위험을 감수 할 의향이 있다면 언뜻보기에는 쉽지 않은 다른 문제가 발생할 것입니다. Docker 데몬은 일반적으로 .gitlab-ci.yaml에서 서비스로 시작되기 때문에 포드에서 별도의 컨테이너로 실행됩니다. 기본적으로 포드의 컨테이너는 할당 된 볼륨과 localhost를 사용하여 서로 연결할 수있는 IP 주소 만 공유합니다. /var/run/docker.sock은 docker : dind 컨테이너에서 공유되지 않으며 docker 바이너리는 기본적으로이를 사용하려고합니다.

## 7.3. dood 와 dind 의 리소스 제한 관련 주의 사항 : 리소스 분리

- dind(`docker:dind`) 및 dood(`/var/run/docker.sock`) 두 경우 모두 Docker 데몬은 호스트 시스템의 기본 커널에 액세스 할 수 있다. 즉, Pod 에 설정된 `limit` 은 Docker 이미지를 빌드 할 때 적용되지 않는다. Docker 데몬은 Kubernetes 가 생성한 도커 빌드 컨테이너에 할당된 `limit` 에 관계 없이 노드의 전체 용량을 나타낸다.
- dind(previleged 모드에서 실행) 혹은 dood(/var/run/docker.sock 을 노출하여 실행) 에서 호스트 커널이 빌드 컨테이너에 노출되는 것을 최소화하는 방법으로 `node_selector` 옵션이 있다. 이는 하나 이상의 레이블이 맞는 노드로 제한하는 것이다. 예를 들어, 어떤 노드에서는 프로덕션 서비스를 실행하는 동안 빌드 컨테이너는 `role=ci` 로 레이블이 지정된 노드에서만 실행될 수 있다. 또는 `taint` 를 사용하여 빌드 컨테이너를 추가로 분리 할 수 ​​있다. 이렇게 하면 추가 설정없이 다른 포드가 빌드 포드와 동일한 노드에서 예약하는 것을 허용하지 않는다.

# 8. 기타

## 8.1. (삭제 예정) runner pod logs

- 기존 runner 연동 시 logs 인데 listen_address 가 무슨 문제 있나 기록해놓음

``` log
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

- 아래 설정을 config.toml 에 넣으면 해결될 것 같다.

``` toml
[session_server]
  listen_address = ':9252'
```

## 8.2. services 에 접근하는 방법

- [참고](https://docs.gitlab.com/ee/ci/services/index.html#accessing-the-services)
- 애플리케이션과의 일부 API 통합을 테스트하기 위해 Wordpress 인스턴스가 필요하다고 가정해 보자. 예를 들어, `.gitlab-ci.yml` 파일에서 `tutum/wordpress` 이미지를 사용한다고 하자.

``` bash
services:
  - tutum/wordpress:latest
```

- 서비스 alias 를 지정하지 않으면 작업이 실행될 때 `tutum/wordpress` 가 시작된다. 두 개의 호스트 이름으로 빌드 컨테이너에서 액세스 할 수 있다.
  - tutum-wordpress
  - tutum__wordpress
- 언더스코어가 있는 호스트네임은 RFC 가 유효하지 않으며 서드파티 애플리케이션에서 문제를 일으킬 수 있다. service 호스트네임의 기본 alias 는 다음 규칙에 따라 이미지 이름에서 생성된다.
  - 콜론 (:) 뒤의 모든 것은 제거.
  - 슬래시 (/)는 더블언더스코어 `__` 로 대체되고 기본 별칭이 생성.
  - 슬래시 (/)는 단일 대시 `-` 로 대체되고 secondary alias 가 생성된다. (GitLab Runner v1.1.0 이상 필요).

# 9. 트러블슈팅

## 9.1. ERROR: Job failed (system failure): secrets is forbidden

### 9.1.1. 현상

- .gitlab-ci.yml 돌려서 실행했지만 완료되지 않고 오류남.

### 9.1.2. 분석

- 오류 log 는 `해당 gitlab repo. -> CI/CD -> Jobs -> 해당 job 선택 -> 해당 stage 선택 -> 출력된 log` 에서 조회할 수 있으며 아래와 같다.

``` log
1  Running with gitlab-runner 13.12.0 (7a6612da)
2  on gitlab-runner-gitlab-runner-568954c478-cgd2z gVQMabxj
3  Preparing the "kubernetes" executor 00:00
4  Using Kubernetes namespace: cicd
5  Using Kubernetes executor with image ruby:2.7.2 ...
7  Preparing environment 00:00
9  ERROR: Job failed (system failure): prepare environment: secrets is forbidden: User "system:serviceaccount:cicd:default" cannot create resource "secrets" in API group "" in the namespace "cicd". Check https://docs.gitlab.com/runner/shells/index.html#shell-profile-loading for more information
```

- 오류 내용으로 보아 현재 runner 는 job 을 수행하기 위해 system:serviceaccount:cicd:default 라는 serviceAccount 를 사용한다. 그런데, 이 SA 는 secret 리소스를 생성할 권한이 없어 오류가 난 것으로 보인다.

### 9.1.3. 해결 1 : RBAC 활성화 (o)

- 참고 : [[GitLab Docs] Enabling RBAC support](https://docs.gitlab.com/runner/install/kubernetes.html#enabling-rbac-support)
- runner 헬름 차트 values.yaml 안에서 rbac 를 생성한다고 명시하면 차트가 필요한 권한을 갖는 serviceAccount 를 생성한다. 그러면, job 이 돌아갈 때, 이 sa 를 자동으로 활용해 executor 로 쓸 파드를 만든다. 아래와 같이 values.yaml 을 수정하자.

`values.yaml`

``` yaml
...
rbac:
  create: true
```

- 만약, 기존에 있던 serviceAccount 를 사용하려면 아래와 같이 작성하자.

``` taml
...
rbac:
  create: false
  serviceAccountName: <your-service-account>
```

- values.yaml 을 수정하여 핼름을 재배포하면 아래와 같이 runner 의 sa 가 생성된다.

``` bash
$ k get sa -n cicd
NAME                          SECRETS   AGE
default                       1         37h
gitlab-certmanager-issuer     1         33h
gitlab-prometheus-server      1         33h
gitlab-runner-gitlab-runner   1         7s

$ k describe sa -n cicd gitlab-runner-gitlab-runner
Name:                gitlab-runner-gitlab-runner
Namespace:           cicd
Labels:              app=gitlab-runner-gitlab-runner
                     app.kubernetes.io/managed-by=Helm
                     chart=gitlab-runner-0.29.0
                     heritage=Helm
                     release=gitlab-runner
Annotations:         meta.helm.sh/release-name: gitlab-runner
                     meta.helm.sh/release-namespace: cicd
Image pull secrets:  <none>
Mountable secrets:   gitlab-runner-gitlab-runner-token-hdv6g
Tokens:              gitlab-runner-gitlab-runner-token-hdv6g
Events:              <none>
```

## 9.2. job 실행 중, gitlab 에 https 접근 불가

### 9.2.1. 현상 : pipeline 실행 중단

- gitlab CI 실행했으나 아래와 같이 에러 발생하며 중단됨.
- 오류 log 는 `해당 gitlab repo. -> CI/CD -> Jobs -> 해당 job 선택 -> 해당 stage 선택 -> 출력된 log` 에서 조회할 수 있으며 아래와 같다.

``` log
1 Running with gitlab-runner 13.12.0 (7a6612da)
2  on gitlab-runner-gitlab-runner-6854dbc547-9ktrg LBxMyCry
3  Preparing the "kubernetes" executor 00:00
4  Using Kubernetes namespace: cicd
5  Using Kubernetes executor with image busybox:latest ...
7  Preparing environment 02:40
8  WARNING: Pulling GitLab Runner helper image from Docker Hub. Helper image is migrating to registry.gitlab.com, for more information see https://docs.gitlab.com/runner/configuration/advanced-configuration.html#migrate-helper-image-to-registrygitlabcom
9  Waiting for pod cicd/runner-lbxmycry-project-2-concurrent-07cfcd to be running, status is Pending
10 Waiting for pod cicd/runner-lbxmycry-project-2-concurrent-07cfcd to be running, status is Pending
11	ContainersNotReady: "containers with unready status: [build helper]"
...
165	ContainersNotReady: "containers with unready status: [build helper]"
166 Running on runner-lbxmycry-project-2-concurrent-07cfcd via gitlab-runner-gitlab-runner-6854dbc547-9ktrg...
168 Getting source from Git repository 00:00
169 Fetching changes with git depth set to 50...
170 Initialized empty Git repository in /builds/LBxMyCry/0/root/mycicd/.git/
171 Created fresh repository.
172 fatal: unable to access 'https://gitlab.mylab.com/root/mycicd.git/': OpenSSL SSL_connect: SSL_ERROR_SYSCALL in connection to gitlab.mylab.com:443 
174 Cleaning up file based variables 00:00
176 ERROR: Job failed: command terminated with exit code 1
```

### 9.2.2. 분석 : git repo. fetch 불가

- pipeline 이 돌기 위해서는 우선 gitlab runner 는 k8s executor 로 파드를 생성하고 그 파드 안에서 해당 pipeline 의 git repo. 를 다운받는 것 같다. 그런데, 이 repo. 를 다운받아 오지 못해 오류가 발생하는 것 같다.

### 9.2.3. 해결 1 [o] : config.toml 에 runners.kubernetes.host_aliases 설정

- [[GitLab Runner Docs] Adding extra host aliases](https://docs.gitlab.com/runner/executors/kubernetes.html#adding-extra-host-aliases) 를 참고했다.
- 사용한 `override-values.yaml` 이다.

``` bash
$ cat runner-override-values.yaml
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
certsSecretName: "ailab-gitlab"
securityContext:
  # runAsUser: 100
  # runAsGroup: 65533
  # fsGroup: 65533
  # supplementalGroups: [65533]

  ## Note: values for the ubuntu image:
  #runAsUser: 999
  #fsGroup: 999
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
        image = "ubuntu:18.04"
        [[runners.kubernetes.host_aliases]]
          ip = "10.0.0.216"
          hostnames = ["gitlab.mylab.com", "minio.mylab.com", "registry.mylab.com"]
rbac:
  create: true

      # tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      # tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      # tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
```

- 수정한 values 기반 gitlab-runner 인스턴스 업데이트

``` bash
$ helm upgrade --install gitlab-runner ./gitlab-runner \
  -n cicd --create-namespace \
  -f runner-override-values.yaml
```

- 파이프라인을 재실행 했을 때 정상적으로 수행 됐다. 로그는 아래와 같다.

``` log
[0KRunning with gitlab-runner 13.12.0 (7a6612da)
[0;m[0K  on gitlab-runner-gitlab-runner-7fcb546446-jcqqb MBpAHPXc
[0;msection_start:1624208285:prepare_executor
[0K[0K[36;1mPreparing the "kubernetes" executor[0;m
[0;m[0KUsing Kubernetes namespace: cicd
[0;m[0KUsing Kubernetes executor with image busybox:latest ...
[0;msection_end:1624208285:prepare_executor
[0Ksection_start:1624208285:prepare_script
[0K[0K[36;1mPreparing environment[0;m
[0;m[0;33mWARNING: Pulling GitLab Runner helper image from Docker Hub. Helper image is migrating to registry.gitlab.com, for more information see https://docs.gitlab.com/runner/configuration/advanced-configuration.html#migrate-helper-image-to-registrygitlabcom
[0;mWaiting for pod cicd/runner-mbpahpxc-project-2-concurrent-0wwbxg to be running, status is Pending
Waiting for pod cicd/runner-mbpahpxc-project-2-concurrent-0wwbxg to be running, status is Pending
	ContainersNotReady: "containers with unready status: [build helper]"
	ContainersNotReady: "containers with unready status: [build helper]"
Running on runner-mbpahpxc-project-2-concurrent-0wwbxg via gitlab-runner-gitlab-runner-7fcb546446-jcqqb...
section_end:1624208291:prepare_script
[0Ksection_start:1624208291:get_sources
[0K[0K[36;1mGetting source from Git repository[0;m
[0;m[32;1mFetching changes with git depth set to 50...[0;m
Initialized empty Git repository in /builds/MBpAHPXc/0/root/mycicd/.git/
[32;1mCreated fresh repository.[0;m
[32;1mChecking out 87f5240d as master...[0;m

[32;1mSkipping Git submodules setup[0;m
section_end:1624208292:get_sources
[0Ksection_start:1624208292:step_script
[0K[0K[36;1mExecuting "step_script" stage of the job script[0;m
[0;m[32;1m$ echo "Hello, $GITLAB_USER_LOGIN!"[0;m
Hello, root!
section_end:1624208292:step_script
[0Ksection_start:1624208292:cleanup_file_variables
[0K[0K[36;1mCleaning up file based variables[0;m
[0;msection_end:1624208292:cleanup_file_variables
[0K[32;1mJob succeeded
[0;m
```
