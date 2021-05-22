# 210521_lvm

## lvscan 에러

에러 메시지

- `Device for PV [PV UUID] not found or rejected by a filter`

에러 확인

``` bash
> $ sudo lvscan
  WARNING: Device for PV mSTSYd-IKqq-S22F-I2MK-WQNk-nYOT-p0xDdO not found or rejected by a filter.
  ACTIVE            '/dev/vg_ee398b8c8b03ce6e6f63c2e97a3078e8/tp_e6a46651d84bcfdd4cfb58d17615eb8e' [2.00 GiB] inherit
  inactive          '/dev/vg_ee398b8c8b03ce6e6f63c2e97a3078e8/brick_e70d38f31f868daffa551a961d03b331' [2.00 GiB] inherit
  WARNING: Device for PV G9wtt3-R0av-qiiB-jV7D-MUr0-LQvu-swxJnE not found or rejected by a filter.
  ACTIVE            '/dev/vg_dff640ddae9953268f632cb622d16a96/tp_3c285659c2e36ac489fb921ae5507f82' [2.00 GiB] inherit
  ACTIVE            '/dev/vg_dff640ddae9953268f632cb622d16a96/brick_1b5a49c029a0b0b7238022f8adc6eb91' [2.00 GiB] inherit
  ACTIVE            '/dev/ubuntu-vg/ubuntu-lv' [<99.00 GiB] inherit
```

에러 제거

``` bash
> $ sudo pvscan --cache
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 0: Input/output error
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 2147418112: Input/output er
ror
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 2147475456: Input/output er
ror
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 4096: Input/output error

> $ sudo pvscan
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 0: Input/output error
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 2147418112: Input/output er
ror
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 2147475456: Input/output er
ror
  /dev/disk/by-id/dm-name-vg_dff640ddae9953268f632cb622d16a96-brick_1b5a49c029a0b0b7238022f8adc6eb91: read failed after 0 of 4096 at 4096: Input/output error
  PV /dev/sda3   VG ubuntu-vg       lvm2 [<99.00 GiB / 0    free]
  Total: 1 [<99.00 GiB] / in use: 1 [<99.00 GiB] / in no VG: 0 [0   ]
```

해결 확인

``` bash
> $ sudo lvscan
  ACTIVE            '/dev/ubuntu-vg/ubuntu-lv' [<99.00 GiB] inherit
```

## pvscan 에러

에러 메시지

- `read failed after 0 of 4096 at 0: Input/output error`

에러 확인

``` bash
> $ sudo pvscan
  /dev/disk/by-id/dm-name-vg_4c5ac6392f5a135f756c8e29f6396a44-brick_0215f93d2a52f3c410aff5f99920c8b6: read failed after 0 of 4096 at 0: Input/output error
  /dev/disk/by-id/dm-name-vg_4c5ac6392f5a135f756c8e29f6396a44-brick_0215f93d2a52f3c410aff5f99920c8b6: read failed after 0 of 4096 at 2147418112: Input/output error
  /dev/disk/by-id/dm-name-vg_4c5ac6392f5a135f756c8e29f6396a44-brick_0215f93d2a52f3c410aff5f99920c8b6: read failed after 0 of 4096 at 2147475456: Input/output error
  /dev/disk/by-id/dm-name-vg_4c5ac6392f5a135f756c8e29f6396a44-brick_0215f93d2a52f3c410aff5f99920c8b6: read failed after 0 of 4096 at 4096: Input/output error
  /dev/disk/by-id/dm-name-vg_bd53a7672b01a010d123ee4ef8a5abe7-brick_6adf46583d689d1c1e8ccd216ec32f61: read failed after 0 of 4096 at 0: Input/output error
  /dev/disk/by-id/dm-name-vg_bd53a7672b01a010d123ee4ef8a5abe7-brick_6adf46583d689d1c1e8ccd216ec32f61: read failed after 0 of 4096 at 2147418112: Input/output error
  /dev/disk/by-id/dm-name-vg_bd53a7672b01a010d123ee4ef8a5abe7-brick_6adf46583d689d1c1e8ccd216ec32f61: read failed after 0 of 4096 at 2147475456: Input/output error
  /dev/disk/by-id/dm-name-vg_bd53a7672b01a010d123ee4ef8a5abe7-brick_6adf46583d689d1c1e8ccd216ec32f61: read failed after 0 of 4096 at 4096: Input/output error
  PV /dev/sda3   VG ubuntu-vg       lvm2 [<99.00 GiB / 0    free]
  Total: 1 [<99.00 GiB] / in use: 1 [<99.00 GiB] / in no VG: 0 [0   ]
```

에러 제거

``` bash
> $ sudo dmsetup remove /dev/disk/by-id/dm-name-vg_4c5ac6392f5a135f756c8e29f6396a44-brick_0215f93d2a52f3c410aff5f99920c8b6
> $ sudo dmsetup remove /dev/disk/by-id/dm-name-vg_bd53a7672b01a010d123ee4ef8a5abe7-brick_6adf46583d689d1c1e8ccd216ec32f6
```

- `dmsetup remove <pvdisplay 시에 나오는 pv 경로>`

해결 확인

``` bash
> $ sudo pvscan
  PV /dev/sda3   VG ubuntu-vg       lvm2 [<99.00 GiB / 0    free]
  Total: 1 [<99.00 GiB] / in use: 1 [<99.00 GiB] / in no VG: 0 [0   ]
```

## 참고

- [블로그 - LVM 에러 해결](https://hiseon.me/linux/lvm-configuration/)