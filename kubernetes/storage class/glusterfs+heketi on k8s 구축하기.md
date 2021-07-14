
# 요약

- on-prem kubernetes 의 dynamic provisioning 을 위해 glusterFS+Heketi 를 사용할 수 있다. glusterFS 는 block 과 file 스토리지를 지원하는 SDS(Software Defined Storage) 이고 heketi 는 glusterFS volume 을 관리하는 RESTful api 서버이다. ceph 에 비해 구축이 쉬우며 PiB 이상의 대용량 데이터 운용에 대한 성능이 좋다고 알려져 있다.
- 아키텍처는 크게 두 가지로 검증했다. 첫 번째 k8s 클러스터 노드들 위에 stacked 하거나 unstacked 하여 별도의 monolithic GlusterFS 클러스터를 구축한 뒤 storageClass 로 연동하여 쓰는 방법이 있다. 두 번째는 k8s 위에 GlusterFS 를 daemonset 으로 배포하여 구성을 좀 더 유연하고 loose coupling 하게 가져갈 수 있다. 하지만, 후자의 경우, 자료가 많지 않고 공식 자료들도 꽤 오래된 자료들이다. 하지만 이 자료들을 적당히 커스터마이징하여 구축해보았다.

# 1. Monolithic glusterFS 검증

## 1.1 장점

- k8s 와 별도로 구성하여 클러스터 장애와 독립적으로 사용할 수 있다.
- k8s 도메인을 고려하지 않고 일반적인 glusterFS 조건만 고려하여 구축할 수 있다.

## 1.2 아키텍처

![](/.uploads/2021-05-24-15-44-51.png)

- glusterFS 를 stacked 하였는데 unstacked 하여도 된다.

## 1.3 전제 및 기본 설정

디스크 추가

- 모든 glusterFS 노드에 새로운 블록 디스크 100GB 추가했으며 /dev/sdb 로 추가되었다.

도메인네임 설정

```bash
# 모든 노드에서 수행
# 자신의 환경에 맞게 설정

# cat <<EOF>> /etc/hosts

# glusterFS
10.0.0.1 heketi
10.0.0.2 k8s-1
10.0.0.3 k8s-2
10.0.0.4 k8s-3
EOF
```

기본 환경 설정

```bash
# 필요에 맞게 환경변수 설정
# 모든 노드에서 수행
export VOL_NAME=gfs_ldcc_volume
export NUMBER_OF_NODES=3
export DOMAIN_NAME1=k8s-1
export DOMAIN_NAME2=k8s-2
export DOMAIN_NAME3=k8s-3
export GFS_DIR=/gfs_brick
export MNT_DIR=/mnt/gfs_mnt
```

기본 패키지 설치

```bash
# 모든 노드에서 수행
sudo apt-get -y install thin-provisioning-tools
```

SSHkey copy

```bash
# 모든 노드에서 수행
# 각 노드에서 ssh key 생성
ssh-keygen -t rsa

# 각 노드의 ssh 공개키를 각 노드에 옮기기
## e.g.
ssh-copy-id -i ~/.ssh/id_rsa.pub <계정명>@<IP>
```

## 1.4 GlusterFS 패키지 설치 및 연동 설정

```bash
### 모든 노드에서 수행
### 7 버전 설치

### 관련 기본 패키지 설치
apt install software-properties-common

### apt 에 gluster7 에 대한 GPG key 추가
add-apt-repository ppa:gluster/glusterfs-7

### apt 업데이트 
apt update

### glusterfs 서버 설치
apt install glusterfs-server

### glusterd start, enable, 상태 확인
sudo systemctl start glusterd
sudo systemctl enable glusterd
sudo systemctl status glusterd

### gluterd 버전 확인
$ glusterd --version
glusterfs 7.9
Repository revision: git://git.gluster.org/glusterfs.git
Copyright (c) 2006-2016 Red Hat, Inc. <https://www.gluster.org/>
GlusterFS comes with ABSOLUTELY NO WARRANTY.
It is licensed to you under your choice of the GNU Lesser
General Public License, version 3 or any later version (LGPLv3
or later), or the GNU General Public License, version 2 (GPLv2),
in all cases as published by the Free Software Foundation.

### 디렉토리 생성
# 모든 노드에서 수행
sudo mkdir -p $GFS_DIR

### peer 로 trusted pool 설정
# sv1 에서 수행
$ sudo gluster peer probe $DOMAIN_NAME2
$ sudo gluster peer probe $DOMAIN_NAME3
peer probe: success.
peer probe: success.

### peer 상태 점검
# sv1 에서 수행
$ sudo gluster peer status
Number of Peers: 2

Hostname: k8s-2
Uuid: 15661d1b-4f1f-4c98-8e78-00983217d462
State: Peer in Cluster (Connected)

Hostname: k8s-3
Uuid: 737f7413-9749-490a-b64b-84f05087731d
State: Peer in Cluster (Connected)
```

## 1.5 Heketi 패키지 설치 및 환경 설정

한 노드에서 수행하면 된다. 이 예제에서는 master 에서 설치하자.

```bash
wget https://github.com/heketi/heketi/releases/download/v9.0.0/heketi-v9.0.0.linux.amd64.tar.gz
tar -zxvf heketi-v9.0.0.linux.amd64.tar.gz
```

bin 설정

```bash
chmod +x heketi/{heketi,heketi-cli}
sudo cp heketi/{heketi,heketi-cli} /usr/local/bin
```

heketi 작동 확인

```bash
$ heketi --version
Heketi v9.0.0

$ heketi-cli --version
heketi-cli v9.0.0
```

heketi 에 대한 user/group 추가

```bash
sudo groupadd --system heketi
sudo useradd -s /sbin/nologin --system -g heketi heketi
```

heketi 디렉토리 생성

```bash
sudo mkdir -p /var/lib/heketi /etc/heketi /var/log/heketi
sudo chown -R heketi:heketi /var/lib/heketi /etc/heketi /var/log/heketi
```

- /var/lib/heketi : container.log 와 heketi.db 가 있음
- /etc/heketi : heketi.json 이 있음
- /var/log/heketi : heketi log 파일이 있음

heketi 환경 설정

```bash
vim /etc/heketi/heketi.json
```

