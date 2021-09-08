**목차**

- [1. TL;DR](#1-tldr)
- [2. 전제](#2-전제)
- [3. nfs 서버 구축 (nfs 서버)](#3-nfs-서버-구축-nfs-서버)
  - [3.1. nfs server 패키지 설치](#31-nfs-server-패키지-설치)
  - [3.2. 공유할 디렉토리 생성](#32-공유할-디렉토리-생성)
  - [3.3. /etc/exports 파일 수정](#33-etcexports-파일-수정)
  - [3.4. nfs 서버 재시작](#34-nfs-서버-재시작)
  - [3.5. nfs 서버 마운트 정책 적용 확인](#35-nfs-서버-마운트-정책-적용-확인)
- [4. (옵션) nfs 클라이언트 테스트 (nfs 클라이언트)](#4-옵션-nfs-클라이언트-테스트-nfs-클라이언트)
  - [4.1. nfs 서버를 마운트](#41-nfs-서버를-마운트)
  - [4.2. fstab 설정](#42-fstab-설정)
  - [4.3. RW 테스트](#43-rw-테스트)
- [5. (옵션) helm 설치](#5-옵션-helm-설치)
- [6. helm 으로 Client Provisioner 구축](#6-helm-으로-client-provisioner-구축)
  - [6.1. nfs-subdir-external-provisioner 의 helm repo 추가](#61-nfs-subdir-external-provisioner-의-helm-repo-추가)
  - [6.2. helm 으로 nfs-subdir-external-provisioner 설치](#62-helm-으로-nfs-subdir-external-provisioner-설치)
  - [6.3. 생성된 컴포넌트 확인](#63-생성된-컴포넌트-확인)
- [7. pvc 생성하여 dynamic provisioning 테스트](#7-pvc-생성하여-dynamic-provisioning-테스트)

**참고**

[블로그 : nfs-provisioner 설치](https://gustjd887.tistory.com/61)

---

# 1. TL;DR

k8s 에서 nfs 기반의 storageClass 를 구축해보자.
nfs 서버를 만들고 nfs 서버 정보를 참고하여 helm 차트로 nfs-provisioner 를 배포하면 nfs storageClass 를 간단히 구축할 수 있다.
nfs 를 storageClass 로 사용하면 다른 CSI 보다 구축 및 유지보수가 간단하다.

특히, 기존 stable 헬름 차트에 있던 `nfs-server-provisioner` 가 2020 말 차트 업데이트 지원이 중단되면서 최신 쿠버네티스 버전에 대한 업데이트가 끊겼다.
이에 대한 대안으로 k8s v1.20+ 에서도 적용가능한 `nfs-subdir-external-provisioner` 방법을 진행해보았다.

# 2. 전제

- k8s v1.20+
- helm v3
- kernel 에서 인터넷 사용 가능한 환경에서 apt 사용

# 3. nfs 서버 구축 (nfs 서버)

## 3.1. nfs server 패키지 설치

```bash
sudo apt update
sudo apt install -y nfs-common nfs-kernel-server portmap
```

## 3.2. 공유할 디렉토리 생성

```bash
sudo mkdir /nfs-sv
sudo chmod 777 /nfs-sv
```

## 3.3. /etc/exports 파일 수정

/etc/exports 파일 안에 내용 추가 (필요 시에 수정)

```bash
sudo bash -c "cat <<EOF >> /etc/exports

# K8s nfs-sv for storageClass by jongbae
/nfs-sv        10.0.0.0/24(rw,sync,no_subtree_check)
EOF
"
```

- 접근을 허용하고자

## 3.4. nfs 서버 재시작

```bash
sudo systemctl restart nfs-server
sudo systemctl enable nfs-server
sudo systemctl status nfs-server
```

## 3.5. nfs 서버 마운트 정책 적용 확인

```bash
$ showmount -e 127.0.0.1
Export list for 127.0.0.1:
/nfs-sv 10.0.0.0/24
```

# 4. (옵션) nfs 클라이언트 테스트 (nfs 클라이언트)

## 4.1. nfs 서버를 마운트

```bash
sudo mkdir -p /mnt/nfs-client

sudo mount -t nfs \
10.0.0.198:/nfs-sv \
/mnt/nfs-client
```

## 4.2. fstab 설정

```bash
sudo bash -c "cat <<EOF >> /etc/fstab

# k8s nfs storageClass mounting by jongbae
10.0.0.198:/nfs-sv /mnt/nfs-client nfs defaults,_netdev 0 0
EOF
"
```

## 4.3. RW 테스트

```bash
# Write
echo "test mount" > /mnt/nfs-client/test_mount_from_client

# Read
$ cat /mnt/nfs-client/test_mount_from_client
test mount
```

nfs 서버에서 수행

```bash
# nfs 서버에서 Read
$ cat /nfs-sv/test_mount_from_client
test mount
```

# 5. (옵션) helm 설치

helm 이 없을 경우에 진행

``` bash
$ curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash

$ helm version
version.BuildInfo{Version:"v3.4.2", GitCommit:"23dd3af5e19a02d4f4baa5b2f242645a1a3af629", GitTreeState:"clean", GoVersion:"go1.14.13"}
```

# 6. helm 으로 Client Provisioner 구축

## 6.1. nfs-subdir-external-provisioner 의 helm repo 추가

``` bash
helm repo add nfs-subdir-external-provisioner https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
helm repo update
helm repo ls
```

## 6.2. helm 으로 nfs-subdir-external-provisioner 설치

``` bash
helm upgrade --install \
    nfs-provisioner nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
    --namespace nfs-provisioner --create-namespace \
    --set nfs.server=10.0.0.164 \
    --set nfs.path=/nfs-sv \
    --set storageClass.defaultClass=true
```

- default 스토리지클래스로 사용할 것이 아니면 storageClass.defaultClass` 인자를 false 로 놓자.
- nfs.server 나 nfs.path 를 자신의 환경에 맞게 알맞게 사용하자.

## 6.3. 생성된 컴포넌트 확인

``` bash
$ k get all -n nfs-provisioner
NAME                                                                  READY   STATUS    RESTARTS   AGE
pod/nfs-provisioner-nfs-subdir-external-provisioner-5f6b5fdc8dbzlfk   1/1     Running   0          20s

NAME                                                              READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/nfs-provisioner-nfs-subdir-external-provisioner   1/1     1            1           20s

NAME                                                                         DESIRED   CURRENT   READY   AGE
replicaset.apps/nfs-provisioner-nfs-subdir-external-provisioner-5f6b5fdc8d   1         1         1       20s

$ k get sc
NAME                   PROVISIONER                                                     RECLAIMPOLICY   VOLUMEBINDINGMODE   ALLOWVOLUMEEXPANSION   AGE
nfs-client (default)   cluster.local/nfs-provisioner-nfs-subdir-external-provisioner   Delete          Immediate           true                   36s
```

# 7. pvc 생성하여 dynamic provisioning 테스트

`sc-test-pvc.yml` 작성

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: sc-test-pvc
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: nfs-client
  resources:
    requests:
      storage: 1Gi
```

`sc-test-po.yml` 작성

```yaml
apiVersion: v1
kind: Pod
metadata:
  creationTimestamp: null
  labels:
    run: sc-test-po
  name: sc-test-po
spec:
  containers:
  - image: nginx
    name: sc-test
    volumeMounts:
    - name: sc-vol-test
      mountPath: "/sc-vol-dir"
  volumes:
  - name: sc-vol-test
    persistentVolumeClaim:
      claimName: sc-test-pvc
```

컴포넌트 생성

``` bash
$ k create -f sc-test-pvc.yaml
$ k create -f sc-test-po.yaml
```

bound 확인

``` bash
$ k get po
NAME         READY   STATUS    RESTARTS   AGE
sc-test-po   1/1     Running   0          2m48s


$ k get pvc
NAME          STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
sc-test-pvc   Bound    pvc-830dc94e-0d31-4c86-bda8-b7e322da67d7   1Gi        RWX            nfs-client     2m55s
```