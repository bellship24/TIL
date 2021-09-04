**목차**

- [1. kube-apiserver 의 외부 user 로드밸런싱](#1-kube-apiserver-의-외부-user-로드밸런싱)
- [2. kube-apiserver 의 클러스터 내부 user 로드밸런싱](#2-kube-apiserver-의-클러스터-내부-user-로드밸런싱)
  - [2.1. kube-apiserver의 kubelet / kube-proxy 로드밸런싱 : nginx-proxy](#21-kube-apiserver의-kubelet--kube-proxy-로드밸런싱--nginx-proxy)
  - [nginx-proxy 상태 확인 방법](#nginx-proxy-상태-확인-방법)
- [3. etcd의 고가용성](#3-etcd의-고가용성)
  - [3.1. quorum (정족수)](#31-quorum-정족수)
  - [3.2. etcd 배포 아키텍처](#32-etcd-배포-아키텍처)
- [4. Ingress (L7) 고려 사항](#4-ingress-l7-고려-사항)
  - [4.1. Ingress controller 네트워킹](#41-ingress-controller-네트워킹)
  - [4.2. 인그레스 컨트롤러 배포 모델](#42-인그레스-컨트롤러-배포-모델)

**참고**

- [Building a Highly Available Kubernetes Cluster](https://rancher.com/learning-paths/building-a-highly-available-kubernetes-cluster/)

# 1. kube-apiserver 의 외부 user 로드밸런싱

![](/images/2021-06-02-17-58-46.png)

- 위의 다이어그램에는 외부로부터 443/tcp 를 수신하는 Layer 4 로드 밸런서가 있다. 이 로드 밸런서는 6443/tcp 를 통해 두 개의 controlplane 호스트로 트래픽을 전달한다. 이렇게 하면 하나의 controlplane 호스트 장애가 발생하더라도 사용자가 Kubernetes API 에 계속 액세스 할 수 있다.
- RKE에는 kube-apiserver cert SAN list 에 hostname 을 추가할 수 있는 기능이 있다. 이는 API 서버 트래픽을 serving 하는 노드의 hostname 이나 IP 와 다른 값을 가진 외부 로드 밸런서를 사용할 때 유용하다. 이 기능 구성에 대한 상세 내용은 [인증에 대한 RKE 문서](https://rancher.com/docs/rke/latest/en/config-options/authentication/) 에서 찾을 수 있다.
- 예를 들어, 다이어그램에 표시된 예제의 경우, kube-apiserver 목록에 api.mycluster.example.com 로드 밸런서를 포함하는 RKE `cluster.yml` 구성을 아래와 같이 해야한다.

  `cluster.yml`  

  ``` yaml
  authentication:
    strategy: x509
    sans:
      - "api.mycluster.example.com"
  ```

- 이후에 L4 로드 밸런서를 추가하는 경우, `kube-apiserver.pem` 인증서의 SAN list 에 추가적인 hostname 을 적절히 추가하기 위해 [RKE certificate rotation](https://rancher.com/docs/rke/latest/en/cert-mgmt/#rotating-a-certificate-on-an-individual-service-while-using-the-same-ca) 을 수행하는 것이 중요하다.
- 또한 `kube_config_cluster.yml` 파일은 로드 밸런서를 통해 API 서버에 액세스하는게 아닌 목록의 첫 번째 컨트롤 플레인 노드를 통해 액세스하도록 구성된다. L4 API 서버 로드 밸런서를 `server` 값으로 포함하는 사용자가 활용할 수 있도록 별도의 kube_config 파일을 생성해야 한다.

# 2. kube-apiserver 의 클러스터 내부 user 로드밸런싱

- 기본적으로 rke 는 10.43.0.1 을 내부 Kubernetes API 서버 엔드포인트로 지정한다(기본 서비스 클러스터 CIDR 10.43.0.0/16을 기반으로 함). Kubernetes API 서버에 지정된 IP 로 resolve 되는 default 네임 스페이스에 kubernetes 라는 ClusterIP 서비스 및 엔드포인트가 생성된다.
- 기본적으로 rke 클러스터에서 ClusterIP 는 `iptables` 를 사용하여 부하 분산된다. 구체적으로는 `NAT pre-routing` 을 사용하여 사용 가능한 API 서버 호스트 수에 따라 결정되는 확률로 원하는 Kubernetes API 서버 엔드 포인트로 트래픽을 보낸다. 이는 내부적으로 Kubernetes API 서버에 대한 고가용성 솔루션을 제공하며, 파드 내에서 API 서버에 연결하는 라이브러리는 재시도를 통해 failover 할 수 ​​있다.

## 2.1. kube-apiserver의 kubelet / kube-proxy 로드밸런싱 : nginx-proxy

- 각 Kubernetes 클러스터 노드의 kubelet 과 kube-proxy 구성 요소는 127.0.0.1:6443 에 연결하도록 rke 에 의해 구성된다. 그러면, 워커 노드에서 127.0.0.1:6443 를 어떻게 resolve 할 수 있을까? 그 이유는 각 컨트롤플레인이 아닌 노드들에 `nginx-proxy` 컨테이너가 있기 때문이다. `nginx-proxy` 는 health checking 을 통해 컨트롤 플레인 노드 IP 에서 L4 라운드 로빈 로드 밸런싱을 수행하는 간단한 컨테이너로, 일시적인 오류가 발생하더라도 노드가 계속 작동할 수 있도록 한다.

## nginx-proxy 상태 확인 방법

- 앞서 말했듯이 nginx-proxy 컨테이너는 컨트롤 플레인 역할이 없는 모든 노드에 배포된다. 정상적으로 구동중인 컨트롤 플레인 역할이 있는 노드들에 대해 nginx configuration 을 동적으로 생성하여 이 노드들에 액세스를 제공한다.
- 컨트롤 플레인 역할이 없는 노드들에서 nginx-proxy 컨테이너 확인

  ``` bash
  $ docker ps -a -f=name=nginx-proxy
  CONTAINER ID   IMAGE                       COMMAND                  CREATED      STATUS      PORTS     NAMES
  099cf1cff6ca   rancher/rke-tools:v0.1.74   "nginx-proxy CP_HOST…"   7 days ago   Up 7 days             nginx-proxy
  ```

- 동적으로 생성된 nginx configuration 확인

  ``` bash
  $ docker exec nginx-proxy cat /etc/nginx/nginx.conf
  error_log stderr notice;

  worker_processes auto;
  events {
    multi_accept on;
    use epoll;
    worker_connections 1024;
  }

  stream {
          upstream kube_apiserver {

              server 10.231.238.244:6443;

              server 10.231.238.242:6443;

              server 10.231.238.243:6443;

          }

          server {
              listen        6443;
              proxy_pass    kube_apiserver;
              proxy_timeout 30;
              proxy_connect_timeout 2s;

          }

  }
  ```

- nginx-proxy 의 log 확인

  ``` bash
  $ docker logs nginx-proxy
  ```

# 3. etcd의 고가용성

- etcd는 배포 될 때 고 가용성 기능이 내장되어 있다. 고가용성 Kubernetes 클러스터를 배포 할 때 etcd 가 쿼럼을 달성하고 리더를 설정할 수 있는 다중 노드 구성에 배포되었는지 확인하는 것이 매우 중요하다.
- 고 가용성 etcd 클러스터를 계획 할 때 유의해야 할 몇 가지 측면이 있다.
  - 노드 수
  - 디스크 I / O 용량
  - 네트워크 지연 및 처리량

## 3.1. quorum (정족수)

- 쿼럼을 달성하기 위해 etcd 는 다수의 구성원이 사용 가능하고 서로 통신해야합니다. 따라서 etcd 에는 홀수의 멤버가 가장 적합합니다. 구성원 수가 증가함에 따라 장애 허용도 향상됩니다. 하지만 구성원 수가 많은 것이 항상 좋은 것은 아닙니다. 구성원이 너무 많으면 etcd 가 구성원간에 쓰기를 전파하는 데 사용하는 Raft 합의 알고리즘으로 인해 실제로 속도가 느려질 수 있습니다.
- 총 노드 수와 허용 할 수 있는 노드 장애 수를 비교하는 표는 다음과 같습니다.

| RECOMMENDED FOR HA | TOTAL ETCD MEMBERS | NUMBER OF FAILED NODES TOLERATED |
|--------------------|--------------------|----------------------------------|
| No                 | 1                  | 0                                |
| No                 | 2                  | 0                                |
| Yes                | 3                  | 1                                |
| No                 | 4                  | 1                                |
| Yes                | 5                  | 2                                |

## 3.2. etcd 배포 아키텍처

- 고 가용성 클러스터에서 rke 를 사용하여 여러 etcd 호스트를 배포 할 때 일반적으로 허용되는 두 가지 아키텍처가 있습니다. 하나는 etcd 가 컨트롤 플레인 구성 요소와 함께 배치되어 컴퓨팅 리소스의 최적화 된 사용을 허용하는 곳입니다. 이는 일반적으로 컴퓨팅 리소스가 제한 될 수 있는 중소 규모 클러스터에만 권장됩니다. 이 구성은 etcd 가 주로 메모리 기반 (메모리 내에서 작동하므로)으로 작동하는 반면 제어 플레인 구성 요소는 일반적으로 계산 집약적입니다. 구성 다이어그램은 다음과 같습니다.

![](/images/2021-06-14-11-49-33.png)

- 중요한 프로덕션 환경에서는 etcd 를 실행하는 데 이상적인 하드웨어가 있는 전용 노드에서 etcd 를 실행하는 것이 좋습니다. 분리된 구성의 다이어그램은 다음과 같습니다.

![](/images/2021-06-14-14-10-24.png)

- 이 아키텍처는 다른 컨트롤 플레인 구성 요소와 함께 배치되지 않은 전용 외부 etcd 클러스터에 의존합니다. 이는 일부 추가 노드를 운영하는 대신 더 큰 중복성과 가용성을 제공합니다.

# 4. Ingress (L7) 고려 사항

- Kubernetes Ingress 객체를 사용하면 Kubernetes 클러스터 내에서 호스팅되는 애플리케이션 사용자에게 트래픽을 제공하기 위해 호스트 및 경로 기반 경로를 지정할 수 있습니다.

## 4.1. Ingress controller 네트워킹

- ingress 컨트롤러를 설치할 때 선택할 수 있는 두 가지 일반 네트워킹 구성이 있습니다.
  - Host network
  - Cluster network
- 첫 번째 모델 인 Host network 는 ingress controller 가 호스트와 동일한 네트워크 네임 스페이스의 노드 집합에서 실행되는 곳입니다. 이렇게하면 호스트에서 직접 인그레스 컨트롤러의 포트 80 및 443 이 노출됩니다. 외부 클라이언트에게는 호스트에 80 / tcp 및 443 / tcp 를 수신하는 웹 서버가 있는 것으로 보입니다.
- 두 번째 옵션 인 클러스터 네트워크는 인그레스 컨트롤러가 클러스터 내의 워크로드와 동일한 클러스터 네트워크에서 실행되는 곳입니다. 이 배포 모델은 LoadBalancer 유형의 서비스를 사용하거나 NodePort 서비스를 사용하여 호스트의 기능을 다중화하는 동시에 인그레스 컨트롤러가 호스트의 네트워크 네임 스페이스를 공유하지 않도록 격리 플레인을 제공 할 때 유용합니다.
- 이 가이드에서는 rke 가 기본적으로 이러한 방식으로 인그레스 컨트롤러를 구성하므로 호스트 네트워크에서 작동하도록 인그레스 컨트롤러를 배포하는 옵션을 살펴 보겠습니다.

## 4.2. 인그레스 컨트롤러 배포 모델

- 기본적으로 rke 는 Kubernetes 클러스터의 모든 작업자 노드에서 실행되는 DaemonSet 로 Nginx 수신 컨트롤러를 배포합니다. 그런 다음 이러한 작업자 노드를 로드 밸런싱 하거나 노드에 트래픽을 보내기 위해 구성된 동적 또는 라운드 로빈 DNS 레코드를 가질 수 있습니다. 이 모델에서 애플리케이션 워크로드는 수신 컨트롤러와 함께 배치됩니다.

![](/images/2021-06-14-14-20-37.png)

- 이는 대부분의 중소 규모 클러스터에서 작동하지만 프로파일링 되지 않거나 이기종이 아닌 워크로드를 실행할 때 CPU 또는 메모리 경합으로 인해 수신 컨트롤러가 트래픽을 제대로 제공하지 못할 수 있습니다. 이러한 시나리오에서는 수신 컨트롤러를 실행할 특정 노드를 지정하는 것이 좋습니다. 이 모델에서는 여전히 라운드 로빈 DNS 또는 동적 DNS를 수행 할 수 있지만이 경우로드 균형 조정이 더 선호되는 솔루션입니다.

![](/images/2021-06-14-14-26-48.png)

- 현재, rke 는 인그레스 컨트롤러의 스케줄링을 제어하기 위해 `node selector` 설정만 지원한다. 하지만, 더 세분화 된 인그레스를 허용하는 노드에 taint 를 배치 할뿐만 아니라 인 그레스 컨트롤러에 feature request 를 가져 오는 기능 요청이 열려 있습니다. 컨트롤러 배포.