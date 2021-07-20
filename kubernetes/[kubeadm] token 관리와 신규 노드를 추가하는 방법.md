**목차**

- [1. kubeadm token 이란?](#1-kubeadm-token-이란)
- [2. kubeadm token 명령어들](#2-kubeadm-token-명령어들)
- [3. 신규 노드 join 하는 방법](#3-신규-노드-join-하는-방법)

**요약**

kubeadm 을 사용해 클러스터를 관리하면, token 을 관리할 줄 알아야 한다. 이 token 을 검토하자.

**참고**

- [[k8s docs] kubeadm token](https://kubernetes.io/docs/reference/setup-tools/kubeadm/kubeadm-token/)

---

# 1. kubeadm token 이란?

부트스트랩 토큰은 클러스터에 join 하는 노드와 컨트롤플레인 노드 간에 양방향 신뢰를 구성하기 위해 사용된다.

# 2. kubeadm token 명령어들

`kubeadm init`

- `kubeadm init` 은 k8s 클러스터를 부트스트랩하면서 24 시간 동안 유효한 초기 토큰을 생성한다.

`kubeadm token list` : 토큰 목록을 조회

``` bash
$ kubeadm token list
TOKEN                     TTL         EXPIRES                     USAGES                   DESCRIPTION                                                EXTRA GROUPS
hypppu.ircr2b2j7m8fvmry   23h         2021-07-16T16:45:51+09:00   authentication,signing   <none>                                                     system:bootstrappers:kubeadm:default-node-token
```

`kubeadm token create` : 토큰을 새롭게 생성할 수 있다.

``` bash
kubeadm token create [token]
```

- `[token]` 으로 토큰의 이름을 명시하지않으면 자동 할당된다.

`kubeadm token delete` : 토큰 삭제

`kubeadm token generate` : 토큰 string 만 생성

`--print-join-command` : join 명령어 포함한 토큰 생성

``` bash
$ kubeadm token create --print-join-command
kubeadm join 10.0.0.116:6443 --token hypppu.ircr2b2j7m8fvmry --discovery-token-ca-cert-hash sha256:e32215ab5ecf730c9c4a5a24a2f486e273aa05fa7c6b05c16d5485c672b783a4

$ kubeadm token list                                             TOKEN                     TTL         EXPIRES                     USAGES                   DESCRIPTION                                                EXTRA GROUPS
hypppu.ircr2b2j7m8fvmry   23h         2021-07-16T16:45:51+09:00   authentication,signing   <none>                                                     system:bootstrappers:kubeadm:default-node-token
```

# 3. 신규 노드 join 하는 방법

토큰 재발급 (기존 마스터 노드)

```bash
### 기존 마스터 노드에서 수행
$ kubeadm token create --print-join-command
kubeadm join 10.0.0.116:6443 --token tqrvur.24yq5f7cx9hyrymi --discovery-token-ca-cert-hash sha256:e32215ab5ecf730c9c4a5a24a2f486e273aa05fa7c6b05c16d5485c672b783a4          
                             
$ kubeadm token list
TOKEN                     TTL         EXPIRES                     USAGES                   DESCRIPTION                                                EXTRA GROUPS
tqrvur.24yq5f7cx9hyrymi   23h         2021-07-16T17:25:45+09:00   authentication,signing   <none>                                                     system:bootstrappers:kubeadm:default-node-token
```

신규 노드가 master 라면 `certficiate-key` 생성

```bash
### 기존 마스터 노드에서 수행
$ kubeadm certs certificate-key
61be2eebdd1668dd023897477e799033f1c4c0494457e6dcfc1a5a92bfd03020
```

- `certificate-key` 는 컨트롤플레인 인증서를 클러스터에 있는 `kubeadm-certs` secret 으로부터 다운 받아 복호화하는 역할을 한다.

kubeadm join 명령어 수행

```bash
### join 할 신규 노드에서 수행

### 워커 노드 추가
sudo kubeadm join 10.0.0.116:6443 \
 --token tqrvur.24yq5f7cx9hyrymi \
 --discovery-token-ca-cert-hash sha256:e32215ab5ecf730c9c4a5a24a2f486e273aa05fa7c6b05c16d5485c672b783a4

### 마스터 노드 추가
sudo kubeadm join 10.0.0.116:6443 \
 --control-plane \
 --certificate-key 61be2eebdd1668dd023897477e799033f1c4c0494457e6dcfc1a5a92bfd03020 \
 --token tqrvur.24yq5f7cx9hyrymi \
 --discovery-token-ca-cert-hash sha256:e32215ab5ecf730c9c4a5a24a2f486e273aa05fa7c6b05c16d5485c672b783a4
```