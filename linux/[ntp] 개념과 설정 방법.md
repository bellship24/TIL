# 210520_ntp

## 배경

NTP 란?

- Network Time Protocol.
- 안정적으로 정확한 시간 정보를 제공해주는 NTP 서버들의 pool 에 연결하는 서비스.

NTP 를 쓰는 이유

- linux 위에서 각종 서비스의 서버를 돌린다 할 때, 기본적으로 대부분 서버 간에 시간 동기화가 되어야 한다. 이를 위해 ntp 등의 시간 동기화 프로토콜을 활용한다.

Chrony 란?

- 기존 NTP 단점을 보완하여 빠르고 정확하게 시간 동기화를 할 수 있도록 개선한 프로토콜.

상황에 맞게 NTP 와 Chrony 사용할 것

- RHEL7 이전에는 NTP 를 기본 네트워크 시간 프로토콜로 사용했고 RHEL7 이후부터 Chrony 로 대체됐다. 물론 Ubuntu 에서도 사용 가능하다.
- 하지만, 시스템 환경이나 상황에 맞게 NTP 혹은 Chrony 를 사용해야 한다.
  - NTP 사용 예시: 영구적으로 유지되는 시스템 또는 브로드 캐스트, 멀티캐스트 IP 를 사용하거나 오토키 프로토콜로 패킷 인증을 수행해야 하는 시스템에 사용.
  - Chrony 사용 예시: 네트워크(모바일 및 가상 서버 등)에서 자주 중단되거나 간헐적으로 연결이 끊어지는 시스템에 적합.

local time server

- 외부 네트워크와 단절된 보안을 요구하는 네트워크 환경에서 돌아가는 내부 time server.

stratum 이란?

- 시간을 전송하는 장비들을 의미. Primary reference clock 장비에는 GPS, 세슘 원자 시계 등이 있음.

하드웨어 시간, 소프트웨어 시간

- hwclock 명령어는 하드웨어의 시간을 관리하고 date 명령어는 운영체제(소프트웨어)의 시간을 관리.
- 하드웨어 시간을 소프트웨어 시간에 설정할 수도 소프트웨어 시간을 하드웨어 시간에 설정할 수도 있다.
- 하드웨어 시간 확인: 메인보드에 있는 BIOS 의 시간으로 `hwclock` 혹은 `clock` 으로 확인 가능.
  - r: 현재시간 확인
  - w: 운영체제의 시간을 참조하여 하드웨어 시간을 설정
  - s: 하드웨어 시간을 참조하여 운영체제의 시간을 설정
- 소프트웨어 시간 확인: 커널에서 인식하고 있는 시간으로 `date` 로 확인 가능. 자세히 보려면 `timedatectl` 로 확인.

## NTP 설치

``` bash
$ sudo apt install ntp

### 버전 확인
$ dpkg -l | grep ntp
ii  ntp                                    1:4.2.8p10+dfsg-5ubuntu7.3                      amd64        Network Time Protocol daemon and utility programs
ii  sntp                                   1:4.2.8p10+dfsg-5ubuntu7.3                      amd64        Network Time Protocol - sntp client
```

## NTP 설정 활용

``` bash
$ sudo vi /etc/ntp.conf
### server 와 restrict 를 모두 주석 처리
server <server_ip or domain_name> iburst
restrict <allowed_ip_range> mask <subnet_mask_range> nomodify notrap
```

  - server
    - 등록한 NTP server 로부터 시간을 가져온다.
    - local time server : 127.127.1.0
    - 공용 NTP 서버 목록
      | 서버주소              | PING 응답여부 | 제공                  |
      |---------------------|------------|----------------------|
      | time.bora.net       | O          | LG유플러스             |
      | time.nuri.net       | O          | 아이네트호스팅           |
      | ntp.kornet.net      | X          | KT                   |
      | ntp2.kornet.net     | O          | KT                   |
      | time.google.com     | O          | 구글                  |
      | time.kriss.re.kr[1] | .          | 한국표준과학연구원(KRISS) |
      | time.nist.gov       | .          | NIST                 |
      | time.windows.com    | .          | 마이크로소프트           |
    - 공용 ntp 의 경우, 사내 서비스에서 연동하는 것을 지양한다. 되도록 사내 ntp 동기화를 사용하자.
    - `iburst` 옵션이 없으면 ntpd 데몬이 시작하는 시점에 시스템 시각이 약 10분 이상 틀어져 있을 경우에 동기화 하는데 너무 오랜 시간이 걸린다.
  - restrict
    - restrict 로 allow 할 client 를 설정
    - restrict 클라이언트의 엑세스를 제어하는 방법
      - ignore : 모든 패킷 거부
      - noquery ntpq : ntpq 쿼리 무시
      - nomodify : 상태 확인 무시
      - noserve : 시간 동기 요구 거부
      - notrust : 신뢰하지 않는 호스트 거부
      - notrap : 패킷 거부

## NTP 설정 적용 예제

``` bash
$ sudo vi /etc/ntp.conf
### server 와 restrict 를 모두 주석 처리
server time.bora.net iburst
server time.bora.net iburst
restrict 10.1.1.0 mask 255.255.255.0 nomodify notrap

### 서비스 재시작
$ sudo systemctl restart ntp
```

## ntp 동기화 확인

``` bash
$ ntpq -pn

remote           refid           st t when poll reach delay   offset  jitter
==============================================================================
+x.x.x.x   x.x.x.x     6 u   53   64  377  0.501    0.226   0.145
*x.x.x.x.   x.x.x.x     6 u    4   64  377  0.834    0.410   0.113
```

- 정확한 동기화를 위해 실제 동기화 되기까지 5분 이상 소요될 수 있다.
- 동기화가 되면 remote 필드의 ip 값 앞에 * 표시가 된다. + 는 secondary 이다.

# 서버간에 ntp 동기화 하는 방법

ntp 서버 노드에서 설정

`/etc/ntp.conf`

- 아래 설정 외에 `server`, `restrict` 를 모두 주석처리

``` text
restrict 10.231.130.0 mask 255.255.255.0 nomodify notrap
server 127.127.1.0 # local clock
```

- local clock 을 별도의 ntp 서버로 설정해서 가져와도 됨

ntp 클라이언트 노드에서 설정

`/etc/ntp.conf`

- 아래 설정 외에 `server`, `restrict` 를 모두 주석처리

``` text
server <ntp 서버 ip> iburst
```

ntp 재시작 및 동기화 확인(모든 노드)

``` bash
sudo systemctl restart ntpd
sudo systemctl status ntpd
ntpq -pn
```

## 기타

### timedatectl 로 timezone 변경

한국 타임존 확인

``` bash
$ timedatectl list-timezones | grep Seoul
Asia/Seoul
```

한국 타임존 설정

``` bash
$ sudo timedatectl set-timezone Asia/Seoul
```

- 변경된 설정은 재부팅 후에도 유지된다.

## 참고

- 센터 ITSM 에 NTP 신청 작성 시에 센터 NTP 사용 가이드 참고
- [블로그 - RHEL, CentOS 에서 ntp 서버 설정하기 (사설망 내부 서버들의 시간 동기화)](https://www.pigletstory.co.kr/620)