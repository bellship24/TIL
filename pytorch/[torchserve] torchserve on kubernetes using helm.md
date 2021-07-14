**요약**

- helm 을 사용해 kubernetes 기반으로 한 torchserve 배포를 검토하고 CPU 기반으로 검증해보자.

**목차**

- [1. helm 으로 torchserve 설치](#1-helm-으로-torchserve-설치)
  - [1.1. 주요 파라미터](#11-주요-파라미터)
  - [1.2. 기본 values.yaml](#12-기본-valuesyaml)
  - [1.3. helm install torchserve](#13-helm-install-torchserve)
- [2. 설치 확인](#2-설치-확인)
  - [2.1. 생성된 ts 파드에서 로그 확인](#21-생성된-ts-파드에서-로그-확인)
  - [2.2. 생성된 svc 로드밸런서 IP 확인](#22-생성된-svc-로드밸런서-ip-확인)
  - [2.3. management / prediction API 로 호출 테스트](#23-management--prediction-api-로-호출-테스트)
- [3. metrics 설정](#3-metrics-설정)
  - [3.1. prometheus 설치](#31-prometheus-설치)
  - [3.2. grafana 설치](#32-grafana-설치)
  - [3.3. grafana 의 admin 패스워드 조회](#33-grafana-의-admin-패스워드-조회)
  - [3.4. grafana 에 데이터 소스로 prometheus 추가](#34-grafana-에-데이터-소스로-prometheus-추가)
  - [3.5. 로드밸런서로 grafana 노출시키기](#35-로드밸런서로-grafana-노출시키기)
  - [3.6. grafana 에 로그인](#36-grafana-에-로그인)
- [4. 로깅](#4-로깅)
- [5. 트러블슈팅](#5-트러블슈팅)

**참조**

- [[TorchServe Docs] kubernetes](https://github.com/pytorch/serve/blob/master/kubernetes/README.md)

---

# 1. helm 으로 torchserve 설치

## 1.1. 주요 파라미터

- 이 helm chart 의 주요 파라미터(인자)는 아래와 같다.

| Parameter        | Description               | Default                       |
|------------------|---------------------------|-------------------------------|
| image            | Torchserve Serving image  | pytorch/torchserve:latest-gpu |
| inference_port   | TS Inference port         | 8080                          |
| management_port  | TS Management port        | 8081                          |
| metrics_port     | TS Mertics port           | 8082                          |
| replicas         | K8S deployment replicas   | 1                             |
| model-store      | EFS mountpath             | /home/model-server/shared/    |
| persistence.size | Storage size to request   | 1Gi                           |
| n_gpu            | Number of GPU in a TS Pod | 1                             |
| n_cpu            | Number of CPU in a TS Pod | 1                             |
| memory_limit     | TS Pod memory limit       | 4Gi                           |
| memory_request   | TS Pod memory request     | 1Gi                           |

- CPU 를 사용하는 노드라면, image 를 `pytorch/torchserve:latest` 등으로 바꾸자.
- 모델 사이즈에 따라 `persistence.size` 를 변경하자.
- `replicas` 값은 노드 수보다 작기를 권고한다.
- `n_gpu` 는 docker 에 의해 torchserve 컨테이너에 노출된다. 이 값은 `config-properties` 안에 `number_of_gpu` 와 일치해야한다.
- `n_gpu` 와 `n_cpu` 값은 전체 클러스터 수준이 아닌 pod 수준에서 사용된다.

## 1.2. 기본 values.yaml

``` yaml
# Default values for torchserve helm chart.

torchserve_image: pytorch/torchserve:latest-gpu

namespace: torchserve

torchserve:
  management_port: 8081
  inference_port: 8080
  metrics_port: 8082
  pvd_mount: /home/model-server/shared/
  n_gpu: 1
  n_cpu: 1
  memory_limit: 4Gi
  memory_request: 1Gi

deployment:
  replicas: 1 # Changes this to number of node in Node Group

persitant_volume:
  size: 1Gi
```

## 1.3. helm install torchserve

- 앞서 받은 torchserve git repo. 의 kubernetes 디렉토리에서 `helm install ts .` 로 설치한다.

``` bash
ubuntu@ip-172-31-50-36:~/serve/kubernetes$ helm install ts .
NAME: ts
LAST DEPLOYED: Wed Jul 29 08:29:04 2020
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
```

# 2. 설치 확인

## 2.1. 생성된 ts 파드에서 로그 확인

``` bash
ubuntu@ip-172-31-50-36:~/serve/kubernetes$ kubectl exec pod/torchserve-fff -- cat logs/ts_log.log
2020-07-29 08:29:08,295 [INFO ] main org.pytorch.serve.ModelServer -
Torchserve version: 0.1.1
TS Home: /home/venv/lib/python3.6/site-packages
Current directory: /home/model-server
......
```

## 2.2. 생성된 svc 로드밸런서 IP 확인

``` bash
ubuntu@ip-172-31-65-0:~/ts/rel/serve$ kubectl get svc
NAME         TYPE           CLUSTER-IP      EXTERNAL-IP                                                              PORT(S)                         AGE
torchserve   LoadBalancer   10.100.142.22   your-loadbalancer-address   8080:31115/TCP,8081:31751/TCP   14m
```

## 2.3. management / prediction API 로 호출 테스트

``` bash
curl http://your-loadbalancer-address:8081/models

# You should something similar to the following
{
  "models": [
    {
      "modelName": "mnist",
      "modelUrl": "mnist.mar"
    },
    {
      "modelName": "squeezenet1_1",
      "modelUrl": "squeezenet1_1.mar"
    }
  ]
}


curl http://your-loadbalancer-address:8081/models/squeezenet1_1

# You should see something similar to the following
[
  {
    "modelName": "squeezenet1_1",
    "modelVersion": "1.0",
    "modelUrl": "squeezenet1_1.mar",
    "runtime": "python",
    "minWorkers": 3,
    "maxWorkers": 3,
    "batchSize": 1,
    "maxBatchDelay": 100,
    "loadedAtStartup": false,
    "workers": [
      {
        "id": "9000",
        "startTime": "2020-07-23T18:34:33.201Z",
        "status": "READY",
        "gpu": true,
        "memoryUsage": 177491968
      },
      {
        "id": "9001",
        "startTime": "2020-07-23T18:34:33.204Z",
        "status": "READY",
        "gpu": true,
        "memoryUsage": 177569792
      },
      {
        "id": "9002",
        "startTime": "2020-07-23T18:34:33.204Z",
        "status": "READY",
        "gpu": true,
        "memoryUsage": 177872896
      }
    ]
  }
]


wget https://raw.githubusercontent.com/pytorch/serve/master/docs/.uploads/kitten_small.jpg
curl -X POST  http://your-loadbalancer-address:8080/predictions/squeezenet1_1 -T kitten_small.jpg

# You should something similar to the following
[
  {
    "lynx": 0.5370921492576599
  },
  {
    "tabby": 0.28355881571769714
  },
  {
    "Egyptian_cat": 0.10669822245836258
  },
  {
    "tiger_cat": 0.06301568448543549
  },
  {
    "leopard": 0.006023923866450787
  }
]
```

# 3. metrics 설정

## 3.1. prometheus 설치

``` bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/prometheus
```

## 3.2. grafana 설치

``` bash
helm repo add grafana https://grafana.github.io/helm-charts
helm install grafana grafana/grafana
```

## 3.3. grafana 의 admin 패스워드 조회

``` bash
kubectl get secret --namespace default grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

## 3.4. grafana 에 데이터 소스로 prometheus 추가

``` bash
kubectl get pods

NAME                                             READY   STATUS    RESTARTS   AGE
grafana-cbd8775fd-6f8l5                          1/1     Running   0          4h12m
model-store-pod                                  1/1     Running   0          4h35m
prometheus-alertmanager-776df7bfb5-hpsp4         2/2     Running   0          4h42m
prometheus-kube-state-metrics-6df5d44568-zkcm2   1/1     Running   0          4h42m
prometheus-node-exporter-fvsd6                   1/1     Running   0          4h42m
prometheus-node-exporter-tmfh8                   1/1     Running   0          4h42m
prometheus-pushgateway-85948997f7-4s4bj          1/1     Running   0          4h42m
prometheus-server-f8677599b-xmjbt                2/2     Running   0          4h42m
torchserve-7d468f9894-fvmpj                      1/1     Running   0          4h33m

kubectl get pod prometheus-server-f8677599b-xmjbt -o jsonpath='{.status.podIPs[0].ip}'
192.168.52.141
```

![](/.uploads/2021-06-26-18-49-07.png)

## 3.5. 로드밸런서로 grafana 노출시키기

``` bash
kubectl patch service grafana -p '{"spec": {"type": "LoadBalancer"}}'

kubectl get svc grafana -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

## 3.6. grafana 에 로그인

`<http://your.grafana.loadbalancer.address:3000>`

# 4. 로깅

EKF stack 을 사용해 log 를 통합할 수 있다. 자세한 내용은 [여기](https://www.digitalocean.com/community/tutorials/how-to-set-up-an-elasticsearch-fluentd-and-kibana-efk-logging-stack-on-kubernetes)를 참고하자.

# 5. 트러블슈팅

- 공식 문서에 나온 트러블슈팅들이 [여기](https://github.com/pytorch/serve/blob/master/kubernetes/README.md#troubleshooting)에 있다.