**요약**

- docker container 기반 torchserve 를 검토하고 CPU 기반으로 검증해보자.
- 순서 : 전제 조건 -> torchserve 도커 이미지 생성 -> 생성한 torchserve 테스트 -> 컨테이너 기반 mar 생성 -> 도커 기반 torchserve 생성

**목차**

- [1. 순서](#1-순서)
- [2. 전제](#2-전제)
- [3. TorchServe 도커 이미지 생성](#3-torchserve-도커-이미지-생성)
  - [3.1. production 환경 기반 이미지 생성](#31-production-환경-기반-이미지-생성)
  - [3.2. developer 환경 이미지 생성](#32-developer-환경-이미지-생성)
  - [3.3. codebuild 환경 기반 이미지 생성](#33-codebuild-환경-기반-이미지-생성)
- [4. 생성한 TorchServe 이미지로 컨테이너 실행 테스트](#4-생성한-torchserve-이미지로-컨테이너-실행-테스트)
- [5. container 를 띄워서 torch-model-archiver 생성](#5-container-를-띄워서-torch-model-archiver-생성)
- [6. 운영 환경에서 docker 기반 torchserve 생성](#6-운영-환경에서-docker-기반-torchserve-생성)
  - [6.1. shared memory size](#61-shared-memory-size)
  - [6.2. system resource 에 대한 사용자 제한](#62-system-resource-에-대한-사용자-제한)
  - [6.3. 특정 포트와 볼륨을 노출 시키기](#63-특정-포트와-볼륨을-노출-시키기)
  - [6.4. torchserve 를 docker 기반으로 실행](#64-torchserve-를-docker-기반으로-실행)
  - [6.5. torchserve 로 curl 추론 테스트](#65-torchserve-로-curl-추론-테스트)
- [7. lesson learned](#7-lesson-learned)

**참조**

- [[TorchServe Docs] docker](https://github.com/pytorch/serve/blob/master/docker/README.md)

---

# 1. 순서

- torchserve on docker 에 대한 순서도를 아래와 같이 그려봤다.

![](images/[torchserve]%20torchserve%20on%20docker.png)

# 2. 전제

- docker
- git
- GPU 를 사용하는 ubuntu 환경은 아래에 nvidia container toolkit 과 nvidia driver 를 설치하자.
  - [[nvidia-docker2] install nvidia container toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html#installing-on-ubuntu-and-debian)
  - [[nvidia-driver] install nvidia-driver](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/install-nvidia-driver.html)
- 먼저, TorchServe GitHub 소스코드가 없다면 clone 하자.

``` bash
git clone https://github.com/pytorch/serve.git
cd serve/docker
```

# 3. TorchServe 도커 이미지 생성

- `build_image.sh` 스크립트를 사용해서 docker image 를 빌드하자.
- 이 이미지는 `production`, `dev`, `codebuild` 라는 docker image 들을 빌드해준다.
- 인자들은 아래와 같다.

| Parameter          | Desciption                                                                                |
|--------------------|-------------------------------------------------------------------------------------------|
| -h, --help         | Show script help                                                                          |
| -b, --branch_name  | Specify a branch name to use. Default: master                                             |
| -g, --gpu          | Build image with GPU based ubuntu base image                                              |
| -bt, --buildtype   | Which type of docker image to build. Can be one of : production, dev, codebuild           |
| -t, --tag          | Tag name for image. If not specified, script uses torchserve default tag names.           |
| -cv, --cudaversion | Specify to cuda version to use. Supported values cu92, cu101, cu102, cu111. Default cu102 |
| --codebuild        | Set if you need AWS CodeBuild                                                             |

## 3.1. production 환경 기반 이미지 생성

- buildtype 인자에 production 인수를 사용하면, 공개적으로 사용 가능한 torchserve 및 torch-model-archiver 바이너리로 Docker 이미지를 만든다.
- CPU 기반 이미지 생성

``` bash
./build_image.sh
```

- GPU 기반 이미지 생성 (아래는 cuda10.2 일 때. 그 외에 cu92, cu101, cu102, cu111 옵션)

``` bash
./build_image.sh -g -cv cu102
```

- custom tag 가 있는 이미지 생성

``` bash
./build_image.sh -t torchserve:1.0
```

## 3.2. developer 환경 이미지 생성

- buildtype 인자에 developer 인수를 사용하면, 소스 기반으로 torchserve 및 torch-model-archiver 가 Docker 이미지를 만든다.

- CPU 기반 이미지 생성

```bash
./build_image.sh -bt dev
```

- 다른 브랜치로 CPU 기반 이미지 생성

```bash
./build_image.sh -bt dev -b my_branch
```

- CUDA 버전에 따른 GPU 기반 이미지 생성

```bash
./build_image.sh -bt dev -g -cv <cu111, cu102, cu101, cu92>
```

- 다른 브랜치로 GPU 기반 이미지 생성

```bash
./build_image.sh -bt dev -g -cv cu111 -b my_branch
```

- custom tag 를 포함해 이미지 생성

```bash
./build_image.sh -bt dev -t torchserve-dev:1.0
```

## 3.3. codebuild 환경 기반 이미지 생성

- buildtype 인자에 codebuild 인수를 사용하면, codebuild 환경을 위한 도커 이미지를 만들 수 있다.

- CPU 기반 이미지 생성:

```bash
./build_image.sh -bt codebuild
```

- CUDA 버전에 따른 GPU 기반 이미지 생성

```bash
./build_image.sh -bt codebuild -g -cv <cu111, cu102, cu101, cu92>
```

- custom tag 를 포함해 이미지 생성

```bash
./build_image.sh -bt codebuild -t torchserve-codebuild:1.0
```

# 4. 생성한 TorchServe 이미지로 컨테이너 실행 테스트

- 아래에 예제는 외부/로컬호스트에 8080/8081/8082 및 7070/7071 포트를 노출시키며 컨테이너를 시작한다.
- CPU 컨테이너 시작

``` bash
docker run --rm -it -p 8080:8080 -p 8081:8081 -p 8082:8082 -p 7070:7070 -p 7071:7071 pytorch/torchserve:latest

### tag 에 latest 를 특정 버전으로 변경 가능
docker run --rm -it -p 8080:8080 -p 8081:8081 -p 8082:8082 -p 7070:7070 -p 7071:7071  pytorch/torchserve:0.1.1-cpu
```

- GPU 컨테이너 시작

``` bash
### 모든 GPU 를 가지고 tag 에 latest-gpu 로 최신 이미지 기반 시작
docker run --rm -it --gpus all -p 8080:8080 -p 8081:8081 -p 8082:8082 -p 7070:7070 -p 7071:7071 pytorch/torchserve:latest-gpu

### GPU devices 1, 2 번을 가지고 tag 에 latest-gpu 로 최신 이미지 기반 시작
docker run --rm -it --gpus '"device=1,2"' -p 8080:8080 -p 8081:8081 -p 8082:8082 -p 7070:7070 -p 7071:7071 pytorch/torchserve:latest-gpu

### 모든 GPU 를 가지고 tag 에 특정한 값을 사용한 이미지 기반 시작
docker run --rm -it --gpus all -p 8080:8080 -p 8081:8081 -p 8082:8082 -p 7070:7070 -p 7071:7071 pytorch/torchserve:0.1.1-cuda10.1-cudnn7-runtime
```

- 컨테이너 안에 있는 TorchServe APIs 에 접근해보기

``` bash
### 로컬호스트를 통해 8080 과 8081 포트로 각각 torchserve 의 inference API 와 management API 에 접근 가능
$ curl http://localhost:8080/ping
{
  "status": "Healthy"
}
$ curl http://localhost:8081/ping
{
  "status": "Healthy"
}
```

# 5. container 를 띄워서 torch-model-archiver 생성

- TorchServe deployment 를 위한 MAR (model archive) 파일을 만들기 위해서, 아래 단계들을 수행하자.

1. 필요하다면, model weight 값들을 다운로드하자.

``` bash
### 컨테이너 안에서 curl 이 없고 sudo 권한이 없기 때문에 로컬에서 마운트 하기 전에 host 경로에서 다운받자
cd serve
curl -o examples/image_classifier/densenet161-8d451a50.pth https://download.pytorch.org/models/densenet161-8d451a50.pth
```

2. 생성한 MAR 를 저장하기 위한 model-store 디렉토리, MAR 생성을 위한 모델이 저장되어 있는 디렉토리, 그 외에 추가적인 디렉토리 등을 마운트하면서 컨테이너를 시작하고 bash prompt 바인딩하여 들어가자.

``` bash
docker run --rm -ti --name mar \
-p 8080:8080 -p 8081:8081 \
-v $(pwd)/model-store:/home/model-server/model-store \
-v $(pwd)/examples:/home/model-server/examples  \
pytorch/torchserve:latest bash
```

3. torch-model-archiver 명령어를 실행하자.

``` bash
### 컨테이너 안에서 실행
torch-model-archiver \
--model-name densenet161 \
--version 1.0 \
--model-file /home/model-server/examples/image_classifier/densenet_161/model.py \
--serialized-file /home/model-server/examples/image_classifier/densenet161-8d451a50.pth \
--export-path /home/model-server/model-store \
--extra-files /home/model-server/examples/image_classifier/index_to_name.json \
--handler image_classifier
```

5. `desnet161.mar` 파일이 `/home/model-server/model-store` 에 있어야 함.

``` bash
### 컨테이너 안에서 실행
$ ll -h /home/model-server/model-store
total 106M
drwxrwxrwx 2 root         root         4.0K Jun 24 16:47 ./
drwxr-xr-x 1 model-server model-server 4.0K Jun 24 16:47 ../
-rw-r--r-- 1 model-server model-server 106M Jun 24 16:47 densenet161.mar
```

# 6. 운영 환경에서 docker 기반 torchserve 생성

- 앞서 생성한 torchserve 용 도커 이미지와 이 이미지를 활용해 생성한 MAR 를 운영 환경에서 생성한 도커 이미지를 활용해 torchserve 해보자.
- docker 기반으로 운영 환경에 torchserve 를 배포할 때 아래에 고려할 점들과 도커 옵션들을 고려해봐야 한다.

## 6.1. shared memory size

- `shm-size` : shm-size 인자는 컨테이너가 사용하는 shared-memory 를 명시할 수 있게 해준다. 할당되는 메모리를 늘려줌으로써 memory-intensive 한 컨테이너가 더 빨리 수행되도록 할 수 있다.

## 6.2. system resource 에 대한 사용자 제한

- `--ulimit memlock=-1` : 최대 locked-in-memory 주소 공간
- `--ulimit stack` : Linux stack size
- 현재 ulimit 값은 `ulimit -a` 로 확인할 수 있다.
- docker 의 리소스 제약은 docker docs 중에 [여기](https://docs.docker.com/config/containers/resource_constraints/), [여기](https://docs.docker.com/engine/reference/commandline/run/#set-ulimits-in-container---ulimit), [여기](https://docs.docker.com/engine/reference/run/#runtime-constraints-on-resources) 에서 더 자세히 확인할 수 있다.

## 6.3. 특정 포트와 볼륨을 노출 시키기

- `-p8080:8080 -p8081:8081 -p8082:8082 -p7070:7070 -p7071:7071` : TorchServe 는 REST 기반 inference 에 8080 포트, management 에 8081 포트, metrics API 에 8082 를 사용하고 gRPC API 에 7070, 7071 포트를 사용한다. docker 와 host 간에 HTTP 및 gRPC 요청을 위해 이러한 포트를 호스트에 노출할 수 있다.
- model store 는 `--model-store` 옵션을 통해 torchserve 로 전달된다. 또는 model-store 디렉토리에 모델을 미리 저장하는 것을 선호한다면, shared volume 사용을 고려할 수 있다.

## 6.4. torchserve 를 docker 기반으로 실행

- 일반적인 명령어

``` bash
docker run --rm --shm-size=1g \
        --ulimit memlock=-1 \
        --ulimit stack=67108864 \
        -p8080:8080 \
        -p8081:8081 \
        -p8082:8082 \
        -p7070:7070 \
        -p7071:7071 \
        --mount type=bind,source=/path/to/model/store,target=/tmp/models <container> torchserve --model-store=/tmp/models 
```

- 예제

``` bash
docker run -tid --rm --shm-size=1g \
  --name torchserve \
  --ulimit memlock=-1 \
  --ulimit stack=67108864 \
  -p8080:8080 \
  -p8081:8081 \
  -p8082:8082 \
  -p7070:7070 \
  -p7071:7071 \
  -v $(pwd)/model-store:/tmp/models \
  pytorch/torchserve:latest-cpu \
  torchserve --start --ncs --model-store=/tmp/models --models densenet161.mar
```

## 6.5. torchserve 로 curl 추론 테스트

- 먼저, 고양이 이미지를 다운로드 하자.

``` bash
curl -O https://raw.githubusercontent.com/pytorch/serve/master/docs/images/kitten_small.jpg
```

- 다운 받은 고양이 이미지로 prediction endpoint 로 호출하자.

``` bash
curl http://127.0.0.1:8080/predictions/densenet161 -T kitten_small.jpg
```

- 추론 결과 JSON 이 아래와 같다.

``` json
[
  {
    "tiger_cat": 0.46933549642562866
  },
  {
    "tabby": 0.4633878469467163
  },
  {
    "Egyptian_cat": 0.06456148624420166
  },
  {
    "lynx": 0.0012828214094042778
  },
  {
    "plastic_bag": 0.00023323034110944718
  }
]
```

# 7. lesson learned

- torchserve 의 buildtype 에 따라 이미지가 다른데 구체적인 차이점이 뭔가?
- 아직, docker 기반으로 mar 를 빌드하는 작업이 크게 편리한 것 같지는 않다. 개선의 여지들이 보인다. 이 부분을 CI 자동화 하는 것도 재밌을 것 같다.
- torchserve 를 띄우는데 성공했지만 잘 운영하는 법도 꽤나 학습이 필요할 것 같다. 특히나 로깅을 잘해야 할 것 같다.