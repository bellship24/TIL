**요약**

> **gitlab CI 과정에서 gitlab-registry 에 PUSH 하는 방법을 검토 및 검증해보자.**

**목차**

- [1. 배경](#1-배경)
- [2. 분석](#2-분석)
- [3. 해결](#3-해결)

**참고**

- [[Blog] Best practices for building docker images with GitLab CI](https://blog.callr.tech/building-docker-images-with-gitlab-ci-best-practices/)

---

# 1. 배경

gitlab-runner 의 k8s executor 를 사용할 때, CI 단계에서 docker build 를 한다면, 여러 가지 방법이 있겠지만 `dind` 를 사용할 수 있다. 이 경우, dind service 를 사용해서 Dockerfile 로 docker image 를 build 하게 될 것이다. 이렇게 빌드한 이미지를 저장소에 PUSH 하게 될 텐데, gitlab 을 helm 의 기본 설정으로 구축했다면, gitlab-registry 도 같이 구축됐을 것이다. 즉, 빌드한 이미지를 gitlab-registry 에 PUSH 할 수 있다. 그런데, 아래와 같은 방법으로 PUSH 를 할 때, 에러가 발생할 수 있다.

`gitlab CI/CD pipeline job 로그`

``` bash
$ docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
The push refers to repository [registry.ailab.com/root/mycicd]
5b8c72934dfc: Preparing
5b8c72934dfc: Layer already exists
errors:
denied: requested access to the resource is denied
unauthorized: authentication required
```

# 2. 분석

에러 원인은 docker push 를 할 권한이 없다는 것이다. push 권한을 갖기 위해서는 docker login 을 하면 된다.

# 3. 해결

gitlab-registry 에 docker login 할 때는 아래와 같이 gitlab 의 predefined variable 을 활용하면 된다.

``` yaml
before_script:
- echo -n $CI_JOB_TOKEN | docker login -u gitlab-ci-token --password-stdin $CI_REGISTRY
```

대략 아래와 같이 `.gitlab-ci.yml` 을 작성하면 된다.

`.gitlab-ci.yml`

``` yaml
stages:
- build

default:
  image:
    name: docker:19.03.13

variables:
  DOCKER_HOST: tcp://docker:2376
  DOCKER_TLS_CERTDIR: "/certs"
  DOCKER_TLS_VERIFY: 1
  DOCKER_CERT_PATH: "$DOCKER_TLS_CERTDIR/client"

before_script:
- echo -n $CI_JOB_TOKEN | docker login -u gitlab-ci-token --password-stdin $CI_REGISTRY

after_script:
- docker logout

build:
  stage: build
  services:
  - name: docker:19.03.13-dind
    command: ["--insecure-registry=registry.ailab.com"]
  script:
  - docker build . -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  - docker images
  - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
```

- `build.services.[0].command` 를 통해 insecure 등록이 추가됐다고 보면 된다.
- `before_script` 에 명시했다시피 gitlab-registry 에 login 한 것을 알 수 있다.

이제 해당 Job 을 실행시키면 아래와 같이 정상적으로 PUSH 된 job 의 log 를 확인할 수 있다.

![](/images/../../../images/2021-07-07-23-52-18.png)