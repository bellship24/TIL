**목차**

- [1. TL;DR](#1-tldr)
- [2. 배경](#2-배경)
- [3. nvidia-driver 설치 (GPU 노드)](#3-nvidia-driver-설치-gpu-노드)
- [4. nvidia-docker 설치 (GPU 노드)](#4-nvidia-docker-설치-gpu-노드)
- [5. docker 의 기본 runtime 을 nvidia 로 설정 (GPU 노드)](#5-docker-의-기본-runtime-을-nvidia-로-설정-gpu-노드)
- [6. nvidia-docker 로 GPU 컨테이너 스케줄링 테스트](#6-nvidia-docker-로-gpu-컨테이너-스케줄링-테스트)
- [7. k8s-device-plugin 이란?](#7-k8s-device-plugin-이란)
- [8. k8s-device-plugin 역할](#8-k8s-device-plugin-역할)
- [9. k8s-device-plugin 전제](#9-k8s-device-plugin-전제)
- [10. k8s-device-plugin 설치](#10-k8s-device-plugin-설치)
- [11. GPU 노드 라벨링 (GPU 노드)](#11-gpu-노드-라벨링-gpu-노드)
- [12. k8s-device-plugin 로 GPU 파드 스케줄링 테스트](#12-k8s-device-plugin-로-gpu-파드-스케줄링-테스트)

**참고**

- [[Blog] Kubernetes 의 GPU 설정](https://crystalcube.co.kr/201)
- [[GitHub] NVIDIA device plugin for Kubernetes](https://github.com/NVIDIA/k8s-device-plugin/tree/v1.12)

---

# 1. TL;DR

> k8s 에서도 GPU 자원에 대해 스케줄링이 가능하다. 관련 내용을 정리하고 설치와 GPU 스케줄링 테스트를 검증해보자.

> nvidia GPU 의 경우, nvidia-driver, nvidia-docker, k8s-device-plugin 을 설치 및 설정하면 GPU 개수 단위 할당이 가능해진다.

> nvidia-driver 의 설치는 여기서 생략한다. 사용할 CUDA 와 k8s-device-plugin 버전의 종속성이 있으니 알맞게 설치하도록 한다.

> nvidia-docker 는 docker 의 기본 runtime 을 nvidia 로 변경해야 하므로 docker 를 재시작해야 한다. 그러므로 되도록 사전에 작업해두는 것이 좋다.

> k8s-device-plugin 은 k8s 위에 daemonset 으로 올라간다.

# 2. 배경

Kubernetes 에서 nvidia GPU 를 사용하기 위해서는 아래와 같이 nvidia 도구들을 미리 구축을 해야한다.

- nvidia-driver v361.93+ (gpu 에 맞는 버전)
- nvidia-docker v2+
- kubernetes-device-plugin on k8s

하나씩 검토하면서 GPU 사용을 검증해보자.

# 3. nvidia-driver 설치 (GPU 노드)

먼저 nvidia driver 가 설치되어 있어야 한다.
사용 중인 GPU 에 맞는 nvidia-driver 를 설치하자. (이 부분 생략)
사용할 CUDA 버전에 대해서도 nvidia driver 버전 종속성이 있다. 그러므로 알맞은 nvidia driver 버전을 미리 파악하여 설치하자.

nvidia driver 설치 확인

``` bash
nvidia-smi
```

# 4. nvidia-docker 설치 (GPU 노드)

nvidia-docker 에 대한 패키지 매니저 repository 추가

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
# ubuntu
sudo apt-get install -y nvidia-docker2
sudo pkill -SIGHUP dockerd

# rhel7
sudo yum install -y nvidia-docker2
sudo pkill -SIGHUP dockerd
```

nvidia-docker 설치 확인

``` bash
nvidia-docker version
```

# 5. docker 의 기본 runtime 을 nvidia 로 설정 (GPU 노드)

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

- `daemon.json` 의 다른 설정 부분을 건드리지 않도록 조심하자. json 문법이 틀리면 도커 재시작이 안되니 편집기 등을 이용해서 점검 후 재시작하자.

도커 재시작

``` bash
sudo systemctl restart docker
sudo systemctl enable docker
sudo systemctl status docker
```

# 6. nvidia-docker 로 GPU 컨테이너 스케줄링 테스트

- k8s 에서 GPU 스케줄링을 하기 전에 먼저 docker 로 올릴 수 있는지 확인하자.

``` bash
$ docker run -ti --rm --gpus all nvidia/cuda:11.0-base nvidia-smi

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

# 7. k8s-device-plugin 이란?

- k8s 에서 GPU 사용을 가능하게 해주는 플러그인이다.
- 아래 명령어를 통해 매니페스트가 배포되며 helm 으로도 배포가능하다.
- 이 plugin 은 daemonset 으로 배포된다.

# 8. k8s-device-plugin 역할

- 이 daemonset 은 클러스터 안에 있는 각 노드의 GPU 개수를 노출시킨다.
- GPU 에 대한 health 를 트래킹한다.
- 클러스터 안에 컨테이너가 GPU 를 쓸 수 있게 함.

# 9. k8s-device-plugin 전제

- NVIDIA drivers v361.93+
- nvidia-docker v2.0+
- 기본적으로 docker 의 runtime 을 nvidia 로 설정
- Kubernetes v1.11+

# 10. k8s-device-plugin 설치

master NoSchedule taint 삭제 (옵션)

- 마스터 노드만 있는 클러스터의 경우, k8s-device-plugin 데몬셋에 딸린 파드를 띄우기 위해 마스터 노드의 NoSchedule taint 를 삭제한다.
- 클러스터 상황에 따라 NoSchedule effect 를 갖은 taint 가 있을 수 있다. 해당 taint 도 아래와 같이 `-` 로 없애주면 된다.

```bash
$ kubectl taint nodes --all node-role.kubernetes.io/master-
node/araview-ws-01 untainted                                                      
node/araview-ws-04 untainted                                                      
node/ubuntu untainted
```

k8s-device-plugin 매니페스트 확인

```bash
$ curl https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/1.0.0-beta4/nvidia-device-plugin.yml

# Copyright (c) 2019, NVIDIA CORPORATION.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: nvidia-device-plugin-daemonset
  namespace: kube-system
spec:
  selector:
    matchLabels:
      name: nvidia-device-plugin-ds
  updateStrategy:
    type: RollingUpdate
  template:
    metadata:
      # This annotation is deprecated. Kept here for backward compatibility
      # See https://kubernetes.io/docs/tasks/administer-cluster/guaranteed-scheduling-critical-addon-pods/
      annotations:
        scheduler.alpha.kubernetes.io/critical-pod: ""
      labels:
        name: nvidia-device-plugin-ds
    spec:
      tolerations:
      # This toleration is deprecated. Kept here for backward compatibility
      # See https://kubernetes.io/docs/tasks/administer-cluster/guaranteed-scheduling-critical-addon-pods/
      - key: CriticalAddonsOnly
        operator: Exists
      - key: nvidia.com/gpu
        operator: Exists
        effect: NoSchedule
      # Mark this pod as a critical add-on; when enabled, the critical add-on
      # scheduler reserves resources for critical add-on pods so that they can
      # be rescheduled after a failure.
      # See https://kubernetes.io/docs/tasks/administer-cluster/guaranteed-scheduling-critical-addon-pods/
      priorityClassName: "system-node-critical"
      containers:
      - image: nvidia/k8s-device-plugin:1.0.0-beta4
        name: nvidia-device-plugin-ctr
        securityContext:
          allowPrivilegeEscalation: false
          capabilities:
            drop: ["ALL"]
        volumeMounts:
          - name: device-plugin
            mountPath: /var/lib/kubelet/device-plugins
      volumes:
        - name: device-plugin
          hostPath:
            path: /var/lib/kubelet/device-plugins
```

k8s-device-plugin 배포

```bash
$ kubectl create -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/1.0.0-beta4/nvidia-device-plugin.yml
daemonset.apps/nvidia-device-plugin-daemonset created
```

k8s-device-plugin 배포 확인

```bash
$ k get ds -n kube-system nvidia-device-plugin-daemonset
NAME                             DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR   AGE
nvidia-device-plugin-daemonset   3         3         3       3            3           <none>          5m16s
```

k8s-device-plugin 파드 상태 확인

```bash
$ k get po -n kube-system -l=name=nvidia-device-plugin-ds
NAME                                   READY   STATUS    RESTARTS   AGE
nvidia-device-plugin-daemonset-54xqh   1/1     Running   0          2m39s
nvidia-device-plugin-daemonset-6czmn   1/1     Running   0          2m39s
nvidia-device-plugin-daemonset-br2x2   1/1     Running   0          2m39s
```

# 11. GPU 노드 라벨링 (GPU 노드)

GPU 노드에 `gpu=true` 라벨 추가

- 파드를 배포할 때는 GPU 가 사용 가능한 노드에 라벨링을 하여 셀렉터를 통해 스케줄링 해야 한다. 그러므로 추후에 애플리케이션 배포할 때, nodeSelector 의 기준으로 사용할 gpu 라벨을 true 값으로 설정해준다.

```bash
kubectl label nodes araview-ws-01 gpu=true
kubectl label nodes araview-ws-04 gpu=true
kubectl label nodes ubuntu gpu=true
```

- 사실 셀렉터 설정을 안해도 GPU 를 파드에 설정할 때 `spec.containers.resources.limits.nvidia.com/gpu` 를 통해 리소스 제약을 걸면, DeploymentController 가 알아서 스케줄링을 할 때, GPU 가 없으면 실패하고 결국 GPU 를 가용할 수 있는 노드에 배포하게 된다.

# 12. k8s-device-plugin 로 GPU 파드 스케줄링 테스트

GPU 배포 테스트

- k8s-device-plugin 을 통해 GPU 리소스를 통제하는 기능은 GPU 개수 지정만 가능하다. GPU 메모리 지정이나 request 는 사용할 수 없다.

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: cuda-vector-add
spec:
  restartPolicy: OnFailure
  containers:
    - name: cuda-vector-add
      # https://github.com/kubernetes/kubernetes/blob/v1.7.11/test/images/nvidia-cuda/Dockerfile
      image: "k8s.gcr.io/cuda-vector-add:v0.1"
      resources:
        limits:
          nvidia.com/gpu: 1 # GPU 1개 요청하기
  nodeSelector:
    gpu: "true"
EOF
```

테스트 결과 확인

```bash
infra@araview-ws-01:~/jongbae$ k get po
NAME              READY   STATUS      RESTARTS   AGE
cuda-vector-add   0/1     Completed   0          2m20s
infra@araview-ws-01:~/jongbae$ 
infra@araview-ws-01:~/jongbae$ 
infra@araview-ws-01:~/jongbae$ 
infra@araview-ws-01:~/jongbae$ k logs cuda-vector-add
[Vector addition of 50000 elements]
Copy input data from the host memory to the CUDA device
CUDA kernel launch with 196 blocks of 256 threads
Copy output data from the CUDA device to the host memory
Test PASSED
Done
```
