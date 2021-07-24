**목차**

- [1. 요약](#1-요약)
- [2. 현상](#2-현상)
- [3. 분석](#3-분석)
- [4. 해결](#4-해결)

# 1. 요약

> GPU 가 있는 서버에서 nvidia-smi 할 때, driver/library version mismatch 이슈가 발생했을 때 해결 방법을 정리했다.

# 2. 현상

아래와 같이 `nvidia-smi` 할 경우, `driver/library version mismatch` 가 발생했다.

``` bash
$ nvidia-smi
Failed to initialize NVML: Driver/library version mismatch
```

# 3. 분석

재부팅 등으로 인해 아래와 같이 GPU driver 의 버전 mismatch 에러가 간혹 발생할 수 있다.

# 4. 해결

nvidia driver 들을 unload 하고 관련 모듈들을 삭제하면 된다.

nvidia driver 관련 모듈 조회

``` bash
$ lsmod | grep nvidia
nvidia_uvm      634880      8
nvidia_drm      53248       0
nvidia_modeset  790528      1   nvidia_drm
nvidia          12312576    86  nvidia_modeset,nvidia_uvm
```

nvidia driver 관련 모듈 unload

``` bash
sudo rmmod nvidia-uvm
sudo rmmod nvidia-drm
sudo rmmod nvidia-modeset
sudo rmod nvidia
```

이후 `nvidia-smi` 실행 시에 정상 작동