```json
{
  "_port_comment": "Heketi Server Port Number",
  "port": "8080",

	"_enable_tls_comment": "Enable TLS in Heketi Server",
	"enable_tls": false,

	"_cert_file_comment": "Path to a valid certificate file",
	"cert_file": "",

	"_key_file_comment": "Path to a valid private key file",
	"key_file": "",

  "_use_auth": "Enable JWT authorization. Please enable for deployment",
  "use_auth": false,

  "_jwt": "Private keys for access",
  "jwt": {
    "_admin": "Admin has access to all APIs",
    "admin": {
      "key": "<KEY_HERE>"
    },
    "_user": "User only has access to /volumes endpoint",
    "user": {
      "key": "<KEY_HERE>"
    }
  },

  "_backup_db_to_kube_secret": "Backup the heketi database to a Kubernetes secret when running in Kubernetes. Default is off.",
  "backup_db_to_kube_secret": false,

  "_profiling": "Enable go/pprof profiling on the /debug/pprof endpoints.",
  "profiling": false,

  "_glusterfs_comment": "GlusterFS Configuration",
  "glusterfs": {
    "_executor_comment": [
      "Execute plugin. Possible choices: mock, ssh",
      "mock: This setting is used for testing and development.",
      "      It will not send commands to any node.",
      "ssh:  This setting will notify Heketi to ssh to the nodes.",
      "      It will need the values in sshexec to be configured.",
      "kubernetes: Communicate with GlusterFS containers over",
      "            Kubernetes exec api."
    ],
    "executor": "ssh",

    "_sshexec_comment": "SSH username and private key file information",
    "sshexec": {
      "keyfile": "/etc/heketi/heketi_key",
      "user": "root",
      "port": "22",
      "fstab": "/etc/fstab"
    },

    "_db_comment": "Database file name",
    "db": "/var/lib/heketi/heketi.db",

     "_refresh_time_monitor_gluster_nodes": "Refresh time in seconds to monitor Gluster nodes",
    "refresh_time_monitor_gluster_nodes": 120,

    "_start_time_monitor_gluster_nodes": "Start time in seconds to monitor Gluster nodes when the heketi comes up",
    "start_time_monitor_gluster_nodes": 10,

    "_loglevel_comment": [
      "Set log level. Choices are:",
      "  none, critical, error, warning, info, debug",
      "Default is warning"
    ],
    "loglevel" : "debug",

    "_auto_create_block_hosting_volume": "Creates Block Hosting volumes automatically if not found or exsisting volume exhausted",
    "auto_create_block_hosting_volume": true,

    "_block_hosting_volume_size": "New block hosting volume will be created in size mentioned, This is considered only if auto-create is enabled.",
    "block_hosting_volume_size": 500,

    "_block_hosting_volume_options": "New block hosting volume will be created with the following set of options. Removing the group gluster-block option is NOT recommended. Additional options can be added next to it separated by a comma.",
    "block_hosting_volume_options": "group gluster-block",

    "_pre_request_volume_options": "Volume options that will be applied for all volumes created. Can be overridden by volume options in volume create request.",
    "pre_request_volume_options": "",

    "_post_request_volume_options": "Volume options that will be applied for all volumes created. To be used to override volume options in volume create request.",
    "post_request_volume_options": ""
  }
}
```

핵심 부분은 jwt 의 admin 과 user 의 key 와 executor 의 ssh 에 대한 설정이다.

Heketi 에서 필요한 kernel module 확인 및 로드하기

```bash
### module 로드 확인
$ for i in dm_snapshot dm_mirror dm_thin_pool; do lsmod | grep $i; done
dm_snapshot            40960  0
dm_bufio               28672  2 dm_persistent_data,dm_snapshot
dm_mirror              24576  0
dm_region_hash         20480  1 dm_mirror
dm_log                 20480  2 dm_region_hash,dm_mirror
dm_thin_pool           69632  4
dm_persistent_data     73728  1 dm_thin_pool
dm_bio_prison          20480  1 dm_thin_pool

### 위에서 해당 모듈이 로드 안 되어 있다면 아래와 같이 로드 수행
for i in dm_snapshot dm_mirror dm_thin_pool; do sudo modprobe $i; done
```

다른 호스트에 대한 접근을 위해 Heketi API 전용 SSH key 생성하고 클러스터 내에 호스트들에게 복사

```bash
$ sudo ssh-keygen -f /etc/heketi/heketi_key -t rsa -N ''
$ sudo chown heketi:heketi /etc/heketi/heketi_key*
# for i in $DOMAIN_NAME1 $DOMAIN_NAME2 $DOMAIN_NAME3; do
  ssh-copy-id -i /etc/heketi/heketi_key.pub root@$i
done

# root 계정으로 로그인이 안 되어 수동으로 key copy 를 해야 할 때 아래와 같이 수행
# 각 호스트의 root 로 접근하여 수행
# 다른 명령어를 사용해도 됨
# for i in $DOMAIN_NAME1 $DOMAIN_NAME2 $DOMAIN_NAME3; do
  cat /etc/heketi/heketi_key.pub | ssh <해당 호스트의 다른 계정>@$i “sudo tee -a /root/.ssh/authorized_keys”
done
```

heketi systemd 파일 생성

```bash
# cat <<EOF > /etc/systemd/system/heketi.service
[Unit]
Description=LDCC Heketi Server

[Service]
Type=simple
WorkingDirectory=/var/lib/heketi
EnvironmentFile=-/etc/heketi/heketi.env
User=heketi
ExecStart=/usr/local/bin/heketi --config=/etc/heketi/heketi.json
Restart=on-failure
StandardOutput=syslog
StandardError=syslog

[Install]
WantedBy=multi-user.target
EOF
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now heketi
sudo systemctl start heketi
```

서비스 활성화 확인

```bash
curl http://heketi:8080/hello
Hello from Heketi
```

## 1.6 heketi 인증 정보 등록

```bash
$ cat <<EOF >> ~/.bashrc
export HEKETI_CLI_SERVER=http://heketi:8080 
export HEKETI_CLI_USER=admin 
export HEKETI_CLI_KEY="<KEY_HERE>"
EOF

$ source ~/.bashrc
```

## 1.7 Heketi topology 생성 및 적용

heketi topology 란?

- heketi 서비스를 정상적으로 시작했다면, glusterfs 를 연동할 볼륨 정보를 가지고 있는 토폴로지를 생성 및 적용한다. 현재 상황에 맞게 아래 정보를 수정하여 사용하자.
- `devices` 는 string array 이며 파일시스템이 아닌 raw한 블록스토리지(block device. 즉, HDD 나 SSD 를 뜻함.)로써 추가되는 각 디스크의 이름이다.

