**목차**

- [요약](#요약)
- [전제](#전제)
- [서비스 구성도](#서비스-구성도)
- [GitLab CI](#gitlab-ci)
  - [CI git repo. 구조](#ci-git-repo-구조)
  - [`Dockerfile`](#dockerfile)
  - [`.gitlab-ci.yml`](#gitlab-ciyml)
- [Argo CD](#argo-cd)

**참고**

---

# 요약

> 토이프로젝트로써 k8s 기반 GitOps CI/CD 를 구현하기 위해, GitLab CI 와 Argo CD 로 torchserve 서빙을 해보자.

# 전제

아래에 사용한 기술 스택들의 설치는 생략한다.

사용한 기술 스택 및 버전

- k8s v1.21.1 (by kubeadm)
- gitLab v13.12.4 (helm chart v4.12.4
- gitlab-runner v13.12.0 (helm chart v0.29.0)
- cert-manager v1.4.0 (helm chart v1.4.0)
- ingress-nginx v0.47.0 (helm chart 3.33.0)
- helm v3.6.1
- metallb v0.10.2 (helm chart v0.10.2)
- nfs-provisioner v4.0.2 (helm chart v4.0.11)
- argocd v2.0.4 (helm chart v3.10.0)

# 서비스 구성도

![](./torchserve%20gitops%20CICD%20서비스%20구성도.png)

# GitLab CI

## CI git repo. 구조

아래에 CI git repo. 의 구조에서 각 요소들에 대해 간략히 설명하고 추후에 하나씩 자세히 설명할 것이다.

``` bash
.
├── Dockerfile
└── mytorch
    ├── densenet161-8d451a50.pth
    ├── index_to_name.json
    └── model.py
```

- `Dockerfile` : 이 토이프로젝트에서 CI/CD 를 적용할 대상인 도커 이미지의 빌드를 어떻게 할 지에 대한 파일이다.
- `mytorch` : TorchServe 서버에 올릴 내 모델 데이터들을 저장하는 폴더.
- `densenet161-8d451a50.pth` : 학습된 모델 정보 파일.
- `index_to_name.json` : 기타 모델 관련 파일.
- `model.py` : 모델 정의 파일

## `Dockerfile`

``` dockerfile
FROM pytorch/torchserve:latest
# FROM $CI_REGISTRY_IMAGE:$CI_COMMIT_BEFORE_SHA

COPY mytorch/ /home/model-server/mydata/

WORKDIR /home/model-server/mydata

RUN torch-model-archiver \
  --model-name densenet161 \
  --version 1.0 \
  --model-file model.py \
  --serialized-file densenet161-8d451a50.pth \
  --export-path  /home/model-server/model-store \
  --extra-files index_to_name.json \
  --handler image_classifier 

```

- 기본적으로 이 프로젝트에서 GitLab CI 를 활용해 도커 이미지 빌드에 대한 Continuous Integration 을 수행한다. 이 도커 이미지를 어떻게 빌드할 것인가에 대한 설정이 이 `Dockerfile` 에 담겨있다.
- `pytorch/torchserve` 이미지를 base image 로 사용한다. 이전 commit 때 gitlab registry 로 PUSH 된 이미지를 사용하고 싶다면, 2번 째 라인에 주석된 부분을 사용하자.
- git repo. 에 있는 mytorch 경로에 `model.py`, `pth` 파일 등이 있다. 이 파일들로 `MAR` 파일을 만들어야 하므로 `COPY` 명령어로 추가하자.
- 다음 layer 에서 `torch-model-archiver` 실행 시, 경로를 편하게 설정하기 위해 `WORKDIR` 을 설정했다.
- `torch-model-archiver` 를 통해 MAR 파일을 빌드하여 `/home/model-server/model-store` 에 저장하는 도커 이미지를 생성한다.
- 즉, 모델 서빙으로 바로 사용할 수 있는 이미지를 만든 것이다.

## `.gitlab-ci.yml`

``` yaml
stages:
- build
- test

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
  - docker images
  # - curl -X GET https://$CI_REGISTRY/v2/_catalog
  - docker pull $CI_REGISTRY_IMAGE:latest || true
  # - docker pull $CI_REGISTRY_IMAGE:$CI_COMMIT_BEFORE_SHA || true
  - >
    docker build .
    --cache-from $CI_REGISTRY_IMAGE:latest 
    --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA 
    --tag $CI_REGISTRY_IMAGE:latest
  - docker images
  - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  - docker push $CI_REGISTRY_IMAGE:latest

test:
  stage: test
  services:
  - name: docker:19.03.13-dind
  script:
  - docker run --rm -it $CI_REGISTRY_IMAGE:latest -p 8080:8080 -p 8081:8081 -p 8082:8082 -p 7070:7070 -p 7071:7071 /bin/bash -c curl http://localhost:8080/ping
```

build 스테이지

- 기본 이미지는 docker client 로 한다.
- docker client 가 호출할 도커 서버의 경로와 TLS 인증 절차를 추가한다.
- before_script 를 통해 service 로 올릴 도커 서버에 로그인한다.
- 물론 after_script 를 통해 job 이 끝나면 docker logout 을 한다.
- build 잡에서 사용할 도커 서버를 `services` 로 명시한다. gitlab registry 에 대한 self-signed 인증서 문제가 있어서 insecure 설정을 했다.
- gitlab 예약어를 통해 작성한 Dockerfile 로 도커 빌드를 하여 gitlab registry 에 PUSH 한다.
- build 가 마무리 되고 gitlab registry 에 올라간 이미지는 gitlab 웹 UI 에서도 확인할 수 있다.

test 스테이지

# Argo CD