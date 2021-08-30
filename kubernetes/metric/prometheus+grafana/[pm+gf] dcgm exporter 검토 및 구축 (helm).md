**목차**

- [1. 요약](#1-요약)
- [2. dcgm exporter 란?](#2-dcgm-exporter-란)
- [dcgm exporter 컨테이너로 실행하는 방법](#dcgm-exporter-컨테이너로-실행하는-방법)
- [3. dcgm exporter 설치 (helm)](#3-dcgm-exporter-설치-helm)
- [4. dcgm exporter 를 prometheus 에 연동하기](#4-dcgm-exporter-를-prometheus-에-연동하기)

**참고**

- [[GitHub] NVIDIA/dcgm-exporter](https://github.com/NVIDIA/dcgm-exporter)
- [[Nvidia docs] dcgm-exporter 를 pm+gf 에 연동하기 위한 GPU Telemetry 구조 및 구축 방법](https://docs.nvidia.com/datacenter/cloud-native/kubernetes/dcgme2e.html#gpu-telemetry)
- [[Nvidia docs] dcgm-exporter Operator 를 pm+gf 에 연동하기 위한 GPU Telemetry 구조 및 구축 방법](https://docs.nvidia.com/datacenter/cloud-native/gpu-operator/getting-started.html#gpu-telemetry)

---

# 1. 요약

>

# 2. dcgm exporter 란?

- NVIDIA GPU metrics 에 대한 prometheus exporter

# dcgm exporter 컨테이너로 실행하는 방법

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

# 3. dcgm exporter 설치 (helm)

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
 --generate-name \
 gpu-helm-charts/dcgm-exporter
```

# 4. dcgm exporter 를 prometheus 에 연동하기

- dcgm-exporter 는 GPU Operator 의 일부로 배포된다. prometheus 에 연동하기 위해서 Operator

[작성중](https://docs.nvidia.com/datacenter/cloud-native/kubernetes/dcgme2e.html#gpu-telemetry)