예제

```bash
cat <<EOF > /etc/heketi/topology.json
{
  "clusters": [
    {
      "nodes": [
                    {
          "node": {
            "hostnames": {
              "manage": [
                "gluster00"
              ],
              "storage": [
                "10.1.1.2"
              ]
            },
            "zone": 1
          },
          "devices": [
            "/dev/vdc","/dev/vdd","/dev/vde"
          ]
        },            {
          "node": {
            "hostnames": {
              "manage": [
                "gluster01"
              ],
              "storage": [
                "10.1.1.3"
              ]
            },
            "zone": 1
          },
          "devices": [
            "/dev/vdc","/dev/vdd","/dev/vde"
          ]
        },            {
          "node": {
            "hostnames": {
              "manage": [
                "gluster02"
              ],
              "storage": [
                "10.1.1.4"
              ]
            },
            "zone": 1
          },
          "devices": [
            "/dev/vdc","/dev/vdd","/dev/vde"
          ]
        }              
      ]
    }
  ]
}
EOF
```

토폴로지 로드

```bash
$ heketi-cli topology load --json=/etc/heketi/topology.json
        Found node k8s-1 on cluster 8678ac207082130f7d0cdd9505eeaf74
                Adding device /dev/sdb ... OK
        Found node k8s-2 on cluster 8678ac207082130f7d0cdd9505eeaf74
                Adding device /dev/sdb ... OK
        Found node k8s-3 on cluster 8678ac207082130f7d0cdd9505eeaf74
                Adding device /dev/sdb ... OK
```

## 1.8 Heketi 서비스 테스트

- heketi 를 통해 볼륨이 생성되는지 테스트하자.

클러스터 목록과 노드 목록 확인

```bash
$ heketi-cli cluster list
Clusters:
Id:8678ac207082130f7d0cdd9505eeaf74 [file][block]

$ heketi-cli node list
Id:2b1210ed00c97f6aae6ae8a11fad6c10     Cluster:8678ac207082130f7d0cdd9505eeaf74
Id:40b54e7db7d6adfd6c1bc29eb8f324f0     Cluster:8678ac207082130f7d0cdd9505eeaf74
Id:64f3d7c23ac3b147980d851e1f45eb9e     Cluster:8678ac207082130f7d0cdd9505eeaf74
```

1GB 볼륨 생성 테스트 및 확인

```bash
$ heketi-cli volume create --size=1
Name: vol_e182296584dbe8de7c8ce465399fbbb3
Size: 1
Volume Id: e182296584dbe8de7c8ce465399fbbb3
Cluster Id: 8678ac207082130f7d0cdd9505eeaf74
Mount: 10.0.0.1:vol_e182296584dbe8de7c8ce465399fbbb3
Mount Options: backup-volfile-servers=10.0.0.2,10.0.0.3
Block: false
Free Size: 0
Reserved Size: 0
Block Hosting Restriction: (none)
Block Volumes: []
Durability Type: replicate
Distribute Count: 1

$ heketi-cli volume list
Id:e182296584dbe8de7c8ce465399fbbb3
Cluster:8678ac207082130f7d0cdd9505eeaf74
Name:vol_e182296584dbe8de7c8ce465399fbbb3

$ heketi-cli volume info e182296584dbe8de7c8ce465399fbbb3
Name: vol_e182296584dbe8de7c8ce465399fbbb3
Size: 1
Volume Id: e182296584dbe8de7c8ce465399fbbb3
Cluster Id: 8678ac207082130f7d0cdd9505eeaf74
Mount: 10.0.0.3:vol_e182296584dbe8de7c8ce465399fbbb3
Mount Options: backup-volfile-servers=10.0.0.2,10.0.0.1
Block: false
Free Size: 0
Reserved Size: 0
Block Hosting Restriction: (none)
Block Volumes: []
Durability Type: replicate
Distribute Count: 1
Replica Count: 3
```

## 1.9 Kubernetes 와 GlusterFS 연동

스토리지 클래스 생성

```bash
$ mkdir -p src-jongbae/jongbae-gluter-k8s-0503 && cd $_

$ cat <<EOF > gfs-sc1.yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gluster-heketi
provisioner: kubernetes.io/glusterfs
parameters:
  resturl: "http://10.0.0.1:8080"
  restuser: "admin"
  restuserkey: "<KEY_HERE>"
EOF

$ kubectl apply -f gfs-sc1.yaml
storageclass.storage.k8s.io/gluster-heketi created

$ kubectl get sc
NAME             PROVISIONER               RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
gluster-heketi   kubernetes.io/glusterfs   Delete          Immediate              false                  28s
```

pvc 생성

```bash
$ cat <<EOF > gfs-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: gvol-1
spec:
  storageClassName: gluster-heketi
  accessModes:
  - ReadWriteMany
  resources:
    requests:
      storage: 1Gi
EOF

$ kubectl apply -f gfs-pvc.yaml
persistentvolumeclaim/gvol-1 created

$ kubectl get pvc
NAME                                          STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS     AGE
gvol-1                                        Bound    pvc-919b90a4-c2a9-4b82-9e2f-a462ca75d0e4   1Gi        RWX            gluster-heketi   17s
```

PVC 를 사용한 Pod 생성

