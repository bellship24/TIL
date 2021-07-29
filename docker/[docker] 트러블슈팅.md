**목차**

- [요약](#요약)
- [1. docker run 할 때, port-forwarding 안되는 이슈](#1-docker-run-할-때-port-forwarding-안되는-이슈)
  - [1.1. 현상](#11-현상)
  - [1.2. 분석](#12-분석)
  - [1.3. 해결](#13-해결)
  - [1.4. 참고](#14-참고)

---

# 요약

> Docker 를 사용하며 생긴 이슈들을 트러블슈팅 하고 기록한다.

# 1. docker run 할 때, port-forwarding 안되는 이슈

## 1.1. 현상

``` bash
$ docker run -ti  -p 8871:8888 --name tensorrt2 -v /home/ubuntu/workspace/by/:/workspace/ tensorrt bash
docker: Error response from daemon: driver failed programming external connectivity on endpoint tensorrt2 (c48b96b1eb4a28b0e51422e2519fa67982d5a6c718f3d69fe4abf2f835afc3f1):  (iptables failed: iptables --wait -t nat -A DOCKER -p tcp -d 0/0 --dport 8871 -j DNAT --to-destination 172.17.0.2:8888 ! -i docker0: iptables: No chain/target/match by that name.
 (exit status 1)).
ERRO[0000] error waiting for container: context canceled
```

- 포트 포워딩 부분에서 위와 같은 에러 발생

## 1.2. 분석

- 이 에러가 발생하기 전에 k8s 재설치 관련 작업을 했는데 이 때, iptable 을 좀 건드렸다. 이게 문제 됐던 것으로 보인다.

## 1.3. 해결

- 참고 링크에 따라 아래와 같이 iptable 작업으로 해결했다.

``` bash
sudo iptables -t filter -F  
sudo iptables -t filter -X 
systemctl restart docker

docker run -it --gpus all -p 8873:8888 --name tensorrt2 -v /home/ubuntu/workspace/by/:/workspace/ tensorrt bashroot@f754290c883c:/# exit
```

## 1.4. 참고

- [[blog] [Docker] iptables failed - No chain/target/match by that name 문제 해결하기](https://data-newbie.tistory.com/479)