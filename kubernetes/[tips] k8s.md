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

# 3. 스케줄링

## 3.1. master node 에 pod schedule 활성화/비활성화 하기

기존 taint 확인

``` bash
$ k get no --show-labels
NAME                  STATUS   ROLES                  AGE     VERSION   LABELS
dtlab-dev-k8s-pjb-1   Ready    control-plane,master   5d22h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=dtlab-dev-k8s-pjb-1,kubernetes.io/os=linux,node-role.kubernetes.io/control-plane=,node-role.kubernetes.io/master=
dtlab-dev-k8s-pjb-2   Ready    control-plane,master   5d22h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=dtlab-dev-k8s-pjb-2,kubernetes.io/os=linux,node-role.kubernetes.io/control-plane=,node-role.kubernetes.io/master=
dtlab-dev-k8s-pjb-3   Ready    control-plane,master   5d21h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=dtlab-dev-k8s-pjb-3,kubernetes.io/os=linux,node-role.kubernetes.io/control-plane=,node-role.kubernetes.io/master=
dtlab-dev-k8s-pjb-4   Ready    <none>                 5d20h   v1.20.7   beta.kubernetes.io/arch=amd64,beta.kubernetes.io/os=linux,kubernetes.io/arch=amd64,kubernetes.io/hostname=dtlab-dev-k8s-pjb-4,kubernetes.io/os=linux
```

마스터 노드에 `NoSchedule` taint 삭제

``` bash
$ kubectl taint nodes --all node-role.kubernetes.io/master-
node/dtlab-dev-k8s-pjb-1 untainted
node/dtlab-dev-k8s-pjb-2 untainted
node/dtlab-dev-k8s-pjb-3 untainted
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