**목차**

- [1. VRRP](#1-vrrp)
  - [1.1. VRRP 란?](#11-vrrp-란)
  - [1.2. VRRP 대표 tool](#12-vrrp-대표-tool)
  - [1.3. VRRP 를 사용한 Route 의 이중화 구조](#13-vrrp-를-사용한-route-의-이중화-구조)
  - [1.4. VRRP 동작 방법](#14-vrrp-동작-방법)
  - [1.5. VRRP 동작 확인 방법](#15-vrrp-동작-확인-방법)
- [2. Keepalived](#2-keepalived)
  - [2.1. keepalived 란?](#21-keepalived-란)
  - [2.2. keepalived 구조](#22-keepalived-구조)
  - [2.3. HA 구성에서 Master 의 장애 검출 방법](#23-ha-구성에서-master-의-장애-검출-방법)
  - [2.4. keepalived 의 healthcheck 방법](#24-keepalived-의-healthcheck-방법)
  - [2.5. keepalived 설정 방법](#25-keepalived-설정-방법)
- [3. haproxy](#3-haproxy)
  - [3.1. haproxy 란?](#31-haproxy-란)
  - [3.2. haproxy 동작 방법](#32-haproxy-동작-방법)
  - [3.3. haproxy 이중화(HA) 구성](#33-haproxy-이중화ha-구성)
  - [3.4. haproxy 설정 방법](#34-haproxy-설정-방법)
  - [3.5. haproxy 설정 점검 방법](#35-haproxy-설정-점검-방법)
  - [3.6. haproxy stats 모니터링](#36-haproxy-stats-모니터링)
    - [3.6.1. haproxy stats page 란?](#361-haproxy-stats-page-란)
    - [3.6.2. haproxy stats 활성화 방법](#362-haproxy-stats-활성화-방법)
    - [3.6.3. 대시보드 접근](#363-대시보드-접근)
    - [3.6.4. Frontend Statistics](#364-frontend-statistics)
- [4. keepalived + haproxy 동작 원리](#4-keepalived--haproxy-동작-원리)
- [5. HA k8s 를 위한 keepalived + haproxy 설정 방법](#5-ha-k8s-를-위한-keepalived--haproxy-설정-방법)
  - [5.1. ha k8s 를 위한 keepalived 설정](#51-ha-k8s-를-위한-keepalived-설정)
  - [5.2. ha k8s 를 위한 haproxy 설정](#52-ha-k8s-를-위한-haproxy-설정)
- [6. 기타](#6-기타)

**참고**

- [keepalived 와 VRRP 개념](https://lascrea.tistory.com/211)
- [haproxy 개념](https://leffept.tistory.com/309)
- [haproxy logging 개념](https://www.haproxy.com/blog/introduction-to-haproxy-logging/)
- [haproxy stats 개념](https://www.haproxy.com/blog/exploring-the-haproxy-stats-page/)

# 1. VRRP

## 1.1. VRRP 란?

- VRRP, Virtual Router Redundancy Protocol
- Route 나 LoadBalancer 의 SPOF(Single Point of Failure) 장애를 극복하고 이중화(Redundancy) 혹은 HA 구성을 하기 위해서는 Route 나 LoadBalancer 를 이중화시킬 필요가 있다. 이렇게 Network 상에 존재하고 있는 Route 나 LoadBalancer 중에 어떤 인터페이스가 트래픽을 전달하는 책임을 가질지 결정하는 프로토콜이 **VRRP** 라고 한다.

## 1.2. VRRP 대표 tool

- VRRP를 사용해 Linux 에서 HA 를 쉽게 해주는 Tool 로는 대표적으로 **Keepalived** 가 있고 비슷한 메커니즘을 사용하는 Corosync 나 PaceMaker 등이 있다.

## 1.3. VRRP 를 사용한 Route 의 이중화 구조

![](/.uploads/2021-06-10-14-26-31.png)

- 위에는 VRRP 를 사용하는 이중화된 Route의 구성도이다.
- VRRP 는 가상의 Route 에 IP 를 할당하고 가상 Route 에 연결된 물리 Route 중 할당 우선순위가 높은 Route 가 Active Route 가 되어 통신에 사용된다. (개념은 Linux Bonding과 같은 개념이다) Stanby Route 는 Active Route 를 장애 발생시 Active 로 승격한다.

## 1.4. VRRP 동작 방법

![](/.uploads/2021-06-10-14-35-21.png)

- 위에는 VRRP 의 동작 방법이다.
- Master Route (Active Route) 에서 Slave Route (Stanby Route) 로 Advertisement (Health Check) 를 전달하여 자신이 정상 상태인 것을 알려준다. Slave Route 에는 Master DownTimer 라는 것을 설정하는데 이 지정된 시간동안 Master 로부터 Advertisement 가 오지 않으면 Master 가 죽은것으로 판단하고 자신이 Master 가 되었다는 Advertisement 를 Master 에 전달한다.
- 상세한 동작 원리는 [여기](https://www.netmanias.com/ko/post/techdocs/5049/data-center-network-protocol/vrrp-virtual-router-redundancy-protocol-detailed-principles-of-operation)를 참고하자.

## 1.5. VRRP 동작 확인 방법

``` bash
$ sudo tcpdump -v vrrp
```

# 2. Keepalived

## 2.1. keepalived 란?

- keepalived 는 디바이스간에 데이터 링크가 잘 동작하고 있는지 확인하거나 데이터 링크가 끊어지는 것을 방지하기 위해서 디바이스 간에 서로 주고받는 메시지를 말한다.
- 공식 홈페이지에 따르면 C 로 작성된 LoadBalancing 및 HA 를 제공하는 프레임워크이다. 간단하게 설명하면 VIP(가상 IP) 를 기반으로 작동하며 Master 노드를 모니터링하다가 해당 노드에 장애가 발생했을시 Stanby 서버로 Failover(장애 극복 기능) 되도록 지원한다. 즉, Heartbeat 체크를 하다가 Master 에 장애발생시 Slave 로 FailOver 하는 역할을 하는 것이다.

## 2.2. keepalived 구조

![](/.uploads/2021-06-10-15-18-04.png)

## 2.3. HA 구성에서 Master 의 장애 검출 방법

- HA 구성에서 Master 의 장애인지 검출할 수 있는 방법은 아래와 같다.
- ICMP (L3) : ping 을 사용하는 것이다. 네트워크 구간이 정상이고 서버가 살아있다면 ICMP echo 요청에 대한 응답이 돌아올 것이지만 ICMP 패킷이 바이러스 침투나 공격으로 사용되는 사례가 많기 때문에 ICMP 를 차단하는 경우가 많아 잘 사용하지 않는다.
- TCP 요청 (L4) : 서비스를 올린 후 방화벽 허용 확인을 위해 telnet 명령을 실행해 정상 체크를 하기도 한다. 이는 TCP 요청이 정상적으로 응답하는지 확인하는 방법이다. 서비스가 살아 있어 Port Listen 은 하고 있지만 서버나 프로그램의 오류가 있어 HTTP 500 Code 를 반환하는 경우 정확한 확인이 불가능한 단점이 있다.
- HTTP 요청 (L7) : 실제 실행중인 서비스에 Heathcheck 를 위한 Endpoint 를 두고 서비스에 요청을 날려 200 OK 가 반환되는지 확인하는 것이다. HTTP 요청 자체가 ICMP 나 TCP 요청에 비해 무겁기 때문에 고려가 필요하다.

## 2.4. keepalived 의 healthcheck 방법

- Keepalived 는 Heathcheck 를 위해 TCP, HTTP, SSL, MISC 와 같은 방법을 사용한다.
- TCP_CHECK : 비동기방식으로 Time-Out TCP 요청을 통해 장애를 검출하는 방식이다. 응답하지 않는 서버는 Server Pool 에서 제외한다.
- HTTP_GET : HTTP GET 요청을 날려 서비스의 정상 동작을 확인한다.
- SSL_GET : HTTP GET 과 같은 방식이지만 HTTPS 기반이다.
- MISC_CHECK : 시스템상에서 특정 기능을 확인하는 Script 를 돌려 그 결과가 0인지 1인지를 가지고 장애를 검출하는 방법이다. 네트워크가 아니라 시스템상에서 돌고 있는 서비스의 정상 동작을 확인하는데 유용하다.

## 2.5. keepalived 설정 방법

- keepalived 설정은 파일 2개로 이루어져 있다. **serivce configuration 파일**과 **health check 스크립트** 이다.
- 헬스체크 스트립트는 주기적으로 virual IP 를 갖고 있는 노드가 살아있는 지 확인한다.
- keepalived 버전 확인 방법
  
  ``` bash
  $ keepalived -version
  Keepalived v1.3.9 (10/21,2017)

  Copyright(C) 2001-2017 Alexandre Cassen, <acassen@gmail.com>

  Build options:  PIPE2 IPV4_DEVCONF LIBNL3 RTA_ENCAP RTA_EXPIRES RTA_NEWDST RTA_PREF RTA_VIA FRA_OIFNAME FRA_SUPPRESS_PREFIXLEN FRA_SUPPRESS_IFGROUP FRA_TUN_ID RTAX_CC_ALGO RTAX_QUICKACK FRA_UID_RANGE LWTUNNEL_ENCAP_MPLS LWTUNNEL_ENCAP_ILA LIBIPTC LIBIPSET_DYNAMIC LVS LIBIPVS_NETLINK IPVS_DEST_ATTR_ADDR_FAMILY IPVS_SYNCD_ATTRIBUTES IPVS_64BIT_STATS VRRP VRRP_AUTH VRRP_VMAC SOCK_NONBLOCK SOCK_CLOEXEC GLOB_BRACE OLD_CHKSUM_COMPAT FIB_ROUTING INET6_ADDR_GEN_MODE SNMP_V3_FOR_V2 SNMP SNMP_KEEPALIVED SNMP_CHECKER SNMP_RFC SNMP_RFCV2 SNMP_RFCV3 DBUS SO_MARK
  ```

- **service configuration 파일**들은 `/etc/keepalived/` 밑에 있어야 한다.
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

  - keepalived+haproxy 를 k8s 에서 static pod 로 띄울 때는 bash 환경 변수로 아래에 placeholder 들을 설정해주면 된다.
  - `${STATE}` : 값을 MASTER, BACKUP 으로 넣으면 되고 서버 하나에 MASTER 를 넣고 나머지는 BACKUP 을 넣어 가상 IP가 처음에 MASTER에 할당되게 한다.
  - `${INTERFACE}` : 가상 IP negotiation 에 참여하는 네트워크 인터페이스이다. (e.g. `eth0`)
  - `${ROUTER_ID}` : 숫자를 값으로 갖으며 이 값은 모든 keepalived 클러스터 호스트들에서 동일해야한다. 또한 해당 서브넷의 모든 클러스터에서 고유해야한다. 대부분의 배포판에는 해당 값을 51로 미리 구성해 놨다.
  - `${PRIORITY}` : BACKUP 노드 보다 MASTER 노드에서 더 높아야한다. 예를 들어, 101, 100 순으로 넣으면 된다.
  - `${AUTH_PASS}` : 비밀번호이다. 모든 keepalived 클러스터 호스트들에서 동일해야한다. e.g. 42
  - `${APISERVER_VIP}` : keepalived 클러스터 호스트들 간에 negotiation 된 가상 IP 주소이다. k8s 의 apiserver 를 로드밸런싱 할 것이기 때문에 apiserver 의 vip 가 된다.

- health check 스크립트도 `/etc/keepalived/` 밑에 있어야 한다.

- 위에 keepalived service configuration 파일은 가상 IP를 보유하고 있는 노드에서 API 서버 를 사용할 수 있는지 확인하는 health check 스크립트 `/etc/keepalived/check_apiserver.sh` 를 사용한다. 이 스크립트의 내용은 아래와 같다.

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

  - 여기서도 keepalived+haproxy 를 k8s 에서 static pod 로 띄울 때는 bash 변수로 아래에 placeholder 들을 설정해주면 된다.
  - `${APISERVER_VIP}` : 연결 유지 클러스터 호스트간에 negotiation 된 가상 IP 주소(VIP)이다.
  - `${APISERVER_DEST_PORT}` : Kubernetes가 VIP 를 통해 apiserver 와 통신하는 데 사용하는 포트이다.

# 3. haproxy

## 3.1. haproxy 란?

- 먼저 Load Balancing 이란 부하 분산을 위해서 가상 IP 를 통해 여러 대의 서버에 접속을 분해하는 기능(scale out)을 말한다.
- haproxy 는 여러 서버에 걸쳐 요청을 분산시키는 TCP 및 HTTP 기반 응용 프로그램을 위한 고가용성 로드 밸런서 및 프록시 서버를 제공하는 무료 오픈 소스 소프트웨어이다. C로 작성되었으며 빠르고 효율적으로 인기 있다.

## 3.2. haproxy 동작 방법

- haproxy 는 기본적으로 reverse proxy 형태로 동작한다. reverse proxy 는 서버로 들어오는 요청을 대신 받아서 서버에 전달하고 요청한 곳에 그 결과를 다시 전달해주는 역할을 한다.

![](/.uploads/2021-06-11-10-39-41.png)

1. 최초 접근 시 서버에 요청 전달
2. 응답 시 쿠키에 서버 정보 추가 후 반환
3. 재요청 시 proxy 에서 쿠키 정보 확인 -> 최초 요청 서버로 전달
4. 다시 접근 시 쿠키 없이 추가 전달 -> 클라이언트에 쿠키 정보가 계속 존재 (쿠키 재사용)

## 3.3. haproxy 이중화(HA) 구성

![](/.uploads/2021-06-11-10-58-22.png)

- haproxy 의 이중화 구성을 통하여 master haproxy 에 문제가 생기는 경우 slave haproxy 에서 서비스가 원할하게 제공될 수 있도록 할 수 있다. 위의 그림에서 가상 IP 주소를 공유하는 Active haproxy 서버와 Standby haproxy 서버가 hearbeat 을 주고 받으면서 서로 정상적으로 작동하는지 확인한다. 이 때 Active 상태의 서버에 문제가 발생하면 Standby 서버가 Active 상태로 변경되면서 기존 Acitve haproxy 의 가상 IP를 가져와 서비스를 정지 없이 유지할 수 있다.

## 3.4. haproxy 설정 방법

- haproxy 설정은 서비스 구성 파일 하나로 구성한다. 이 파일은 `/etc/haproxy/haproxy.cfg` 이다. 그러나 일부 Linux 배포판은 다른 곳에 보관할 수 있다. 다음 구성은 haproxy 버전 2.1.4에서 검증됐다.

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

- 여기도 앞서 본 keepalived 와 마찬가지로 k8s 의 static pod 로 띄우고 싶다면 파드의 bash 변수로 아래에 placeholder 들을 설정해주면 됩니다.
- `${APISERVER_DEST_PORT}` : Kubernetes가 API 서버와 통신하는 데 사용하는 포트입니다. (lb 의 port)
- `${APISERVER_SRC_PORT}` : API 서버 인스턴스에서 사용하는 포트입니다. (apiserver 의 port)
- `${HOST1_ID}` : 첫 번째로 로드밸런싱 된 API 서버 호스트의 symbolic name 입니다.
- `${HOST1_ADDRESS}` : 첫 번째로 로드밸런싱 된 API 서버 호스트의 resolvable address (DNS 이름, IP 주소) 입니다.
- 추가적인 `server` 라인들 : 로드밸런싱 된 API 서버 호스트 당 라인 추가

## 3.5. haproxy 설정 점검 방법

``` bash
$ /usr/sbin/haproxy -f /etc/haproxy/haproxy.cfg -c
[WARNING] 157/095622 (29318) : config : log format ignored for frontend 'apiserver' since it has no log address.
Configuration file is valid
```

## 3.6. haproxy stats 모니터링

### 3.6.1. haproxy stats page 란?

- HAProxy stats page 는 프록시된 서비스의 상태 데이터에 대해 거의 실시간 피드를 제공한다.
- 여기서는 서버 상태, 현재 요청 비율, 응답 시간 등 여러 메트릭들을 대시보드 형태로 보여준다. 이런 메트릭들은 프론트엔드, 백엔드, 서버 측면에서 다양하게 제공된다.

### 3.6.2. haproxy stats 활성화 방법
  
``` text
frontend stats
    bind *:8404
    stats enable
    stats uri /stats
    stats refresh 10s
    stats admin if LOCALHOST
```

- 기본적으로 아래와 같이 특정한 `frontend` 섹션에서 `stats enable` 지시어만 넣어주면 활성화 된다.
- `bind` : 접근할 대시보드의 주소와 포트를 지정한다.
- `stats uri` : URL 경로를 변경할 수 있다.
- `stats refresh` : 대시보드 최신화 주기이다.
- `stats admin` : 대시보드에서 서버를 maintenance 모드로 전환하거나 트래픽을 drain 할 수도 있다. 이를 위해, `stats admin` 줄을 추가하고 ACL 문을 통해 이러한 기능에 액세스 할 수 있는 사용자를 제한한다. TRUE 인 경우 제한을 우회하기 위해 `stats admin` 으로 설정할 수도 있다. 선택적으로 `stats auth <username : password>` 줄을 추가하여 기본 인증을 적용할 수 있다.

### 3.6.3. 대시보드 접근

![](/.uploads/2021-06-11-15-07-03.png)

- 앞서 설정을 하고 haproxy 를 재시작하면 /stats URL 에 8404 포트로 접근하면 위와 같이 대시보드를 볼 수 있다.
- HAProxy stats page 는 프록시 서비스 문제를 해결하고 트래픽에 대한 통찰력을 얻고 서버에 가해지는 부하를 관찰하는 데 사용할 수 있는 정보의 거의 실시간 피드를 제공한다.
- 그러나 메트릭은 축약형 이름과 접두사를 사용하여 레이블이 지정된다. 이것들을 완전히 사용하기 전에 그 의미를 잘 이해해야한다.

### 3.6.4. Frontend Statistics

- frontend 는 클라이언트가 접근하는 곳이다.
- request(요청) 는 로드밸런서에 들어가고 클라이언트에게 response(응답) 로 반환되는데 이 과정들은 프런트엔드를 통과한다. 따라서 전체 요청/응답 라이프사이클을 포괄하는 end-to-end timing, message size 및 health indicator 에 액세스 할 수 있다.

Session rate

![](/.uploads/2021-06-11-15-16-41.png)

- 클라이언트가 HAProxy 에 연결하는 속도를 나타낸다.
- `Cur` 칼럼은 클라이언트 세션 또는 클라이언트와 서버간에 생성되는 fully established connection(완전히 설정된 연결) 의 현재 속도를 보여준다. 이 필드 위로 마우스를 가져가면 아래와 같이 상세 메트릭이 표시된다.
![](/.uploads/2021-06-11-15-23-16.png)
- `Max` 칼럼은 한번에 가장 많이 사용된 세션 개수가 표시된다. 이 필드 위로 마우스를 가져가면 아래와 같이 상세 메트릭이 표시된다.
![](/.uploads/2021-06-11-15-22-43.png)

Sessions

![](/.uploads/2021-06-11-15-25-11.png)

- 로드밸런서에서 사용중인 세션 수 또는 전체 클라이언트-서버 연결을 계산한다.

Bytes

Denied

Errors

Server

# 4. keepalived + haproxy 동작 원리

![](/.uploads/2021-06-10-16-41-13.png)
![](/.uploads/2021-06-10-16-41-20.png)

- 두 노드 모두 keepalived 를 통해 vip 를 갖게한다.
- 평상시에는 첫 번째 그림과 같이 stg1 에 VIP 12.34.56.129 이 붙어 있고 리퀘스트가 들어오면 stg1 의 haproxy 를 통해 stg1 과 stg2 로 로드밸런싱 된다.
- 그러다가 stg1 이 다운되면 두 번째 그림과 같이 VIP 가 stg2 로 옮겨 가게 되고 stg2 -> stg2 로만 리퀘스트를 보내다가 stg1이 다시 살아 나면 stg1 로도 리퀘스트를 나눠 준다.

# 5. HA k8s 를 위한 keepalived + haproxy 설정 방법

## 5.1. ha k8s 를 위한 keepalived 설정

`/etc/keepalived/keepalived.conf` 예시

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
    state MASTER            ### SLAVE
    interface ens3
    virtual_router_id 151
    priority 255            ### 254 253 ...
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

## 5.2. ha k8s 를 위한 haproxy 설정

`/etc/haproxy/haproxy.cfg` 예시

``` text
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
        server dtlab-dev-k8s-pjb-1 10.231.238.163:6443 check
        server dtlab-dev-k8s-pjb-2 10.231.238.164:6443 check
        server dtlab-dev-k8s-pjb-3 10.231.238.165:6443 check
```

# 6. 기타

- keepalived 를 통해 VIP 를 사용할 때 IP Spoofing 으로 인식하여 통신이 끊길 수 있으므로 백본 환경에 맞춰 추가적인 인터페이스와 VIP 를 할당해 사용해야 할 수 있다. 이에 대해 [여기서](https://navercloudplatform.medium.com/keepalived를-활용하여-간단하게-ha-구성해보기-c840b90149a5) 예시를 검토할 수 있다.