```bash
$ cat <<EOF > gfs-client.yaml
apiVersion: apps/v1 
kind: Deployment 
metadata: 
  name: gfs-client 
spec: 
  replicas: 2 
  selector: 
    matchLabels: 
      app: ubuntu 
  template: 
    metadata: 
      labels: 
        app: ubuntu 
    spec: 
      containers: 
      - name: ubuntu 
        image: ubuntu 
        volumeMounts: 
        - name: gfs 
          mountPath: /mnt 
        command: ["/usr/bin/tail","-f","/dev/null"] 
      volumes: 
      - name: gfs 
        persistentVolumeClaim: 
          claimName: gvol-1
EOF

$ kubectl apply -f gfs-client.yaml
deployment.apps/gfs-client created

$ kubectl get deploy,pod
NAME                                               READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/gfs-client                         2/2     2            2           81s
NAME                                                    READY   STATUS    RESTARTS   AGE
pod/gfs-client-6557c98dbd-mtbdn                         1/1     Running   0          81s
pod/gfs-client-6557c98dbd-vjpbx                         1/1     Running   0          81s

## pod 안에서 pv 마운트 확인
$ kubectl exec -ti pod/gfs-client-6557c98dbd-mtbdn -- df -Th
Filesystem                                          Type            Size  Used Avail Use% Mounted on
overlay                                             overlay          97G   15G   78G  17% /
tmpfs                                               tmpfs            64M     0   64M   0% /dev
tmpfs                                               tmpfs           3.9G     0  3.9G   0% /sys/fs/cgroup
**10.231.238.199:vol_65fff457453f74d07774c422260e1bad fuse.glusterfs 1014M   44M  971M   5% /mnt**
/dev/mapper/ubuntu--vg-ubuntu--lv                   ext4             97G   15G   78G  17% /etc/hosts
shm                                                 tmpfs            64M     0   64M   0% /dev/shm
tmpfs                                               tmpfs           3.9G   12K  3.9G   1% /run/secrets/kubernetes.io/serviceaccount
tmpfs                                               tmpfs           3.9G     0  3.9G   0% /proc/acpi
tmpfs                                               tmpfs           3.9G     0  3.9G   0% /proc/scsi
tmpfs                                               tmpfs           3.9G     0  3.9G   0% /sys/firmware
```

# 2. glusterFS on k8s 검증

glusterFS 를 kubernetes 위에 올려 hyper-converged service 를 할 수 있다.

## 2.1 장점

- glusterfs 의 배포가 쉬워지고 yaml 로 관리할 수 있다.
- glusterfs volume 을 유연하게 구성할 수 있다.

## 2.2 아키텍처

![](/.uploads/2021-05-24-15-54-36.png)

## 2.3 전제 및 기본 설정

전제

- 파드를 스케줄링할 수 있는 최소 3 노드 이상의 k8s 클러스터와 관리자급 접근 권한이 필요하다.
- 각 노드들은 heketi 가 전용으로 쓰기 위한 비어있는 블록 디바이스 1 개 이상이 필요하다. 앞서 각 노드에 대해 /dev/sdc 로 100GiB 를 추가했다.
- glusterFS 의 통신을 위한 포트 목록
    - 2222 - GlusterFS pod's sshd.
    - 24007 - GlusterFS Daemon.
    - 24008 - GlusterFS Management.
    - 49152 ~ 49251 - 각 host 에서 volume 들의 각 brick 의 포트. 49152 부터 시작함.

heketi GitHub 저장소 클론

```bash
git clone https://github.com/heketi/heketi
cd heketi/extras/kubernetes
```

glusterfs 노드 라벨링

- glusterfs 로 사용할 노드에 라벨을 추가한다. 여기서는 모든 노드에 라벨링을 추가했다.

```bash
$ kubectl label node storagenode=glusterfs --all
node/m01 labeled
node/w01 labeled
node/w02 labeled
```

노드 확인

```bash
$ kubectl get no
NAME                  STATUS   ROLES               AGE   VERSION
m01   Ready    controlplane,etcd   85m   v1.20.6
w01   Ready    worker              72m   v1.20.6
w02   Ready    worker              72m   v1.20.6
```

(옵션) master taint 수정

- master 노드 에서 Pod 띄울 수 있게 Taint 재설정
- glusterfs 파드가 최소 3개가 되어야 하기 때문에 여기서는 master 노드에 파드를 띄울 수 있게 설정을 한 것이다.

```bash
### 기존 taint 확인
$ kubectl describe node -m01
...
Taints:             node-role.kubernetes.io/etcd=true:NoExecute
										node-role.kubernetes.io/controlplane=true:NoSchedule

### 마스터 노드들에서 NoSchedule Taint 삭제
$ kubectl taint nodes --all node-role.kubernetes.io/etcd-
node/m01 untainted
taint "node-role.kubernetes.io/etcd" not found
taint "node-role.kubernetes.io/etcd" not found

$ kubectl taint nodes --all node-role.kubernetes.io/controlplane-
node/m01 untainted
taint "node-role.kubernetes.io/controlplane" not found
taint "node-role.kubernetes.io/controlplane" not found
```

## 2.4 glusterfs 구축

glusterfs 매니페스트 작성

`glusterfs-daemonset.json`

```json
{
    "kind": "DaemonSet",
    "apiVersion": "apps/v1",
    "metadata": {
        "name": "glusterfs",
        "labels": {
            "glusterfs": "deployment"
        },
        "annotations": {
            "description": "GlusterFS Daemon Set",
            "tags": "glusterfs"
        }
    },
    "spec": {
        "selector": {
            "matchLabels": {
                "glusterfs-node": "daemonset"
            }
        },
        "template": {
            "metadata": {
                "name": "glusterfs",
                "labels": {
                    "glusterfs-node": "daemonset"
                }
            },
            "spec": {
                "nodeSelector": {
                    "storagenode" : "glusterfs"
                },
                "hostNetwork": true,
                "containers": [
                    {
                        "image": "gluster/gluster-centos:latest",
                        "imagePullPolicy": "Always",
                        "name": "glusterfs",
                        "volumeMounts": [
                            {
                                "name": "glusterfs-heketi",
                                "mountPath": "/var/lib/heketi"
                            },
                            {
                                "name": "glusterfs-run",
                                "mountPath": "/run"
                            },
                            {
                                "name": "glusterfs-lvm",
                                "mountPath": "/run/lvm"
                            },
                            {
                                "name": "glusterfs-etc",
                                "mountPath": "/etc/glusterfs"
                            },
                            {
                                "name": "glusterfs-logs",
                                "mountPath": "/var/log/glusterfs"
                            },
                            {
                                "name": "glusterfs-config",
                                "mountPath": "/var/lib/glusterd"
                            },
                            {
                                "name": "glusterfs-dev",
                                "mountPath": "/dev"
                            },
                            {
                                "name": "glusterfs-cgroup",
                                "mountPath": "/sys/fs/cgroup"
                            }
                        ],
                        "securityContext": {
                            "capabilities": {},
                            "privileged": true
                        },
                        "readinessProbe": {
                            "timeoutSeconds": 3,
                            "initialDelaySeconds": 60,
                            "exec": {
                                "command": [
                                    "/bin/bash",
                                    "-c",
                                    "systemctl status glusterd.service"
                                ]
                            }
                        },
                        "livenessProbe": {
                            "timeoutSeconds": 3,
                            "initialDelaySeconds": 60,
                            "exec": {
                                "command": [
                                    "/bin/bash",
                                    "-c",
                                    "systemctl status glusterd.service"
                                ]
                            }
                        }
                    }
                ],
                "volumes": [
                    {
                        "name": "glusterfs-heketi",
                        "hostPath": {
                            "path": "/var/lib/heketi"
                        }
                    },
                    {
                        "name": "glusterfs-run"
                    },
                    {
                        "name": "glusterfs-lvm",
                        "hostPath": {
                            "path": "/run/lvm"
                        }
                    },
                    {
                        "name": "glusterfs-etc",
                        "hostPath": {
                            "path": "/etc/glusterfs"
                        }
                    },
                    {
                        "name": "glusterfs-logs",
                        "hostPath": {
                            "path": "/var/log/glusterfs"
                        }
                    },
                    {
                        "name": "glusterfs-config",
                        "hostPath": {
                            "path": "/var/lib/glusterd"
                        }
                    },
                    {
                        "name": "glusterfs-dev",
                        "hostPath": {
                            "path": "/dev"
                        }
                    },
                    {
                        "name": "glusterfs-cgroup",
                        "hostPath": {
                            "path": "/sys/fs/cgroup"
                        }
                    }
                ]
            }
        }
    }
}
```

