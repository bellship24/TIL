# 210513_RancherD_검토&설치

- 참고 URL
    - [Introducing RancherD: A Simpler Tool for Deploying Rancher](https://rancher.com/blog/2020/rancherd-simplifies-rancher-deployment)
    - [[Rancher Docs] Install/Upgrade Rancher on a Linux OS](https://rancher.com/docs/rancher/v2.x/en/installation/install-rancher-on-linux/#2-launch-the-first-server-node)
    - [[Rancher] Rancherd 쿠버네티스 HA 클러스터 구성 및 설치, 폐쇄망 대처 방법](https://myjamong.tistory.com/253)

## RancherD 란?

rancher v2.5.4 부터 새롭게 도입된 rancher 설치 방법.

rancherD 는 호스트에서 실행할 수 있는 single binary.

rancherD 를 통해 명령어 한 번에 rancher 의 upstream 용 RKE2 k8s 클러스터를 배포하고 그 위에서 helm 을 사용해 rancher 를 배포할 수 있다. 즉, 이전에는 RKE 로 rancher 의 upstream k8s 를 직접 구축하고 설정이나 업그레이드를 관리해야하는 어려움이 있었으나 rancherD 로 이부분을 자동화할 수 있게 됐다.

하지만, 아직 이 기능은 preview 단계이므로 에러들이 있을 수 있고 production 으로 사용하기 부적합할 수 있다.

## RancherD 아키텍처

rancherD 를 통해 rancher 구축 시에 선택할 수 있는 초기 아키텍처 두 가지.

1. single node 구성 (on RKE)
2. HA 구성 (on RKE)

## RancherD 장점

앞서 언급한 설치의 단순화 뿐만 아니라, rancherD 의 또다른 장점은 single node 구성을 하여도 HA 구성으로 쉽게 업그레이드가 가능하다는 점이다. 큰 장점이 될 수 있는 기능으로써 기존에 수동 설치를 해봤다면 알 수 있다. 수동으로 single node 구성을 한다면, 단순히 docker container 위에 container in container 구축으로 HA 구성으로 전환이 불가능하다. HA 구성은 RKE 클러스터를 Upstream k8s 로 기반하기 때문이다.

즉, rancherd 의 장점을 요약하면 아래와 같이 두 가지가 있다.

1. rancher 구축, 업그레이드 및 설정 단순화
2. 싱글 노드 → HA 로 아키텍처 전환 가능 및 단순화

## RancherD 전제 사항

- Linux OS 만 가능
- systemd 를 쓸 수 있는 OS 만 가능
- os 는 64-bit x86 지원
- RancherD 설치에 Docker 가 필요하지는 않음
- rancher v2.5.4 부터 적용 가능
- ntp 설치되어 있어야 함
    - client 와 server 간에 시간 불일치로 인한 certification validation error 예방
- 하드웨어, 네트워크 전제: [https://rancher.com/docs/rancher/v2.x/en/installation/requirements/](https://rancher.com/docs/rancher/v2.x/en/installation/requirements/)
    - 아래 내용은 rancher v2.4.0 이상부터 적용되며 그 이하 버전에서는 cpu 코어가 더 필요함.
    - rancherD 가 설치된 각 인스턴스의 하드웨어 스팩 요구사항

        ![](/.uploads/2021-05-13-23-08-02.png)

    - Upstream RKE 혹은 Hosted k8s(AKS, EKS 등) 의 각 노드 당 하드웨어 스팩 요구사항

        ![](/.uploads/2021-05-13-23-08-16.png)

    - 랜처 1 노드에 설치할 때 docker 에 필요한 하드웨어 스팩 요구사항

        ![](/.uploads/2021-05-13-23-08-28.png)

    - helm 통신을 위해 firewall 해제.
    - rancher 의 disk 성능은 etcd 에 대한 성능이므로 SSD 를 쓰자.
- RancherD 설치를 위해 root 권한 필요.
- CentOS8 SElinux 환경이라면 추가작업 필요: [https://rancher.com/docs/rancher/v2.x/en/installation/requirements/#rancherd-on-selinux-enforcing-centos-8-or-rhel-8-nodes](https://rancher.com/docs/rancher/v2.x/en/installation/requirements/#rancherd-on-selinux-enforcing-centos-8-or-rhel-8-nodes)

## Fixed Registration Address

- 이 주소는 아래와 같은 두 가지 목적을 위한 endpoint 이다.
    1. kubernetes API 접근
    2. 새로운 노드 추가
- 이 주소는 RancherD 로 rancher 서버를 설치할 때, HA 구성 시에 필수이며 single-node 구성에서도 권고되는 사항이다. 후자의 경우, 추후에 downtime 없이 HA 구성을 하기 위함이다. 즉, 이 주소 없이 single-node 를 구성했다면, 추후에 노드를 증설할 때, 이 주소를 설정에 추가하고 설치 스크립트를 다시 돌려 클러스터를 재시작해야 하므로 downtime 이 발생한다.
- 이 주소는 어떤 서버 노드 간에 IP 나 hostname 이 될 수 있다. 하지만, 이는 빈번히 변경될 수 있으므로 서버 노드 앞단에 stable 한 endpoint 를 구성해 사용하는게 좋다.
    - 이러한 stable endpoint 를 구성하는 방법은 대략 아래와 같다.
        1. 4 layer (TCP) 로드밸런서
        2. round-robin DNS
        3. virtual or elastic IP 주소
    - 만약, 이러한 stable endpoint 를 구성한다면, 아래에 요소들을 고려하자.
        - RancherD 서버 프로세스는 새로운 노드가 등록되기 위해 9345 포트를 listen 한다.
        - 통상적으로 kubernetes API 는 6443 포트로 서빙된다.
        - RancherD 를 통한 설치에서 Rancher UI 는 8443 포트로 서빙된다. (반면에, Helm chart 를 통한 Rancher 설치 시에, Rancher UI 는 default 로 443 포트를 사용한다.)

## 설치 내용

1. RancherD 를 구동하면 제일 먼저 RKE2 K8s 클러스터를 설치
2. 이 클러스터 위에서 helm chart 를 사용하여 Rancher 를 daemonset 으로 배포

업그레이드의 경우, RancherD 바이너리를 업그레이드 하면 Upstream K8s 클러스터와 Rancher helm chart 도 업데이트 된다.

## RancherD 로 Rancher 구축

계정 전환

- rancherd 의 제어는 기본적으로 root 계정으로 진행한다. 단 환경변수는 기존 계정으로 가져간다.

    ```bash
    $ sudo -s
    ```

tls-san 설정

- `fixed registration address` 에 대해 인증서 에러를 피하기 위해 rancherd 구동 전에 `tls-san` 파라미터 설정 작업을 수행해야 한다. 이 파라미터에 `fixed registration address` 값을 넣으면 된다. IP 나 Hostname 을 넣으면 된다.
- `/etc/rancher/rke2/config.yaml`

    ```bash
    token: my-shared-secret
    tls-san:
      - https://my-fixed-registration-address.com
      - another-kubernetes-domain.com
    ```

    ```bash
    ### e.g.
    # mkdir -p /etc/rancher/rke2
    # cat <<EOF> /etc/rancher/rke2/config.yaml
    tls-san:
      - hci.dev-rancher.com
    EOF
    ```

    - token : 다른 노드에서 통신하기 위해 사용되는 token 이름
    - tls-san : 서버 tls cert 에 추가된다.

    `token` 에는 pre-shared secret 을 지정할 수 있으나, 지정하지 않으면 RancherD 가 생성하는 rke 첫 번째 노드가 자동으로 생성하여 `/var/lib/rancher/rke2/server/node-token`  에 저장하고 다른 노드들에 전달하는 식이다.

RancherD 설치

```bash
### 최신 버전 rancherd 설치 및 systemd 설정
sudo curl -sfL https://get.rancher.io | sudo sh -

### 특정 버전 rancherd 설치
sudo curl -sfL https://get.rancher.io | sudo INSTALL_RANCHERD_VERSION=v2.5.4-rc6 sh -

### 설치 확인
# rancherd -v
rancherd version v2.5.8 (HEAD)
go version go1.14.15

# rancherd --help
NAME:
   rancherd - Rancher Kubernetes Engine 2

USAGE:
   rancherd [global options] command [command options] [arguments...]

VERSION:
   v2.5.8 (HEAD)

COMMANDS:
   server       Run management server
   agent        Run node agent
   reset-admin  Bootstrap and reset admin password
   help, h      Shows a list of commands or help for one command

GLOBAL OPTIONS:
   --debug        Turn on debug logs [$RKE2_DEBUG]
   --help, -h     show help
   --version, -v  print the version
```

rancherd-server 실행하여 rancher 서버 설치

```bash
# enable , start(rancher 구축 시작)
systemctl enable rancherd-server.service
systemctl start rancherd-server.service

# 클러스터 상태 확인 (로그가 많이 발생함)
journalctl -eu rancherd-server -f
```

- 설치가 완료되려면 꽤 시간이 걸린다. 적어도 30분은 걸리는 것 같다.
- 완료되면 마지막 로그는 아래와 같다.

    ```bash
    ...
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.069651    4497 node_controller.go:115] Sending events to api server.
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.069788    4497 controllermanager.go:238] Started "cloud-node"
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.070420    4497 node_controller.go:154] Waiting for informer caches to sync
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.071412    4497 node_lifecycle_controller.go:77] Sending events to api server
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.071462    4497 controllermanager.go:238] Started "cloud-node-lifecycle"
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.178422    4497 node_controller.go:390] Initializing node hci.dev-rancher.com with cloud provider
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.187524    4497 node_controller.go:454] Successfully initialized node hci.dev-rancher.com with cloud provider
    May 10 08:43:36 hci.dev-rancher.com rancherd[4497]: I0510 08:43:36.187837    4497 event.go:291] "Event occurred" object="hci.dev-rancher.com" kind="Node" apiVersion="v1" type="Normal" reason="Synced" message="Node synced successfully"
    ```

Rancher 초기 설정

```bash
### bash-completion 설치
sudo apt-get install bash-completion
source /usr/share/bash-completion/bash_completion
### 쉘을 다시 로드하자

echo '' >> ~/.bashrc
echo '### kubernetes setting' >> ~/.bashrc
echo 'alias k=kubectl' >> ~/.bashrc
echo 'complete -F __start_kubectl k' >>~/.bashrc
echo 'export KUBECONFIG=/etc/rancher/rke2/rke2.yaml PATH=$PATH:/var/lib/rancher/rke2/bin' >>~/.bashrc
echo "export sys='-n=kube-system'" >>~/.bashrc
echo 'source <(kubectl completion bash)' >>~/.bashrc
echo '########################' >> ~/.bashrc
source ~/.bashrc

### Rancher 배포 상태 확인
kubectl get no
kubectl get daemonset rancher -n cattle-system
kubectl get pod -n cattle-system

### rancher 패스워드 초기화
# rancherd reset-admin
INFO[0000] Server URL: https://hci.dev-rancher.com:8443
INFO[0000] Default admin and password created. Username: admin, Password: j98ztrmhm5g5gmlrkhptc8wvghn9x2fq7nks6wmvzm8wvmzc8zrr5z
```

웹UI 접근 및 패스워드 변경

![](/.uploads/2021-05-13-23-08-51.png)

- <지정한 도메인>:8443

사용할 패스워드 설정

![](/.uploads/2021-05-13-23-09-02.png)

사용할 URL 설정

![](/.uploads/2021-05-13-23-09-13.png)

접근 확인

![](/.uploads/2021-05-13-23-09-22.png)

## Rancher HA 구성

랜처를 통해 downstream 클러스터를 관리한다면, 랜처를 HA 구성할 필요가 있다. 랜처 HA 를 구성 하는 방법은 upstream 클러스터에 노드를 추가하기만 하면 된다. 랜처는 데몬셋으로 구동되기 때문에 노드를 추가하면 자동으로 인스턴스가 배포되기 때문이다.

클러스터의 메타데이터가 저장되어 있는 etcd 클러스터 때문에 ha 구성 시에 upstream 클러스터의 노드는 홀수 개여야 한다. split-brain 을 방지하고 quorum 구성을 하기 위함이다. 그래서 보통 3 노드 구성을 추천한다.

### 마스터 노드 추가

아래에는 신규 노드 위에서 작업한다.

rancher config 설정

- 새로 추가되는 노드에도 기존 노드와 비슷하게 작업한다. 단, 새로운 노드가 기존 초기 노드와 커넥션을 하기 위해서 `server` 와 `token` 파라미터를 명시해야 한다. 예를 들면, 아래와 같다.
- `/etc/rancher/rke2/config.yaml`

    ```bash
    server: https://my-fixed-registration-address.com:9345
    token: my-shared-secret
    tls-san:
      - my-fixed-registration-address.com
      - another-kubernetes-domain.com
    ```

    ```bash
    # e.g.
    server: https://hci.rancher.com:9345 ## 기존 노드 정보
    token: my-shared-secret #???
    tls-san:
      - hci.rancher2.com #??? ## 신규 노드 정보
    ```

rancherd 설치

- 앞선 설정을 갖고 이제 새로운 노드에서 rancherd 를 다운받아 시작하기만 하면 된다. 이 또한 초기 노드 시작과 같다.

```bash
# 최신 버전 rancherd 설치 및 systemd 설정
sudo curl -sfL https://get.rancher.io | sudo sh -

# 특정 버전 rancherd 설치
sudo curl -sfL https://get.rancher.io | sudo INSTALL_RANCHERD_VERSION=v2.5.4-rc6 sh -

# 설치 확인
$ rancherd -v
rancherd version v2.5.4-rc6 (HEAD)
go version go1.14.12

$ rancherd --help
```

- 위에 명령어로 인해 rancherD 를 다운받고 systemd 등록을 하게 된다.

rancherd-server 실행하여 rancher 서버(마스터) 증설

- 이제 다운받은 바이너리를 실행하자.

```bash
systemctl enable rancherd-server.service
systemctl start rancherd-server.service

# 클러스터 상태 확인
journalctl -eu rancherd-server -f
```

rancher 웹 UI 에서 증설 확인

반복

- 이렇게 새로운 노드를 추가하는 방법을 새로운 노드에 똑같이 반복하면 된다.

### 워커 노드 추가

아래에는 신규 노드 위에서 작업한다.

워커 노드는 rancherd-server 가 아닌 rancherd-agent 서비스를 실행한다. 마찬가지로 rancher config 에 대한 부분과 agent 로 실행하는 것 외에는 증설 내용이 동일하다.

rancher config 설정

- `/etc/rancher/rke2/config.yaml`

    ```bash
    server: https://my-fixed-registration-address.com:9345
    token: my-shared-secret
    ```

    ```bash
    # e.g.
    server: https://hci.rancher.com:9345 ## 기존 노드 정보
    token: my-shared-secret #???
    ```

    - 워커의 경우, `tls-san` 에 대한 정보를 기입하지 않고 `server` 와 `token` 만 지정한다.

rancherd 설치

```bash
# 최신 버전 rancherd 설치 및 systemd 설정
sudo curl -sfL https://get.rancher.io | sudo sh -

# 특정 버전 rancherd 설치
sudo curl -sfL https://get.rancher.io | sudo INSTALL_RANCHERD_VERSION=v2.5.4-rc6 sh -

# 설치 확인
$ rancherd -v
rancherd version v2.5.4-rc6 (HEAD)
go version go1.14.12

$ rancherd --help
```

rancherd-agent 실행하여 rancher 서버 (워커) 증설

- 이제 다운받은 바이너리를 실행하자.

```bash
systemctl enable rancherd-agent.service
systemctl start rancherd-agent.service

# 클러스터 상태 확인
journalctl -eu rancherd-agent -f
```

rancher 웹 UI 에서 증설 확인

반복

- 이렇게 새로운 노드를 추가하는 방법을 새로운 노드에 똑같이 반복하면 된다.

## 기타

### RancherD 삭제

```bash
rancherd-uninstall.sh
```