**목차**

- [1. 요약](#1-요약)
- [2. HA k8s 의 필요성](#2-ha-k8s-의-필요성)
- [3. HA k8s 구성 방안](#3-ha-k8s-구성-방안)
- [4. HA k8s 구성 요소](#4-ha-k8s-구성-요소)
  - [4.1. 내부 로드밸런서](#41-내부-로드밸런서)
    - [4.1.1. 내부 로드밸런서란?](#411-내부-로드밸런서란)
    - [4.1.2. 내부 로드밸런서의 용도](#412-내부-로드밸런서의-용도)
  - [4.2. 외부 로드밸런서](#42-외부-로드밸런서)
    - [4.2.1. 외부 로드밸런서란?](#421-외부-로드밸런서란)
    - [4.2.2. 외부 로드밸런서의 용도](#422-외부-로드밸런서의-용도)
- [5. HA k8s using kubeadm with Keepalived+HAProxy on systemd](#5-ha-k8s-using-kubeadm-with-keepalivedhaproxy-on-systemd)
- [6. HA rke using rancher with nginx-proxy container](#6-ha-rke-using-rancher-with-nginx-proxy-container)

**참고**

- [rancher docs : rke component diagram](https://rancher.com/docs/rancher/v2.x/en/troubleshooting/kubernetes-components/)
- [rancher blog : Building a Highly Available Kubernetes Cluster](https://rancher.com/learning-paths/building-a-highly-available-kubernetes-cluster/)

# 1. 요약

- HA 구성을 하면 운영 안정성이 증가한다. 예를 들어, 컨트롤 플레인 노드가 3 개일 경우, etcd 의 quorum 구조에 따라 최대 1대 장애에 내결함성이 있다. 즉, 2대 노드 장애 시에 클러스터 장애로 이어진다. 또한 로드밸런싱을 통해 부하가 많은 kube-apiserver 의 안정성을 증가시킬 수 있다.
- HA k8s 구성 시에 외부 로드밸런서 혹은 내부 로드 밸런서가 필요하다. 외부 로드밸런서는 추가 구축이 필요하고 내부 로드밸런서는 k8s 구축 시에 부트스트랩된다.
- HA k8s 는 k8s 배포판에 따라 구성 방법이 다양하다. 크게 두 가지 방법이 있다. 첫 째, 범용적인 kubeadm 을 사용하면 keepalived+haproxy 등을 통한 외부 로드밸런서가 필요하다. 둘 째, rancher 기반 rke 배포판을 사용하면 nginx-proxy 가 자동 배포되어 외부 로드밸런서가 필요없다. 다만, rancher 서버에 대한 추가적인 리소스가 필요하며 rke 배포판 특성을 고려해야한다.

# 2. HA k8s 의 필요성

- k8s 를 프로덕션 환경에서 운영 안정성을 위해 일반적으로 **HA(High Availability, 고가용성)** 구성을 한다. 즉, control plane 노드, etcd 인스턴스, worker 노드를 각각 여러 대 두어 다중화 혹은 서버 복제라고 하는 **redundancy** 를 통해 이 중에서 몇 대가 장애 발생하더라도 클러스터가 계속 작동 할 수 있는 **fault-tolerance** 기능이 필요하다.
- fault-tolerance 는 장애내결함성으로 클러스터 구조에서 노드 일부가 죽어도 클러스터 운영을 이어갈 수 있는 정도를 나타낸다.
- HA 컨트롤 플레인은 etcd 가 계속 작동하기 위해 quorum 구조(n/2+1)를 형성 할 수 있어야 한다.
- etcd 클러스터 크기에 따른 내결함성

  | etcd Cluster Size | Majority | Failure Tolerance |
  |-------------------|----------|-------------------|
  | 1                 | 1        | 0                 |
  | 2                 | 2        | 0                 |
  | 3                 | 2        | 1                 |
  | 4                 | 3        | 1                 |
  | 5                 | 3        | 2                 |
  | 6                 | 4        | 2                 |
  | 7                 | 4        | 3                 |
  | 8                 | 5        | 3                 |
  | 9                 | 5        | 4                 |

# 3. HA k8s 구성 방안

- HA k8s using kubeadm with Keepalived+HAProxy on systemd
- HA k8s using kubeadm with Keepalived+HAProxy on static pod
- HA k8s using kubeadm with kube-vip on static pod
- HA rke using rancher with nginx-proxy container

# 4. HA k8s 구성 요소

## 4.1. 내부 로드밸런서

### 4.1.1. 내부 로드밸런서란?

- 클러스터 네트워크 안에서 사용할 수 있는 로드밸런서로 `kubernetes.default.svc` 오브젝트이다.

### 4.1.2. 내부 로드밸런서의 용도

- 클러스터 내부 파드에서 kube-apiserver 로 통신할 때 ClusterIP 혹은 DNS 엔드포인트로 사용되며 트래픽은 자동으로 로드밸런싱이 된다.

## 4.2. 외부 로드밸런서

### 4.2.1. 외부 로드밸런서란?

- 클러스터 네트워크 밖에서 사용할 수 있는 로드밸런서로 쿠버네티스 외에 별도로 구축을 해줘야 한다. 외부 ㅌ로드밸런서의 구축 방법은 다양하다.
- 외부 로드밸런서 구축 방법
  - HW 사용
  - SW 사용
    - Keepalived+HAProxy on systemd
    - Keepalived+HAProxy on static pod
    - kube-vip on static pod
    - nginx-proxy container

### 4.2.2. 외부 로드밸런서의 용도

- 클러스터 외부의 사용자, 관리자가 kubectl 혹은 https 를 통해 kube-apiserver 로 통신할 때 L4 endpoint 로 사용되며 트래픽은 자동으로 로드밸런싱 된다.
- 워커 노드 kubelet 에서 kube-apiserver 로 통신할 때 L4 endpoint 로 사용되며 트래픽은 자동으로 로드밸런싱 된다. 만약, 외부 로드밸런서가 없다면 워커 노드는 단일 API 엔드 포인트를 통해 컨트롤 플레인과 통신한다. 그런데, 이 kube-apiserver 가 장애날 경우, 바인딩 된 워커 노드에 cascading failure(단계적인 실패)가 발생하며 이는 고가용성에 장애가 된다. 그러므로 kubelet 을 위한 외부 로드밸런서가 필요하다.

# 5. HA k8s using kubeadm with Keepalived+HAProxy on systemd

![](/images/2021-06-16-18-04-25.png)

- keepalived 는 외부 로드밸런서의 VIP 를 관리한다. 3개의 컨트롤 플레인 노드 위에 systemd 로 구축하여 keepalived 클러스터를 HA 로 구성했다. 이로 인해, VIP 에 대한 Master 가 죽었을 때 우선 순위에 따라 BACKUP 서버가 VIP 를 관리하게 된다.
- HAProxy 는 외부 로드밸런서의 L4 로드밸런싱 기능을 한다. 3개의 컨트롤 플레인 노드 위에 systemd 로 구축하여 HAProxy 클러스터를 HA 로 구성했다. 이로 인해, 8443 포트로 들어오는 트래픽을 각 노드에 6443 포트로 로드밸런싱해준다.
- kubernetes.default.svc 는 ClusterIP 타입의 svc 이고 내부 로드밸런서로써 endpoint 가 apiserver 들이다. 즉, pod 등의 클러스터 내부 컴포넌트들이 kube-apiserver 와 로드밸런싱 기반의 통신이 가능하다.

# 6. HA rke using rancher with nginx-proxy container

![](/images/2021-06-16-17-42-32.png)

- rancher 를 통해 rke 기반으로 HA k8s 클러스터를 구축하면 컨트롤플레인 역할이 아닌 워커 노드들에 nginx-proxy 컨테이너도 배포된다.
- nginx-proxy 는 API 서버에 대한 로드밸런서이다.
- rke 는 kubelet 컴포넌트를 컨테이너로 올리는데 kubelet 설정에서 kube-apiserver 의 엔드포인트를 127.0.0.1:443 으로 한다. kubelet 과 nginx-proxy 는 Host 네트워크를 사용한다. 그러므로 nginx-proxy 는 127.0.0.1:443 에 대한 트래픽을 L4 로드밸런싱을 해주는데 도착지를 컨트롤플레인 노드로 `nginx.conf` 를 동적으로 설정해준다.
- kubernetes.default.svc 는 ClusterIP 타입의 svc 이고 내부 로드밸런서로써 endpoint 가 apiserver 들이다. 즉, pod 등의 클러스터 내부 컴포넌트들이 kube-apiserver 와 로드밸런싱 기반의 통신이 가능하다.