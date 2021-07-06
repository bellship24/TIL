**요약**

- `gitlab runner k8s executor` 를 사용하면 `dind` 로 `docker build` 를 수행한다. 그런데 `FROM` 으로 `base image` 를 가져올 때 시간이 오래 걸릴 수 있으며 빈번한 작업이므로 시간을 단축시킬 필요가 있다. `docker build` 의 시간을 줄이기 위해서는 여러 가지 방법이 존재한다. docker layer 의 cache 를 사용할 수 있다. 또는 cache 저장소로 aws s3 같은 퍼블릭 클라우드 스토리지나 s3 compatible 인 minio 를 대신해서 쓸 수 있다. 이런 방법들을 검토하고 검증해보자.

**목차**

- [1. gitlab runner k8s executor 의 dind 에서 `docker build` 속도를 줄일 수 있는 방법](#1-gitlab-runner-k8s-executor-의-dind-에서-docker-build-속도를-줄일-수-있는-방법)
- [2. docker layer caching 를 사용하는 방법](#2-docker-layer-caching-를-사용하는-방법)
  - [2.1. Docker 캐싱 작동 방식](#21-docker-캐싱-작동-방식)
  - [2.2. Docker 캐싱 예제](#22-docker-캐싱-예제)
- [3. distributed cache 를 사용하여 job 에서 image 관련 속도 줄이기](#3-distributed-cache-를-사용하여-job-에서-image-관련-속도-줄이기)
  - [distributed cache 서버로 minio 를 사용하기](#distributed-cache-서버로-minio-를-사용하기)

**참조**

- [[gitlab docs] Use a distributed cache](https://docs.gitlab.com/runner/configuration/speed_up_job_execution.html#use-a-distributed-cache)
- [[gitlab docs] Make Docker-in-Docker builds faster with Docker layer caching](https://docs.gitlab.com/ee/ci/docker/using_docker_build.html#make-docker-in-docker-builds-faster-with-docker-layer-caching)
- [[gitlab docs] Use a distributed cache](https://docs.gitlab.com/runner/configuration/speed_up_job_execution.html#use-a-distributed-cache)

# 1. gitlab runner k8s executor 의 dind 에서 `docker build` 속도를 줄일 수 있는 방법

1. docker layer caching 사용하여 dind build 를 빠르게 하기
2. distributed cache 사용하기
3. 매번 파드에 대해 docker dind 를 service 로 생성하지 않고 하나의 docker dind 컨테이너를 만들어 사용

# 2. docker layer caching 를 사용하는 방법

- dind 를 사용할 때 도커는 빌드를 생성할 때마다 이미지의 모든 레이어를 다운로드한다. Docker v1.13+ 는 빌드 단계에서 기존 이미지를 캐시로 사용할 수 있다. 이렇게 하면 빌드 프로세스가 상당히 빨라진다.
- 즉, 이 방법은 dockerfile 에 명시된 작업이 오래걸리고 반복적으로 수행할 때 적합한 방법이다.

## 2.1. Docker 캐싱 작동 방식

- `docker build` 를 실행할 때 Dockerfile 의 각 명령은 레이어를 생성한다. 이러한 레이어는 캐시로 보관되며 변경 사항이 없는 경우 재사용 할 수 있다. 한 레이어에서 변경이 생기면 그 아래 모든 후속 레이어는 다시 생성되어야 한다.
- `docker build` 커맨드에서 `--cache-from` 인수를 사용하여 캐시의 소스로 사용할 태그된 이미지를 명시할 수 있다. 여러 `--cache-from` 인수를 사용하여 여러 이미지를 캐시 소스로 지정할 수 있다. `--cache-from` 인수와 함께 사용되는 모든 이미지는 캐시의 소스로 사용되기 전에 먼저 `docker pull` 로 가져와야한다.

## 2.2. Docker 캐싱 예제

- 다음은 Docker 캐싱을 사용하는 방법을 보여주는 `.gitlab-ci.yml` 파일이다.

``` yaml
image: docker:19.03.12

services:
  - docker:19.03.12-dind

variables:
  # Use TLS https://docs.gitlab.com/ee/ci/docker/using_docker_build.html#tls-enabled
  DOCKER_HOST: tcp://docker:2376
  DOCKER_TLS_CERTDIR: "/certs"

before_script:
  - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY

build:
  stage: build
  script:
    - docker pull $CI_REGISTRY_IMAGE:latest || true
    - docker build --cache-from $CI_REGISTRY_IMAGE:latest --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA --tag $CI_REGISTRY_IMAGE:latest .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker push $CI_REGISTRY_IMAGE:latest
```

- `build` stage 의 `script` 섹션에서 첫 번째 명령은 `docker build` 명령의 캐시로 사용할 수 있도록 레지스트리에서 이미지를 가져오려고 한다. 두 번째 명령은 가져온 이미지를 캐시로 사용하여 Docker 이미지를 빌드하고 (`--cache-from $CI_REGISTRY_IMAGE:latest` 인수를 참조) 정상적으로 수행 됐다면, 태그를 지정한다. 마지막 두 명령은 태그가 지정된 Docker 이미지를 컨테이너 레지스트리로 푸시하여 후속 빌드의 캐시로 사용할 수도 있다.

# 3. distributed cache 를 사용하여 job 에서 image 관련 속도 줄이기

- distributed cache 를 사용하여 language dependencies 를 다운로드 하는데 걸리는 시간을 단축 할 수 있다.
- distributed cache 를 지정하려면 캐시 서버를 설정 한 다음, 해당 [캐시 서버를 사용하도록 runner 를 설정](https://docs.gitlab.com/runner/configuration/advanced-configuration.html#the-runnerscache-section)하면 된다.
- autoscaling 을 사용하는 경우, [distributed runner cache feature](https://docs.gitlab.com/runner/configuration/autoscale.html#distributed-runners-caching) 에 대해 검토하자.
- aws s3, minio, google cloud storage, azure blob storage 등을 캐시 서버로 활용한다.

## distributed cache 서버로 minio 를 사용하기

- AWS S3 를 사용하는 대신 자체 캐시 스토리지 minio 를 생성할 수 있다.

1. 캐시 서버가 실행될 전용 머신에 로그인한다.
2. Docker Engine 이 해당 머신에 설치되어 있는지 확인해라.
3. Go 로 작성된 간단한 S3-compatible 서버 MinIO 를 시작하자.

    ``` bash
    docker run -d --restart always -p 9005:9000 \
            -v /.minio:/root/.minio -v /export:/export \
            -e "MINIO_ROOT_USER=<minio_root_username>" \
            -e "MINIO_ROOT_PASSWORD=<minio_root_password>" \
            --name minio \
            minio/minio:latest server /export
    ```

    - 포트를 9005 로 수정하여 다른 포트에서 캐시 서버를 노출 할 수도 있다.

4. 서버의 IP 주소를 확인해라.

    ``` bash
    hostname --ip-address
    ```

5. 생성한 캐시 서버는 `MY_CACHE_IP:9005` 로 사용할 수 있다.
6. 러너가 사용할 버킷을 만들자. 아래에 경우 runner 는 버킷의 이름이다. 버킷 이름을 바꿔도 된다. 모든 캐시는 `/export` 디렉토리에 저장된다.

    ``` bash
    sudo mkdir /export/runner
    ```

7. 러너를 구성할 때 Access 및 Secret Keys 로 `MINIO_ROOT_USER` 및 `MINIO_ROOT_PASSWORD` 값을 사용하자.
8. 이제 [캐시서버를 runner 의 `config.toml` 에 추가](https://docs.gitlab.com/runner/configuration/autoscale.html#distributed-runners-caching)하자.
