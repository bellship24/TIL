**목차**

- [1. 요약](#1-요약)
- [2. 아키텍처](#2-아키텍처)
- [3. 사전 작업](#3-사전-작업)
  - [3.1. tmux 설치 (m1)](#31-tmux-설치-m1)
  - [3.2. `tmux.conf` 설정 (m1)](#32-tmuxconf-설정-m1)
  - [3.3. netplan 설정 (모든 노드)](#33-netplan-설정-모든-노드)
  - [3.4. disk 마운트 확인 및 설정 (모든 노드)](#34-disk-마운트-확인-및-설정-모든-노드)
  - [3.5. swap 해제 (모든 노드)](#35-swap-해제-모든-노드)
  - [3.6. hostname 설정 (모든 노드)](#36-hostname-설정-모든-노드)
  - [3.7. `/etc/hosts` 설정 (모든 노드)](#37-etchosts-설정-모든-노드)
  - [3.8. ssh-key 추가](#38-ssh-key-추가)
  - [3.9. ntp 설치 및 설정, timezone 설정 (모든 노드)](#39-ntp-설치-및-설정-timezone-설정-모든-노드)
  - [3.10. docker 설치 및 설정 (모든 노드)](#310-docker-설치-및-설정-모든-노드)
  - [3.11. iptable 설정(모든 노드)](#311-iptable-설정모든-노드)
  - [3.12. kubeadm, kubelet, kubectl 설치 (모든 노드)](#312-kubeadm-kubelet-kubectl-설치-모든-노드)
- [4. keepalived 설치 및 설정](#4-keepalived-설치-및-설정)
  - [4.1. keepalived 설치](#41-keepalived-설치)
  - [4.2. keepalived 설정](#42-keepalived-설정)
- [5. haproxy 설치 및 설정](#5-haproxy-설치-및-설정)
  - [5.1. haproxy 설치](#51-haproxy-설치)
  - [5.2. haproxy 설정](#52-haproxy-설정)
- [6. keepalived + haproxy 구동 및 점검](#6-keepalived--haproxy-구동-및-점검)
  - [6.1. keepalived, haproxy 서비스 시작](#61-keepalived-haproxy-서비스-시작)
  - [6.2. keepalived 를 통한 vip 적용 확인](#62-keepalived-를-통한-vip-적용-확인)
  - [6.3. haproxy 를 통한 포트포워딩 및 로드밸런싱 적용 확인](#63-haproxy-를-통한-포트포워딩-및-로드밸런싱-적용-확인)
    - [6.3.1. haproxy stats 대시보드](#631-haproxy-stats-대시보드)
    - [6.3.2. netcat 으로 tcp 통신 확인](#632-netcat-으로-tcp-통신-확인)
- [7. ha k8s 클러스터 구성](#7-ha-k8s-클러스터-구성)
- [8. HA 기능 테스트](#8-ha-기능-테스트)
- [9. HA k8s 장애 테스트](#9-ha-k8s-장애-테스트)
  - [9.1. 장애 테스트 시나리오](#91-장애-테스트-시나리오)
  - [9.2. 노드 1 개 장애 case](#92-노드-1-개-장애-case)
  - [9.3. 노드 2 개 장애 case](#93-노드-2-개-장애-case)
  - [9.4. 노드 3 개 장애 case](#94-노드-3-개-장애-case)
- [10. HA k8s 장애 시에 복구 방법](#10-ha-k8s-장애-시에-복구-방법)
  - [10.1. etcd 백업](#101-etcd-백업)
  - [10.2. etcd 백업 확인](#102-etcd-백업-확인)
  - [10.3. 테스트를 위해 백업 이후에 워크로드 생성](#103-테스트를-위해-백업-이후에-워크로드-생성)
  - [10.4. etcd 복원 및 데이터 볼륨 생성](#104-etcd-복원-및-데이터-볼륨-생성)
  - [10.5. etcd static pod 설정 변경](#105-etcd-static-pod-설정-변경)
  - [10.6. 복원 완료 후 상태 확인](#106-복원-완료-후-상태-확인)

**참고**

- [k8s docs - Options for Highly Available topology](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/ha-topology/)
- [k8s docs - Installing kubeadm](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/)

# 1. 요약

> **3 노드를 가지고 기본 설정부터 Keepalived + HAProxy 기반의 HA K8s 를 구성 및 점검해보자.**

> **아무리 HA 구성을 했어도 장애가 날 수 있다. 그러므로 etcdctl 로 쿠버네티스의 etcd 를 백업 및 복구하는 방법도 검증해보자**

# 2. 아키텍처

![](/.uploads/2021-06-10-13-57-55.png)

- k8s HA 에서 각 controlplane 노드들은 kube-apiserver, kube-controller-manager, kube-scheduler 인스턴스를 운영한다.
- kube-apiserver 들은 모두 active 상태이며 kube-controller-manager 와 kube-scheduler 는 하나만 active 고 나머지 두 개는 standby 상태이다.
- kubeadm 으로 etcd 를 controlplane 노드 위에 올리면 stacked etcd cluster 라고 한다. 이 etcd 를 별도의 노드에 구성하면 unstacked etcd cluster 라고 한다.
- stacked etcd cluster 로 구성하면 리소스 효율성이 증가하고 구성 및 관리가 단순해진다. 하지만 한 노드가 다운되면 그 안에 있는 etcd 멤버와 controlplane 인스턴스 모두 잃게 되어 커플링 실패의 위험성이 있고 중복성도 손상된다. 그러므로 HA 클러스터를 만들기 위해 stacked controlplane 노드를 최소 3 개 이상 구성 해야한다.
- stacked etcd cluster 에서는 각 controlplane 노드들이 local etcd 멤버를 생성하여 오직 해당 노드의 apiserver 와 통신한다. kube-scheduler, kube-controller-manager 인스턴스도 마찬가지로 각 apiserver 와 통신한다.
- k8s HA 구성은 etcd 분산을 위해 쿼럼이 과반수 이상이여야 하므로 3 개 이상의 홀수 개 노드가 필요하다.
- k8s HA 구성을 위해서는 로드밸런서가 필요하다. kube-apiserver 는 로드밸런서를 이용하여 워커 노드들에게 노출되기 때문이다.
- Load balancer 는 소프트웨어 기반으로 구성할 것이며 구성 방법으로는 여러 가지가 있고 대표적으로 아래와 같은 방법들이 있다.
  - keepalived+haproxy on systemd
  - keepalived+haproxy on static pod
  - kube-vip on static pod
- 이 문서에서는 keepalived+haproxy on systemd 를 사용한다.
- keepalived 와 haproxy 를 마스터 노드 3 곳에서 서비스 한다.
- keepalived 클러스터는 vip 에 대한 트래픽을 VRRP 프로토콜을 사용해 받아온다.
- haproxy 클러스터는 tcp 및 http 에 대해 reverse proxy 하여 부하 분산과 포트를 포워딩 해준다.

# 3. 사전 작업

## 3.1. tmux 설치 (m1)

``` bash
# ubuntu 에 설치
$ sudo apt update
$ sudo apt install -y tmux

# tmux 세션 시작
$ tmux

# 현재 tmux 세션 종료
ctrl + d
```

## 3.2. `tmux.conf` 설정 (m1)

`~/.tmux.conf`

``` bash
cat <<EOF > ~/.tmux.conf
# config file 적용 
bind r source-file ~/.tmux.conf

# pane 동시 입력 토글 키 y 설정
bind-key y set-window-option synchronize-panes 

# pane 분할키를 | 과 - 로 변경
bind | split-window -h 
bind - split-window -v 
unbind '"' 
unbind %
EOF
```

tmux 진입

``` bash
tmux new -t 0
```

`.tmux.conf` 적용

- ctrl + b
- shit + ;
- source-file ~/.tmux.conf

## 3.3. netplan 설정 (모든 노드)

`/etc/netplan/50-cloud-init.yaml`

``` yaml
network:
  ethernets:
    ens3:
      addresses:
        - 10.0.0.186/24
      gateway4: 10.0.0.254
      nameservers:
        addresses:
          - 8.8.8.8
  version: 2
```

## 3.4. disk 마운트 확인 및 설정 (모든 노드)

LVM 증설했다면 적용 확인할 것

swap 마운트 확인

`/etc/fstab`

``` bash
#/swap.img      none    swap    sw      0       0
```

``` bash
sudo mount -a
```

- 위와 같이 swap 마운트 해제 및 적용할 것

## 3.5. swap 해제 (모든 노드)

swap 확인

``` bash
free -h
```

swap 해제 (필요 시에)

``` bash
sudo swapoff -a
```

## 3.6. hostname 설정 (모든 노드)

- 편의를 위해 설정하자
- 아래 명령어 실행 후에 적용을 위해 터미널 다시 접속

``` bash
sudo hostnamectl set-hostname cicd-ops-ai-m[1,2,3]
```

## 3.7. `/etc/hosts` 설정 (모든 노드)

`/etc/hosts`

``` bash
sudo bash -c 'cat <<EOF >> /etc/hosts

# cicd-ops-ai
10.0.0.216 cicd-ops-ai-vip
10.0.0.217 cicd-ops-ai-lb
10.0.0.186 cicd-ops-ai-m1
10.0.0.187 cicd-ops-ai-m2
10.0.0.188 cicd-ops-ai-m3
EOF'
```

## 3.8. ssh-key 추가

- 편의를 위해 추가했다.

pc -> 모든 노드

``` bash
$ ssh-copy-id -i ~/.ssh/id_rsa.pub myid@10.0.0.186
$ ssh-copy-id -i ~/.ssh/id_rsa.pub myid@10.0.0.187
$ ssh-copy-id -i ~/.ssh/id_rsa.pub myid@10.0.0.188
```

m1 -> m2, m3

``` bash
$ ssh-keygen -t rsa -b 2048
$ ssh-copy-id -i ~/.ssh/id_rsa.pub myid@10.0.0.187
$ ssh-copy-id -i ~/.ssh/id_rsa.pub myid@10.0.0.188
```

## 3.9. ntp 설치 및 설정, timezone 설정 (모든 노드)

``` bash
$ sudo apt install -y ntp

$ sudo vi /etc/ntp.conf

# 사내망
server <사내망 ntp 서버 도메인네임> iburst
server <사내망 ntp 서버 도메인네임2> iburst
restrict 10.0.0.0 mask 255.255.255.0 nomodify notrap

$ sudo systemctl enable ntp && \
  sudo systemctl restart ntp && \
  sudo systemctl status ntp

# ntp 적용 확인. 잡히는데 시간이 조금 걸릴 수 있음
$ ntpq -pn

$ sudo timedatectl set-timezone Asia/Seoul
$ timedatectl
Local time: Thu 2021-08-19 13:07:00 KST
                  Universal time: Thu 2021-08-19 04:07:00 UTC
                        RTC time: Thu 2021-08-19 04:07:00
                       Time zone: Asia/Seoul (KST, +0900)
       System clock synchronized: yes
systemd-timesyncd.service active: yes
                 RTC in local TZ: no
```

## 3.10. docker 설치 및 설정 (모든 노드)

설치 및 확인

``` bash
# 설치
curl -fsSL https://get.docker.com/ | sudo sh \
  && sudo systemctl --now enable docker \
  && sudo systemctl start docker \
  && sudo systemctl enable docker

# 설치 확인
$ docker version
Client: Docker Engine - Community
 Version:           20.10.7
 API version:       1.41
 Go version:        go1.13.15
 Git commit:        f0df350
 Built:             Wed Jun  2 11:56:40 2021
 OS/Arch:           linux/amd64
 Context:           default
 Experimental:      true

Server: Docker Engine - Community
 Engine:
  Version:          20.10.7
  API version:      1.41 (minimum version 1.12)
  Go version:       go1.13.15
  Git commit:       b0f5bc3
  Built:            Wed Jun  2 11:54:48 2021
  OS/Arch:          linux/amd64
  Experimental:     false
 containerd:
  Version:          1.4.8
  GitCommit:        7eba5930496d9bbe375fdf71603e610ad737d2b2
 runc:
  Version:          1.0.0
  GitCommit:        v1.0.0-0-g84113ee
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0
```

docker 그룹 추가

``` bash
sudo usermod -aG docker myid
```

`/etc/docker/daemon.json` 설정

``` bash
sudo bash -c "cat <<EOF > /etc/docker/daemon.json
{
  \"exec-opts\": [\"native.cgroupdriver=systemd\"],
  \"storage-driver\": \"overlay2\",
  \"storage-opts\": [
    \"overlay2.override_kernel_check=true\"
  ]
}
EOF
"
```

- cgroupdriver 를 systemd 로 설정
- storage-driver 를 overlay2 로 설정

GPU 를 사용한다면, 아래처럼 default-runtime 을 nvidia 로 설정하는 부분을 추가하자

- 아래 설정을 적용하되 사전에 nvidia-driver, nvidia-docker 가 설치되어 있어야 한다.

``` json
"default-runtime": "nvidia",
"runtimes": {
 "nvidia": {
   "path": "nvidia-container-runtime",
     "runtimeArgs": []
  }
}
```

도커 재시작 및 확인

``` bash
sudo systemctl restart docker \
  && sudo systemctl enable docker \
  && systemctl status docker

docker ps -a
```

## 3.11. iptable 설정(모든 노드)

- 브릿지 되어있는 IPv4 트래픽을 iptables 체인으로 전달될 수 있게 한다.

```bash
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
EOF
```

```bash
cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.ipv4.ip_nonlocal_bind = 1
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF
```

```bash
$ sudo sysctl --system
...
net.ipv4.ip_nonlocal_bind = 1
net.ipv4.ip_forward = 1
...
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
```

docker 재시작

```bash
sudo systemctl enable docker
sudo systemctl daemon-reload
sudo systemctl restart docker
sudo systemctl status docker
```

## 3.12. kubeadm, kubelet, kubectl 설치 (모든 노드)

- 먼저, k8s docs 에 따라 master 노드 3 곳에 kubeadm, kubelet, kubectl 을 설치한다.

``` bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg

echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
# 특정 버전 설치 시에
sudo apt install -y kubeadm=1.21.3-00 kubelet=1.21.3-00 kubectl=1.21.3-00

sudo apt-mark hold kubelet kubeadm kubectl
```

kubeadm, kubectl, kubelet 버전 정보

```bash
$ dpkg -l | grep kube
ii  kubeadm                                1.21.1-00                                       amd64        Kubernetes Cluster Bootstrapping Tool
ii  kubectl                                1.21.1-00                                       amd64        Kubernetes Command Line Tool
ii  kubelet                                1.21.1-00                                       amd64        Kubernetes Node Agent
ii  kubernetes-cni                         0.8.7-00                                        amd64        Kubernetes CNI
```

kernel parameter 수정

```bash
$ sudo bash -c 'cat >> /etc/sysctl.conf << EOF
net.ipv4.ip_nonlocal_bind = 1
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables=1
EOF'

$ sudo sysctl -p
net.ipv4.ip_nonlocal_bind = 1
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables = 1
```

- `/etc/sysctl.conf` 는 리눅스 보안 설정이다.
- `ip_forward` 는 커널에서 패킷 포워딩을 활성화하여 브릿지된 컨테이너에 대한 패킷을 처리한다.

포트 통신 확인

![](/.uploads/2021-06-11-16-05-34.png)

# 4. keepalived 설치 및 설정

## 4.1. keepalived 설치

``` bash
$ sudo apt install -y keepalived
$ keepalived -version
Keepalived v1.3.9 (10/21,2017)

Copyright(C) 2001-2017 Alexandre Cassen, <acassen@gmail.com>

Build options:  PIPE2 IPV4_DEVCONF LIBNL3 RTA_ENCAP RTA_EXPIRES RTA_NEWDST RTA_PREF RTA_VIA FRA_OIFNAME FRA_SUPPRESS_PREFIXLEN FRA_SUPPRESS_IFGROUP FRA_TUN_ID RTAX_CC_ALGO RTAX_QUICKACK FRA_UID_RANGE LWTUNNEL_ENCAP_MPLS LWTUNNEL_ENCAP_ILA LIBIPTC LIBIPSET_DYNAMIC LVS LIBIPVS_NETLINK IPVS_DEST_ATTR_ADDR_FAMILY IPVS_SYNCD_ATTRIBUTES IPVS_64BIT_STATS VRRP VRRP_AUTH VRRP_VMAC SOCK_NONBLOCK SOCK_CLOEXEC GLOB_BRACE OLD_CHKSUM_COMPAT FIB_ROUTING INET6_ADDR_GEN_MODE SNMP_V3_FOR_V2 SNMP SNMP_KEEPALIVED SNMP_CHECKER SNMP_RFC SNMP_RFCV2 SNMP_RFCV3 DBUS SO_MARK
```

## 4.2. keepalived 설정

`/etc/keepalived/keepalived.conf` 예시

``` bash
sudo bash -c "cat <<EOF > /etc/keepalived/keepalived.conf
! /etc/keepalived/keepalived.conf
! Configuration File for keepalived
global_defs {
    router_id LVS_DEVEL
}
vrrp_script check_apiserver {
  script \"/etc/keepalived/check_apiserver.sh\"
  interval 3
  weight -2
  fall 10
  rise 2
}

vrrp_instance VI_1 {
    state MASTER            ### SLAVE
    interface ens3
    virtual_router_id 151
    priority 255            ### 254 253 ...
    authentication {
        auth_type PASS
        auth_pass P@##D321!
    }
    virtual_ipaddress {
        10.0.0.216/24
    }
    track_script {
        check_apiserver
    }
}
EOF"
```

- MATSER 와 SLAVE 각 역할에 맞게 노드별로 주석한 부분을 설정 변경해줄 것

`/etc/keepalived/check_apiserver.sh` 예시

``` bash
sudo bash -c "cat <<EOF > /etc/keepalived/check_apiserver.sh
#!/bin/sh
APISERVER_VIP=10.0.0.216
APISERVER_DEST_PORT=8443

errorExit() {
    echo \"*** $*\" 1>&2
    exit 1
}

curl --silent --max-time 2 --insecure https://localhost:${APISERVER_DEST_PORT}/ -o /dev/null || errorExit \"Error GET https://localhost:${APISERVER_DEST_PORT}/\"
if ip addr | grep -q ${APISERVER_VIP}; then
    curl --silent --max-time 2 --insecure https://${APISERVER_VIP}:${APISERVER_DEST_PORT}/ -o /dev/null || errorExit \"Error GET https://${APISERVER_VIP}:${APISERVER_DEST_PORT}/\"
fi
EOF
"

sudo chmod +x /etc/keepalived/check_apiserver.sh
```

# 5. haproxy 설치 및 설정

## 5.1. haproxy 설치

``` bash
$ sudo apt install -y haproxy
$ haproxy -v
HA-Proxy version 1.8.8-1ubuntu0.11 2020/06/22
Copyright 2000-2018 Willy Tarreau <willy@haproxy.org>
```

## 5.2. haproxy 설정

`/etc/haproxy/haproxy.cfg` 예시

``` bash
sudo bash -c "cat <<EOF > /etc/haproxy/haproxy.cfg
defaults
    timeout connect 5s
    timeout server 5s
    timeout client 5s
    timeout http-request 20s
    timeout http-keep-alive 20s

listen stats
    mode http
    bind *:31999
    stats enable
    stats scope         .
    stats realm         Haproxy\ Statistics
    stats uri           /haproxy_stats
    stats auth          myuser:mypass
    stats refresh       30s

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
        server cicd-ops-ai-m1 10.0.0.186:6443 check
        server cicd-ops-ai-m2 10.0.0.187:6443 check
        server cicd-ops-ai-m3 10.0.0.188:6443 check
EOF
"
```

# 6. keepalived + haproxy 구동 및 점검

## 6.1. keepalived, haproxy 서비스 시작

```bash
sudo systemctl enable keepalived --now
sudo systemctl enable haproxy --now
```

## 6.2. keepalived 를 통한 vip 적용 확인

```bash
### keepalived master 노드인 10.0.0.163 에서 수행
$ ip a s | grep 10.231.238
    inet 10.0.0.186/24 brd 10.0.0.255 scope global ens3
    inet 10.0.0.216/24 scope global secondary ens3
```

- keepalived 의 MASTER 노드에서 VIP 를 잡은 것을 확인할 수 있다.

## 6.3. haproxy 를 통한 포트포워딩 및 로드밸런싱 적용 확인

### 6.3.1. haproxy stats 대시보드

![](/.uploads/2021-06-11-16-01-16.png)

### 6.3.2. netcat 으로 tcp 통신 확인

``` bash
$ nc -v 10.0.0.216 8443
Connection to 10.0.0.216 8443 port [tcp/*] succeeded
```

# 7. ha k8s 클러스터 구성

클러스터 부트스트랩

``` bash
$ sudo kubeadm init --control-plane-endpoint 10.0.0.216:8443 --upload-certs

...
Your Kubernetes control-plane has initialized successfully!

To start using your cluster, you need to run the following as a regular user:

  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config

Alternatively, if you are the root user, you can run:

  export KUBECONFIG=/etc/kubernetes/admin.conf

You should now deploy a pod network to the cluster.
Run "kubectl apply -f [podnetwork].yaml" with one of the options listed at:
  https://kubernetes.io/docs/concepts/cluster-administration/addons/

You can now join any number of the control-plane node running the following command on each as root:

  kubeadm join 10.0.0.216:8443 --token 07ig43.vllhpw2pe0tkpt9w \
        --discovery-token-ca-cert-hash sha256:a17037396ad76da2f4f1014cbfe2ffb2371617c1c60205772005a01b1613d7a2 \
        --control-plane --certificate-key ddee7a57eeaca83edf17b5c8621c0c016d224e1b7ed7a6fefa4bc52d7466e60a

Please note that the certificate-key gives access to cluster sensitive data, keep it secret!
As a safeguard, uploaded-certs will be deleted in two hours; If necessary, you can use
"kubeadm init phase upload-certs --upload-certs" to reload certs afterward.

Then you can join any number of worker nodes by running the following on each as root:

kubeadm join 10.0.0.216:8443 --token 07ig43.vllhpw2pe0tkpt9w \
        --discovery-token-ca-cert-hash sha256:a17037396ad76da2f4f1014cbfe2ffb2371617c1c60205772005a01b1613d7a2

```

kubectl alias 설정

``` bash
cat <<EOF >> ~/.bashrc
alias k=kubectl
EOF

source ~/.bashrc
```

cni 설치 (weavenet)

``` bash
kubectl apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"
```

마스터 노드 추가 (예시)

``` bash
$ sudo kubeadm join 10.0.0.216:8443 --token 07ig43.vllhpw2pe0tkpt9w \
        --discovery-token-ca-cert-hash sha256:a17037396ad76da2f4f1014cbfe2ffb2371617c1c60205772005a01b1613d7a2 \
        --control-plane --certificate-key ddee7a57eeaca83edf17b5c8621c0c016d224e1b7ed7a6fefa4bc52d7466e60a


## join 명령어 재생성
$ kubeadm token create --print-join-command
kubeadm join 10.0.0.162:6443 --token 054ks4.8cg3a3rkh5aye5xt --discovery-token-ca-cert-hash sha256:8a1a0dd5643edccefd202e36e7503da2a0dbff7d55c670d02521986202f96ca5
```

워커 노드 추가 (예시)

``` bash
$ sudo kubeadm join 10.0.0.216:8443 --token 07ig43.vllhpw2pe0tkpt9w \
        --discovery-token-ca-cert-hash sha256:a17037396ad76da2f4f1014cbfe2ffb2371617c1c60205772005a01b1613d7a2
```

HA 구성 완료 확인

``` bash
$ k get no
NAME                  STATUS   ROLES                  AGE    VERSION
dtlab-dev-k8s-pjb-1   Ready    control-plane,master   3h6m   v1.20.7
dtlab-dev-k8s-pjb-2   Ready    control-plane,master   176m   v1.20.7
dtlab-dev-k8s-pjb-3   Ready    control-plane,master   149m   v1.20.7
dtlab-dev-k8s-pjb-4   Ready    <none>                 89m    v1.20.7
```

# 8. HA 기능 테스트

``` bash
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

### curl 테스트 (아래 3 명령어 모두 같음)
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

# 9. HA k8s 장애 테스트

## 9.1. 장애 테스트 시나리오

- 노드 1 개 장애 case
- 노드 2 개 장애 case
- 노드 3 개 장애 case

## 9.2. 노드 1 개 장애 case

- 결론: 3 개의 컨트롤플레인 노드로 만든 HA k8s 클러스터는 fault-tolerance 가 1 이므로 노드 1개가 장애여도 클러스터는 유지된다. 단, 장애난 노드에 워크로드를 생성했었다면, 다른 노드로 리스케줄링 된다.

m1 노드 셧다운

![](/.uploads/2021-06-11-16-24-48.png)

m2, m3 에서 kubectl 통신 가능

```bash
### m2, m3 에서 수행
### 아래 명령어는 VIP:LBport 로 통신한 것
$ k get no
NAME                  STATUS     ROLES                  AGE     VERSION
dtlab-dev-k8s-pjb-1   NotReady   control-plane,master   3h32m   v1.20.7
dtlab-dev-k8s-pjb-2   Ready      control-plane,master   3h23m   v1.20.7
dtlab-dev-k8s-pjb-3   Ready      control-plane,master   3h15m   v1.20.7
dtlab-dev-k8s-pjb-4   Ready      <none>                 3h12m   v1.20.7
```

m1 위에 있던 파드들의 리스케줄링

```bash
### m1 죽인 후 m1 위에 올라가 있던 파드 상태 확인
### 일정 시간이 지나지 않아 아직 살아있는 것으로 판단
$ k get po -o wide -w
NAME                     READY   STATUS    RESTARTS   AGE    IP           NODE                  NOMINATED NODE   READINESS GATES
...
nginx-6799fc88d8-nv5cb   1/1     Running   0          164m   10.42.0.1    dtlab-dev-k8s-pjb-1   <none>           <none>

### 종료 확인
### 일정 시간이 지나 죽은 것으로 판단하고 삭제
$ k get po -o wide -w
NAME                     READY   STATUS        RESTARTS   AGE    IP           NODE                  NOMINATED NODE   READINESS GATES
...
nginx-6799fc88d8-nv5cb   1/1     Terminating   0          167m   10.42.0.1    dtlab-dev-k8s-pjb-1   <none>           <none>

### 리스케줄링 확인
### 삭제 후에 새로운 agent 에 파드 리스케줄링
### 2swfb 파드가 리스케줄링 된 것이라 AGE 가 다른 파드들 보다 최신인 것을 확인할 수 있다.
$ k get po -o wide
$ k get po
NAME                     READY   STATUS    RESTARTS   AGE
nginx-6799fc88d8-2swfb   1/1     Running   0          46h
nginx-6799fc88d8-48bs2   1/1     Running   0          2d1h
nginx-6799fc88d8-5gcxf   1/1     Running   0          2d1h
nginx-6799fc88d8-5jxhk   1/1     Running   0          2d1h
nginx-6799fc88d8-79bnx   1/1     Running   0          2d1h
nginx-6799fc88d8-bczjc   1/1     Running   0          2d1h
nginx-6799fc88d8-d7qhb   1/1     Running   0          2d1h
nginx-6799fc88d8-fc6kr   1/1     Running   0          2d1h
nginx-6799fc88d8-gv2fx   1/1     Running   0          2d1h
nginx-6799fc88d8-gvq66   1/1     Running   0          2d1h
nginx-6799fc88d8-mkvtb   1/1     Running   0          2d1h
nginx-6799fc88d8-v5gwx   1/1     Running   0          2d1h
nginx-6799fc88d8-vks7m   1/1     Running   0          2d1h

### 해당 파드에 대한 deployment 도 13 개로 정상 상태이다
$ k get deploy
NAME    READY   UP-TO-DATE   AVAILABLE   AGE
nginx   13/13   13           13          3h6m

$ k get po --no-headers | wc -l
13
```

- m1 죽인 후에 일정 시간이 지나기 전까지 running 으로 판단하지만 kubectl exec 을 해도 접근은 안 됨
- 일정 시간이 지나면 pod 를 Terminating 함
- 그 후 삭제된 파드는 deployment 의 리소스였기 때문에 새로운 파드를 생성하여 총 13개를 맞춤

m1 리부트

![](/.uploads/2021-06-14-00-04-01.png)

클러스터 상태 확인

``` bash
$ k get no
NAME                  STATUS   ROLES                  AGE    VERSION
dtlab-dev-k8s-pjb-1   Ready    control-plane,master   2d7h   v1.20.7
dtlab-dev-k8s-pjb-2   Ready    control-plane,master   2d7h   v1.20.7
dtlab-dev-k8s-pjb-3   Ready    control-plane,master   2d7h   v1.20.7
dtlab-dev-k8s-pjb-4   Ready    <none>                 2d7h   v1.20.7
```

- m1 이 다시 Ready 상태가 된 것을 확인할 수 있다.

## 9.3. 노드 2 개 장애 case

- 결론: 3 개의 컨트롤 플레인 노드로 만든 HA k8s 클러스터는 fault-tolerance 가 1 이므로 노드 2 개가 장애이면 클러스터는 장애가 난다. 이럴 경우, 보통 기존에 장애난 컨트롤 플레인 노드들이 정상적으로 올라오면 해결된다. 하지만, 디스크 장애나 노드를 복구할 수 없는 상태가 왔을 때는 새로운 노드에 k8s 컨트롤 플레인을 재설치하고 etcd 데이터 백업본을 복원하는 방법이 있다. 예를 들면, 밑에 작성한 [9. HA k8s 장애 시에 복구 방법](#9-ha-k8s-장애-시에-복구-방법) 등을 참고하여 운영을 재기동할 수 있다.

m1, m2 노드 셧다운

![](/.uploads/2021-06-12-01-46-56.png)

m3 에서 kubectl 통신 불가능

```bash
### m3 에서 수행
### 아래 명령어는 VIP:LBport 로 통신한 것
$ k get no
Error from server (InternalError): an error on the server ("") has prevented the request from succeeding (get nodes)
```

VIP 확인

``` bash
myid@dtlab-dev-k8s-pjb-3:~$ ip a s | grep ens3
2: ens3: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP group default qlen 1000
    inet 10.0.0.165/24 brd 10.0.0.255 scope global ens3
    inet 10.0.0.162/24 scope global secondary ens3
```

로드밸런싱 확인

``` bash
$ nc -v 10.0.0.162 8443
Connection to 10.0.0.162 8443 port [tcp/*] succeeded!
```

m1, m2 리부트

![](/.uploads/2021-06-14-00-09-11.png)

클러스터 상태 확인

``` bash
$ k get no
NAME                  STATUS   ROLES                  AGE    VERSION
dtlab-dev-k8s-pjb-1   Ready    control-plane,master   4d9h   v1.20.7
dtlab-dev-k8s-pjb-2   Ready    control-plane,master   4d9h   v1.20.7
dtlab-dev-k8s-pjb-3   Ready    control-plane,master   4d9h   v1.20.7
dtlab-dev-k8s-pjb-4   Ready    <none>                 4d9h   v1.20.7
```

- m1, m2 가 다시 Ready 상태가 된 것을 확인할 수 있다.

파드, 디플로이먼트 상태 확인

``` bash
$ k get po
NAME                               READY   STATUS    RESTARTS   AGE
nginx-6799fc88d8-2swfb             1/1     Running   0          4d5h
nginx-6799fc88d8-48bs2             1/1     Running   0          4d8h
nginx-6799fc88d8-5gcxf             1/1     Running   1          4d8h
nginx-6799fc88d8-5jxhk             1/1     Running   0          4d8h
nginx-6799fc88d8-79bnx             1/1     Running   0          4d8h
nginx-6799fc88d8-bczjc             1/1     Running   0          4d8h
nginx-6799fc88d8-d7qhb             1/1     Running   0          4d8h
nginx-6799fc88d8-fc6kr             1/1     Running   0          4d8h
nginx-6799fc88d8-gv2fx             1/1     Running   0          4d8h
nginx-6799fc88d8-gvq66             1/1     Running   0          4d8h
nginx-6799fc88d8-mkvtb             1/1     Running   1          4d8h
nginx-6799fc88d8-v5gwx             1/1     Running   0          4d9h
nginx-6799fc88d8-vks7m             1/1     Running   0          4d8h
nginx-after-bkp-798567d8b9-22j9t   1/1     Running   1          46h
nginx-after-bkp-798567d8b9-k5zth   1/1     Running   0          46h
nginx-after-bkp-798567d8b9-s8rqz   1/1     Running   0          46h
nginx-after-bkp-798567d8b9-wqq4b   1/1     Running   0          2d1h

$ k get deploy
NAME              READY   UP-TO-DATE   AVAILABLE   AGE
nginx             13/13   13           13          4d9h
nginx-after-bkp   4/4     4            4           2d1h
```

- 클러스터 장애가 있었지만 실행 중이던 파드들은 아직 살아있는 것을 확인할 수 있다.

## 9.4. 노드 3 개 장애 case

- 결론: 3 개의 컨트롤 플레인 노드로 만든 HA k8s 클러스터는 fault-tolerance 가 1 이므로 노드 2 개가 장애이면 클러스터는 장애가 난다. 이럴 경우, 보통 기존에 장애난 컨트롤 플레인 노드들이 정상적으로 올라오면 해결된다. 하지만, 디스크 장애나 노드를 복구할 수 없는 상태가 왔을 때는 새로운 노드에 k8s 컨트롤 플레인을 재설치하고 etcd 데이터 백업본을 복원하는 방법이 있다. 예를 들면, 밑에 작성한 [9. HA k8s 장애 시에 복구 방법](#9-ha-k8s-장애-시에-복구-방법) 등을 참고하여 운영을 재기동할 수 있다.

m1, m2, m3 노드 셧다운

![](/.uploads/2021-06-14-00-16-18.png)

어느 마스터도 살아있지 않아 kubectl 통신 불가능

w1 에서 vip 로 통신 불가능

```bash
### w1 에서 수행
### 아래 명령어는 VIP:LBport 로 통신한 것
$ ping 10.0.0.162
PING 10.0.0.162 (10.0.0.162) 56(84) bytes of data.
From 10.0.0.166 icmp_seq=1 Destination Host Unreachable
From 10.0.0.166 icmp_seq=2 Destination Host Unreachable
--- 10.0.0.162 ping statistics ---
3 packets transmitted, 0 received, +2 errors, 100% packet loss, time 2000ms

$ nc -v 10.0.0.162 8443
nc: connect to 10.0.0.162 port 8443 (tcp) failed: No route to host
```

m1, m2, m3 리부트

![](/.uploads/2021-06-14-00-24-34.png)

클러스터 상태 확인

``` bash
### m1, m2, m3 에서 수행
$ k get no
NAME                  STATUS   ROLES                  AGE    VERSION
dtlab-dev-k8s-pjb-1   Ready    control-plane,master   4d9h   v1.20.7
dtlab-dev-k8s-pjb-2   Ready    control-plane,master   4d9h   v1.20.7
dtlab-dev-k8s-pjb-3   Ready    control-plane,master   4d9h   v1.20.7
dtlab-dev-k8s-pjb-4   Ready    <none>                 4d9h   v1.20.7
```

- m1, m2, m3 가 다시 Ready 상태가 된 것을 확인할 수 있다.

파드, 디플로이먼트 상태 확인

``` bash
$ k get po
NAME                               READY   STATUS    RESTARTS   AGE
nginx-6799fc88d8-2swfb             1/1     Running   0          4d6h
nginx-6799fc88d8-48bs2             1/1     Running   0          4d8h
nginx-6799fc88d8-5gcxf             1/1     Running   2          4d8h
nginx-6799fc88d8-5jxhk             1/1     Running   0          4d9h
nginx-6799fc88d8-79bnx             1/1     Running   0          4d8h
nginx-6799fc88d8-bczjc             1/1     Running   0          4d9h
nginx-6799fc88d8-d7qhb             1/1     Running   0          4d9h
nginx-6799fc88d8-fc6kr             1/1     Running   0          4d8h
nginx-6799fc88d8-gv2fx             1/1     Running   1          4d8h
nginx-6799fc88d8-gvq66             1/1     Running   0          4d8h
nginx-6799fc88d8-mkvtb             1/1     Running   2          4d8h
nginx-6799fc88d8-v5gwx             1/1     Running   0          4d9h
nginx-6799fc88d8-vks7m             1/1     Running   0          4d8h
nginx-after-bkp-798567d8b9-22j9t   1/1     Running   2          46h
nginx-after-bkp-798567d8b9-k5zth   1/1     Running   0          46h
nginx-after-bkp-798567d8b9-s8rqz   1/1     Running   1          46h
nginx-after-bkp-798567d8b9-wqq4b   1/1     Running   1          2d1h

$ k get deploy
NAME              READY   UP-TO-DATE   AVAILABLE   AGE
nginx             13/13   13           13          4d9h
nginx-after-bkp   4/4     4            4           2d1h
```

- 클러스터 장애가 있었지만 실행 중이던 파드들은 아직 살아있는 것을 확인할 수 있다.

# 10. HA k8s 장애 시에 복구 방법

- k8s 복구를 위해 etcd 백업 및 복구 방법을 사용할 것이다. 자세한 방법은 [여기](https://github.com/bellship24/study-k8s/blob/main/etc/etcd%20%EB%B0%B1%EC%97%85%EA%B3%BC%20%EB%B3%B5%EC%9B%90%20%EB%B0%A9%EB%B2%95.md) 에 정리해놨으니 참고하자.

## 10.1. etcd 백업

``` bash
$ sudo ETCDCTL_API=3 etcdctl --endpoints=https://127.0.0.1:2379 --cacert=/etc/kubernetes/pki/etcd/ca.crt --cert=/etc/kubernetes/pki/etcd/server.crt --key=/etc/kubernetes/pki/etcd/server.key snapshot save /home/myid/etcd-bkp-20210611.db
```

## 10.2. etcd 백업 확인

``` bash
$ sudo ETCDCTL_API=3 etcdctl snapshot status /home/myid/etcd-bkp-20210611.db \
--write-out=table
+----------+----------+------------+------------+
|   HASH   | REVISION | TOTAL KEYS | TOTAL SIZE |
+----------+----------+------------+------------+
| 107812bf |   344862 |       1262 |     4.3 MB |
+----------+----------+------------+------------+
```

## 10.3. 테스트를 위해 백업 이후에 워크로드 생성

``` bash
$ k create deploy nginx-after-bkp --replicas=4 --image=nginx
deployment.apps/nginx-after-bkp created

$ k get deploy nginx-after-bkp
NAME              READY   UP-TO-DATE   AVAILABLE   AGE
nginx-after-bkp   4/4     4            4           37s

$ k get po -l app=nginx-after-bkp -o wide
NAME                               READY   STATUS    RESTARTS   AGE   IP          NODE                  NOMINATED NODE   READINESS GATES
nginx-after-bkp-798567d8b9-kfnq8   1/1     Running   0          65s   10.42.0.0   dtlab-dev-k8s-pjb-1   <none>           <none>
nginx-after-bkp-798567d8b9-n7pw4   1/1     Running   0          65s   10.42.0.2   dtlab-dev-k8s-pjb-1   <none>           <none>
nginx-after-bkp-798567d8b9-qtnqm   1/1     Running   0          65s   10.42.0.1   dtlab-dev-k8s-pjb-1   <none>           <none>
nginx-after-bkp-798567d8b9-wqq4b   1/1     Running   0          65s   10.40.0.2   dtlab-dev-k8s-pjb-3   <none>           <none>
```

## 10.4. etcd 복원 및 데이터 볼륨 생성

- `etcdctl snapshot restore <etcd 백업본 경로>` 로 etcd 백업 파일을 복원한다.
- `--data-dir` 인자를 통해 별도 폴더에 복원 및 앞으로 etcd 에서 사용할 데이터 볼륨을 만들어주자.

```bash
$ ETCDCTL_API=3 etcdctl  --data-dir /var/lib/etcd-from-backup \
     snapshot restore /home/myid/etcd-bkp-20210611.db

```

## 10.5. etcd static pod 설정 변경

- 이제 etcd 에 대한 파드 설정을 바꿔주자. 보통 k8s 클러스터를 구축할 때 `kubeadm` 을 이용한다. 그러면, 다른 kube-system 컴포넌트들과 마찬가지로 etcd 도 static pod 로 마스터 노드들에 배포 된다. 그렇기 때문에 etcd 의 설정은 `/etc/kubernetes/manifests/etcd.yaml` 파일을 수정해줘야 한다. 바꿔야할 부분은 `--data-dir` 과 `volumes, containers[0].volumeMounts` 부분이다.

```yaml
...
spec:
  containers:
  - command:
    ...
    *--data-dir=/var/lib/etcd-from-backup*
    ...
    volumeMounts:
    - mountPath: */var/lib/etcd-from-backup*
      name: etcd-data
    ...
volumes:
  - hostPath:
      path: */var/lib/etcd-from-backup*
      type: DirectoryOrCreate
    name: etcd-data
    ...
```

## 10.6. 복원 완료 후 상태 확인

- 설정이 끝났으면 배포 상태를 확인해보자. 먼저 컨테이너를 확인해보자. 해당노드에서 아래와 같이 확인할 수 있다.

```bash
$ watch "docker ps | grep etcd"
d44732f897fc        d4ca8726196c                     "etcd --advertise-cl   "   19 seconds ago      Up 18 seconds                           k8s_etcd_etcd-contr
olplane_kube-system_0b2662ac7503a325433287de0dfa3888_0
67de4cd98f6a        k8s.gcr.io/pause:3.2             "/pause"                 20 seconds ago      Up 19 seconds                           k8s_POD_etcd-controlp
lane_kube-system_0b2662ac7503a325433287de0dfa3888_0
```

이제 파드도 확인해보자.

```bash
$ kubectl get po -n kube-system etcd-controlplane   
NAME                READY   STATUS    RESTARTS   AGE
etcd-controlplane   1/1     Running   0          4m52s
```

마지막으로 클러스터 내에 기존에 배포된 앱들이 정상적으로 배포됐는지 확인해보자.

```bash
$ kubectl get all
NAME                        READY   STATUS    RESTARTS   AGE
pod/blue-746c87566d-8m8c9   1/1     Running   0          2m26s
pod/blue-746c87566d-bjftc   1/1     Running   0          14m
pod/blue-746c87566d-bx6qr   1/1     Running   0          2m26s
pod/blue-746c87566d-kgnlm   1/1     Running   0          14m
pod/blue-746c87566d-tcsb5   1/1     Running   0          2m26s
pod/blue-746c87566d-z7s7x   1/1     Running   0          14m
pod/red-75f847bf79-8djnp    1/1     Running   0          2m26s
pod/red-75f847bf79-hdf7m    1/1     Running   0          14m
pod/red-75f847bf79-n7bvr    1/1     Running   0          2m26s
pod/red-75f847bf79-zgkhr    1/1     Running   0          14m

NAME                   TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
service/blue-service   NodePort    10.105.64.46     <none>        80:30082/TCP   14m
service/kubernetes     ClusterIP   10.96.0.1        <none>        443/TCP        17m
service/red-service    NodePort    10.102.100.212   <none>        80:30080/TCP   14m

NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/blue   0/3     0            0           14m
deployment.apps/red    0/2     0            0           14m

NAME                              DESIRED   CURRENT   READY   AGE
replicaset.apps/blue-746c87566d   3         0         0       14m
replicaset.apps/red-75f847bf79    2         0         0       14m
```