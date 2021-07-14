**요약**

``` text
debian 위에 anaconda3 를 공식 문서에 따라 설치하고 예시로 conda 환경을 생성하고 사용해보자.
```

**목차**

- [1. 전제](#1-전제)
  - [1.1. 관련 패키지 설치 (옵션)](#11-관련-패키지-설치-옵션)
  - [1.2. anaconda installer 다운로드](#12-anaconda-installer-다운로드)
  - [1.3. SHA-256 으로 데이터 무결성 검증](#13-sha-256-으로-데이터-무결성-검증)
- [2. 설치](#2-설치)
- [3. 검증](#3-검증)
  - [3.1. conda 가상 환경 생성](#31-conda-가상-환경-생성)
  - [3.2. 생성한 conda 가상 환경 activate 및 python 실행](#32-생성한-conda-가상-환경-activate-및-python-실행)
- [4. 기타](#4-기타)
  - [4.1. conda 자동시작 해제](#41-conda-자동시작-해제)

**참조**

- [[Anaconda Docs] Installing on Linux](https://docs.anaconda.com/anaconda/install/linux/)
- [[conda Docs] Command reference](https://conda.io/projects/conda/en/latest/commands.html)
- [[Blog] python interpreter 에서 python 경로 읽기](https://medium.com/@jspark141515/python과-python-패키지는-어디에-설치될까-2f2f31fc9baf)

---

# 1. 전제

## 1.1. 관련 패키지 설치 (옵션)

- ubuntu 에서 gui 를 사용해서 설치하려면 아래 패키지들을 미리 설치하자.

``` bash
sudo apt-get install libgl1-mesa-glx libegl1-mesa libxrandr2 libxrandr2 libxss1 libxcursor1 libxcomposite1 libasound2 libxi6 libxtst6
```

## 1.2. anaconda installer 다운로드

- [여기 링크](https://www.anaconda.com/products/individual#linux) 에서 linux 64-Bit (x86) Installer (544MB) 를 다운받았다. linux 에서 wget 으로 받으니 느려서 macOS 에 받은 다음에 FileZilla 로 옮겨놨다. 엄청 큰 shell 파일로 보인다.

``` bash
$ ls | grep Anaconda
Anaconda3-2021.05-Linux-x86_64.sh
```

## 1.3. SHA-256 으로 데이터 무결성 검증

- anaconda docs 에서는 사용할 installer 쉘 파일의 데이터 무결성(data integrity)을 검증하고 사용하길 권고한다. 아무래도 바이너리가 아닌 코드가 긴 쉘 파일이기 때문에 흔히 사용하는 다운로드 시에 해킹, 누락 등이 없이 제대로 파일이 다운 됐는지 확인하기 위해서이다.
- sha-256 암호화를 통해 데이터의 수정이나 변경이 있었는지 확인해보자.

``` bash
$ sha256sum Anaconda3-2021.05-Linux-x86_64.sh
2751ab3d678ff0277ae80f9e8a74f218cfc70fe9a9cdc7bb1c137d7e47e33d53  Anaconda3-2021.05-Linux-x86_64.sh
```

- 이 쉘 파일에 대한 해시값은 아래와 같이 [여기 링크](https://docs.anaconda.com/anaconda/install/hashes/Anaconda3-2021.05-Linux-x86_64.sh-hash/) 에서 확인할 수 있다. 일치하기 때문에 데이터가 무결하다.

![](/.uploads/2021-06-21-23-43-25.png)

# 2. 설치

- 실행 권한 부여

``` bash
$ chmod +x Anaconda3-2021.05-Linux-x86_64.sh
```

- installer 실행

``` bash
$ bash ./Anaconda3-2021.05-Linux-x86_64.sh
```

  - 라이선스 동의를 위해 enter 입력
  - 라이선스 문서 맨 밑으로 스크롤하여 Yes 입력
  - anaconda3 설치 경로 확정하기. 기본 경로인 `PREFIX=/home/<user>/anaconda<2 or 3>` 을 권고한다. 그리고 `/usr` 밑에는 권고하지 않는다.
  - installer 에 의해 매 스타트업 마다 anaconda3 를 시작하길 원하는 물음에 yes 를 권고한다. 만약, no 라고 했다면, 수동으로 `source <path to conda>/bin/activate` 를 실행하고 `conda init` 을 하자.

- terminal 재접속하여 base conda 환경이 activate 됐는지 확인해보자. 아래 처럼 `(base)` 로 보아 base conda 환경이 잡힌 것을 확인할 수 있다.

``` bash
(base) ldccai@dtlab-dev-k8s-pjb-4:~$
```

# 3. 검증

## 3.1. conda 가상 환경 생성

- 아래와 같이 pytorch 등의 필요 패키지를 포함하여 설치할 수도 있고 필요 패키지를 누락해서 설치하면 가상환경만 설정 가능하다.

``` bash
(base) $ conda create -n mytorchserve pytorch
Collecting package metadata (current_repodata.json): done
Solving environment: failed with repodata from current_repodata.json, will retry with next repodata source.
Collecting package metadata (repodata.json): done
Solving environment: done

## Package Plan ##

  environment location: /home/ldccai/anaconda3/envs/mytorchserve

  added / updated specs:
    - pytorch


The following packages will be downloaded:

    package                    |            build
    ---------------------------|-----------------
    _openmp_mutex-4.5          |            1_gnu          22 KB
    _pytorch_select-0.1        |            cpu_0           3 KB
    ca-certificates-2021.5.25  |       h06a4308_1         112 KB
    certifi-2021.5.30          |   py39h06a4308_0         139 KB
    cffi-1.14.5                |   py39h261ae71_0         226 KB
    intel-openmp-2019.4        |              243         729 KB
    ld_impl_linux-64-2.35.1    |       h7274673_9         586 KB
    libgcc-ng-9.3.0            |      h5101ec6_17         4.8 MB
    libgomp-9.3.0              |      h5101ec6_17         311 KB
    libmklml-2019.0.5          |                0        22.3 MB
    libstdcxx-ng-9.3.0         |      hd4cf53a_17         3.1 MB
    mkl-2020.2                 |              256       138.3 MB
    mkl-service-2.3.0          |   py39he8ac12f_0          54 KB
    mkl_fft-1.3.0              |   py39h54f3939_0         176 KB
    mkl_random-1.0.2           |   py39h63df603_0         346 KB
    ninja-1.10.2               |       hff7bd54_1         1.4 MB
    numpy-1.19.2               |   py39h89c1606_0          22 KB
    numpy-base-1.19.2          |   py39h2ae0177_0         4.2 MB
    pip-21.1.2                 |   py39h06a4308_0         1.8 MB
    python-3.9.5               |       h12debd9_4        22.6 MB
    pytorch-1.7.1              |cpu_py39h6a09485_0        31.7 MB
    setuptools-52.0.0          |   py39h06a4308_0         724 KB
    six-1.16.0                 |     pyhd3eb1b0_0          18 KB
    typing-extensions-3.7.4.3  |       hd3eb1b0_0          12 KB
    typing_extensions-3.7.4.3  |     pyh06a4308_0          28 KB
    tzdata-2020f               |       h52ac0ba_0         113 KB
    ------------------------------------------------------------
                                           Total:       233.9 MB

The following NEW packages will be INSTALLED:

  _libgcc_mutex      pkgs/main/linux-64::_libgcc_mutex-0.1-main
  _openmp_mutex      pkgs/main/linux-64::_openmp_mutex-4.5-1_gnu
  _pytorch_select    pkgs/main/linux-64::_pytorch_select-0.1-cpu_0
  blas               pkgs/main/linux-64::blas-1.0-mkl
  ca-certificates    pkgs/main/linux-64::ca-certificates-2021.5.25-h06a4308_1
  certifi            pkgs/main/linux-64::certifi-2021.5.30-py39h06a4308_0
  cffi               pkgs/main/linux-64::cffi-1.14.5-py39h261ae71_0
  intel-openmp       pkgs/main/linux-64::intel-openmp-2019.4-243
  ld_impl_linux-64   pkgs/main/linux-64::ld_impl_linux-64-2.35.1-h7274673_9
  libffi             pkgs/main/linux-64::libffi-3.3-he6710b0_2
  libgcc-ng          pkgs/main/linux-64::libgcc-ng-9.3.0-h5101ec6_17
  libgomp            pkgs/main/linux-64::libgomp-9.3.0-h5101ec6_17
  libmklml           pkgs/main/linux-64::libmklml-2019.0.5-0
  libstdcxx-ng       pkgs/main/linux-64::libstdcxx-ng-9.3.0-hd4cf53a_17
  mkl                pkgs/main/linux-64::mkl-2020.2-256
  mkl-service        pkgs/main/linux-64::mkl-service-2.3.0-py39he8ac12f_0
  mkl_fft            pkgs/main/linux-64::mkl_fft-1.3.0-py39h54f3939_0
  mkl_random         pkgs/main/linux-64::mkl_random-1.0.2-py39h63df603_0
  ncurses            pkgs/main/linux-64::ncurses-6.2-he6710b0_1
  ninja              pkgs/main/linux-64::ninja-1.10.2-hff7bd54_1
  numpy              pkgs/main/linux-64::numpy-1.19.2-py39h89c1606_0
  numpy-base         pkgs/main/linux-64::numpy-base-1.19.2-py39h2ae0177_0
  openssl            pkgs/main/linux-64::openssl-1.1.1k-h27cfd23_0
  pip                pkgs/main/linux-64::pip-21.1.2-py39h06a4308_0
  pycparser          pkgs/main/noarch::pycparser-2.20-py_2
  python             pkgs/main/linux-64::python-3.9.5-h12debd9_4
  pytorch            pkgs/main/linux-64::pytorch-1.7.1-cpu_py39h6a09485_0
  readline           pkgs/main/linux-64::readline-8.1-h27cfd23_0
  setuptools         pkgs/main/linux-64::setuptools-52.0.0-py39h06a4308_0
  six                pkgs/main/noarch::six-1.16.0-pyhd3eb1b0_0
  sqlite             pkgs/main/linux-64::sqlite-3.35.4-hdfb4753_0
  tk                 pkgs/main/linux-64::tk-8.6.10-hbc83047_0
  typing-extensions  pkgs/main/noarch::typing-extensions-3.7.4.3-hd3eb1b0_0
  typing_extensions  pkgs/main/noarch::typing_extensions-3.7.4.3-pyh06a4308_0
  tzdata             pkgs/main/noarch::tzdata-2020f-h52ac0ba_0
  wheel              pkgs/main/noarch::wheel-0.36.2-pyhd3eb1b0_0
  xz                 pkgs/main/linux-64::xz-5.2.5-h7b6447c_0
  zlib               pkgs/main/linux-64::zlib-1.2.11-h7b6447c_3


Proceed ([y]/n)? y


Downloading and Extracting Packages
libstdcxx-ng-9.3.0   | 3.1 MB    | ################################################## | 100%
six-1.16.0           | 18 KB     | ################################################## | 100%
libgcc-ng-9.3.0      | 4.8 MB    | ################################################## | 100%
numpy-base-1.19.2    | 4.2 MB    | ################################################## | 100%
mkl-service-2.3.0    | 54 KB     | ################################################## | 100%
typing-extensions-3. | 12 KB     | ################################################## | 100%
numpy-1.19.2         | 22 KB     | ################################################## | 100%
ld_impl_linux-64-2.3 | 586 KB    | ################################################## | 100%
cffi-1.14.5          | 226 KB    | ################################################## | 100%
python-3.9.5         | 22.6 MB   | ################################################## | 100%
mkl_random-1.0.2     | 346 KB    | ################################################## | 100%
_pytorch_select-0.1  | 3 KB      | ################################################## | 100%
setuptools-52.0.0    | 724 KB    | ################################################## | 100%
certifi-2021.5.30    | 139 KB    | ################################################## | 100%
typing_extensions-3. | 28 KB     | ################################################## | 100%
libmklml-2019.0.5    | 22.3 MB   | ################################################## | 100%
ca-certificates-2021 | 112 KB    | ################################################## | 100%
mkl_fft-1.3.0        | 176 KB    | ################################################## | 100%
pip-21.1.2           | 1.8 MB    | ################################################## | 100%
intel-openmp-2019.4  | 729 KB    | ################################################## | 100%
libgomp-9.3.0        | 311 KB    | ################################################## | 100%
pytorch-1.7.1        | 31.7 MB   | ################################################## | 100%
ninja-1.10.2         | 1.4 MB    | ################################################## | 100%
tzdata-2020f         | 113 KB    | ################################################## | 100%
mkl-2020.2           | 138.3 MB  | ################################################## | 100%
_openmp_mutex-4.5    | 22 KB     | ################################################## | 100%
Preparing transaction: done
Verifying transaction: done
Executing transaction: done
#
# To activate this environment, use
#
#     $ conda activate mytorchserve
#
# To deactivate an active environment, use
#
#     $ conda deactivate
```

## 3.2. 생성한 conda 가상 환경 activate 및 python 실행

- 생성한 가상환경으로 activate 해보자.

``` bash
(base) $ conda activate mytorchserve
(mytorchserve) $ pip list
Package           Version
----------------- -------------------
certifi           2021.5.30
cffi              1.14.5
mkl-fft           1.3.0
mkl-random        1.0.2
mkl-service       2.3.0
numpy             1.19.2
pip               21.1.2
pycparser         2.20
setuptools        52.0.0.post20210125
six               1.16.0
torch             1.7.1
typing-extensions 3.7.4.3
wheel             0.36.2
```

- 이제 python 으로 들어가서 파이썬 경로와 설치한 패키지들을 정상적으로 import 가능한지 확인해보자.

``` python
(mytorchserve) $ python
Python 3.9.5 (default, Jun  4 2021, 12:28:51)
[GCC 7.5.0] :: Anaconda, Inc. on linux
Type "help", "copyright", "credits" or "license" for more information.
>>> import sys
>>> sys.executable
'/home/ldccai/anaconda3/envs/mytorchserve/bin/python'
>>>
>>> import torch
>>> torch.__version__
'1.7.1'
>>>
```

- python 경로가 확실히 앞서 생성했던 `mytorchserve` 가상환경 경로이다.
- pytorch 도 설치한 대로 버전 출력이 잘 됐다.

# 4. 기타

## 4.1. conda 자동시작 해제

- 시스템 시작 시에 conda 의 base 환경을 자동으로 activate 하는 것을 중지하기 위해서 아래와 같이 파라미터를 설정하자.

``` bash
conda config --set auto_activate_base false
```