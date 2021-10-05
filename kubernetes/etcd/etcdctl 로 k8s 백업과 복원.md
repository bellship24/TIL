**목차**

- [1. 요약](#1-요약)
- [etcdctl 설치 (m1 노드)](#etcdctl-설치-m1-노드)
- [etcd 백업](#etcd-백업)
- [etcd 백업 확인](#etcd-백업-확인)
- [백업 파일을 etcd 노드들에 복사 (etcd 노드들 대상)](#백업-파일을-etcd-노드들에-복사-etcd-노드들-대상)
- [etcd 복원](#etcd-복원)
- [etcd 복원 확인](#etcd-복원-확인)
- [복원된 etcd 데이터를 k8s 에 적용](#복원된-etcd-데이터를-k8s-에-적용)
- [클러스터 상태 확인](#클러스터-상태-확인)

**참고**

- [](https://)

---

# 1. 요약

# etcdctl 설치 (m1 노드)

``` bash
ETCD_VER=v3.3.12
wget https://storage.googleapis.com/etcd/${ETCD_VER}/etcd-${ETCD_VER}-linux-amd64.tar.gz
tar xzvf etcd-${ETCD_VER}-linux-amd64.tar.gz
sudo mv etcd-${ETCD_VER}-linux-amd64/etcdctl /usr/local/bin/etcdctl
etcdctl --version
rm -rf etcd-${ETCD_VER}-linux-amd64/
rm -f etcd-${ETCD_VER}-linux-amd64.tar.gz
```

# etcd 백업

``` bash
sudo ETCDCTL_API=3 etcdctl --endpoints=https://127.0.0.1:2379 \
--cacert=/etc/kubernetes/pki/etcd/ca.crt \
--cert=/etc/kubernetes/pki/etcd/server.crt \
--key=/etc/kubernetes/pki/etcd/server.key \
snapshot save /opt/snapshot-pre-boot.db
```

# etcd 백업 확인

``` bash
$ ETCDCTL_API=3 etcdctl snapshot status /opt/snapshot-pre-boot.db \
--write-out=table

+----------+----------+------------+------------+
|   HASH   | REVISION | TOTAL KEYS | TOTAL SIZE |
+----------+----------+------------+------------+
| a75dd0d3 |     1675 |       1683 |     2.6 MB |
+----------+----------+------------+------------+
```

# 백업 파일을 etcd 노드들에 복사 (etcd 노드들 대상)

- 만약, 마스터 노드를 HA 구성했거나 etcd 를 unstack 으로 클러스터 구성했다면, 각 모든 etcd 노드에서 아래 작업들을 수행해야 함.

(백업한 m1 에서 수행)

``` bash
scp /opt/snapshot-pre-boot.db <다른 마스터노드 id>@<다른 마스터노드 ip>:/opt/snapshot-pre-boot.db
```

# etcd 복원

- 각 etcd 노드들에서 복사해놓은 백업본을 통해 복원해준다.

``` bash
$ ETCDCTL_API=3 etcdctl  --data-dir /var/lib/etcd-from-backup \
     snapshot restore /opt/snapshot-pre-boot.db

{"level":"info","ts":1614603871.9047706,"caller":"snapshot/v3_snapshot.go:296","msg":"restoring snapshot","path":"/opt/snapshot-pre-boot.db","wal-dir":"/var/lib/etcd-from-backup/member/wal","data-dir":"/var/lib/etcd-from-backup","snap-dir":"/var/lib/etcd-from-backup/member/snap"}
{"level":"info","ts":1614603871.9488358,"caller":"membership/cluster.go:392","msg":"added member","cluster-id":"cdf818194e3a8c32","local-member-id":"0","added-peer-id":"8e9e05c52164694d","added-peer-peer-urls":["http://localhost:2380"]}
{"level":"info","ts":1614603871.9771183,"caller":"snapshot/v3_snapshot.go:309","msg":"restored snapshot","path":"/opt/snapshot-pre-boot.db","wal-dir":"/var/lib/etcd-from-backup/member/wal","data-dir":"/var/lib/etcd-from-backup","snap-dir":"/var/lib/etcd-from-backup/member/snap"}
```

- 그런데 여기서 주의해야 할 사항이 하나 있다. 위 명령어는 etcd 노드에서 수행했다는 점이다. 때문에 별도 인자가 필요하지 않았다. 상황에 따라 --initial-cluster 등의 인자를 추가해줘야 한다.

# etcd 복원 확인

``` bash
$ sudo ls /var/lib/etcd-from-backup/member
snap  wal
```

- `.db` 확장자의 백업본이 정상적인 etcd 파일 구조로 복원된 것을 확인했다.

# 복원된 etcd 데이터를 k8s 에 적용

`etcd.yaml` 수정

- 이제 etcd 에 대한 파드 설정을 바꿔주자. 보통 k8s 클러스터를 구축할 때 kubeadm 을 이용한다. 그러면, 다른 kube-system 컴포넌트들과 마찬가지로 etcd 도 static pod 로 마스터 노드들에 배포 된다. 그렇기 때문에 etcd 의 설정은 /etc/kubernetes/manifests/etcd.yaml 파일을 수정해줘야 한다. 바꿔야할 부분은 --data-dir 과 volumes, containers[0].volumeMounts 부분이다.

`/etc/kubernetes/manifests/etcd.yaml`

``` bash
...
spec:
  containers:
  - command:
    ...
    --data-dir=/var/lib/etcd-from-backup
    ...
    volumeMounts:
    - mountPath: /var/lib/etcd-from-backup
      name: etcd-data
    ...
volumes:
  - hostPath:
      path: /var/lib/etcd-from-backup
      type: DirectoryOrCreate
    name: etcd-data
    ...
```

`kube-apiserver.yaml` 수정

- 만약 그럴 일은 거의 없을 테지만 복원 시에, etcd 클러스터의 노드가 변경되어 access URL 도 변경 됐다면, kube-apiserver.yaml 에서 --etcd-servers 인자에 대해 변경된 URL 로 바꿔줘야 한다.

# 클러스터 상태 확인

etcd 컨테이너 상태 확인

``` bash
$ watch "docker ps | grep etcd"
d44732f897fc        d4ca8726196c                     "etcd --advertise-cl   "   19 seconds ago      Up 18 seconds                           k8s_etcd_etcd-contr
olplane_kube-system_0b2662ac7503a325433287de0dfa3888_0
67de4cd98f6a        k8s.gcr.io/pause:3.2             "/pause"                 20 seconds ago      Up 19 seconds                           k8s_POD_etcd-controlp
lane_kube-system_0b2662ac7503a325433287de0dfa3888_0
```

static pod 들의 상태 확인

``` bash
kubectl get po -n kube-system -w
```

백업했던 클러스터 상태 복원됐는지 확인

``` bash
kubectl get all
```