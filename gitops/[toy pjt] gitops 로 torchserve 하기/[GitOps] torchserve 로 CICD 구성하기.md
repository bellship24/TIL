**요약**

- k8s 기반 torchserve 를 서빙하는데 k8s 기반 gitlab CICD 를 통해 구현해보자.

**목차**

- [`Dockerfile`](#dockerfile)
- [`.gitlab-ci.yml`](#gitlab-ciyml)
- [build](#build)

**참고**

---

# `Dockerfile`

``` dockerfile
FROM pytorch/torchserve:latest

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

- torchserve 이미지를 base image 로 사용한다.
- git repo. 에 있는 mytorch 경로에 `model.py`, `pth` 파일 등이 있다. 이 파일들로 `MAR` 파일을 만들어야 하므로 `COPY` 명령어로 추가하자.
- 다음 layer 에서 `torch-model-archiver` 실행 시, 경로를 편하게 설정하기 위하여 `WORKDIR` 를 설정했다.
- `torch-model-archiver` 를 통해 MAR 파일을 빌드하여 `/home/model-server/model-store` 에 저장하는 도커 이미지를 생성한다.
- 즉, 모델 서빙으로 바로 사용할 수 있는 이미지를 만든 것이다.

# `.gitlab-ci.yml`

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
  - >
    docker build .
    -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    --cache-from $CI_REGISTRY_IMAGE:latest
  - docker images
  - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHAㅌ
```

# build

- 기본 이미지는 docker client 로 한다.
- docker client 가 호출할 도커 서버의 경로와 TLS 인증 절차를 추가한다.
- before_script 를 통해 service 로 올릴 도커 서버에 로그인한다.
- 물론 after_script 를 통해 job 이 끝나면 docker logout 을 한다.
- build 잡에서 사용할 도커 서버를 `services` 로 명시한다. gitlab registry 에 대한 self-signed 인증서 문제가 있어서 insecure 설정을 했다.
- gitlab 예약어를 통해 작성한 Dockerfile 로 도커 빌드를 하여 gitlab registry 에 PUSH 한다.
- build 가 마무리 되고 gitlab registry 에 올라간 이미지는 gitlab 웹 UI 에서도 확인할 수 있다.

  ![](/.uploads/../../../.uploads/2021-07-08-23-57-52.png)