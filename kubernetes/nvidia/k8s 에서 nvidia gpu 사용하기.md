**목차**

**참고**

- [[Blog] Kubernetes 의 GPU 설정](https://crystalcube.co.kr/201)

---

# 요약

>

# 배경

kubernetes 에서 nvidia gpu 를 사용하기 위해서는 아래와 같이 nvidia 도구들을 미리 구축을 해야한다.

- nvidia-driver (gpu 에 맞는 버전)
- nvidia-docker v2+
- kubernetes-device-plugin on k8s

# nvidia-driver 설치 (GPU 워커 노드)

먼저 nvidia driver 가 설치되어 있어야 한다.
사용 중인 GPU 에 맞는 nvidia-driver 를 설치하자. (이 부분 생략)

nvidia driver 확인

``` bash
nvidia-smi
```

# nvidia-docker 설치 (GPU 워커 노드)

nvidia-docker 의 repository 추가

``` bash
# ubuntu
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | \
  sudo apt-key add -
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
  sudo tee /etc/apt/sources.list.d/nvidia-docker.list
sudo apt-get update
```

apt 로 nvidia-docker 설치

``` bash

```

nvidia-docker 설치 확인

``` bash
nvidia-docker version
```

# docker 의 기본 runtime 을 nvidia 로 설정 (GPU 워커 노드)

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

# nvidia-docker 로 GPU 컨테이너 스케줄링 테스트

``` bash
docker run -ti --rm --gpus all cuda
```

# nvidia-docker 설정

# kubernetes-device-plugin 설치 (k8s 클러스터)

``` bash
$ kubectl create -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v1.11/nvidia-device-plugin.yml
```

# GPU 워커 노드 라벨링

파드를 배포할 때는 GPU 가 사용 가능한 노드에 라벨링을 하여 셀렉터를 통해 스케줄링 해야 한다.
사실 셀렉터 설정을 안해도 GPU 를 파드에 설정할 때 `spec.containers.resources.limits.nvidia.com/gpu` 를 통해 리소스 제약을 걸면, DeploymentController 가 알아서 스케줄링을 할 때, GPU 가 없으면 실패하고 결국 GPU 를 가용할 수 있는 노드에 배포하게 된다.

# kubernetes-device-plugin 로 GPU 파드 스케줄링 테스트

kubernetes-device-plugin 을 통해 GPU 리소스를 통제하는 기능은 GPU 개수 지정만 가능하다. 그리고 GPU 메모리 지정이나 request 는 사용할 수 없다.