glusterfs Daemonset 생성

```bash
$ kubectl create -f glusterfs-daemonset.json
daemonset.apps/glusterfs created
```

파드 확인

```bash
$ kubectl get pods
NAME              READY   STATUS    RESTARTS   AGE
glusterfs-54q6c   1/1     Running   0          71s
glusterfs-nj82z   1/1     Running   0          71s
glusterfs-zwvvv   1/1     Running   0          71s
```

glusterfs peering 확인

```bash
### m01 노드에 생성된 gluster 파드로 접근
$ k exec -ti glusterfs-54q6c -- bash

### 글러스터 연동 상태 확인
# gluster peer status
Number of Peers: 2

Hostname: w01
Uuid: a5aaef77-722c-4cd3-9b39-64640b6d7cbb
State: Peer in Cluster (Connected)

Hostname: w02
Uuid: b2f4c1f5-1c6b-4bf0-a806-49e1ceb4d5a2
State: Peer in Cluster (Connected)
```

## 2.5 heketi 계정 생성

heketi ServiceAccount 생성

```bash
$ kubectl create -f heketi-service-account.json
serviceaccount/heketi-service-account created
```

heketi ServiceAccount 에 대한 ClusterRoleBinding 생성

- heketi serviceaccount 가 gluster Pod 제어할 수 있게 생성

```bash
$ kubectl create clusterrolebinding heketi-gluster-admin --clusterrole=edit --serviceaccount=default:heketi-service-account
clusterrolebinding.rbac.authorization.k8s.io/heketi-gluster-admin created
```

## 2.6 heketi.json 설정

heketi.json 예제 복사

- 앞서 클론한 git 에 예제가 있다.
- 백업을 해놓자.

`heketi/extras/kubernetes/heketi.json`

```bash
cp heketi.json heketi.json.bkp
```

`heketi.json` 업데이트

- jwt 패스워드 부분을 업데이트했다.

```json
{
  "_port_comment": "Heketi Server Port Number",
  "port": "8080",
"_use_auth": "Enable JWT authorization. Please enable for deployment",
  "use_auth": false,

  "_jwt": "Private keys for access",
  "jwt": {
    "_admin": "Admin has access to all APIs",
    "admin": {
      "key": "<KEY_HERE>"
    },
    "_user": "User only has access to /volumes endpoint",
    "user": {
      "key": "<KEY_HERE>"
    }
  },

  "_glusterfs_comment": "GlusterFS Configuration",
  "glusterfs": {
    "_executor_comment": "Execute plugin. Possible choices: mock, kubernetes, ssh",
    "executor": "kubernetes",

    "_db_comment": "Database file name",
    "db": "/var/lib/heketi/heketi.db",

    "kubeexec": {
      "rebalance_on_expansion": true
    },

    "sshexec": {
      "rebalance_on_expansion": true,
      "keyfile": "/etc/heketi/private_key",
      "fstab": "/etc/fstab",
      "port": "22",
      "user": "root",
      "sudo": false
    }
  },

  "_backup_db_to_kube_secret": "Backup the heketi database to a Kubernetes secret when running in Kubernetes. Default is off.",
  "backup_db_to_kube_secret": false
}
```

heketi.json 으로 secret 생성

```bash
$ kubectl create secret generic heketi-config-secret --from-file=./heketi.json
secret/heketi-config-secret created
```

## 2.7 heketi topology 설정

topology.json 수정 및 Secret 생성

