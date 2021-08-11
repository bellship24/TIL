**목차**

- [1. 요약](#1-요약)
- [2. weavenet cni error 발생 시에 해결 방법](#2-weavenet-cni-error-발생-시에-해결-방법)
  - [2.1. 참고](#21-참고)
  - [2.2. 현상](#22-현상)
  - [2.3. 분석](#23-분석)
  - [2.4. 해결](#24-해결)

---

# 1. 요약

> kubernetes 관련 이슈들에 대해 트러블슈팅한 내용을 기록했다. 절차는 대부분 `현상 -> 분석 -> 해결` 로 진행했다.

# 2. weavenet cni error 발생 시에 해결 방법

## 2.1. 참고

- [[GitHub] Reset Kubernetes cluster with Weave](https://gist.github.com/carlosedp/5040f4a1b2c97c1fa260a3409b5f14f9)

## 2.2. 현상

- 신규 노드 증설 시에 아래와 같이 노드 상태는 Ready 혹은 NotReady 이지만 kube-system 에 cni 파드가 Running 이 되지 않고 Error 상태이며 CrashLoopBackOff 로 계속해서 RESTART 된다.

``` bash
$ k get no                     
NAME            STATUS   ROLES                  AGE     VERSION
mysv-03   Ready    <none>                 14m     v1.21.3
mysv-k8s-m1   Ready    control-plane,master   5d23h   v1.21.2
mysv-k8s-m2   Ready    control-plane,master   5d23h   v1.21.2
mysv-k8s-m3   Ready    control-plane,master   5d23h   v1.21.2

$ k get po -n kube-system                
NAME                                    READY   STATUS    RESTARTS   AGE
coredns-558bd4d5db-7xldn                1/1     Running   0          6d1h
coredns-558bd4d5db-cbpq6                1/1     Running   0          6d1h
etcd-mysv-k8s-m1                      1/1     Running   0          6d1h
etcd-mysv-k8s-m2                      1/1     Running   0          6d
etcd-mysv-k8s-m3                      1/1     Running   0          6d
kube-apiserver-mysv-k8s-m1            1/1     Running   0          6d1h
kube-apiserver-mysv-k8s-m2            1/1     Running   0          6d
kube-apiserver-mysv-k8s-m3            1/1     Running   0          6d
kube-controller-manager-mysv-k8s-m1   1/1     Running   1          6d1h
kube-controller-manager-mysv-k8s-m2   1/1     Running   0          6d
kube-controller-manager-mysv-k8s-m3   1/1     Running   0          6d
kube-proxy-7nhjk                        1/1     Running   0          6d1h
kube-proxy-h2stn                        1/1     Running   0          6d
kube-proxy-lmst8                        1/1     Running   0          103m
kube-proxy-q9vwd                        1/1     Running   0          6d
kube-scheduler-mysv-k8s-m1            1/1     Running   1          6d1h
kube-scheduler-mysv-k8s-m2            1/1     Running   0          6d
kube-scheduler-mysv-k8s-m3            1/1     Running   0          6d
weave-net-7m2jh                         2/2     Running   1          171m
weave-net-fr4ns                         2/2     Running   1          171m
weave-net-gtc52                         1/2     Error     19         87m
weave-net-rflwc                         2/2     Running   1          171m
```

## 2.3. 분석

error 가 발생한 weave 파드의 로그 조회

``` bash
$ k logs -n kube-system weave-net-2v7sz weave
DEBU: 2021/07/26 06:35:20.752834 [kube-peers] Checking peer "36:41:2b:46:51:08" against list &{[{8e:6d:25:f1:60:79 mysv-k8s-m1} {8a:91:43:86:68:9f mysv-k8s-m2} {a6:fd:4b:89:d1:f0 mysv-k8s-m3}]} 
Peer not in list; removing persisted data
INFO: 2021/07/26 06:35:20.863653 Command line options: map[conn-limit:200 datapath:datapath db-prefix:/weavedb/weave-net docker-api: expect-npc:true http-addr:127.0.0.1:6784 ipalloc-init:consensus=3 ipalloc-range:10.32.0.0/12 metrics-addr:0.0.0.0:6782 name:36:41:2b:46:51:08 nickname:mysv-03 no-dns:true no-masq-local:true port:6783]
INFO: 2021/07/26 06:35:20.863690 weave  2.8.1
FATA: 2021/07/26 06:35:21.336686 Inconsistent bridge state detected. Please do 'weave reset' and try again
```

- bridge 의 상태가 부적절하다고 `weave reset` 을 수행하라는 에러 로그가 보인다.

## 2.4. 해결

`weave reset` 을 수행하자.

``` bash
# Drain and delete the nodes (for each node you have)
kubectl drain kubenode1 --delete-local-data --force --ignore-daemonsets
kubectl delete node kubenode1

# Reset the deployment
sudo kubeadm reset

# On each node

## Reset the nodes and weave
sudo curl -L git.io/weave -o /usr/local/bin/weave
sudo chmod a+x /usr/local/bin/weave
sudo kubeadm reset

sudo weave reset --force

## Clean weave binaries
sudo rm /opt/cni/bin/weave-*

## Flush iptables rules on all nodes and restart Docker
# iptables -P INPUT ACCEPT && \
  iptables -P FORWARD ACCEPT && \
  iptables -P OUTPUT ACCEPT && \
  iptables -t nat -F && \
  iptables -t mangle -F && \
  iptables -F && \
  iptables -X && \
  systemctl restart docker
```

- 해당 노드를 클러스터에서 drain 하고 delete 한다.
- 해당 노드에서 `kubeadm reset` 을 수행한다.
- 해당 노드에서 weave 관련 파일들을 받고 바이너리 등록을 한 뒤에 `weave reset` 을 수행한다.
- cni 경로에서 weave 바이너리를 삭제한다.
- iptables rule 들을 삭제하고 도커를 재시작하면 해결된다.

클러스터 재구성 후에 확인

``` bash
$ k get no
NAME            STATUS   ROLES                  AGE    VERSION
mysv-03   Ready    <none>                 35m    v1.21.3
mysv-k8s-m1   Ready    control-plane,master   6d1h   v1.21.2
mysv-k8s-m2   Ready    control-plane,master   6d1h   v1.21.2
mysv-k8s-m3   Ready    control-plane,master   6d1h   v1.21.2

$ k get po -n kube-system
NAME                                    READY   STATUS    RESTARTS   AGE
coredns-558bd4d5db-7xldn                1/1     Running   0          6d1h
coredns-558bd4d5db-cbpq6                1/1     Running   0          6d1h
etcd-mysv-k8s-m1                      1/1     Running   0          6d1h
etcd-mysv-k8s-m2                      1/1     Running   0          6d1h
etcd-mysv-k8s-m3                      1/1     Running   0          6d1h
kube-apiserver-mysv-k8s-m1            1/1     Running   0          6d1h
kube-apiserver-mysv-k8s-m2            1/1     Running   0          6d1h
kube-apiserver-mysv-k8s-m3            1/1     Running   0          6d1h
kube-controller-manager-mysv-k8s-m1   1/1     Running   1          6d1h
kube-controller-manager-mysv-k8s-m2   1/1     Running   0          6d1h
kube-controller-manager-mysv-k8s-m3   1/1     Running   0          6d1h
kube-proxy-7nhjk                        1/1     Running   0          6d1h
kube-proxy-9q6z4                        1/1     Running   0          35m
kube-proxy-h2stn                        1/1     Running   0          6d1h
kube-proxy-q9vwd                        1/1     Running   0          6d1h
kube-scheduler-mysv-k8s-m1            1/1     Running   1          6d1h
kube-scheduler-mysv-k8s-m2            1/1     Running   0          6d1h
kube-scheduler-mysv-k8s-m3            1/1     Running   0          6d1h
weave-net-7m2jh                         2/2     Running   1          3h37m
weave-net-d59j5                         2/2     Running   1          35m
weave-net-fr4ns                         2/2     Running   1          3h37m
weave-net-rflwc                         2/2     Running   1          3h37m
```