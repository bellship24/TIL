CPU 사양

``` bash
cat /proc/cpuinfo | more
```

CPU 모델명

``` bash
cat /proc/cpuinfo | grep -i "model name" | sort -u
```

물리 CPU 개수

``` bash
cat /proc/cpuinfo | grep -i "physical id" | sort -u | wc -l
```

물리 CPU 당 물리 CPU Core 개수

``` bash
cat /proc/cpuinfo | grep -i "cpu core" | sort -u
```

쓰레드 개수

``` bash
cat /proc/cpuinfo | egrep 'sibling|cpu cores' | head -2
```

- 쓰레드 개수 = `siblings`/`cpu cores`

전체 논리 CPU Core 개수

``` bash
cat /proc/cpuinfo | grep "processor" | wc -l
```

MEM 사양

``` bash
cat /proc/meminfo | more
```

MEM 사용량 요약

``` bash
free -h
```

disk 디바이스

``` bash
sudo fdisk -l
```

disk 마운트

``` bash
df -Th
```