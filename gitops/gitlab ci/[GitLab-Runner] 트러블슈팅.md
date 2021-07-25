**목차**

- [1. 요약](#1-요약)
- [2. CI 에서 docker build 할 때, dockerfile 에서 gitlab predefined variable 사용하는 방법](#2-ci-에서-docker-build-할-때-dockerfile-에서-gitlab-predefined-variable-사용하는-방법)
  - [2.1. 현상](#21-현상)
  - [2.2. 분석](#22-분석)
  - [2.3. 해결](#23-해결)
  - [2.4. 기타](#24-기타)
    - [2.4.1. 실패한 해결 `--build-arg` 방법](#241-실패한-해결---build-arg-방법)

**참고**

---

# 1. 요약

> gitlab-runner 를 사용하면서 생긴 이슈들에 대한 트러블슈팅 내용을 정리했다. 대체로 `현상 -> 분석 -> 해결` 순으로 진행했다.

# 2. CI 에서 docker build 할 때, dockerfile 에서 gitlab predefined variable 사용하는 방법

## 2.1. 현상

`Dockerfile`

``` dockerfile
FROM $CI_REGISTRY_IMAGE:$CI_COMMIT_BEFORE_SHA
...
```

`.gitlab-ci.yml`

``` bash
...
build:
  stage: build
  services:
  - name: docker:19.03.13-dind
    command: ["--insecure-registry=registry.mylab.com"]
  script:
  - >
    docker build .
    --cache-from $CI_REGISTRY_IMAGE:latest
    --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
...
```

- 위에 dockerfile 의 의도는 다음과 같다. base image 이름에 gitlab predefined variable 인 `$CI_REGISTRY_IMAGE` 와 `$CI_COMMIT_BEFORE_SHA` 를 넣어 해당 CI git repo. 에 대한 gitlab registry 안에 이미지 이름과 이전 커밋 SHA 로 정의된 tag 를 사용하여 편리성을 높였다.
- .gitlab-ci.yml 에서도 빌드한 도커 이미지의 tag 를 `$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA` 로써 이 역시도 gitlab redefined variable 을 사용하여 편리성을 높였다.
- 하지만, CI 파이프라인을 돌렸을 때 아래와 같은 에러가 출력된다.

``` bash
...
[32;1m$ docker build . --cache-from $CI_REGISTRY_IMAGE:latest --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA[0;m
Sending build context to Docker daemon  226.6MB

Step 1/4 : FROM $CI_REGISTRY_IMAGE:$CI_COMMIT_BEFORE_SHA
invalid reference format
section_end:1627233650:step_script
[0Ksection_start:1627233650:after_script
[0K[0K[36;1mRunning after_script[0;m
[0;m[32;1mRunning after script...[0;m
[32;1m$ docker logout[0;m
Removing login credentials for https://index.docker.io/v1/
section_end:1627233650:after_script
[0Ksection_start:1627233650:cleanup_file_variables
[0K[0K[36;1mCleaning up file based variables[0;m
[0;msection_end:1627233650:cleanup_file_variables
[0K[31;1mERROR: Job failed: command terminated with exit code 1
[0;m
```

## 2.2. 분석

- gitlab runner 의 predefined variable 은 bash 에서 잘 조회되지만 dockerfile 안에서는 사용 불가능한 것으로 보인다. 즉, dockerfile 첫번째 줄인 `FROM $CI_REGISTRY_IMAGE:$CI_COMMIT_BEFORE_SHA` 에서 각 변수들은 정의되어 있지 않은 상태로 보인다.

## 2.3. 해결

- `docker build` 할 때, `--build-arg` 를 통해 `ARG` 를 덮어쓸 수 있다. 그러면, 아래와 같이 `.gitlab-ci.yml` 이 완성된다.
- 여기서는 `$CI_REGISTRY_TAG` 에 대해 이전 커밋 SHA 를 태그로 사용도 가능하다. 이 부분은 주석 처리 했다. 즉, 주석 처리를 하면 dockerfile 의 ARG 를 따르고 --build-arg 를 쓰면 ARG 를 override 할 수 있다.

``` yaml
...
build:
  stage: build
  services:
  - name: docker:19.03.13-dind
    command: ["--insecure-registry=registry.mylab.com"]
  script:
  - >
    docker build .
    --cache-from $CI_REGISTRY_IMAGE:latest
    --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    --build-arg CI_REGISTRY_IMAGE=$CI_REGISTRY_IMAGE
  # --build-arg CI_REGISTRY_TAG=$CI_COMMIT_BEFORE_SHA
...
```

- 단, 덮어쓸 `ARG` 를 꼭 명시해줘야 한다. 그러면, 아래와 같이 `Dockerfile` 이 완성된다.

``` dockerfile
ARG CI_REGISTRY_IMAGE=pytorch/torchserve:latest
ARG CI_REGISTRY_TAG=base

FROM $CI_REGISTRY_IMAGE:$CI_REGISTRY_TAG

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

- 이제, git repo. 를 커밋해서 실행되는 파이프라인의 로그는 아래와 같이 dockerfile 에서 gitlab predefined variable 을 정상적으로 사용했다.

``` bash
[32;1m$ docker images[0;m
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
[32;1m$ docker build . --cache-from $CI_REGISTRY_IMAGE:latest --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA --build-arg CI_REGISTRY_IMAGE=$CI_REGISTRY_IMAGE[0;m
Sending build context to Docker daemon  226.6MB

Step 1/6 : ARG CI_REGISTRY_IMAGE=pytorch/torchserve:latest
Step 2/6 : ARG CI_REGISTRY_TAG=base
Step 3/6 : FROM $CI_REGISTRY_IMAGE:$CI_REGISTRY_TAG
base: Pulling from root/mycicd
e7ae86ffe2df: Pulling fs layer
0b2fe2118c34: Pulling fs layer
2bea50abcc80: Pulling fs layer
910e6e0ca555: Pulling fs layer
a3d6a01785b9: Pulling fs layer
4f4fb700ef54: Pulling fs layer
910e6e0ca555: Waiting
a3d6a01785b9: Waiting
4f4fb700ef54: Waiting
2bea50abcc80: Verifying Checksum
2bea50abcc80: Download complete
910e6e0ca555: Verifying Checksum
910e6e0ca555: Download complete
e7ae86ffe2df: Verifying Checksum
e7ae86ffe2df: Download complete
4f4fb700ef54: Verifying Checksum
4f4fb700ef54: Download complete
e7ae86ffe2df: Pull complete
0b2fe2118c34: Verifying Checksum
0b2fe2118c34: Download complete
a3d6a01785b9: Download complete
0b2fe2118c34: Pull complete
2bea50abcc80: Pull complete
910e6e0ca555: Pull complete
a3d6a01785b9: Pull complete
4f4fb700ef54: Pull complete
Digest: sha256:1793abc1987d246842e6e4c428ac66ff00ed2d018943132fbe4bb1870f5b3590
Status: Downloaded newer image for registry.mylab.com/root/mycicd:base
 ---> aafcbaecf9ea
Step 4/6 : COPY mytorch/ /home/model-server/mydata/
 ---> 8018d1083594
Step 5/6 : WORKDIR /home/model-server/mydata
 ---> Running in 9f25d4d469d1
Removing intermediate container 9f25d4d469d1
 ---> 0cf99a4ba419
Step 6/6 : RUN torch-model-archiver   --model-name densenet161   --version 1.0   --model-file model.py   --serialized-file densenet161-8d451a50.pth   --export-path  /home/model-server/model-store   --extra-files index_to_name.json   --handler image_classifier
 ---> Running in 6bcad0f00196
Removing intermediate container 6bcad0f00196
 ---> 2401d18fcf42
Successfully built 2401d18fcf42
Successfully tagged registry.mylab.com/root/mycicd:4100a213805e5b40dd9356ba3d92e95790ef381e
[32;1m$ docker images[0;m
REPOSITORY                       TAG                                        IMAGE ID            CREATED             SIZE
registry.mylab.com/root/mycicd   4100a213805e5b40dd9356ba3d92e95790ef381e   2401d18fcf42        1 second ago        3.2GB
registry.mylab.com/root/mycicd   base                                       aafcbaecf9ea        3 days ago          2.86GB
```

- 위에 보면 `dockerfile` 에 따라 Step 1/6 에서 `pytorch/torchserve:latest` 를 쓰는 것 처럼 보이지만 실제로는 base 이미지가 `.gitlab-ci.yml` 에서 `docker build` 명령어 부분에서 `--build-arg` 로 명시한 `CI_REGISTRY_IMAGE` 를 쓴 것을 알 수 있다.

## 2.4. 기타

### 2.4.1. 실패한 해결 `--build-arg` 방법

- dockerfile 안에서 bash 의 환경 변수를 인식 못하기 때문에 `docker build` 명령어를 수행할 때 `--build-arg` 로 해당 변수를 환경 변수에서 dockerfile 안에 변수로 전달해주면 해결될 것으로 생각했다.
- 그래서 dockerfile 은 아래와 같이 그대로이다.

``` dockerfile
FROM $CI_REGISTRY_IMAGE:$CI_COMMIT_BEFORE_SHA
...
```

- .gitlab-ci.yml 에는 docker build 부분에 `--build-arg` 를 아래와 같이 추가했다.

``` yaml
...
build:
  stage: build
  services:
  - name: docker:19.03.13-dind
    command: ["--insecure-registry=registry.mylab.com"]
  script:
  - >
    docker build .
    --cache-from $CI_REGISTRY_IMAGE:latest
    --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    --build-arg CI_REGISTRY_IMAGE=$CI_REGISTRY_IMAGE
    --build-arg CI_COMMIT_BEFORE_SHA=$CI_COMMIT_BEFORE_SHA
...
```

- 하지만, 아래와 같이 에러 로그가 발생했고 `--build-arg` 만 사용해서는 효과가 없는 것으로 보인다.

``` bash
...
[32;1m$ docker build . --cache-from $CI_REGISTRY_IMAGE:latest --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA --build-arg CI_REGISTRY_IMAGE=$CI_REGISTRY_IMAGE --build-arg CI_COMMIT_BEFORE_SHA=$CI_COMMIT_BEFORE_SHA[0;m
Sending build context to Docker daemon  226.6MB

Step 1/4 : FROM $CI_REGISTRY_IMAGE:$CI_COMMIT_BEFORE_SHA
invalid reference format
section_end:1627233952:step_script
[0Ksection_start:1627233952:after_script
[0K[0K[36;1mRunning after_script[0;m
[0;m[32;1mRunning after script...[0;m
[32;1m$ docker logout[0;m
Removing login credentials for https://index.docker.io/v1/
section_end:1627233953:after_script
[0Ksection_start:1627233953:cleanup_file_variables
[0K[0K[36;1mCleaning up file based variables[0;m
[0;msection_end:1627233953:cleanup_file_variables
[0K[31;1mERROR: Job failed: command terminated with exit code 1
[0;m
```