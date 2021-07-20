# 테스트 내역

- [x] 인증서 정보를 변경 후, 다시 원복할 때, helm upgrade 로 잘 적용됨.
- [x] gitlab 에서 했던 것과 같이 registry 에 대해(minio 도 동일할 것) cert-manager 로 별도 certificate 와 그에 따른 secret 을 생성하고 registry ingress 에 tls 의 secret 경로를 바로 잡아주면, 이 secret 의 ca.crt 를 클라이언트에 전달 시에 오류 없이 접근 가능하다.
- [ ] gitlab 인증서를 registry 와 minio 에서도 쓸 수 있게 wildcard? 로 설정하는 방법
- [ ] gitlab 처럼 registry 와 minio 의 인증서 secret 설정을 config.toml 에서도 할 수 있는지 확인해보기.