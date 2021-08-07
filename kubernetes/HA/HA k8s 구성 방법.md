**요약**

- ha k8s 구성을 위해 외부 및 내부 로드밸런서가 필요한 이유와 작동 방식을 알아보자.
- ha k8s 를 구축하는 많은 방법들 중에서 stack k8s, keepalived + haproxy on service 방법을 검증해보자.
- 그 외에 static pod 로 올리는 법과 kube-vip 방법은 검토까지만 해보자.

**목차**

- [1. 배경](#1-배경)
  - [1.1. 외부 로드밸런서가 필요한 이유](#11-외부-로드밸런서가-필요한-이유)
  - [1.2. 운영 환경의 HA k8s Cluster 와 redundancy(다중화)](#12-운영-환경의-ha-k8s-cluster-와-redundancy다중화)
  - [1.3. HA k8s 구성 방법 : kubeadm + 로드밸런서](#13-ha-k8s-구성-방법--kubeadm--로드밸런서)
  - [1.4. 로드밸런서 종류](#14-로드밸런서-종류)
  - [1.5. keepalived 와 haproxy](#15-keepalived-와-haproxy)
  - [1.6. keepalived 설정 방법](#16-keepalived-설정-방법)
    - [1.6.1. service configuration](#161-service-configuration)
    - [1.6.2. health check 스크립트](#162-health-check-스크립트)
  - [1.7. haproxy 설정 방법](#17-haproxy-설정-방법)
- [2. HA k8s 클러스터 구축해보기 (실습)](#2-ha-k8s-클러스터-구축해보기-실습)
  - [2.1. stacked 로 OS 위에 keepalived + haproxy service 올리기](#21-stacked-로-os-위에-keepalived--haproxy-service-올리기)
    - [2.1.1. 아키텍처](#211-아키텍처)
    - [2.1.2. 전제](#212-전제)
    - [2.1.3. 패키지 설치](#213-패키지-설치)
    - [2.1.4. keepalived 설정](#214-keepalived-설정)
    - [2.1.5. haproxy 설정](#215-haproxy-설정)
    - [2.1.6. 적용 확인](#216-적용-확인)
    - [2.1.7. 클러스터 부트스트랩](#217-클러스터-부트스트랩)
    - [2.1.8. 구성 완료 후 테스트](#218-구성-완료-후-테스트)
  - [2.2. stacked, static pods 로 keepalived + haproxy 서비스 올리기](#22-stacked-static-pods-로-keepalived--haproxy-서비스-올리기)
  - [2.3. kube-vip](#23-kube-vip)

**참고**

- [GitHub kubernetes/kubeadm - ha-considerations.md](https://github.com/kubernetes/kubeadm/blob/master/docs/ha-considerations.md#options-for-software-load-balancing)
- [k8s docs - Creating Highly Available clusters with kubeadm](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/high-availability/)
- [Blog - How to Setup Kubernetes(k8s) Cluster in HA with Kubeadm](https://www.linuxtechi.com/setup-highly-available-kubernetes-cluster-kubeadm/)
- [k8s docs - 파드에서 API 접근](https://kubernetes.io/ko/docs/tasks/access-application-cluster/access-cluster/#파드에서-api-접근)

---

# 1. 배경

## 1.1. 외부 로드밸런서가 필요한 이유

- 파드에서 api 서버로 접근할 때는 kubernetes svc 를 사용하여 자동으로 로드밸런싱이 된다. 더 자세히는 `kubernetes.default.svc` DNS 네임을 사용한다. 이 DNS 네임을 api서버로 라우팅되는 서비스 IP로 resolve 된다.
- 하지만, 이러한 내부 로드밸런서는 클러스터 외부에서 api서버로 접근하는, 예를 들어, kubectl 등으로 노드에 접근할 때 사용할 수 없어 외부 로드밸런서가 필요하다.
- 또 다른 예로 워커 노드 kubelet 에서 api 서버로의 접근이 있다. 외부 로드밸런서가 없다면 워커 노드는 단일 API 엔드 포인트를 통해 컨트롤 플레인과 통신한다. 그런데, 이 kube-apiserver가 실패하면 바인딩 된 워커 노드에 cascading failure(단계적인 실패)가 발생하며 이는 고가용성에 장애가 된다. 그러므로 kubelet 을 위한 외부 로드밸런서가 필요하다.

## 1.2. 운영 환경의 HA k8s Cluster 와 redundancy(다중화)

- k8s 를 프로덕션 환경에서 운영할 때 일반적으로 HA(High Availability, 고가용성) 구성이 필요합니다. 즉, control plane 노드, etcd 인스턴스, worker 노드를 각각 여러 대 두어 다중화 혹은 서버 복제라고 하는 **redundancy(다중화)** 를 통해 이중에서 몇 대가 장애 발생하더라도 클러스터가 계속 작동 할 수 있는 HA 기능이 필요합니다.

## 1.3. HA k8s 구성 방법 : kubeadm + 로드밸런서

- kubeadm은 multi control plane 및 multi etcd 클러스터 설정을 지원합니다. 단계별 설치 방법은 [공식 문서](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/high-availability/)를 참고하면 됩니다. 아래 내용은 이 단계별 설치 방법에 따라 수행했습니다.
- 하지만, 로드밸런서와 같이 HA 구성 방법에 일부는 Kubernetes 범위 내에 내용이 아니므로 도큐먼트에서 다루지 않는 몇 가지 측면을 고려하고 설정해야합니다. 전용 로드밸런싱 장비가 있는 데이터 센터(e.g. 일부 클라우드 업체에서 제공 등) 같은 일부 환경에서는 로드밸런싱 기능을 이미 사용할 수 있습니다. 그렇지 않은 경우 사용자가 관리하는 로드밸런싱을 사용할 수 있습니다. 이 경우 클러스터를 부트 스트랩하기 전에 몇 가지 준비가 필요합니다. 예를 들면, 로드밸런서와 VIP 설정을 해야하는데 그 방법들 중에 Keepalived + HAproxy 등이 있습니다. 이 방법은 [kubernetes/kubeadm GitHub - ha-considerations.md](https://github.com/kubernetes/kubeadm/blob/master/docs/ha-considerations.md#options-for-software-load-balancing) 에 자세히 나와있어 참고하여 진행했습니다. 이 외에도 kube-vip 나 rancher 등의 다양한 로드밸런서를 적용할 수 있습니다. 이는 뒤에서 검토하겠습니다.
- 물론 이 로드밸런서 자체도 HA 구성이어야 합니다. 이 또한 일반적으로 로드밸런서를 다중화(redundancy) 구성 하여 수행됩니다. 즉, VIP(virtual IP. 가상 IP) 를 관리하는 호스트들(로드밸런서의 인스턴스)의 클러스터가 구성됩니다. 다시 말해 클러스터 내에 각 호스트들은 로드밸런서의 인스턴스를 실행하고 있습니다. 이로 인해 현재 VIP 를 보유하고 있는 호스트의 로드 밸런서가 항상 사용되고(active) 나머지는 대기 상태(stand by)에 있습니다.
- 로드밸런서가 준비 됐다면, kubeadm 으로 둘 이상의 control plane 이 있는 클러스터를 부트스트랩핑 합니다. 이 때, `--control-plane-endpoint 'VIP':6443` 옵션을 통해 kube-apiserver 인스턴스를 LB 뒤에 배치하고 `kubeadm init` 을 실행합니다.

## 1.4. 로드밸런서 종류

- 대표적인 예로 keepalived + haproxy 를 사용할 수 있습니다. 구성 방법은 크게 두 가지로 별도의 서버 2 대에 keepalived 와 haproxy 를 모두 설치하는 방법이 있고 controlplane 위에 static pod 로 올리는 방법이 있습니다.
- 그 외에도 keepalived + haproxy 의 대안으로 kube-vip 가 있습니다. 이 또한 static pod 로 구성합니다. keepalived 와 haproxy 를 한 서비스로 구현하는 등 차이점이 있습니다. 자세한 내용은 뒤에서 다뤘습니다.
- rancher 의 rke 경우에는 kube-apiserver 의 로드밸런싱을 nginx-proxy 컨테이너로 수행하며 이는 자동으로 구축됩니다. nginx-proxy 는 health check 를 통해 알려진 control plane 노드 IP 에서 L4 round robin load balancing 을 수행하는 간단한 컨테이너로, 일시적인 오류가 발생하더라도 노드가 계속 작동할 수 있도록 합니다. 자세한 내용은 [여기서](https://rancher.com/learning-paths/building-a-highly-available-kubernetes-cluster/) 볼 수 있습니다.

## 1.5. keepalived 와 haproxy

- virtual IP 에서 로드 밸런싱을 제공하기 위해 keepalived + haproxy 는 오랫동안 사용되어 왔으며 유명합니다.
- keepalived 는 configurable health check 로 관리되는 가상 IP 를 제공합니다. 가상 IP 가 협상되는 모든 호스트는 동일한 IP 서브넷에 있어야합니다.
- haproxy 는 간단한 stream-based 로드 밸런싱 기능을 제공합니다. TLS termination 은 그 뒤에 있는 API 서버 인스턴스에서 처리 할 수 ​​있습니다.
- 이 조합은 운영 체제의 서비스(systemd 등)로 실행되거나 controlplane 호스트의 static pod 로 실행될 수 있습니다. 서비스의 configuration 설정은 static pod 로 띄우나 서비스로 띄우나 두 경우 모두 동일합니다.

## 1.6. keepalived 설정 방법

- keepalived 설정은 파일 2개로 이루어져 있습니다. serivce configuration 파일과 health check 스크립트 입니다.
- 헬스 체크 스트립트는 주기적으로 virual IP 를 갖고 있는 노드가 살아있는 지 확인합니다.

### 1.6.1. service configuration

- 이 파일들은 `/etc/keepalived` 밑에 있어야 합니다.

`/etc/keepalived/keepalived.conf` (keepalived v2.0.17 기준)

``` text
! /etc/keepalived/keepalived.conf
! Configuration File for keepalived
global_defs {
    router_id LVS_DEVEL
}
vrrp_script check_apiserver {
  script "/etc/keepalived/check_apiserver.sh"
  interval 3
  weight -2
  fall 10
  rise 2
}

vrrp_instance VI_1 {
    state ${STATE}
    interface ${INTERFACE}
    virtual_router_id ${ROUTER_ID}
    priority ${PRIORITY}
    authentication {
        auth_type PASS
        auth_pass ${AUTH_PASS}
    }
    virtual_ipaddress {
        ${APISERVER_VIP}
    }
    track_script {
        check_apiserver
    }
}
```

- bash 변수로 아래에 placeholder 들을 설정해주면 됩니다.
- `${STATE}` : 값을 MASTER, BACKUP 으로 넣으면 되고 서버 하나에 MASTER 를 넣고 나머지는 BACKUP 을 넣어 가상 IP가 처음에 MASTER에 할당되게 합니다.
- `${INTERFACE}` : 가상 IP negotiation 에 참여하는 네트워크 인터페이스입니다. e.g. `eth0`.
- `${ROUTER_ID}` : 숫자를 값으로 갖으며 이 값은 모든 keepalived 클러스터 호스트들에서 동일해야합니다. 또한 해당 서브넷의 모든 클러스터에서 고유해야합니다. 대부분의 배포판에는 해당 값을 51로 미리 구성해 놨습니다.
- `${PRIORITY}` : 백업보다 컨트롤 플레인 노드에서 더 높아야합니다. 따라서 101과 100은 각각 충분합니다.
- `${AUTH_PASS}` : 모든 keepalived 클러스터 호스트들에서 동일해야합니다. e.g. 42
- `${APISERVER_VIP}` : keepalived 클러스터 호스트들 간에 negotiation 된 가상 IP 주소입니다.

### 1.6.2. health check 스크립트

- 위에 keepalived service configuration 파일은 가상 IP를 보유하고 있는 노드에서 API 서버 를 사용할 수 있는지 확인하는 health check 스크립트 `/etc/keepalived/check_apiserver.sh` 를 사용합니다. 이 스크립트의 내용은 아래와 같습니다.

`/etc/keepalived/check_apiserver.sh`

``` text
#!/bin/sh

errorExit() {
    echo "*** $*" 1>&2
    exit 1
}

curl --silent --max-time 2 --insecure https://localhost:${APISERVER_DEST_PORT}/ -o /dev/null || errorExit "Error GET https://localhost:${APISERVER_DEST_PORT}/"
if ip addr | grep -q ${APISERVER_VIP}; then
    curl --silent --max-time 2 --insecure https://${APISERVER_VIP}:${APISERVER_DEST_PORT}/ -o /dev/null || errorExit "Error GET https://${APISERVER_VIP}:${APISERVER_DEST_PORT}/"
fi
```

- bash 변수로 아래에 placeholder 들을 설정해주면 됩니다.
- `${APISERVER_VIP}` : 연결 유지 클러스터 호스트간에 negotiation 된 가상 IP 주소입니다.
- `${APISERVER_DEST_PORT}` : Kubernetes가 API 서버 와 통신하는 데 사용하는 포트입니다. (lb 의 port)

## 1.7. haproxy 설정 방법

- haproxy 설정은 서비스 구성 파일 하나로 구성합니다. 이 파일은 `/etc/haproxy/haproxy.cfg` 입니다. 그러나 일부 Linux 배포판은 다른 곳에 보관할 수 있습니다. 다음 구성은 haproxy 버전 2.1.4에서 성공적으로 사용되었습니다.

`/etc/haproxy/haproxy.cfg`  (haproxy v2.1.4 기준)

``` text
# /etc/haproxy/haproxy.cfg
#---------------------------------------------------------------------
# Global settings
#---------------------------------------------------------------------
global
    log /dev/log local0
    log /dev/log local1 notice
    daemon

#---------------------------------------------------------------------
# common defaults that all the 'listen' and 'backend' sections will
# use if not designated in their block
#---------------------------------------------------------------------
defaults
    mode                    http
    log                     global
    option                  httplog
    option                  dontlognull
    option http-server-close
    option forwardfor       except 127.0.0.0/8
    option                  redispatch
    retries                 1
    timeout http-request    10s
    timeout queue           20s
    timeout connect         5s
    timeout client          20s
    timeout server          20s
    timeout http-keep-alive 10s
    timeout check           10s

#---------------------------------------------------------------------
# apiserver frontend which proxys to the control plane nodes
#---------------------------------------------------------------------
frontend apiserver
    bind *:${APISERVER_DEST_PORT}
    mode tcp
    option tcplog
    default_backend apiserver

#---------------------------------------------------------------------
# round robin balancing for apiserver
#---------------------------------------------------------------------
backend apiserver
    option httpchk GET /healthz
    http-check expect status 200
    mode tcp
    option ssl-hello-chk
    balance     roundrobin
        server ${HOST1_ID} ${HOST1_ADDRESS}:${APISERVER_SRC_PORT} check
        # [...]
```

- 여기도 앞서 본 keepalived 와 마찬가지로 bash 변수로 아래에 placeholder 들을 설정해주면 됩니다.
- `${APISERVER_DEST_PORT}` : Kubernetes가 API 서버와 통신하는 데 사용하는 포트입니다. (lb 의 port)
- `${APISERVER_SRC_PORT}` : API 서버 인스턴스에서 사용하는 포트입니다. (apiserver 의 port)
- `${HOST1_ID}` : 첫 번째로 로드밸런싱 된 API 서버 호스트의 symbolic name 입니다.
- `${HOST1_ADDRESS}` : 첫 번째로 로드밸런싱 된 API 서버 호스트의 resolvable address (DNS 이름, IP 주소) 입니다.
- 추가적인 `server` 라인들 : 로드밸런싱 된 API 서버 호스트 당 라인 추가

# 2. HA k8s 클러스터 구축해보기 (실습)

- 앞서 말했다시피, Keepalived + HAproxy HA k8s 클러스터를 구축하는데 3 가지 정도의 아키텍처가 있다. 하나씩 검토해보겠다.
  1. HA k8s using Keepalived + HAproxy on systemd
  2. HA k8s using Keepalived + HAproxy on static pods
  3. HA k8s using kube-vip on static pods

## 2.1. stacked 로 OS 위에 keepalived + haproxy service 올리기

### 2.1.1. 아키텍처

![](/.uploads/2021-05-31-17-41-05.png)

### 2.1.2. 전제

- ntp 설정
- /etc/hosts 설정
- docker cgroup 의 systemd 사용가능 설정

### 2.1.3. 패키지 설치

``` bash
$ sudo apt install -y haproxy keepalived
```

### 2.1.4. keepalived 설정

`/etc/keepalived/keepalived.conf`

```bash
! /etc/keepalived/keepalived.conf
! Configuration File for keepalived
global_defs {
    router_id LVS_DEVEL
}
vrrp_script check_apiserver {
  script "/etc/keepalived/check_apiserver.sh"
  interval 3
  weight -2
  fall 10
  rise 2
}

vrrp_instance VI_1 {
    state MASTER  # SLAVE
    interface ens3
    virtual_router_id 151
    priority 255  # 254 253
    authentication {
        auth_type PASS
        auth_pass P@##D321!
    }
    virtual_ipaddress {
        10.0.0.162/24
    }
    track_script {
        check_apiserver
    }
}
```

- 주석 처리된 부분을 다른 2 노드에서 변경하여 사용하면 됩니다.

`/etc/keepalived/check_apiserver.sh`

```bash
#!/bin/sh
APISERVER_VIP=10.0.0.162
APISERVER_DEST_PORT=6443

errorExit() {
    echo "*** $*" 1>&2
    exit 1
}

curl --silent --max-time 2 --insecure https://localhost:${APISERVER_DEST_PORT}/ -o /dev/null || errorExit "Error GET https://localhost:${APISERVER_DEST_PORT}/"
if ip addr | grep -q ${APISERVER_VIP}; then
    curl --silent --max-time 2 --insecure https://${APISERVER_VIP}:${APISERVER_DEST_PORT}/ -o /dev/null || errorExit "Error GET https://${APISERVER_VIP}:${APISERVER_DEST_PORT}/"
fi
```

``` bash
$ sudo chmod +x /etc/keepalived/check_apiserver.sh
```

### 2.1.5. haproxy 설정

`/etc/haproxy/haproxy.cfg`

```bash
#---------------------------------------------------------------------
# apiserver frontend which proxys to the masters
#---------------------------------------------------------------------
frontend apiserver
    bind *:8443
    mode tcp
    option tcplog
    default_backend apiserver
#---------------------------------------------------------------------
# round robin balancing for apiserver
#---------------------------------------------------------------------
backend apiserver
    option httpchk GET /healthz
    http-check expect status 200
    mode tcp
    option ssl-hello-chk
    balance     roundrobin
        server dtlab-dev-k8s-pjb-1 10.0.0.163:6443 check
        server dtlab-dev-k8s-pjb-2 10.0.0.164:6443 check
        server dtlab-dev-k8s-pjb-3 10.0.0.165:6443 check
```

keepalived, haproxy 서비스 시작

```bash
$ sudo systemctl enable keepalived --now
$ sudo systemctl enable haproxy --now
```

### 2.1.6. 적용 확인

```bash
$ ip a s
...
2: ens3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP group default qlen 1000
    inet 10.0.0.163/24 brd 10.0.0.255 scope global ens3
       valid_lft forever preferred_lft forever
    inet **10.0.0.162/24** scope global secondary ens3
       valid_lft forever preferred_lft forever
...

$ nc -v 10.231.238.162 8443
Ncat: Version 7.50 ( https://nmap.org/ncat )
Ncat: Connected to 10.231.238.162:8443.
```

### 2.1.7. 클러스터 부트스트랩

``` bash
### kube-* 설치
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg

echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt install -y kubeadm=1.20.7-00 kubelet=1.20.7-00 kubectl=1.20.7-00
sudo apt-mark hold kubelet kubeadm kubectl


### 로드밸런서 endpoint 연결 확인
ping 10.0.0.162
nc -v 10.0.0.162 6443


### 마스터노드1 에서 클러스터 init
sudo kubeadm init --control-plane-endpoint 10.0.0.162:6443 --upload-certs


### cni 설치 (calico)
curl https://docs.projectcalico.org/manifests/calico.yaml -O
kubectl apply -f calico.yaml

### kube config 설정
### 마스터 2, 3 에서 kubeadm join 수행
```

### 2.1.8. 구성 완료 후 테스트

```bash
### 노드 연동 확인
$ k get no
NAME                  STATUS   ROLES                  AGE    VERSION
dtlab-dev-k8s-pjb-1   Ready    control-plane,master   3h6m   v1.20.7
dtlab-dev-k8s-pjb-2   Ready    control-plane,master   176m   v1.20.7
dtlab-dev-k8s-pjb-3   Ready    control-plane,master   149m   v1.20.7
dtlab-dev-k8s-pjb-4   Ready    <none>                 89m    v1.20.7

### nginx deploy 배포
$ kubectl create deployment nginx-lab --image=nginx
deployment.apps/nginx-lab created

### nginx deploy 배포 확인
$ kubectl get deployments.apps nginx-lab
NAME        READY   UP-TO-DATE   AVAILABLE   AGE
nginx-lab   1/1     1            1           79m

### nginx pod 배포 확인
$ k get po
NAME                         READY   STATUS    RESTARTS   AGE
nginx-lab-5c8f4ffff8-dwnz8   1/1     Running   0          80m

### nginx deploy scaling
$ kubectl scale deployment nginx-lab --replicas=4
deployment.apps/nginx-lab scaled

### scaling 확인
$ kubectl get deployments.apps nginx-lab
NAME        READY   UP-TO-DATE   AVAILABLE   AGE
nginx-lab   4/4     4            4           81m

### nginx svc 생성
$ kubectl expose deployment nginx-lab --name=nginx-lab --type=NodePort --port=80 --target-port=80
service/nginx-lab exposed

### nginx svc 생성 확인
$ kubectl get svc nginx-lab
NAME        TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
nginx-lab   NodePort   10.100.46.233   <none>        80:31736/TCP   78m

### curl 테스트
$ curl http://10.0.0.162:31736
$ curl http://10.0.0.163:31736
$ curl http://10.0.0.166:31736
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
    body {
        width: 35em;
        margin: 0 auto;
        font-family: Tahoma, Verdana, Arial, sans-serif;
    }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

## 2.2. stacked, static pods 로 keepalived + haproxy 서비스 올리기

![](/.uploads/2021-05-31-17-41-05.png)

순서

- 클러스터를 부트스트래핑 하기 전 `/etc/kubernetes/manifests` 경로에 관련된 매니페스트를 작성해 놓습니다.
- 그 후 클러스터를 부트스트랩하면 과정 중에 kubelet 이 해당 프로세스를 시작하여 클러스터가 돌아가는 중 사용할 수 있게 됩니다. 로드밸런서 엔드포인트를 갖고 클러스터를 부트스트랩 하는 명령어는 [여기에서](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/high-availability/#stacked-control-plane-and-etcd-nodes) 확인할 수 있습니다.

    ``` bash
    sudo kubeadm init --control-plane-endpoint "LOAD_BALANCER_DNS:LOAD_BALANCER_PORT" --upload-certs
    ```

`/etc/kubernetes/manifests/keepalived.yaml`

``` yaml
apiVersion: v1
kind: Pod
metadata:
  creationTimestamp: null
  name: keepalived
  namespace: kube-system
spec:
  containers:
  - image: osixia/keepalived:2.0.17
    name: keepalived
    resources: {}
    securityContext:
      capabilities:
        add:
        - NET_ADMIN
        - NET_BROADCAST
        - NET_RAW
    volumeMounts:
    - mountPath: /usr/local/etc/keepalived/keepalived.conf
      name: config
    - mountPath: /etc/keepalived/check_apiserver.sh
      name: check
  hostNetwork: true
  volumes:
  - hostPath:
      path: /etc/keepalived/keepalived.conf
    name: config
  - hostPath:
      path: /etc/keepalived/check_apiserver.sh
    name: check
status: {}
```

`/etc/kubernetes/manifests/haproxy.yaml`

``` yaml
apiVersion: v1
kind: Pod
metadata:
  name: haproxy
  namespace: kube-system
spec:
  containers:
  - image: haproxy:2.1.4
    name: haproxy
    livenessProbe:
      failureThreshold: 8
      httpGet:
        host: localhost
        path: /healthz
        port: ${APISERVER_DEST_PORT}
        scheme: HTTPS
    volumeMounts:
    - mountPath: /usr/local/etc/haproxy/haproxy.cfg
      name: haproxyconf
      readOnly: true
  hostNetwork: true
  volumes:
  - hostPath:
      path: /etc/haproxy/haproxy.cfg
      type: FileOrCreate
    name: haproxyconf
status: {}
```

- 위에 yaml 에서 placeholder 를 채워줘야 합니다. `${APISERVER_DEST_PORT}` 는 `/etc/haproxy/haproxy.cfg` 에서와 동일한 값을 보유해야합니다 (위 참조).
- 이 조합은 예제에 사용 된 버전에서 성공적으로 사용되었습니다. 다른 버전도 작동하거나 구성 파일을 변경해야 할 수도 있습니다.
- 서비스가 가동되면 이제 kubeadm init 을 사용하여 Kubernetes 클러스터를 부트 스트랩 할 수 있습니다.

## 2.3. kube-vip

kube-vip 란?

- keepalived + haproxy 의 새로운 대안으로 **kube-vip** 가 있습니다.
- kube-vip 는 가상 IP 관리와 로드밸런싱을 하나의 서비스로 구현합니다.
- kube-vip 는 controlplane 노드에서 static pod 로 실행됩니다.
- keepalived 와 마찬가지로 가상 IP를 협상하는 호스트는 동일한 IP 서브넷에 있어야 합니다. 또한 haproxy 와 마찬가지로 stream-based 로드밸런싱을 사용하면 TLS termination 을 그 뒤에있는 API 서버 인스턴스에서 처리 할 수 ​​있습니다.

`/etc/kube-vip/config.yaml`

``` yaml
localPeer:
  id: ${ID}
  address: ${IPADDR}
  port: 10000
remotePeers:
- id: ${PEER1_ID}
  address: ${PEER1_IPADDR}
  port: 10000
# [...]
vip: ${APISERVER_VIP}
gratuitousARP: true
singleNode: false
startAsLeader: ${IS_LEADER}
interface: ${INTERFACE}
loadBalancers:
- name: API Server Load Balancer
  type: tcp
  port: ${APISERVER_DEST_PORT}
  bindToVip: false
  backends:
  - port: ${APISERVER_SRC_PORT}
    address: ${HOST1_ADDRESS}
  # [...]
```

- bash 변수로 placeholder 를 입력해줘야 합니다.
- `${ID}` : 현재 호스트의 symbolic name
- `${IPADDR}` : 현재 호스트의 IP 주소
- `${PEER1_ID}` : 첫 번째 vIP peer 의 symbolic name
- `${PEER1_IPADDR}` : 첫 번째 vIP peer 의 IP 주소
- `[...]` : 추가적인 vIP peer 의 (ID, 주소, 포트) 엔트리를 추가할 수 있습니다.
- `${APISERVER_VIP}` : kube-vip 클러스터 호스트간에 negotiation 된 가상 IP 주소입니다.
- `${IS_LEADER}` : 정확히 한 명의 리더에 대해 true이고 나머지는 false 입니다.
- `${INTERFACE}` : 가상 IP 협상에 참여하는 네트워크 인터페이스입니다. e.g. eth0.
- `${APISERVER_DEST_PORT}` Kubernetes가 API 서버와 통신하는 데 사용하는 포트입니다.
- `${APISERVER_SRC_PORT}` API 서버 인스턴스에서 사용하는 포트 입니다.
- `${HOST1_ADDRESS}` : 로드 밸런싱 된 첫 번째 API 서버 호스트의 IP 주소입니다.
- `[...]` : 추가적인 로드 밸런싱 된 API 서버 호스트의 (포트, 주소) 엔트리를 추가할 수 있습니다.

클러스터에서 서비스를 시작하려면 이제 매니페스트 kube-vip.yaml 을 */etc/kubernetes/manifests* 안에 배치해야 합니다.(먼저 디렉토리 생성할 것)
이 작업은 `kube-vip` 도커 이미지를 사용하여 생성 할 수 있습니다.

``` bash
# docker run -it --rm plndr/kube-vip:0.1.1 /kube-vip sample manifest \
    | sed "s|plndr/kube-vip:'|plndr/kube-vip:0.1.1'|" \
    | sudo tee /etc/kubernetes/manifests/kube-vip.yaml
```

`/etc/kubernetes/manifests/kube-vip.yaml` 예시

``` yaml
apiVersion: v1
kind: Pod
metadata:
  creationTimestamp: null
  name: kube-vip
  namespace: kube-system
spec:
  containers:
  - command:
    - /kube-vip
    - start
    - -c
    - /vip.yaml
    image: 'plndr/kube-vip:0.1.1'
    name: kube-vip
    resources: {}
    securityContext:
      capabilities:
        add:
        - NET_ADMIN
        - SYS_TIME
    volumeMounts:
    - mountPath: /vip.yaml
      name: config
  hostNetwork: true
  volumes:
  - hostPath:
      path: /etc/kube-vip/config.yaml
    name: config
status: {}
```

클러스터 부트스트랩하기

- 이제 kubeadm을 사용하여 고 가용성 클러스터 만들기에 설명 된대로 실제 클러스터 부트 스트랩이 발생할 수 있습니다.
- `${APISERVER_DEST_PORT}` 가 위 구성에서 6443과 다른 값으로 구성된 경우 kubeadm init는 API 서버에 해당 포트를 사용하도록 지시해야합니다. 새 클러스터 포트 8443 에서 로드 밸런싱 된 API 서버 및 DNS 이름이 vip.mycluster.local 인 가상 IP에 사용된다고 가정하면 다음과 같이 --control-plane-endpoint 인수를 kubeadm에 전달해야합니다.

``` bash
# kubeadm init --control-plane-endpoint vip.mycluster.local:8443 [additional arguments ...]
```