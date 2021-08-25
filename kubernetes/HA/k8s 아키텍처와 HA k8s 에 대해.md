**목차**

- [1. 기본 k8s 클러스터 아키텍처](#1-기본-k8s-클러스터-아키텍처)
- [2. k8s 의 scalability and availability](#2-k8s-의-scalability-and-availability)
  - [2.1. node plane 의 scalability 와 availability (노드 플레인의 확장성 및 가용성)](#21-node-plane-의-scalability-와-availability-노드-플레인의-확장성-및-가용성)
  - [2.2. control plane 의 scalability 와 availability (컨트롤 플레인의 확장성 및 가용성)](#22-control-plane-의-scalability-와-availability-컨트롤-플레인의-확장성-및-가용성)
- [3. HA k8s 클러스터 아키텍처](#3-ha-k8s-클러스터-아키텍처)
- [4. HA 구성 시에 몇 가지 추가 고려 사항](#4-ha-구성-시에-몇-가지-추가-고려-사항)
- [5. HA control plane 이 필요할까?](#5-ha-control-plane-이-필요할까)
- [6. HA k8s 클러스터에서 외부 로드밸런서가 필요한 이유](#6-ha-k8s-클러스터에서-외부-로드밸런서가-필요한-이유)

**참고**

- [Setting up highly available Kubernetes clusters](https://elastisys.com/wp-content/uploads/2018/01/kubernetes-ha-setup.pdf)
- [k8s docs - 클러스터 접근](https://kubernetes.io/ko/docs/tasks/access-application-cluster/access-cluster/#파드에서-api-접근)

# 1. 기본 k8s 클러스터 아키텍처

![](/images/2021-06-12-21-21-53.png)

- Kubernetes 는 **master component** 와 이 컴포넌트가 지시한 애플리케이션 워크로드(컨테이너)를 실행하는 **node component** 로 이뤄진 마스터-슬레이브 유형의 아키텍처이다.
- master component(마스터 구성 요소)는 클러스터의 상태를 관리한다. 여기에는 클라이언트 요청(desired state)을 수락하고 컨테이너 예약 및 실제 클러스터 상태를 원하는 상태로 유도하기(desired state) 위한 제어 루프(control loop) 실행이 포함된다. 이러한 구성 요소는 다음과 같다.
- master component 구성 요소
  
  - apiserver : API 객체(e.g. pod, deployment, svc)에 대한 기본 CRUD 작업을 지원하는 REST API 이다. 이 apiserver 는 클러스터 관리자가 kubectl 을 사용하여 통신하는 엔드 포인트이다. apiserver 는 그 자체로 상태비저장(stateless)이다. 대신 모든 클러스터 상태를 저장하기 위한 백엔드로 분산 key-value 스토리지 시스템(etcd)을 사용한다.
  - controller manager : apiserver 에서 원하는 상태(desired state)를 감시하고 실제 상태를 원하는 상태로 이동하려고 시도하는 제어/조정루프를 실행한다. 내부적으로는 다양한 컨트롤러로 구성되며, replication controller 는 각 배포에 대해 적절한 수의 복제본 포드가 실행되도록 하는 눈에 띄는 컨트롤러이다.
  - scheduler : 사용 가능한 노드 집합에서 포드 배치를 처리하여 클러스터 노드에 과도한 부하를 주지 않도록 리소스 소비의 균형을 맞추는 일을 한다. 또한 (anti-)affinity rules 과 같은 사용자 예약 제한을 고려한다.

- node component(노드 구성 요소) 는 모든 클러스터 노드에서 실행된다. 여기에는 다음이 포함된다.
- node component 구성 요소

  - continer runtime : 노드에서 컨테이너를 실행하는 Docker 와 같은 것들이다.
  - kubelet : control plane 의 scheduling 에 따라 노드에서 컨테이너(포드)를 실행하고 해당 포드의 health 를 보장(e.g. 실패한 포드를 자동으로 다시 시작함)한다.
  - kube-proxy : svc abstraction(서비스 추상화)을 구현하는 네트워크 프록시 / 로드 밸런서이다. 노드에서 iptables 규칙을 프로그래밍하여 svc IP request 를 등록된 백엔드 포드 중 하나로 리디렉션한다.

- 마스터 구성 요소를 클러스터의 특정 호스트에서 실행하기 위한 공식적인 요구 사항은 없지만 일반적으로 이러한 control plane 컴포넌트는 함께 그룹화 되어 하나 이상의 마스터 노드를 실행한다. 전용 machine 에서 실행되도록 설정할 수 있는 etcd 를 제외하고 이러한 마스터 노드는 일반적으로 모든 구성 요소(control plane master component 및 node component 모두)를 포함하지만 control plane을 실행하는 데 전념한다. 애플리케이션 워크로드를 처리하지 않는다.(일반적으로 tainted 된다.) 다른 노드는 일반적으로 worker node (또는 그냥 node 라고 함)로 지정되며 node component 만 포함한다.

# 2. k8s 의 scalability and availability

- 일반적인 요구 사항은 Kubernetes 클러스터가 증가하는 워크로드를 수용하기 위해 scale(확장) 되고 fault-tolerant(내결함성)이 있어야 하며 장애(데이터 센터 중단, 머신 장애, 네트워크 파티션)가 있는 경우에도 계속 사용할 수 있어야 한다는 것이다.

## 2.1. node plane 의 scalability 와 availability (노드 플레인의 확장성 및 가용성)

- 클러스터는 워커 노드를 추가하여 확장할 수 있으며, 이는 클러스터의 워크로드 용량을 증가시켜 Kubernetes 가 컨테이너를 예약할 수 있는 더 많은 공간을 제공한다.
- Kubernetes 는 항상 노드를 추적하고 있는데 만약, 노드가 유실된 것으로 간주 된다면(더 이상 마스터에 하트 비트 상태 메시지를 전달하지 않는 상태), control plane 이 누락된 노드의 포드를 여전히 접근가능한 다른 노드에 다시 예약 할 수 있다는 점에서 self-healing(자동 복구) 한다. 따라서 클러스터에 더 많은 노드를 추가하면 Kubernetes가 실패한 노드에서 새 노드로 포드를 더 자유롭게 다시 예약 할 수 있으므로 클러스터의 내결함성이 향상된다.
- 클러스터에 노드를 추가하는 작업은 일반적으로 클러스터 관리자가 판단했을 때, 클러스터가 과도하게 로드 되거나 추가적인 포드가 환경에 맞지 않음을 감지 할 때 수동으로 수행하면 된다. 하지만, 클러스터 크기를 수동으로 모니터링하고 관리하는 것은 지루하고 구식이다. 그래서 autoscaling(자동 확장)을 사용하여 이 작업을 자동화 할 수 있다. [Kubernetes cluster-autoscaler](https://github.com/kubernetes/autoscaler/tree/master/cluster-autoscaler) 가 그러한 솔루션 중 하나이다. 하지만 퍼블릭 클라우드에서만 지원 된다는 단점이 있다. 그리고 지원하는 기능은 포드가 예약되지 않을 때마다 클러스터를 확장하는 단순한 기능을 제공한다.

## 2.2. control plane 의 scalability 와 availability (컨트롤 플레인의 확장성 및 가용성)

- 사실 워커 노드를 더 추가한다고해서 모든 종류의 오류에 대해 클러스터가 회복되는 것은 아니다. 예를 들어, 마스터 API 서버가 다운되면 (e.g. 머신 장애 또는 네트워크 파티션으로 인해 클러스터의 나머지 부분에서 차단됨) 더 이상 kubectl을 통해 클러스터를 제어 할 수 없다.
- 진정한 고 가용성 클러스터의 경우 컨트롤 플레인 컴포넌트도 복제해야 한다. 이러한 컨트롤 플레인은 복제 요소에 따라 하나 또는 몇 개의 노드에 장애가 발생하더라도 도달 가능하고 기능을 유지할 수 있다.
- HA 컨트롤 플레인 설정은 etcd 가 계속 작동하기 위해 quorum, 쿼럼(과반수, majority, (n/2)+1)을 형성 할 수 있어야 하므로 마스터 하나의 손실을 견디기 위해 최소 3 개의 마스터가 필요하다.

# 3. HA k8s 클러스터 아키텍처

- 복제 갯수가 3 인 HA 클러스터는 다음 도식 이미지에 표시된대로 실현할 수 있다.
- HA k8s 클러스터 아키텍처
  
  ![](/images/2021-06-12-23-05-09.png)

- Kubernetes 구성 요소의 loosely coupled 된 특성을 고려할 때 HA 클러스터는 여러 방법으로 실현 될 수 있다. 그러나 일반적으로 몇 가지 일반적인 지침이 있다.
- HA 클러스터 구현 방법 지침

  - replicated, distributed 된 etcd 스토리지 계층이 필요.
  - 여러 머신에 걸쳐 apiserver 를 replicate 하고 로드 밸런서를 앞에 배치.
  - controller 와 scheduler 를 replicate 하고 leader-election 설정.
  - 로드 밸런서를 통해 apiserver 에 액세스하도록 (워커) 노드의 kubelet 및 kube-proxy 를 구성.

- 사용할 복제 요소는 달성하려는 가용성 수준에 따라 다르다. 예를 들어, 세 세트의 마스터 구성 요소를 사용한 클러스터는 한 마스터 노드의 장애를 허용 할 수 있다. 이 경우, etcd 는 쿼럼 (노드 과반수)을 형성하고 계속 작업 할 수 있는 두 개의 라이브 멤버가 필요하기 때문이다. 아래에 표는 다양한 etcd 클러스터 크기의 내결함성을 제공한다.
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

# 4. HA 구성 시에 몇 가지 추가 고려 사항

  - etcd 는 클러스터 상태를 모든 etcd 노드에 복제한다. 따라서 모든 데이터가 손실 되려면 세 노드 모두 동시에 디스크 오류가 발생해야 한다. 혹은 머신/VM 의 수명주기와 분리된 별도의 디스크(e.g. AWS EBS 볼륨) 를 사용하여 스토리지 계층을 더욱 안정적으로 만들어도 된다. 아니면 RAID 설정을 사용하여 디스크를 미러링하고 정기적인 백업을 수행하도록 etcd 를 설정할 수도 있다.
  - etcd 복제본을 별도의 전용 머신에 배치하여 성능 향상을 할 수 있다.
  - 로드 밸런서는 apiserver 의 상태를 모니터링하고 트래픽을 라이브 서버로만 전달해야 한다.
  - 데이터 센터 (AWS 용어의 availability zone)에 마스터를 분산하여 클러스터의 전체 가동 시간을 늘린다. 마스터가 서로 다른 zone 에 있는 경우, 클러스터는 특정 availability zone 의 중단을 허용 할 수 있다.
  - (워커) 노드의 kubelet 및 kube-proxy 는 특정한 마스터 인스턴스 엔드포인트가 아닌 로드 밸런서 엔드포인트를 통해 apiserver 에 액세스 해야 한다.
  - 로드 밸런서는 SPOF(Single Point of Failure, 단일 장애 지점) 가 되어서는 안 된다. 대부분의 클라우드 공급자는 fault-tolerant load balancer 서비스를 제공 할 것이다. 온 프레미스 설정의 경우, failover(장애 조치)가 필요할 때, keepalived 에 의해 재할당되는 virtual/floating IP 로 active/passive nginx/HAProxy 설정을 사용할 수 있다.

# 5. HA control plane 이 필요할까?

- 성공적인 Kubernetes 클러스터를 실행하기 위해 HA 솔루션이 반드시 필요한 것은 아니다. HA 설정을 결정하기 전에 HA 클러스터의 요구 사항과 필요성, 실행하려는 워크로드 등을 고려해야한다. 그리고 HA 구성은 더 많은 자원이 포함되므로 비용이 많이 든다는 점을 고려해야한다. 단일 마스터는 대부분의 경우 매우 잘 작동한다. 대부분의 클라우드에서 머신 장애는 거의 발생하지 않으며 가용성 영역 중단도 마찬가지이다.
- 다소 정적 워크로드가있는 경우 1 년에 몇 시간의 다운 타임이 허용 될 수 있습니다. 또한 Kubernetes 의 loosely coupled 된 아키텍처는 좋은 점으로써 마스터가 다운 되더라도 워커 노드가 작동 상태를 유지하고 기존에 실행하도록 지시 됐던 모든 것들을 계속 실행한다는 것이다. 따라서 워커 노드, 특히 다른 zone 에 분산된 경우, 마스터가 다운 된 경우에도 애플리케이션 서비스를 계속 제공 할 수 있다.
- 단, 단일 마스터를 실행하기로 결정한 경우, etcd 데이터가 안정적으로 저장되었는지 확인해야하며 주기적으로 백업하는 것이 좋다. 단일 마스터 Kubernetes 클러스터를 배포 할 때, 일반적으로 두 개의 디스크를 마스터에 마운트한다. 하나는 etcd 데이터 디렉토리를 보유하고 다른 하나는 스냅샷을 보유하는 디스크이다. 이런 방식으로 장애가 발생한 마스터 노드를 교체 할 수 있으며 (교체 노드에 할당할 수 있는 고정 IP를 사용해야한다) etcd 디스크를 교체 노드에 연결할 수 있다. 또한 디스크 손상시 백업 디스크에서 백업을 복원 할 수 있다.
- 결국 이는 추가 서버를 계속 실행하는 비용과 항상 마스터를 사용할 수 있어야한다는 필요성을 절충해야하는 균형 잡기 작업이다.

# 6. HA k8s 클러스터에서 외부 로드밸런서가 필요한 이유

- 파드에서 api 서버로 접근할 때는 kubernetes svc 를 사용하여 자동으로 로드밸런싱이 된다. 더 자세히는 `kubernetes.default.svc` DNS 네임을 사용한다. 이 DNS 네임을 api서버로 라우팅되는 서비스 IP로 resolve 된다. 이를 내부 로드밸런서라고 한다.
- 하지만, 이러한 내부 로드밸런서는 클러스터 외부에서 api서버로 접근하는, 예를 들어, kubectl 등으로 노드에 접근할 때 사용할 수 없어 외부 로드밸런서가 필요하다.
- 또 다른 예로 워커 노드 kubelet 에서 api 서버로의 접근이 있다. 외부 로드밸런서가 없다면 워커 노드는 단일 API 엔드 포인트를 통해 컨트롤 플레인과 통신한다. 그런데, 이 kube-apiserver 가 죽으면 바인딩 된 워커 노드에 kubelet 이 api서버를 업데이트 할 수 없고 cascading failure(단계적인 실패)가 발생하며 이는 고가용성에 장애가 된다. 그러므로 kubelet 을 위한 외부 로드밸런서가 필요하다.
- 그 외에도 클러스터에 기능적인 API 엔드 포인트가 없으면 클러스터가 중지된다. 예를 들어, kube-controller-manager 는 다양한 제어 개체에서 작동 할 수 없다.