- 토폴로지는 glusterFS 로 사용될 모든 노드, 디스크, 클러스터에 대한 정보를 리스트로 담은 JSON 매니페스트이다.
- 토폴로지 예제는 [여기서](https://github.com/heketi/heketi/blob/master/client/cli/go/topology-sample.json) 볼 수 있다.

실제 사용 topology.json

`vi topology.json`

```json
{
  "clusters": [
    {
      "nodes": [
                    {
          "node": {
            "hostnames": {
              "manage": [
                "m01"
              ],
              "storage": [
                "10.0.0.1"
              ]
            },
            "zone": 1
          },
          "devices": [
             "/dev/sdb"
          ]
        },            {
          "node": {
            "hostnames": {
              "manage": [
                "w01"
              ],
              "storage": [
                "10.0.0.2"
              ]
            },
            "zone": 1
          },
          "devices": [
             "/dev/sdb"
          ]
        },            {
          "node": {
            "hostnames": {
              "manage": [
				"w02"
              ],
              "storage": [
                "10.0.0.3"
              ]
            },
            "zone": 1
          },
          "devices": [
            "/dev/sdb"
          ]
        }
      ]
    }
  ]
}
```

- 각 노드에 대해 `hostnames.manage` 에 호스트네임을 입력
- 각 노드에 대해 `hostnames.storage` 에 ip 를 입력
- 각 노드에 대해 `devices` 에 raw block device 를 입력

topology.json 으로 secret 생성

```bash
$ kubectl create secret generic heketi-topology-secret --from-file=./topology.json
secret/heketi-topology-secret created
```

## 2.8 heketi bootstrap 을 통한 초기 설정

heketi-bootstrap 매니페스트 수정

```bash
{
  "kind": "List",
  "apiVersion": "v1",
  "items": [
    {
      "kind": "Service",
      "apiVersion": "v1",
      "metadata": {
        "name": "deploy-heketi",
        "labels": {
          "glusterfs": "heketi-service",
          "deploy-heketi": "support"
        },
        "annotations": {
          "description": "Exposes Heketi Service"
        }
      },
      "spec": {
        "type": "NodePort",
        "selector": {
          "name": "deploy-heketi"
        },
        "ports": [
          {
            "name": "deploy-heketi",
            "port": 8080,
            "targetPort": 8080,
            "nodePort": 31808
          }
        ]
      }
    },
    {
      "kind": "Deployment",
      "apiVersion": "apps/v1",
      "metadata": {
        "name": "deploy-heketi",
        "labels": {
          "glusterfs": "heketi-deployment",
          "deploy-heketi": "deployment"
        },
        "annotations": {
          "description": "Defines how to deploy Heketi"
        }
      },
      "spec": {
        "selector": {
          "matchLabels": {
            "name": "deploy-heketi",
            "glusterfs": "heketi-pod",
            "deploy-heketi": "pod"
          }
        },
        "replicas": 1,
        "template": {
          "metadata": {
            "name": "deploy-heketi",
            "labels": {
              "name": "deploy-heketi",
              "glusterfs": "heketi-pod",
              "deploy-heketi": "pod"
            }
          },
          "spec": {
            "serviceAccountName": "heketi-service-account",
            "containers": [
              {
                "image": "heketi/heketi:dev",
                "imagePullPolicy": "Always",
                "name": "deploy-heketi",
                "env": [
                  {
                    "name": "HEKETI_EXECUTOR",
                    "value": "kubernetes"
                  },
                  {
                    "name": "HEKETI_DB_PATH",
                    "value": "/var/lib/heketi/heketi.db"
                  },
                  {
                    "name": "HEKETI_FSTAB",
                    "value": "/var/lib/heketi/fstab"
                  },
                  {
                    "name": "HEKETI_SNAPSHOT_LIMIT",
                    "value": "14"
                  },
                  {
                    "name": "HEKETI_KUBE_GLUSTER_DAEMONSET",
                    "value": "y"
                  },
                  {
                    "name": "HEKETI_CLI_SERVER",
                    "value": "http://localhost:8080"
                  },
                  {
                    "name": "HEKETI_CLI_USER",
                    "value": "admin"
                  },
                  {
                    "name": "HEKETI_CLI_KEY",
                    "value": "<KEY_HERE>"
                  }
                ],
                "ports": [
                  {
                    "containerPort": 8080
                  }
                ],
                "volumeMounts": [
                  {
                    "name": "db",
                    "mountPath": "/var/lib/heketi"
                  },
                  {
                    "name": "config",
                    "mountPath": "/etc/heketi"
                  },
                  {
                    "name": "topology",
                    "mountPath": "/myData"
                  },
                  {
                    "name": "heketi-storage-vol",
                    "mountPath": "/myHeketiData"
                  }
                ],
                "readinessProbe": {
                  "timeoutSeconds": 3,
                  "initialDelaySeconds": 3,
                  "httpGet": {
                    "path": "/hello",
                    "port": 8080
                  }
                },
                "livenessProbe": {
                  "timeoutSeconds": 3,
                  "initialDelaySeconds": 30,
                  "httpGet": {
                    "path": "/hello",
                    "port": 8080
                  }
                }
              }
            ],
            "volumes": [
              {
                "name": "db"
              },
              {
                "name": "config",
                "secret": {
                  "secretName": "heketi-config-secret"
                }
              },
              {
                "name": "topology",
                "secret": {
                  "secretName": "heketi-topology-secret"
                }
              },
              {
                "name": "heketi-storage-vol",
                "hostPath": {
                  "path": "/data/heketi",
                  "type": "Directory"
                }
              }
            ]
          }
        }
      }
    }
  ]
}
```

heketi-bootstrap 의 service, deployment 생성

```bash
$ kubectl create -f heketi-bootstrap.json
service/deploy-heketi created
deployment.apps/deploy-heketi created
```

배포 확인

```bash
$ kubectl get po -l name=deploy-heketi
NAME                             READY   STATUS    RESTARTS   AGE
deploy-heketi-794d8d9b58-bn5qx   1/1     Running   0          44s
```

heketi-cli 로 초기 설정

```bash
### heketi 파드로 접속
$ k exec -ti deploy-heketi-794d8d9b58-bn5qx -- bash

### heketi-cli 버전 확인
heketi-cli --version
heketi-cli v10.0.0-60-ge812490e

### 앞서 추가한 heketi-cli 환경변수 조회
# printenv | grep -i HEKETI_CLI
HEKETI_CLI_USER=admin
HEKETI_CLI_KEY=<KEY_HERE>
HEKETI_CLI_SERVER=http://localhost:8080

### heketi-cli 로 heketi api 호출 테스트
# curl $HEKETI_CLI_SERVER/hello
Hello from Heketi

### heketi-cli 로 topology 로드
# heketi-cli topology load --json=/myData/topology.json
Creating cluster ... ID: ea3f8e10a8f26b8baf00ad11e743f6cb
	Allowing file volumes on cluster.
	Allowing block volumes on cluster.
	Creating node -m01 ... ID: 135574b2feaf0d5a72b459036e650f99
		Adding device /dev/sdb ... OK
	Creating node -w01 ... ID: 7ea27a24e80c7fd496bf46589087052a
		Adding device /dev/sdb ... OK
	Creating node -w02 ... ID: 6d878583abe9def8c7df3153c9268fc4
		Adding device /dev/sdb ... OK
```

볼륨 생성

```bash
### glusterfs 위에 volume 으로 올릴 heketi db 구축 (heketi 를 위한 openshift/kubernetes persistent storage 구축)
# heketi-cli setup-openshift-heketi-storage
Saving heketi-storage.json

### 앞선 명령어로 생성된 heketi-storage.json 확인
# ls | grep heketi
heketi-storage.json

### 해당 파일을 kubectl 명령 가능한 곳으로 이동시켜서 실행하기
$ kubectl create -f heketi-storage.json
secret/heketi-storage-secret created
endpoints/heketi-storage-endpoints created
service/heketi-storage-endpoints created
job.batch/heketi-storage-copy-job created

### glusterfs 위에 volume 으로 올라간 heketi db 데이터 확인
# heketi-cli volume list
Id:20bc9b93ef4649bab99116bdcf428177    Cluster:5b303a6f30542924eb769d3ef1240f82    Name:heketidbstorage
```

부트스트랩 삭제

```bash
$ kubectl delete all,service,jobs,deployment,secret --selector="deploy-heketi"
pod "deploy-heketi-794d8d9b58-mghjm" deleted
service "deploy-heketi" deleted
deployment.apps "deploy-heketi" deleted
replicaset.apps "deploy-heketi-794d8d9b58" deleted
job.batch "heketi-storage-copy-job" deleted
secret "heketi-storage-secret" deleted
```

## 2.9 heketi Deployment 구축

heketi 인증정보 환경변수 등록을 위한 heketi Deployment 편집

`heketi-deployment.json`

```json
{
  "kind": "List",
  "apiVersion": "v1",
  "items": [
    {
      "kind": "Secret",
      "apiVersion": "v1",
      "metadata": {
        "name": "heketi-db-backup",
        "labels": {
          "glusterfs": "heketi-db",
          "heketi": "db"
        }
      },
      "data": {
      },
      "type": "Opaque"
    },
    {
      "kind": "Service",
      "apiVersion": "v1",
      "metadata": {
        "name": "heketi",
        "labels": {
          "glusterfs": "heketi-service",
          "deploy-heketi": "support"
        },
        "annotations": {
          "description": "Exposes Heketi Service"
        }
      },
      "spec": {
        "type": "NodePort",
        "selector": {
          "name": "heketi"
        },
        "ports": [
          {
            "name": "heketi",
            "port": 8080,
            "targetPort": 8080,
            "nodePort": 31808
          }
        ]
      }
    },
    {
      "kind": "Deployment",
      "apiVersion": "apps/v1",
      "metadata": {
        "name": "heketi",
        "labels": {
          "glusterfs": "heketi-deployment"
        },
        "annotations": {
          "description": "Defines how to deploy Heketi"
        }
      },
      "spec": {
        "selector": {
          "matchLabels": {
            "glusterfs": "heketi-pod",
            "name": "heketi"
          }
        },
        "replicas": 1,
        "template": {
          "metadata": {
            "name": "heketi",
            "labels": {
              "name": "heketi",
              "glusterfs": "heketi-pod"
            }
          },
          "spec": {
            "serviceAccountName": "heketi-service-account",
            "containers": [
              {
                "image": "heketi/heketi:dev",
                "imagePullPolicy": "Always",
                "name": "heketi",
                "env": [
                  {
                    "name": "HEKETI_EXECUTOR",
                    "value": "kubernetes"
                  },
                  {
                    "name": "HEKETI_DB_PATH",
                    "value": "/var/lib/heketi/heketi.db"
                  },
                  {
                    "name": "HEKETI_FSTAB",
                    "value": "/var/lib/heketi/fstab"
                  },
                  {
                    "name": "HEKETI_SNAPSHOT_LIMIT",
                    "value": "14"
                  },
                  {
                    "name": "HEKETI_KUBE_GLUSTER_DAEMONSET",
                    "value": "y"
                  },
                  {
                    "name": "HEKETI_CLI_SERVER",
                    "value": "http://localhost:8080"
                  },
                  {
                    "name": "HEKETI_CLI_USER",
                    "value": "admin"
                  },
                  {
                    "name": "HEKETI_CLI_KEY",
                    "value": "<KEY_HERE>"
                  }
                ],
                "ports": [
                  {
                    "containerPort": 8080
                  }
                ],
                "volumeMounts": [
                  {
                    "mountPath": "/backupdb",
                    "name": "heketi-db-secret"
                  },
                  {
                    "name": "db",
                    "mountPath": "/var/lib/heketi"
                  },
                  {
                    "name": "config",
                    "mountPath": "/etc/heketi"
                  },
                  {
                    "name": "topology",
                    "mountPath": "/myData/"
                  }
                ],
                "readinessProbe": {
                  "timeoutSeconds": 3,
                  "initialDelaySeconds": 3,
                  "httpGet": {
                    "path": "/hello",
                    "port": 8080
                  }
                },
                "livenessProbe": {
                  "timeoutSeconds": 3,
                  "initialDelaySeconds": 30,
                  "httpGet": {
                    "path": "/hello",
                    "port": 8080
                  }
                }
              }
            ],
            "volumes": [
              {
                "name": "db",
                "glusterfs": {
                  "endpoints": "heketi-storage-endpoints",
                  "path": "heketidbstorage"
                }
              },
              {
                "name": "heketi-db-secret",
                "secret": {
                  "secretName": "heketi-db-backup"
                }
              },
              {
                "name": "config",
                "secret": {
                  "secretName": "heketi-config-secret"
                }
              },
              {
                "name": "topology",
                "secret": {
                  "secretName": "heketi-topology-secret"
                }
              }              
            ]
          }
        }
      }
    }
  ]
}
```

heketi Deployment 생성 및 연동 유지 확인

```bash
$ kubectl create -f heketi-deployment.json
secret/heketi-db-backup created
service/heketi created
deployment.apps/heketi created
```

배포 확인

```bash
k get po -l name=heketi
NAME                     READY   STATUS    RESTARTS   AGE
heketi-8bfb875c8-tqqrv   1/1     Running   0          37s
```

연동 유지 확인

```bash
$ k exec -ti heketi-8bfb875c8-tqqrv -- heketi-cli cluster list 
Clusters:
Id:5b303a6f30542924eb769d3ef1240f82 [file][block]

$ k exec -ti heketi-8bfb875c8-tqqrv -- heketi-cli volume list
Id:20bc9b93ef4649bab99116bdcf428177    Cluster:5b303a6f30542924eb769d3ef1240f82    Name:heketidbstorage
```

- 이제 heketi db 가 glusterFS 볼륨으로 지속되어 heketi 파드가 재시작 될 때마다 heketi 와 gluster 연동이 리셋되지 않는다.
- `heketi-cli cluster list` 및 `heketi-cli volume list` 와 같은 명령을 사용하여 이전에 설정된 클러스터가 존재하고 Heketi 가 부트 스트랩 단계에서 생성 된 db 스토리지 볼륨을 인식하고 있는지 확인하자.

생성한 svc 로 통신 확인

```bash
### svc 확인
$ k get svc -l glusterfs=heketi-service
NAME     TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)          AGE
heketi   NodePort   10.43.113.26   <none>        8080:31808/TCP   12m

### ep 확인
$ k get ep -l glusterfs=heketi-service
NAME     ENDPOINTS           AGE
heketi   10.42.50.149:8080   13m

### heketi pod 로 접근 및 svc 도메인 네임으로 curl 테스트
$ k exec -ti heketi-8bfb875c8-tqqrv -- bash
# curl heketi:8080/hello
Hello from Heketi

### 환경 변수 설정 외에 heketi-cli 로 인수를 줘서 curl 테스트
# heketi-cli --server http://heketi:8080 --user admin --secret <KEY_HERE> cluster list
Clusters:
Id:5b303a6f30542924eb769d3ef1240f82 [file][block]
```

## 2.10 Dynamic Provisioning 테스트

구축한 GlusterFS+Heketi on k8s 에 대한 Storage Class 를 만들고 Dynamic Provisioning 으로 PVC 를 연동한 hello world Nginx 애플리케이션 배포해보자.

기타 사항

- 기본적으로 user_authorization 은 disabled 되어 있다. 만약, enabled 되어 있다면, REST user 와 secret key 를 찾아야 할 수도 있다. 이를 StorageClass 에서 사용할 때  패스워드에 대한 secret 을 구성하여 credential 을 전달할 수도 있다.
- 사용할 hello world nginx 는 5gb 스토리지를 request 한다.

restuser 패스워드 Secret 생성

```bash
$ kubectl create secret generic heketi-secret \
  --type="kubernetes.io/glusterfs" --from-literal=key='<KEY_HERE>' \
  --namespace=default
secret/heketi-secret created
```

StorageClass 매니페스트 작성

`gfs-sc1.yaml`

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gluster-heketi
provisioner: kubernetes.io/glusterfs
parameters:
  resturl: 
    "http://10.43.113.26:8080" # svc clusterIP : port [o]
  restuser: "admin"
  secretNamespace: "default"
  secretName: "heketi-secret"
```

StorageClass 생성 및 확인

```bash
$ k create -f gfs-sc1.yaml
storageclass.storage.k8s.io/gluster-heketi created

$ k get sc
NAME             PROVISIONER               RECLAIMPOLICY   VOLUMEBINDINGMODE   ALLOWVOLUMEEXPANSION   AGE
gluster-heketi   kubernetes.io/glusterfs   Delete          Immediate           false                  111s
```

테스트용 pvc 매니페스트 작성

`gluster-pvc.yaml`

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
 name: gluster1
 annotations:
   volume.beta.kubernetes.io/storage-class: gluster-heketi
spec:
 accessModes:
  - ReadWriteOnce
 resources:
   requests:
     storage: 5Gi
```

pvc 생성 및 바운드 확인

```bash
$ k create -f gluster-pvc.yaml
persistentvolumeclaim/gluster1 created

$ k get pvc
NAME       STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS     AGE
gluster1   Bound    pvc-83742baa-59f9-411c-966a-5c14a392a2ab   5Gi        RWO            gluster-heketi   94s
```

pv 자동 생성 및 바운드 확인

```bash
$ k get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM              STORAGECLASS     REASON   AGE
pvc-83742baa-59f9-411c-966a-5c14a392a2ab   5Gi        RWO            Delete           Bound    default/gluster1   gluster-heketi            28m
```

nginx Pod 매니페스트 작성

`nginx-pod.yaml`

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod1
  labels:
    name: nginx-pod1
spec:
  containers:nginx
  - name: nginx-pod1
    image: gcr.io/google_containers/nginx-slim:0.8
    ports:
    - name: web
      containerPort: 80
    volumeMounts:
    - name: gluster-vol1
      mountPath: /usr/share/nginx/html
  volumes:
  - name: gluster-vol1
    persistentVolumeClaim:
      claimName: gluster1
```

- 앞서 생성한 pvc 를 사용하도록 설정

nginx Pod 생성

```bash
$ k create -f nginx-pod.yaml
pod/nginx-pod1 created
```

nginx Pod 확인

```bash
$ k get po -o wide -l name=nginx-pod1
NAME         READY   STATUS    RESTARTS   AGE    IP             NODE                  NOMINATED NODE   READINESS GATES
nginx-pod1   1/1     Running   0          108s   10.42.50.150   w02   <none>           <none>
```

nginx Pod 에 exec 하여 index.html 생성

```bash
$ k exec -ti nginx-pod1 -- bash
root@nginx-pod1:/# cd /usr/share/nginx/html
root@nginx-pod1:/usr/share/nginx/html# echo 'Hello World from GlusterFS!!!' > index.html
root@nginx-pod1:/usr/share/nginx/html# ls
index.html
```

노드에서 Pod ip 로 curl 해보기

```bash
$ curl 10.42.50.150
Hello World from GlusterFS!!!
```

gluster Pod 점검

```bash
### gluster Pod 하나를 골라 exec 하자
$ k exec -ti glusterfs-54q6c -- bash

# mount | grep heketi
/dev/mapper/ubuntu--vg-ubuntu--lv on /var/lib/heketi type ext4 (rw,relatime,stripe=256,data=ordered)
/dev/mapper/vg_199af666c36742f678351e23b54d5828-brick_e20537bae9ed4b22999a04dab94d0000 on /var/lib/heketi/mounts/vg_199af666c36742f678351e23b54d5828/brick_e20537bae9ed4b22999a04dab94d0000 type xfs (rw,noatime,nouuid,attr2,inode64,noquota)
/dev/mapper/vg_199af666c36742f678351e23b54d5828-brick_32ddddbabf65a093d0685fb423365619 on /var/lib/heketi/mounts/vg_199af666c36742f678351e23b54d5828/brick_32ddddbabf65a093d0685fb423365619 type xfs (rw,noatime,nouuid,attr2,inode64,noquota)

# cd /var/lib/heketi/mounts/vg_199af666c36742f678351e23b54d5828/brick_32ddddbabf65a093d0685fb423365619/brick

# ls
index.html

# cat index.html
Hello World from GlusterFS!!!
```