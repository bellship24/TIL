**목차**

- [1. 요약](#1-요약)
- [2. 현상](#2-현상)
- [3. 분석](#3-분석)
- [4. 해결 방법](#4-해결-방법)
  - [4.1. access token 생성 및 사용](#41-access-token-생성-및-사용)
  - [4.2. vs code 에 access token 등록](#42-vs-code-에-access-token-등록)
  - [4.3. 기존 macOS keychain 미사용 설정](#43-기존-macos-keychain-미사용-설정)
  - [4.4. git push 테스트](#44-git-push-테스트)

---

# 1. 요약

github 의 정책에 따라 2021.08.13 부터 여러 git 작업에서 password 인증을 못하게 하고 access token 을 사용하도록 정책이 업데이트 됐는데 이에 access token 을 생성하고 사용해보자.

# 2. 현상

GitHub 로 git push 할 때, 에러 발생

``` bash
$ git push -v  
다음에 푸시: https://github.com/bellship24/TIL
remote: Support for password authentication was removed on August 13, 2021. Please use a personal access token instead.
remote: Please see https://github.blog/2020-12-15-token-authentication-requirements-for-git-operations/ for more information.
fatal: unable to access 'https://github.com/bellship24/TIL/': The requested URL returned error: 403
```

vs code 에서도 안 됨

![](/.uploads/2021-08-14-04-37-23.png)

``` txt
Git: remote: Support for password authentication was removed on August 13, 2021. Please use a personal access token instead.
```

# 3. 분석

- [참고](https://github.blog/2020-12-15-token-authentication-requirements-for-git-operations/)
- 2021.08.13 부터 github 에서는 패스워드 기반으로 git 작업을 할 수 없다고 한다. 이로 인해 personal access token 을 사용하라고 한다.

# 4. 해결 방법

## 4.1. access token 생성 및 사용

- 아래에 링크의 절차를 따라 수행하자.
- [참고](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token)

## 4.2. vs code 에 access token 등록

명령 파레트에서 `GitHub Personal Access Token` 클릭

![](/.uploads/2021-08-14-04-50-02.png)

access token 입력

![](/.uploads/2021-08-14-04-50-32.png)

## 4.3. 기존 macOS keychain 미사용 설정

- 기존에 github 패스워드 를 macOS keychain 에 설정하고 git config 에 설정했을 수도 있다.
- 이 경우, 아래와 같이 이 기능을 삭제해야 한다.

``` bash
git config --unset credential.helper
git config --global --unset credential.helper
git config --system --unset credential.helper
```

## 4.4. git push 테스트

- 이제 PUSH 를 하면 username, password 를 입력하라고 할 수 있다. password 에 아까 생성한 PAT 를 입력하자.
- 혹은 keychain 에 github.com 에 대한 password 를 PAT 로 넣었다면, 위에 git config 삭제 후에 git push 를 하면 password 입력 없이 정상적으로 수행된다. 이 부분의 이유는 잘 모르겠다.