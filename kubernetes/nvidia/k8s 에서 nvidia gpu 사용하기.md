**목차**

- [1. 요약](#1-요약)
- [2. 배경](#2-배경)
- [3. nvidia-driver 설치 (GPU 워커 노드)](#3-nvidia-driver-설치-gpu-워커-노드)
- [4. nvidia-docker 설치 (GPU 워커 노드)](#4-nvidia-docker-설치-gpu-워커-노드)
- [5. docker 의 기본 runtime 을 nvidia 로 설정 (GPU 워커 노드)](#5-docker-의-기본-runtime-을-nvidia-로-설정-gpu-워커-노드)
- [6. nvidia-docker 로 GPU 컨테이너 스케줄링 테스트](#6-nvidia-docker-로-gpu-컨테이너-스케줄링-테스트)
- [7. kubernetes-device-plugin 란?](#7-kubernetes-device-plugin-란)
- [8. kubernetes-device-plugin 역할](#8-kubernetes-device-plugin-역할)
- [9. kubernetes-device-plugin 전제](#9-kubernetes-device-plugin-전제)
- [10. kubernetes-device-plugin 설치 (k8s 클러스터)](#10-kubernetes-device-plugin-설치-k8s-클러스터)
- [11. GPU 워커 노드 라벨링](#11-gpu-워커-노드-라벨링)
- [12. kubernetes-device-plugin 로 GPU 파드 스케줄링 테스트](#12-kubernetes-device-plugin-로-gpu-파드-스케줄링-테스트)

**참고**

- [[Blog] Kubernetes 의 GPU 설정](https://crystalcube.co.kr/201)
- [[GitHub] NVIDIA device plugin for Kubernetes](https://github.com/NVIDIA/k8s-device-plugin/tree/v1.12)

---

# 1. 요약

>

# 2. 배경

kubernetes 에서 nvidia gpu 를 사용하기 위해서는 아래와 같이 nvidia 도구들을 미리 구축을 해야한다.

- nvidia-driver (gpu 에 맞는 버전)
- nvidia-docker v2+
- kubernetes-device-plugin on k8s

# 3. nvidia-driver 설치 (GPU 워커 노드)

먼저 nvidia driver 가 설치되어 있어야 한다.
사용 중인 GPU 에 맞는 nvidia-driver 를 설치하자. (이 부분 생략)

nvidia driver 확인

``` bash
nvidia-smi
```

# 4. nvidia-docker 설치 (GPU 워커 노드)

nvidia-docker 의 repository 추가

``` bash
# ubuntu
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | \
  sudo apt-key add -
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
  sudo tee /etc/apt/sources.list.d/nvidia-docker.list
sudo apt-get update

# rhel7
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.repo | \
  sudo tee /etc/yum.repos.d/nvidia-docker.repo
```

리눅스 패키지 매니저로 nvidia-docker 설치

``` bash
sudo apt-get install -y nvidia-docker2
sudo pkill -SIGHUP dockerd

sudo yum install -y nvidia-docker2
sudo pkill -SIGHUP dockerd
```

nvidia-docker 설치 확인

``` bash
nvidia-docker version
```

# 5. docker 의 기본 runtime 을 nvidia 로 설정 (GPU 워커 노드)

`/etc/docker/daemon.json` 에 아래 내용 추가

``` yaml
{
    "default-runtime": "nvidia",
    "runtimes": {
        "nvidia": {
            "path": "nvidia-container-runtime",
            "runtimeArgs": []
        }
    }
}
```

도커 재시작

``` bash
sudo systemctl restart docker
sudo systemctl enable docker
sudo systemctl status docker
```

# 6. nvidia-docker 로 GPU 컨테이너 스케줄링 테스트

``` bash
$ docker run -ti --rm --gpus all nvidia/cuda:11.0-base

+-----------------------------------------------------------------------------+
| NVIDIA-SMI 465.19.01    Driver Version: 465.19.01    CUDA Version: 11.3     |
|-------------------------------+----------------------+----------------------+
| GPU  Name        Persistence-M| Bus-Id        Disp.A | Volatile Uncorr. ECC |
| Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute M. |
|                               |                      |               MIG M. |
|===============================+======================+======================|
|   0  NVIDIA GeForce ...  Off  | 00000000:19:00.0 Off |                  N/A |
| 23%   32C    P8     9W / 250W |    139MiB / 11178MiB |      0%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+
|   1  NVIDIA GeForce ...  Off  | 00000000:1A:00.0 Off |                  N/A |
| 23%   35C    P8    10W / 250W |      4MiB / 11178MiB |      0%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+
|   2  NVIDIA GeForce ...  Off  | 00000000:67:00.0 Off |                  N/A |
| 23%   39C    P8     9W / 250W |      4MiB / 11178MiB |      0%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+
|   3  NVIDIA GeForce ...  Off  | 00000000:68:00.0  On |                  N/A |
| 26%   47C    P8    19W / 250W |    353MiB / 11177MiB |      2%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+
                                                                               
+-----------------------------------------------------------------------------+
| Processes:                                                                  |
|  GPU   GI   CI        PID   Type   Process name                  GPU Memory |
|        ID   ID                                                   Usage      |
|=============================================================================|
+-----------------------------------------------------------------------------+

$ docker run -ti --rm --gpus '"device=0,1,2"' nvidia/cuda:11.0-base nvidia-smi

+-----------------------------------------------------------------------------+
| NVIDIA-SMI 465.19.01    Driver Version: 465.19.01    CUDA Version: 11.3     |
|-------------------------------+----------------------+----------------------+
| GPU  Name        Persistence-M| Bus-Id        Disp.A | Volatile Uncorr. ECC |
| Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute M. |
|                               |                      |               MIG M. |
|===============================+======================+======================|
|   0  NVIDIA GeForce ...  Off  | 00000000:19:00.0 Off |                  N/A |
| 23%   32C    P8     9W / 250W |    139MiB / 11178MiB |      0%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+
|   1  NVIDIA GeForce ...  Off  | 00000000:1A:00.0 Off |                  N/A |
| 23%   35C    P8    10W / 250W |      4MiB / 11178MiB |      0%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+
|   2  NVIDIA GeForce ...  Off  | 00000000:67:00.0 Off |                  N/A |
| 23%   39C    P8     9W / 250W |      4MiB / 11178MiB |      0%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+
                                                                               
+-----------------------------------------------------------------------------+
| Processes:                                                                  |
|  GPU   GI   CI        PID   Type   Process name                  GPU Memory |
|        ID   ID                                                   Usage      |
|=============================================================================|
+-----------------------------------------------------------------------------+
```

# 7. kubernetes-device-plugin 란?

- k8s 에서 GPU 사용을 가능하게 해주는 플러그인이다.
- 아래 명령어를 통해 매니페스트가 배포되며 helm 으로도 배포가능하다.
- 이 plugin 은 daemonset 으로 배포된다.

# 8. kubernetes-device-plugin 역할

- 이 daemonset 은 클러스터 안에 있는 각 노드의 GPU 개수를 노출시킨다.
- GPU 에 대한 health 를 트래킹한다.
- 클러스터 안에 컨테이너가 GPU 를 쓸 수 있게 함.

# 9. kubernetes-device-plugin 전제

- NVIDIA drivers v361.93+
- nvidia-docker v2.0+
- 기본적으로 docker 의 runtime 을 nvidia 로 설정
- Kubernetes v1.11+

# 10. kubernetes-device-plugin 설치 (k8s 클러스터)

v1.11 설치

``` bash
$ kubectl create -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v1.11/nvidia-device-plugin.yml
```

설치 확인

``` bash

```

# 11. GPU 워커 노드 라벨링

파드를 배포할 때는 GPU 가 사용 가능한 노드에 라벨링을 하여 셀렉터를 통해 스케줄링 해야 한다.
사실 셀렉터 설정을 안해도 GPU 를 파드에 설정할 때 `spec.containers.resources.limits.nvidia.com/gpu` 를 통해 리소스 제약을 걸면, DeploymentController 가 알아서 스케줄링을 할 때, GPU 가 없으면 실패하고 결국 GPU 를 가용할 수 있는 노드에 배포하게 된다.

# 12. kubernetes-device-plugin 로 GPU 파드 스케줄링 테스트

kubernetes-device-plugin 을 통해 GPU 리소스를 통제하는 기능은 GPU 개수 지정만 가능하다. 그리고 GPU 메모리 지정이나 request 는 사용할 수 없다.

`gputest-po.yaml` 작성

``` yaml
apiVersion: v1
kind: Pod
metadata:
  name: gputest-po
spec:
  nodeSelector:
    gpu: "true"
  containers:
    - name: cuda-container
      image: nvidia/cuda:11.0-base
      resources:
        limits:
          nvidia.com/gpu: 2 # requesting 2 GPUs
```

`gputest-po.yaml` 적용

``` bash
k apply -f gputest-po.yaml
```