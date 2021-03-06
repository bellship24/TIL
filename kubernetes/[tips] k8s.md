**요약**

> kubernetes 관련 tip 들을 작성

**목차**

- [1. kubectl](#1-kubectl)
  - [1.1. 네임스페이스 소속 유무 리소스 확인](#11-네임스페이스-소속-유무-리소스-확인)
  - [1.2. `kubectl run` 에서 `command` 와 `args` 사용 방법](#12-kubectl-run-에서-command-와-args-사용-방법)
- [2. 클러스터 배포](#2-클러스터-배포)
  - [2.1. kubernetes 재설치 방법](#21-kubernetes-재설치-방법)
- [3. 스케줄링](#3-스케줄링)
  - [3.1. master node 에 pod schedule 활성화/비활성화 하기](#31-master-node-에-pod-schedule-활성화비활성화-하기)

---

# 1. kubectl

## 1.1. 네임스페이스 소속 유무 리소스 확인

``` bash
k api-resources --namespaced=<true/false>
```

## 1.2. `kubectl run` 에서 `command` 와 `args` 사용 방법

``` bash
### args 만 변경
kubectl run nginx --image=nginx -- <arg1> <arg2> ... <argN>

### command와 args 변경
kubectl run nginx --image=nginx --command -- <cmd> <arg1> ... <argN>

### e.g.
k run alpine-test --image=alpine --command -- /bin/ash -c ls -la
```

# 2. 클러스터 배포

## 2.1. kubernetes 재설치 방법

kubeadm 리셋

``` bash
$ sudo kubeadm reset
```

kube* 패키지 삭제

``` bash
## ubuntu
sudo apt remove -y kubelet kubeadm kubectl
### 삭제 확인
$ dpkg -l | grep kube
### 만약 그래도 남아 있다면, dpkg 로 삭제
sudo dpkg -P "kubelet" "kubeadm" "kubectl"

## centos
sudo yum remove -y kubelet, kubeadm, kubectl
### 삭제 확인
rpm -qa | grep kube
```

남은 폴더들 삭제

``` bash
$ rm -rf ~/.kube

## root 권한에서 실행
# rm -rf /etc/ceph \
       /var/lib/dockershim \
       /etc/cni \
       /etc/kubernetes \
       /var/run/kubernetes \
       /opt/cni \
       /opt/rke \
       /run/secrets/kubernetes.io \
       /run/calico \
       /run/flannel \
       /var/lib/calico \
       /var/lib/etcd \
       /var/lib/cni \
       /var/lib/kubelet \
       /var/lib/rancher/rke/log \
       /var/log/containers \
       /var/log/kube-audit \
       /var/log/pods \
       /var/run/calico \
       /run/weave \
       /var/lib/weave \
       /var/run/weave
```

iptable 재설정 및 도커 재시작, cgroup 확인

```bash
# iptables -F && iptables -X
# iptables -t nat -F && iptables -t nat -X
# iptables -t raw -F && iptables -t raw -X
# iptables -t mangle -F && iptables -t mangle -X

sudo systemctl restart docker.service

$ sudo docker info | grep -i cgroup
Cgroup Driver: systemd
Cgroup Version: 1

### 남은 컨테이너 확인
$ docker ps
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```

kube* 패키지 재설치

```bash
## ubuntu
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl
sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg
echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt install -y kubeadm=1.19.0-00 kubelet=1.19.0-00 kubectl=1.19.0-00
sudo apt-mark hold kubelet kubeadm kubectl

### 설치 확인
$ dpkg -l | grep kube
hi  kubeadm                                     1.19.0-00                                       amd64        Kubernetes Cluster Bootstrapping Tool
hi  kubectl                                     1.19.0-00                                       amd64        Kubernetes Command Line Tool
hi  kubelet                                     1.19.0-00                                       amd64        Kubernetes Node Agent
ii  kubernetes-cni                              0.8.7-00                                        amd64        Kubernetes CNI

## centos
sudo yum install -y kubelet-1.19.0-0.x86_64 kubeadm-1.19.0-0.x86_64 kubectl-1.19.0-0.x86_64 --disableexcludes=kubernetes
sudo systemctl enable kubelet

### 설치 확인
$ rpm -qa | grep kube
kubectl-1.19.0-0.x86_64
kubernetes-cni-0.8.7-0.x86_64
kubeadm-1.19.0-0.x86_64
kubelet-1.19.0-0.x86_64
```

클러스터 부트스트랩

```bash
## 마스터 1 에서 수행
## HA 구성으로 인해 로드밸런서를 앤드포인트로 할 때 control-plane-endpoint 옵션을 아래와 같이 추가
sudo kubeadm init  --upload-certs --control-plane-endpoint 10.0.0.168:8443
```

kubeconfig 설정

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

kubectl 설정

```bash
cat <<EOF >> ~/.bashrc
alias k=kubectl
EOF

source ~/.bashrc
```

cni weavenet 설치

```bash
kubectl apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"
```

- 이 외에 다른 cni 를 설치해도 된다.

그 외에 master 노드와 worker 노드 조인

``` bash
sudo kubeadm join 10.0.0.168:8443 --token 0cyr3y.7ri4fzeiodpnumnw \
    --discovery-token-ca-cert-hash sha256:43067b531c17b2e5f6cde99d94f4065398e3065fb5fd47dc335830218d19f43f \
    --control-plane --certificate-key 46bd2d459fd8d8573e4b57349de5507400f9a5cb4cd8869ce2b2c0638ee84bf0

sudo kubeadm join 10.0.0.168:8443 --token 0cyr3y.7ri4fzeiodpnumnw \
    --discovery-token-ca-cert-hash sha256:43067b531c17b2e5f6cde99d94f4065398e3065fb5fd47dc335830218d19f43f
```

클러스터 확인

```bash
$ k get no -o wide
NAME            STATUS   ROLES    AGE     VERSION   INTERNAL-IP    EXTERNAL-IP   OS-IMAGE                   KERNEL-VERSION           CONTAINER-RUNTIME
bellship-k8s-m1   Ready    master   12m     v1.19.0   10.0.0.165   <none>        Red Hat Enterprise Linux   3.10.0-1127.el7.x86_64   docker://20.10.7
bellship-k8s-m2   Ready    master   7m22s   v1.19.0   10.0.0.166   <none>        Red Hat Enterprise Linux   3.10.0-1127.el7.x86_64   docker://20.10.7
bellship-k8s-m3   Ready    master   6m33s   v1.19.0   10.0.0.167   <none>        Red Hat Enterprise Linux   3.10.0-1127.el7.x86_64   docker://20.10.7
```

# 3. 스케줄링

## 3.1. master node 에 pod schedule 활성화/비활성화 하기

기존 taint 확인

``` bash
$ k get no --show-labels
NAME                  STATUS   ROLES                  AGE     VERSION   LABELS
bellship-dev-k8s-pjb-1   Ready    control-plane,master   5d22h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=bellship-dev-k8s-pjb-1,kubernetes.io/os=linux,node-role.kubernetes.io/control-plane=,node-role.kubernetes.io/master=
bellship-dev-k8s-pjb-2   Ready    control-plane,master   5d22h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=bellship-dev-k8s-pjb-2,kubernetes.io/os=linux,node-role.kubernetes.io/control-plane=,node-role.kubernetes.io/master=
bellship-dev-k8s-pjb-3   Ready    control-plane,master   5d21h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=bellship-dev-k8s-pjb-3,kubernetes.io/os=linux,node-role.kubernetes.io/control-plane=,node-role.kubernetes.io/master=
bellship-dev-k8s-pjb-4   Ready    <none>                 5d20h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=bellship-dev-k8s-pjb-4,kubernetes.io/os=linux
```

마스터 노드에 `NoSchedule` taint 삭제

``` bash
$ kubectl taint nodes --all node-role.kubernetes.io/master-
node/bellship-dev-k8s-pjb-1 untainted
node/bellship-dev-k8s-pjb-2 untainted
node/bellship-dev-k8s-pjb-3 untainted
error: taint "node-role.kubernetes.io/master" not found

### 참고: etcd 레이블 제거하고 싶을 때
$ kubectl taint nodes --all node-role.kubernetes.io/etcd-
node/hci-dev-rancher-m01 untainted
taint "node-role.kubernetes.io/etcd" not found
taint "node-role.kubernetes.io/etcd" not found

### 참고: controlplane 레이블 제거하고 싶을 때
$ kubectl taint nodes --all node-role.kubernetes.io/controlplane-
node/hci-dev-rancher-m01 untainted
taint "node-role.kubernetes.io/controlplane" not found
taint "node-role.kubernetes.io/controlplane" not found

```

마스터 노드에 `NoSchedule` taint 추가

``` bash
$ k taint no <적용할 master node> node-role.kubernetes.io/master=:NoSchedule
<적용할 master node> tainted
```