**요약**

- azure vm 의 스냅샷을 portal 에서 만들고 복구하는 방법을 검토해보자.

**목차**

- [1. azure portal 에서 azure vm 의 스냅샷 만드는 방법](#1-azure-portal-에서-azure-vm-의-스냅샷-만드는-방법)
- [2. Azure Portal 에서 azure vm 데이터를 복원하는 방법](#2-azure-portal-에서-azure-vm-데이터를-복원하는-방법)
  - [복원의 종류](#복원의-종류)
  - [기존 disk 를 선택한 restore point 로 복원](#기존-disk-를-선택한-restore-point-로-복원)

**참고 URL**

- [[azure docs] 포털 또는 PowerShell을 사용하여 스냅샷 만들기](https://docs.microsoft.com/ko-kr/azure/virtual-machines/windows/snapshot-copy-managed-disk)
- [[azure docs] Azure Portal에서 Azure VM 데이터를 복원하는 방법](https://docs.microsoft.com/ko-kr/azure/backup/backup-azure-arm-restore-vms)

---

# 1. azure portal 에서 azure vm 의 스냅샷 만드는 방법

- 스냅샷은 `Virtual Hard Drive`(VHD) 의 전체 읽기 전용 복사본이다. OS disk 또는 data disk VHD 의 스냅샷을 생성하여 백업으로 사용하거나 VM 문제를 해결할 수 있다.
- 스냅샷을 사용하여 새 VM 을 만들려면 스냅샷을 만들기 전에 VM 을 완전히 종료하여 진행 중인 모든 프로세스를 지우는 것이 권고된다.

1. azure portal 에서 `snapshot` 에 들어가기.
2. 스냅샷 창에서 `+Create` 를 선택.
3. 스냅샷의 이름을 입력.
4. 기존 리소스 그룹을 선택하거나 새 리소스 그룹의 이름을 입력.
5. Azure 데이터 센터 위치 선택.
6. 스냅샷을 만들기 위해 source disk 로 사용할 대상의 managed disk 를 선택.
7. 스냅샷 저장에 사용할 Account type 을 선택. 스냅샷이 고성능 디스크에 저장되어야 하는 경우가 아니면 Standard_HDD 를 선택.
8. `Create` 선택.

# 2. Azure Portal 에서 azure vm 데이터를 복원하는 방법

## 복원의 종류

- 새로운 VM 생성
- disk 만 복원
- 기존 disk 를 선택한 restore point 로 복원
- Cross region 복원

1. azure portal 에서 `Backup center` 에 들어가기.
2. `Backup center` 의 `Overview` 페이지에서 `Restore` 클릭.
3. `Datasource type` 에 `Azure Virtual machines` 를 클릭하고 `Backup instance` 를 선택.
4. VM 을 선택하고 `Continue` 클릭.
5. restore point 를 선택.

## 기존 disk 를 선택한 restore point 로 복원

- 특정 디스크를 복원하고 이 디스크를 기존 VM 에 있는 디스크에 대해 교체할 수 있다.
- 일단 대상이 되는 VM 이 존재해야 한다. 만약, VM 이 삭제됐다면, 이 옵션은 사용할 수 없다.
- 디스크를 교체하기 전에 Azure Backup 은 기존 VM 의 스냅샷을 만들어 지정된 staging location 에 저장한다. VM 에 연결된 기존 디스크가 선택된 restore point 로 바뀐다.
- 만든 스냅샷은 보존 정책에 따라 vault 에 복사되어 유지된다.
- 디스크 바꾸기 작업 후에는 원래 디스크가 리소스 그룹에 유지된다. 필요하지 않은 경우, 원본 디스크를 수동으로 삭제하도록 선택할 수 있다.
- 사용자 지정 이미지를 사용하여 만들어진 VM 을 포함하여 암호화되지 않은 managed VM 에 대해서도 이 기능을 지원한다. classic VM 과 unmanaged VM 에는 지원되지 않는다.

- 해당 rg 에 backup vault 생성
- backup center 에서 생성한 backup vault 로 해당 vm 에 대해 backup 활성화하여 backup instance 생성
- backup center 에서 restore 클릭 생성한 backup instance 선택

- azure portal 에서 Backup center 접속
- `+ backup` 클릭