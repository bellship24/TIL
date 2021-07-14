**요약**

- CI 를 k8s 기반으로 구성했는데 executor 가 docker 명령어를 인식하지 못하여 docker build 를 못하는 상황이다. docker 명령어를 사용하기 위해서는 DinD 를 쓸 수 있을 것 같아서 이에 대해 알아보고 간단히 컨테이너 안에서 docker 명령어 사용 방법을 검증하려 한다.

**목차**

- [1. DinD 란?](#1-dind-란)
- [2. DinD 로 도커 데몬 실행하는 방법](#2-dind-로-도커-데몬-실행하는-방법)
  - [2.1. 도커 데몬 컨테이너 실행](#21-도커-데몬-컨테이너-실행)
  - [2.2. 도커 클라이언트 컨테이너를 실행하여 도커의 버전 확인 및 테스트용 alpine 컨테이너 실행](#22-도커-클라이언트-컨테이너를-실행하여-도커의-버전-확인-및-테스트용-alpine-컨테이너-실행)
- [3. DooD 란?](#3-dood-란)
- [4. DooD 로 도커 데몬 실행하는 방법](#4-dood-로-도커-데몬-실행하는-방법)
- [5. 기타](#5-기타)

**참고**

- [[Blog] DinD, DooD란?](https://mns010.tistory.com/25)

---

# 1. DinD 란?

![](/.uploads/2021-06-27-03-15-03.png)

- DinD 는 Docker in Docker 의 약어로, 도커 컨테이너 내부에 호스트 도커 데몬과는 별개의 새로운 도커 데몬을 실행시키는 것이다.
- 새로 실행된 도커 데몬 컨테이너에 새로운 도커 클라이언트 컨테이너를 이용하여 명령을 내리는 것이 가능하다.
- 새로 생성된 도커 환경은 컨테이너 환경이기 때문에, 테스트 후에 정리가 간편하다. 하지만 설정 방법은 약간 불편하다.
- DinD 를 이용하기 위해서는 새로운 도커 데몬 컨테이너를 실행시킬 때, `--privileged` 옵션을 이용할 필요가 있다. 이 옵션은 컨테이너가 호스트 머신의 많은 권한을 얻을 수 있게 해준다. 하지만, 이 방법은 보안상 아주 좋지 않은 환경을 만든다. 예를 들면, 거의 대부분의 장치들에 접근 가능하여 호스트의 `/boot` 를 컨테이너에서 마운트하여 initramfs 나 부트로더를 수정 및 삭제 등 작업을 할 수도 있다.

# 2. DinD 로 도커 데몬 실행하는 방법

## 2.1. 도커 데몬 컨테이너 실행

``` bash
$ docker run --privileged -ti -d --name docker-daemon-con -e DOCKER_TLS_CERTDIR="" docker:dind
$ docker ps | grep docker-daemon-con
7130d4e7dd75   docker:dind              "dockerd-entrypoint.…"   17 seconds ago   Up 15 seconds   2375-2376/tcp   docker-daemon-con
```

- 호스트 도커 클라이언트를 사용해 docker:dind 이미지로 privileged 된 도커 데몬 컨테이너를 실행한다.

## 2.2. 도커 클라이언트 컨테이너를 실행하여 도커의 버전 확인 및 테스트용 alpine 컨테이너 실행

``` bash
$ docker run --rm --link docker-daemon-con:docker docker version
$ docker run --rm --link docker-daemon-con:docker docker run -ti -d --name alpine-by-host alpine
$ docker run --rm --link docker-daemon-con:docker docker docker ps -a
CONTAINER ID   IMAGE     COMMAND     CREATED         STATUS        PORTS     NAMES
2a84bedd3c42   alpine    "/bin/sh"   2 seconds ago   Up 1 second             alpine-by-host
```

- 호스트 도커 클라이언트를 사용해 docker:latest 이미지로 도커 클라이언트 컨테이너를 만들어 도커 데몬 컨테이너에 접근 및 명령어를 실행하여 도커 데몬 컨테이너의 도커 서버에서 각종 작업을 한다.
- link 옵션은 같은 호스트 내에 컨테이너 간 네트워크 연결을 할 때 사용한다. 구체적으로는 private ip 를 사용하는 각 컨테이너 간에 /etc/hosts 를 설정해줘서 호스트이름으로 통신할 수 있게 해준다. 아무튼 여기서는 link 옵션을 통해 도커 데몬 컨테이너와 도커 클라이언트 컨테이너 간에 통신을 하고자 함이다.
- 즉, 다시 말해, 위에 docker 명령어는 아래와 같이 사용할 수도 있다. 호스트 도커 클라이언트를 사용해 도커 데몬 컨테이너를 실행하여 그 안에 있는 도커 클라이언트를 사용하는 것과 같은 효과이다.

``` bash
$ docker exec -ti docker-daemon-con docker version
$ docker exec -ti docker-daemon-con docker run -tid --name alpine-by-exec alpine
$ docker exec -ti docker-daemon-con docker ps -a
CONTAINER ID   IMAGE     COMMAND     CREATED              STATUS              PORTS     NAMES
91ef47eb80ec   alpine    "/bin/sh"   3 seconds ago        Up 2 seconds                  alpine-by-exec
2a84bedd3c42   alpine    "/bin/sh"   About a minute ago   Up About a minute             alpine-by-host
```

# 3. DooD 란?

![](/.uploads/2021-06-27-15-34-00.png)

- DooD 는 Docker Out Of Docker 의 약어로 호스트 도커 데몬이 사용하는 소켓을 공유하여 도커 클라이언트 컨테이너에서 컨테이너를 실행시키는 것이다. 새로 실행시킨 컨테이너는 도커 클라이언트 컨테이너와 sibling 관계를 가진다. 따라서 테스트 환경이 도커 호스트 환경과 일치하는 것을 알 수 있다. 예를 들어, 호스트와 도커 이미지를 공유한다든지 등등이 있다.

# 4. DooD 로 도커 데몬 실행하는 방법

- 실행 방법은 간단하다. 아래와 같이 호스트의 도커 소켓을 컨테이너에 마운트 시켜주면 된다.

``` bash
$ docker run -tid -v /var/run/docker.sock:/var/run/docker.sock docker

### DooD 도커 데몬 컨테이너 안에서 host docker 환경을 조회할 수 있다.
$ docker exec -ti 9ac2 docker ps
CONTAINER ID   IMAGE                    COMMAND                  CREATED              STATUS              PORTS     NAMES
9ac2cd4c9eb9   docker                   "docker-entrypoint.s…"   About a minute ago   Up About a minute             vigilant_bell
80448e5de526   6600fae04efd             "/docker-entrypoint.…"   21 minutes ago       Up 21 minutes                 k8s_haproxy_haproxy-dtlab-dev-k8s-pjb-1_kube-system_c7bbe5fc89460b243e15e3913490c346_0
56b3a3f9848a   metallb/speaker          "/speaker --port=747…"   9 days ago           Up 9 days                     k8s_speaker_speaker-92ddk_metallb-system_7e70a724-7d74-401a-9981-746077f2d5e9_0
```

- 이 방법은 도커 클라이언트 컨테이너에 `--privileged` 옵션을 주지 않았기 때문에 DinD 보다 안전하다. 하지만, 컨테이너의 격리에 문제가 조금 있다. 예를 들면 아래와 같다.
    1. 컨테이너 이름 충돌: 도커 클라이언트 컨테이너에서 컨테이너 실행 명령을 내릴 때, 컨테이너의 이름이 호스트에서 실행한 컨테이너의 이름과 겹치면 실행이 불가능하다.
    2. 마운트 경로: 도커 클라이언트 컨테이너에서 새로운 컨테이너를 bind mount 를 이용하여 만들 때, 호스트의 파일 경로를 기준으로 해야한다.
    3. 포트 맵핑: 포트 맵핑 또한 호스트 레벨에서 하기 때문에, 포트가 충돌이 일어날 가능성이 있다.

- 하지만, DinD 보다는 DooD 를 권장한다.

# 5. 기타

- DinD 와 DooD 는 반대되는 용어이다.