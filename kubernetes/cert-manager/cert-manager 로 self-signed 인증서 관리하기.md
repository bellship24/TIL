**목차**

- [1. SelfSigned](#1-selfsigned)
- [2. 루트 인증서란?](#2-루트-인증서란)

**참고**

- [[cert-manager docs] SelfSigned](https://cert-manager.io/docs/configuration/selfsigned/)
- [[Blog] Root CA 인증서는 무엇인가?](https://brunch.co.kr/@sangjinkang/47)

---

# 1. SelfSigned

`SelfSigned` issuer 는 CA(Certificate Authority, 인증 기관) 자체를 나타내지는 않지만 대신 주어진 개인키를 사용하여 인증서를 "자체 서명" 할 것임을 나타낸다. 즉, 인증서의 개인키는 인증서 자체에 서명하는 데 사용된다.
이 `Issuer` 타입은 커스텀 PKI(Public Key Infrastructure) 에 대한 루트 인증서를 부트스트랩하거나 간단한 임시 인증서를 만드는 데 유용하다.


- commonName: CA 이름
- secretName: 인증서가 저장될 Secret 이름

# 2. 루트 인증서란?

루트 인증서는 루트 인증 기관(CA)에서 관리하는 공개키 인증서나 자체 서명 인증서이다. 일반적으로 인증서는 ROOT, Intermediate, Leaf 3단계로 이루어져 있고 이를 인증서 체인(certificate chain) 이라고 한다. 사용자가 구입한 SSL 인증서는 Leaf 인증서를 의미하며 이는 인증서 체인의 일부이지 전체가 아니다. 이 3 개의 인증서 체인은 하위 구조의 인증서를 서명하고 상위 구조의 인증서를 참고하는 방식으로 만들어진다.

서버의 인증서를 신뢰하려면 루트 CA 로 추적할 수 있어야 한다. 브라우저가 웹 사이트에 접속하면 웹 사이트의 인증서를 다운로드하고 해당 인증서를 루트에 다시 연결하기 시작한다. 체인을 따라가며 신뢰할 수 있는 루트 인증서에 도달할 때까지 계속해서 역추적한다.

루트 CA 인증서는 CA 에서 자체 서명한 인증서로 공개키 기반 암호화를 사용한다. 모든 유효한 SSL 인증서는 업계에서 보안 리더로 알려진 신뢰할 수 있는 CA 가 발행한 루트 CA 인증서 아래 체인에 위치한다.