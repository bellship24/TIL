**목차**

- [1. 요약](#1-요약)
- [2. dcgm exporter 란?](#2-dcgm-exporter-란)
- [3. 전제 조건](#3-전제-조건)
  - [3.1. nvidia driver 설치](#31-nvidia-driver-설치)
  - [3.2. docker 설치](#32-docker-설치)
  - [3.3. NVIDIA Container Toolkit (nvidia-docker2) 설치](#33-nvidia-container-toolkit-nvidia-docker2-설치)
  - [3.4. docker runtime 을 nvidia 로 설정 (GPU 노드)](#34-docker-runtime-을-nvidia-로-설정-gpu-노드)
  - [3.5. k8s 설치](#35-k8s-설치)
  - [3.6. NVIDIA Device Plugin 설치](#36-nvidia-device-plugin-설치)
  - [3.7. kube-prometheus-stack 구축 (helm)](#37-kube-prometheus-stack-구축-helm)
- [4. dcgm exporter 설치 (helm)](#4-dcgm-exporter-설치-helm)
- [5. 서비스 확인](#5-서비스-확인)
  - [5.1. prometheus 에서 dcgm metrics 확인](#51-prometheus-에서-dcgm-metrics-확인)
  - [5.2. grafana 에서 dcgm metrics 확인](#52-grafana-에서-dcgm-metrics-확인)
- [6. 기타](#6-기타)
  - [6.1. dcgm exporter 설치 (Docker) (참고)](#61-dcgm-exporter-설치-docker-참고)

**참고**

- [[GitHub] NVIDIA/dcgm-exporter](https://github.com/NVIDIA/dcgm-exporter)
- [[Nvidia docs] dcgm-exporter 를 pm+gf 에 연동하기 위한 GPU Telemetry 구조 및 구축 방법](https://docs.nvidia.com/datacenter/cloud-native/kubernetes/dcgme2e.html#gpu-telemetry)
- [[Nvidia docs] dcgm-exporter Operator 를 pm+gf 에 연동하기 위한 GPU Telemetry 구조 및 구축 방법](https://docs.nvidia.com/datacenter/cloud-native/gpu-operator/getting-started.html#gpu-telemetry)

---

# 1. 요약

- NVIDIA GPU 의 metrics 을 모니터링 하려면, helm 을 사용해 dcgm exporter 를 구축하고 prometheus+grafana 를 통해 시각화하면 된다.
- 이런 기능을 하기 위해서는 다양한 전제 조건이 필요하다. nvidia driver 설치, docker 설치, nvidia-docker2 설치, docker runtime 설정, k8s 구성, NVIDIA Device Plugin 설치, kube-prometheus-stack 설치, dcgm exporter 설치, grafana nvidia 대시보드 추가 까지 필요한 작업들이 굉장히 많다.

# 2. dcgm exporter 란?

- NVIDIA GPU metrics 에 대한 prometheus exporter

# 3. 전제 조건

- 아래 전제 조건들 중에 불충족한 것들이 있다면, 내용을 따라 진행하자.

## 3.1. nvidia driver 설치

kernel header 와 development package 설치

``` bash
sudo apt-get install linux-headers-$(uname -r)
```

CUDA network 리포지토리 추가

- 리포지토리를 추가하고 Canonical 보다 우선순위를 높임.

``` bash
distribution=$(. /etc/os-release;echo $ID$VERSION_ID | sed -e 's/\.//g') \
   && wget https://developer.download.nvidia.com/compute/cuda/repos/$distribution/x86_64/cuda-$distribution.pin \
   && sudo mv cuda-$distribution.pin /etc/apt/preferences.d/cuda-repository-pin-600
```

CUDA 리포지토리의 GPG key 설치

``` bash
sudo apt-key adv --fetch-keys https://developer.download.nvidia.com/compute/cuda/repos/$distribution/x86_64/7fa2af80.pub \
   && echo "deb http://developer.download.nvidia.com/compute/cuda/repos/$distribution/x86_64 /" | sudo tee /etc/apt/sources.list.d/cuda.list
```

cuda-drivers 설치

``` bash
sudo apt-get update \
   && sudo apt-get -y install cuda-drivers
```

## 3.2. docker 설치

``` bash
curl https://get.docker.com | sh
sudo systemctl --now enable docker
```

## 3.3. NVIDIA Container Toolkit (nvidia-docker2) 설치

리포지토리 추가

``` bash
distribution=$(. /etc/os-release;echo $ID$VERSION_ID) \
   && curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add - \
   && curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | sudo tee /etc/apt/sources.list.d/nvidia-docker.list
```

nvidia-docker2 설치

``` bash
sudo apt-get update \
  && sudo apt-get install -y nvidia-docker2
```

## 3.4. docker runtime 을 nvidia 로 설정 (GPU 노드)

- k8s 가 아직 `--gpus` 옵션을 지원하지 않기 때문에 도커 런타임을 앞서 설치한 nvidia-docker2 로 변경.

`/etc/docker/daemon.json` 작성 혹은 내용 추가

``` json
{
   "default-runtime": "nvidia",
   "runtimes": {
        "nvidia": {
            "path": "/usr/bin/nvidia-container-runtime",
            "runtimeArgs": []
      }
   }
}
```

도커 서버 재시작

``` bash
sudo systemctl restart docker
```

CUDA 컨테이너를 돌려 GPU 할당 잘 되는지 점검

``` bash
$ sudo docker run --rm -gpus all nvidia/cuda:11.0-base nvidia-smi

+-----------------------------------------------------------------------------+
| NVIDIA-SMI 450.51.06    Driver Version: 450.51.06    CUDA Version: 11.0     |
|-------------------------------+----------------------+----------------------+
| GPU  Name        Persistence-M| Bus-Id        Disp.A | Volatile Uncorr. ECC |
| Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute M. |
|                               |                      |               MIG M. |
|===============================+======================+======================|
|   0  Tesla T4            On   | 00000000:00:1E.0 Off |                    0 |
| N/A   34C    P8     9W /  70W |      0MiB / 15109MiB |      0%      Default |
|                               |                      |                  N/A |
+-------------------------------+----------------------+----------------------+

+-----------------------------------------------------------------------------+
| Processes:                                                                  |
|  GPU   GI   CI        PID   Type   Process name                  GPU Memory |
|        ID   ID                                                   Usage      |
|=============================================================================|
|  No running processes found                                                 |
+-----------------------------------------------------------------------------+
```

## 3.5. k8s 설치

- k8s 에서 GPU 를 사용하려면, 당연히 클러스터가 있어야함.

## 3.6. NVIDIA Device Plugin 설치

- k8s 에서 GPU 를 사용하기 위해서는 NVIDIA Device Plugin 이 필요.
- NVIDIA Device Plugin 은 클러스터의 각 노드에 있는 GPU 수를 자동으로 나열하고 GPU 를 갖고 Pod 를 실행할 수 있도록 하는 daemonset.
- 설치 방법은 helm 을 사용함.

helm 설치 (옵션)

- helm 이 없다면, 아래 처럼 설치.

``` bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 \
   && chmod 700 get_helm.sh \
   && ./get_helm.sh
```

helm repo 추가

``` bash
helm repo add nvdp https://nvidia.github.io/k8s-device-plugin \
   && helm repo update
```

device plugin 배포

``` bash
helm install nvidia-device-plugin nvdp/nvidia-device-plugin
```

배포 확인

``` bash
$ kubectl get po -n kube-system | grep nvidia
NAMESPACE     NAME                                       READY   STATUS      RESTARTS   AGE
kube-system   nvidia-device-plugin-1595448322-42vgf      1/1     Running     2          9d
```

CUDA 파드 배포 테스트

- gpu 1개 요청

``` bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: gpu-operator-test
spec:
  restartPolicy: OnFailure
  containers:
  - name: cuda-vector-add
    image: "nvidia/samples:vectoradd-cuda10.2"
    resources:
      limits:
         nvidia.com/gpu: 1
EOF
```

배포 확인

``` bash
$ kubectl get po gpu-operator-test
NAME                READY   STATUS      RESTARTS   AGE
gpu-operator-test   0/1     Completed   0          9d
```

로그 확인

``` bash
$ kubectl logs gpu-operator-test
[Vector addition of 50000 elements]
Copy input data from the host memory to the CUDA device
CUDA kernel launch with 196 blocks of 256 threads
Copy output data from the CUDA device to the host memory
Test PASSED
Done
```

파드 삭제

``` bash
kubectl delete po gpu-operator-test
```

## 3.7. kube-prometheus-stack 구축 (helm)

`prometheus-community` helm repo 추가

``` bash
helm repo add prometheus-community \
   https://prometheus-community.github.io/helm-charts

### 확인
helm search repo kube-prometheus
```

kube-prometheus-stack 차트 다운로드

``` bash
helm fetch prometheus-community/kube-prometheus-stack --untar
cd kube-prometheus-stack
```

override-values.yaml 작성

통신 방식 설정

- 아래 예제에서는 nodeport 를 사용하도록 설정함.
- 필요에 따라 ingress 를 사용하도록 설정을 바꿔도 됨.

``` yaml
From:
 ## Port to expose on each node
 ## Only used if service.type is 'NodePort'
 ##
 nodePort: 30090

 ## Loadbalancer IP
 ## Only use if service.type is "loadbalancer"
 loadBalancerIP: ""
 loadBalancerSourceRanges: []
 ## Service type
 ##
 type: ClusterIP

To:
 ## Port to expose on each node
 ## Only used if service.type is 'NodePort'
 ##
 nodePort: 30090

 ## Loadbalancer IP
 ## Only use if service.type is "loadbalancer"
 loadBalancerIP: ""
 loadBalancerSourceRanges: []
 ## Service type
 ##
 type: NodePort

serviceMonitorSelectorNilUsesHelmValues: false

additionalScrapeConfigs:
- job_name: gpu-metrics
  scrape_interval: 1s
  metrics_path: /metrics
  scheme: http
  kubernetes_sd_configs:
  - role: endpoints
    namespaces:
      names:
      - gpu-operator-resources
  relabel_configs:
  - source_labels: [__meta_kubernetes_pod_node_name]
    action: replace
    target_label: kubernetes_node
```

설치

``` bash
helm install prometheus-community/kube-prometheus-stack \
   --create-namespace --namespace prometheus \
   prometheus-stack \
   --values override-values.yaml
```

설치 확인

``` bash
kubectl get all -n prometheus
```

# 4. dcgm exporter 설치 (helm)

helm repo 추가

``` bash
helm repo add gpu-helm-charts \
  https://nvidia.github.io/dcgm-exporter/helm-charts
```

helm repo 업데이트

``` bash
helm repo update
```

chart 다운로드 (옵션)

``` bash
helm fetch gpu-helm-charts/dcgm-exporter --untar
```

override-values.yaml 작성

``` yaml

```

helm chart 설치

``` bash
helm update --install \
 -n dcgm --create-namespace \
 dcgm \
 gpu-helm-charts/dcgm-exporter
```

설치 확인

``` bash
kubectl get po -n dcgm
kubectl get svc -n dcgm
```

# 5. 서비스 확인

## 5.1. prometheus 에서 dcgm metrics 확인

- `DCGM_FI_DEV_GPU_UTIL` 로 쿼리하여 확인 가능.

![](/.uploads2/2021-09-29-23-49-06.png)

## 5.2. grafana 에서 dcgm metrics 확인

nvidia dashboard 추가

- `https://grafana.com/grafana/dashboards/12239` 추가

![](/.uploads2/2021-09-30-00-35-48.png)
![](/.uploads2/2021-09-30-00-37-05.png)
![](/.uploads2/2021-09-30-00-37-14.png)

# 6. 기타

## 6.1. dcgm exporter 설치 (Docker) (참고)

``` bash
$ docker run -d --gpus all --rm -p 9400:9400 nvcr.io/nvidia/k8s/dcgm-exporter:2.2.9-2.4.0-ubuntu18.04
$ curl localhost:9400/metrics
# HELP DCGM_FI_DEV_SM_CLOCK SM clock frequency (in MHz).
# TYPE DCGM_FI_DEV_SM_CLOCK gauge
# HELP DCGM_FI_DEV_MEM_CLOCK Memory clock frequency (in MHz).
# TYPE DCGM_FI_DEV_MEM_CLOCK gauge
# HELP DCGM_FI_DEV_MEMORY_TEMP Memory temperature (in C).
# TYPE DCGM_FI_DEV_MEMORY_TEMP gauge
...
DCGM_FI_DEV_SM_CLOCK{gpu="0", UUID="GPU-604ac76c-d9cf-fef3-62e9-d92044ab6e52"} 139
DCGM_FI_DEV_MEM_CLOCK{gpu="0", UUID="GPU-604ac76c-d9cf-fef3-62e9-d92044ab6e52"} 405
DCGM_FI_DEV_MEMORY_TEMP{gpu="0", UUID="GPU-604ac76c-d9cf-fef3-62e9-d92044ab6e52"} 9223372036854775794
...
```