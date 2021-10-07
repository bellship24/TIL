**목차**

- [1. 요약](#1-요약)
- [2. 현상](#2-현상)
- [3. 분석](#3-분석)
- [4. 해결](#4-해결)

---

# 1. 요약

- helm 기반의 gitlab 과 gitlab-runner k8s executor 로 CI 를 수행하기 위해 docker-dind 를 사용할 때, docker 서버가 구동되지 않는 오류가 발생할 수 있다.
- k8s executor 에서 docker-dind 를 사용하기 위해서는 이를 사이드카 컨테이너인 service 컨테이너로 올려야 한다. 그리고 build 컨테이너는 docker client 역할을 하게 되는데, 여기서 docker 서버를 사용하려면, 두 컨테이너 간에 도커 관련 볼륨을 empty_dir 로 잡아 공유할 수 있도록 해야 정상적으로 구동된다. 사용한 볼륨은 아래 `config.toml` 에서 확인할 수 있다.
- 이런 전제하에 만약, `/certs/client` 경로에 다른 인증서 파일을 오버라이드 한다면(예를 들어, 레지스트리 인증을 위한 자체 서명 인증서), docker-dind 의 인증서 생성이 되지 않아 docker 서버가 구동되지 않는 문제가 발생할 수 있으므로 주의해야 한다.

# 2. 현상

![](/.uploads2/2021-10-08-02-16-15.png)

- CI 수행 중에 `docker info` 명령어에 대해 docker 서버가 돌아가고 있지 않다는 에러가 발생했다.

# 3. 분석

- `.gitlab-ci.yml` 을 통해 k8s executor 기반 gitlab runner 를 수행할 때, dind 를 사용했다.
- dind 는 service 형태인 사이드카 컨테이너로 올렸다. 즉, `.gitlab-ci.yml` 이 커밋되고 gitlab runner 는 k8s executor 에 의해 job 을 수행하는 일시적인 파드를 생성할 텐데 여기에 컨테이너가 build, helper, svc-0 이렇게 3 가지가 들어가게 된다.
- 그런데, svc 로 올린 docker-dind 가 정상적으로 작동하지 않은 것이다.

- 관련 파일들은 아래와 같다.

`.gitlab-ci.yaml`

``` yaml
stages:
  - build
  - test
  - deploy

default:
  image: 
    name: docker:19.03.13
  services:
    - name: docker:19.03.13-dind
      command: ["--insecure-registry=registry.bellship.com"]

variables:
  # Use TLS https://docs.gitlab.com/ee/ci/docker/using_docker_build.html#tls-enabled
  DOCKER_HOST: tcp://docker:2376
  DOCKER_TLS_CERTDIR: "/certs"
  DOCKER_TLS_VERIFY: 1
  DOCKER_CERT_PATH: "$DOCKER_TLS_CERTDIR/client"
  CONTAINER_TEST_IMAGE: $CI_REGISTRY_IMAGE:$CI_COMMIT_REF_SLUG
  CONTAINER_RELEASE_IMAGE: $CI_REGISTRY_IMAGE:latest

before_script:
  - apk add --update curl && rm -rf /var/cache/apk/*
  - ls -a $DOCKER_TLS_CERTDIR/client
  - echo $CI_REGISTRY
  - curl -v --cacert /$DOCKER_TLS_CERTDIR/client/ca.pem https://$CI_REGISTRY
  - docker info
  - echo -n $CI_JOB_TOKEN | docker login -u gitlab-ci-token --password-stdin $CI_REGISTRY

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

`override-values.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.bellship.com"
runnerRegistrationToken: "JyJDr_4PZPYChjpj1Hsq"
hostAliases:
- ip: "10.123.123.217"
  hostnames:
  - "bellship.com"
  - "gitlab.bellship.com"
  - "minio.bellship.com"
  - "registry.bellship.com"
certsSecretName: "bellship-wildcard-tls"
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
      name = "ains-specific-runner"
      url = "https://gitlab.bellship.com"
      token = "JyJDr_4PZPYChjpj1Hsq"
      executor = "kubernetes"
      # tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      # tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      # tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
      [runners.kubernetes]
        image = "docker:19.03.13"
        privileged = true
        [[runners.kubernetes.host_aliases]]
          ip = "10.123.123.217"
          hostnames = ["gitlab.bellship.com", "minio.bellship.com", "registry.bellship.com"]
      [[runners.kubernetes.volumes.secret]]
        name = "bellship-ca-pem"
        mount_path = "/certs/client"
        read_only = true
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
```

`build-job` stage 의 logs

``` txt
[0KRunning with gitlab-runner 14.2.0 (58ba2b95)[0;m
[0K  on gitlab-runner-specific-gitlab-runner-58f658699c-bqvzx czqJ3FaV[0;m
section_start:1633605201:prepare_executor
[0K[0K[36;1mPreparing the "kubernetes" executor[0;m[0;m
[0KUsing Kubernetes namespace: cicd[0;m
[0KUsing Kubernetes executor with image docker:19.03.13 ...[0;m
[0KUsing attach strategy to execute scripts...[0;m
section_end:1633605201:prepare_executor
[0Ksection_start:1633605201:prepare_script
[0K[0K[36;1mPreparing environment[0;m[0;m
Waiting for pod cicd/runner-czqj3fav-project-2-concurrent-0kgnps to be running, status is Pending
Waiting for pod cicd/runner-czqj3fav-project-2-concurrent-0kgnps to be running, status is Pending
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
Running on runner-czqj3fav-project-2-concurrent-0kgnps via gitlab-runner-specific-gitlab-runner-58f658699c-bqvzx...

section_end:1633605208:prepare_script
[0Ksection_start:1633605208:get_sources
[0K[0K[36;1mGetting source from Git repository[0;m[0;m
[32;1mFetching changes with git depth set to 50...[0;m
Initialized empty Git repository in /builds/czqJ3FaV/0/root/test/.git/
[32;1mCreated fresh repository.[0;m
[32;1mChecking out a5be5f2f as main...[0;m

[32;1mSkipping Git submodules setup[0;m

section_end:1633605208:get_sources
[0Ksection_start:1633605208:step_script
[0K[0K[36;1mExecuting "step_script" stage of the job script[0;m[0;m
[32;1m$ apk add --update curl && rm -rf /var/cache/apk/*[0;m
fetch http://dl-cdn.alpinelinux.org/alpine/v3.12/main/x86_64/APKINDEX.tar.gz
fetch http://dl-cdn.alpinelinux.org/alpine/v3.12/community/x86_64/APKINDEX.tar.gz
(1/3) Installing nghttp2-libs (1.41.0-r0)
(2/3) Installing libcurl (7.79.1-r0)
(3/3) Installing curl (7.79.1-r0)
Executing busybox-1.31.1-r19.trigger
OK: 12 MiB in 23 packages
[32;1m$ ls -a $DOCKER_TLS_CERTDIR/client[0;m
.
..
..2021_10_07_11_13_22.383769750
..data
ca.pem
[32;1m$ echo $CI_REGISTRY[0;m
registry.bellship.com
[32;1m$ curl -v --cacert /$DOCKER_TLS_CERTDIR/client/ca.pem https://$CI_REGISTRY[0;m
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed

  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 10.123.123.217:443...
* Connected to registry.bellship.com (10.123.123.217) port 443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* successfully set certificate verify locations:
*  CAfile: //certs/client/ca.pem
*  CApath: none
} [5 bytes data]
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
} [512 bytes data]
* TLSv1.3 (IN), TLS handshake, Server hello (2):
{ [122 bytes data]
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
{ [19 bytes data]
* TLSv1.3 (IN), TLS handshake, Certificate (11):
{ [808 bytes data]
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
{ [264 bytes data]
* TLSv1.3 (IN), TLS handshake, Finished (20):
{ [52 bytes data]
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
} [1 bytes data]
* TLSv1.3 (OUT), TLS handshake, Finished (20):
} [52 bytes data]
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server accepted to use h2
* Server certificate:
*  subject: CN=*.bellship.com
*  start date: Oct  6 12:04:27 2021 GMT
*  expire date: Jan  4 12:04:27 2022 GMT
*  subjectAltName: host "registry.bellship.com" matched cert's "*.bellship.com"
*  issuer: CN=*.bellship.com
*  SSL certificate verify ok.
* Using HTTP2, server supports multiplexing
* Connection state changed (HTTP/2 confirmed)
* Copying HTTP/2 data in stream buffer to connection buffer after upgrade: len=0
} [5 bytes data]
* Using Stream ID: 1 (easy handle 0x5643b04f7fe0)
} [5 bytes data]
> GET / HTTP/2
> Host: registry.bellship.com
> user-agent: curl/7.79.1
> accept: */*
> 
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [57 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [57 bytes data]
* old SSL session ID is stale, removing
{ [5 bytes data]
* Connection state changed (MAX_CONCURRENT_STREAMS == 128)!
} [5 bytes data]
< HTTP/2 200 
< date: Thu, 07 Oct 2021 11:13:29 GMT
< content-length: 0
< cache-control: no-cache
< strict-transport-security: max-age=15724800; includeSubDomains
< 
{ [0 bytes data]

  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0
* Connection #0 to host registry.bellship.com left intact
[32;1m$ docker info[0;m
Client:
 Debug Mode: false

Server:
ERROR: Cannot connect to the Docker daemon at tcp://docker:2376. Is the docker daemon running?
errors pretty printing info

section_end:1633605210:step_script
[0Ksection_start:1633605210:cleanup_file_variables
[0K[0K[36;1mCleaning up file based variables[0;m[0;m

section_end:1633605210:cleanup_file_variables
[0K[31;1mERROR: Job failed: command terminated with exit code 1[0;m
```

- 내용을 조금 설명하자면, 앞서 말했듯이 docker-dind 를 svc 로 올렸다. 그리고 registry 는 helm 으로 설치했으므로 built-in 인 gitlab-registry 를 사용하는데 여기에 `docker login` 하기 위해 먼저, 도메인네임을 해석하고 접근할 수 있는지 curl 로 테스트해봤다.
- gitlab 을 helm 으로 구축할 때, 인증서를 자체서명 인증서로 사용했다. 그렇기 때문에, curl 테스트 과정에서 ca.crt 가 없다는 에러를 직면했다.(이 에러는 여기서 명시하지 않음) 이를 해결하기 위해, 자체서명 인증서의 ca.pem 을 마운트하려고 gitlab 의 `config.toml` 을 수정했다.(ca.pem 을 마운트 하려는 이유는 svc-0 의 `docker logs` 를 보면, ca.pem 이 없다고 하는데, 이는 registry 의 자체서명인증서가 없다는 것으로 착각하고 수행한 것이다. 그런데, 사실은 도커 서버의 ca.pem 을 말한 것이다.) helm 으로 gitlab 을 구축했으므로 config.toml 은 override-values.yaml 에 작성했다.
- **그런데, 이 과정에서 문제가 생겼다. config.toml 을 보면, ca.pem 을 마운트할 때, secret 으로 마운트했는데 그 경로가 `/certs/client` 이다. 그런데, 이 경로는 build 컨테이너인 docker client 와 svc-0 컨테이너인 docker-dind 가 서로 고유할 마운트로써 docker 서버의 인증서가 담겨있는 곳이다. 이런 경로를 override 했으므로 도커 서버의 인증서가 삭제됐고 그로 인해, 도커 서버를 실행시키지 못한 것이다.**

# 4. 해결

- 그러므로 registry 의 인증서를 build 컨테이너에서도 사용하고 싶다면, `/certs/client` 경로는 제외해야 한다.
- **그리고, docker-dind 의 인증서를 유지하며 docker-client 도 공유받을 수 있게 empty_dir 로 볼륨을 잡아줘야 한다.**
- 즉, 아래와 같이 `config.toml` 을 작성해주면 된다.

`override-values.yaml`

``` yaml
image: gitlab/gitlab-runner:latest
gitlabUrl: "https://gitlab.bellship.com"
runnerRegistrationToken: "JyJDr_4PZPYChjpj1Hsq"
hostAliases:
- ip: "10.123.123.217"
  hostnames:
  - "bellship.com"
  - "gitlab.bellship.com"
  - "minio.bellship.com"
  - "registry.bellship.com"
certsSecretName: "bellship-wildcard-tls"
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
      name = "ains-specific-runner"
      url = "https://gitlab.bellship.com"
      token = "JyJDr_4PZPYChjpj1Hsq"
      executor = "kubernetes"
      # tls-ca-file = "/home/gitlab-runner/.gitlab-runner/certs/ca.crt"
      # tls-cert-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.crt"
      # tls-key-file = "/home/gitlab-runner/.gitlab-runner/certs/tls.key"
      [runners.kubernetes]
        image = "docker:19.03.13"
        privileged = true
        [[runners.kubernetes.host_aliases]]
          ip = "10.123.123.217"
          hostnames = ["gitlab.bellship.com", "minio.bellship.com", "registry.bellship.com"]
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
```

- 새로 작성한 override-values.yaml 을 적용해 helm 배포를 다시 하자. 상황에 맞게 옵션을 바꿔주자.

``` bash
helm upgrade \
  --install gitlab-runner-specific . \
  -n cicd --create-namespace \
  --version 0.33.0 \
  -f override-values-specific.yaml
```

- 배포가 다시 정상적으로 됐는지 확인한다.
- CI job 을 다시 수행해보면, 아래와 같이 `docker info` 명령에 대해 정상적으로 도커 서버를 확인할 수 있고 로그인도 됐다.

`.gitlab-ci.yml`

``` yaml
stages:
  - build
  - test
  - deploy

default:
  image: 
    name: docker:19.03.13
  services:
    - name: docker:19.03.13-dind
      command: ["--insecure-registry=registry.bellship.com"]

variables:
  # Use TLS https://docs.gitlab.com/ee/ci/docker/using_docker_build.html#tls-enabled
  DOCKER_HOST: tcp://docker:2376
  DOCKER_TLS_CERTDIR: "/certs"
  DOCKER_TLS_VERIFY: 1
  DOCKER_CERT_PATH: "$DOCKER_TLS_CERTDIR/client"
  CONTAINER_TEST_IMAGE: $CI_REGISTRY_IMAGE:$CI_COMMIT_REF_SLUG
  CONTAINER_RELEASE_IMAGE: $CI_REGISTRY_IMAGE:latest

before_script:
  - apk add --update curl && rm -rf /var/cache/apk/*
  - ls -a $DOCKER_TLS_CERTDIR/client
  - echo $CI_REGISTRY
  # - curl -v --cacert /$DOCKER_TLS_CERTDIR/client/ca.pem https://$CI_REGISTRY
  - docker info
  - echo -n $CI_JOB_TOKEN | docker login -u gitlab-ci-token --password-stdin $CI_REGISTRY
  # - sleep 999999
  # - docker info

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

`build-job` stage 로그

``` txt
[0KRunning with gitlab-runner 14.2.0 (58ba2b95)[0;m
[0K  on gitlab-runner-specific-gitlab-runner-5cc699b787-t97xl EygxcsJz[0;m
section_start:1633606843:prepare_executor
[0K[0K[36;1mPreparing the "kubernetes" executor[0;m[0;m
[0KUsing Kubernetes namespace: cicd[0;m
[0KUsing Kubernetes executor with image docker:19.03.13 ...[0;m
[0KUsing attach strategy to execute scripts...[0;m
section_end:1633606843:prepare_executor
[0Ksection_start:1633606843:prepare_script
[0K[0K[36;1mPreparing environment[0;m[0;m
Waiting for pod cicd/runner-eygxcsjz-project-2-concurrent-0xhvdl to be running, status is Pending
Waiting for pod cicd/runner-eygxcsjz-project-2-concurrent-0xhvdl to be running, status is Pending
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
Running on runner-eygxcsjz-project-2-concurrent-0xhvdl via gitlab-runner-specific-gitlab-runner-5cc699b787-t97xl...

section_end:1633606850:prepare_script
[0Ksection_start:1633606850:get_sources
[0K[0K[36;1mGetting source from Git repository[0;m[0;m
[32;1mFetching changes with git depth set to 50...[0;m
Initialized empty Git repository in /builds/EygxcsJz/0/root/test/.git/
[32;1mCreated fresh repository.[0;m
[32;1mChecking out f216f3bb as main...[0;m

[32;1mSkipping Git submodules setup[0;m

section_end:1633606851:get_sources
[0Ksection_start:1633606851:step_script
[0K[0K[36;1mExecuting "step_script" stage of the job script[0;m[0;m
[32;1m$ apk add --update curl && rm -rf /var/cache/apk/*[0;m
fetch http://dl-cdn.alpinelinux.org/alpine/v3.12/main/x86_64/APKINDEX.tar.gz
fetch http://dl-cdn.alpinelinux.org/alpine/v3.12/community/x86_64/APKINDEX.tar.gz
(1/3) Installing nghttp2-libs (1.41.0-r0)
(2/3) Installing libcurl (7.79.1-r0)
(3/3) Installing curl (7.79.1-r0)
Executing busybox-1.31.1-r19.trigger
OK: 12 MiB in 23 packages
[32;1m$ ls -a $DOCKER_TLS_CERTDIR/client[0;m
.
..
ca.pem
cert.pem
csr.pem
key.pem
openssl.cnf
[32;1m$ echo $CI_REGISTRY[0;m
registry.bellship.com
[32;1m$ docker info[0;m
Client:
 Debug Mode: false

Server:
 Containers: 0
  Running: 0
  Paused: 0
  Stopped: 0
 Images: 0
 Server Version: 19.03.13
 Storage Driver: overlay2
  Backing Filesystem: extfs
  Supports d_type: true
  Native Overlay Diff: true
 Logging Driver: json-file
 Cgroup Driver: cgroupfs
 Plugins:
  Volume: local
  Network: bridge host ipvlan macvlan null overlay
  Log: awslogs fluentd gcplogs gelf journald json-file local logentries splunk syslog
 Swarm: inactive
 Runtimes: runc
 Default Runtime: runc
 Init Binary: docker-init
 containerd version: 8fba4e9a7d01810a393d5d25a3621dc101981175
 runc version: dc9208a3303feef5b3839f4323d9beb36df0a9dd
 init version: fec3683
 Security Options:
  apparmor
  seccomp
   Profile: default
 Kernel Version: 4.15.0-140-generic
 Operating System: Alpine Linux v3.12 (containerized)
 OSType: linux
 Architecture: x86_64
 CPUs: 6
 Total Memory: 15.63GiB
 Name: runner-eygxcsjz-project-2-concurrent-0xhvdl
 ID: 5IKN:WCK6:ACQ7:DISV:6LY5:N7UR:Q4GM:NSVG:XDFC:MVBM:TXRZ:EB6H
 Docker Root Dir: /var/lib/docker
 Debug Mode: false
 Registry: https://index.docker.io/v1/
 Labels:
 Experimental: false
 Insecure Registries:
  registry.bellship.com
  127.0.0.0/8
 Live Restore Enabled: false
 Product License: Community Engine

WARNING: No swap limit support
[32;1m$ echo -n $CI_JOB_TOKEN | docker login -u gitlab-ci-token --password-stdin $CI_REGISTRY[0;m
WARNING! Your password will be stored unencrypted in /root/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store

Login Succeeded
[32;1m$ echo "Hello, $GITLAB_USER_LOGIN!"[0;m
Hello, root!

section_end:1633606853:step_script
[0Ksection_start:1633606853:cleanup_file_variables
[0K[0K[36;1mCleaning up file based variables[0;m[0;m

section_end:1633606853:cleanup_file_variables
[0K[32;1mJob succeeded[0;m
```